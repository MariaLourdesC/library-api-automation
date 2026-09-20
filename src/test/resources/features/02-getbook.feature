# language: es
@funcional @getbook
Característica: GET /GetBook.php - Obtener un libro
  Como consumidor del servicio de librería
  quiero consultar un libro por su ID
  para conocer sus datos.

  @getbook_exitoso
  Escenario: Consulta exitosa de un libro existente
    Dado que preparo un libro válido
    Y el libro ya fue dado de alta
    Cuando consulto el libro por su ID
    Entonces el código de respuesta es 200
    Y la respuesta cumple el esquema "getbook-200.json"
    Y el tiempo de respuesta es menor al umbral configurado
    Y los datos devueltos coinciden con los del libro que di de alta

  @getbook_inexistente
  Escenario: La consulta de un ID inexistente responde 404 con mensaje de validación
    Cuando consulto un ID que no existe
    Entonces el código de respuesta es 404
    Y la respuesta cumple el esquema "mensaje-error.json"
    Y el tiempo de respuesta es menor al umbral configurado
    Y el mensaje de error es "The book by requested bookid / author name does not exists!"
