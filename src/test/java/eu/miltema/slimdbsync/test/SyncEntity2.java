package eu.miltema.slimdbsync.test;

import javax.persistence.*;

public class SyncEntity2 {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sync_id_seq2")
	public Integer id;
	public String name;
}
