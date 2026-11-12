# Enigma · Wiki del proyecto (Reto 2)

Documentación de seguimiento del **Reto 2 de metodologías ágiles** para el monorrepo `dm2-ethazi/Reto2_Enigma`: cuatro clientes (Python, JavaFX, Android, Unity) sobre el mismo contrato técnico **R2AE**.

## Índice

| Página | Contenido |
|---|---|
| [Anexo II · Seguimiento y retrospectiva](Anexo-II-Seguimiento) | Informe final obligatorio: referencias, resumen por sprint, seguimiento, colaboración, reviews, comparación con el contrato y reflexión individual |
| [Pair programming](Pair-Programming) | Práctica, reglas del reto y los **cuatro registros** de sesiones con rotación de roles |
| [Tablero de GitHub Projects](Tablero-GitHub-Projects) | Workflow del tablero (`projects/16`): vistas, columnas, automatizaciones y criterios de entrada/salida |
| [Calidad, CI/CD y revisiones](Calidad-CI-CD) | Workflows, puerta de revisión, protección de ramas y Definition of Done |

## Referencias rápidas

- **Repositorio:** https://github.com/dm2-ethazi/Reto2_Enigma
- **Tablero:** https://github.com/orgs/dm2-ethazi/projects/16
- **Contrato técnico:** `Contrato_Tecnico_Reto_02_Enigma_v1.1.pdf` (raíz del repositorio)
- **Guía de metodologías:** `Guía de metodologías ágiles y seguimiento de proyectos (Intro Reto 2).pdf`

## Estructura del monorrepo

```
android/      Cliente Android (Kotlin): AES de imágenes y César de textos
javafx/       Cliente de escritorio JavaFX
python-cli/   Cliente de consola Python (referencia de formato R2AE)
unity/        Escena Unity
docs/         Documentación técnica y de metodología
.github/      Workflows de CI y de revisión
```

## Cómo se publica esta wiki

Estas páginas se mantienen versionadas en el repositorio (`docs/wiki/*.md`) y se copian a la pestaña **Wiki** del repositorio, empezando por `Home`. Así la documentación queda también en el historial de commits, como pide la guía.

## Equipo

| Integrante | Responsabilidad principal |
|---|---|
| Carlos | JavaFX, experiencia de usuario, pruebas y accesibilidad |
| Koldo | Python, interoperabilidad R2AE, diseño y documentación |
| Kenneth | Android, Unity, validación técnica y CI/CD |
