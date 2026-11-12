using System.Reflection;
using Enigma.Game;
using NUnit.Framework;
using UnityEngine;

namespace Enigma.Tests.Game
{
    /// <summary>Verifica las reglas de la campaña sin depender de animaciones ni tiempos de física.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class GameSessionTests
    {
        private CampaignSettings settings;
        private LevelDefinition[] levels;
        private GameSession session;

        [SetUp]
        public void CreateRules()
        {
            levels = new LevelDefinition[5];
            for (int index = 0; index < levels.Length; index++)
            {
                levels[index] = ScriptableObject.CreateInstance<LevelDefinition>();
                JsonUtility.FromJsonOverwrite(
                    "{\"phrases\":[\"HOLA\"],\"minimumShift\":1,\"maximumShift\":1,\"timeLimitSeconds\":" + (50 - index * 5) + "}",
                    levels[index]);
            }

            settings = ScriptableObject.CreateInstance<CampaignSettings>();
            typeof(CampaignSettings).GetField("levels", BindingFlags.NonPublic | BindingFlags.Instance)
                .SetValue(settings, levels);
            session = new GameSession(settings, 42);
            session.BeginPlaying();
        }

        [TearDown]
        public void ReleaseRules()
        {
            Object.DestroyImmediate(settings);
            foreach (var level in levels)
                Object.DestroyImmediate(level);
        }

        [Test]
        public void CipherWrapsAndNormalizesAnswers()
        {
            Assert.That(CaesarCipher.Encode("Z A", 1), Is.EqualTo("A B"));
            Assert.That(CaesarCipher.Encode("A B", -1), Is.EqualTo("Z A"));
            Assert.That(CaesarCipher.Normalize("  lá luz\tvence  "), Is.EqualTo("LA LUZ VENCE"));
            Assert.That(session.Submit("  hola "), Is.EqualTo(AnswerResult.Correct));
        }

        [Test]
        public void TutorialBlocksAnswersAndTimeUntilStarted()
        {
            session.Restart();
            Assert.That(session.Phase, Is.EqualTo(GamePhase.Tutorial));
            Assert.That(session.Submit("HOLA"), Is.EqualTo(AnswerResult.Unavailable));
            Assert.That(session.Tick(200f), Is.False);
            Assert.That(session.RemainingSeconds, Is.EqualTo(50f));
            session.BeginPlaying();
            Assert.That(session.Phase, Is.EqualTo(GamePhase.Ready));
        }

        [Test]
        public void NormalizationUsesTrimAndPreservesEnyeWhileEncodingOnlyAZ()
        {
            Assert.That(CaesarCipher.Normalize("  ni\u00f1o \t  c\u00e1lido  "), Is.EqualTo("NI\u00d1O CALIDO"));
            Assert.That(CaesarCipher.Normalize("NIN\u0303O"), Is.EqualTo("NI\u00d1O"));
            Assert.That(CaesarCipher.Normalize("NI\u00d1O"), Is.Not.EqualTo(CaesarCipher.Normalize("NINO")));
            Assert.That(CaesarCipher.Encode(" A  Z, \u00d1 \u00e1! ", 1), Is.EqualTo(" B  A, \u00d1 \u00e1! "));
        }

        [Test]
        public void LostLifeIsNotRestoredOrReducedWhenChangingRounds()
        {
            for(int error = 0; error < 4; error++)
            {
                session.Submit("ERROR");
                session.FinishMovement();
            }
            session.RegisterDoorArrival();
            session.FinishResolution();
            for(int level = 0; level < 5; level++)
            {
                Assert.That(session.Lives, Is.EqualTo(2));
                session.Submit("HOLA");
                session.FinishResolution();
            }
            Assert.That(session.Phase, Is.EqualTo(GamePhase.Won));
        }

        [Test]
        public void EmptyAndRepeatedSubmissionsDoNotCostExtraErrors()
        {
            Assert.That(session.Submit("   "), Is.EqualTo(AnswerResult.Empty));
            Assert.That(session.Errors, Is.Zero);
            Assert.That(session.Submit("ERROR"), Is.EqualTo(AnswerResult.Incorrect));
            Assert.That(session.Submit("ERROR"), Is.EqualTo(AnswerResult.Unavailable));
            Assert.That(session.Errors, Is.EqualTo(1));
        }

        [Test]
        public void FourErrorsNeedTheDoorTriggerAndRetryKeepsTheSamePuzzle()
        {
            string cipher = session.CipherText;
            int shift = session.Shift;
            for (int index = 0; index < 4; index++)
            {
                session.Submit("ERROR");
                if (index < 3)
                {
                    Assert.That(session.RegisterDoorArrival(), Is.False);
                    session.FinishMovement();
                }
            }

            Assert.That(session.Lives, Is.EqualTo(3));
            Assert.That(session.RegisterDoorArrival(), Is.True);
            Assert.That(session.RegisterDoorArrival(), Is.False);
            Assert.That(session.Lives, Is.EqualTo(2));
            session.FinishResolution();
            Assert.That(session.CipherText, Is.EqualTo(cipher));
            Assert.That(session.Shift, Is.EqualTo(shift));
            Assert.That(session.Errors, Is.Zero);
            Assert.That(session.Phase, Is.EqualTo(GamePhase.Ready));
        }

        [Test]
        public void FiveCorrectRoundsKeepLivesAndWin()
        {
            int[] limits = { 3, 3, 3, 3, 3 };
            for (int index = 0; index < 5; index++)
            {
                Assert.That(session.Lives, Is.EqualTo(limits[index]));
                session.Submit("HOLA");
                session.FinishResolution();
            }

            Assert.That(session.Phase, Is.EqualTo(GamePhase.Won));
            session.Restart();
            Assert.That(session.LevelIndex, Is.Zero);
            Assert.That(session.Lives, Is.EqualTo(3));
        }

        [Test]
        public void TimeoutAddsOnlyOneErrorUntilMovementFinishes()
        {
            Assert.That(session.Tick(49.75f), Is.False);
            Assert.That(session.RemainingSeconds, Is.EqualTo(0.25f));
            Assert.That(session.Tick(0.25f), Is.True);
            Assert.That(session.Errors, Is.EqualTo(1));
            Assert.That(session.Phase, Is.EqualTo(GamePhase.Advancing));
            Assert.That(session.Tick(500f), Is.False);
            Assert.That(session.Submit("HOLA"), Is.EqualTo(AnswerResult.Unavailable));
            Assert.That(session.Errors, Is.EqualTo(1));
            session.FinishMovement();
            Assert.That(session.RemainingSeconds, Is.EqualTo(50f));
            Assert.That(session.Tick(-1f), Is.False);
            Assert.That(session.RemainingSeconds, Is.EqualTo(50f));
        }

        [Test]
        public void FourTimeoutsWaitForTheDoorAndRestoreTimeOnRetry()
        {
            string cipher = session.CipherText;
            for (int error = 0; error < 4; error++)
            {
                Assert.That(session.Tick(50f), Is.True);
                session.FinishMovement();
            }

            Assert.That(session.Lives, Is.EqualTo(3));
            Assert.That(session.Phase, Is.EqualTo(GamePhase.Advancing));
            Assert.That(session.RegisterDoorArrival(), Is.True);
            Assert.That(session.Tick(100f), Is.False);
            session.FinishResolution();
            Assert.That(session.Lives, Is.EqualTo(2));
            Assert.That(session.CipherText, Is.EqualTo(cipher));
            Assert.That(session.RemainingSeconds, Is.EqualTo(50f));
            Assert.That(session.Errors, Is.Zero);
        }

        [Test]
        public void NewRoundsShortenTimeAndCorrectAnswersStopTheCountdown()
        {
            for (int index = 0; index < 5; index++)
            {
                Assert.That(session.RemainingSeconds, Is.EqualTo(50f - index * 5f));
                session.Submit("HOLA");
                Assert.That(session.Tick(100f), Is.False);
                Assert.That(session.Errors, Is.Zero);
                session.FinishResolution();
            }

            Assert.That(session.Phase, Is.EqualTo(GamePhase.Won));
            Assert.That(session.Tick(100f), Is.False);
            session.Restart();
            Assert.That(session.RemainingSeconds, Is.EqualTo(50f));
        }

        [Test]
        public void LosingAllLivesEndsTheCampaign()
        {
            for (int attempt = 0; attempt < 3; attempt++)
            {
                for (int error = 0; error < 4; error++)
                {
                    session.Submit("ERROR");
                    session.FinishMovement();
                }

                session.RegisterDoorArrival();
                session.FinishResolution();
            }

            Assert.That(session.Lives, Is.Zero);
            Assert.That(session.Phase, Is.EqualTo(GamePhase.Lost));
        }
    }
}
