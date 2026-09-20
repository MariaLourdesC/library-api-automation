package com.library.clients;

import com.library.config.ApiConfig;
import com.library.support.Book;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Capa de servicio. Concentra el "cómo se llama" a cada endpoint para que los
 * steps solo expresen el "qué se valida".
 */
public class LibraryClient {

    /*
     * Dos ajustes, por dos motivos distintos.
     *
     * 1. Reutilizar el cliente HTTP. Sin esto REST Assured crea un cliente nuevo
     *    por llamada y con el una conexion TLS nueva. Ese handshake cuesta entre
     *    700 y 900 ms contra este servicio, mas del doble del umbral exigido, y
     *    se sumaria al tiempo medido aunque el servidor responda rapido.
     *
     * 2. Darle un pool de conexiones. Reutilizar el cliente a secas deja como
     *    gestor un BasicClientConnectionManager, que admite una sola conexion
     *    simultanea y aborta con "connection still allocated" en cuanto una
     *    llamada se solapa con otra. El PoolingHttpClientConnectionManager
     *    mantiene varias conexiones vivas y las recicla.
     *
     * Con ambos, a partir de la segunda llamada el tiempo medido es el del
     * servicio, que es lo que la prueba debe evaluar.
     */
    private static final RestAssuredConfig CONFIG = RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .reuseHttpClientInstance()
                    .httpClientFactory(LibraryClient::crearClienteConPool)
                    .setParam("http.connection.timeout", 15000)
                    .setParam("http.socket.timeout", 15000));

    /*
     * Se usa DefaultHttpClient, que esta marcado como obsoleto, porque REST
     * Assured castea internamente el cliente a AbstractHttpClient. Un cliente
     * construido con HttpClientBuilder devuelve InternalHttpClient y la llamada
     * falla con GroovyCastException antes de salir a la red.
     */
    @SuppressWarnings("deprecation")
    private static org.apache.http.client.HttpClient crearClienteConPool() {
        PoolingClientConnectionManager pool = new PoolingClientConnectionManager();
        pool.setMaxTotal(20);
        pool.setDefaultMaxPerRoute(20);
        return new DefaultHttpClient(pool);
    }

    private static final RequestSpecification SPEC = new RequestSpecBuilder()
            .setConfig(CONFIG)
            .setBaseUri(ApiConfig.baseUrl())
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .build();

    private static boolean conexionPreparada = false;

    /**
     * Prepara la conexión antes de medir tiempos.
     *
     * El handshake TLS contra este servicio cuesta entre 700 y 900 ms, mucho más
     * que el propio procesamiento del servidor, que ronda los 330 ms. Si no se
     * descarta ese costo, la primera llamada de la suite incumpliría el umbral de
     * 500 ms por un motivo que no tiene que ver con el servicio bajo prueba.
     *
     * Esta llamada de calentamiento establece la conexión una sola vez; las
     * siguientes la reutilizan (keep-alive) y miden el tiempo real del servidor.
     */
    public static synchronized void prepararConexion() {
        if (conexionPreparada) return;
        try {
            RestAssured.given().spec(SPEC).queryParam("ID", "warmup").get("/GetBook.php").then().extract().response();
        } catch (RuntimeException ignorada) {
            // El calentamiento no valida nada; si falla, el propio test lo reportará.
        }
        conexionPreparada = true;
    }

    public Response agregarLibro(Book libro) {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("name", libro.name());
        cuerpo.put("isbn", libro.isbn());
        cuerpo.put("aisle", libro.aisle());
        cuerpo.put("author", libro.author());
        return consumir(RestAssured.given().spec(SPEC).body(cuerpo).when().post("/Addbook.php"));
    }

    public Response obtenerLibro(String id) {
        return consumir(RestAssured.given().spec(SPEC).queryParam("ID", id).when().get("/GetBook.php"));
    }

    public Response eliminarLibro(String id) {
        return consumir(RestAssured.given().spec(SPEC).body(Map.of("ID", id)).when().post("/DeleteBook.php"));
    }

    /**
     * Lee el cuerpo de la respuesta antes de devolverla.
     *
     * Al reutilizar el cliente HTTP, las conexiones vuelven a un pool con un
     * cupo limitado. Una conexion solo se libera cuando su respuesta fue leida
     * por completo. Si algun punto del codigo descarta la respuesta sin leerla
     * -por ejemplo la limpieza posterior al escenario- esa conexion queda
     * retenida y, tras unos pocos casos, la suite se queda sin conexiones
     * disponibles y falla por agotamiento, no por un defecto del servicio.
     *
     * REST Assured cachea el cuerpo, asi que leerlo aca no impide volver a
     * consultarlo despues desde los pasos de validacion.
     */
    private Response consumir(Response respuesta) {
        respuesta.getBody().asString();
        return respuesta;
    }

    /** Activa el log de request y response hacia el archivo indicado. */
    public static void registrarTrafico(PrintStream destino) {
        RestAssured.filters(new RequestLoggingFilter(destino), new ResponseLoggingFilter(destino));
    }
}
