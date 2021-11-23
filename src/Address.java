import Database.DBField;

import javax.annotation.processing.Generated;

public class Address {
    @DBField(name = "id", isPrimaryKey = true, datatype = "int(11)", isAutoincrement = true, type = Integer.class)
    private int id;
    @DBField(name = "name", useQuotes = true, datatype = "varchar(255)", type = String.class)
    private String name;
    @DBField(name = "floor_idFloor", datatype = "int(11)", isForeignKey = true, foreignTable = "floor", foreignTableColumn = "id", type = Integer.class)
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

    public int getIdFloor() {
        return idFloor;
    }

    public void setIdFloor(int idFloor) {
        this.idFloor = idFloor;
    }
}