package eu.miltema.slimdbsync.test;

import javax.persistence.*;

public class Entity4 {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	public int id;
	public String name;
}
