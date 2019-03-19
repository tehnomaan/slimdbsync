package eu.miltema.slimdbsync.test;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.List;

import org.junit.*;

import eu.miltema.slimdbsync.DatabaseSync;

public class TestBasics extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Before
	public void setup() throws Exception {
		dropAllArtifacts();
	}

	@Test
	public void testNoChanges() throws Exception {
		new DatabaseSync(db).sync(SyncTypesEntity.class);
		new DatabaseSyncEx(db, 0).sync(SyncTypesEntity.class);
	}

	@Test
	public void testAddTable() throws Exception {
		new DatabaseSync(db).dropUnusedElements(false).sync(SyncEntity6.class);
		db.insert(new SyncEntity6());
		assertTrue(db.listAll(SyncEntity6.class).size() == 1);
	}

	@Test
	public void testAddSequence() throws Exception {
		new DatabaseSync(db).dropUnusedElements(false).sync(SyncEntity1.class);
		SyncEntity1 e = db.insert(new SyncEntity1());
		assertTrue(e.id > 0);
		assertNotNull(db.getById(SyncEntity1.class, e.id));
	}

	@Test
	public void testAddColumn() throws Exception {
		new DatabaseSync(db).sync(SyncEntity1.class);
		assertNotNull(db.insert(new SyncEntity1("John")));
		new DatabaseSyncEx(db, 1).sync(SyncEntity10.class);
		SyncEntity10 e10 = new SyncEntity10();
		e10.name = "Jack";
		e10.count = 15;
		e10 = db.insert(e10);
		List<SyncEntity10> list =  db.listAll(SyncEntity10.class);
		assertEquals(2, list.size());
		assertTrue(list.get(0).name.equals("John") || list.get(1).name.equals("John"));
	}

	@Test
	public void testDropColumn() throws Exception {
		new DatabaseSync(db).sync(SyncEntity10.class);
		SyncEntity10 e10 = new SyncEntity10();
		e10.name = "Jack";
		e10.count = 15;
		e10 = db.insert(e10);
		new DatabaseSyncEx(db, 1).sync(SyncEntity1.class);
		SyncEntity1 e1 = db.insert(new SyncEntity1("John"));
		assertEquals("John", db.getById(SyncEntity1.class, e1.id).name);
		assertNull(db.getById(SyncEntity10.class, e10.id).count);// column "count" was dropped
	}

	@Test
	public void testDropIdColumn() throws Exception {
		new DatabaseSync(db).sync(SyncEntity10.class);
		new DatabaseSyncEx(db, 2).sync(SyncEntity11.class);//drop id-column and associated sequence; primary key constraint will be silently cascade-dropped
	}
	
	@Test
	public void testAddAndDropColumnsSimultaneously() throws Exception {
		new DatabaseSync(db).sync(SyncEntity1.class);
		new DatabaseSyncEx(db, 3).sync(SyncEntity11.class);//drop (1) id-column and (2) associated sequence (primary key will be silently cascade-dropped); add (3) count-column
	}

	@Test(expected = SQLException.class)
	public void testDropTable() throws Exception {
		new DatabaseSync(db).sync(SyncEntity1.class, SyncEntity2.class);
		SyncEntity1 e1 = db.insert(new SyncEntity1("John"));
		new DatabaseSyncEx(db, 2).sync(SyncEntity2.class);//drop table & sequence; primary key will be cascade-dropped
		db.getById(SyncEntity1.class, e1.id);
	}
}
