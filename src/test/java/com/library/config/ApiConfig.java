package com.library.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Carga la configuración del ambiente desde src/test/resources/config/<env>.properties */
public final class ApiConfig {

    private static final Properties SETTINGS = new Properties();
    private static final String ENVIRONMENT = System.getProperty("env", "qa");

    static {
        if (!ENVIRONMENT.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("El nombre del ambiente no es válido: " + ENVIRONMENT);
        }
        try (InputStream in = ApiConfig.class.getResourceAsStream("/config/" + ENVIRONMENT + ".properties")) {
            if (in == null) {
                throw new IllegalStateException("No existe configuración para el ambiente " + ENVIRONMENT);
            }
            SETTINGS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("No pude cargar la configuración del ambiente.", e);
        }
    }

    private ApiConfig() { }

    public static String baseUrl() {
        return SETTINGS.getProperty("base.url").replaceAll("/+$", "");
    }

    /** Umbral de tiempo de respuesta exigido por la consigna, en milisegundos. */
    public static long responseTimeThresholdMs() {
        return Long.parseLong(SETTINGS.getProperty("response.time.ms"));
    }

    public static String environment() {
        return ENVIRONMENT;
    }
}
