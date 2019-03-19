package eu.miltema.slimdbsync;

public class ForeignKeyDef {
	public String localTable;
	public String localColumn;
	public String foreignTable;
	public String foreignColumn;
	public String constraintName;

	public ForeignKeyDef(String localTable, String localColumn, String foreignTable, String foreignColumn, String constraintName) {
		this.localTable = localTable;
		this.localColumn = localColumn;
		this.foreignTable = foreignTable;
		this.foreignColumn = foreignColumn;
		this.constraintName = constraintName;
	}
}
