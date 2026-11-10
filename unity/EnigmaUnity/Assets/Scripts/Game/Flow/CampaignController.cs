using System;
using System.Collections;
using Enigma.Physics;
using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Coordina el modelo de partida con la física, las pistas y la animación de la escena.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class CampaignController : MonoBehaviour
    {
        [SerializeField] private CampaignSettings settings;
        [SerializeField] private DementorMotor2D motor;
        [SerializeField] private DoorTrigger2D door;
        [SerializeField] private DementorDefeat defeat;
        [SerializeField] private DoorPassage passage;
        [SerializeField] private DementorAura aura;
        [SerializeField] private DementorShadowTrail shadowTrail;
        [SerializeField] private GameAudio gameAudio;
        [SerializeField] private CluePlacement clue;
        [SerializeField] private Camera sceneCamera;

        private Coroutine resolution;

        public event Action Changed;
        public GameSession Session { get; private set; }
        public CluePlacement Clue => clue;
        public Camera SceneCamera => sceneCamera;
        public bool IsPaused { get; private set; }
        public string Feedback { get; private set; }

        /// <summary>Conecta los objetos preparados en el Inspector e inicia la campaña.</summary>
        private void Start()
        {
            Session = new GameSession(settings);
            Session.Changed += NotifyChanged;
            door.DementorReached.AddListener(OnDoorReached);
            clue.SelectForLevel();
            Feedback = "Busca el símbolo oculto. Cuatro errores cuestan una vida.";
            NotifyChanged();
        }

        /// <summary>Espera a que la física termine el avance antes de admitir otra respuesta.</summary>
        private void Update()
        {
            if (Session == null || IsPaused)
                return;

            if (Session.Phase == GamePhase.Advancing && !motor.IsMoving && Session.Errors < Session.ErrorLimit)
            {
                Session.FinishMovement();
                return;
            }

            if (Session.Tick(Time.deltaTime))
            {
                Feedback = "Tiempo agotado. El dementor avanza.";
                motor.AdvanceOneStep();
                NotifyChanged();
            }
        }

        /// <summary>Valida una respuesta y ejecuta su efecto sobre el dementor.</summary>
        /// <param name="answer">Texto proporcionado por la interfaz.</param>
        public void SubmitAnswer(string answer)
        {
            if (Session == null || IsPaused)
                return;

            switch (Session.Submit(answer))
            {
                case AnswerResult.Empty:
                    Feedback = "Escribe una respuesta antes de validar.";
                    break;
                case AnswerResult.Incorrect:
                    Feedback = "Respuesta incorrecta. El dementor avanza.";
                    motor.AdvanceOneStep();
                    break;
                case AnswerResult.Correct:
                    gameAudio?.PlaySuccess();
                    Feedback = "¡Correcto! El dementor se desintegra.";
                    resolution = StartCoroutine(ResolveAttempt());
                    break;
            }

            NotifyChanged();
        }

        /// <summary>Revela el desplazamiento del nivel al pulsar su símbolo.</summary>
        public void RevealClue()
        {
            if (Session == null || IsPaused || Session.Phase == GamePhase.Won || Session.Phase == GamePhase.Lost)
                return;

            clue.ToggleReveal();
            NotifyChanged();
        }

        /// <summary>Alterna una pausa que congela tanto el movimiento como la resolución.</summary>
        public void TogglePause()
        {
            if (Session == null || Session.Phase == GamePhase.Won || Session.Phase == GamePhase.Lost)
                return;

            IsPaused = !IsPaused;
            Time.timeScale = IsPaused ? 0f : 1f;
            gameAudio?.SetPaused(IsPaused);
            NotifyChanged();
        }

        /// <summary>Reinicia los datos y los objetos existentes sin recargar o generar el escenario.</summary>
        public void RestartCampaign()
        {
            if (resolution != null)
                StopCoroutine(resolution);

            resolution = null;
            IsPaused = false;
            Time.timeScale = 1f;
            gameAudio?.Restart();
            Session.Restart();
            motor.enabled = true;
            motor.ResetToStart();
            passage.ResetVisual();
            defeat.ResetVisual();
            aura?.Restart();
            shadowTrail?.Restart();
            clue.SelectForLevel();
            Feedback = "Busca el símbolo oculto y descifra el mensaje.";
            NotifyChanged();
        }

        /// <summary>Sale del juego; en el Editor termina el modo Play.</summary>
        public void ExitGame()
        {
            Time.timeScale = 1f;
#if UNITY_EDITOR
            UnityEditor.EditorApplication.isPlaying = false;
#else
            Application.Quit();
#endif
        }

        /// <summary>Resuelve el fallo únicamente después de recibir el trigger real de la puerta.</summary>
        private void OnDoorReached()
        {
            if (!Session.RegisterDoorArrival())
                return;

            Feedback = "Cuatro errores: pierdes una vida.";
            resolution = StartCoroutine(ResolveAttempt(true));
            NotifyChanged();
        }

        /// <summary>Reproduce la derrota o la entrada en la puerta y prepara el nivel o reintento siguiente.</summary>
        private IEnumerator ResolveAttempt(bool enteredDoor = false)
        {
            motor.enabled = false;
            aura?.StopEmission();
            shadowTrail?.StopEmission();

            if (enteredDoor)
            {
                yield return passage.Play();
                yield return new WaitForSeconds(Mathf.Max(0f, settings.ResolutionSeconds - passage.Duration));
            }
            else
            {
                defeat.Play();
                yield return new WaitForSeconds(settings.ResolutionSeconds);
            }

            int previousLevel = Session.LevelIndex;
            Session.FinishResolution();
            resolution = null;

            if (Session.Phase == GamePhase.Won || Session.Phase == GamePhase.Lost)
            {
                clue.Hide();
                Feedback = Session.Phase == GamePhase.Won ? "¡Has superado todas las rondas!" : "Te has quedado sin vidas.";
            }
            else
            {
                motor.enabled = true;
                motor.ResetToStart();
                passage.ResetVisual();
                defeat.ResetVisual();
                aura?.Restart();
                shadowTrail?.Restart();
                if (Session.LevelIndex != previousLevel)
                    clue.SelectForLevel();

                Feedback = "Descifra el mensaje. Busca la pista en el pasillo.";
            }

            NotifyChanged();
        }

        /// <summary>Pausa si el sistema operativo interrumpe la aplicación.</summary>
        private void OnApplicationPause(bool paused)
        {
            if (paused && !IsPaused)
                TogglePause();
        }

        private void NotifyChanged() => Changed?.Invoke();

        /// <summary>Desconecta eventos y restaura el tiempo al abandonar la escena.</summary>
        private void OnDestroy()
        {
            if (Session != null)
                Session.Changed -= NotifyChanged;

            if (door != null)
                door.DementorReached.RemoveListener(OnDoorReached);

            Time.timeScale = 1f;
        }
    }
}
