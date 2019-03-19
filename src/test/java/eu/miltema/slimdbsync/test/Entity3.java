package eu.miltema.slimdbsync.test;

import javax.persistence.*;

public class Entity3 {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public int id;
	public String name;
}
