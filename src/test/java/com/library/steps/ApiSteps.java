package com.library.steps;

import com.library.clients.LibraryClient;
import com.library.config.ApiConfig;
import com.library.support.Book;
import com.library.support.BookFactory;
import com.library.support.TestContext;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Pasos de las suites funcionales por endpoint. */
public class ApiSteps {

    private final TestContext context;
    private final LibraryClient client = new LibraryClient();

    public ApiSteps(TestContext context) { this.context = context; }

    // ---------------- Preparación ----------------

    @Dado("que preparo un libro válido")
    public void prepararLibroValido() {
        context.ultimoLibro(BookFactory.libroValido());
    }

    @Dado("que preparo un libro con el aisle no numérico")
    public void prepararLibroAisleInvalido() {
        context.ultimoLibro(BookFactory.libroConAisleNoNumerico());
    }

    @Dado("el libro ya fue dado de alta")
    public void darDeAltaPrevio() {
        Book libro = context.ultimoLibro();
        Response alta = client.agregarLibro(libro);
        assertEquals("La precondición falló: no se pudo dar de alta el libro " + libro,
                200, alta.statusCode());
        context.idsCreados().add(libro.expectedId());
    }

    // ---------------- Acciones ----------------

    @Cuando("envío la solicitud de alta del libro")
    public void enviarAlta() {
        Book libro = context.ultimoLibro();
        context.ultimaRespuesta(client.agregarLibro(libro));
        if (context.ultimaRespuesta().statusCode() == 200) {
            context.idsCreados().add(libro.expectedId());
        }
    }

    @Cuando("consulto el libro por su ID")
    public void consultarLibro() {
        context.ultimaRespuesta(client.obtenerLibro(context.ultimoLibro().expectedId()));
    }

    @Cuando("consulto un ID que no existe")
    public void consultarIdInexistente() {
        context.ultimaRespuesta(client.obtenerLibro(BookFactory.idInexistente()));
    }

    @Cuando("elimino el libro por su ID")
    public void eliminarLibro() {
        context.ultimaRespuesta(client.eliminarLibro(context.ultimoLibro().expectedId()));
    }

    @Cuando("elimino un ID que no existe")
    public void eliminarIdInexistente() {
        context.ultimaRespuesta(client.eliminarLibro(BookFactory.idInexistente()));
    }

    // ---------------- Validaciones ----------------

    @Entonces("el código de respuesta es {int}")
    public void validarCodigo(int esperado) {
        Response r = context.ultimaRespuesta();
        assertEquals("El código de respuesta no es el esperado. Cuerpo: " + r.asString(),
                esperado, r.statusCode());
    }

    @Entonces("la respuesta cumple el esquema {string}")
    public void validarEsquema(String esquema) {
        context.ultimaRespuesta().then()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/" + esquema));
    }

    @Entonces("el tiempo de respuesta es menor al umbral configurado")
    public void validarTiempo() {
        long umbral = ApiConfig.responseTimeThresholdMs();
        long real = context.ultimaRespuesta().time();
        assertTrue("El servicio tardó " + real + " ms y el umbral es " + umbral + " ms.",
                real < umbral);
    }

    @Entonces("el mensaje de la respuesta es {string}")
    public void validarMensajeMsgMayuscula(String esperado) {
        assertEquals("El mensaje de la respuesta no es el esperado.",
                esperado, context.ultimaRespuesta().jsonPath().getString("Msg"));
    }

    @Entonces("el mensaje de error es {string}")
    public void validarMensajeMsgMinuscula(String esperado) {
        assertEquals("El mensaje de la respuesta no es el esperado.",
                esperado, context.ultimaRespuesta().jsonPath().getString("msg"));
    }

    @Entonces("el ID devuelto es la concatenación de isbn y aisle")
    public void validarIdConcatenado() {
        Book libro = context.ultimoLibro();
        assertEquals("El ID devuelto no es isbn + aisle.",
                libro.expectedId(), context.ultimaRespuesta().jsonPath().getString("ID"));
    }

    @Entonces("los datos devueltos coinciden con los del libro que di de alta")
    public void validarDatosDevueltos() {
        Book libro = context.ultimoLibro();
        var json = context.ultimaRespuesta().jsonPath();
        assertEquals("El nombre del libro no coincide.", libro.name(),   json.getString("[0].book_name"));
        assertEquals("El isbn no coincide.",             libro.isbn(),   json.getString("[0].isbn"));
        assertEquals("El aisle no coincide.",            libro.aisle(),  json.getString("[0].aisle"));
        assertEquals("El autor no coincide.",            libro.author(), json.getString("[0].author"));
    }
}
