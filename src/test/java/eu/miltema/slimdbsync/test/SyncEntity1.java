package eu.miltema.slimdbsync.test;

import javax.persistence.*;

public class SyncEntity1 {

	@Id
	@GeneratedValue
	public Integer id;
	public String name;
}
