package eu.miltema.slimdbsync.test;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name = "custom_table")
public class EntityCustomNames {

	public String name;
	public String complexName;

	@Column(name = "name2")
	public String customName;
}
