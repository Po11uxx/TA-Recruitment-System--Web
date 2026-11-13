package com.tamo.persistence;

import com.google.gson.*;
import com.tamo.model.*;
import com.tamo.util.JsonUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** JDBC persistence for the existing model. A transaction surrounds each API request. */
public final class PostgresStore {
    // Shared by migration and requests, including requests from another application process.
    private static final long LOCK_ID = 846512308L;
    private static final Gson GSON = JsonUtil.getGson();
    private static final Map<String, Class<?>> MODELS = new LinkedHashMap<>();
    static {
        MODELS.put("users", User.class);
        MODELS.put("profiles", Profile.class);
        MODELS.put("modules", com.tamo.model.Module.class);
        MODELS.put("jobs", Job.class);
        MODELS.put("applications", Application.class);
        MODELS.put("interviews", Interview.class);
        MODELS.put("notifications", Notification.class);
        MODELS.put("terms", Term.class);
        MODELS.put("departments", Department.class);
        MODELS.put("logs", LogEntry.class);
        MODELS.put("workloadAssignments", WorkloadAssignment.class);
        MODELS.put("messages", Message.class);
        MODELS.put("applicationPeriods", ApplicationPeriod.class);
        MODELS.put("weeklyAvailabilities", WeeklyAvailability.class);
        MODELS.put("courseSections", CourseSection.class);
        MODELS.put("sectionAssignments", SectionAssignment.class);
        MODELS.put("offerEmails", OfferEmail.class);
        MODELS.put("reviewRecords",ReviewRecord.class);
    }

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;
    private JsonObject baseline;

    public PostgresStore(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static PostgresStore fromEnvironment() {
        return new PostgresStore(env("TAMO_DB_URL", "jdbc:postgresql://127.0.0.1:5433/tamo"),
                env("TAMO_DB_USER", "tamo"), env("TAMO_DB_PASSWORD", "tamo_local_only"));
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public void begin() throws SQLException {
        if (connection != null) throw new IllegalStateException("Transaction already active");
        connection = DriverManager.getConnection(url, user, password);
        try {
            connection.setAutoCommit(false);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET LOCAL lock_timeout = '30s'");
                stmt.execute("SELECT pg_advisory_xact_lock(" + LOCK_ID + ")");
            }
        } catch (SQLException e) {
            close();
            throw e;
        }
    }

    /** Seed only an uninitialized database; restarting never reimports the JSON file. */
    public void initialize(Path seed, Path backupDirectory) throws SQLException, IOException {
        begin();
        try {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS tamo_schema_version "
                        + "(version INTEGER PRIMARY KEY, installed_at TIMESTAMPTZ NOT NULL DEFAULT now())");
            }
            boolean installed;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version = 1")) {
                installed = rs.next();
            }
            if (!installed) {
                for (String collection : MODELS.keySet()) {
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT to_regclass(?)")) {
                        stmt.setString(1, table(collection));
                        try (ResultSet rs = stmt.executeQuery()) {
                            rs.next();
                            if (rs.getObject(1) != null) {
                                throw new SQLException("Refusing to import into an unmanaged existing table: " + table(collection));
                            }
                        }
                    }
                }
                JsonObject source = JsonParser.parseString(Files.readString(seed)).getAsJsonObject();
                validate(source);
                Files.createDirectories(backupDirectory);
                Path backup = backupDirectory.resolve("data-before-postgres-"
                        + Instant.now().toString().replace(':', '-') + "-" + UUID.randomUUID() + ".json");
                Files.copy(seed, backup);
                try (var stream = PostgresStore.class.getResourceAsStream("/db/V1__initial.sql")) {
                    if (stream == null) throw new IOException("Missing database migration V1");
                    String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    try (Statement stmt = connection.createStatement()) { stmt.execute(sql); }
                }
                applyPeriodSchema();
                applyAvailabilitySchema();
                applySectionSchema();
                applyAssignmentSchema();
                applyOfferResponseSchema();
                applyReplyDeadlineSchema();
                applyQualificationsSchema();
                applyMatchingSchema();
                applyReviewSchema();
                applyCycleSchema();
                applySponsorSchema();
                applyAvailabilityEvidenceSchema();
                applyJobSectionsSchema();
                applyOptionalReplySchema();
                applyDirectAssignmentSchema();
                applyOfferEmailSchema();
                load();
                save(source);
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES (1)");
                }
                System.out.println("[Database] Imported existing data. JSON backup: " + backup.toAbsolutePath());
            } else {
                applyPeriodSchema();
                applyAvailabilitySchema();
                applySectionSchema();
                applyAssignmentSchema();
                applyOfferResponseSchema();
                applyReplyDeadlineSchema();
                applyQualificationsSchema();
                applyMatchingSchema();
                applyReviewSchema();
                applyCycleSchema();
                applySponsorSchema();
                applyAvailabilityEvidenceSchema();
                applyJobSectionsSchema();
                applyOptionalReplySchema();
                applyDirectAssignmentSchema();
                applyOfferEmailSchema();
            }
            finishPeriodMigration();
            finishReviewMigration(installed);
            commit();
        } finally {
            close();
        }
    }

    private boolean periodMigrationPending;

    private void applyPeriodSchema() throws SQLException, IOException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version = 2")) {
            periodMigrationPending = !rs.next();
        }
        if (!periodMigrationPending) return;
        try (var stream = PostgresStore.class.getResourceAsStream("/db/V2__application_periods.sql")) {
            if (stream == null) throw new IOException("Missing database migration V2");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    private void finishPeriodMigration() throws SQLException {
        if (!periodMigrationPending) return;
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("INSERT INTO application_periods "
                    + "(id,name,term_id,opens_at,closes_at,status,legacy,created_at,updated_at,row_order) "
                    + "SELECT 'period_legacy','Historical applications',NULL,'1970-01-01T00:00:00Z',"
                    + "'1970-01-02T00:00:00Z','closed',true,now()::text,now()::text,0 "
                    + "WHERE EXISTS (SELECT 1 FROM applications WHERE period_id IS NULL)");
            stmt.executeUpdate("UPDATE applications SET period_id = 'period_legacy' WHERE period_id IS NULL");
            stmt.execute("SET CONSTRAINTS ALL IMMEDIATE");
            stmt.execute("ALTER TABLE applications ALTER COLUMN period_id SET NOT NULL");
            stmt.execute("CREATE UNIQUE INDEX applications_one_per_period ON applications "
                    + "(applicant_id,job_id,period_id) WHERE period_id <> 'period_legacy'");
            stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES (2)");
        }
        periodMigrationPending = false;
    }

    private void applyAvailabilitySchema() throws SQLException, IOException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version = 3")) {
            if (rs.next()) return;
        }
        try (var stream = PostgresStore.class.getResourceAsStream("/db/V3__weekly_availability.sql")) {
            if (stream == null) throw new IOException("Missing database migration V3");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES (3)");
            }
        }
    }

    private void applyOfferEmailSchema() throws SQLException, IOException {
        try (Statement stmt=connection.createStatement();
             ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=17")) {
            if(rs.next()) return;
        }
        try (var stream=PostgresStore.class.getResourceAsStream("/db/V17__offer_email.sql")) {
            if(stream==null) throw new IOException("Missing migration V17");
            try (Statement stmt=connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES (17)");
            }
        }
    }

    private void applyDirectAssignmentSchema() throws SQLException, IOException {
        try (Statement stmt=connection.createStatement(); ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=16")) { if(rs.next()) return; }
        try (var stream=PostgresStore.class.getResourceAsStream("/db/V16__direct_assignment.sql")) {
            if(stream==null) throw new IOException("Missing migration V16");
            try (Statement stmt=connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
                stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES(16)");
            }
        }
    }

    private void applyOptionalReplySchema() throws SQLException, IOException {
        try (Statement stmt=connection.createStatement(); ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=15")) { if(rs.next()) return; }
        try (var stream=PostgresStore.class.getResourceAsStream("/db/V15__optional_assignment_reply.sql")) {
            if(stream==null) throw new IOException("Missing migration V15");
            try (Statement stmt=connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
                stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES(15)");
            }
        }
    }

    private void applyJobSectionsSchema() throws SQLException, IOException {
        try (Statement stmt=connection.createStatement(); ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=14")) { if(rs.next()) return; }
        try (var stream=PostgresStore.class.getResourceAsStream("/db/V14__job_sections.sql")) {
            if(stream==null) throw new IOException("Missing migration V14");
            try (Statement stmt=connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES(14)");
            }
        }
    }

    private void applyAvailabilityEvidenceSchema() throws SQLException, IOException {
        try(Statement stmt=connection.createStatement();ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=13")){if(rs.next())return;}
        try(var stream=PostgresStore.class.getResourceAsStream("/db/V13__availability_evidence.sql")){if(stream==null)throw new IOException("Missing migration V13");try(Statement stmt=connection.createStatement()){stmt.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES(13)");}}
    }
    private void applySponsorSchema() throws SQLException, IOException {
        try(Statement stmt=connection.createStatement();ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=12")){if(rs.next())return;}
        try(var stream=PostgresStore.class.getResourceAsStream("/db/V12__sponsor_csv.sql")){if(stream==null)throw new IOException("Missing migration V12");try(Statement stmt=connection.createStatement()){stmt.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES(12)");}}
    }
    private void applyCycleSchema() throws SQLException, IOException {
        try(Statement stmt=connection.createStatement();ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=11")){if(rs.next())return;}
        try(var stream=PostgresStore.class.getResourceAsStream("/db/V11__cycle_course_configuration.sql")){if(stream==null)throw new IOException("Missing migration V11");try(Statement stmt=connection.createStatement()){stmt.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES(11)");}}
    }
    private boolean reviewMigrationPending;
    private void applyReviewSchema() throws SQLException, IOException {
        try(Statement stmt=connection.createStatement();ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=10")){reviewMigrationPending=!rs.next();}
        if(!reviewMigrationPending)return;
        try(var stream=PostgresStore.class.getResourceAsStream("/db/V10__review_governance.sql")){if(stream==null)throw new IOException("Missing migration V10");try(Statement stmt=connection.createStatement()){stmt.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES(10)");}}
    }
    private void finishReviewMigration(boolean existing) throws SQLException {
        if(!reviewMigrationPending||!existing)return;
        try(Statement stmt=connection.createStatement()){
            stmt.executeUpdate("INSERT INTO review_records(id,application_id,actor_id,action,note,created_at,row_order) SELECT 'legacy_approval_'||a.id,a.id,'system','approve','Existing offer retained during governance upgrade',now()::text,0 FROM applications a WHERE a.status='offer' AND NOT EXISTS (SELECT 1 FROM review_records r WHERE r.application_id=a.id AND r.action IN ('approve','not_selected'))");
        }reviewMigrationPending=false;
    }
    private void applyMatchingSchema() throws SQLException, IOException {
        try(Statement stmt=connection.createStatement();ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=9")){if(rs.next())return;}
        try(var stream=PostgresStore.class.getResourceAsStream("/db/V9__matching_criteria.sql")){if(stream==null)throw new IOException("Missing migration V9");try(Statement stmt=connection.createStatement()){stmt.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES(9)");}}
    }
    private void applyQualificationsSchema() throws SQLException, IOException {
        try (Statement stmt=connection.createStatement();ResultSet rs=stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version=8")){if(rs.next())return;}
        try(var stream=PostgresStore.class.getResourceAsStream("/db/V8__application_qualifications.sql")){
            if(stream==null)throw new IOException("Missing migration V8");
            try(Statement stmt=connection.createStatement()){stmt.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES(8)");}
        }
    }
    private void applyReplyDeadlineSchema() throws SQLException, IOException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version = 7")) {
            if (rs.next()) return;
        }
        try (var stream = PostgresStore.class.getResourceAsStream("/db/V7__assignment_reply_deadlines.sql")) {
            if (stream == null) throw new IOException("Missing database migration V7");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES (7)");
            }
        }
    }

    private void applyOfferResponseSchema() throws SQLException, IOException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version = 6")) {
            if (rs.next()) return;
        }
        try (var stream = PostgresStore.class.getResourceAsStream("/db/V6__assignment_publication.sql")) {
            if (stream == null) throw new IOException("Missing database migration V6");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES (6)");
            }
        }
    }

    private void applyAssignmentSchema() throws SQLException, IOException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version = 5")) {
            if (rs.next()) return;
        }
        try (var stream = PostgresStore.class.getResourceAsStream("/db/V5__section_assignments.sql")) {
            if (stream == null) throw new IOException("Missing database migration V5");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES (5)");
            }
        }
    }

    private void applySectionSchema() throws SQLException, IOException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM tamo_schema_version WHERE version = 4")) {
            if (rs.next()) return;
        }
        try (var stream = PostgresStore.class.getResourceAsStream("/db/V4__course_sections.sql")) {
            if (stream == null) throw new IOException("Missing database migration V4");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                stmt.executeUpdate("INSERT INTO tamo_schema_version(version) VALUES (4)");
            }
        }
    }

    private static boolean jsonField(Class<?> type) { return type != String.class && !type.isPrimitive(); }

    public DataStore load() throws SQLException {
        requireTransaction();
        JsonObject root = new JsonObject();
        for (var model : MODELS.entrySet()) {
            JsonArray rows = new JsonArray();
            List<Field> fields = fields(model.getValue());
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM " + table(model.getKey()) + " ORDER BY row_order, id")) {
                while (rs.next()) {
                    JsonObject row = new JsonObject();
                    for (Field field : fields) {
                        Object value = rs.getObject(column(field.getName()));
                        if (value == null) continue;
                        if (jsonField(field.getType())) {
                            row.add(field.getName(), JsonParser.parseString(value.toString()));
                        } else {
                            row.add(field.getName(), GSON.toJsonTree(value));
                        }
                    }
                    rows.add(row);
                }
            }
            root.add(model.getKey(), rows);
        }
        baseline = root;
        return GSON.fromJson(root, DataStore.class);
    }

    public void save(DataStore data) throws SQLException {
        save(GSON.toJsonTree(data).getAsJsonObject());
    }

    private void save(JsonObject root) throws SQLException {
        requireTransaction();
        validate(root);
        for (var model : MODELS.entrySet()) {
            String collection = model.getKey();
            String table = table(collection);
            JsonArray rows = root.has(collection) && !root.get(collection).isJsonNull()
                    ? root.getAsJsonArray(collection) : new JsonArray();
            JsonArray oldRows = baseline.getAsJsonArray(collection);
            Map<String, JsonObject> previous = new HashMap<>();
            Map<String, Integer> previousOrder = new HashMap<>();
            for (int i = 0; i < oldRows.size(); i++) {
                JsonObject row = oldRows.get(i).getAsJsonObject();
                previous.put(row.get("id").getAsString(), row);
                previousOrder.put(row.get("id").getAsString(), i);
            }
            Set<String> currentIds = new HashSet<>();
            List<Field> fields = fields(model.getValue());
            String names = fields.stream().map(f -> quote(column(f.getName()))).collect(Collectors.joining(","));
            String params = fields.stream().map(f -> jsonField(f.getType()) ? "?::jsonb" : "?")
                    .collect(Collectors.joining(","));
            String updates = fields.stream().filter(f -> !f.getName().equals("id"))
                    .map(f -> quote(column(f.getName())) + "=EXCLUDED." + quote(column(f.getName())))
                    .collect(Collectors.joining(","));
            String sql = "INSERT INTO " + table + " (" + names + ",row_order) VALUES (" + params
                    + ",?) ON CONFLICT(id) DO UPDATE SET " + updates + ",row_order=EXCLUDED.row_order";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < rows.size(); i++) {
                    JsonObject row = rows.get(i).getAsJsonObject();
                    String id = row.get("id").getAsString();
                    currentIds.add(id);
                    if (row.equals(previous.get(id)) && Objects.equals(previousOrder.get(id), i)) continue;
                    int index = 1;
                    for (Field field : fields) bind(stmt, index++, row.get(field.getName()), field.getType());
                    stmt.setInt(index, i);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
                for (String id : previous.keySet()) {
                    if (!currentIds.contains(id)) { stmt.setString(1, id); stmt.addBatch(); }
                }
                stmt.executeBatch();
            }
        }
        baseline = root.deepCopy();
    }

    private static void bind(PreparedStatement stmt, int index, JsonElement value, Class<?> type) throws SQLException {
        if (value == null || value.isJsonNull()) { stmt.setObject(index, null); }
        else if (type == int.class) stmt.setInt(index, value.getAsInt());
        else if (type == double.class) stmt.setDouble(index, value.getAsDouble());
        else if (type == boolean.class) stmt.setBoolean(index, value.getAsBoolean());
        else if (jsonField(type)) stmt.setString(index, value.toString());
        else stmt.setString(index, value.getAsString());
    }

    private static void validate(JsonObject root) {
        for (String collection : MODELS.keySet()) {
            if (!root.has(collection) || root.get(collection).isJsonNull()) continue;
            Set<String> ids = new HashSet<>();
            for (JsonElement item : root.getAsJsonArray(collection)) {
                JsonObject row = item.getAsJsonObject();
                if (!row.has("id") || row.get("id").isJsonNull() || row.get("id").getAsString().isBlank()
                        || !ids.add(row.get("id").getAsString())) {
                    throw new IllegalArgumentException("Missing or duplicate record ID in " + collection);
                }
            }
        }
    }

    private static List<Field> fields(Class<?> model) {
        return Arrays.stream(model.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()).toList();
    }

    private static String table(String collection) { return column(collection); }
    private static String column(String field) { return field.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT); }
    private static String quote(String identifier) { return "\"" + identifier + "\""; }
    private void requireTransaction() {
        if (connection == null) throw new IllegalStateException("No active database transaction");
    }

    public void commit() throws SQLException { requireTransaction(); connection.commit(); }

    /** Closing an uncommitted JDBC transaction rolls it back. */
    public void close() throws SQLException {
        if (connection != null) {
            Connection closing = connection;
            connection = null;
            baseline = null;
            try { closing.rollback(); } finally { closing.close(); }
        }
    }
}
