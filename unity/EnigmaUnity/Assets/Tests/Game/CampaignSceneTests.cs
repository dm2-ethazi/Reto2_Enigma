using System.Collections;
using Enigma.Game;
using Enigma.Physics;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.TestTools;
using UnityEngine.UIElements;

namespace Enigma.Tests.Game
{
    /// <summary>Verifica tutorial, prefabs, fisica, estados finales, interfaz y recarga de la escena real.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class CampaignSceneTests
    {
        private CampaignController game;
        private VisualElement Root => Object.FindFirstObjectByType<UIDocument>().rootVisualElement;

        [UnitySetUp]
        public IEnumerator LoadGame()
        {
            Time.timeScale = 1f;
            yield return SceneManager.LoadSceneAsync("EnigmaGame");
            yield return null;
            yield return null;
            game = Object.FindFirstObjectByType<CampaignController>();
        }

        [TearDown]
        public void RestoreTime() => Time.timeScale = 1f;

        /// <summary>Usa la accion publica que tambien invoca el boton Comenzar.</summary>
        private void StartGame()
        {
            game.BeginPlaying();
            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Ready));
        }

        private static void Click(Button button)
        {
            var click = typeof(Clickable).GetMethod("SimulateSingleClick",
                System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic |
                System.Reflection.BindingFlags.Public);
            click.Invoke(button.clickable, new object[] { null, 0 });
        }

        [UnityTest]
        public IEnumerator TutorialBlocksInputTimeAndMovementUntilTheStartButton()
        {
            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Tutorial));
            Assert.That(game.Clue.Revealed, Is.True);
            Assert.That(Root.Q<TextField>("answerInput").enabledInHierarchy, Is.False);
            Assert.That(Root.Q<Label>("modalMessage").text, Does.Contain("IPMB"));
            float time = game.Session.RemainingSeconds;
            game.SubmitAnswer("ERROR");
            game.Enemy.Motor.AdvanceOneStep();
            yield return new WaitForSeconds(0.15f);
            Assert.That(game.Session.RemainingSeconds, Is.EqualTo(time));
            Assert.That(game.Enemy.Motor.StepsTaken, Is.Zero);
            Click(Root.Q<Button>("resumeGame"));
            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Ready));
            Assert.That(Root.Q<TextField>("answerInput").enabledInHierarchy, Is.True);
        }

        [UnityTest]
        public IEnumerator FiveCorrectAnswersReplaceTheEnemyAndBlockAfterVictory()
        {
            StartGame();
            for (int round = 0; round < 5; round++)
            {
                var previous = game.Enemy;
                int previousId = previous.GetInstanceID();
                game.SubmitAnswer(CaesarCipher.Encode(game.Session.CipherText, -game.Session.Shift));
                Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Resolving));
                yield return WaitForResolution();
                yield return null;
                Assert.That(previous == null, Is.True, "La instancia anterior debe destruirse.");
                Assert.That(game.Session.Lives, Is.EqualTo(3));
                if (round < 4)
                {
                    Assert.That(game.Enemy.GetInstanceID(), Is.Not.EqualTo(previousId));
                    Assert.That(game.Enemy.Motor.StepsTaken, Is.Zero);
                }
            }

            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Won));
            Assert.That(game.Enemy == null, Is.True);
            Assert.That(Root.Q<TextField>("answerInput").enabledInHierarchy, Is.False);
            Assert.That(Root.Q<Button>("submitAnswer").enabledInHierarchy, Is.False);
            Assert.That(Root.Q<Button>("nextLetter").enabledInHierarchy, Is.False);
            game.SubmitAnswer("ERROR");
            Assert.That(game.Session.Errors, Is.Zero);
            Assert.That(game.Session.Tick(500f), Is.False);
        }

        [UnityTest]
        public IEnumerator ThreeDoorAttemptsLoseExactlyThreeLivesAndEndInDefeat()
        {
            StartGame();
            string cipher = game.Session.CipherText;
            int shift = game.Session.Shift;
            int spot = game.Clue.SpotIndex;
            int arrivals = 0;
            Object.FindFirstObjectByType<DoorTrigger2D>().DementorReached.AddListener(() => arrivals++);
            for (int attempt = 0; attempt < 3; attempt++)
            {
                var previous = game.Enemy;
                for (int error = 0; error < 4; error++)
                {
                    game.SubmitAnswer("ERROR");
                    yield return WaitForMovement();
                }
                Assert.That(arrivals, Is.EqualTo(attempt + 1));
                Assert.That(game.Session.Lives, Is.EqualTo(2 - attempt));
                yield return WaitForResolution();
                yield return null;
                Assert.That(previous == null, Is.True);
                if (attempt < 2)
                {
                    Assert.That(game.Enemy, Is.Not.Null);
                    Assert.That(game.Session.CipherText, Is.EqualTo(cipher));
                    Assert.That(game.Session.Shift, Is.EqualTo(shift));
                    Assert.That(game.Clue.SpotIndex, Is.EqualTo(spot));
                    Assert.That(game.Session.Errors, Is.Zero);
                }
            }
            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Lost));
            Assert.That(game.Enemy == null, Is.True);
            Assert.That(Root.Q<TextField>("answerInput").enabledInHierarchy, Is.False);
            game.SubmitAnswer("ERROR");
            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Lost));
        }

        [UnityTest]
        public IEnumerator RestartReloadsTheSceneAndReturnsToTheTutorial()
        {
            StartGame();
            var previousController = game;
            var previousActor = game.Enemy;
            game.SubmitAnswer("ERROR");
            game.TogglePause();
            game.RestartCampaign();
            yield return null;
            yield return null;
            game = Object.FindFirstObjectByType<CampaignController>();
            Assert.That(previousController == null, Is.True);
            Assert.That(previousActor == null, Is.True);
            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Tutorial));
            Assert.That(game.Session.Lives, Is.EqualTo(3));
            Assert.That(game.Session.Errors, Is.Zero);
            Assert.That(Time.timeScale, Is.EqualTo(1f));
            Assert.That(Object.FindObjectsByType<DementorMotor2D>(FindObjectsSortMode.None).Length, Is.EqualTo(1));
            Assert.That(Root.Q<TextField>("answerInput").enabledInHierarchy, Is.False);
        }

        [UnityTest]
        public IEnumerator PenaltiesIncreaseSpeedButKeepDiscretePhysicsTargets()
        {
            StartGame();
            var motor = game.Enemy.Motor;
            float original = motor.CurrentSpeed;
            game.SubmitAnswer("ERROR");
            Assert.That(motor.CurrentSpeed, Is.GreaterThan(original));
            Assert.That(Root.Q<TextField>("answerInput").enabledInHierarchy, Is.False);
            yield return WaitForMovement();
            Assert.That(motor.GetComponent<Rigidbody2D>().position.x,
                Is.EqualTo(GameObject.Find("Step 1").transform.position.x).Within(0.03f));
            float first = motor.CurrentSpeed;
            game.SubmitAnswer("ERROR");
            Assert.That(motor.CurrentSpeed, Is.GreaterThan(first));
            yield return WaitForMovement();
            Assert.That(motor.GetComponent<Rigidbody2D>().position.x,
                Is.EqualTo(GameObject.Find("Step 2").transform.position.x).Within(0.03f));
            Assert.That(motor.StepsTaken, Is.EqualTo(2));
            Assert.That(Root.Q<ProgressBar>("doorProgress").value, Is.EqualTo(2f));
        }

        [UnityTest]
        public IEnumerator DoorPassageReachesItsCentreBeforeTheEnemyIsReplaced()
        {
            StartGame();
            for (int error = 0; error < 3; error++)
            {
                game.SubmitAnswer("ERROR");
                yield return WaitForMovement();
            }
            var actor = game.Enemy;
            var visual = actor.transform.Find("Visual");
            var artwork = visual.GetComponentInChildren<SpriteRenderer>();
            Vector3 originalScale = visual.localScale;
            game.SubmitAnswer("ERROR");
            yield return WaitForMovement();
            Assert.That(actor.Motor.GetComponent<Rigidbody2D>().position.x,
                Is.EqualTo(GameObject.Find("Step 4 · Door").transform.position.x).Within(0.03f));
            yield return new WaitForSeconds(0.25f);
            Assert.That(visual.localScale.x, Is.LessThan(originalScale.x).And.GreaterThan(0));
            yield return new WaitForSeconds(0.55f);
            Assert.That(artwork.enabled, Is.False);
            Assert.That(Vector3.Distance(visual.position, GameObject.Find("Door · Passage Center").transform.position), Is.LessThan(0.02f));
            yield return WaitForResolution();
            yield return null;
            Assert.That(actor == null, Is.True);
            Assert.That(game.Enemy.transform.Find("Visual").localScale, Is.EqualTo(originalScale));
        }

        [UnityTest]
        public IEnumerator HudAcceptsAnswersAndShowsClueFeedbackAndRemainingAdvances()
        {
            StartGame();
            var answer = Root.Q<TextField>("answerInput");
            answer.value = "   ";
            Click(Root.Q<Button>("submitAnswer"));
            Assert.That(game.Session.Errors, Is.Zero);
            Assert.That(game.Feedback, Does.Contain("Escribe"));
            Click(Root.Q<Button>("clueHitArea"));
            Assert.That(game.Clue.Revealed, Is.False);
            Click(Root.Q<Button>("clueHitArea"));
            Assert.That(Root.Q<Label>("clueFeedback").text, Does.Contain(game.Session.Shift.ToString()));
            answer.value = "  " + CaesarCipher.Encode(game.Session.CipherText, -game.Session.Shift).ToLowerInvariant() + "  ";
            Click(Root.Q<Button>("submitAnswer"));
            yield return WaitForResolution();
            Assert.That(game.Session.LevelIndex, Is.EqualTo(1));
            Assert.That(answer.value, Is.Empty);
        }

        [UnityTest]
        public IEnumerator PauseFreezesPhysicsTimeAndBlocksInput()
        {
            StartGame();
            game.SubmitAnswer("ERROR");
            yield return new WaitForFixedUpdate();
            game.TogglePause();
            var body = game.Enemy.Motor.GetComponent<Rigidbody2D>();
            Vector2 position = body.position;
            float remaining = game.Session.RemainingSeconds;
            int errors = game.Session.Errors;
            yield return new WaitForSecondsRealtime(0.15f);
            Assert.That(body.position, Is.EqualTo(position));
            Assert.That(game.Session.RemainingSeconds, Is.EqualTo(remaining));
            Assert.That(Root.Q<TextField>("answerInput").enabledInHierarchy, Is.False);
            game.SubmitAnswer("ERROR");
            Assert.That(game.Session.Errors, Is.EqualTo(errors));
            game.TogglePause();
            yield return WaitForMovement();
        }

        [UnityTest]
        public IEnumerator TimerPausesAndExpirationStartsOnePhysicalAdvance()
        {
            StartGame();
            game.Session.Tick(game.Session.TimeLimitSeconds - 0.2f);
            game.TogglePause();
            float remaining = game.Session.RemainingSeconds;
            yield return new WaitForSecondsRealtime(0.3f);
            Assert.That(game.Session.RemainingSeconds, Is.EqualTo(remaining));
            game.TogglePause();
            float deadline = Time.realtimeSinceStartup + 3f;
            while (game.Session.Errors == 0 && Time.realtimeSinceStartup < deadline) yield return null;
            Assert.That(game.Session.Errors, Is.EqualTo(1));
            Assert.That(game.Enemy.Motor.StepsTaken, Is.EqualTo(1));
            Assert.That(game.Feedback, Does.Contain("Tiempo agotado"));
        }

        [UnityTest]
        public IEnumerator ShadowTrailStaysInWorldSpaceAndDisappearsWhenActorIsDestroyed()
        {
            StartGame();
            var actor = game.Enemy;
            var trail = actor.transform.Find("VFX \u00b7 Shadow Trail").GetComponent<ParticleSystem>();
            Assert.That(trail.main.simulationSpace, Is.EqualTo(ParticleSystemSimulationSpace.World));
            game.SubmitAnswer("ERROR");
            float deadline = Time.realtimeSinceStartup + 2f;
            while (trail.particleCount == 0 && Time.realtimeSinceStartup < deadline) yield return null;
            Assert.That(trail.particleCount, Is.GreaterThan(0));
            var particles = new ParticleSystem.Particle[trail.main.maxParticles];
            trail.GetParticles(particles);
            Vector3 position = particles[0].position;
            uint seed = particles[0].randomSeed;
            yield return new WaitForSeconds(0.08f);
            int count = trail.GetParticles(particles);
            bool found = false;
            for(int index = 0; index < count; index++)
            {
                if(particles[index].randomSeed != seed) continue;
                found = true;
                Assert.That(Vector3.Distance(position, particles[index].position), Is.LessThan(0.001f));
            }
            Assert.That(found, Is.True);
            yield return WaitForMovement();
            game.SubmitAnswer(CaesarCipher.Encode(game.Session.CipherText, -game.Session.Shift));
            yield return WaitForResolution();
            yield return null;
            Assert.That(actor == null, Is.True);
            Assert.That(game.Enemy.transform.Find("VFX \u00b7 Shadow Trail").GetComponent<ParticleSystem>().particleCount, Is.Zero);
        }

        [UnityTest]
        public IEnumerator AudioPlaysOnlyOnCorrectAnswersAndRespectsPause()
        {
            StartGame();
            var audio = Object.FindFirstObjectByType<GameAudio>();
            var source = audio.GetComponentInChildren<AudioSource>();
            Assert.That(audio.GetComponentsInChildren<AudioSource>().Length, Is.EqualTo(1));
            game.SubmitAnswer("ERROR");
            Assert.That(source.isPlaying, Is.False);
            yield return WaitForMovement();
            game.SubmitAnswer(CaesarCipher.Encode(game.Session.CipherText, -game.Session.Shift));
            Assert.That(source.isPlaying, Is.True);
            game.TogglePause();
            Assert.That(source.isPlaying, Is.False);
            game.TogglePause();
            Assert.That(source.isPlaying, Is.True);
            yield return null;
        }

        [Test]
        public void ClueSelectionUsesSixSpotsAndAvoidsConsecutiveRepeats()
        {
            for(int index = 0; index < 20; index++)
            {
                int spot = game.Clue.SpotIndex;
                int symbol = game.Clue.SymbolIndex;
                game.Clue.SelectForLevel();
                Assert.That(game.Clue.SpotIndex, Is.InRange(0,5).And.Not.EqualTo(spot));
                Assert.That(game.Clue.SymbolIndex, Is.InRange(0,5).And.Not.EqualTo(symbol));
            }
        }

        /// <summary>Espera a que el controlador reabra la respuesta o empiece la resolucion de puerta.</summary>
        private IEnumerator WaitForMovement()
        {
            float deadline = Time.realtimeSinceStartup + 4f;
            while(game.Session.Phase == GamePhase.Advancing && Time.realtimeSinceStartup < deadline)
                yield return null;
            Assert.That(game.Session.Phase, Is.Not.EqualTo(GamePhase.Advancing));
        }

        private IEnumerator WaitForResolution()
        {
            float deadline = Time.realtimeSinceStartup + 4f;
            while(game.Session.Phase == GamePhase.Resolving && Time.realtimeSinceStartup < deadline)
                yield return null;
            Assert.That(game.Session.Phase, Is.Not.EqualTo(GamePhase.Resolving));
        }
    }
}
