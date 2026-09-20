package com.library.hooks;

import com.library.clients.LibraryClient;
import com.library.config.ApiConfig;
import com.library.support.TestContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.concurrent.atomic.AtomicInteger;

public class ApiHooks {

    private static final AtomicInteger ORDEN = new AtomicInteger();
    private final TestContext context;

    public ApiHooks(TestContext context) { this.context = context; }

    /**
     * Calienta la conexión una sola vez por ejecución y habilita el log del
     * tráfico HTTP hacia la carpeta de la corrida, como evidencia.
     */
    @BeforeAll
    public static void prepararSuite() throws Exception {
        Path run = carpetaDeCorrida();
        Files.createDirectories(run);
        PrintStream log = new PrintStream(Files.newOutputStream(
                run.resolve("trafico-http.log"),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND), true, "UTF-8");
        LibraryClient.registrarTrafico(log);
        LibraryClient.prepararConexion();
    }

    @Before
    public void anunciarEscenario(Scenario scenario) {
        scenario.log("Ambiente: " + ApiConfig.environment()
                + " | Servicio: " + ApiConfig.baseUrl()
                + " | Umbral de tiempo: " + ApiConfig.responseTimeThresholdMs() + " ms");
    }

    /**
     * Deja el servicio como lo encontró: elimina los libros creados aunque el
     * escenario haya fallado, para que una corrida no condicione a la siguiente.
     * Además guarda un resumen del caso como evidencia.
     */
    @After
    public void limpiarYRegistrar(Scenario scenario) throws Exception {
        LibraryClient client = new LibraryClient();
        for (String id : context.idsCreados()) {
            try {
                client.eliminarLibro(id);
            } catch (RuntimeException e) {
                scenario.log("No pude limpiar el libro " + id + ": " + e.getMessage());
            }
        }

        Path run = carpetaDeCorrida();
        Files.createDirectories(run);
        String nombre = String.format("%02d-%s", ORDEN.incrementAndGet(), slug(scenario.getName()));
        String resumen = "Caso: " + scenario.getName() + "\n"
                + "Resultado: " + scenario.getStatus() + "\n"
                + "Ambiente: " + ApiConfig.environment() + "\n"
                + "Servicio: " + ApiConfig.baseUrl() + "\n"
                + "Libros creados y limpiados: " + context.idsCreados() + "\n"
                + "Fecha UTC: " + java.time.Instant.now() + "\n";
        Files.writeString(run.resolve(nombre + "-resultado.txt"), resumen);
        scenario.attach(resumen.getBytes("UTF-8"), "text/plain", "Resumen del caso");
    }

    private static Path carpetaDeCorrida() {
        return Path.of(System.getProperty("run.directory", "target/runs/manual"));
    }

    private static String slug(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String s = sinAcentos.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.length() > 60 ? s.substring(0, 60) : s;
    }
}
