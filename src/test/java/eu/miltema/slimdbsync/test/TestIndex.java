package eu.miltema.slimdbsync.test;

import org.junit.*;

import eu.miltema.slimdbsync.SchemaGenerator;

public class TestIndex extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Before
	public void setup() throws Exception {
		dropAllArtifacts();
	}

	@Test
	public void testAddIndexes() throws Exception {
		new SchemaGenerator(db).sync(Entity2.class);
		new SchemaGenEx(db, 2).sync(Entity2Index.class);//add 2 indexes declared in Entity2Index header
		Entity2Index e = new Entity2Index();
		db.insert(e);
		db.insert(e);
		e.name = "John";
		e.count2 = 27;
		db.insert(e);
		db.insert(e);
	}

	@Test
	public void testDropIndexes() throws Exception {
		new SchemaGenerator(db).sync(Entity2Index.class);
		new SchemaGenEx(db, 2).sync(Entity2.class);//drop 2 indexes declared in Entity2Index header
	}
}
