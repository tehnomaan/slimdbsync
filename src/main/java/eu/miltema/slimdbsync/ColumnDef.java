package eu.miltema.slimdbsync;

public class ColumnDef {
	public String name;
	public String type;
	public boolean isNullable;
	public boolean isJson;
	public boolean isIdentity;

	/**
	 * Value for this column is fetched from this sequence
	 */
	public String sourceSequence;

	/**
	 * A value from @Column(columnDefinition)
	 */
	public String columnDefinitionOverride;
}