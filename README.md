# SlimDbSync
SlimDbSync is a lightweight and simple-to-use library for updating database according to current entity model.
No update scripts (Java, XML, JSon) are necessary - this is different from other change management systems like Liquibase. 
2 lines of Java code is enough to configure and execute syncing.

# Basic Usage

Establish database link:

```java
Database db = new Database("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/demoDB", "demouser", "password");
new SchemaGenerator(db).sync(Employee.class, EmployeeDetails.class);
```

# Dependencies

Add SlimDbSync dependency into build.gradle:

```gradle
dependencies {
    implementation 'eu.miltema:slim-db-sync:1.0.1'
}
```

or alternatively, if using Maven, then into pom.xml:

```xml
<dependencies>
  <dependency>
    <groupId>eu.miltema</groupId>
    <artifactId>slim-db-sync</artifactId>
    <version>x.y.z</version>
  </dependency>
</dependencies>
```

In addition, build.gradle or pom.xml must refer to database driver. For example, when using PostgreSQL, build.gradle contains:

```gradle
dependencies {
    implementation 'eu.miltema:slim-db-sync:x.y.z'
    runtime 'org.postgresql:postgresql:42.2.5'
}
```

SlimDbSync itself depends on eu.miltema.slimorm and javax.persistence. These are resolved by build system automatically.

# Annotations

SlimDbSync supports these javax.persistence annotations (listed with supported attributes) when declaring entities:
* **@Table (name, uniqueConstraints)** - without this annotation, SlimDbSync uses snake-case class name as table name. For example, class EmployeeDetails would be stored into table employee\_details
* **@Column (name, nullable, unique)** - without this annotation, SlimDbSync uses snake-case field name as column name. For example, field dateOfBirth would be stored into column date\_of\_birth
* **@Transient** - annotation @Transient and Java modifier transient have the same effect: SlimDbSync will not create a column for this field in the database
* **@Id** - declares a primary key field. Only single-field primary keys are supported - composite primary keys are not. SlimDbSync generates a corresponding primary key
* **@GeneratedValue** - indicates that a sequence or identity is used as a default value for this column
* **@ManyToOne** - indicates that this is a foreign key field. Field type must be elementary type to store key value, not target entity class.
* **@JSon** - declares that this field will be stored as a JSon object. This is not a javax.persistence annotation, but SlimDbSync annotation
* **@Indexes** - this SlimDbSync annotation declares the indexes for this table. Several indexes can be declared, for example @Indexes({@Index("id"), @Index({"dateOfBirth", "name"})})

For example:

```java
@Table(name="employees")
@Indexes(@Index("name"))
public class Employee {
	@Id
	@GeneratedValue
	int id;

	String name;

	@Column(name = "dob")
	LocalDate dateOfBirth;

	@Transient
	boolean isDirty;

	transient boolean isDirty2;

	@JSon Contract[] contracts;

	@ManyToOne
	Department department;
}
```

NB! Since most database designs have an auto-generated primary key with name _id_, then SlimDbSync has a special shorthand: a field with name _id_ and without @Id is still treated as if both annotations were present.
If this is not what You need, declare Your primary key with a different name or add @Id annotation to a different field.

# SQL Dialects

By default, SlimDbSync uses PostgreSQL dialect.

# Data Types

SlimDbSync supports these Java types:
String, byte, Byte, short, Short, int, Integer, long, Long, float, Float, double, Double, BigDecimal, byte[], Timestamp, Instant, Date, LocalDate, LocalDateTime, ZonedDateTime, enum.

Be aware that PostgreSQL does not store timezone id into record (even when data type is _with time zone_). Therefore, all time-related columns store correct instant in time, but have lost the original timezone id.

# Logging

To keep the amount of dependencies low, SlimDbSync is not logging automatically. To add logging (System.out, log4j, slf etc), add custom logger:

```java
new DatabaseSync(db).
	setLogger(message -> System.out.println(message)).
	sync(entityClasses);
```
