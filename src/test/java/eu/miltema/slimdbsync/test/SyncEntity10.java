package eu.miltema.slimdbsync.test;

import javax.persistence.Table;

@Deprecated // siin läheb tuletamise tõttu primary key'de loogika sassi
@Table(name = "sync_entity1")
public class SyncEntity10 extends SyncEntity1 {

	public Integer count;
}
