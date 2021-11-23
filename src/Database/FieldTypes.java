package Database;

public enum FieldTypes {
    INT("int"), STRING("varchar(255)");

    public String sqlNotation;
    FieldTypes(String sqlNotation) {
        this.sqlNotation = sqlNotation;
    }
}
