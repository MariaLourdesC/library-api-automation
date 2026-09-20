# language: es
@funcional @addbook
Característica: POST /Addbook.php - Agregar un libro
  Como consumidor del servicio de librería
  quiero registrar libros nuevos
  para que queden disponibles en el catálogo.

  @addbook_exitoso
  Escenario: Alta exitosa de un libro con datos válidos
    Dado que preparo un libro válido
    Cuando envío la solicitud de alta del libro
    Entonces el código de respuesta es 200
    Y la respuesta cumple el esquema "addbook-200.json"
    Y el tiempo de respuesta es menor al umbral configurado
    Y el mensaje de la respuesta es "successfully added"
    Y el ID devuelto es la concatenación de isbn y aisle

  @addbook_aisle_invalido
  Escenario: El alta falla con 500 cuando el aisle no es numérico
    Dado que preparo un libro con el aisle no numérico
    Cuando envío la solicitud de alta del libro
    Entonces el código de respuesta es 500
    Y el tiempo de respuesta es menor al umbral configurado

  @addbook_duplicado
  Escenario: El servicio no permite dar de alta dos veces el mismo libro
    Dado que preparo un libro válido
    Y el libro ya fue dado de alta
    Cuando envío la solicitud de alta del libro
    Entonces el código de respuesta es 200
    Y la respuesta cumple el esquema "addbook-200.json"
    Y el mensaje de la respuesta es "Book Already Exists"
