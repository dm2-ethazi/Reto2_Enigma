using Enigma.Caesar;
using Enigma.Game;
using UnityEngine;
using UnityEngine.UIElements;

namespace Enigma.UI
{
    /// <summary>Vincula la partida con el HUD UXML, la respuesta, los menús y el área táctil de la pista.</summary>
    /// <remarks>@author koldo</remarks>
    [RequireComponent(typeof(UIDocument))]
    public sealed class GameHudController : MonoBehaviour
    {
        [SerializeField] private CampaignController game;
        [SerializeField] private CaesarPanelController alphabet;

        private UIDocument document;
        private VisualElement root;
        private TextField answer;
        private Button submit;
        private Button pause;
        private Button clue;
        private Button resume;
        private Button restart;
        private Button exit;
        private int displayedLevel = -1;
        private int displayedErrors = -1;
        private Label timer;
        private int displayedSeconds = -1;

        /// <summary>Conecta el evento de partida; el documento se vincula cuando UI Toolkit está preparado.</summary>
        private void OnEnable()
        {
            document = GetComponent<UIDocument>();
            game.Changed += Refresh;
        }

        /// <summary>Recupera los controles si el documento se reconstruye al cambiar de panel o resolución.</summary>
        private void Update()
        {
            if (document.rootVisualElement == root)
                return;

            Unbind();
            root = document.rootVisualElement;
            if (root?.Q<TextField>("answerInput") == null)
                return;

            timer = root.Q<Label>("timerLabel");
            displayedSeconds = -1;
            answer = root.Q<TextField>("answerInput");
            submit = root.Q<Button>("submitAnswer");
            pause = root.Q<Button>("pauseGame");
            clue = root.Q<Button>("clueHitArea");
            resume = root.Q<Button>("resumeGame");
            restart = root.Q<Button>("restartGame");
            exit = root.Q<Button>("exitGame");
            submit.clicked += Submit;
            pause.clicked += game.TogglePause;
            clue.clicked += game.RevealClue;
            resume.clicked += Continue;
            restart.clicked += Restart;
            exit.clicked += game.ExitGame;
            answer.RegisterCallback<KeyDownEvent>(OnAnswerKeyDown);
            Refresh();
        }

        /// <summary>Coloca el área táctil sobre el símbolo usando la cámara real y la escala del panel.</summary>
        private void LateUpdate()
        {
            RefreshTimer();
            if (clue == null || root?.panel == null || game.Session == null)
                return;

            var stage = root.Q("sceneViewport");
            Vector3 screenPoint = game.SceneCamera.WorldToScreenPoint(game.Clue.Position);
            Vector2 point = RuntimePanelUtils.ScreenToPanel(root.panel,
                new Vector2(screenPoint.x, Screen.height - screenPoint.y));

            // La posición depende de la cámara y del punto de pista elegido, no de un valor fijo de USS.
            clue.style.left = point.x - stage.worldBound.xMin - 24f;
            clue.style.top = point.y - stage.worldBound.yMin - 24f;
        }

        /// <summary>Actualiza el reloj al cambiar el segundo visible sin reconstruir los controles de respuesta.</summary>
        private void RefreshTimer()
        {
            if (timer == null || game.Session == null)
                return;

            int seconds = Mathf.CeilToInt(game.Session.RemainingSeconds);
            if (seconds == displayedSeconds)
                return;

            displayedSeconds = seconds;
            timer.text = $"{seconds / 60:00}:{seconds % 60:00}";
            timer.EnableInClassList("timer-warning", seconds <= 10);
        }

        /// <summary>Envía la respuesta actual al coordinador de partida.</summary>
        private void Submit() => game.SubmitAnswer(answer.value);

        /// <summary>El boton principal inicia la partida desde tutorial o continua desde pausa.</summary>
        private void Continue()
        {
            if (game.Session.Phase == GamePhase.Tutorial)
                game.BeginPlaying();
            else
                game.TogglePause();
        }

        /// <summary>Permite validar con Intro en Windows sin quitar la entrada táctil del TextField.</summary>
        private void OnAnswerKeyDown(KeyDownEvent evt)
        {
            if (evt.keyCode != KeyCode.Return && evt.keyCode != KeyCode.KeypadEnter)
                return;

            Submit();
            evt.StopPropagation();
        }

        /// <summary>Reinicia la campaña y la representación del alfabeto.</summary>
        private void Restart()
        {
            displayedLevel = -1;
            alphabet.ResetShift();
            game.RestartCampaign();
        }

        /// <summary>Actualiza las etiquetas y los controles sin sobrescribir la respuesta mientras se escribe.</summary>
        private void Refresh()
        {
            if (answer == null || game.Session == null)
                return;

            RefreshTimer();
            var session = game.Session;
            bool tutorial = session.Phase == GamePhase.Tutorial;
            bool ended = session.Phase == GamePhase.Won || session.Phase == GamePhase.Lost;
            bool ready = session.Phase == GamePhase.Ready && !game.IsPaused;

            if (displayedLevel != session.LevelIndex || (displayedErrors > 0 && session.Errors == 0))
                answer.SetValueWithoutNotify(string.Empty);

            if (displayedLevel != session.LevelIndex)
                alphabet.ResetShift();

            displayedLevel = session.LevelIndex;
            displayedErrors = session.Errors;
            root.Q<Label>("roundLabel").text = $"Ronda {session.LevelIndex + 1}/{session.LevelCount}";
            root.Q<Label>("livesLabel").text = $"Vidas {session.Lives} {new string('\u2665', session.Lives)}";
            root.Q<Label>("errorsLabel").text = $"Errores {session.Errors}/{session.ErrorLimit}";
            var progress = root.Q<ProgressBar>("doorProgress");
            progress.highValue = session.ErrorLimit;
            progress.value = session.Errors;
            progress.title = $"Puerta: {session.Errors}/{session.ErrorLimit} · Quedan {session.ErrorLimit - session.Errors} avances";
            root.Q<Label>("cipherText").text = session.CipherText;
            root.Q<Label>("gameFeedback").text = game.Feedback;
            root.Q<Label>("clueFeedback").text = game.Clue.Revealed
                ? $"Pista: muévete {session.Shift} posiciones."
                : "Busca el símbolo en el pasillo y tócalo.";

            answer.SetEnabled(ready);
            if (!ready)
                answer.Blur();

            submit.SetEnabled(ready);
            alphabet.SetInteractionEnabled(ready);
            pause.SetEnabled(!ended && !tutorial);
            clue.SetEnabled(!ended && !tutorial && !game.IsPaused && session.Phase != GamePhase.Resolving);
            clue.EnableInClassList("hidden", ended || tutorial);
            root.Q("gameModal").EnableInClassList("hidden", !game.IsPaused && !ended && !tutorial);
            resume.EnableInClassList("hidden", !game.IsPaused && !tutorial);
            resume.text = tutorial ? "Comenzar" : "Continuar";
            restart.EnableInClassList("hidden", tutorial);
            root.Q<Label>("modalTitle").text = tutorial ? "Cómo jugar" : game.IsPaused
                ? "En pausa"
                : session.Phase == GamePhase.Won ? "¡Victoria!" : "Fin de la partida";

            root.Q<Label>("modalMessage").text = tutorial
                ? "Descifra cinco rondas. Ejemplo: IPMB, desplazamiento 1 → HOLA. Mueve el alfabeto y escribe la respuesta. Tienes 3 vidas; cada cuatro fallos pierdes una. El reloj también provoca avances. Se ignoran mayúsculas, espacios extra y tildes; Ñ y N son distintas."
                : game.IsPaused
                ? "Continúa cuando estés preparado."
                : game.Feedback;
        }

        /// <summary>Elimina las suscripciones de los controles antes de reemplazar la raíz.</summary>
        private void Unbind()
        {
            if (submit == null)
                return;

            submit.clicked -= Submit;
            pause.clicked -= game.TogglePause;
            clue.clicked -= game.RevealClue;
            resume.clicked -= Continue;
            restart.clicked -= Restart;
            exit.clicked -= game.ExitGame;
            answer.UnregisterCallback<KeyDownEvent>(OnAnswerKeyDown);
            submit = null;
            answer = null;
            clue = null;
            timer = null;
        }

        /// <summary>Libera los eventos del HUD al desactivarlo.</summary>
        private void OnDisable()
        {
            game.Changed -= Refresh;
            Unbind();
            root = null;
        }
    }
}
