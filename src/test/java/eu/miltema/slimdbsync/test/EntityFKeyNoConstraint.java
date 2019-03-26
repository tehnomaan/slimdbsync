package eu.miltema.slimdbsync.test;

import javax.persistence.Table;

@Table(name = "entity_fkey")
public class EntityFKeyNoConstraint {

	public int id;

	public String name;

	public Integer entity1Id;
}
