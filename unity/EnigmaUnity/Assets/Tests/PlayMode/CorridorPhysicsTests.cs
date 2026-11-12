using System.Collections;
using Enigma.Physics;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.TestTools;

namespace Enigma.Tests.Physics
{
    /// <summary>Comprueba los componentes físicos y sus referencias en la escena real del pasillo.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class CorridorPhysicsTests
    {
        private DementorMotor2D motor;

        /// <summary>Carga la escena guardada para que cada prueba empiece con su configuración del Editor.</summary>
        [UnitySetUp]
        public IEnumerator LoadCorridor()
        {
            Time.timeScale = 1f;
            yield return SceneManager.LoadSceneAsync("EnigmaGame", LoadSceneMode.Single);
            yield return new WaitForFixedUpdate();
            motor = Object.FindFirstObjectByType<DementorMotor2D>();
            Assert.That(motor, Is.Not.Null);
            // Aisla la fisica del tutorial, que bloquea el motor durante el juego normal.
            motor.enabled = true;
        }

        /// <summary>Verifica que el actor conserva su cuerpo cinemático y que existen seis pistas preparadas.</summary>
        [Test]
        public void SceneContainsAuthoredPhysicsAndSixClueSpots()
        {
            var body = motor.GetComponent<Rigidbody2D>();
            Assert.That(body.bodyType, Is.EqualTo(RigidbodyType2D.Kinematic));
            Assert.That(body.gravityScale, Is.Zero);
            Assert.That(motor.GetComponent<CapsuleCollider2D>().enabled, Is.True);
            Assert.That(GameObject.Find("Clues · Six Authored Spots").transform.childCount, Is.EqualTo(6));
            Assert.That(GameObject.Find("Dementor Route").transform.childCount, Is.EqualTo(5));
            Assert.That(Object.FindFirstObjectByType<DoorTrigger2D>().GetComponent<BoxCollider2D>().isTrigger, Is.True);
        }

        /// <summary>Verifica un avance físico y que las pulsaciones durante el movimiento no acumulen pasos.</summary>
        [UnityTest]
        public IEnumerator OneAdvanceStopsAtTheAuthoredPoint()
        {
            var body = motor.GetComponent<Rigidbody2D>();
            Vector2 start = body.position;
            motor.AdvanceOneStep();
            motor.AdvanceOneStep();
            yield return WaitForMovement();

            var target = GameObject.Find("Step 1").transform.position;
            Assert.That(body.position.x, Is.EqualTo(target.x).Within(0.03f));
            Assert.That(body.position.y, Is.EqualTo(start.y).Within(0.03f));
            Assert.That(motor.StepsTaken, Is.EqualTo(1));
            Assert.That(motor.HasReachedDoor, Is.False);
        }

        /// <summary>Comprueba la llegada por trigger y un segundo intento después de reiniciar.</summary>
        [UnityTest]
        public IEnumerator DoorFiresOncePerAttemptAndResetAllowsAnotherArrival()
        {
            int arrivals = 0;
            var door = Object.FindFirstObjectByType<DoorTrigger2D>();
            door.DementorReached.AddListener(() => arrivals++);

            for (int attempt = 0; attempt < 2; attempt++)
            {
                for (int step = 0; step < 4; step++)
                {
                    motor.AdvanceOneStep();
                    yield return WaitForMovement();
                }

                yield return new WaitForFixedUpdate();
                Assert.That(motor.HasReachedDoor, Is.True);
                Assert.That(arrivals, Is.EqualTo(attempt + 1));
                motor.AdvanceOneStep();
                Assert.That(motor.IsMoving, Is.False);
                motor.ResetToStart();
                yield return new WaitForFixedUpdate();
                Assert.That(motor.HasReachedDoor, Is.False);
                Assert.That(motor.StepsTaken, Is.Zero);
            }
        }

        /// <summary>Comprueba el balanceo real, los anclajes del techo y las luces hijas de cada lámpara.</summary>
        [UnityTest]
        public IEnumerator LampsSwayWithinTheirLimitsAndKeepTheirLightsAttached()
        {
            var lamps = Object.FindObjectsByType<HangingLampSway2D>(FindObjectsSortMode.None);
            Assert.That(lamps.Length, Is.EqualTo(3));
            yield return new WaitForSeconds(1f);

            foreach (var lamp in lamps)
            {
                var joint = lamp.GetComponent<HingeJoint2D>();
                Assert.That(Mathf.Abs(joint.jointAngle), Is.GreaterThan(0.005f));
                Assert.That(Mathf.Abs(joint.jointAngle), Is.LessThan(3.5f));
                Vector2 anchor = lamp.transform.TransformPoint(joint.anchor);
                Assert.That(Vector2.Distance(anchor, joint.connectedAnchor), Is.LessThan(0.03f));
                Assert.That(lamp.transform.childCount, Is.GreaterThanOrEqualTo(1));
                Assert.That(lamp.transform.GetChild(0).name, Does.StartWith("Luz calida"));
            }
        }

        /// <summary>Espera al final del avance con un límite que evita bloquear el ejecutor de pruebas.</summary>
        private IEnumerator WaitForMovement()
        {
            float deadline = Time.realtimeSinceStartup + 5f;

            while (motor.IsMoving && Time.realtimeSinceStartup < deadline)
                yield return new WaitForFixedUpdate();

            yield return new WaitForFixedUpdate();
            Assert.That(motor.IsMoving, Is.False, "El dementor no terminó el avance en cinco segundos.");
        }
    }
}
