package com.library.steps;

import com.library.clients.LibraryClient;
import com.library.config.ApiConfig;
import com.library.support.Book;
import com.library.support.BookFactory;
import com.library.support.TestContext;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pasos del flujo integrado. Cada aserción reutiliza las mismas validaciones de
 * las pruebas funcionales (código, esquema, tiempo y datos), agrupadas por etapa
 * para que el escenario se lea como el recorrido completo del libro.
 */
public class CrudSteps {

    private final TestContext context;
    private final LibraryClient client = new LibraryClient();

    public CrudSteps(TestContext context) { this.context = context; }

    // ---------------- Alta ----------------

    @Cuando("doy de alta el libro {string}")
    public void darDeAlta(String alias) {
        Book libro = BookFactory.libroValido();
        context.registrarLibro(alias, libro);
        context.ultimaRespuesta(client.agregarLibro(libro));
    }

    @Entonces("el alta de {string} fue correcta")
    public void validarAlta(String alias) {
        Book libro = context.libro(alias);
        Response r = context.ultimaRespuesta();
        validarCodigoYTiempo(r, 200);
        r.then().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/addbook-200.json"));
        assertEquals("El mensaje del alta no es el esperado.",
                "successfully added", r.jsonPath().getString("Msg"));
        assertEquals("El ID devuelto no es isbn + aisle.",
                libro.expectedId(), r.jsonPath().getString("ID"));
    }

    // ---------------- Consulta ----------------

    @Cuando("consulto el libro {string}")
    public void consultar(String alias) {
        context.ultimaRespuesta(client.obtenerLibro(context.libro(alias).expectedId()));
    }

    @Entonces("la consulta de {string} devuelve sus datos")
    public void validarConsulta(String alias) {
        Book libro = context.libro(alias);
        Response r = context.ultimaRespuesta();
        validarCodigoYTiempo(r, 200);
        r.then().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/getbook-200.json"));
        var json = r.jsonPath();
        assertEquals("El nombre del libro no coincide.", libro.name(),   json.getString("[0].book_name"));
        assertEquals("El isbn no coincide.",             libro.isbn(),   json.getString("[0].isbn"));
        assertEquals("El aisle no coincide.",            libro.aisle(),  json.getString("[0].aisle"));
        assertEquals("El autor no coincide.",            libro.author(), json.getString("[0].author"));
    }

    // ---------------- Baja ----------------

    @Cuando("elimino el libro {string}")
    public void eliminar(String alias) {
        context.ultimaRespuesta(client.eliminarLibro(context.libro(alias).expectedId()));
    }

    @Entonces("la baja de {string} fue correcta")
    public void validarBaja(String alias) {
        Response r = context.ultimaRespuesta();
        validarCodigoYTiempo(r, 200);
        r.then().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/mensaje-error.json"));
        assertEquals("El mensaje de la baja no es el esperado.",
                "book is successfully deleted", r.jsonPath().getString("msg"));
    }

    // ---------------- Verificación final ----------------

    @Entonces("la consulta de {string} indica que el libro ya no existe")
    public void validarQueYaNoExiste(String alias) {
        Response r = context.ultimaRespuesta();
        assertEquals("Tras eliminarlo, el libro \"" + alias + "\" seguía respondiendo. Cuerpo: "
                + r.asString(), 404, r.statusCode());
        r.then().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/mensaje-error.json"));
        assertEquals("El mensaje de libro inexistente no es el esperado.",
                "The book by requested bookid / author name does not exists!",
                r.jsonPath().getString("msg"));
    }

    // ---------------- Utilidad ----------------

    private void validarCodigoYTiempo(Response r, int codigoEsperado) {
        assertEquals("El código de respuesta no es el esperado. Cuerpo: " + r.asString(),
                codigoEsperado, r.statusCode());
        long umbral = ApiConfig.responseTimeThresholdMs();
        assertTrue("El servicio tardó " + r.time() + " ms y el umbral es " + umbral + " ms.",
                r.time() < umbral);
    }
}
