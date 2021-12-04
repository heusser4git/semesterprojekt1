import Database.DBField;

import java.util.ArrayList;

public class Project {
    @DBField(name="id", isFilter = true, isPrimaryKey = true, datatype = "int", datatypesize = "(11)", isAutoincrement = true)
    private int id;
    @DBField(name = "name", isFilter = true, useQuotes = true, datatype = "varchar", datatypesize = "(255)", type = String.class)
    private String name;
    @DBField(isNotInDb = true)
    private ArrayList floors;


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

    public ArrayList getFloors() {
        return floors;
    }

    public void setFloors(ArrayList floors) {
        this.floors = floors;
    }

    public void addFloor(Floor floor) {
        this.floors.add(floor);
    }

    public void removeFloor(Floor floor) {
        this.floors.remove(floor);
    }
}
