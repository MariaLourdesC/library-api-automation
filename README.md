# Library API — automatización de pruebas con Java y REST Assured

Suite de pruebas funcionales y de integración sobre el servicio REST
[https://rahulshettyacademy.com/Library](https://rahulshettyacademy.com/Library).

## Qué estoy validando

El servicio expone tres endpoints y los cubro todos, primero por separado y después encadenados.

**Suite funcional, un archivo por endpoint:**

| Endpoint | Casos |
|---|---|
| `POST /Addbook.php` | Alta válida, alta con `aisle` no numérico (500), alta duplicada |
| `GET /GetBook.php` | Consulta de libro existente, consulta de ID inexistente (404) |
| `POST /DeleteBook.php` | Baja de libro existente, baja de ID inexistente (404) |

**Suite integrada (CRUD):** doy de alta dos libros, los consulto, los elimino y vuelvo a consultarlos para comprobar que ya no están.

En cada caso valido cuatro cosas: el código de respuesta, la estructura del JSON contra un JSON Schema, el tiempo de respuesta contra el umbral configurado y el contenido de los datos devueltos.

## Qué uso y para qué

- **Java 17**: para escribir las pruebas.
- **REST Assured**: para construir las peticiones HTTP y leer las respuestas.
- **JSON Schema Validator**: para validar nombres de clave y tipos de dato sin escribir una aserción por campo.
- **Cucumber y Gherkin**: para que cada caso se lea como una frase y el reporte sea entendible por alguien que no programa.
- **Maven**: para las dependencias y la ejecución.

## Antes de ejecutar

Necesito un JDK 17 o superior, Maven 3.9 e internet. Lo compruebo con:

```sh
mvn -version
```

No hace falta configurar credenciales: el servicio es público y no pide autenticación.

## Cómo ejecuto

```sh
mvn test                                              # toda la suite
mvn test -Dcucumber.filter.tags="@funcional"          # solo las pruebas por endpoint
mvn test -Dcucumber.filter.tags="@integracion"        # solo el flujo CRUD
mvn test -Dcucumber.filter.tags="@addbook"            # solo un endpoint
```

## Cómo está organizado

```text
src/test/java/com/library/
  config/ApiConfig.java         Lee la URL y el umbral de tiempo del ambiente
  support/Book.java             El libro y el ID que el servicio debe devolver
  support/BookFactory.java      Generador de datos dinámicos
  support/TestContext.java      Estado compartido dentro de cada escenario
  clients/LibraryClient.java    Las tres llamadas al servicio
  steps/ApiSteps.java           Pasos de las pruebas por endpoint
  steps/CrudSteps.java          Pasos del flujo integrado
  hooks/ApiHooks.java           Calentamiento, evidencia y limpieza
  runners/RunApiTest.java       Ejecuta las suites con Cucumber
src/test/resources/
  config/qa.properties          URL del servicio y umbral de tiempo
  schemas/*.json                Esquemas de las respuestas
  features/01-addbook.feature   Alta de libros
  features/02-getbook.feature   Consulta de libros
  features/03-deletebook.feature  Baja de libros
  features/04-crud-integracion.feature  Flujo completo
target/runs/                    Reportes y evidencias de cada ejecución
```

## Variables y configuración

Todo lo que cambia entre ambientes vive en `src/test/resources/config/qa.properties`:

```properties
base.url=https://rahulshettyacademy.com/Library
response.time.ms=500
```

Para otro ambiente agrego su archivo en la misma carpeta y lo selecciono con `-Denv=<ambiente>`.

## Manejo de datos dinámicos

Este punto merece explicación porque condiciona todo el diseño.

El servicio **rechaza un alta cuya combinación `isbn` + `aisle` ya exista**: responde `Book Already Exists` en lugar de crear el libro. Con datos fijos, la suite pasaría la primera vez y fallaría en todas las siguientes.

Por eso `BookFactory` genera cada `isbn` a partir del instante de arranque más un contador incremental:

```java
private static final long SEMILLA = Instant.now().toEpochMilli();
private static final AtomicInteger SECUENCIA = new AtomicInteger();

public static Book libroValido() {
    int n = SECUENCIA.incrementAndGet();
    return new Book("Fundamentos de Pruebas de Software " + n,
                    "qa" + SEMILLA + n, String.valueOf(700 + n), "Maria Cordova");
}
```

Así la suite es **repetible**: se puede correr las veces que haga falta, sin limpiar nada a mano, y dos ejecuciones simultáneas no chocan entre sí.

El `ID` que devuelve el servicio es la concatenación de `isbn` y `aisle`. Lo calculo por mi cuenta en `Book.expectedId()` para poder afirmar contra un valor propio, y no contra el mismo dato que me devolvió el servicio, que no probaría nada.

Además, el hook posterior a cada escenario **elimina los libros creados aunque el caso haya fallado**, de modo que una corrida no condicione a la siguiente.

## El umbral de 500 ms y por qué hizo falta reutilizar la conexión

Medido de punta a punta, cada llamada tardaba alrededor de **1000 ms** y ninguna cumplía el umbral. Antes de dar por incumplido el requisito, separé el tiempo:

| Medición | Tiempo |
|---|---|
| Primera llamada, conexión nueva | ~1820 ms |
| Handshake TLS dentro de esa llamada | ~700 a 900 ms |
| Llamadas siguientes, conexión reutilizada | **~330 ms** |

El servicio responde en unos 330 ms y **sí cumple el umbral**. Lo que no cumplía era el establecimiento de la conexión, que no forma parte de lo que la prueba quiere evaluar.

REST Assured, por defecto, crea un cliente HTTP nuevo en cada llamada, y con él una conexión TLS nueva. La configuración en `LibraryClient` corrige eso:

- `reuseHttpClientInstance()` hace que todas las llamadas compartan el mismo cliente.
- Un `PoolingClientConnectionManager` mantiene varias conexiones vivas. Sin él, reutilizar el cliente deja un gestor de una sola conexión que aborta con `connection still allocated` en cuanto dos llamadas se solapan.
- Una llamada de calentamiento en `@BeforeAll` negocia la conexión antes de que se mida el primer caso.

Se usa `DefaultHttpClient`, que está marcado como obsoleto, porque REST Assured castea internamente el cliente a `AbstractHttpClient`. Un cliente construido con `HttpClientBuilder` devuelve `InternalHttpClient` y la llamada falla antes de salir a la red.

## Dónde veo el resultado

Cada `mvn test` crea una carpeta con fecha y hora dentro de `target/runs/`. Allí encuentro:

- `reporte/cucumber-html-reports/overview-features.html`: el reporte presentable, con gráficos y el detalle por escenario.
- `cucumber.html`, `cucumber.json` y `cucumber.xml`: resultados en crudo.
- `trafico-http.log`: la petición y la respuesta completas de cada llamada, como evidencia.
- `NN-<caso>-resultado.txt`: resumen de cada escenario, numerado en orden de ejecución.
- `surefire/`: resultado de Maven y JUnit.

Una nota sobre la versión del plugin de reportes: quedó fijada en `5.8.0` a propósito. Las posteriores están compiladas para Maven 4 y, con Maven 3, su configuración se ignora en silencio y la generación falla.

## Qué se ha comprobado

La suite completa se ejecutó el 20 de septiembre de 2026: ocho escenarios, ninguno fallido.

```text
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
```

## Lo que encontré mientras probaba

**El alta duplicada responde 200, no un código de error.** Al repetir un `isbn` + `aisle` ya existente, el servicio devuelve `HTTP 200` con el cuerpo `{"Msg":"Book Already Exists"}`. El libro no se crea, pero el código de respuesta dice que todo salió bien. Un cliente que solo mirara el status code creería que el alta fue exitosa. Lo correcto sería un `409 Conflict`. Lo dejo cubierto con un caso propio para que quede documentado el comportamiento real.

**El error 500 llega con el cuerpo vacío.** Al enviar un `aisle` no numérico el servicio responde `500` sin ningún mensaje. El consumidor no tiene forma de saber qué campo estaba mal. Un `400 Bad Request` con el detalle del campo sería lo esperable: un `500` sugiere una falla del servidor y no un dato inválido del cliente.

**Las claves del JSON no son consistentes entre endpoints.** El alta devuelve `Msg` con mayúscula inicial, mientras que la consulta y la baja devuelven `msg` en minúscula. Obliga a tratar cada respuesta distinto y es una fuente de errores fácil de evitar.
