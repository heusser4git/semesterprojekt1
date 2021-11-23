package Database;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Sql {
    private final ObjectToDb otb = new ObjectToDb();
    private Connection connection;

    public Connection getConnection() {
        return connection;
    }

    /**
     * Creates a DB Connection
     * @param database
     * @param user
     * @param password
     * @return
     * @throws SQLException
     */
    public boolean createConnection(String database, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(
                "jdbc:mariadb://localhost:3306/"+database, user, password);
        return this.isConnected();
    }

    public boolean isConnected() throws SQLException {
        return this.connection.isValid(10);
    }


    public <T> String sqlInsertObject(T object) {
//      INSERT INTO Customers SET ID=2, FirstName='User2';
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
     * @param field
     * @param value
     * @param <T>
     * @return
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

//    private String generateSqlColumnName(Field field) {
//        if(field.getType() == ArrayList.class) {
//            return null;
//        }
//        return field.getName();
//    }
    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Creates a table for object if its not allready there
     * @param object
     * @param <T>
     * @return
     * @throws SQLException
     */
    public <T> boolean createTable(T object) throws SQLException {
        // check if table allready exists for this object
        String sql = "SELECT COUNT(*) as result FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '" + otb.getObjectName(object) + "'";
        if(this.dbVorhanden(sql)) {
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
     * @param sql
     * @return
     * @throws SQLException
     */
    private boolean dbVorhanden(String sql) throws SQLException {
        ResultSet resultSet = this.executeQuery(sql);
        resultSet.first();
        int anzahl = Integer.parseInt(resultSet.getString("result"));
        if(anzahl>0) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the table has all columns required for the object
     * @param object
     * @param <T>
     * @return
     */
    public <T> boolean alterTableColumn(T object) throws SQLException {
        String className = otb.getObjectName(object);
        ArrayList<Field> fields = otb.getObjectFields(object);
        fields.forEach(field -> {
                DBField dbField = otb.getDbFieldFromField(field);
                if(!dbField.isNotInDb()) {
                    String sql = "SELECT COUNT(*) as result FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '" + className + "' AND column_name = '" + dbField.name() + "'";
                    try {
                        // column doesnt exist -> alter table
                        if (!this.dbVorhanden(sql)) {
                            this.addColumnToTable(className, field);
                        }
                        // column exists, but has the wrong type -> reset the type
                        if (this.dbVorhanden(sql) && !this.checkDbColumnType(className, field)) {
                            this.resetColumnAtTable(className, field);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        );
        return true;
    }

    /**
     * Creates a CREATE TABLE sql-string for the given object
     * @param object
     * @param <T>
     * @return
     */
    public <T> String sqlCreateTable(T object) {
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
     * Returns the SQL datatype for a attributeType
     * @param attributeType
     * @param <T>
     * @return
     */
    private <T> String sqlTypeFromAttributeType(T attributeType) {
        return this.sqlTypeFromAttributeType(attributeType, false);
    }
    private <T> String sqlTypeFromAttributeType(T attributeType, boolean withoutFieldsize) {
        String sqlFieldtyp = "";
        if(attributeType == String.class) {
            sqlFieldtyp = FieldTypes.STRING.sqlNotation;
            if(withoutFieldsize)
                sqlFieldtyp = "varchar";
        } else if(attributeType == int.class) {
            sqlFieldtyp = FieldTypes.INT.sqlNotation;
        } else if(attributeType == ArrayList.class) {
            // foreign key column zu sub-object-table
            sqlFieldtyp = FieldTypes.INT.sqlNotation;
        }
        return sqlFieldtyp;
    }

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
        System.out.println(sql);
        return statement.executeQuery(sql);
    }

    /**
     * Gets all matching Objects in a ArrayList
     * @param filter
     * @param <T>
     * @return
     */
    public <T> ArrayList<T> select(T filter) {

        String className = otb.getObjectName(filter);

        String sql = "Select * from " + className;

        // TODO hole die Daten aus der DB

        ArrayList<T> result = new ArrayList<>();

        return result;
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
