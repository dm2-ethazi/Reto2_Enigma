# Enigma · Reto 2

Proyecto del **Reto 2 de DAM**, desarrollado por **Koldo, Kenneth y Carlos**.
El repositorio reúne las implementaciones de las distintas plataformas.

## Documentación

| Área | Ubicación |
| --- | --- |
| Aplicación Python: instalación, cifrado, descifrado y pruebas | [Guía de Python](python/README.md) |
| Formato AES compartido por la implementación Python | [Contenedor R2AE v1](python/docs/R2AE.md) |
| Revisiones automáticas, hooks e informes de Python | [Guía de CI](docs/GUIA_CI_CD.md) |

## Python

La aplicación de terminal permite cifrar y descifrar texto con **Vigenère** y
archivos con **AES-256-GCM**. Incluye verificación de autenticidad mediante AAD,
procesamiento por bloques con partes de hasta 20 MiB, claves enmascaradas y
apertura del explorador tras guardar.

Con Python **3.14.x**, desde la raíz del repositorio:

```powershell
python -m pip install uv
cd python
python -m uv sync --locked
python -m uv run --locked python main.py
```

Consulta [el README de Python](python/README.md) para la instalación con venv,
el uso de cada algoritmo y la resolución de problemas.

La configuración de CI de esta rama valida Python con Ruff, pytest y una
cobertura mínima del 80 %. Las carpetas de Android, JavaFX y Unity están
separadas de estas validaciones.
