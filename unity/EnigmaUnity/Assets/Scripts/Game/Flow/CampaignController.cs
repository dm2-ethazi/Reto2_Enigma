using System;
using System.Collections;
using Enigma.Physics;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace Enigma.Game
{
    /// <summary>Coordina estados, prefab enemigo, fisica y pistas; mantiene la validacion fuera de la interfaz.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class CampaignController : MonoBehaviour
    {
        [SerializeField] private CampaignSettings settings;
        [SerializeField] private DementorSpawner enemies;
        [SerializeField] private DoorTrigger2D door;
        [SerializeField] private GameAudio gameAudio;
        [SerializeField] private CluePlacement clue;
        [SerializeField] private Camera sceneCamera;

        private Coroutine resolution;
        private bool restarting;

        public event Action Changed;
        public GameSession Session { get; private set; }
        public CluePlacement Clue => clue;
        public Camera SceneCamera => sceneCamera;
        public DementorActor Enemy => enemies.Current;
        public bool IsPaused { get; private set; }
        public string Feedback { get; private set; }

        /// <summary>Prepara la instancia inicial y espera a que el jugador complete el tutorial.</summary>
        private void Start()
        {
            Session = new GameSession(settings);
            Session.Changed += NotifyChanged;
            door.DementorReached.AddListener(OnDoorReached);
            enemies.EnsureActor();
            Enemy.Motor.enabled = false;
            clue.SelectForLevel(true);
            Feedback = "Lee el ejemplo y pulsa Comenzar. Tienes tres vidas para toda la partida.";
            NotifyChanged();
        }

        /// <summary>Inicia el juego tras el ejemplo guiado; pulsarlo de nuevo no reinicia datos ni tiempo.</summary>
        public void BeginPlaying()
        {
            if (Session == null || Session.Phase != GamePhase.Tutorial || restarting)
                return;

            Enemy.Motor.enabled = true;
            Session.BeginPlaying();
            Feedback = "Descifra el mensaje. Cada cuatro fallos pierdes una vida.";
            NotifyChanged();
        }

        /// <summary>Consume tiempo solo en juego y reabre la entrada cuando termina cada avance fisico.</summary>
        private void Update()
        {
            if (Session == null || IsPaused || restarting)
                return;

            if (Session.Phase == GamePhase.Advancing && !Enemy.Motor.IsMoving && Session.Errors < Session.ErrorLimit)
            {
                Session.FinishMovement();
                return;
            }

            if (Session.Tick(Time.deltaTime))
            {
                Feedback = "Tiempo agotado. El dementor avanza mas rapido.";
                AdvanceEnemy();
                NotifyChanged();
            }
        }

        /// <summary>Valida la respuesta y enlaza los resultados del modelo con la presentacion.</summary>
        public void SubmitAnswer(string answer)
        {
            if (Session == null || IsPaused || restarting)
                return;

            switch (Session.Submit(answer))
            {
                case AnswerResult.Empty:
                    Feedback = "Escribe una respuesta antes de validar. No cuenta como fallo.";
                    break;
                case AnswerResult.Incorrect:
                    Feedback = "Respuesta incorrecta. El dementor avanza mas rapido.";
                    AdvanceEnemy();
                    break;
                case AnswerResult.Correct:
                    gameAudio.PlaySuccess();
                    Feedback = "¡Correcto! El dementor se desintegra.";
                    resolution = StartCoroutine(ResolveAttempt());
                    break;
            }

            NotifyChanged();
        }

        /// <summary>Aplica la penalizacion acotada y ordena exactamente un tramo de movimiento.</summary>
        private void AdvanceEnemy()
        {
            Enemy.Motor.SetPenaltyLevel(Session.Errors);
            Enemy.Motor.AdvanceOneStep();
        }

        /// <summary>Permite pistas durante el juego, nunca desde tutorial, pausa o pantallas finales.</summary>
        public void RevealClue()
        {
            if (Session == null || IsPaused || restarting ||
                (Session.Phase != GamePhase.Ready && Session.Phase != GamePhase.Advancing))
                return;

            clue.ToggleReveal();
            NotifyChanged();
        }

        /// <summary>Congela simultaneamente el contador, la fisica, las animaciones y el sonido.</summary>
        public void TogglePause()
        {
            if (Session == null || restarting || Session.Phase == GamePhase.Tutorial ||
                Session.Phase == GamePhase.Won || Session.Phase == GamePhase.Lost)
                return;

            IsPaused = !IsPaused;
            Time.timeScale = IsPaused ? 0f : 1f;
            gameAudio.SetPaused(IsPaused);
            NotifyChanged();
        }

        /// <summary>Recarga la escena completa, incluidos prefabs y tutorial, como exige el contrato.</summary>
        public void RestartCampaign()
        {
            if (restarting)
                return;

            restarting = true;
            if (resolution != null)
                StopCoroutine(resolution);

            Enemy?.StopEffects();
            Time.timeScale = 1f;
            gameAudio.Restart();
            SceneManager.LoadScene(gameObject.scene.buildIndex);
        }

        /// <summary>Termina el ejecutable; en el Editor sale del modo Play.</summary>
        public void ExitGame()
        {
            Time.timeScale = 1f;
#if UNITY_EDITOR
            UnityEditor.EditorApplication.isPlaying = false;
#else
            Application.Quit();
#endif
        }

        /// <summary>El trigger fisico inicia una unica perdida de vida al finalizar el cuarto tramo.</summary>
        private void OnDoorReached()
        {
            if (!Session.RegisterDoorArrival())
                return;

            Feedback = "El dementor ha llegado a la puerta. Pierdes una vida.";
            resolution = StartCoroutine(ResolveAttempt(true));
            NotifyChanged();
        }

        /// <summary>Espera a la animacion, destruye el enemigo y crea el siguiente solo si la partida continua.</summary>
        private IEnumerator ResolveAttempt(bool enteredDoor = false)
        {
            var actor = Enemy;
            actor.StopEffects();
            if (enteredDoor)
            {
                yield return actor.Passage.Play();
                yield return new WaitForSeconds(Mathf.Max(0f, settings.ResolutionSeconds - actor.Passage.Duration));
            }
            else
            {
                actor.PlayDefeat();
                yield return new WaitForSeconds(settings.ResolutionSeconds);
            }

            int previousLevel = Session.LevelIndex;
            enemies.RemoveActor();
            Session.FinishResolution();
            resolution = null;
            if (Session.Phase == GamePhase.Won || Session.Phase == GamePhase.Lost)
            {
                clue.Hide();
                Feedback = Session.Phase == GamePhase.Won ? "¡Has superado las cinco rondas!" : "Has perdido las tres vidas.";
            }
            else
            {
                enemies.EnsureActor();
                if (Session.LevelIndex != previousLevel)
                    clue.SelectForLevel();

                Feedback = "Nuevo dementor. Descifra el mensaje para detenerlo.";
            }

            NotifyChanged();
        }

        /// <summary>Pausa el juego cuando el sistema operativo interrumpe la aplicacion.</summary>
        private void OnApplicationPause(bool paused)
        {
            if (paused && !IsPaused)
                TogglePause();
        }

        private void NotifyChanged() => Changed?.Invoke();

        /// <summary>Desconecta los eventos y restaura la escala de tiempo al recargar o cerrar la escena.</summary>
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
