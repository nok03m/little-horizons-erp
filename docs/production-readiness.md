# Procedimiento de staging y paso a producción

La aplicación no se debe conectar a producción hasta completar estos pasos. Las migraciones SQL hacen cambios de esquema y MySQL puede confirmar DDL aunque una sentencia posterior falle; el respaldo probado es el mecanismo de reversión.

## 1. Preparar staging

1. Crear un servidor y esquema MySQL aislados, con la misma versión principal y configuración de caracteres que producción.
2. Restaurar una copia reciente de producción en staging. Enmascarar datos personales cuando sea posible. No ejecutar `docs/seed.sql` sobre una copia.
3. Crear una cuenta de aplicación propia para staging, sin privilegios administrativos. Inyectar `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` y `DB_PARAMS` desde el gestor de secretos del entorno.
4. Instalar la CA del servidor en el truststore de la JVM y mantener `sslMode=VERIFY_IDENTITY`. El nombre de `DB_HOST` debe coincidir con el certificado.
5. Detener la aplicación de staging, tomar un respaldo y probar la restauración de ese respaldo en un esquema alterno.
6. Aplicar `docs/migrations/001_restrict_cascade_foreign_keys.sql` hasta `008_payment_rate_reference.sql`, en orden, una sola vez. Registrar salida y hora de cada migración.
7. Iniciar la aplicación contra staging. Ejecutar `mvn verify` y los flujos de aceptación de la auditoría: autenticación/roles, horarios, cupos concurrentes, tarifas/pagos, obligaciones parciales, facturas y préstamos.
8. Confirmar que una cuenta de aplicación sin permisos de DDL puede ejecutar las operaciones normales y que TLS valida identidad. Verificar el log de errores y restaurar la copia si una migración o prueba modifica datos de forma inesperada.

## 2. Criterios para autorizar el corte

- Staging utiliza una copia restaurada y su recuperación desde respaldo fue probada.
- Las ocho migraciones finalizaron sin errores y los conteos/restricciones se validaron.
- Los flujos críticos y permisos fueron aprobados por sus responsables funcionales.
- La CA/certificado real valida con `VERIFY_IDENTITY` desde la JVM de despliegue.
- Las credenciales expuestas se rotaron; la aplicación usa una cuenta distinta de la cuenta administrativa.
- Se definieron responsable, ventana, duración máxima y canal de reversión.

## 3. Corte en producción

1. Anunciar la ventana y detener todas las instancias del ERP.
2. Tomar un respaldo consistente de la base y probarlo o verificarlo según el procedimiento de infraestructura.
3. Aplicar las migraciones `001` a `008` en orden y guardar sus resultados.
4. Ejecutar consultas de integridad y validar conteos, claves foráneas, estudiantes, pagos y facturas antes de desplegar la nueva aplicación.
5. Configurar secretos y CA en el host; arrancar una instancia y validar conexión TLS, inicio de sesión, lectura académica y una operación de bajo riesgo aprobada.
6. Reanudar el servicio y observar errores, tiempos de respuesta y colas de facturas pendientes durante el período de monitoreo acordado.

## 4. Reversión

Si una migración o validación falla, mantener la aplicación detenida y restaurar el respaldo completo. No intentar revertir manualmente una migración parcialmente aplicada. Registrar el punto de fallo y repetir primero en staging.

## Pendientes funcionales

Antes de usar facturación recurrente, el área financiera debe definir periodicidad, fecha de generación, vencimiento, tratamiento de estudiantes que ingresan o se retiran a mitad del período, excepciones y quién puede corregir una obligación. El responsable académico debe definir si una cuenta representa a un docente, varios docentes o grupos, y qué operaciones puede realizar. Sin esas reglas no se debe automatizar la generación de cargos ni el alcance por fila.
