package eu.miltema.slimdbsync.test;

import javax.persistence.ManyToOne;

public class EntityFKey {

	public int id;

	public String name;

	@ManyToOne
	public Entity1 entity1;

	public EntityFKey() {
	}

	public EntityFKey(String name, Entity1 entity1) {
		this.name = name;
		this.entity1 = entity1;
	}
}
