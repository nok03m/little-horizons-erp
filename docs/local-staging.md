# Ejecución local de validación

El proyecto ya cuenta con un esquema local de prueba `app_db`, sembrado y con el esquema actual. Para abrir la aplicación contra esa instancia:

1. Confirma que MySQL esté escuchando en `127.0.0.1:3306`.
2. Ejecuta `mvn verify` desde la raíz del repositorio.
3. Ejecuta `./scripts/run-local-staging.ps1` en PowerShell. El script solicita la clave de `app_user` de forma interactiva, la conserva solo en el proceso y fija el host en loopback y la base en `app_db`.
4. Para crear la primera cuenta administrativa, inicia con `./scripts/run-local-staging.ps1 --bootstrap-admin` y sigue el diálogo. Para iniciar normalmente, ejecuta el script sin argumentos.

La conexión desactiva TLS únicamente en loopback. `app_db` sirve para pruebas funcionales y no es una copia de producción; no permite validar certificados, rendimiento ni el efecto de migraciones sobre datos reales. El script no modifica `erp_little_horizons`.
