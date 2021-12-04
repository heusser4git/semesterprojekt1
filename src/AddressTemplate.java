import Database.DBField;

import java.util.ArrayList;

public class AddressTemplate {
    @DBField(name = "id", isFilter = true, datatype = "int", datatypesize = "(11)", isAutoincrement = true, isPrimaryKey = true, type = Integer.class)
    private int id;
    @DBField(name = "name", datatype = "varchar", datatypesize = "(255)", type = String.class, useQuotes = true)
    private String name;
}
