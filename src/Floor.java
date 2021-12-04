import Database.DBField;

import java.util.ArrayList;

/**
 * Floor aka Hauptgruppe or Etage (expl. 1. Stock)
 */
public class Floor {
    @DBField(name="id", isFilter = true, isPrimaryKey = true, datatype = "int(11)", isAutoincrement = true, type = Integer.class)
    private int id;
    @DBField(name = "name", isFilter = true, useQuotes = true, datatype = "varchar(255)", type = String.class)
    private String name;
    @DBField(isNotInDb = true)
    private ArrayList addresses;
    @DBField(name = "project_idProject", isFilter = true, isForeignKey=true, foreignTable = "project", foreignTableColumn = "id", datatype = "int(11)", type = Integer.class)
    private int idProject;

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

    public ArrayList getAddresses() {
        return addresses;
    }

    public void setAddresses(ArrayList addresses) {
        this.addresses = addresses;
    }

    public void addAddress(Address address) {
        this.addresses.add(address);
    }

    public void removeAddress(Address address) {
        this.addresses.remove(address);
    }

    public int getIdProject() {
        return idProject;
    }

    public void setIdProject(int idProject) {
        this.idProject = idProject;
    }
}
