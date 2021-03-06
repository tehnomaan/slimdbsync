package eu.miltema.slimdbsync.test;

import static org.junit.Assert.*;
import java.sql.*;
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
	public void testColumnIsNullable() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class);
		assertNull(db.insert(new Entity1(null)).name);
	}

	@Test(expected = SQLException.class)
	public void testColumnIsNotNullable() throws Exception {
		new SchemaGenerator(db).sync(Entity1Columns.class);
		db.insert(new Entity1Columns());//count2 cannot be null: SQLException will be thrown
	}

	@Test
	public void testAlterColumn1() throws Exception {
		new SchemaGenerator(db).sync(Entity2.class);
		new SchemaGenEx(db, 5).sync(Entity2Altered.class);//5 changes: id default value, name nullability, count2 type, drop id_seq2, add id_seq3
		Entity2Altered e = new Entity2Altered();
		e.name = "John";
		e.count2 = 20000;
		assertEquals(20000, db.insert(e).count2.shortValue());
	}

	@Test
	public void testAlterColumn2() throws Exception {
		new SchemaGenerator(db).sync(Entity2Altered.class);
		new SchemaGenEx(db, 5).sync(Entity2.class);//5 changes: id default value, name nullability, count2 type, drop id_seq3, add id_seq2
		Entity2 e = db.insert(new Entity2(null, 123));
		assertNull(e.name);
		assertEquals(123, e.count2.intValue());
	}

	@Test(expected = SQLException.class)
	public void testAlterColumn3() throws Exception {
		new SchemaGenerator(db).sync(Entity2.class);//5 changes: id default value, name nullability, count2 type, drop id_seq2, add id_seq3
		new SchemaGenEx(db, 5).sync(Entity2Altered.class);
		db.insert(new Entity2(null, 123));//name is null, NOT NULL constraint must throw exception
	}

	@Test
	public void testNoChanges() throws Exception {
		new SchemaGenerator(db).sync(EntityWithTypes.class);
		new SchemaGenEx(db, 0).sync(EntityWithTypes.class);
	}

	@Test
	public void testColumnOrder() throws Exception {
		new SchemaGenerator(db).sync(Entity2.class);
		long id = db.insert(new Entity2("John Smith", 123456)).id;
		Object[] record = new Object[3];
		db.transaction((db, conn) -> {
			try (Statement stmt = conn.createStatement()) {
				try (ResultSet rs = stmt.executeQuery("SELECT * FROM entity2")) {
					rs.next();
					for(int i = 0; i < 3; i++)
						record[i] = rs.getObject(i + 1);
				}
				return 0L;
			}
		});
		assertEquals(id + "", record[0].toString());
		assertEquals("John Smith", record[1]);
		assertEquals(123456 + "", record[2].toString());
	}
}
