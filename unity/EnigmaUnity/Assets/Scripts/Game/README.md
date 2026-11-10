# Partida CÃ©sar



@author koldo



## Estructura



- `Configuration`: assets de nivel y campaÃ±a; frases, desplazamientos, vidas y duraciÃ³n de resoluciÃ³n.

- `Model`: cifrado y reglas de la campaÃ±a sin referencias a objetos de escena.

- `Flow`: coordinaciÃ³n de respuestas, movimiento, llegada, pausa y resoluciÃ³n.

- `Clues`: elecciÃ³n de un sÃ­mbolo y uno de los seis puntos ya guardados en la escena.

- `Presentation`: reproducciÃ³n y reinicio de la animaciÃ³n existente del dementor.

  `DoorPassage` contrae y desvanece al personaje hacia `Door Â· Passage Center` al llegar a la puerta;

  la desintegraciÃ³n se reserva para las respuestas correctas. El punto de llegada y el de desapariciÃ³n

  se editan en la escena, y el sprite del dementor se dibuja delante de la puerta.

  `DementorAura` detiene los emisores al resolver un intento y los limpia al reiniciarlo.

  El prefab `Assets/AuraDementor.prefab` configura la niebla y la escarcha en espacio local,

  con escala jerÃ¡rquica y el material `DementorMist`, que usa `Enigma/Dementor Mist 2D`.

- `Assets/Scripts/UI/Game`: vÃ­nculo de los controles UXML con la partida.

- `Assets/UI/Game/Styles`: presentaciÃ³n del HUD, respuesta, pista y menÃºs.



## Configurar en el Editor



Abre `SampleScene`. El objeto `Game Â· Campaign` contiene las referencias a la campaÃ±a,

el motor, la puerta, la animaciÃ³n, la pista y la cÃ¡mara. `Assets/GameData/Campaign.asset`

ordena los cinco assets de `Assets/GameData/Levels`; cada uno permite cambiar sus frases,

el intervalo de desplazamientos y el lÃ­mite de vidas desde el Inspector.



`CluePlacement` tiene seis sprites del atlas `Assets/Sprites/Clues/ClueSymbols.png` y las

seis referencias de `Clues Â· Six Authored Spots`. Mueve esos puntos en Scene View para

editar posiciones. Cada nivel elige un sÃ­mbolo y una posiciÃ³n diferentes de los anteriores;

el reintento conserva el mensaje, desplazamiento y pista del nivel actual. No se generan

GameObjects, prefabs ni posiciones aleatorias fuera de los puntos preparados.



## Reglas y controles



Cinco rondas; lÃ­mites de vidas 3, 3, 2, 2 y 1. Cada error mueve al dementor un paso.

El cuarto llega al trigger de la puerta y descuenta una vida. Una respuesta correcta

reproduce la desintegraciÃ³n y abre el siguiente nivel. La pista revela el desplazamiento

al tocar el sÃ­mbolo. Las dos flechas ajustan el alfabeto, sin resolver la respuesta por sÃ­ solas.



La entrada usa un TextField de UI Toolkit, con teclado mÃ³vil y validaciÃ³n mediante el botÃ³n

o Intro en Windows. Pausa congela la fÃ­sica y los temporizadores. Los menÃºs permiten continuar,

reiniciar o salir; salir termina Play Mode cuando se ejecuta en el Editor.



Pruebas en `Assets/Tests/Game`: reglas, victoria, derrota, reintento, selecciÃ³n de pistas,

contacto fÃ­sico, pausa y posiciÃ³n del Ã¡rea tÃ¡ctil.



## Estela de sombra



`DementorShadowTrail` emite siluetas solo durante los avances. El objeto

`Dementor/VFX · Shadow Trail` y su material estan preparados en la escena.

Las particulas permanecen en coordenadas del mundo y se desvanecen sin seguir al actor.

El Inspector permite ajustar distancia entre siluetas, duracion y color.

La resolucion detiene la emision y cada reinicio elimina las sombras anteriores.


## Temporizador de ronda

`Model/RoundTimer.cs` cuenta el tiempo sin depender de Unity. `GameSession` aplica
una sola penalizacion al agotarse y `CampaignController` inicia el avance fisico.
Cada asset `LevelDefinition` tiene `Time Limit Seconds`: 50, 45, 40, 35 y 30
segundos en las cinco rondas actuales. Tras un avance o reintento, se recupera
el tiempo completo de esa ronda. Pausa, movimiento, resolucion y fin de partida
detienen la cuenta. El cuarto fallo por tiempo sigue esperando a la puerta
para descontar una vida, igual que una respuesta incorrecta.

El reloj del HUD usa UXML/USS y cambia de color en los ultimos 10 segundos.
`GameHudController` solo actualiza la etiqueta cuando cambia el segundo visible.

## Audio

`Presentation/GameAudio.cs` controla el AudioSource preparado bajo `Audio - Game`.
El efecto del dementor solo se dispara al acertar. Respeta la pausa y se detiene
al reiniciar. El WAV original y sus instrucciones estan en `Assets/Audio`.
