package eu.miltema.slimdbsync.test;

import javax.persistence.Table;

@Table(name = "entity1")
public class Entity1WithCount extends Entity1 {
	Integer count;

	public Entity1WithCount() {
	}

	public Entity1WithCount(String name, Integer count) {
		super.name = name;
		this.count = count;
	}
}
