package eu.miltema.slimdbsync.test;

import javax.persistence.*;

public class Entity1 {

	@Id
	@GeneratedValue
	public Integer id;
	public String name;

	public Entity1() {
	}

	public Entity1(String name) {
		this.name = name;
	}
}
