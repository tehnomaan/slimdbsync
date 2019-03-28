package eu.miltema.slimdbsync.test;

import javax.persistence.*;

public class Entity2 {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq2")
	public Integer id;
	public String name;
	public Integer count2;

	public Entity2() {
	}

	public Entity2(String name, Integer count2) {
		this.name = name;
		this.count2 = count2;
	}
}
