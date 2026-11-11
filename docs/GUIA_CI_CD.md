# CI de pruebas Python

Adaptado de `Kenalvsm/pruebas_enigma`, sin copiar la calculadora de demostración
ni configuraciones de protección del repositorio original.

Las revisiones automáticas de aplicación ejecutan únicamente herramientas de
Python: `uv sync --locked`, Ruff (formato y lint) y pytest con cobertura de ramas
de `src` mínima del 80%. Las pruebas existentes basadas en unittest son compatibles
con pytest. Las dependencias están fijadas en `python/uv.lock`, incluido
`cryptography==50.0.2`, usado por la aplicación real.

## Alcance

- Push: `python`, `python-cli`, ramas terminadas exactamente en `/python` y
  `ci/cd` para validar la integración de infraestructura.
- PR: los mismos orígenes hacia `main`, `develop` o las ramas anteriores.
- Solo cambios en `python/`, `scripts/`, `.githooks/`, el workflow o la
  configuración de pre-commit disparan el workflow.
- JavaFX, Android y Unity no tienen jobs ni herramientas de validación.
- Los hooks validan Python en `python`, `python-cli` y `*/python`. No bloquean
  operaciones de otras plataformas ni añaden restricciones a `main` o `develop`.
- La ejecución manual del workflow permite solicitar expresamente la validación.

El workflow comprueba primero el alcance de los hooks. Si aún no existen
`python/src` y `python/tests`, deja una nota en el resumen y omite la validación
de aplicación; esto **no acredita** pruebas ni cobertura de la aplicación.
Una ejecución local explícita falla si falta ese código.

## Uso local

Instalar Python 3.14 y uv (`python -m pip install uv`). Cada clon activa los hooks:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/install-hooks.ps1
python -m uv sync --locked --project python
python -m unittest discover -s scripts -p 'test_*.py' -v
python scripts/quality.py test
```

En Linux: `sh scripts/install-hooks.sh`. No hace falta activar hooks para GitHub
Actions. `.pre-commit-config.yaml` ofrece una alternativa con pre-commit;
los hooks nativos son la opción recomendada para validar los refspecs de push.

Para corregir el formato antes de pasar las revisiones:

```powershell
python -m uv run --locked --project python ruff format python
```

Los informes JUnit y cobertura XML/HTML se guardan en `.artifacts/python/` y se
publican como artefacto `python-informes-<run_id>` incluso cuando fallan los tests.
`.actrc` permite ejecutar `act -j python-quality` con Docker.

Este cambio implementa integración continua y publicación de informes. No añade
un despliegue de la aplicación ni modifica rulesets o revisores de GitHub.
