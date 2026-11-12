using System;

namespace Enigma.Game
{
    /// <summary>Estado de la partida, independiente de cámaras, UI y objetos de la escena.</summary>
    public enum GamePhase
    {
        Tutorial,
        Ready,
        Advancing,
        Resolving,
        Won,
        Lost
    }

    /// <summary>Resultado de validar una respuesta sin modificar la presentación.</summary>
    public enum AnswerResult
    {
        Unavailable,
        Empty,
        Incorrect,
        Correct
    }

    /// <summary>Aplica las reglas de niveles, errores, reintentos y vidas de la demo.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class GameSession
    {
        private readonly CampaignSettings settings;
        private readonly Random random;
        private readonly RoundTimer timer = new RoundTimer();
        private string expectedAnswer;
        private bool correctResolution;

        public event Action Changed;
        public int LevelIndex { get; private set; }
        public int LevelCount => settings.Levels.Length;
        public int Lives { get; private set; }
        public int Errors { get; private set; }
        public int ErrorLimit => settings.ErrorsPerAttempt;
        public int Shift { get; private set; }
        public string CipherText { get; private set; }
        public GamePhase Phase { get; private set; }
        public float RemainingSeconds => timer.RemainingSeconds;
        public float TimeLimitSeconds => settings.Levels[LevelIndex].TimeLimitSeconds;

        /// <summary>Inicializa una campaña; la semilla permite pruebas reproducibles.</summary>
        /// <param name="settings">Reglas y niveles guardados como assets.</param>
        /// <param name="seed">Semilla opcional para seleccionar las frases y desplazamientos.</param>
        public GameSession(CampaignSettings settings, int? seed = null)
        {
            this.settings = settings;
            random = seed.HasValue ? new Random(seed.Value) : new Random();
            Restart();
        }

        /// <summary>Reinicia la campaña y genera su primera ronda.</summary>
        public void Restart()
        {
            LevelIndex = 0;
            Lives = settings.InitialLives;
            BeginLevel();
            Phase = GamePhase.Tutorial;
            Changed?.Invoke();
        }

        /// <summary>Comienza la partida tras leer el ejemplo guiado, sin consumir tiempo durante el tutorial.</summary>
        public void BeginPlaying()
        {
            if (Phase != GamePhase.Tutorial)
                return;

            Phase = GamePhase.Ready;
            Changed?.Invoke();
        }

        /// <summary>Valida una respuesta y bloquea nuevas respuestas mientras se resuelve su efecto.</summary>
        /// <param name="answer">Texto introducido por el jugador.</param>
        /// <returns>Resultado para que la presentación muestre la respuesta adecuada.</returns>
        public AnswerResult Submit(string answer)
        {
            if (Phase != GamePhase.Ready)
                return AnswerResult.Unavailable;

            string normalized = CaesarCipher.Normalize(answer);
            if (normalized.Length == 0)
                return AnswerResult.Empty;

            correctResolution = normalized == expectedAnswer;
            if (correctResolution)
            {
                Phase = GamePhase.Resolving;
                Changed?.Invoke();
                return AnswerResult.Correct;
            }

            RegisterMistake();
            return AnswerResult.Incorrect;
        }

        /// <summary>Consume tiempo solo mientras se admite una respuesta; devuelve una unica penalizacion al agotarse.</summary>
        public bool Tick(float deltaSeconds)
        {
            if (Phase != GamePhase.Ready || !timer.Tick(deltaSeconds))
                return false;

            RegisterMistake();
            return true;
        }

        /// <summary>Comparte la misma regla de avance para respuestas incorrectas y tiempo agotado.</summary>
        private void RegisterMistake()
        {
            correctResolution = false;
            Errors++;
            Phase = GamePhase.Advancing;
            Changed?.Invoke();
        }

        /// <summary>Permite otra respuesta después de un avance que todavía no alcanza la puerta.</summary>
        public void FinishMovement()
        {
            if (Phase != GamePhase.Advancing || Errors >= ErrorLimit)
                return;

            timer.Reset(TimeLimitSeconds);
            Phase = GamePhase.Ready;
            Changed?.Invoke();
        }

        /// <summary>Descuenta una vida una sola vez cuando el cuarto error llega físicamente a la puerta.</summary>
        /// <returns>Verdadero si esta llegada inicia la resolución de un intento fallido.</returns>
        public bool RegisterDoorArrival()
        {
            if (Phase != GamePhase.Advancing || Errors != ErrorLimit)
                return false;

            Lives--;
            correctResolution = false;
            Phase = GamePhase.Resolving;
            Changed?.Invoke();
            return true;
        }

        /// <summary>Finaliza la animación: cambia de nivel o repite el mismo mensaje y desplazamiento.</summary>
        public void FinishResolution()
        {
            if (Phase != GamePhase.Resolving)
                return;

            if (correctResolution)
            {
                LevelIndex++;
                if (LevelIndex >= LevelCount)
                {
                    LevelIndex = LevelCount - 1;
                    Phase = GamePhase.Won;
                    Changed?.Invoke();
                    return;
                }

                BeginLevel();
                return;
            }

            Errors = 0;
            timer.Reset(TimeLimitSeconds);
            Phase = Lives <= 0 ? GamePhase.Lost : GamePhase.Ready;
            Changed?.Invoke();
        }

        /// <summary>Selecciona datos del nivel y aplica su límite progresivo de vidas.</summary>
        private void BeginLevel()
        {
            var level = settings.Levels[LevelIndex];
            Shift = random.Next(level.MinimumShift, level.MaximumShift + 1);
            expectedAnswer = CaesarCipher.Normalize(level.Phrases[random.Next(level.Phrases.Length)]);
            CipherText = CaesarCipher.Encode(expectedAnswer, Shift);
            Errors = 0;
            timer.Reset(TimeLimitSeconds);
            Phase = GamePhase.Ready;
            Changed?.Invoke();
        }
    }
}
