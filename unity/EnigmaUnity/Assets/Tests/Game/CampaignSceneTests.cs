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
    /// <summary>Comprueba la integración de la partida con las pistas, la animación, la UI y la física.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class CampaignSceneTests
    {
        private CampaignController game;

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

        [Test]
        public void ClueSelectionUsesSixSpotsAndAvoidsConsecutiveRepeats()
        {
            for (int index = 0; index < 30; index++)
            {
                int spot = game.Clue.SpotIndex;
                int symbol = game.Clue.SymbolIndex;
                game.Clue.SelectForLevel();
                Assert.That(game.Clue.SpotIndex, Is.InRange(0, 5).And.Not.EqualTo(spot));
                Assert.That(game.Clue.SymbolIndex, Is.InRange(0, 5).And.Not.EqualTo(symbol));
                Assert.That(game.Clue.Revealed, Is.False);
            }
        }

        [UnityTest]
        public IEnumerator CorrectAnswerWinsAllRoundsAndReusesTheActor()
        {
            int actorId = Object.FindFirstObjectByType<DementorMotor2D>().GetInstanceID();
            for (int index = 0; index < 5; index++)
            {
                string answer = CaesarCipher.Encode(game.Session.CipherText, -game.Session.Shift);
                game.SubmitAnswer(answer);
                Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Resolving));
                yield return WaitForResolution();
            }

            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Won));
            game.RestartCampaign();
            Assert.That(game.Session.LevelIndex, Is.Zero);
            Assert.That(Object.FindFirstObjectByType<DementorMotor2D>().GetInstanceID(), Is.EqualTo(actorId));
            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Ready));
        }

        [UnityTest]
        public IEnumerator FourWrongAnswersUsePhysicsAndPreserveTheClueOnRetry()
        {
            string cipher = game.Session.CipherText;
            int spot = game.Clue.SpotIndex;
            int symbol = game.Clue.SymbolIndex;
            for (int index = 0; index < 4; index++)
            {
                game.SubmitAnswer("RESPUESTA INCORRECTA");
                float deadline = Time.realtimeSinceStartup + 5f;
                while (game.Session.Phase == GamePhase.Advancing && Time.realtimeSinceStartup < deadline)
                    yield return new WaitForFixedUpdate();

                Assert.That(game.Session.Phase, Is.Not.EqualTo(GamePhase.Advancing));
            }

            yield return WaitForResolution();
            Assert.That(game.Session.Lives, Is.EqualTo(2));
            Assert.That(game.Session.CipherText, Is.EqualTo(cipher));
            Assert.That(game.Clue.SpotIndex, Is.EqualTo(spot));
            Assert.That(game.Clue.SymbolIndex, Is.EqualTo(symbol));
            Assert.That(game.Session.Errors, Is.Zero);
        }

        [UnityTest]
        public IEnumerator AudioPlaysSuccessOnlyForCorrectAnswersAndRespectsPauseAndRestart()
        {
            var audio = Object.FindFirstObjectByType<GameAudio>();
            var success = audio.transform.Find("Dementor Defeated").GetComponent<AudioSource>();
            Assert.That(success.isPlaying, Is.False);
            Assert.That(audio.GetComponentsInChildren<AudioSource>().Length, Is.EqualTo(1));
            Assert.That(success.loop, Is.False);

            game.SubmitAnswer("RESPUESTA INCORRECTA");
            Assert.That(success.isPlaying, Is.False);
            game.RestartCampaign();
            string answer = CaesarCipher.Encode(game.Session.CipherText, -game.Session.Shift);
            game.SubmitAnswer(answer);
            yield return null;
            Assert.That(success.isPlaying, Is.True);

            game.TogglePause();
            Assert.That(success.isPlaying, Is.False);
            game.TogglePause();
            Assert.That(success.isPlaying, Is.True);
            game.RestartCampaign();
            Assert.That(success.isPlaying, Is.False);
        }

        [UnityTest]
        public IEnumerator TimerPausesAndExpirationStartsOnePhysicalAdvance()
        {
            var motor = Object.FindFirstObjectByType<DementorMotor2D>();
            game.Session.Tick(game.Session.TimeLimitSeconds - 0.2f);
            game.TogglePause();
            float remaining = game.Session.RemainingSeconds;
            yield return new WaitForSecondsRealtime(0.3f);
            Assert.That(game.Session.RemainingSeconds, Is.EqualTo(remaining));
            Assert.That(motor.StepsTaken, Is.Zero);

            game.TogglePause();
            float deadline = Time.realtimeSinceStartup + 3f;
            while (game.Session.Errors == 0 && Time.realtimeSinceStartup < deadline)
                yield return null;

            Assert.That(game.Session.Errors, Is.EqualTo(1));
            Assert.That(motor.StepsTaken, Is.EqualTo(1));
            Assert.That(game.Feedback, Does.Contain("Tiempo agotado"));
            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Advancing));
            yield return new WaitForFixedUpdate();
            Assert.That(motor.IsMoving, Is.True);
            Assert.That(motor.StepsTaken, Is.EqualTo(1));
        }

        [UnityTest]
        public IEnumerator ShadowTrailAppearsDuringMovementAndClearsOnRestart()
        {
            var trail = Object.FindFirstObjectByType<DementorShadowTrail>();
            var particles = trail.transform.Find("VFX \u00b7 Shadow Trail").GetComponent<ParticleSystem>();
            Assert.That(particles.main.simulationSpace, Is.EqualTo(ParticleSystemSimulationSpace.World));
            Assert.That(particles.particleCount, Is.Zero);

            game.SubmitAnswer("RESPUESTA INCORRECTA");
            float deadline = Time.realtimeSinceStartup + 3f;
            while (particles.particleCount == 0 && Time.realtimeSinceStartup < deadline)
                yield return null;

            Assert.That(particles.particleCount, Is.GreaterThan(0));
            var before = new ParticleSystem.Particle[particles.main.maxParticles];
            particles.GetParticles(before);
            uint seed = before[0].randomSeed;
            Vector3 position = before[0].position;
            yield return new WaitForSeconds(0.08f);
            var after = new ParticleSystem.Particle[particles.main.maxParticles];
            int count = particles.GetParticles(after);
            bool found = false;
            for (int index = 0; index < count; index++)
            {
                if (after[index].randomSeed != seed)
                    continue;

                found = true;
                Assert.That(Vector3.Distance(after[index].position, position), Is.LessThan(0.001f));
                break;
            }

            Assert.That(found, Is.True, "La sombra debe permanecer en su posicion mientras el actor avanza.");
            game.RestartCampaign();
            Assert.That(particles.particleCount, Is.Zero);
            yield return new WaitForSeconds(0.1f);
            Assert.That(particles.particleCount, Is.Zero, "No debe emitir estando parado tras el reinicio.");
        }

        [UnityTest]
        public IEnumerator AuraHasValidMaterialsAndFollowsTheVisualScale()
        {
            var aura = Object.FindFirstObjectByType<DementorAura>();
            var emitters = aura.transform.Find("Visual/Flotacion").GetComponentsInChildren<ParticleSystem>();
            Assert.That(emitters.Length, Is.GreaterThan(0));

            foreach (var emitter in emitters)
            {
                var renderer = emitter.GetComponent<ParticleSystemRenderer>();
                Assert.That(renderer.sharedMaterial, Is.Not.Null);
                Assert.That(renderer.sharedMaterial.shader.name, Is.EqualTo("Enigma/Dementor Mist 2D"));
                Assert.That(renderer.sharedMaterial.shader.isSupported, Is.True);
                Assert.That(emitter.main.simulationSpace, Is.EqualTo(ParticleSystemSimulationSpace.Local));
                Assert.That(emitter.main.scalingMode, Is.EqualTo(ParticleSystemScalingMode.Hierarchy));
            }

            aura.StopEmission();
            foreach (var emitter in emitters)
                Assert.That(emitter.isEmitting, Is.False);

            aura.Restart();
            yield return null;
            foreach (var emitter in emitters)
                Assert.That(emitter.isPlaying, Is.True);
        }

        [UnityTest]
        public IEnumerator DoorPassageBeginsAtItsCentreAndRestoresTheVisualAfterRetry()
        {
            var motor = Object.FindFirstObjectByType<DementorMotor2D>();
            var visual = motor.transform.Find("Visual");
            var artwork = visual.GetComponentInChildren<SpriteRenderer>();
            Vector3 originalScale = visual.localScale;
            var doorArt = GameObject.Find("Puerta al final del pasillo").GetComponent<SpriteRenderer>();

            for (int index = 0; index < 4; index++)
            {
                game.SubmitAnswer("RESPUESTA INCORRECTA");
                float deadline = Time.realtimeSinceStartup + 5f;
                while (game.Session.Phase == GamePhase.Advancing && Time.realtimeSinceStartup < deadline)
                    yield return new WaitForFixedUpdate();
            }

            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Resolving));
            Assert.That(motor.GetComponent<Rigidbody2D>().position.x,
                Is.EqualTo(GameObject.Find("Step 4 · Door").transform.position.x).Within(0.03f));
            Assert.That(artwork.sortingOrder, Is.GreaterThan(doorArt.sortingOrder));
            yield return new WaitForSeconds(0.25f);
            Assert.That(visual.localScale.x, Is.LessThan(originalScale.x).And.GreaterThan(0f));
            Assert.That(artwork.color.a, Is.LessThan(1f).And.GreaterThan(0f));
            Assert.That(visual.GetComponent<Animator>().enabled, Is.False);

            yield return new WaitForSeconds(0.55f);
            Assert.That(artwork.enabled, Is.False);
            Assert.That(Vector3.Distance(visual.position,
                GameObject.Find("Door · Passage Center").transform.position), Is.LessThan(0.02f));

            yield return WaitForResolution();
            Assert.That(artwork.enabled, Is.True);
            Assert.That(visual.localScale, Is.EqualTo(originalScale));
            Assert.That(artwork.color.a, Is.EqualTo(1f));
            Assert.That(visual.GetComponent<Animator>().enabled, Is.True);
        }

        [UnityTest]
        public IEnumerator HudButtonsSubmitAnswersAndRevealClues()
        {
            var root = Object.FindFirstObjectByType<UIDocument>().rootVisualElement;
            var simulateClick = typeof(Clickable).GetMethod("SimulateSingleClick",
                System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic |
                System.Reflection.BindingFlags.Public);

            simulateClick.Invoke(root.Q<Button>("clueHitArea").clickable, new object[] { null, 0 });
            Assert.That(game.Clue.Revealed, Is.True);

            var answer = root.Q<TextField>("answerInput");
            answer.value = CaesarCipher.Encode(game.Session.CipherText, -game.Session.Shift);
            simulateClick.Invoke(root.Q<Button>("submitAnswer").clickable, new object[] { null, 0 });
            Assert.That(game.Session.Phase, Is.EqualTo(GamePhase.Resolving));
            yield return WaitForResolution();
            Assert.That(game.Session.LevelIndex, Is.EqualTo(1));
            Assert.That(answer.value, Is.Empty);
        }

        [UnityTest]
        public IEnumerator PauseFreezesPhysicsAndClueTargetMatchesItsArtwork()
        {
            var root = Object.FindFirstObjectByType<UIDocument>().rootVisualElement;
            var target = root.Q<Button>("clueHitArea");
            Vector3 screen = game.SceneCamera.WorldToScreenPoint(game.Clue.Position);
            Vector2 panelPoint = RuntimePanelUtils.ScreenToPanel(root.panel, new Vector2(screen.x, Screen.height - screen.y));
            Assert.That(Vector2.Distance(target.worldBound.center, panelPoint), Is.LessThan(2f));

            game.TogglePause();
            Assert.That(Time.timeScale, Is.Zero);
            Assert.That(root.Q<TextField>("answerInput").enabledInHierarchy, Is.False);
            Assert.That(root.Q("gameModal").ClassListContains("hidden"), Is.False);
            int errors = game.Session.Errors;
            game.SubmitAnswer("ERROR");
            Assert.That(game.Session.Errors, Is.EqualTo(errors));
            game.TogglePause();
            game.RevealClue();
            Assert.That(game.Clue.Revealed, Is.True);
            yield return null;
            Assert.That(root.Q<Label>("clueFeedback").text, Does.Contain(game.Session.Shift.ToString()));
        }

        /// <summary>Espera a la animación con un límite independiente de la escala temporal.</summary>
        private IEnumerator WaitForResolution()
        {
            float deadline = Time.realtimeSinceStartup + 4f;
            while (game.Session.Phase == GamePhase.Resolving && Time.realtimeSinceStartup < deadline)
                yield return null;

            Assert.That(game.Session.Phase, Is.Not.EqualTo(GamePhase.Resolving));
        }
    }
}
