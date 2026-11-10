# Física del pasillo

La escena `Assets/Scenes/SampleScene.unity` contiene todos los cuerpos, colliders,
articulaciones, referencias y posiciones. Los scripts no generan el escenario al iniciar.

- `Dementor/DementorMotor2D.cs`: avance cinemático por cuatro posiciones y reinicio al inicio.
- `Door/DoorTrigger2D.cs`: detección de llegada del actor asignado y evento configurable en el Inspector.
- `Environment/HangingLampSway2D.cs`: brisa mediante par físico sobre las lámparas con articulación de bisagra.

## Probar desde el Editor

1. Abre `SampleScene` y entra en Play Mode.
2. Selecciona `Dementor`. En el menú contextual de `DementorMotor2D`, ejecuta
   **Física/Avanzar una posición**. Espera a que termine antes del siguiente avance.
3. En el cuarto avance, `Door · Arrival Trigger` detecta al dementor y lo detiene.
4. Usa **Física/Volver al inicio** para reiniciar el intento.

Los botones del alfabeto siguen controlando únicamente el cifrado. La conexión de
errores, rondas y vidas con `AdvanceOneStep()` queda para el módulo de juego.

## Ajustes y referencias

- `Physics · Corridor/Dementor Route`: inicio y cuatro destinos editables.
- `Physics · Corridor/Door · Arrival Trigger`: tamaño y posición de la zona de llegada.
- Cada `Lampara de cadena`: `Rigidbody2D`, `BoxCollider2D`, `HingeJoint2D` con límites
  de ±3 grados y brisa editable. Su luz es hija del mismo objeto y acompaña el movimiento.
- `Pasillo del castillo/Clues · Six Authored Spots`: seis posiciones para el futuro sistema de pistas.

Las pruebas de Play Mode están en `Assets/Tests/PlayMode/CorridorPhysicsTests.cs`.

@author koldo
