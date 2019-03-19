package eu.miltema.slimdbsync;

public class PrimaryKeyDef {
	public String table;
	public String column;
	public String constraintName;

	public PrimaryKeyDef(String table, String column, String constraintName) {
		this.table = table;
		this.column = column;
		this.constraintName = constraintName;
	}
}
