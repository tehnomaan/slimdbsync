package eu.miltema.slimdbsync.pg;

import javax.persistence.Table;

@Table(name = "information_schema.sequences")
public class PgSequence {
	String sequenceName;
}
