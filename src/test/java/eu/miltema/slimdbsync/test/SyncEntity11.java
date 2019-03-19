package eu.miltema.slimdbsync.test;

import javax.persistence.Table;

@Deprecated // siin läheb tuletamise tõttu primary key'de loogika sassi
@Table(name = "sync_entity1")
public class SyncEntity11 {
	public String name;
	public Integer count;
}
