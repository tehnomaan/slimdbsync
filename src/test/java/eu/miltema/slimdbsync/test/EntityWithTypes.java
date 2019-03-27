package eu.miltema.slimdbsync.test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.function.Consumer;

import eu.miltema.slimorm.JSon;

public class EntityWithTypes {

	public String fString;
	public byte fByte1;
	public Byte fByte2;
	public short fShort1;
	public Short fShort2;
	public int fInt1;
	public Integer fInt2;
	public long fLong1;
	public Long fLong2;
	public float fFloat1;
	public Float fFloat2;
	public double fDouble1;
	public Double fDouble2;
	public BigDecimal fBigDecimal;
	public byte[] fByteArray;
	public Timestamp fTimestamp;
	public Instant fInstant;
	public ZonedDateTime fZonedDateTime;
	public LocalDate fLocalDate;
	public LocalDateTime fLocalDateTime;
	@JSon public String[] fJson;
	public enum Enum1 {Jack, John, Joe};
	public Enum1 fEnum;

	public EntityWithTypes() {
	}

	@SafeVarargs
	public EntityWithTypes(Consumer<EntityWithTypes> ... initializers) {
		for(Consumer<EntityWithTypes> initializer : initializers)
			initializer.accept(this);
	}
}
