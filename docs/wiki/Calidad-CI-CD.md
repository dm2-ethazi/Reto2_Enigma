# Calidad, CI/CD y revisiones

## Workflows del repositorio

| Workflow | Archivo | Se ejecuta en | Qué comprueba |
|---|---|---|---|
| **CI Python** | `.github/workflows/ci.yml` | Push a ramas `python*` y PR hacia `main`, `develop`, `python*` | Infraestructura de scripts, y si existe `python/`: formato, lint, tests y **cobertura ≥ 80 %** |
| **Puerta de revisión** | `.github/workflows/puerta-revision.yml` | **Todas** las PR (sin filtro de rutas) y las revisiones enviadas o retiradas | Que la PR hacia `main`/`develop` tenga al menos una revisión **Approved** de otra persona |

Notas de diseño:

- El CI de Python usa filtros `paths` (`python/**`, `scripts/**`, `.githooks/**`), por eso **los cambios de Android, JavaFX o Unity no lanzan ese workflow**: es intencionado, cada cliente valida en local según el contrato.
- La puerta de revisión **no lleva filtro de rutas**, así que cubre cualquier cliente.
- Se usa el evento `pull_request` y no `pull_request_target`, porque este último toma el archivo del workflow de la rama **por defecto** (`main`) y no de la rama base.

## Bloqueo de PR hacia `main` y `develop`

Para que el check impida fusionar hay que activar la protección de rama (solo administradores):

1. *Settings → Rules → Rulesets* (o *Branches → Add branch protection rule*).
2. Objetivo: `main` y `develop`.
3. **Require a pull request before merging** · **Require approvals: 1**.
4. **Require status checks to pass** → seleccionar `Revisión obligatoria (main y develop)`.
5. Opcional: descartar aprobaciones antiguas al hacer push y bloquear force pushes.

## Cómo se hace una revisión

1. PR → pestaña **Files changed** → **Review changes**.
2. Escribir el comentario y elegir **Approve** (cuenta), **Comment** (no aprueba) o **Request changes** (bloquea).
3. **Submit review**. Nadie puede aprobar su propia PR.

## Hooks locales

| Hook | Qué hace |
|---|---|
| `.githooks/pre-commit` | Comprobaciones rápidas de formato antes de confirmar |
| `.githooks/pre-push` | Evita subir cambios que rompan el CI |

Se activan con `git config core.hooksPath .githooks`.

## Definition of Done (contrato §7.6)

- Issue con criterios de aceptación verificables.
- Código integrado por **PR revisada** por otra persona.
- Tests unitarios y funcionales aplicables superados.
- Workflow de Python sin fallos y **cobertura del 80 %** acreditada por aplicación (en local donde no haya CI).
- Documentación actualizada y evidencia enlazada en la Issue.
- Sin defectos bloqueantes conocidos.

## Pruebas mínimas del reto

- Ficheros de 0 B, 1 B, 1 MiB y al menos **20 MiB** (aceptación obligatoria del contrato).
- Ida y vuelta cifrar/descifrar en cada cliente y **vectores cruzados** entre clientes (R2AE).
- Entradas inválidas: contraseña fuera de formato, archivo truncado, cabecera incorrecta, falta de espacio.
- Límites de la app Android: contenedor AES de hasta 100 MiB y `.txt` de César de hasta 200 KiB, con aviso de «archivo demasiado grande».

## Límites conocidos

- Solo **Python** tiene bloqueo automático de tests y cobertura en CI/CD; JavaFX, Android y Unity se validan con pruebas locales e informes, tal como admite el contrato.
- Las PR hacia `main` no reciben el check de revisión mientras el workflow no exista también en `main`.
