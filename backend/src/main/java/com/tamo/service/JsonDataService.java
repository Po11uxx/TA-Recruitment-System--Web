package com.tamo.service;

import com.tamo.model.DataStore;
import com.tamo.model.LogEntry;
import com.tamo.persistence.PostgresStore;
import com.tamo.util.JsonUtil;
import jakarta.servlet.ServletContext;
import java.io.File;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Compatibility facade for existing services; all persistence now uses PostgreSQL.
 * The historical class name is retained to avoid changing every service API at once.
 */
public class JsonDataService {
    private static JsonDataService instance;
    private final PostgresStore store;
    private DataStore cache;
    private String beforeRequest;
    private final Object lock = new Object();

    private JsonDataService(DataStore detached) {store=null;cache=detached;}
    public static JsonDataService detached(DataStore data){return new JsonDataService(data);}
    public void replaceImportedData(DataStore changed){cache=changed;save();}
    private JsonDataService(String basePath) {
        store = PostgresStore.fromEnvironment();
        try {
            Path classes = Paths.get(basePath).toAbsolutePath();
            Path root = classes;
            while (root != null && !Files.exists(root.resolve("pom.xml"))) root = root.getParent();
            Path seed = root == null ? classes.resolve("data.json") : root.resolve("src/main/resources/data.json");
            Path backups = root == null ? Paths.get("backups") : root.getParent().resolve("backups");
            store.initialize(seed, backups);
            store.begin();
            try { cache = store.load();
                boolean changed=false;for(com.tamo.model.User user:cache.getUsers())if(user.getPassword()!=null&&!PasswordSecurity.encoded(user.getPassword())&&!user.getPassword().isBlank()){user.setPassword(PasswordSecurity.hash(user.getPassword()));changed=true;}
                if(changed)store.save(cache);store.commit();
                PrivateDocuments.migrateLegacyDirectory();
            } finally { store.close(); }
            System.out.println("[Database] PostgreSQL ready (JSON is now an import/backup only).");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize PostgreSQL. Start it with: docker compose up -d --wait db. "
                    + "Check TAMO_DB_URL / TAMO_DB_USER / TAMO_DB_PASSWORD.", e);
        }
    }

    public static String resolveClassesPath(ServletContext ctx) {
        if (ctx != null && ctx.getRealPath("/WEB-INF/classes") != null) return ctx.getRealPath("/WEB-INF/classes");
        return new File("target/classes").getAbsolutePath();
    }

    public static synchronized JsonDataService getInstance(String basePath) {
        if (instance == null) {
            if (basePath == null || basePath.isBlank()) throw new IllegalArgumentException("basePath is required");
            instance = new JsonDataService(basePath);
        }
        return instance;
    }

    public static synchronized JsonDataService ensureInitialized(String basePath) { return getInstance(basePath); }
    public static synchronized JsonDataService getInstance() {
        if (instance == null) throw new IllegalStateException("Database has not been initialized");
        return instance;
    }

    public Object requestLock() { return lock; }

    /** Called under requestLock, before any service reads or mutates the shared model. */
    public void beginRequest() {
        try {
            store.begin();
            cache = store.load();
            beforeRequest = JsonUtil.getGson().toJson(cache);
        } catch (Exception e) {
            try { store.close(); } catch (Exception suppressed) { e.addSuppressed(suppressed); }
            throw new IllegalStateException("Cannot read PostgreSQL", e);
        }
    }

    public void endRequest(boolean success) {
        try {
            if (success) store.commit();
        } catch (Exception e) {
            success = false;
            throw new IllegalStateException("Cannot commit PostgreSQL transaction", e);
        } finally {
            try { store.close(); }
            catch (Exception e) { throw new IllegalStateException("Cannot close PostgreSQL transaction", e); }
            finally {
                if (!success && beforeRequest != null) cache = JsonUtil.getGson().fromJson(beforeRequest, DataStore.class);
                beforeRequest = null;
            }
        }
    }

    public DataStore getData() { synchronized (lock) { return cache; } }

    public void save() {
        synchronized (lock) {
            try { if(store!=null)store.save(cache); }
            catch (Exception e) { throw new IllegalStateException("Cannot save PostgreSQL data", e); }
        }
    }

    public void reload() {
        synchronized (lock) {
            try { cache = store.load(); }
            catch (Exception e) { throw new IllegalStateException("Cannot reload PostgreSQL data", e); }
        }
    }

    public String generateId(String prefix, java.util.List<?> list) {
        synchronized (lock) {
            int max = 0;
            if (list != null) {
                for (Object item : list) {
                    String id = extractEntityId(item);
                    if (id != null && id.startsWith(prefix)) {
                        String suffix = id.substring(prefix.length());
                        try {
                            max = Math.max(max, Integer.parseInt(suffix));
                        } catch (NumberFormatException ignored) {
                            // non-numeric suffix — skip
                        }
                    }
                }
            }
            return prefix + String.format("%03d", max + 1);
        }
    }

    private static String extractEntityId(Object item) {
        if (item == null) return null;
        try {
            Object id = item.getClass().getMethod("getId").invoke(item);
            return id != null ? id.toString() : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * Append an audit log entry and persist.
     */
    /**
     * Executes the addLog operation.
     * @param action the action argument
     * @param type the type argument
     * @param actor the actor argument
     * @param actorId the actorId argument
     * @param details the details argument
     * @param ip the ip argument
     */
    /**
     * Executes the addLog operation.
     * @param action the action argument
     * @param type the type argument
     * @param actor the actor argument
     * @param actorId the actorId argument
     * @param details the details argument
     * @param ip the ip argument
     */
    public void addLog(String action, String type, String actor, String actorId, String details, String ip) {
        synchronized (lock) {
            LogEntry entry = new LogEntry();
            int nextNum = cache.getLogs() != null ? cache.getLogs().size() + 1 : 1;
            entry.setId("EVT-" + (9000 + nextNum));
            entry.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            entry.setAction(action);
            entry.setType(type);
            entry.setActor(actor != null ? actor : "System");
            entry.setActorId(actorId);
            entry.setDetails(details);
            entry.setIp(ip != null ? ip : "127.0.0.1");
            if (cache.getLogs() == null) {
                cache.setLogs(new java.util.ArrayList<>());
            }
            cache.getLogs().add(entry);
            save();
        }
    }
}
