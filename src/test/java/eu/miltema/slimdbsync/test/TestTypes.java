package eu.miltema.slimdbsync.test;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;

import org.junit.*;
import org.junit.runners.MethodSorters;

import eu.miltema.slimdbsync.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTypes extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
		new SchemaGenerator(db).sync(EntityWithTypes.class);
	}

	@Before
	public void setup() throws Exception {
		db.deleteWhere(EntityWithTypes.class, "f_short1<>?", 8);
	}

	private EntityWithTypes fetch() throws Exception {
		return db.listAll(EntityWithTypes.class).get(0);
	}

	@Test
	public void testString() throws Exception {
		db.insert(new EntityWithTypes(x -> x.fString = "abc"));
		assertEquals("abc", fetch().fString);
	}

	@Test
	public void testByte() throws Exception {
		db.insert(new EntityWithTypes(x -> x.fByte1 = (byte) 0xF3, x -> x.fByte2 = 34));
		EntityWithTypes e = fetch();
		assertEquals((byte) 0xF3, e.fByte1);
		assertEquals(34, e.fByte2.byteValue());
	}

	@Test
	public void testShort() throws Exception {
		db.insert(new EntityWithTypes(x -> x.fShort1 = -23, x -> x.fShort2 = 34));
		EntityWithTypes e = fetch();
		assertEquals(-23, e.fShort1);
		assertEquals(34, e.fShort2.shortValue());
	}

	@Test
	public void testInt() throws Exception {
		db.insert(new EntityWithTypes(x -> x.fInt1 = 23, x -> x.fInt2 = -34));
		EntityWithTypes e = fetch();
		assertEquals(23, e.fInt1);
		assertEquals(-34, e.fInt2.intValue());
	}

	@Test
	public void testLong() throws Exception {
		db.insert(new EntityWithTypes(x -> x.fLong1 = -23L, x -> x.fLong2 = 34L));
		EntityWithTypes e = fetch();
		assertEquals(-23L, e.fLong1);
		assertEquals(34L, e.fLong2.longValue());
	}

	@Test
	public void testFloat() throws Exception {
		db.insert(new EntityWithTypes(x -> x.fFloat1 = 23.78f, x -> x.fFloat2 = 34.11f));
		EntityWithTypes e = fetch();
		assertEquals(23.78f, e.fFloat1, .0001f);
		assertEquals(34.11f, e.fFloat2.floatValue(), .0001f);
	}

	@Test
	public void testDouble() throws Exception {
		db.insert(new EntityWithTypes(x -> x.fDouble1 = 23.28d, x -> x.fDouble2 = 34.11d));
		EntityWithTypes e = fetch();
		assertEquals(23.28d, e.fDouble1, .0001d);
		assertEquals(34.11d, e.fDouble2.doubleValue(), .0001d);
	}

	@Test
	public void testBigDecimal() throws Exception {
		BigDecimal b = new BigDecimal("1234567890123456789012345678901234567890.55");
		db.insert(new EntityWithTypes(x -> x.fBigDecimal = b));
		assertEquals(b, fetch().fBigDecimal);
	}

	@Test
	public void testByteArray() throws Exception {
		byte[] ba = {4, 7, 9, -5};
		db.insert(new EntityWithTypes(x -> x.fByteArray = ba));
		assertArrayEquals(ba, fetch().fByteArray);
	}

	@Test
	public void testTimestampWithoutTimezone() throws Exception {
		Instant i = Instant.parse("2007-12-03T10:15:30.00Z");
		Timestamp ts = Timestamp.from(i);
		db.insert(new EntityWithTypes(x -> x.fTimestamp = ts));
		assertEquals(ts, fetch().fTimestamp);
	}

	@Test
	public void testInstant() throws Exception {
		Instant i = Instant.parse("2007-12-03T10:15:30.00Z");
		db.insert(new EntityWithTypes(x -> x.fInstant = i));
		assertEquals(i, fetch().fInstant);
	}

	@Test
	public void testZonedDateTime() throws Exception {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse("2007-12-03T10:15:30.00Z"), ZoneId.of("Asia/Tokyo"));
		db.insert(new EntityWithTypes(x -> x.fZonedDateTime = zdt));
		assertEquals(zdt.toInstant(), fetch().fZonedDateTime.toInstant());
	}

	@Test
	public void testLocalDate() throws Exception {
		LocalDate ld = LocalDate.parse("2012-12-23");
		db.insert(new EntityWithTypes(x -> x.fLocalDate = ld));
		assertEquals(ld, fetch().fLocalDate);
	}

	@Test
	public void testLocalDateTime() throws Exception {
		LocalDateTime ldt = LocalDateTime.parse("2007-12-03T10:15:30.20");
		db.insert(new EntityWithTypes(x -> x.fLocalDateTime = ldt));
		assertEquals(ldt, fetch().fLocalDateTime);
	}

	@Test
	public void testJSon() throws Exception {
		db.insert(new EntityWithTypes(x -> x.fJson = new String[] {"abc", "def"}));
		assertEquals("def", fetch().fJson[1]);
	}
}
