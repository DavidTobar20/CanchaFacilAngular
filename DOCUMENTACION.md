# CanchaFacil — Documentación del proyecto

Aplicación web de **reservas de espacios deportivos**, construida con Spring Boot 4.1.1 + Thymeleaf,
siguiendo el patrón de **arquitectura por capas** visto en la guía de clase
(*Javeriana – Desarrollo Web – Clase 9 – Parte 1 Springboot*).

- **Fecha de la intervención:** 24 de agosto de 2026
- **Java del proyecto:** 17 (el JDK instalado en el equipo es el 24, no hay problema)
- **Arranque:** `./mvnw spring-boot:run` → http://localhost:8080

---

## 1. Punto de partida: qué estaba mal

El proyecto **no compilaba**. Estos eran los problemas, en orden de gravedad:

### 1.1 El `pom.xml` no tenía las dependencias necesarias

Solo tenía `spring-boot-starter` (el núcleo, sin servidor web). Faltaban:

| Dependencia | Para qué |
|---|---|
| `spring-boot-starter-web` | Controladores, peticiones HTTP y el servidor Tomcat embebido |
| `spring-boot-starter-thymeleaf` | El motor de plantillas que convierte los HTML en páginas con datos |
| `lombok` | Las entidades ya usaban `@Data` y `@AllArgsConstructor`, pero la librería no estaba declarada |

Sin esto, **ninguna** clase que importara `lombok.*` o `org.springframework.web.*` podía compilar.

### 1.2 Lombok no generaba nada (el error más difícil de ver)

Aun después de agregar la dependencia, el compilador seguía diciendo
`cannot find symbol: method getId()`.

**Causa:** Lombok no es una librería normal, es un **procesador de anotaciones**: se ejecuta
*durante* la compilación y escribe los getters, setters y constructores por nosotros.
En versiones anteriores el `spring-boot-starter-parent` lo registraba automáticamente, pero
**el parent de Spring Boot 4.1 ya no lo hace**, y además los JDK modernos (23+) desactivaron
el procesamiento automático de anotaciones por seguridad.

**Solución:** registrarlo a mano en el `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

> **Si en el futuro Lombok "deja de funcionar" y todos los getters aparecen en rojo,
> el problema casi siempre es este bloque.**

### 1.3 Paquetes que no coincidían con la carpeta

`Espacio.java`, `Pago.java` y `Reserva.java` declaraban:

```java
package com.reservasdeportivas.model;   // ❌
```

...pero los archivos estaban físicamente en `src/main/java/com/example/demo/Entidades/`.
En Java **la ruta de carpetas y la línea `package` deben coincidir exactamente**.
Se corrigieron los tres a `package com.example.demo.Entidades;`.

### 1.4 `Calificacion.java` estaba vacío

El archivo existía pero tenía 0 bytes. Se escribió la entidad completa.

### 1.5 Errores de sintaxis e imports faltantes

| Archivo | Error | Corrección |
|---|---|---|
| `Usuario.java` | `private fechaRegistro LocalDateTime;` | Estaba invertido → `private LocalDateTime fechaRegistro;` |
| `Usuario.java` | Usaba `Rol` pero ese tipo no existía | Se creó el enum `Rol` |
| `Usuario.java` | Usaba `List` y `LocalDateTime` sin importarlos | Se agregaron los `import` |
| `Negocio.java` | Usaba `List` sin importarlo | Se agregó `import java.util.List;` |
| `Notificacion.java` | Usaba `LocalDateTime` sin importarlo | Se agregó `import java.time.LocalDateTime;` |

### 1.6 Identificadores inconsistentes

Unas entidades usaban `int id` y otras `UUID id`. Como las entidades se relacionan entre sí,
los tipos deben ser compatibles. **Se unificaron todos a `Long`**, que es el tipo estándar
para identificadores en Spring.

### 1.7 `@AllArgsConstructor` sin `@NoArgsConstructor`

Cuando una clase declara un constructor con parámetros, Java **deja de crear el constructor
vacío**. Thymeleaf y Spring necesitan ese constructor vacío para armar el objeto de un
formulario (`new Usuario()`), así que se agregó `@NoArgsConstructor` a todas las entidades.

### 1.8 Un `StackOverflowError` esperando a suceder

`@Data` genera `toString()`, `equals()` y `hashCode()` recorriendo **todos** los atributos.
Como las relaciones van en los dos sentidos:

```
Usuario.toString() → negocios → Negocio.toString() → administrador → Usuario.toString() → ...
```

...el programa se habría quedado en un bucle infinito al imprimir cualquier usuario.
Lo mismo pasaba entre `Reserva` y `Pago`.

**Solución:** excluir de `toString`/`equals` el extremo que cierra el ciclo:

```java
@Data
@ToString(exclude = {"negocios", "reservas", "calificaciones", "notificaciones"})
@EqualsAndHashCode(exclude = {"negocios", "reservas", "calificaciones", "notificaciones"})
public class Usuario { ... }
```

---

## 2. Arquitectura: las cuatro capas

La guía plantea que **cada parte debe tener una única responsabilidad**. El proyecto quedó así:

```
Navegador
   │  (petición HTTP: GET /usuarios)
   ▼
┌─────────────────────────────────────────────┐
│ CONTROLADOR   @Controller                   │  Recibe la petición, decide qué hacer
│ Controladores/UsuarioController.java        │  y qué vista devolver. NO tiene lógica.
└───────────────┬─────────────────────────────┘
                │  usuarioService.listar()
                ▼
┌─────────────────────────────────────────────┐
│ SERVICIO      @Service                      │  Reglas de negocio: validar, calcular,
│ Servicios/UsuarioService.java               │  decidir si algo se puede hacer.
└───────────────┬─────────────────────────────┘
                │  usuarioRepository.findAll()
                ▼
┌─────────────────────────────────────────────┐
│ REPOSITORIO   @Repository                   │  Acceso a los datos: buscar, guardar,
│ Repositorios/UsuarioRepositoryImpl.java     │  modificar, eliminar.
└───────────────┬─────────────────────────────┘
                │
                ▼
        HashMap en memoria
                │
                ▼  (el controlador pone los datos en el Model)
┌─────────────────────────────────────────────┐
│ VISTA         Thymeleaf                     │  templates/usuarios/lista.html
└─────────────────────────────────────────────┘
                │
                ▼
           HTML al navegador
```

**Por qué importa:** si mañana se cambia el `HashMap` por una base de datos real,
**solo se tocan las clases `*RepositoryImpl`**. El controlador y el servicio no se enteran.

### 2.1 Estructura de carpetas

```
src/main/java/com/example/demo/
├── DemoApplication.java          ← punto de arranque (@SpringBootApplication)
├── Entidades/                    ← los datos (Modelo)
├── Repositorios/                 ← acceso a datos
├── Servicios/                    ← reglas de negocio (interfaces)
│   └── Impl/                     ← implementaciones (*ServiceImpl, @Service)
├── DTO/                          ← objetos de transferencia (RegistroDTO, UsuarioDTO, proyecciones JPQL)
└── Controladores/                ← atienden al navegador

src/main/resources/
├── application.properties        ← configuración (puerto, caché)
├── static/css/estilos.css        ← CSS (lo sirve Spring directamente)
└── templates/                    ← HTML de Thymeleaf
    ├── index.html
    ├── fragmentos/cabecera.html  ← menú reutilizable
    ├── usuarios/{lista,formulario,detalle}.html
    ├── negocios/...
    ├── espacios/...
    ├── reservas/...
    ├── pagos/...
    ├── calificaciones/...
    └── notificaciones/...
```

> **`static/` vs `templates/`:** lo que está en `static/` el navegador lo pide tal cual
> (`/css/estilos.css`). Lo que está en `templates/` **nunca** se pide directamente:
> lo devuelve un controlador y Thymeleaf lo procesa antes de enviarlo.

---

## 3. Capa de Entidades

Siete entidades + un enum, todas POJOs con Lombok:

| Entidad | Descripción | Se relaciona con |
|---|---|---|
| `Usuario` | Persona registrada (cliente o administrador) | Negocios, Reservas, Calificaciones, Notificaciones |
| `Negocio` | Empresa dueña de las canchas | Un administrador (Usuario), muchos Espacios |
| `Espacio` | La cancha o espacio deportivo concreto | Un Negocio, muchas Reservas y Calificaciones |
| `Reserva` | Un usuario aparta un espacio en una fecha y hora | Usuario, Espacio, un Pago, una Calificación |
| `Pago` | El pago de una reserva | Una Reserva |
| `Calificacion` | Puntuación de 1 a 5 tras usar el espacio | Usuario, Espacio, Reserva |
| `Notificacion` | Aviso dirigido a un usuario | Un Usuario |
| `Rol` | *enum*: `CLIENTE` o `ADMINISTRADOR` | — |

### Anotaciones de Lombok usadas

```java
@Data                 // getters, setters, toString, equals y hashCode
@NoArgsConstructor    // constructor vacío  → lo necesitan Thymeleaf y Spring
@AllArgsConstructor   // constructor con todos los atributos
@ToString(exclude=…)  // evita el bucle infinito descrito en 1.8
```

### Un detalle importante en `Reserva`

Los formularios envían la fecha como texto (`"2026-09-01"`) y la hora como `"18:00"`.
Para que Spring las convierta a `LocalDate` y `LocalTime` se agregó:

```java
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
private LocalDate fecha;

@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
private LocalTime horaInicio;
```

Sin esas anotaciones el formulario de reservas fallaría al enviarse.

---

## 4. Capa de Repositorios — interfaz + implementación

Esta capa aplica directamente el concepto de la guía: **la interfaz es un contrato**.

| | Interfaz | Clase |
|---|---|---|
| Qué hace | Dice **qué** métodos deben existir | Escribe **cómo** se hacen |
| Palabra clave | `interface` | `class ... implements` |
| Ejemplo | `UsuarioRepository.java` | `UsuarioRepositoryImpl.java` |

Hay un contrato común del que heredan todos:

```java
public interface CrudRepository<T> {
    List<T> findAll();
    T       findById(Long id);
    T       save(T entidad);
    void    deleteById(Long id);
    boolean existsById(Long id);
}
```

Y cada entidad añade sus propias búsquedas:

```java
public interface UsuarioRepository extends CrudRepository<Usuario> {
    Usuario       findByEmail(String email);
    List<Usuario> findByRol(Rol rol);
    List<Usuario> findByNombre(String nombre);
}
```

La implementación guarda todo en un `LinkedHashMap` y genera los ids con un contador:

```java
@Repository
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final Map<Long, Usuario> datos = new LinkedHashMap<>();
    private final AtomicLong secuencia = new AtomicLong(0);

    @Override
    public Usuario save(Usuario usuario) {
        if (usuario.getId() == null) {              // es nuevo → id automático
            usuario.setId(secuencia.incrementAndGet());
        }
        datos.put(usuario.getId(), usuario);        // si ya existía, lo reemplaza
        return usuario;
    }
    ...
}
```

> ⚠️ **Los datos viven en memoria.** Al apagar la aplicación se pierden.
> Es exactamente lo que plantea la guía: *"primero se simula con HashMap; después puede
> conectarse a una BD"*. Ver la sección 9 para el siguiente paso.

---

## 5. Capa de Servicios — las reglas del negocio

Aquí es donde se decide **si una operación se puede realizar**. Reglas implementadas:

### `UsuarioService`
- El **email no se puede repetir**.
- La `fechaRegistro` se pone sola al crear, y se conserva al editar.

### `NegocioService`
- El **NIT es único**.
- Todo negocio **debe tener un administrador que exista**.

### `EspacioService`
- El **precio por hora no puede ser negativo**.
- Todo espacio **pertenece a un negocio existente**.

### `ReservaService`
- La **hora de inicio debe ser anterior a la hora de fin**.
- **Un espacio no puede tener dos reservas activas que se crucen** en la misma fecha.
  Ésta es la regla más interesante; así se detecta el cruce:

  ```java
  otra.getHoraInicio().isBefore(reserva.getHoraFin())
      && otra.getHoraFin().isAfter(reserva.getHoraInicio())
  ```

  Las reservas `CANCELADA` se ignoran, y al editar se ignora la propia reserva.
- Toda reserva nace en estado `PENDIENTE`.

### `PagoService`
- El **monto no puede ser negativo**.
- **Una reserva solo puede tener un pago**.
- Al **aprobar** un pago, la reserva pasa automáticamente a `CONFIRMADA`.
  Al **rechazarlo o reembolsarlo**, la reserva pasa a `CANCELADA`.

### `CalificacionService`
- La **puntuación debe estar entre 1 y 5**.
- **Cada reserva se califica una sola vez**.
- El usuario y el espacio **se toman de la reserva**, no se piden aparte
  (así es imposible calificar una cancha en la que nunca se estuvo).
- Calcula el **promedio** de un espacio.

### `NotificacionService`
- Toda notificación nace **no leída** y con la fecha del momento.
- Permite **marcarla como leída**.

### Inyección de dependencias

Ningún servicio crea sus dependencias con `new`. Se las entrega Spring por constructor:

```java
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Autowired                                   // "Spring, dame este objeto"
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }
}
```

Spring ve que `UsuarioService` necesita un `UsuarioRepository`, busca en su contenedor
un **bean** que cumpla ese contrato, encuentra `UsuarioRepositoryImpl` (marcado con
`@Repository`) y se lo entrega. Eso es la **inyección de dependencias**.

---

## 6. Capa de Controladores

Se crearon **8 controladores**: uno por cada entidad, más el de la página de inicio.

Todos siguen el mismo patrón de la guía:

```java
@Controller                          // "esta clase atiende al navegador"
@RequestMapping("/usuarios")         // todas las rutas empiezan por /usuarios
public class UsuarioController {

    @GetMapping                      // GET /usuarios
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return "usuarios/lista";     // → templates/usuarios/lista.html
    }

    @GetMapping("/{id}")             // GET /usuarios/5
    public String detalle(@PathVariable("id") Long id, Model model) { ... }

    @GetMapping("/add")              // muestra el formulario vacío
    public String mostrarFormularioCrear(Model model) { ... }

    @PostMapping("/add")             // recibe el formulario enviado
    public String guardar(@ModelAttribute("usuario") Usuario usuario, Model model) { ... }

    @GetMapping("/update/{id}")      // formulario con los datos ya cargados
    @GetMapping("/delete/{id}")      // elimina
    @GetMapping(params = "nombre")   // GET /usuarios?nombre=Ana  → búsqueda
}
```

### El `Model` y cómo llegan los datos al HTML

```java
model.addAttribute("usuarios", usuarioService.listar());
return "usuarios/lista";
```

El `Model` es una **caja temporal**. El controlador mete la lista con el nombre `"usuarios"`,
y en el HTML Thymeleaf la saca por ese mismo nombre:

```html
<tr th:each="usuario : ${usuarios}">
    <td th:text="${usuario.nombre}">Juan</td>
</tr>
```

> El texto `Juan` que está escrito dentro del `<td>` es solo para poder abrir el archivo
> en el navegador sin servidor y ver cómo queda. Thymeleaf lo reemplaza en tiempo de ejecución.

### El patrón POST-Redirect-GET

Después de guardar, el controlador **no** devuelve un HTML: devuelve una redirección.

```java
return "redirect:/usuarios";
```

Así, si el usuario recarga la página, **no se vuelve a enviar el formulario** (evita duplicados).

### Manejo de errores

Cuando el servicio rechaza una operación, el controlador lo captura y vuelve a mostrar
el formulario con el mensaje, en lugar de reventar con una página de error:

```java
try {
    usuarioService.guardar(usuario);
} catch (IllegalArgumentException ex) {
    model.addAttribute("error", ex.getMessage());
    return "usuarios/formulario";
}
return "redirect:/usuarios";
```

Y en el HTML:

```html
<div class="error" th:if="${error}" th:text="${error}"></div>
```

---

## 7. Mapa completo de URLs

### Inicio
| URL | Qué hace |
|---|---|
| `GET /` | Panel con los totales de cada entidad |

### Usuarios
| URL | Qué hace |
|---|---|
| `GET /usuarios` | Lista todos |
| `GET /usuarios?nombre=Ana` | Busca por nombre |
| `GET /usuarios/{id}` | Detalle |
| `GET /usuarios/add` | Formulario de creación |
| `POST /usuarios/add` | Guarda (crea o actualiza) |
| `GET /usuarios/update/{id}` | Formulario de edición |
| `GET /usuarios/delete/{id}` | Elimina |

### Negocios
`GET /negocios` · `?nombre=` · `/{id}` · `/add` · `POST /add` · `/update/{id}` · `/delete/{id}`

### Espacios
`GET /espacios` · `?deporte=futbol` · `/{id}` *(incluye promedio de calificación)* · `/add` · `POST /add` · `/update/{id}` · `/delete/{id}`

### Reservas
| URL | Qué hace |
|---|---|
| `GET /reservas` | Lista todas |
| `GET /reservas?estado=CONFIRMADA` | Filtra por estado |
| `GET /reservas?usuarioId=2` | Reservas de un usuario |
| `GET /reservas/cancelar/{id}` | Cambia el estado a `CANCELADA` |
| + `/{id}`, `/add`, `POST /add`, `/update/{id}`, `/delete/{id}` | |

### Pagos
| URL | Qué hace |
|---|---|
| `GET /pagos?estado=APROBADO` | Filtra por estado |
| `GET /pagos/estado/{id}?valor=APROBADO` | Aprueba / rechaza / reembolsa |
| + `/`, `/{id}`, `/add`, `POST /add`, `/update/{id}`, `/delete/{id}` | |

### Calificaciones
`GET /calificaciones` · `?espacioId=1` · `/{id}` · `/add` · `POST /add` · `/update/{id}` · `/delete/{id}`

### Notificaciones
| URL | Qué hace |
|---|---|
| `GET /notificaciones?usuarioId=2` | Notificaciones de un usuario |
| `GET /notificaciones/no-leidas/{usuarioId}` | Solo las pendientes |
| `GET /notificaciones/leer/{id}` | Marca como leída |
| + `/`, `/{id}`, `/add`, `POST /add`, `/update/{id}`, `/delete/{id}` | |

---

## 8. Las vistas (Thymeleaf)

Cada entidad tiene tres plantillas:

- **`lista.html`** — tabla con todos los registros + buscador + botones Ver/Editar/Eliminar
- **`formulario.html`** — sirve para crear **y** para editar (el `id` viaja en un campo oculto)
- **`detalle.html`** — ficha completa de un registro

### El menú reutilizable

En vez de repetir la barra de navegación en 22 archivos, se definió un **fragmento**:

```html
<!-- templates/fragmentos/cabecera.html -->
<header th:fragment="cabecera">
    <nav>
        <a th:href="@{/usuarios}">Usuarios</a>
        ...
    </nav>
</header>
```

Y en cada página basta una línea:

```html
<header th:replace="~{fragmentos/cabecera :: cabecera}"></header>
```

### Sintaxis de Thymeleaf usada

| Expresión | Para qué |
|---|---|
| `${...}` | Lee un dato del `Model` |
| `*{...}` | Lee un campo del objeto del formulario (`th:object`) |
| `@{...}` | Construye una URL (respeta la ruta base de la app) |
| `th:each` | Recorre una lista |
| `th:text` | Escribe texto dentro de la etiqueta |
| `th:if` / `th:unless` | Muestra u oculta según una condición |
| `th:field` | Conecta un `<input>` con un atributo del objeto |
| `th:selected` | Marca la opción elegida en un `<select>` |
| `th:replace` | Inserta un fragmento |

---

## 9. Cómo ejecutar el proyecto

```bash
./mvnw spring-boot:run
```

Luego abrir **http://localhost:8080**.

### Orden recomendado para probar

1. Crear un **usuario** con rol `ADMINISTRADOR`.
2. Crear un **negocio** y asignarle ese administrador.
3. Agregar un **espacio** a ese negocio.
4. Crear una **reserva** (usuario + espacio + fecha + horas).
5. Registrar el **pago** de esa reserva y **aprobarlo** → la reserva pasa a `CONFIRMADA`.
6. **Calificar** la reserva → aparece el promedio en el detalle del espacio.

### Si sale `Port 8080 was already in use`

Quedó un proceso Java colgado de una ejecución anterior. En PowerShell:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

O cambiar el puerto en `src/main/resources/application.properties`:

```properties
server.port=8090
```

### Advertencias de Lombok al compilar

```
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by lombok.permit.Permit
```

**Son inofensivas.** Aparecen porque Lombok habla con el JDK 24 instalado en el equipo,
aunque el proyecto compile para Java 17. No rompen nada.

---

## 10. Verificación realizada

El build completo pasa:

```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Y con la aplicación corriendo se probó cada flujo con peticiones reales:

| Prueba | Resultado |
|---|---|
| Las 7 listas y los 7 formularios cargan | `200 OK` |
| Crear usuario, negocio, espacio, reserva, pago, calificación, notificación | `302` (redirección correcta) |
| Email duplicado | `Ya existe un usuario con el email ana@mail.com` |
| Reserva que se cruza con otra | `El espacio ya esta reservado en ese horario` |
| Hora de fin antes que la de inicio | `La hora de inicio debe ser anterior a la hora de fin` |
| Dos pagos para la misma reserva | `La reserva 1 ya tiene un pago registrado` |
| Aprobar el pago | La reserva pasó a `CONFIRMADA` automáticamente |
| Calificación con puntuación 9 | `La puntuacion debe estar entre 1 y 5` |
| Promedio del espacio tras calificar con 5 | `5,0 / 5` |
| Todos los filtros (`?nombre=`, `?deporte=`, `?estado=`, `?usuarioId=`) | Correctos |
| Editar un usuario existente | Guardó el cambio |
| Pedir un id que no existe (`/usuarios/999`) | Redirige a la lista, no revienta |

---

## 11. Cosas que hay que saber

1. **Los datos se pierden al apagar la aplicación.** Están en un `HashMap`, no en una base de datos.

2. **Al editar un usuario, el campo contraseña sale vacío.** Es a propósito: Spring nunca
   reenvía contraseñas al HTML. Como el campo es obligatorio, hay que volver a escribirla.

3. **Las contraseñas se guardan en texto plano.** Para un ejercicio de clase está bien;
   en un proyecto real habría que cifrarlas con Spring Security (`BCryptPasswordEncoder`).

4. **Los paquetes usan mayúscula inicial** (`Entidades`, `Servicios`...). La convención de Java
   es minúscula (`entidades`, `servicios`), pero se respetó el nombre que ya tenía el proyecto
   para no romper el trabajo de los compañeros en otras ramas.

---

## 12. Siguiente paso: conectar una base de datos

Gracias a las interfaces, el cambio es acotado. En resumen:

1. Agregar al `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-jpa</artifactId>
   </dependency>
   ```
   más el driver de la base de datos (H2, MySQL, PostgreSQL...).

2. Anotar las entidades con `@Entity`, `@Id`, `@GeneratedValue`, `@ManyToOne`, `@OneToMany`.

3. Cambiar las interfaces para que extiendan `JpaRepository<Usuario, Long>`
   y **borrar las clases `*RepositoryImpl`** — Spring Data las genera solo.

4. **Los servicios y los controladores no se tocan.** Ésa es exactamente la ventaja de
   haber separado el contrato (interfaz) de la implementación.

---

## Anexo — Inventario de archivos

**Modificados:** `pom.xml`, `application.properties`, y las 7 entidades.

**Creados:**

| Capa | Archivos |
|---|---|
| Entidades | `Rol.java` |
| Repositorios | `CrudRepository` + 7 interfaces + 7 implementaciones (15) |
| Servicios | 7 servicios |
| Controladores | 8 controladores |
| Vistas | `index.html`, `fragmentos/cabecera.html` + 21 plantillas (7 entidades × 3) |
| Estilos | `static/css/estilos.css` |

---

## 13. Actualización: requisitos de la rúbrica (login, validación, excepciones, consultas)

Esta sección describe lo que se agregó para cumplir los 10 requisitos de la entrega.
No se tocó `UsuarioRestController` ni la carpeta `frontend-angular`.

### 13.1 Cómo se cubre cada requisito

| # | Requisito | Dónde se evidencia |
|---|---|---|
| 1 | Proyecto Spring Boot funcionando con Thymeleaf | Todas las páginas en `templates/`, menú en `fragmentos/cabecera.html` |
| 2 | Arquitectura en capas | `Controladores/` → `Servicios/` (contratos) + `Servicios/Impl/` (implementaciones) → `Repositorios/` → `Entidades/` |
| 3 | Modelo con ≥3 entidades relacionadas | Usuario, Negocio, Espacio, Reserva, Pago, Calificacion, Notificacion (`@ManyToOne`, `@OneToMany`, `@OneToOne`) |
| 4 | CRUD de una entidad principal | Reservas (y las demás): listar `/reservas`, crear `/reservas/add`, consultar `/reservas/{id}`, editar `/reservas/update/{id}` |
| 5 | Relación usada realmente | Reserva usa Usuario y Espacio al guardarse; `/reservas/mis-reservas` consulta las reservas del usuario en sesión; Calificacion hereda usuario y espacio de la Reserva; Pago aprobado confirma la Reserva |
| 6 | Repositorios con `JpaRepository` | Los 7 repositorios extienden `JpaRepository` y los servicios usan `save()`, `findAll()`, `findById()` |
| 7 | Consulta personalizada | `@Query` JPQL en `ReservaRepository`, `EspacioRepository`, `CalificacionRepository`, `PagoRepository`, `NotificacionRepository` + consultas derivadas. Se ven en `/consultas` |
| 8 | Validación básica | Bean Validation (`@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`, `@DecimalMin`, `@Pattern`...) en entidades y `RegistroDTO`; `@Valid` + `BindingResult` en los controladores; errores bajo cada campo |
| 9 | Manejo de errores | `Servicios/` contiene `RecursoNoEncontradoException` y `ReglaNegocioException`; `Controladores/` contiene `ManejadorGlobalExcepciones` (`@ControllerAdvice`) + plantilla `error.html` |
| 10 | Datos iniciales | `Servicios/DatosIniciales.java` (`CommandLineRunner`) |

### 13.2 Capa de servicios: interfaz + implementación

Antes cada servicio era una clase. Ahora:

```
Servicios/
├── UsuarioService.java          ← interfaz (contrato)
├── Impl/
│   └── UsuarioServiceImpl.java  ← @Service con la lógica
├── DatosIniciales.java          ← carga inicial
└── ...                          ← servicios de autenticación y excepciones
```

Las interfaces quedan en `Servicios` y sus implementaciones en `Servicios/Impl`; los controladores
siguen dependiendo de las interfaces (`UsuarioService`, `ReservaService`, etc.).

Cada servicio tiene ahora dos formas de buscar por id:

- `buscarPorId(id)` → devuelve `null` si no existe (se conserva para el API REST).
- `obtenerPorId(id)` → lanza `RecursoNoEncontradoException` (la usan las páginas: muestra un 404).

### 13.3 Login y registro (Spring Security)

- `Controladores/SeguridadConfig.java`: reglas de acceso, formulario de login, logout y BCrypt.
- `Servicios/UsuarioDetallesService.java`: busca el usuario por email para el login.
- `Servicios/SesionActual.java`: bean `sesion` para saber quién inició sesión
  (en Thymeleaf: `${@sesion.autenticado}`, `${@sesion.admin}`, `${@sesion.nombre}`).
- `Controladores/AuthController.java` + `templates/auth/login.html` y `auth/registro.html`.
- `DTO/RegistroDTO.java`: datos del formulario de registro (incluye "confirmar contraseña").

Reglas de acceso:

| Ruta | Quién entra |
|---|---|
| `/login`, `/registro`, `/css/**`, `/error` | Todos |
| `/api/**` | Todos (sin CSRF) — para no romper Angular por ahora |
| `/usuarios/**` | Solo `ADMINISTRADOR` |
| Todo lo demás | Cualquier usuario con sesión |

Quien se registra entra siempre como `CLIENTE`. Las contraseñas se guardan cifradas con BCrypt;
al editar un usuario, si la contraseña se deja vacía se conserva la actual.

Si la base ya tenía usuarios con la contraseña en texto plano, al arrancar `DatosIniciales`
las cifra, así que esos usuarios pueden entrar con la misma clave de antes.

### 13.4 Validación

- Anotaciones en las entidades y en `RegistroDTO`.
- En cada `@PostMapping` se usa `@Valid @ModelAttribute ... BindingResult result`.
  Si `result.hasErrors()` se vuelve a mostrar el formulario.
- En las plantillas: `th:errors="*{campo}"` bajo cada campo, `th:errorclass` para pintar el
  borde en rojo y `#fields.globalErrors()` para errores generales (ej. "Debe seleccionar un negocio").
- Los formularios tienen `novalidate` para que se vea la validación del servidor.
- `messages.properties` traduce los errores de conversión (letras en un número, fecha inválida).
- Se desactivó la re-validación automática de Hibernate al guardar
  (`jakarta.persistence.validation.mode=none`): la validación se hace en el controlador.

### 13.5 Excepciones

| Excepción | Cuándo | Resultado |
|---|---|---|
| `RecursoNoEncontradoException` | `/espacios/999`, editar o borrar un id que no existe | Página de error 404 |
| `ReglaNegocioException` | Email repetido, horario cruzado, reserva ya calificada, fecha pasada… | En formularios: mensaje en rojo. Fuera de formularios: página 400 |
| `MethodArgumentTypeMismatchException` | `/reservas/abc` | Página 400 |
| `DataIntegrityViolationException` | Violación de llave foránea o campo único | Página 409 |
| Cualquier otra | Error inesperado | Página 500 (detalle en el log) |

`ManejadorGlobalExcepciones` solo aplica a los controladores Thymeleaf (`assignableTypes`),
así que el API REST sigue respondiendo igual que antes.

### 13.6 Consultas personalizadas (`@Query` / JPQL)

| Repositorio | Método | Qué hace | Dónde se usa |
|---|---|---|---|
| `ReservaRepository` | `buscarCruces(...)` | Reservas activas del mismo espacio y día que se cruzan en horario | Regla "no se puede reservar un horario ocupado" |
| `ReservaRepository` | `buscarPorEmailDeUsuario(email)` | Reservas del usuario en sesión (`join fetch`) | `/reservas/mis-reservas` |
| `ReservaRepository` | `contarPorEstado()` | `group by` estado | `/consultas` |
| `EspacioRepository` | `buscarPorRangoDePrecio(min, max)` | `between` sobre el precio | `/consultas` |
| `CalificacionRepository` | `promedioPorEspacio(id)` | `avg()` de puntuación | Detalle del espacio |
| `CalificacionRepository` | `rankingDeEspacios()` | `select new EspacioRankingDTO(...)` con `avg` y `count`; proyección en `DTO/` | `/consultas` |
| `PagoRepository` | `ingresosPorNegocio()` | `sum()` de pagos aprobados; proyección en `Repositorios/` | `/consultas` |
| `NotificacionRepository` | `findByUsuarioIdYNoLeidas(id)` | Notificaciones sin leer | `/notificaciones/no-leidas/{id}` |
| `UsuarioRepository` | `existsByEmailIgnoreCase(email)` | Consulta derivada | Registro |

### 13.7 Datos iniciales

Al arrancar, si la base está vacía se cargan 3 usuarios, 2 negocios, 4 espacios, 5 reservas
(pasadas y futuras, en distintos estados), 3 pagos aprobados, 2 calificaciones y 3 notificaciones.
Siempre se asegura que exista el administrador.

| Email | Contraseña | Rol |
|---|---|---|
| `admin@canchafacil.com` | `admin123` | ADMINISTRADOR |
| `laura@correo.com` | `cliente123` | CLIENTE |
| `carlos@correo.com` | `cliente123` | CLIENTE |

### 13.8 Otros cambios

- `Usuario.Direccion` se renombró a `direccion` (misma columna en la BD) para que coincida
  con el formulario y los mensajes de validación. El JSON del API no cambia.
- Mensajes de éxito ("Reserva guardada correctamente") con `RedirectAttributes`.
- Nueva página `/consultas` y enlace "Mis reservas" en el menú.
- `pom.xml`: se agregaron `spring-boot-starter-validation` y `spring-boot-starter-security`.

### 13.9 Guion de demostración

1. Abrir `http://localhost:8080` → redirige al login.
2. Registrarse con datos inválidos (email mal escrito, contraseñas distintas) → errores bajo cada campo.
3. Registrarse bien → login → entrar como cliente. "Usuarios" no aparece en el menú; `/usuarios` da 403.
4. Crear una reserva que se cruce con otra existente → mensaje de regla de negocio.
5. Ver "Mis reservas".
6. Abrir `/reservas/9999` → página 404 personalizada.
7. Entrar como `admin@canchafacil.com` → CRUD de usuarios, negocios y espacios.
8. Abrir `/consultas` → ranking, reservas por estado, ingresos y búsqueda por rango de precio.

---

## 14. Rediseño de la interfaz

Todas las vistas se rehicieron siguiendo el diseño de Figma (login dividido con estadio,
panel con barra lateral y portada con buscador).

### Estructura de plantillas

| Archivo | Qué contiene |
|---|---|
| `fragmentos/layout.html` | `head(titulo)`, `topbar`, `sidebar(activo)`, `footer`, `alertas`, `nav-publica(activo)`, `footer-publico` |
| `fragmentos/iconos.html` | Iconos SVG en línea: `icono('calendario')`, dibujo de cancha y `estrellas(n)` |
| `static/css/estilos.css` | Sistema de diseño: colores, botones, insignias, tarjetas, tablas, formularios, responsive |

Cada página de la app usa el mismo esqueleto:

```html
<head th:replace="~{fragmentos/layout :: head('Reservas')}"></head>
<body class="app">
<header th:replace="~{fragmentos/layout :: topbar}"></header>
<div class="app-cuerpo">
    <aside th:replace="~{fragmentos/layout :: sidebar('reservas')}"></aside>
    <main class="app-main"> ... </main>
</div>
<footer th:replace="~{fragmentos/layout :: footer}"></footer>
```

### Cambios de comportamiento

- La portada (`/`) ahora es pública y muestra las canchas reales con su calificación.
- Nueva página **Mi perfil** (`/perfil`, `PerfilController`): datos del usuario, estadísticas
  calculadas de sus reservas (reservas, canchas diferentes, horas jugadas, calificación promedio)
  y próximas reservas. En `/perfil/editar` cada usuario edita su nombre, teléfono, dirección y
  contraseña (el email y el rol no se pueden cambiar desde ahí).
- Después de iniciar sesión se entra a `/perfil`.
- La campana de la barra superior muestra las notificaciones sin leer (`SesionActual`).
- Las canchas se muestran como tarjetas con precio y calificación.
- Tipografías: Plus Jakarta Sans (títulos) e Inter (texto), desde Google Fonts.

## 15. DTOs y reorganización de paquetes

- `DTO/` agrupa todos los objetos de transferencia: `RegistroDTO` (formulario de registro),
  `UsuarioDTO` (respuesta del API REST) y las proyecciones JPQL `EspacioRankingDTO` e `IngresoNegocioDTO`.
- `Servicios/Impl/` contiene las implementaciones `*ServiceImpl`; `Servicios/` deja solo interfaces,
  excepciones, `DatosIniciales`, `SesionActual` y `UsuarioDetallesService`.
- `UsuarioRestController` ya no devuelve la entidad `Usuario`: la convierte con `UsuarioDTO.desde(...)`.
  Así el JSON no incluye el password ni las relaciones (que causaban recursión infinita
  usuario → negocios → administrador → ...).
- Relación Usuario–Negocio ahora **unidireccional**: se quitó `List<Negocio> negocios` de `Usuario`
  (solo `Negocio.administrador` conoce la relación), así no hay ciclo usuario → negocios → administrador.
  - Los negocios de un usuario se consultan con `NegocioRepository.findByAdministradorId(id)`
    (o `NegocioService.listarPorAdministrador(id)`); `usuarios/detalle.html` usa `${totalNegocios}`.
  - Como ya no hay cascade desde Usuario, `UsuarioServiceImpl.eliminar()` borra primero los negocios
    del usuario y después el usuario.
