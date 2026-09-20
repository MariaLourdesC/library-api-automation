# language: es
@funcional @deletebook
Característica: POST /DeleteBook.php - Eliminar un libro
  Como consumidor del servicio de librería
  quiero eliminar libros del catálogo
  para mantenerlo actualizado.

  @deletebook_exitoso
  Escenario: Eliminación exitosa de un libro existente
    Dado que preparo un libro válido
    Y el libro ya fue dado de alta
    Cuando elimino el libro por su ID
    Entonces el código de respuesta es 200
    Y la respuesta cumple el esquema "mensaje-error.json"
    Y el tiempo de respuesta es menor al umbral configurado
    Y el mensaje de error es "book is successfully deleted"

  @deletebook_inexistente
  Escenario: La eliminación de un libro inexistente responde 404 con mensaje de validación
    Cuando elimino un ID que no existe
    Entonces el código de respuesta es 404
    Y la respuesta cumple el esquema "mensaje-error.json"
    Y el tiempo de respuesta es menor al umbral configurado
    Y el mensaje de error es "Delete Book operation failed, looks like the book doesnt exists"
