import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;

public class KeyValueDataStore {
    private final String filePath;
    private final ConcurrentHashMap<String, KeyValueEntry> store;
    private static final int MAX_KEY_LENGTH = 32;
    private static final int MAX_VALUE_SIZE = 16 * 1024; // 16KB
    private static final long MAX_FILE_SIZE = 1L * 1024 * 1024 * 1024; // 1GB
    private final ReentrantLock lock = new ReentrantLock();

    public KeyValueDataStore(String filePath) throws IOException {
        this.filePath = filePath != null ? filePath : getDefaultFilePath();
        this.store = new ConcurrentHashMap<>();
        loadFromFile();
    }

    private String getDefaultFilePath() {
        return Paths.get(System.getProperty("user.home"), "kv_store.json").toString();
    }

    private void loadFromFile() throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            String content = new String(Files.readAllBytes(file.toPath()));
            if (!content.isEmpty()) {
                try {
                    JSONObject json = new JSONObject(content);
                    for (Object keyObj : json.keySet()) {
                        String key = (String) keyObj;
                        JSONObject entryJson = json.getJSONObject(key);
                        KeyValueEntry entry = new KeyValueEntry(entryJson);
                        if (!entry.isExpired()) {
                            store.put(key, entry);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void persistToFile() throws IOException {
        lock.lock();
        try {
            if (new File(filePath).length() > MAX_FILE_SIZE) {
                throw new IOException("File size exceeded limit of 1GB.");
            }
            JSONObject json = new JSONObject();
            for (Map.Entry<String, KeyValueEntry> entry : store.entrySet()) {
                json.put(entry.getKey(), entry.getValue().toJSON());
            }
            Files.write(Paths.get(filePath), json.toString().getBytes());
        } finally {
            lock.unlock();
        }
    }

    public synchronized void create(String key, JSONObject value, int ttlInSeconds) throws Exception {
        validateKey(key);
        validateValueSize(value);

        if (store.containsKey(key)) {
            throw new Exception("Key already exists");
        }
        if (key.length() > 32) {
            throw new Exception("Key length exceeds 32 characters.");
        }

        KeyValueEntry entry = new KeyValueEntry(value, ttlInSeconds);
        store.put(key, entry);
        persistToFile();
    }

    public synchronized JSONObject read(String key) throws Exception {
        if (!store.containsKey(key)) {
            throw new Exception("Key not found.");
        }
        KeyValueEntry entry = store.get(key);
        if (entry.isExpired()) {
            store.remove(key);
            persistToFile();
            throw new Exception("Key '" + key + "' has expired.");
        }
        return entry.getValue();
    }

    public void delete(String key) throws Exception {
        if (!store.containsKey(key)) {
            throw new Exception("Key not found.");
        }

        KeyValueEntry entry = store.get(key);
        if (entry.isExpired()) {
            store.remove(key);
            persistToFile();
            throw new Exception("Key '" + key + "' has expired.");
        }

        store.remove(key);
        persistToFile();
    }

    public void startAutoCleanup(long intervalInSeconds) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            cleanupExpiredKeys();
            try {
                persistToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, intervalInSeconds, TimeUnit.SECONDS);
    }

    private void cleanupExpiredKeys() {
        Iterator<Map.Entry<String, KeyValueEntry>> iterator = store.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, KeyValueEntry> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    private void validateKey(String key) throws Exception {
        if (key.length() > MAX_KEY_LENGTH) {
            throw new Exception("Key length exceeded limit");
        }
    }

    private void validateValueSize(JSONObject value) throws Exception {
        if (value.toString().getBytes().length > MAX_VALUE_SIZE) {
            throw new Exception("Value size exceeded limit");
        }
    }

    public class KeyValueEntry {
        private JSONObject value;
        private long expirationTime;

        public KeyValueEntry(JSONObject value, long ttlInSeconds) {
            this.value = value;
            if (ttlInSeconds > 0) {
                this.expirationTime = System.currentTimeMillis() + ttlInSeconds * 1000;
            } else {
                this.expirationTime = -1;
            }
        }

        public KeyValueEntry(JSONObject jsonObject) {
            this.value = jsonObject.getJSONObject("value");
            this.expirationTime = jsonObject.getLong("expirationTime");
        }

        public boolean isExpired() {
            return expirationTime != -1 && System.currentTimeMillis() > expirationTime;
        }

        public JSONObject getValue() {
            return value;
        }

        public JSONObject toJSON() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("value", value);
            jsonObject.put("expirationTime", expirationTime);
            return jsonObject;
        }
    }

    public static void main(String[] args) {
        try {
            KeyValueDataStore kvStore = new KeyValueDataStore(null);
            JSONObject value = new JSONObject();
            value.put("data", "example value with TTL");

            kvStore.create("exampleKeyWithTTL", value, 5); // TTL of 5 seconds
            System.out.println("Value before TTL expires: " + kvStore.read("exampleKeyWithTTL"));

            Thread.sleep(6000); // Wait for TTL to expire

            try {
                System.out.println("Value after TTL expires: " + kvStore.read("exampleKeyWithTTL"));
            } catch (Exception e) {
                System.out.println(e.getMessage()); // Expect "Key 'exampleKeyWithTTL' has expired."
            }

            kvStore.startAutoCleanup(60); // Auto cleanup expired keys every 60 seconds

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
