# Tablero de GitHub Projects · workflow de trabajo

Tablero del equipo: **https://github.com/orgs/dm2-ethazi/projects/16** (proyecto de organización `dm2-ethazi`).

Es la fuente de verdad del trabajo: cada tarea es una **Issue** con criterios de aceptación, estimación en horas-persona, prioridad y responsable; las PR se enlazan a su Issue y mueven la tarjeta.

## Vistas

| Vista | Uso | Configuración |
|---|---|---|
| **Backlog** (tabla) | Ordenar y priorizar | Campos: Status, Prioridad, Estimación, Responsable, Sprint |
| **Board** (kanban) | Trabajo del día a día | Columnas por `Status`, agrupadas por `Sprint` |
| **Roadmap** o **Iteration** | Planificación de sprints | Campo `Sprint` con valores Discovery, Sprint 1, Sprint 2 |

## Columnas y criterios

| Columna | Entra cuando | Sale cuando |
|---|---|---|
| **Backlog** | La idea existe pero no está lista | Tiene criterios de aceptación, estimación y prioridad |
| **Ready** | Está definida y cabe en el sprint | Alguien la toma y crea la rama |
| **In progress** | Hay rama y commits en curso | Se abre la PR contra la rama del cliente |
| **In review** | La PR está abierta y pide revisión | La PR se aprueba y fusiona |
| **Done** | Fusionada y verificada | — |

**Límite de trabajo en curso (WIP):** máximo **3 tarjetas** en `In progress` por persona y **2** en `In review` por el equipo; si se supera, se termina antes de empezar otra cosa.

## Reglas del equipo (workflow)

1. **Nada se trabaja sin Issue.** Si surge una tarea nueva, primero se crea la Issue con criterio de aceptación y estimación; después se empieza.
2. **Una rama por Issue**, con el patrón `feature-<tema>/<cliente>` (`feature-descifrado_aes/android`, `feature-ventana/javafx`, …).
3. **Cada PR enlaza su Issue** (`Closes #NN`) y describe qué se probó.
4. **La PR pasa por `In review`** cuando está abierta; nadie fusiona su propia PR a `develop` o `main` sin aprobación (ver [Calidad, CI/CD y revisiones](Calidad-CI-CD)).
5. **`Done` exige evidencia**: criterio de aceptación cumplido, PR fusionada y prueba ejecutada.
6. **Un solo responsable por tarjeta**; las tareas compartidas se parten en Issues hijas o se resuelven en una sesión de pair programming.

## Campos del proyecto

| Campo | Tipo | Valores |
|---|---|---|
| Status | single select | Backlog, Ready, In progress, In review, Done |
| Prioridad | single select | Urgent, High, Medium, Low |
| Estimación | number | horas-persona (no horas de reloj) |
| Responsable | single select | Carlos, Koldo, Kenneth |
| Sprint | single select | Discovery, Sprint 1, Sprint 2 |
| Cliente | single select | Python, JavaFX, Android, Unity, Transversal |

## Automatizaciones del proyecto (Project → ⋯ → Workflows)

Se configuran una vez en el propio proyecto, no en el repositorio:

| Workflow | Disparador | Acción | Estado |
|---|---|---|---|
| **Auto-add to project** | Issue o PR creada en `dm2-ethazi/Reto2_Enigma` | Añadir al proyecto con `Status = Backlog` | Activar |
| **Item closed** | Issue o PR cerrada | Pasar a `Done` | Activar |
| **Pull request linked to issue** | PR enlazada | Mover a `In review` | Activar |
| **Item reopened** | Reabierta | Volver a `Ready` | Opcional |

Cómo activarlos: abrir el proyecto → menú `⋯` → **Workflows** → elegir la automatización → **Edit** → seleccionar el repositorio y el campo/valor → **Save and turn on**.

### Snippet opcional: añadir Issues al proyecto desde Actions

Si más adelante se quiere que el alta en el tablero sea automática desde el repositorio, el workflow sería este (requiere un secreto `ADD_TO_PROJECT_PAT` con permiso `project` sobre la organización; no está activo para no romper el CI mientras no exista el secreto):

```yaml
name: Añadir al tablero
on:
  issues:
    types: [opened]
  pull_request:
    types: [opened]
permissions:
  contents: read
jobs:
  add:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/add-to-project@v1
        with:
          project-url: https://github.com/orgs/dm2-ethazi/projects/16
          github-token: ${{ secrets.ADD_TO_PROJECT_PAT }}
```

## Etiquetas del repositorio

| Etiqueta | Uso |
|---|---|
| `python`, `javafx`, `android`, `unity` | Cliente afectado |
| `ci/cd` | Workflows, ramas y automatizaciones |
| `docs` | Documentación y wiki |
| `bug` | Defecto con pasos de reproducción |

Estas etiquetas permiten filtrar el tablero por cliente y montar la vista de cada integrante.
