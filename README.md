# Sistema de Reservas de Aulas

Aplicación web para gestionar aulas, cursos, usuarios y reservas, con inicio de sesión y registro de cuentas.

**Stack:** Java 17 · Spring Boot 3.5 · Spring MVC + Thymeleaf · Spring Data JPA · Spring Security · H2 / MySQL 8

## Ejecución

Requisito: Java 17. **No hace falta instalar ninguna base de datos.**

1. Ejecute `./mvnw spring-boot:run` (o ejecute `ReservasAulasApplication` desde IntelliJ).
2. Espere el mensaje `Started ReservasAulasApplication in ... seconds`.
3. Abra http://localhost:8080 — le llevará al inicio de sesión.

Flujo: **Login → Menú principal**. Si no tiene cuenta, el botón **Crear cuenta nueva** del login lleva al registro,
que crea la cuenta, inicia la sesión y entra directamente al menú.

### Base de datos SQL

| Modo | Cómo activarlo | Datos |
|---|---|---|
| **H2 en archivo** (por defecto) | No hace nada: ya viene configurado | Se **conservan** entre ejecuciones (carpeta `./data`) |
| **MySQL** | Perfil `mysql`: en IntelliJ, *Run > Edit Configurations > Active profiles = mysql*; por consola `-Dspring-boot.run.profiles=mysql` | Se conservan en MySQL |

Para MySQL: enciéndalo antes (por ejemplo desde XAMPP). La base `reservas_aulas` y las tablas se crean solas.
Si `root` tiene contraseña, defina la variable de entorno `DB_PASSWORD`.
El script `database/schema.sql` contiene el esquema MySQL equivalente por si se prefiere crearlo manualmente.

La aplicación ya no carga datos de ejemplo. Si en una ejecución anterior se guardaron datos en H2 y quiere empezar de cero, detenga la aplicación y borre la carpeta `data`.

## Si la aplicación no arranca

| Síntoma en la consola | Causa | Solución |
|---|---|---|
| `Communications link failure` / `Connection refused` | Está usando el perfil `mysql` y MySQL no está encendido | Encienda MySQL, o quite el perfil `mysql` para usar H2 |
| `Access denied for user 'root'` | MySQL tiene contraseña | Defina la variable `DB_PASSWORD` |
| `Port 8080 was already in use` | Otro programa usa el puerto | Cierre ese programa o cambie `server.port` en `application.properties` |
| `Database may be already in use` | Hay otra copia de la aplicación abierta | Detenga la otra ejecución (botón rojo de IntelliJ) |

## Inicio de sesión y registro

| Ruta         | Descripción                                                                    |
|--------------|--------------------------------------------------------------------------------|
| `/login`     | Ingreso con correo y contraseña; botón **Crear cuenta nueva**; al entrar lleva al menú |
| `/registro`  | Crea una cuenta nueva, **o** asigna contraseña a un usuario existente sin ella, e **inicia la sesión automáticamente** |
| `/logout`    | Cierra la sesión (botón en el menú)                                            |

- Las contraseñas se guardan con **BCrypt** en la columna `usuarios.password`; nunca en texto plano.
- Política: mínimo 8 caracteres, al menos una letra y un número.
- La aplicación **arranca sin datos**: no hay aulas, cursos, usuarios ni reservas precargados. Todo se ingresa manualmente (Postman o formularios web).
- Un usuario creado por la API (`POST /api/usuarios`) no tiene contraseña: la crea desde `/registro` usando ese mismo correo.
- La API REST (`/api/**`) usa HTTP Basic con el mismo correo y contraseña (o sin credenciales si `api.publica=true`; ver sección **API REST**).

## API REST (servicios web)

CRUD en JSON de cuatro recursos, sobre la persistencia JPA de la Semana 13 (entidades y repositorios sin cambios).

| Recurso | Base | Controlador |
|---|---|---|
| Aulas | `/api/aulas` | `AulaController` |
| Cursos | `/api/cursos` | `CursoController` |
| Usuarios | `/api/usuarios` | `UsuarioController` |
| Reservas | `/api/reservas` | `ReservaController` |

### Arquitectura (separación de responsabilidades)

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Controller | `Controller` | Solo endpoints y respuestas HTTP (`ResponseEntity`). No contiene lógica |
| Service | `Service` | Operaciones del negocio y validaciones; lanza excepciones propias |
| Modelo de entrada | `Dto` (`*Entrada`) | Datos recibidos en POST/PUT, **sin identificador** (con `@Valid`) |
| Modelo de salida | `Dto` (`*Salida`, `ErrorRespuesta`) | Recurso devuelto, **con identificador** |
| Excepciones | `Excepcion` | `RecursoNoEncontradoException` (404) y `SolicitudInvalidaException` (400) |
| Errores HTTP | `Controller/ApiExceptionHandler` | Traduce excepciones a 400, 404 y 500 en JSON |
| Persistencia | `Model` (entidades), `Repository` | Sin cambios |

Las entidades JPA nunca se exponen: la API recibe `*Entrada` y devuelve `*Salida`. La contraseña de `Usuario` no existe en ningún modelo de la API.

### Acceso: público o con contraseña
Se controla con `api.publica` en `application.properties`:

- `api.publica=true` (por defecto): se prueba en Postman **sin cuenta ni contraseña** (Authorization = No Auth).
- `api.publica=false`: toda la API exige HTTP Basic (correo y contraseña de una cuenta creada en `/registro`).

Use `true` solo en local.

### Endpoints y códigos HTTP

Los cuatro recursos tienen los mismos cinco endpoints:

| Método | Endpoint | Descripción | Éxito | Errores |
|---|---|---|---|---|
| GET | `/api/{recurso}` | Consultar todos | **200** | 500 |
| GET | `/api/{recurso}/{id}` | Consultar por ID | **200** | 400 (id no numérico), 404 |
| POST | `/api/{recurso}` | Crear | **201** + cabecera `Location` | 400 |
| PUT | `/api/{recurso}/{id}` | Actualizar | **200** | 400, 404 |
| DELETE | `/api/{recurso}/{id}` | Eliminar | **204** (sin contenido) | 400 (tiene reservas), 404 |

Reservas añade: `GET /api/reservas/fecha/{AAAA-MM-DD}` y `GET /api/reservas/aula/{aulaId}`.

| Código | Cuándo ocurre |
|---|---|
| 200 | Consulta o actualización exitosa |
| 201 | Recurso creado; `Location` apunta al nuevo recurso (p. ej. `http://localhost:8080/api/aulas/3`) |
| 204 | Eliminación exitosa, sin cuerpo |
| 400 | Campos inválidos, JSON mal formado, id o fecha con formato incorrecto, correo repetido, aula ocupada, o eliminar un registro con reservas |
| 404 | No existe un registro con ese id |
| 500 | Error interno no previsto: `{"estado":500,"mensaje":"Error interno del servidor"}` (el detalle queda solo en el log) |

### Modelos de entrada (POST y PUT, sin `id`)

| Recurso | Campos | Reglas |
|---|---|---|
| Aula | `nombre`, `capacidad`, `ubicacion`, `tipo`, `disponible` | nombre máx. 50; capacidad 1–500; ubicación máx. 100; tipo obligatorio; disponible opcional (por defecto `true`) |
| Curso | `nombre`, `horas` | nombre obligatorio, máx. 100; horas ≥ 1 |
| Usuario | `nombre`, `correo`, `rol` | correo válido y no repetido; rol = `DOCENTE`, `ESTUDIANTE` o `ADMINISTRATIVO` |
| Reserva | `aulaId`, `usuarioId`, `cursoId`, `fecha`, `horaInicio`, `horaFin` | todos obligatorios; fecha `AAAA-MM-DD` desde hoy; horas `HH:mm`; fin posterior al inicio; aula, usuario y curso deben existir; aula libre en ese horario |

Si se envía `id` en el cuerpo, se ignora: lo genera la base de datos.

### Modelos de salida (con `id`)

**201 — POST `/api/aulas`** (cabecera `Location: http://localhost:8080/api/aulas/1`)
```json
{ "id": 1, "nombre": "F101", "capacidad": 35, "ubicacion": "Edificio F", "tipo": "Laboratorio", "disponible": true }
```

**200 — GET `/api/reservas/1`**
```json
{
  "id": 1,
  "aulaId": 1, "aulaNombre": "F101",
  "usuarioId": 1, "usuarioNombre": "Sofia Herrera",
  "cursoId": 1, "cursoNombre": "Arquitectura de Software",
  "fecha": "2026-12-15", "horaInicio": "08:00:00", "horaFin": "10:00:00",
  "estado": "CONFIRMADA"
}
```

**204 — DELETE `/api/aulas/1`:** sin cuerpo.

**400 — datos inválidos**
```json
{
  "estado": 400,
  "mensaje": "Los datos enviados no son válidos",
  "errores": { "nombre": "El nombre del aula es obligatorio", "capacidad": "La capacidad debe ser al menos 1" }
}
```

**404 — recurso inexistente**
```json
{ "estado": 404, "mensaje": "No existe un aula con id 999999" }
```

**500 — error interno**
```json
{ "estado": 500, "mensaje": "Error interno del servidor" }
```
El 500 se comprueba con la prueba automática `ErrorInternoApiTests` (simula un fallo del servicio); no hay forma natural de provocarlo desde Postman con datos normales.

### Ver en localhost los cambios hechos desde Postman
La API y las páginas web usan **la misma base de datos**; la aplicación arranca vacía.

1. Inicie la aplicación con la base vacía (borre la carpeta `data` si ya existía).
2. En Postman: `POST /api/usuarios` con un usuario (queda sin contraseña).
3. En el navegador: http://localhost:8080/registro con **ese mismo correo** y una contraseña. Se activa la cuenta y entra al menú.
4. Deje abiertas **/aulas**, **/cursos**, **/usuarios** y **/reservas**; haga las peticiones en Postman y refresque (F5).
5. También puede ver el JSON en http://localhost:8080/api/aulas

Las listas web muestran 5 registros por página. Use el `id` que devuelva cada POST. Para una reserva deben existir antes un aula, un usuario y un curso.

### Pruebas en Postman
Cree cada petición a mano (método + URL; en POST y PUT: **Body → raw → JSON**) o, como opción, importe `postman/Reservas-API.postman_collection.json`.
Ciclo de sustentación: GET todos (200) → POST (201, revisar la cabecera **Location** en la pestaña *Headers* de la respuesta) → GET por id (200) → PUT (200) → DELETE (204) → GET (404) → errores 400.

```bash
curl -i http://localhost:8080/api/aulas
curl -i -X POST http://localhost:8080/api/cursos -H "Content-Type: application/json" -d '{"nombre":"Arquitectura de Software","horas":48}'
```

**Evidencias:** agregue aquí las capturas de Postman de cada paso, por ejemplo en `docs/evidencias/`.

## Estructura del proyecto

```
src/main/java/Sistema/Reservas/Aulas
├── Aspect/        Registro (log) de la ejecución de los servicios
├── Controller/    Controladores web (Thymeleaf) y REST (/api); ModeloGlobalAdvice da tema y usuario activo a todas las vistas
├── Model/         Entidades JPA y formularios (CursoForm, ReservaForm, RegistroForm)
├── Repository/    Acceso a datos (Spring Data JPA)
├── Security/      Configuración de seguridad, login, registro y manejo de la HttpSession (entrada y salida)
├── Service/       Lógica de negocio
├── Util/          Utilidades (paginación, sesión)
├── Validation/    Validaciones propias reutilizables (@ContrasenaSegura, @PasswordsCoinciden)
└── ReservasAulasApplication.java

src/main/resources
├── templates/     Vistas Thymeleaf (login, registro, módulos) y fragments/componentes.html
├── static/css/    Hojas de estilo
└── application*.properties   Configuración (H2 en archivo por defecto; perfil mysql opcional)

database/schema.sql          Esquema SQL de MySQL
```

Flujo de capas: `Controller → Service → Repository → Base de datos SQL`.
`Security` agrupa lo relacionado con acceso; queda fuera de `Service` a propósito, porque `LogAspect` registra los
argumentos de los servicios y no deben aparecer contraseñas en el log.

## Pruebas

`./mvnw test` — usan H2 en memoria (`src/test/resources/application.properties`).

## Sesión, cookies y base de datos

Tres mecanismos distintos para guardar información. Cada uno responde a una pregunta diferente:

| | Sesión (`HttpSession`) | Cookie | Base de datos |
|---|---|---|---|
| **Dónde vive** | En el **servidor** (memoria) | En el **navegador** del usuario | En el **servidor** (MySQL), en disco |
| **Cuánto dura** | Hasta cerrar sesión o 30 min sin actividad | Lo que se defina (la de tema: 1 año) | Hasta que se borre |
| **Sobrevive a reiniciar el servidor** | No | Sí (está en el navegador) | Sí |
| **Quién puede alterarla** | Solo el servidor | El usuario puede editarla → **no es confiable** | Solo el servidor |
| **Sirve para** | "Quién está conectado ahora" | Preferencias simples de ese navegador | Datos del negocio: usuarios, aulas, reservas |
| **En este proyecto** | Nombre del usuario activo (`usuarioNombre`) | Tema visual (`tema`) y el id de sesión (`JSESSIONID`) | `usuarios`, `aulas`, `cursos`, `reservas` |

Cómo se conectan: la sesión **usa** una cookie. El servidor guarda los datos de la sesión y entrega al navegador solo
la cookie `JSESSIONID` con un identificador; en cada petición el navegador la devuelve y el servidor encuentra su sesión.

### Sesión (HttpSession)
- **Ingreso:** `LoginSuccessHandler` guarda el nombre con `session.setAttribute("usuarioNombre", ...)`.
- **Lectura:** `ModeloGlobalAdvice` lo recupera con `session.getAttribute(...)` y el menú lo muestra en todas las páginas.
- **Salida:** `CerrarSesionHandler` quita el atributo y ejecuta `session.invalidate()`; además se borra la cookie `JSESSIONID`.
- La sesión caduca tras 30 minutos sin actividad (`server.servlet.session.timeout`).

### Cookie de preferencias (tema visual)
- El botón **Tema oscuro / Tema claro** del menú envía `POST /preferencias/tema`.
- `PreferenciasController` → `PreferenciasService` crea la cookie `tema` (`HttpOnly`, `SameSite=Lax`, 1 año, `Secure` si hay HTTPS).
- En cada petición `ModeloGlobalAdvice` lee la cookie (`@CookieValue`) y, si vale `oscuro`, las páginas cargan `tema-oscuro.css`.
- La cookie **no se borra al cerrar sesión**: es una preferencia del navegador, no del acceso.
- Solo se aceptan los valores `claro` y `oscuro`; cualquier otro se descarta.

### Páginas de error
`templates/error/404.html` (página no encontrada), `templates/error/500.html` (error interno) y `templates/error.html`
(400, 403, 405 y demás). Todas muestran un mensaje claro, sin detalles técnicos, y el botón **Volver a la aplicación**.

