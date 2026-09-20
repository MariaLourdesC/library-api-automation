# language: es
@integracion @crud
Característica: Flujo integrado CRUD sobre todos los endpoints
  Quiero encadenar alta, consulta y baja de dos libros
  para verificar que los tres servicios funcionan de forma consistente entre sí.

  @crud_completo
  Escenario: Alta, consulta, baja y verificación de baja de dos libros
    # --- Alta de los dos libros: valida 200, esquema, tiempo e ID (puntos a, b, c, d) ---
    Cuando doy de alta el libro "primero"
    Entonces el alta de "primero" fue correcta
    Cuando doy de alta el libro "segundo"
    Entonces el alta de "segundo" fue correcta

    # --- Consulta de los dos libros: valida 200, esquema y tiempo (puntos a, b, c) ---
    Cuando consulto el libro "primero"
    Entonces la consulta de "primero" devuelve sus datos
    Cuando consulto el libro "segundo"
    Entonces la consulta de "segundo" devuelve sus datos

    # --- Baja de los dos libros: valida 200, esquema y tiempo (puntos a, b, c) ---
    Cuando elimino el libro "primero"
    Entonces la baja de "primero" fue correcta
    Cuando elimino el libro "segundo"
    Entonces la baja de "segundo" fue correcta

    # --- Verificación final: los libros ya no existen (punto d) ---
    Cuando consulto el libro "primero"
    Entonces la consulta de "primero" indica que el libro ya no existe
    Cuando consulto el libro "segundo"
    Entonces la consulta de "segundo" indica que el libro ya no existe
