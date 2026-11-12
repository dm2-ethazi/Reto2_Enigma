# Fisicas 2D del pasillo

@author koldo

## Configuracion y justificacion (RA5)

- El enemigo usa `Rigidbody2D` **Kinematic**, `CapsuleCollider2D`, gravedad **0**,
  deteccion continua y contactos cinematicos completos. El juego controla exactamente
  los cuatro tramos; no necesitamos caidas, empujes ni una simulacion dinamica del actor.
- La puerta usa un `Rigidbody2D` **Static**, gravedad **0** y un `BoxCollider2D`
  con **Is Trigger**. El volumen esta colocado para que la primera entrada coincida
  con el centro del hueco. El personaje puede atravesarlo sin recibir un impulso fisico.
- Al menos uno de los dos objetos debe participar mediante `Rigidbody2D` para que
  Unity genere eventos de contacto 2D. Dos colliders estaticos sin rigidbody no son
  una forma fiable de obtener estos eventos. Aqui el cuerpo cinemático del enemigo
  realiza el movimiento que hace entrar su collider en el trigger estatico.
- `OnTriggerEnter2D` confirma la llegada y distingue la instancia asignada de otros
  cuerpos. `HasReachedDoor` y la transicion del modelo impiden descontar mas de una vida.
- Un **trigger** notifica solapamientos sin respuesta de choque. Una **colision solida**
  bloquea o transmite impulsos y se atenderia con `OnCollisionEnter2D`. No se usa esa
  variante en la puerta porque el dementor entra por ella.
- Las tres lamparas decorativas tienen cuerpos dinamicos con gravedad 0 y
  `HingeJoint2D`: la velocidad angular inicial y `AddTorque` producen su balanceo
  limitado a unos tres grados sin alterar el recorrido del enemigo.

## Movimiento (RA2 y RA4)

`DementorMotor2D.FixedUpdate` emplea `Rigidbody2D.MovePosition` y
`Time.fixedDeltaTime`. Cada fallo aumenta la velocidad un 10 %, con un maximo
configurable, conservando los mismos cuatro destinos y evitando ordenes acumuladas.
El tutorial, la pausa y los estados finales bloquean el avance.

## Prefabs y referencias (RA3)

- `Assets/Prefabs/Characters/Dementor.prefab`: cuerpo, collider, animacion, aura y estela.
- `Assets/Prefabs/Environment/CorridorDoor.prefab`: arte, cuerpo estatico, trigger y centro de paso.
- `Enemies/DementorSpawner`: prefab, instancia inicial y recorrido de la escena.
- `Physics · Corridor/Dementor Route`: inicio y cuatro destinos editables en Scene View.

Los prefabs no guardan referencias a objetos externos de la escena. `DementorSpawner`
conecta el recorrido y la puerta al crear cada instancia. No se construye el escenario
por codigo: posiciones, arte, componentes y parametros quedan preparados en el Editor.

## Demostracion sin intervencion tecnica

1. Abre `Assets/Scenes/EnigmaGame.unity` y entra en Play.
2. Lee el ejemplo del tutorial y pulsa **Comenzar**.
3. Introduce una respuesta incorrecta: observa un tramo de avance y su penalizacion de velocidad.
4. Tras cuatro errores entra por la puerta y se pierde una vida; aparece un prefab nuevo al inicio.
5. Con tres intentos fallidos aparece **Derrota**. **Reiniciar** recarga la escena y el tutorial.
6. Resuelve las cinco rondas para mostrar **Victoria**. En ambos finales se bloquea la entrada.

Pruebas locales: `Assets/Tests/PlayMode/CorridorPhysicsTests.cs` y
`Assets/Tests/Game/CampaignSceneTests.cs`.
