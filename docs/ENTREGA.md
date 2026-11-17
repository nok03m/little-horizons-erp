# Guia de entrega

## Repositorio

- URL del repositorio privado de GitHub (agregar al trainer como colaborador lectura)
- Rama principal: `main`
- Hash SHA completo del commit final de entrega

Formato sugerido en la plataforma:

```text
URL del repositorio: https://github.com/<usuario>/little-horizons-erp
Hash del commit: <sha-completo>
```

## Artefactos incluidos

| Artefacto | Ubicacion |
|-----------|-----------|
| Codigo fuente MVC | `src/main/java/com/littlehorizons/erp/` |
| Script BD DDL+DML | `docs/database.sql` |
| README completo | `README.md` |
| Config JDBC | `src/main/resources/application.properties` |

## Verificacion previa

1. `mvn clean package`
2. Importar/verificar `docs/database.sql` en MySQL 8+
3. `java -jar target/little-horizons-erp-1.0.0.jar`
4. Probar un flujo por modulo (1-7) y confirmar:
   - `error_log.txt` se genera ante errores
   - facturas PDF (y TXT) en `invoices/`
   - reportes en `reports/`

## No versionar (local)

Ignorados por `.gitignore`:

- `Proyecto.md` (enunciado local)
- `tools/`, `target/`, logs y artefactos generados en `invoices/` / `reports/`
