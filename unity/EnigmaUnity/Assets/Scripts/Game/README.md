# Partida Cesar

@author koldo

## Modulos

- `Configuration`: assets de campaña y nivel, con tres vidas iniciales, frases, desplazamientos y tiempos.
- `Model`: `GameSession`, `RoundTimer` y `CaesarCipher`, independientes de la interfaz y de los objetos de escena.
- `Flow`: `CampaignController` coordina las acciones y `DementorSpawner` destruye e instancia prefabs.
- `Clues`: elige un simbolo y una de seis posiciones preparadas en el Editor.
- `Presentation`: agrupa el prefab enemigo, sus animaciones, puerta, aura, estela y sonido de acierto.
- `Scripts/UI/Game`: adapta el modelo a UXML/USS sin comparar respuestas desde la vista.

## Estados y flujo

`GamePhase` distingue `Tutorial`, `Ready` (jugando), `Advancing`, `Resolving`,
`Won` (victoria) y `Lost` (derrota). La escena empieza en tutorial con un ejemplo,
reglas de normalizacion y el boton Comenzar. El tiempo y el enemigo esperan al jugador.
Cada fallo mueve una de cuatro posiciones. La llegada fisica del cuarto fallo descuenta
exactamente una vida; la animacion termina, se destruye el enemigo y se instancia el prefab
para repetir el mismo desafio. Un acierto crea la ronda siguiente y otro prefab.
Las tres vidas se conservan durante toda la campaña: los aciertos no recuperan vidas
ni las rondas reducen su limite. Doce fallos de puerta producen derrota.

Al terminar no hay enemigo activo y el estado bloquea validacion, movimiento e input.
**Reiniciar** llama a `SceneManager.LoadScene` y devuelve todos los objetos y el tutorial
a su estado inicial. **Salir** cierra la aplicacion; en el Editor termina Play Mode.

## Desafios y pistas

Cinco rondas con desplazamientos 1-3, 1-5, 1-10, 1-17 y 1-25. Los bancos
progresan desde 4-6 letras hasta 18-25 letras, sin contar espacios. La primera
pista esta visible; las siguientes son opcionales y no muestran la respuesta completa.
Los seis puntos de pista y el recorrido se editan en la escena; no hay posiciones
ni recursos construidos al iniciar.

## Validacion

`Normalize` aplica `Trim()`, mayusculas invariantes, espacios consecutivos y equivalencia
de vocales acentuadas. Preserva la Ñ: NIÑO y NINO son distintos. `Encode` desplaza
exclusivamente A-Z y conserva espacios, puntuacion y cualquier caracter fuera del alfabeto.
Una entrada vacia avisa sin penalizar. Se normaliza para aceptar escrituras equivalentes
sin exigir capitalizacion o espacios exactos.

## Temporizador y presentacion

Cada nivel tiene 50, 45, 40, 35 o 30 segundos, editables en el Inspector.
El tiempo agotado cuenta como un fallo y provoca un avance. Pausa, movimiento,
resolucion y final detienen la cuenta. Un avance o reintento recupera el tiempo del nivel.
El HUD muestra vidas, ronda, fallos, reloj y barra de avances restantes.
La puerta conserva su efecto de contraccion al centro. El efecto sonoro se reproduce
solo al acertar, y respeta la pausa. No se utiliza sonido de ambiente.

## Evidencias de la rubrica

La justificacion de cuerpos, gravedad, triggers y colisiones esta en `Scripts/Physics/README.md`.
Las pruebas cubren tutorial, normalizacion y Ñ, cuatro errores por vida, doce errores,
conservacion de vidas, sustitucion real de prefabs, victoria, derrota, bloqueo,
recarga de escena, penalizacion de velocidad, pausa, reloj, UI y efectos.
Las posiciones y componentes de los dos prefabs se pueden inspeccionar en Unity.
