package com.library.support;

/** Representa el libro que se envía al servicio y el ID que este devuelve. */
public class Book {

    private final String name;
    private final String isbn;
    private final String aisle;
    private final String author;

    public Book(String name, String isbn, String aisle, String author) {
        this.name = name;
        this.isbn = isbn;
        this.aisle = aisle;
        this.author = author;
    }

    public String name()   { return name; }
    public String isbn()   { return isbn; }
    public String aisle()  { return aisle; }
    public String author() { return author; }

    /**
     * El ID que el servicio debe devolver es la concatenación de isbn y aisle.
     * Calcularlo acá permite que la prueba afirme contra un valor esperado propio
     * en vez de contra el mismo dato que devolvió el servicio.
     */
    public String expectedId() {
        return isbn + aisle;
    }

    @Override
    public String toString() {
        return "Book{name='" + name + "', isbn='" + isbn + "', aisle='" + aisle + "', author='" + author + "'}";
    }
}
