# Little Horizons ERP

Sistema de Planificacion de Recursos Empresariales (ERP) de consola para el **Colegio Internacional Little Horizons**.

## Descripcion

Aplicacion Java que centraliza la gestion academica, extracurricular, financiera y de recursos del colegio.

### Problemas que resuelve

- Perdida de informacion academica (notas y asistencia)
- Desorganizacion de clubes y eventos
- Facturacion manual lenta y propensa a errores
- Falta de control en prestamos de biblioteca/laboratorio
- Escasa visibilidad del rendimiento estudiantil

### Funcionalidades principales

- Gestion de estudiantes, acudientes y matriculas
- Profesores, materias, notas (0.0-5.0) y asistencia
- Clubes (con validacion de cupo) y eventos
- Conceptos de pago, tarifas por estudiante/concepto, obligaciones con pagos parciales, morosos y facturas simuladas PDF (con QR)
- Inventario y prestamos a estudiantes/profesores
- Reportes y estadisticas exportados a TXT

## Autores

| Nombre | Correo | Contacto |
|--------|--------|----------|
| Lopez Andres | lopezquintero271@gmail.com | GitHub: [nok03m](https://github.com/nok03m) |

## Tecnologias Utilizadas

- **Lenguaje:** Java 17
- **Build:** Apache Maven 3.9+
- **Base de datos:** MySQL 8.0 o superior
- **Driver JDBC:** `mysql-connector-j` 8.3.0
- **IDE sugerido:** IntelliJ IDEA / VS Code / Eclipse
- **Librerias externas:** MySQL Connector/J; OpenPDF (PDF); ZXing (codigo QR embebido en la factura). Reportes estadisticos en TXT.

## Requerimientos Previos

- JDK 17 o superior
- Apache Maven 3.9+
- **MySQL 8.0 o superior** (requerido por CHECK constraints y charset `utf8mb4`)
- Acceso a un servidor MySQL local o remoto

## Configuracion de la Base de Datos

### 6.1. Creacion de la base de datos

El esquema (DDL) y los datos de demostración (DML) están separados:

```text
docs/database.sql
docs/seed.sql
```

Ejecuta ambos scripts únicamente para una instalación nueva. El primero crea el esquema; el segundo carga filas de demostración y debe ejecutarse una sola vez. Ninguno es una migración para una base existente.

Ejecutar, por ejemplo:

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p < docs/database.sql
mysql -h 127.0.0.1 -P 3306 -u root -p erp_little_horizons < docs/seed.sql
```

El esquema crea `erp_little_horizons` y sus tablas. El segundo comando agrega las filas de demostración (>= 30 registros en tablas principales). No vuelvas a ejecutar `seed.sql`: sus claves únicas harán que MySQL rechace duplicados y el script no borra datos existentes.

La carga inicial no es una migración y no debe repetirse sobre una base poblada. Para una instalación existente, respalda primero la base y revisa las migraciones de `docs/migrations/`; `001_restrict_cascade_foreign_keys.sql` preserva filas y cambia las claves foráneas con borrado en cascada a `RESTRICT`.

La instalación nueva incluye las tablas `usuarios_app`, `invoice_sequence`, `tarifas_estudiante_concepto`, `obligaciones_pago` y `aplicaciones_pago`, además de horario opcional para asignaciones heredadas y obligatorio desde la aplicación. En una base existente, respalda la base y aplica, en orden, las migraciones `001` a `008` de `docs/migrations/`, deteniendo antes las instancias del ERP. Las migraciones restringen cascadas, agregan autenticación, secuencia de facturas, horarios, tarifas, obligaciones/saldos parciales, reintento de facturas pendientes, bloqueo persistente de cuentas y referencia de tarifa por pago. Requieren una cuenta administrativa de MySQL y no se ejecutan automáticamente desde la aplicación.

Para preparar y validar una migración con respaldo, staging, criterios de corte y reversión, sigue [docs/production-readiness.md](docs/production-readiness.md). No ejecutes migraciones en producción antes de superar esos criterios en staging.

Para ejecutar la aplicación en el entorno local de validación `app_db`, usa [docs/local-staging.md](docs/local-staging.md) y `scripts/run-local-staging.ps1`. Este entorno no sustituye staging con una copia de producción.

> Nota tecnica: el identificador `anio_academico` se usa en lugar de `año_academico` para evitar problemas de encoding entre Windows/WSL/MySQL. Los ENUMs se normalizaron sin tildes donde fue necesario por la misma razon.

### 6.2. Configuracion de credenciales

La configuración no debe contener contraseñas. Define las credenciales mediante variables de entorno:

```properties
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=erp_little_horizons
DB_USER=app_user
DB_PASSWORD=<contraseña>
DB_PARAMS=sslMode=VERIFY_IDENTITY&serverTimezone=UTC&characterEncoding=UTF-8
```

`DB_PASSWORD` es obligatoria. No guardes credenciales en el repositorio. La conexión usa TLS con verificación de identidad de forma predeterminada; configura el certificado de confianza de MySQL en el entorno cliente. La aplicación rechaza conexiones remotas que no declaren `sslMode=VERIFY_IDENTITY`; solo permite desactivar TLS para hosts de loopback.

Ejemplo local con Docker MySQL:

| Campo | Valor |
|-------|-------|
| Host | `127.0.0.1` |
| Puerto | `3306` |
| Base | `erp_little_horizons` |
| Usuario | `app_user` |
| Password | Variable de entorno `DB_PASSWORD` |

Conceder únicamente los permisos necesarios a la cuenta de aplicación. No uses `ALL PRIVILEGES` en entornos compartidos o de producción. Por ejemplo, ajusta los privilegios de lectura y escritura a las operaciones habilitadas por la aplicación:

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON erp_little_horizons.* TO 'app_user'@'%';
FLUSH PRIVILEGES;
```

Para una instancia MySQL local de desarrollo sin TLS, se puede sobrescribir `DB_PARAMS` con `sslMode=DISABLED&serverTimezone=UTC&characterEncoding=UTF-8`. Limita esta excepción a conexiones locales; no desactives TLS para conexiones de red.

### 6.3. Instalacion del driver JDBC

Maven descarga automaticamente `mysql-connector-j` desde el `pom.xml`. No se requiere instalacion manual.

## Instalacion y Ejecucion

### 7.1. Clonar el repositorio

```bash
git clone https://github.com/nok03m/little-horizons-erp.git
cd little-horizons-erp
```

### 7.2. Abrir el proyecto en el IDE

Abrir la carpeta raiz como proyecto Maven.

### 7.3. Compilar

```bash
mvn clean package
```

### 7.4. Ejecutar

La primera vez, crea la cuenta administradora con el bootstrap interactivo. Desde IntelliJ, la contraseña se solicita en un diálogo enmascarado:

```bash
java -jar target/little-horizons-erp-1.0.0.jar --bootstrap-admin
```

#### Credenciales de demostración

Para una demo local, crea la cuenta predeterminada solo cuando no existan cuentas:

```bash
java -jar target/little-horizons-erp-1.0.0.jar --bootstrap-demo-admin
```

Credenciales demo:

- Usuario: `demo_admin`
- Contraseña: `DemoHorizons2026!`

Estas credenciales son públicas y solo para demostraciones locales. No las uses en producción ni en una base con datos reales. El bootstrap demo se rechaza si ya existe cualquier cuenta.

Luego, inicia la aplicación normalmente; solicitará usuario y contraseña:

```bash
java -jar target/little-horizons-erp-1.0.0.jar
```

Un administrador puede crear cuentas de rol `ADMIN`, `ACADEMIC`, `FINANCE` o `LIBRARIAN` con:

```bash
java -jar target/little-horizons-erp-1.0.0.jar --create-user
```

El administrador autenticado puede desactivar/reactivar cuentas con `--disable-user <usuario>` y `--enable-user <usuario>`. No se permite desactivar la cuenta propia ni el último administrador activo. Cinco contraseñas incorrectas bloquean la cuenta durante 15 minutos; un inicio válido reinicia el contador.

Las contraseñas se ocultan al escribirlas y se almacenan con PBKDF2-HMAC-SHA256, salt aleatorio y 310 000 iteraciones. Se usa entrada de terminal cuando está disponible y un diálogo enmascarado al ejecutar desde IntelliJ.

## Estructura del Proyecto

Arquitectura **MVC** con capas adicionales `dao/` y `service/`:

```text
src/main/java/com/littlehorizons/erp/
├── Main.java                 # Punto de entrada
├── model/                    # Entidades del dominio
├── dao/                      # Acceso a datos (PreparedStatement)
├── controller/               # Orquestacion de casos de uso
├── view/                     # Menus de consola
├── service/                  # Promedios, facturas TXT, reportes TXT
└── util/                     # Conexion, config, logger, consola
src/main/resources/
└── application.properties
docs/
└── database.sql              # DDL + DML
invoices/                     # Facturas TXT generadas
reports/                      # Reportes TXT generados
error_log.txt                 # Log de errores (runtime)
```

### Separacion de responsabilidades

- **Model:** datos del dominio sin logica de UI
- **View:** menus, validacion de entradas y mensajes
- **Controller:** reglas de negocio y coordinacion
- **DAO:** SQL con `PreparedStatement` y `try-with-resources`
- **Service:** calculos/reportes reutilizables (streams, archivos)

## Principios y Patrones de Diseno Aplicados

### 9.1. Principios SOLID

| Principio | Aplicacion en el proyecto |
|-----------|---------------------------|
| **S**ingle Responsibility | La separación por DAO, servicio y vista asigna responsabilidades por módulo; algunos controladores todavía coordinan varias operaciones |
| **O**pen/Closed | La separación por módulos permite añadir funcionalidades, aunque extender reportes requiere editar controladores concretos |
| **L**iskov Substitution | No se demuestra sustitución por contratos: los controladores reciben DAOs y servicios concretos |
| **I**nterface Segregation | Las vistas están divididas por módulo, pero no hay interfaces pequeñas que definan contratos de uso |
| **D**ependency Inversion | `Main` compone dependencias y las vistas usan controladores; el acceso JDBC se mantiene detrás de DAOs, aunque no se inyectan abstracciones |

La aplicación usa separación MVC/DAO y composición centralizada, pero su cumplimiento de SOLID es parcial. No se implementaron interfaces solo para afirmar los principios de sustitución o inversión de dependencias.

### 9.2. Patrones de Diseno

- **Singleton:** `DatabaseConnection` centraliza la configuracion JDBC
- **DAO:** encapsula SQL y mapeo ResultSet -> model
- **MVC:** separacion model / view / controller
- **Service Layer:** `AcademicService`, `InvoiceTxtService`, `ReportTxtService`

Tambien se usan **lambdas** y **Stream API** (promedios, filtros de reportes, totales de pagos).

## Funcionalidades Principales

### 1. Gestion de Estudiantes

- CRUD estudiantes y acudientes
- Vinculo estudiante-acudiente
- Matriculas por grado/anio

### 2. Gestion Academica

- Materias e inscripciones
- Notas 0.0-5.0 e historial
- Promedios por materia/estudiante/curso
- Asistencia y reporte de inasistencias

### 3. Gestion de Profesores

- CRUD profesores
- Asignacion profesor-materia

### 4. Actividades Extracurriculares

- Clubes con validacion de cupo maximo
- Membresias (activa/retirada)
- Eventos y participantes (posiciones si hay competencia)

### 5. Gestion Financiera

- Conceptos de pago
- Registro de pagos (monto base, tarifa vigente o monto personalizado con opción de guardar tarifa)
- Factura electronica simulada DIAN en **PDF** (`invoices/factura_XXXXXX.pdf`) + copia TXT companera
- Obligaciones por período/vencimiento, aplicación parcial de pagos y reporte por saldos vencidos

### 6. Gestion de Recursos

- Inventario por categoria y estado
- Prestamos a estudiantes y profesores
- Devolucion (estado vuelve a `Disponible`)
- Reporte de prestamos vencidos

### 7. Reportes y Estadisticas (TXT)

Exporta a `reports/`:

1. Estadisticas generales
2. Historial academico / promedios
3. Ausencias
4. Morosos
5. Prestamos vencidos
6. Clubes y membresias
7. Eventos y participacion
8. Pagos por estudiante

## Manejo de Errores y Logs

- Excepciones de negocio/datos capturadas en menus con mensajes claros
- Persistencia de detalle tecnico en `error_log.txt` via `AppLogger` (`java.util.logging`)
- SQL con `PreparedStatement` para prevenir inyeccion
- Recursos JDBC cerrados con `try-with-resources`
- Validacion de entradas en `ConsoleHelper` (reintento ante datos invalidos)

Ubicacion del log:

```text
error_log.txt   (raiz del proyecto, generado en runtime)
```

## Problemas Conocidos y Limitaciones

- Facturacion DIAN es **simulada** (CUFE/QR en PDF/TXT, no transmision real a DIAN)
- Los roles limitan el acceso a módulos completos; todavía no filtran filas por profesor, estudiante o grupo
- No hay bloqueo persistente de cuentas después de intentos fallidos
- La conexion JDBC abre una conexion por operacion (sin pool)
- Caracteres especiales en consola Windows pueden verse distintos segun code page

## Futuras Mejoras

- Autorizacion por registro/grupo para docentes y permisos mas granulares
- Exportacion PDF real opcional
- Pool de conexiones (HikariCP)
- Pruebas unitarias/integracion automatizadas
- Interfaz web

## Licencia

Proyecto academico. Uso educativo. Todos los derechos reservados por el autor salvo acuerdo contrario con la institucion formadora.

## Contacto

- **Autor:** Lopez Andres
- **Email:** lopezquintero271@gmail.com
- **GitHub:** https://github.com/nok03m/little-horizons-erp

---

## Checklist rapido de entrega

- [x] Proyecto Java Maven + MVC
- [x] Codigo en ingles (clases/metodos/variables)
- [x] MySQL 8 + `docs/database.sql` (DDL + DML)
- [x] CRUD y flujos principales por modulo
- [x] Logs en `error_log.txt`
- [x] PreparedStatement + try-with-resources
- [x] DAO / Singleton / Streams; SOLID aplicado parcialmente (ver evaluación anterior)
- [x] Facturas PDF (+ TXT) y reportes TXT
- [x] README completo
