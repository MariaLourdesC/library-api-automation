package com.library.support;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generador de datos dinámicos.
 *
 * El servicio rechaza un alta cuya combinación isbn + aisle ya exista: responde
 * "Book Already Exists" en lugar de crear el libro. Si las pruebas usaran datos
 * fijos, pasarían la primera vez y fallarían en todas las siguientes.
 *
 * Por eso cada ejecución genera un isbn único a partir del instante de arranque
 * más un contador incremental. Así la suite es repetible: se puede correr las
 * veces que haga falta, en cualquier ambiente, sin limpiar nada a mano y sin que
 * dos ejecuciones simultáneas choquen entre sí.
 */
public final class BookFactory {

    private static final String PREFIJO = "qa";
    private static final long SEMILLA = Instant.now().toEpochMilli();
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private BookFactory() { }

    /** Libro válido con isbn único. El aisle es numérico, como exige el servicio. */
    public static Book libroValido() {
        int n = SECUENCIA.incrementAndGet();
        return new Book(
                "Fundamentos de Pruebas de Software " + n,
                PREFIJO + SEMILLA + n,
                String.valueOf(700 + n),
                "Maria Cordova");
    }

    /** Libro con aisle no numérico, para provocar el error 500 del servicio. */
    public static Book libroConAisleNoNumerico() {
        Book base = libroValido();
        return new Book(base.name(), base.isbn(), "pasillo-A", base.author());
    }

    /** ID que con certeza no existe en el servicio. */
    public static String idInexistente() {
        return PREFIJO + "-inexistente-" + SEMILLA + "-" + SECUENCIA.incrementAndGet();
    }
}
