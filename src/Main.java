import Database.ObjectToDb;
import Database.Sql;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
ObjectToDb otb = new ObjectToDb();

        Sql sql = new Sql();
        sql.createConnection("semesterprojekt", "root", "123456");

        Project p = new Project();
        p.setId(2);
        p.setName("hansi");

//        System.out.println(sql.sqlInsertObject(p));
//
        sql.createTable(p);
        sql.insert(p);

        Address a = new Address();
        a.setName("Adresse 1");
        a.setIdFloor(1);

        Address a2 = new Address();
        a2.setName("Adresse 2");
        a2.setIdFloor(1);

        Floor f = new Floor();
        f.setName("First Floor");
        f.setIdProject(1);
//        f.addAddress(a);
//        f.addAddress(a2);

        sql.createTable(f);
        sql.insert(f);
//        sql.createTable(a2);
//        sql.insert(a);
//        sql.insert(a2);

//        sql.createTable(f);
//        AddressTemplate at = new AddressTemplate();
//        sql.createTable(at);
//        sql.createTable(a);


    }
}
