package com.library.support;

import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Estado compartido entre los pasos de un mismo escenario (inyectado por PicoContainer). */
public class TestContext {

    private Response ultimaRespuesta;
    private Book ultimoLibro;

    /** Libros creados durante el escenario, indexados por un alias legible del Gherkin. */
    private final Map<String, Book> librosPorAlias = new LinkedHashMap<>();

    /** IDs creados durante el escenario, para limpiarlos al final aunque el caso falle. */
    private final List<String> idsCreados = new ArrayList<>();

    public Response ultimaRespuesta() { return ultimaRespuesta; }
    public void ultimaRespuesta(Response r) { this.ultimaRespuesta = r; }

    public Book ultimoLibro() { return ultimoLibro; }
    public void ultimoLibro(Book b) { this.ultimoLibro = b; }

    public void registrarLibro(String alias, Book libro) {
        librosPorAlias.put(alias, libro);
        idsCreados.add(libro.expectedId());
        this.ultimoLibro = libro;
    }

    public Book libro(String alias) {
        Book libro = librosPorAlias.get(alias);
        if (libro == null) {
            throw new IllegalStateException("El escenario no creó ningún libro con el alias: " + alias);
        }
        return libro;
    }

    public Map<String, Book> librosPorAlias() { return librosPorAlias; }
    public List<String> idsCreados() { return idsCreados; }
}
