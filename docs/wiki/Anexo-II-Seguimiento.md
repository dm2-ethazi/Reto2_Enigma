# Anexo II · Seguimiento y retrospectiva del desarrollo

> Documento obligatorio del **Hito 2** (Reto 2). Se entrega como PDF junto a la documentación final y contrasta lo planificado con el trabajo real. Todos los datos de actividad salen del historial de GitHub (`dm2-ethazi/Reto2_Enigma`, rama `develop`); solo quedan marcados como `PENDIENTE` los datos que únicamente conoce el equipo: **horas reales, dailies y reflexiones**. Las sesiones de pair programming tienen su propio registro en [Pair-Programming](Pair-Programming).

## 0. Resumen de cifras

| Métrica | Valor |
|---|---|
| Periodo con actividad | 28/10/2026 (primera Issue) – 12/11/2026 (último commit) |
| Commits en `develop` | 96 |
| Pull requests creadas | 46 (33 fusionadas, 13 sin fusionar al cierre) |
| Issues + PR | 38 Issues y 46 PR (84 ítems en total) |

---

## 1. Referencias

| Evidencia | Enlace |
|---|---|
| Repositorio | https://github.com/dm2-ethazi/Reto2_Enigma |
| Tablero (GitHub Projects) | https://github.com/orgs/dm2-ethazi/projects/16 |
| Contrato técnico completo | `Contrato_Tecnico_Reto_02_Enigma_v1.1.pdf` |

## 2. Resumen por sprint

Plan de tres semanas del contrato: semana 1 Discovery, semanas 2 y 3 los dos sprints. Las fechas de cierre son las de la última actividad registrada en cada fase.

| Fase | Inicio | Cierre | Commits | PR creadas | De ellas fusionadas | De ellas sin fusionar | Estimado (contrato) |
|---|---|---|---|---|---|---|---|
| **Discovery** | 26/10/2026 | 01/11/2026 | 4 | 2 | 1 (#36) | 1 (#35) | **48 h** (D-01…D-06) |
| **Sprint 1** | 02/11/2026 | 08/11/2026 | 38 | 15 | 14 | 1 (#44) | **48 h** (S1-01…S1-06) |
| **Sprint 2** | 09/11/2026 | 12/11/2026 | 54 | 29 | 18 | 11 | **48 h** (S2-01…S2-06) |
| **Total** | 26/10/2026 | 12/11/2026 | **96** | **46** | **33** | **13** | **144 h** |

Criterio de recuento: una PR pertenece a la fase en la que se **creó** (campo `created_at`); las columnas de fusión cuentan si esa PR está fusionada hoy. Si se cuenta por fecha de fusión dentro de cada ventana, los valores son 1, 13 y 19; la única diferencia es la PR #55, creada el 08/11 y fusionada el 09/11. Los commits se cuentan por fecha de autor sobre la rama `develop` (96 en total).

**Objetivo y resultado por fase**

| Fase | Objetivo del contrato | Terminado, con evidencia |
|---|---|---|
| Discovery | Estructura, formato R2AE y viabilidad | Monorrepo con ramas por cliente y workflows base; primer prototipo de JavaFX (#36). La PR #35 (hacia `main`) quedó sin fusionar |
| Sprint 1 | Un camino funcional demostrable en las cuatro apps | César y AES en Python (#42, #49), JavaFX (#41, #45, #50), Android (#43, #46, #51) y ventana de Unity (#52); 14 PR de la fase fusionadas |
| Sprint 2 | Integración, accesibilidad, pruebas y documentación | Vista previa JavaFX (#55), físicas y estados de Unity (#56, #57, #62), cierre de Python con CI (#58, #61, #67), multidioma y README de Android (#64), puerta de revisión (#69, #71) y centralización de README (#75, #79, #82, #83, #84) |

**Commits por integrante y fase** (identidades de git unificadas por correo):

| Fase | Carlos | Koldo | Kenneth | Total |
|---|---|---|---|---|
| Discovery | 3 | 0 | 1 | 4 |
| Sprint 1 | 14 | 11 | 13 | 38 |
| Sprint 2 | 8 | 27 | 19 | 54 |
| **Total** | **25** | **38** | **33** | **96** |

Las horas se recogen **por Issue**: el campo «Horas» del tablero da la estimación y el equipo declara las reales.

### Horas por Issue (recogida real)

Plantilla `docs/metodologia/horas-reales-issues.csv`: una fila por **cada Issue**, sacada del historial de GitHub (responsable = assignee, fase = fecha de cierre). Se rellenan solo `Estimado_h` y `Real_h`; el bloque `RESUMEN` suma automáticamente por integrante y fase con `SUMIFS`/`COUNTIFS` y calcula la desviación (`=Real−Estimado`) y el porcentaje.

| Fase | Carlos | Koldo | Kenneth | Sin asignar | Filas |
|---|---|---|---|---|---|
| Discovery | 1 (#2) | 0 | 0 | 1 (#1) | 2 |
| Sprint 1 | 7 | 8 | 3 | 1 (#4) | 19 |
| Sprint 2 | 1 (#19) | 10 | 8 | 0 | 19 |
| Abiertas | 0 | 0 | 0 | 1 (#21) | 1 |
| **Total** | **9** | **18** | **11** | **3** | **41 filas / 38 Issues** |

Notas de la recogida:

- Las Issues #25 (pre-commit), #26 (pre-push) y #27 (CI/CD) tienen **dos responsables** (Koldo y Kenneth) y aparecen en dos filas: son las 3 filas de más del Sprint 2 (16 Issues, 19 filas).
- **Responsables por plataforma**: las Issues que estaban sin assignee en GitHub se repartieron según su plataforma · Java → Carlos (#19 vista previa Java, #38 generador de contraseñas JavaFX), Unity → Koldo (#8 cifrado César, #14 descifrado, #33 colliders y físicas, #34 estados de juego), Python → Koldo (#18 vista previa, #39 generador de contraseñas) y Android → Kenneth (#47 descifrado César, #48 descifrado AES).
- Quedan **tres Issues transversales sin responsable**, sin plataforma propia: #1 (aprobación de la propuesta), #4 (generador de contraseñas) y #21 (pruebas de cifrado/descifrado). Hay que asignarlas en GitHub o completar la fila a mano.
- GitHub no guarda horas por Issue: el campo `Estimación` vive en el tablero `projects/16` y no es accesible sin autenticación, así que `Estimado_h` se copia de ahí (o de la tabla del contrato) y `Real_h` se apunta al cerrar cada Issue.

**Horas volcadas desde el tablero** (`projects/16`, campo «Horas», según la vista del 12/11). Están en la columna `Estimado_h` de `horas-reales-issues.csv`:

| Concepto | Horas |
|---|---|
| Carlos (9 Issues) | 47 |
| Koldo (18 Issues, incluidas las tres compartidas) | 82 |
| Kenneth (11 Issues, incluidas las tres compartidas) | 47 |
| Sin responsable asignado (#4, #21; la #1 no tiene horas) | 9 |
| **Total** | **170** |

Las 15 h de #25/#26/#27 están contadas enteras para Koldo y para Kenneth; si se reparten al 50 %, el reparto queda en **Koldo 74,5 h** y **Kenneth 39,5 h**.

**Horas reales declaradas por el equipo**

El tablero solo trae estimaciones. La cifra real cerrada hasta ahora es la de Koldo, que sustituye a su estimación:

| Integrante | Estimado (tablero) | Real declarado | Desviación | Desviación % |
|---|---|---|---|---|
| **Koldo** | 82 h (18 Issues, compartidas incluidas) | **50 h** | −32 h | −39,0 % |
| Carlos | 47 h (9 Issues) | `PENDIENTE` | — | — |
| Kenneth | 47 h (11 Issues, compartidas incluidas) | `PENDIENTE` | — | — |
| Sin asignar (#4, #21) | 9 h | `PENDIENTE` | — | — |
| **Total** | **170 h** | **50 h declaradas** | — | — |

Lectura del dato:

- Con las 50 h de Koldo, la desviación ya es **negativa (−32 h, −39 %)**: el tablero estimó bastante más de lo que costó de verdad, y sus Issues más grandes son precisamente las de Unity y Python (#22 Chunks Python 10 h, #5 Cifrado AES Python 10 h, #11 Descifrado Python 10 h).
- Se anota como **error de estimación**, no como error de reparto: el número de Issues y el reparto por plataforma no cambia, solo la duración real de cada una.
- Faltan Carlos, Kenneth y las tres Issues transversales (#1, #4, #21) para poder cerrar el total real frente a las 170 h estimadas. Mientras falten, la desviación global no se puede calcular.
- Comparado con el contrato (48 h por persona en las tres fases), las 50 h reales de Koldo quedan **a +2 h (+4 %)**, prácticamente en línea con lo comprometido.

Avisos para cerrar el dato:

- **Cinco Issues no tienen horas en el tablero**: #1 (aprobación de la propuesta), #38, #39, #47 y #48. Hasta que se completen, la suma está incompleta.
- **Descuadre de 24 h**: la cabecera de la columna *Done* del tablero marca «Horas: 146» y el sumatorio de las filas legibles da 170. Conviene revisar esos cinco huecos y los valores de las Issues compartidas antes de llevar la cifra al PDF.
- La estimación por Issue del tablero **no reparte igual que el contrato** (que daba 16 h por persona y fase, 48 h por persona): aquí Koldo concentra 82 h frente a 47 h de Carlos y 47 h de Kenneth. Esa diferencia es un hallazgo válido para el apartado 6, no algo que haya que "cuadrar" a posteriori.

**Cambios de alcance:** las 13 PR sin fusionar al cierre (#35, #44, #60, #65, #68, #70, #73, #74, #76–#78, #80, #81) son tareas abiertas o sustituidas por otra PR equivalente, no alcance nuevo. El único alcance añadido durante el reto es la automatización de revisión en CI (#69, #71), que no estaba en el contrato.

## 3. Seguimiento verificable

- **Dailies:** `PENDIENTE` de enlazar el registro de las jornadas trabajadas.
- **Issues:** https://github.com/dm2-ethazi/Reto2_Enigma/issues?q=is%3Aissue
- **Pull requests:** https://github.com/dm2-ethazi/Reto2_Enigma/pulls?q=is%3Apr
- **Commits:** https://github.com/dm2-ethazi/Reto2_Enigma/commits/develop

### Bloqueos y decisiones (extraídos del historial)

| Fecha | Bloqueo | Decisión | Evidencia |
|---|---|---|---|
| 04/11 | La PR #44 (selector de archivos Android) queda sin fusionar | El selector de Android se integra con la PR #46, fusionada el 05/11 | #44, #46 |
| 12/11 | Las PR hacia `develop` se fusionaban sin revisión | Añadir workflow de puerta de revisión y exigirlo en la protección de rama | #69, #71 |
| 12/11 | El CI solo cubría Python | Se documenta que el bloqueo por cobertura es solo de Python y el resto valida en local | `docs/GUIA_CI_CD.md` |
| 12/11 | Documentación repartida en cada rama de cliente | Centralizar los README en `docs/` | #75, #79 |
| 12/11 | Once PR del Sprint 2 quedan abiertas al cierre | Se prioriza cerrar el mínimo demostrable y documentar el resto | #60, #65, #68, #70, #73, #74, #76, #77, #78, #80, #81 |

### Burndown

- Sprint 1: `PENDIENTE` (imagen y datos de horas-persona restantes por jornada).
- Sprint 2: `PENDIENTE`.

Formato de datos para la hoja de cálculo: `jornada · horas pendientes reales · horas pendientes ideales (48 h repartidas)`.

## 4. Colaboración técnica

- **PR de cada fase que acabaron fusionadas:** 1 en Discovery (#36); 14 en Sprint 1 (#37, #40–#43, #45, #46, #49–#55); 18 en Sprint 2 (#56–#59, #61–#64, #66, #67, #69, #71, #72, #75, #79, #82–#84).
- **Revisiones registradas:** 6 aprobaciones, todas el 12/11 y todas posteriores a la puerta de revisión: #72 (11:44, koldoaso), #75 (11:53, koldoaso), #79 (12:04), #80 (12:15), #83 (12:24) y #84 (12:29 y 12:30, koldoaso y Kenalvsm). No consta ninguna revisión con cambios solicitados.
- **Pair programming:** los cuatro registros obligatorios están en [Pair-Programming](Pair-Programming), con la plantilla de evidencias (fecha, duración, Issue, roles, decisión técnica, resultado y enlace).

| Fase | Sesiones exigidas | Registradas |
|---|---|---|
| Sprint 1 | 2 | `PENDIENTE` (PP-01, PP-02) |
| Sprint 2 | 2 | `PENDIENTE` (PP-03, PP-04) |

## 5. Reviews y retrospectivas

Las ceremonias no dejan rastro en GitHub, así que las fechas que siguen son las de **cierre de cada fase** y lo demostrado se deduce de las PR fusionadas hasta esa fecha.

| Ceremonia | Cierre de fase | Qué se demostró (según PR fusionadas) | Qué funcionó | Qué no funcionó |
|---|---|---|---|---|
| Review Sprint 1 | 08/11/2026 | Camino funcional en los cuatro clientes: César, AES y selección de archivos (#43, #46, #49–#53) | Paralelizar por cliente mantuvo el ritmo: 38 commits y 14 PR de la fase fusionadas | Las PR se fusionaban sin revisión y el CI solo miraba Python |
| Retro Sprint 1 | 08/11/2026 | — | Backlog por cliente con Issues verificables | Sobrecarga de PR abiertas (#44 seguía abierta al cerrar) |
| Review Sprint 2 | 12/11/2026 | Vista previa JavaFX (#55), Unity con físicas y estados (#56, #57, #62), Python con CI (#58, #61), Android multidioma (#64) | Se cerró la integración y la documentación técnica | Última jornada con 34 commits y 29 PR creadas en el sprint: exceso de trabajo acumulado |
| Retro Sprint 2 | 12/11/2026 | — | Se aplicó una mejora de proceso verificable: revisión obligatoria antes de fusionar (6 aprobaciones ese mismo día) | La mejora llegó al final del reto |

**Mejora de proceso aplicada y su evidencia:** revisión obligatoria de otra persona antes de fusionar a `develop`; workflow `.github/workflows/puerta-revision.yml` y PR #69 y #71 (12/11), con 6 aprobaciones ese mismo día. Queda condicionada a que la protección de rama esté activada; si no lo está, debe explicarse aquí como mejora **no aplicada**.

## 6. Comparación con el contrato inicial

| Planificado (contrato) | Realidad (historial) | Qué haríamos distinto |
|---|---|---|
| 144 h-persona en tres fases de 48 h | 96 commits y 33 PR fusionadas en 14 jornadas de actividad (28/10 – 12/11) | Estimar con el reparto real por cliente y no por igual entre las cuatro apps |
| Un camino funcional por app al cerrar el Sprint 1 | Conseguido en los cuatro clientes, con la PR #44 pendiente | Cerrar el selector de archivos antes de abrir nuevas funcionalidades |
| Integración y pruebas en el Sprint 2 | Integración hecha; 13 PR quedaron sin fusionar (29 creadas en 4 días) | Reservar el último día solo a cerrar PR y documentación |
| CI/CD bloqueante | Solo Python bloquea por tests y cobertura; el resto valida en local | Definir desde Discovery qué bloquea cada cliente |
| Revisión de PR por otra persona | Las 6 únicas aprobaciones del historial son del 12/11, después de activar la puerta (#72, #75, #79, #80, #83, #84) | Activar la puerta de revisión desde el primer sprint |
| Documentación en el repositorio | Docs centralizados en `docs/` el último día (#75, #79) | Documentar cada PR al fusionarla, no al final |

## 7. Reflexión sobre las aportaciones

| Integrante | Contribución medida en el historial | Aprendizaje | Experiencia driver / navigator |
|---|---|---|---|
| Carlos | 25 commits; JavaFX completo (ventana, selector, cifrado AES, generador de contraseñas, chunks, vista previa) y la PR de integración de JavaFX a `develop` (#84) | `PENDIENTE` | `PENDIENTE` |
| Koldo | 38 commits; Python (Vigenère, AES/R2AE, CLI, CI, multiidioma, dailies locales), Unity (ventana, físicas, estados, adaptación al contrato) y la centralización de README | `PENDIENTE` | `PENDIENTE` |
| Kenneth | 33 commits; Android (AES de imágenes y César de textos, multidioma, límites de tamaño) y el CI/CD de revisión (#69, #71) | `PENDIENTE` | `PENDIENTE` |

> El informe interpreta las evidencias, no las duplica: cada afirmación se apoya en una Issue o PR enlazada y las desviaciones se explican con su causa.
