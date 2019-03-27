package eu.miltema.slimdbsync.def;

public class ColumnDef {
	public String name;
	public String type;
	public boolean isNullable;
	public boolean isJson;
	public int ordinal;//0-based column index within table

	/**
	 * Value for this column is fetched from this sequence
	 */
	public String sourceSequence;

	/**
	 * A value from @Column(columnDefinition)
	 */
	public String columnDefinitionOverride;

	public boolean isPrimaryKey() {
		throw new RuntimeException("Database ColumnDef primary key unknown");
	}
}
