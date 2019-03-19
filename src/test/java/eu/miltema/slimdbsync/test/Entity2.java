package eu.miltema.slimdbsync.test;

import javax.persistence.*;

public class Entity2 {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq2")
	public Integer id;
	public String name;
	public Integer count2;
}
