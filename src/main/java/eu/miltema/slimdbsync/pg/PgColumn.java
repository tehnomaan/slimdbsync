package eu.miltema.slimdbsync.pg;

import javax.persistence.Column;

public class PgColumn {

	@Column(name = "column_name")
	String name;

	boolean isNullable;

	public String dataType;

	@Column(name = "column_default")
	public String defaultValue;

	public int ordinalPosition;
}
