# Auditoría del ERP Little Horizons

**Fecha:** 17 de noviembre de 2026
**Dictamen:** cumplimiento parcial. Las verificaciones de código y el esquema desechable pasaron; quedan controles de producción y cobertura integral antes de usar el sistema con datos reales.

## Alcance y método

Se revisó `Proyecto.md`, la arquitectura, seguridad, DDL/DML, README y código. Se compiló el proyecto y se ejecutó `mvn verify`. La base originalmente configurada (`erp_little_horizons`) solo tuvo las consultas de lectura de la auditoría previa y no se modificó en esta continuación.

`app_db` estaba vacío y se utilizó como esquema de prueba: se cargaron los datos de instalación, se aplicaron las migraciones 006, 007 y 008, y se ejecutaron pruebas con filas temporales que se limpiaron al terminar. Tras separar DDL y seed, ambos scripts se ejecutaron en `audit_install_tmp`; se comprobaron 32 estudiantes, 32 pagos y 10 facturas, y el esquema temporal se eliminó. Las migraciones 001–005 se probaron en otro esquema temporal con forma de esquema antiguo; también fue eliminado. En las pruebas de base local se desactivó TLS solo en loopback. No se probó un servidor con certificados de producción.

## Resultados de verificación

| Verificación | Resultado | Evidencia |
|---|---|---|
| Compilación y Maven | Pasa | `mvn verify`; build y empaquetado sombreado completados |
| Suite unitaria | Pasa | 7 pruebas JUnit: hash PBKDF2/sal, roles, balance de obligaciones y política TLS local/remota; 0 fallos |
| CI | Configurada | GitHub Actions ejecuta `mvn verify` con Java 17 en push y pull request; aún no se ha ejecutado en GitHub |
| Configuración TLS | Pasa | Se rechaza conexión MySQL remota sin `sslMode=VERIFY_IDENTITY`; TLS desactivado solo se permite en loopback |
| DDL/DML de instalación | Pasa | `app_db` se instaló desde cero y cumple los mínimos de filas del enunciado |
| Instalación separada | Pasa | `docs/database.sql` + `docs/seed.sql` instalados en esquema temporal; 32 estudiantes, 32 pagos, 10 facturas; temporal eliminado |
| Migraciones 001–005 | Pasa en esquema temporal | 001 cambió una FK `CASCADE` a `RESTRICT` sin perder la fila hija; 004/005 probaron columnas y checks |
| Migraciones 006–008 | Pasa en `app_db` | Estado de factura, obligaciones/aplicaciones, bloqueo persistente y FK pago-tarifa instalados |
| Integridad de `app_db` | Pasa | 27 tablas; 0 FK `ON DELETE CASCADE`; 0 notas fuera de 0–5; horario y secuencia de facturas válidos |
| Horarios | Pasa | Solapamiento rechazado y horario contiguo aceptado; filas de prueba limpiadas |
| Tarifas y pagos | Pasa | Consulta/renovación por vigencia; `PaymentDao` guardó y leyó el ID de tarifa en `app_db`; filas de prueba limpiadas |
| Capacidad de clubes | Pasa | Dos altas concurrentes por el último cupo: solo una tuvo éxito; datos limpiados |
| Secuencia de facturas | Pasa | 8 llamadas concurrentes produjeron números únicos; contador restaurado |
| Morosidad y pagos parciales | Pasa | Una obligación vencida recibió aplicación parcial FIFO; saldo y fecha vencida correctos; pago posterior liquidó el saldo |
| Reintento de factura | Pasa | Factura pendiente emitida, estado actualizado y segundo intento idempotente; PDF/TXT y filas temporales limpiados |
| Cuentas | Pasa | Bootstrap único, hash, bloqueo al quinto intento, desbloqueo por acceso correcto, desactivar/reactivar y protección del último administrador |

Estado final del esquema de prueba `app_db`: **27 tablas**, 32 pagos, 10 facturas emitidas y 22 pagos con factura pendiente. Las tablas de cuentas, obligaciones, aplicaciones y tarifas quedaron sin filas temporales; la verificación del vínculo de tarifa también limpió su pago de prueba. `app_db` conserva las filas semilla; no se modificó `erp_little_horizons`.

La suite no cubre todos los CRUD, login interactivo, emisión bajo fallos de proceso, ni TLS con CA/nombre de certificado de producción. Maven mostró una advertencia de recursos `MANIFEST.MF` duplicados al sombrear dependencias; el empaquetado terminó correctamente.

## Matriz de cumplimiento de `Proyecto.md`

| Área | Estado | Observación |
|---|---|---|
| MVC, Java 17, capas DAO/Service | Cumple | Separación visible y compilación completa |
| Validación de consola | Parcial | Helpers comunes y pruebas unitarias; faltan recorridos de todos los menús/campos |
| Estudiantes, acudientes, matrícula y materias | Parcial | CRUD disponible; no se garantiza en la BD que todo estudiante tenga acudiente |
| Docentes y horarios | Parcial | Día/rango requeridos al crear; cruce por docente/año/día controlado bajo bloqueo. Registros antiguos siguen sin horario |
| Notas, promedio e historial | Parcial | Lecturas/cálculos previos pasaron; faltan pruebas de escritura y períodos completos |
| Asistencia y ausencias | Parcial | La regla de más de tres ausencias por semana requiere prueba integral |
| Clubes y cupos | Parcial | Altas, reactivaciones y cambio de capacidad serializados; la prueba concurrente pasó |
| Tarifas especiales | Parcial | Vigencia, monto, motivo y autorizador se guardan; los pagos conservan la tarifa aplicada por FK. Prueba MySQL del ida y vuelta por DAO |
| Pagos y morosidad | Parcial | Obligaciones vencidas y aplicaciones parciales soportadas. Las obligaciones se crean manualmente; no hay generación automática por calendario/concepto |
| Facturas simuladas PDF/QR | Parcial | Emisión se intenta tras el pago, estado pendiente recuperable, reintento e idempotencia. El alta del pago y la emisión siguen siendo pasos separados |
| Recursos y préstamos | Parcial | Transacciones de préstamo/devolución implementadas; falta probar ciclos CRUD completos |
| DDL/DML MySQL | Cumple parcialmente | 27 tablas, constraints e índices; mínimo de filas cubierto. Instalación es para esquema nuevo/vacío, no migración repetible |
| Autenticación | Parcial | PBKDF2, roles por módulo, bloqueo persistente y ciclo básico de activación. Sin alcance por fila/grupo ni auditoría completa de acciones |
| README | Parcial | Instrucciones y funcionalidades descritas; documenta SOLID parcial, setup de staging local y migración operacional |
| Entrega GitHub/trainer/commit | No verificable | La copia local no acredita privacidad, colaboradores ni commit/entrega final |

## Hallazgos priorizados

### Alto — Credenciales y transporte

La contraseña ya no está en `application.properties`; se lee de entorno y TLS `VERIFY_IDENTITY` es el valor predeterminado. La prueba local usó SSL desactivado en loopback y no valida TLS de producción. Las credenciales que se compartieron en la conversación deben rotarse si son reales/reutilizables. La base original no se conectó con la configuración nueva.

**Pendiente:** rotar secretos reales, limitar privilegios y probar con host/certificado/CA de producción. El código ahora rechaza TLS desactivado o no verificado fuera de loopback. No se compartieron credenciales productivas ni configuración de certificados.

### Alto — Migración de producción pendiente

Se eliminó `DROP DATABASE` y se separaron el esquema (`docs/database.sql`) y los datos demostrativos (`docs/seed.sql`). Se retiró el bloque `TRUNCATE`; la carga de seed no elimina ni reinicia datos. Ambos scripts se verificaron en un esquema desechable. Siguen siendo una instalación inicial y no migran una base existente.

**Pendiente:** aún no existe staging con copia de producción. Se preparó `scripts/run-local-staging.ps1` para ejecutar la aplicación contra `app_db`; ese esquema local no sustituye una copia ni valida las migraciones frente a datos productivos. Aplicar 001–008 en staging real y, tras validar respaldo y datos, en la base original durante mantenimiento. El procedimiento está en `docs/production-readiness.md`.

### Alto — Historial ante borrados

Los DDL nuevos evitan `ON DELETE CASCADE`, el estudiante se retira lógicamente y no se permite borrar pagos desde la vista. La migración 001 pasó en esquema temporal y preservó la fila hija. Las claves recreadas reciben un nombre determinista distinto para evitar la colisión encontrada durante la primera prueba.

**Pendiente:** aplicar la migración en el esquema original con respaldo; diseñar reversos/ajustes financieros auditables.

### Medio — Alcance y auditoría de acceso

Las cuentas usan PBKDF2, roles por módulo, bloqueo tras 5 fallos por 15 minutos y comandos de administración de estado. Se protege el último administrador activo. El rol `ACADEMIC` aún puede operar sobre todos los estudiantes/notas; no existe vínculo cuenta-docente/grupo ni registro central de cada operación.

**Pendiente:** acordar alcance de docentes/funcionarios, aplicar autorización por fila/grupo y completar un audit log protegido.

### Medio — Obligaciones y saldos vencidos

El reporte calcula saldo vencido real a partir de `obligaciones_pago`, vencimientos y `aplicaciones_pago`; los pagos se asignan al saldo más antiguo y admiten abonos parciales. Se verificó en MySQL.

**Pendiente:** decidir si se generarán obligaciones automáticamente (p. ej. mensualidad por calendario/concepto); hoy un usuario financiero crea cada obligación, período, valor y vencimiento.

### Medio — Pago y factura recuperables, aún separados

El pago nace con estado de factura `PENDIENTE`; la aplicación intenta emitirla, el menú permite reintentar pendientes, la unicidad de factura evita duplicados y la inserción de factura/estado emitido es atómica. Si la emisión falla, el pago queda registrado y recuperable, no se pierde. El PDF/TXT y la BD no comparten transacción; puede quedar un archivo huérfano si el proceso termina abruptamente antes de guardar la fila.

**Pendiente:** prueba de fallos de proceso y limpieza de archivos huérfanos. El estado pendiente se probó; el flujo visual completo no se automatizó.

## Pendientes antes de producción

1. Crear staging con una copia restaurada y validar recuperación, TLS y migraciones 001–008; el runner local para `app_db` está listo, pero no reemplaza esta etapa. Después respaldar y migrar la base original en ventana de mantenimiento.
2. Rotar credenciales compartidas si son reales, usar usuario SQL de mínimo privilegio y verificar TLS con el certificado de producción.
3. Definir quién crea obligaciones recurrentes y cómo se corrigen/revierten pagos y facturas emitidos.
4. Definir políticas de acceso por docente/grupo y guardar auditoría de operaciones financieras y administrativas.
5. Completar pruebas CRUD, permisos de cada rol, fallos de disco/DB y el flujo interactivo en un ambiente de staging.
6. Revisar y versionar el estado local pendiente de seguimiento; confirmar configuración/entrega GitHub fuera de esta copia.
