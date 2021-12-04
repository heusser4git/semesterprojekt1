package Database;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Sql{
    private final ObjectToDb otb = new ObjectToDb();
    private Connection connection;
    private String dburl;
    public Connection getConnection() {
        return connection;
    }

    /**
     * Creates a DB Connection
     * @param databaseTyp   String "mariadb" or "mysql"
     * @param database      String database name
     * @param user          String database-user
     * @param password      String database-password
     * @return boolean Returns a TRUE for positive Connection
     * @throws SQLException
     */
    public boolean createConnection(String databaseTyp, String database, String user, String password) throws SQLException {
        if (databaseTyp == "mariadb"){
            dburl = "jdbc:mariadb://localhost:3306/";
        }
        if (databaseTyp == "mysql"){
            dburl = "jdbc:mysql://localhost/";
        }
        this.connection = DriverManager.getConnection(dburl + database, user, password);
        return this.isConnected();
    }

    public boolean isConnected() throws SQLException {
        return this.connection.isValid(10);
    }

    /**
     * Creates the SQL-Query String to INSERT a object into the DB
     * @param object    Object with Data to be written into the DB
     * @return          SQL-Query String for the Sql-INSERT
     */
    private <T> String sqlInsertObject(T object) {
      // INSERT INTO Customers SET ID=2, FirstName='User2';
        AtomicReference<String> sqlInserts = new AtomicReference<>("INSERT INTO " + otb.getObjectName(object) + " SET ");
        String className = otb.getObjectName(object);
        ArrayList<Field> fields = otb.getObjectFields(object);
        fields.forEach(field -> {
                DBField dbField = otb.getDbFieldFromField(field);
                if(!dbField.isNotInDb() && !dbField.isAutoincrement()) {
                    Method method = null;
                    try {
                        method = object.getClass().getMethod("get" + this.capitalize(field.getName()));
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                    try {
                        sqlInserts.set(sqlInserts + this.sqlPairs(field, method.invoke(object)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        );
        // remove the last ","
        return sqlInserts.toString().substring(0, sqlInserts.toString().length()-1);
    }

    /**
     * Makes Field=Value Pairs
     * @param field     Field which ist the column-part of the pair
     * @param value     Value which has to be paired to the column-part
     * @return  SQL-String Pair like <firstname='User2',>
     */
    private <T> String sqlPairs(Field field, Object value) {
        DBField dbField = otb.getDbFieldFromField(field);
//      INSERT INTO Customers SET ID=2, FirstName='User2';
        if(!dbField.isNotInDb()) {
            String str = dbField.name() + "=";
            if(dbField.useQuotes()) {
                str = str + "'" + value + "'";
            } else {
                str = str + value;
            }
            return str + ",";
        }
        return "";
    }

    /**
     * Capitalizes the given String (first letter big)
     * @param str   String to capitalize
     * @return      Capitalized String
     */
    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Creates a table for object if it is not allready there
     * @param object    Object to create a DB-Table for
     * @return          true = table created, false = checked if table-columns had to be changed
     * @throws SQLException
     */
    public <T> boolean createTable(T object) throws SQLException {
        // check if table allready exists for this object
        String sql = "SELECT COUNT(*) as result FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '" + otb.getObjectName(object) + "'";
        if(this.dbAvailable(sql)) {
            // table allready exists -> ALTER
            this.alterTableColumn(object);
            return false;
        } else {
            // table doesnt exist
            sql = this.sqlCreateTable(object);
            this.executeQuery(sql);
            return true;
        }
    }

    /**
     * Executes the sql and checks the result-column "result" for an 1 or 0
     * @param sql   Sql-Query String for checking the DB with
     * @return      true = is allready in DB
     * @throws SQLException
     */
    private boolean dbAvailable(String sql) throws SQLException {
        ResultSet resultSet = this.executeQuery(sql);
        resultSet.first();
        int count = Integer.parseInt(resultSet.getString("result"));
        if(count>0) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the table has all columns required for the object
     * @param <T>   Object to check the Columns
     */
    private <T> void alterTableColumn(T object) throws SQLException {
        String className = otb.getObjectName(object);
        ArrayList<Field> fields = otb.getObjectFields(object);
        fields.forEach(field -> {
                DBField dbField = otb.getDbFieldFromField(field);
                if(!dbField.isNotInDb()) {
                    String sql = "SELECT COUNT(*) as result FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '" + className + "' AND column_name = '" + dbField.name() + "'";
                    try {
                        // column doesnt exist -> alter table
                        if (!this.dbAvailable(sql)) {
                            this.addColumnToTable(className, field);
                        }
                        // column exists, but has the wrong type -> reset the type
                        if (this.dbAvailable(sql) && !this.checkDbColumnType(className, field)) {
                            this.resetColumnAtTable(className, field);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        );
    }

    /**
     * Creates a CREATE TABLE sql-string for the given object
     * @param <T>   Object to create a table for
     * @return      Returns the SQL-Query String used to create the Table for given object
     */
    private <T> String sqlCreateTable(T object) {
        // CREATE TABLE Persons (PersonID int, LastName varchar(255), FirstName varchar(255), Address varchar(255), City varchar(255));
        String className = otb.getObjectName(object);
        AtomicReference<String> sql = new AtomicReference<>("CREATE TABLE " + className + " (");
        ArrayList<Field> fields = otb.getObjectFields(object);
        AtomicReference<String> sqlFields = new AtomicReference<>("");
        fields.forEach(field -> {
            DBField dbField = otb.getDbFieldFromField(field);
            if(!dbField.isNotInDb()) {
                sqlFields.set(sqlFields + this.getSqlStringColumn(field));
            }
        });
        // remove the last ", "
        String sqlF = sqlFields.toString().substring(0, sqlFields.toString().length()-1);
        sql.set(sql + sqlF + ");");
        return sql.toString();
    }

    /**
     * Adds a Column to a DB-Table
     * @param tableName Name of the target table
     * @param field     Object-Field
     * @return boolean  if true, column was created
     * @throws SQLException
     */
    private <T> boolean addColumnToTable(String tableName, Field field) throws SQLException {
        String sqlColumn = this.getSqlStringColumn(field);
        if(sqlColumn != null) {
            String sql = "ALTER TABLE " + tableName + " ADD " + sqlColumn.substring(0, sqlColumn.length()-1) + ";";
            this.executeQuery(sql);
            return true;
        }
        return false;
    }
    private String getSqlStringColumn(Field field) {
        DBField dbField = otb.getDbFieldFromField(field);
        if(!dbField.isNotInDb()) {
            String fieldSuffix = "";
            if (dbField.isPrimaryKey()) {
                fieldSuffix += " PRIMARY KEY";
            }
            if (dbField.isAutoincrement()) {
                fieldSuffix += " NOT NULL AUTO_INCREMENT";
            }
            if (dbField.isForeignKey() && dbField.foreignTable().length() > 0 && dbField.foreignTableColumn().length() > 0) {
                fieldSuffix += ", FOREIGN KEY (" + dbField.name() + ") REFERENCES " + dbField.foreignTable() + "(" + dbField.foreignTableColumn() + ")";
            }
            return dbField.name() + " " + dbField.datatype() + dbField.datatypesize() + fieldSuffix + ",";
        }
        return null;
    }
    private boolean resetColumnAtTable(String tableName, Field field) throws SQLException {
        // Dieser SQL-Befehl ist abhaengig von der Datenbank -> MySQL arbeitet mit MODIFY, waehrend SQL-Server mit ALTER arbeitet
        String sqlColumn = this.getSqlStringColumn(field);
        String sql = "ALTER TABLE " + tableName + " MODIFY COLUMN " + sqlColumn.substring(0, sqlColumn.length()-1) + ";";
        this.executeQuery(sql);
        return true;
    }

    private <T> boolean removeColumnFromTable(String tableName, Field field) throws SQLException {
        DBField dbField = otb.getDbFieldFromField(field);
        String sql = "ALTER TABLE " + tableName + " DROP COLUMN " + dbField.name() + ";";
        this.executeQuery(sql);
        return true;
    }




    /**
     * Checks if the column-datatype is equivalent to the expectedColumnType
     * @param tableName
     * @param field
     * @return true if the column-datatype equals the expected-column-type, false if they arent equal
     * @throws SQLException
     */
    private boolean checkDbColumnType(String tableName, Field field) throws SQLException {
        DBField dbField = otb.getDbFieldFromField(field);
        if(!dbField.isNotInDb()) {
            // nicht in der DB -> kein check noetig
            return true;
        }
        String sql = "SELECT DATA_TYPE as result FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '" + tableName + "' AND column_name = '" + dbField.name() + "'";
        ResultSet resultSet = this.executeQuery(sql);
        resultSet.first();
        String colType = resultSet.getString("result");
        if (colType.equals(dbField.datatype())) {
            return true;
        }
        return false;
    }

    /**
     * Runs the sql-query with statement.executeQuery
     * @param sql
     * @return Returns the ResultSet
     * @throws SQLException
     */
    private ResultSet executeQuery(String sql) throws SQLException {
        Statement statement = this.connection.createStatement();
        statement.executeUpdate(sql);
        //System.out.println(sql);
        return statement.executeQuery(sql);
    }

    /**
     * Gets all matching Objects in a ArrayList
     * @param filter
     * @param <T>
     * @return
     */
    public <T> ArrayList<T> select(T filter) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, SQLException, InstantiationException {
        ArrayList<T> result = new ArrayList<>();
        String className = otb.getObjectName(filter);
        String where = this.getWhereClauseOfObject(filter);
        String sql = "SELECT * FROM " + className + " WHERE " + where;
        ResultSet resultSet = this.executeQuery(sql);
        // going through the select-results
        while (resultSet.next()) {
            T newObject = (T) filter.getClass().getDeclaredConstructor().newInstance();
            // going through the fields of the filter-objects
            for (Field field : otb.getObjectFields(filter)) {
                Method method = otb.getObjectMethod(newObject, "set" + this.capitalize(field.getName()));
                DBField dbField = otb.getDbFieldFromField(field);
                if (!dbField.isNotInDb() && dbField.name().length() > 0)
                    if (dbField.type().equals(Integer.class)) {
                        // integer-values as int
                        int value = resultSet.getInt(dbField.name());
                        if (value > 0) {
                            method.invoke(newObject, value);
                        }
                    } else if (dbField.type().equals(String.class)) {
                        // String-values as String
                        String string = resultSet.getString(dbField.name());
                        if (string != null && string.length() > 0) {
                            method.invoke(newObject, string);
                        }
                    }
                }
            result.add(newObject);
        }
        return result;
    }

    /**
     * Creates a WhereClause for a Object, which is used to SELECT the Data out of the DB-Table
     * @param filter    A Filter-Object which contains the Filter-Data
     * @return          Returns the WHERE-Part of the SQL-SELECT-Query
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public <T> String getWhereClauseOfObject(T filter) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String where = "";
        for (Field field : otb.getObjectFields(filter)) {
            DBField dbField = otb.getDbFieldFromField(field);
            Method method = filter.getClass().getMethod("get" + this.capitalize(field.getName()));
            if(dbField.isPrimaryKey()) {
                if(where.length()>0)
                    where += " AND ";
                where += dbField.name() + " IS NOT NULL";
                break;
            }
        }
        for (Field field : otb.getObjectFields(filter)) {
            DBField dbField = otb.getDbFieldFromField(field);
            if(dbField.isFilter()) {
                Method method = filter.getClass().getMethod("get" + this.capitalize(field.getName()));
                if (dbField.type().equals(Integer.class)) {
                    int i = (int) method.invoke(filter);
                    if (i > 0) {
                        if (where.length() > 0)
                            where += " AND ";
                        where += dbField.name() + "=" + i;
                    }
                } else if (dbField.type().equals(String.class)) {
                    String str = (String) method.invoke(filter);
                    if (str != null && str.length() > 0) {
                        if (where.length() > 0)
                            where += " AND ";
                        where += dbField.name() + " like '" + str + "'";
                    }
                }
            }
        }
        return where;
    }

    public <T> boolean insert(T object) throws SQLException {
        this.executeQuery(this.sqlInsertObject(object));
        return true;
    }
    public boolean update() {

        return true;
    }
    public boolean delete() {

        return true;
    }

}
