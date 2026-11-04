package com.lavagest.config;

public class SupabaseConfig {
    private final String url;
    private final String anonKey;

    public SupabaseConfig(String url, String anonKey) {
        if (isBlank(url) || isBlank(anonKey)) {
            throw new IllegalArgumentException("Faltan las variables de configuracion de Supabase.");
        }
        this.url = url;
        this.anonKey = anonKey;
    }

    public static SupabaseConfig fromEnvironment() {
        return new SupabaseConfig(
                System.getenv("SUPABASE_URL"),
                System.getenv("SUPABASE_ANON_KEY")
        );
    }

    public String getUrl() {
        return url;
    }

    public String getAnonKey() {
        return anonKey;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
