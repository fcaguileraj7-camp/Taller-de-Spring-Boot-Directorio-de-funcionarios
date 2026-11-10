# Directorio de funcionarios

Avance académico del taller: API REST con Java 21, Spring Boot 4.1.2, Maven,
Spring Data JPA y MySQL. La interfaz HTML, CSS y JavaScript queda pendiente.

## Referencias revisadas

- PDF `Taller_Spring_Boot_Directorio_de_Funcionarios-PORT-CAMPUS-CR5.pdf`
  (dos páginas, disponible en Downloads).
- ZIP `SpringBoot.zip`: ejemplos `demoproductosrest`, `ejemmetodosrest` y
  configuración de `demoarticulosjpa`. El ZIP tiene un archivo de error en lugar
  del código Java de `demoarticulosjpa`; se revisó su POM y su estructura disponible.

Se mantiene el estilo sencillo de los ejemplos: controlador REST, servicio y
entidad. Se sustituye el almacenamiento en memoria del ejemplo REST por un
repositorio JPA. Se usa inyección por constructor y getters/setters explícitos
para que el funcionamiento sea visible, sin añadir Lombok, DTO ni interfaces
de servicio innecesarias para este avance.

## Preparación y ejecución

1. Instalar un JDK 21 y configurar `JAVA_HOME` con su ruta. Comprobar `java -version`
   y `./mvnw.cmd -version` (en Linux/macOS, `sh mvnw -version`). Maven Wrapper
   descarga Maven si es necesario; la primera ejecución necesita internet.
2. Tener MySQL 8 o posterior disponible y ejecutar `database/crear-base.sql`
   desde un cliente MySQL con permisos de creación de bases. El script no crea
   usuarios ni guarda contraseñas. Se debe disponer de un usuario con permisos
   sobre `directorio_funcionarios`, incluidos los necesarios para crear la tabla.
3. Configurar las variables de entorno en la misma terminal de PowerShell:

   ```powershell
   $env:DB_URL = 'jdbc:mysql://localhost:3306/directorio_funcionarios'
   $env:DB_USERNAME = Read-Host 'Usuario MySQL'
   $claveMysql = Read-Host 'Contraseña MySQL' -AsSecureString
   $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $claveMysql).Password
   ./mvnw.cmd spring-boot:run
   ```

   `DB_URL` es opcional si se usa la URL indicada. No se deben escribir credenciales
   en archivos versionados. Spring Boot no carga archivos `.env` automáticamente.
4. La API queda en `http://localhost:8080/api/v1/funcionarios`.

Hibernate crea o actualiza la tabla `funcionarios` mediante `ddl-auto=update`.
Esta configuración es suficiente para el taller y conserva los registros al
reiniciar; la conservación con MySQL todavía debe comprobarse en este entorno.
No hay scripts de carga, registros iniciales ni configuración de seguridad/CORS.

## Operaciones implementadas

| Método | Ruta (base `/api/v1/funcionarios`) | Respuesta |
| --- | --- | --- |
| GET | ruta base | 200 con lista; 204 sin cuerpo si está vacía |
| GET | `/{id}` | 200 con funcionario; 404 si no existe |
| POST | ruta base | 201 con funcionario e ID generado |
| PUT | `/{id}` | 200 actualizado; 404 si no existe |
| DELETE | `/{id}` | 204 sin cuerpo; 404 si no existe |
| GET | `?nombre=ana` | Búsqueda parcial sin distinguir mayúsculas: 200 o 204 |
| GET | `?departamento=Contabilidad` | Filtro exacto sin distinguir mayúsculas: 200 o 204 |

El cuerpo de POST y PUT es JSON:

```json
{
  "nombre": "Nombre del funcionario",
  "cargo": "Cargo del funcionario",
  "departamento": "Área de trabajo",
  "telefono": "Texto opcional"
}
```

Este ejemplo no se inserta automáticamente. `nombre`, `cargo` y `departamento`
son obligatorios: si faltan, son nulos, están vacíos o contienen solo espacios,
se devuelve 400. Se valida en el servicio con `isBlank()` y
`ResponseStatusException`, sin añadir dependencias de validación.

En POST, un `id` no nulo produce 400; puede omitirse o ser nulo. En PUT, se usa
siempre el ID de la ruta y se ignora el ID del cuerpo. `telefono` se guarda como
texto y puede omitirse, ser nulo o quedar vacío.

Los filtros vacíos o con solo espacios se ignoran. Si ambos tienen valor, se
aplica únicamente el nombre. Se quitan espacios en los extremos de los filtros.

## Pruebas y resultado real de esta sesión

```powershell
./mvnw.cmd test
```

Se ejecutaron **16 pruebas, 0 fallos, 0 errores y 0 omitidas** mediante JUnit,
MockMvc y H2 en memoria con modo MySQL. Recorren el controlador, el servicio y
el repositorio JPA real, sin simular el repositorio:

- CRUD, generación de ID, conservación del ID de la ruta y teléfono opcional/vacío.
- 200, 201, 204 sin cuerpo, 404 y 400 para solicitudes inválidas.
- Campos obligatorios ausentes, nulos, vacíos o con espacios, tanto en POST como PUT.
- Rechazo del ID al crear, sin sobrescribir el registro existente.
- Nombre parcial, departamento exacto, diferencias entre mayúsculas/minúsculas,
  prioridad del nombre, filtros vacíos y búsquedas sin coincidencias.
- JSON mal formado e ID de ruta no numérico.

H2 tiene alcance Maven `test` y su configuración está solo en
`src/test/resources/application-test.properties`. Los datos de ejemplo de las
pruebas se revierten mediante transacciones; no se insertan en MySQL.

Limitaciones comprobadas:

- El Java del PATH es 17. Se usó el JDK 25.0.5 incluido en la extensión Java de
  VS Code para compilar con `--release 21` y ejecutar las pruebas. El POM sigue
  configurado para Java 21. **Falta repetir las pruebas usando un JDK 21**.
- El cliente MySQL alcanzó el servidor local, pero la consulta de comprobación
  sin contraseña con el usuario de prueba `directorio` devolvió
  `ERROR 1045: Access denied`. No se proporcionaron credenciales válidas.
  No se creó la base ni se ejecutó el CRUD contra MySQL.
- H2 permite comprobar el flujo Java/JPA; no sustituye la comprobación del
  esquema, la conexión, las reglas de comparación y la persistencia en MySQL.

## Pendiente para la siguiente sesión

1. Ejecutar con JDK 21 y credenciales locales de MySQL; crear la base y comprobar
   las operaciones desde un cliente HTTP. Registrar un funcionario, reiniciar
   la aplicación y comprobar que sigue disponible. Eliminar después el registro
   utilizado para esta comprobación manual si corresponde.
2. Crear la interfaz vanilla: tabla, formulario de alta/edición, limpieza del
   formulario, consulta por ID, búsquedas y vuelta al listado completo.
3. Añadir confirmación antes de eliminar y mensajes de éxito, vacío y error;
   conectar la interfaz a la API y comprobar el recorrido desde el navegador.

## Conceptos para explicar el código

- Maven: POM, dependencias, scopes `runtime`/`test` y Maven Wrapper.
- `@SpringBootApplication`, auto-configuración y escaneo de componentes.
- MVC y responsabilidades de `@RestController`, `@Service` y repositorio.
- Inyección de dependencias por constructor.
- JPA: entidad, tabla, clave primaria, `IDENTITY` y campos obligatorios/opcionales.
- `JpaRepository`, `findById`, `save`, `delete` y consultas derivadas por nombre.
- HTTP, JSON, `@RequestBody`, `@PathVariable`, `@RequestParam` y `ResponseEntity`.
- Validación de negocio, `ResponseStatusException` y códigos 400/404.
- Transacciones, actualización de entidades y rollback de datos de prueba.
- Perfiles de Spring y diferencias entre pruebas en H2 y ejecución con MySQL.

El avance se conserva en el repositorio local. No se ha realizado push ni creado
pull requests.
