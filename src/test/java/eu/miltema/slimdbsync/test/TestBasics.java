package eu.miltema.slimdbsync.test;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.junit.*;

import eu.miltema.slimdbsync.SchemaGenerator;

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
		new SchemaGenerator(db).sync(EntityWithTypes.class);
		new SchemaGenEx(db, 0).sync(EntityWithTypes.class);
	}

	@Test
	public void testCreateTable() throws Exception {
		new SchemaGenerator(db).sync(Entity0.class);
		db.insert(new Entity0());
		assertTrue(db.listAll(Entity0.class).size() == 1);
	}

	@Test
	public void testCreateAutoIncrementSequence() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class);
		Entity1 e = db.insert(new Entity1());
		assertTrue(e.id > 0);
		assertNotNull(db.getById(Entity1.class, e.id));
	}

	@Test
	public void testTableCustomNames() throws Exception {
		new SchemaGenerator(db).sync(EntityCustomNames.class);
		EntityCustomNames e = new EntityCustomNames();
		e.name = "John";
		e.complexName = "Fitzgerald";
		e.customName = "Kennedy";
		db.insert(e);
		e = db.sql("SELECT name, complex_name, name2 FROM custom_table").list(EntityCustomNames.class).get(0);
		assertEquals("John", e.name);
		assertEquals("Fitzgerald", e.complexName);
		assertEquals("Kennedy", e.customName);
	}

	@Test
	public void testAddColumn() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class);
		db.insert(new Entity1());
		new SchemaGenEx(db, 1).sync(Entity1WithCount.class);//add column count
		Entity1 e = db.insert(new Entity1WithCount("Jack", 15));
		assertEquals(2, db.listAll(Entity1WithCount.class).size());
		assertEquals(15, db.getById(Entity1WithCount.class, e.id).count.intValue());
	}

	@Test
	public void testDropColumn() throws Exception {
		new SchemaGenerator(db).sync(Entity1WithCount.class);
		Entity1WithCount e = db.insert(new Entity1WithCount("John", 15));
		new SchemaGenEx(db, 1).sync(Entity1.class);//drop column count
		assertNull(db.getById(Entity1WithCount.class, e.id).count);
	}

	@Test
	public void testDropIdColumn() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class);
		db.insert(new Entity1("Jack"));
		new SchemaGenEx(db, 2).sync(Entity1WithoutId.class);//drop (1) id-column and (2) associated sequence; primary key constraint will be implicitly cascade-dropped
		assertNull(db.listAll(Entity1.class).get(0).id);// column "id" was dropped
	}
	
	@Test
	public void testAddAndDropColumnsSimultaneously() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class);
		new SchemaGenEx(db, 2).sync(Entity1Columns.class);//drop name; add count2
		Entity1Columns e = new Entity1Columns();
		e.count2 = 123;
		assertEquals(123, db.insert(e).count2.intValue());
	}

	@Test(expected = SQLException.class)
	public void testDropTable() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class, Entity2.class);
		Entity1 e1 = db.insert(new Entity1("John"));
		new SchemaGenEx(db, 2).sync(Entity2.class);//drop table & sequence; primary key will be cascade-dropped
		db.getById(Entity1.class, e1.id);
	}

	@Test(expected = SQLException.class)
	public void testDropUnused() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class, Entity2.class);
		db.insert(new Entity1("John"));
		new SchemaGenerator(db).sync(Entity2.class);
		db.listAll(Entity1.class);
	}

	@Test
	public void testDontDropUnused() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class, Entity2.class);
		db.insert(new Entity1("John"));
		new SchemaGenerator(db).dropUnused(false).sync(Entity2.class);
		assertEquals(1, db.listAll(Entity1.class).size());
	}

	@Test
	public void columnIsNullable() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class);
		assertNull(db.insert(new Entity1(null)).name);
	}

	@Test(expected = SQLException.class)
	public void columnIsNotNullable() throws Exception {
		new SchemaGenerator(db).sync(Entity1Columns.class);
		db.insert(new Entity1Columns());//count2 cannot be null: SQLException will be thrown
	}
}
