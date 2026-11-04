package com.lavagest.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Gestor de conexión a la base de datos PostgreSQL de Supabase.
 * Pool de conexiones en caliente de ultra baja latencia.
 */
public class DatabaseConfig {
    private static final String DEFAULT_URL = "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0&tcpKeepAlive=true";
    private static final String DEFAULT_USER = "postgres.bjskevrbjzjceorecbzf";
    private static final int POOL_SIZE = 5;

    private final String url;
    private final String user;
    private final String password;

    private final BlockingQueue<Connection> pool = new LinkedBlockingQueue<Connection>(POOL_SIZE);
    private static DatabaseConfig instance;

    public DatabaseConfig(String url, String user, String password) {
        String cleanUrl = url;
        if (cleanUrl != null) {
            if (!cleanUrl.contains("prepareThreshold")) {
                cleanUrl += (cleanUrl.contains("?") ? "&" : "?") + "prepareThreshold=0";
            }
            if (!cleanUrl.contains("tcpKeepAlive")) {
                cleanUrl += "&tcpKeepAlive=true";
            }
        }
        this.url = cleanUrl;
        this.user = user;
        this.password = password;
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            Map<String, String> env = loadEnvFile();

            String dbUrl = getVal(env, "DB_URL", DEFAULT_URL);
            String dbUser = getVal(env, "DB_USER", DEFAULT_USER);
            String dbPass = getVal(env, "DB_PASSWORD", "");

            instance = new DatabaseConfig(dbUrl, dbUser, dbPass);
        }
        return instance;
    }

    /**
     * Obtiene una conexión reutilizable del pool en milisegundos sin pings lentos.
     * Espera brevemente si hay una conexión en uso para evitar crear sockets SSL costosos.
     */
    public Connection getConnection() throws SQLException {
        if (!isConfigured()) {
            throw new SQLException("La contraseña de la base de datos no está configurada en el archivo .env");
        }

        Connection physical = null;
        try {
            // Espera hasta 800ms por una conexión liberada antes de abrir una nueva
            physical = pool.poll(800, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {}

        while (physical != null && physical.isClosed()) {
            physical = pool.poll();
        }

        if (physical == null || physical.isClosed()) {
            physical = DriverManager.getConnection(url, user, password);
        }

        final Connection target = physical;
        return (Connection) Proxy.newProxyInstance(
                DatabaseConfig.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new InvocationHandler() {
                    private boolean closed = false;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("close".equals(method.getName())) {
                            if (!closed) {
                                closed = true;
                                if (!target.isClosed() && pool.remainingCapacity() > 0) {
                                    pool.offer(target);
                                } else {
                                    try { target.close(); } catch (Exception ignored) {}
                                }
                            }
                            return null;
                        }
                        if ("isClosed".equals(method.getName())) {
                            return closed || target.isClosed();
                        }
                        return method.invoke(target, args);
                    }
                }
        );
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public void warmupPool(final int count) {
        if (!isConfigured()) return;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < count; i++) {
                    try {
                        Connection c = DriverManager.getConnection(url, user, password);
                        if (!pool.offer(c)) {
                            c.close();
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
        t.setDaemon(true);
        t.setName("db-pool-warmup");
        t.start();
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public boolean isConfigured() {
        return password != null && !password.trim().isEmpty() && !password.contains("TU_CONTRASENA");
    }

    private static Map<String, String> loadEnvFile() {
        Map<String, String> map = new HashMap<String, String>();
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("../.env");
        }
        if (envFile.exists() && envFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                        map.put(key, val);
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return map;
    }

    private static String getVal(Map<String, String> env, String key, String defaultValue) {
        String val = env.get(key);
        if (val == null || val.trim().isEmpty()) {
            val = System.getenv(key);
        }
        return (val != null && !val.trim().isEmpty()) ? val.trim() : defaultValue;
    }
}
