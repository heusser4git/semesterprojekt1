import Database.DBField;

import java.util.ArrayList;

public class Function {
    @DBField(name="id", isPrimaryKey = true, datatype = "int(11)", isAutoincrement = true, type = Integer.class)
    private int id;
    @DBField(name = "name", useQuotes = true, datatype = "varchar(255)", type = String.class)
    private String name;
    @DBField(isNotInDb = true)
    private ArrayList addressTemplates;
    @DBField(name = "floor_idFloor", isForeignKey=true, foreignTable = "floor", foreignTableColumn = "id", datatype = "int(11)", type = Integer.class)
    private int idFloor;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList getAddressTemplates() {
        return addressTemplates;
    }

    public void setAddressTemplates(ArrayList addressTemplates) {
        this.addressTemplates = addressTemplates;
    }

    public int getIdFloor() {
        return idFloor;
    }

    public void setIdFloor(int idFloor) {
        this.idFloor = idFloor;
    }
}
