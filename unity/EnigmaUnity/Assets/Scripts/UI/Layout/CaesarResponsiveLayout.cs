using UnityEngine;
using UnityEngine.UIElements;

namespace Enigma.Caesar
{
    /// <summary>
    /// Adapta la interfaz al tamaño y al área segura de la pantalla y limita el móvil a horizontal.
    /// </summary>
    /// <remarks>@author koldo</remarks>
    [DefaultExecutionOrder(100)]
    [RequireComponent(typeof(UIDocument))]
    public sealed class CaesarResponsiveLayout : MonoBehaviour
    {
        private UIDocument document;
        private VisualElement screen;
        private PanelSettings runtimeSettings;
        private PanelSettings originalSettings;
        private Rect lastSafeArea;
        private Vector2Int lastSize;

        /// <summary>Permite ambas orientaciones horizontales y desactiva las verticales en móvil.</summary>
        private void Awake()
        {
            if (Application.isMobilePlatform)
            {
                Screen.autorotateToPortrait = false;
                Screen.autorotateToPortraitUpsideDown = false;
                Screen.autorotateToLandscapeLeft = true;
                Screen.autorotateToLandscapeRight = true;
                Screen.orientation = ScreenOrientation.AutoRotation;
            }
        }

        /// <summary>Crea una configuración de panel propia y conecta los cambios de geometría.</summary>
        private void OnEnable()
        {
            document = GetComponent<UIDocument>();
            originalSettings = document.panelSettings;
            runtimeSettings = Instantiate(originalSettings);
            document.panelSettings = runtimeSettings;

            screen = document.rootVisualElement.Q<VisualElement>("caesarScreen");
            screen.RegisterCallback<GeometryChangedEvent>(OnGeometryChanged);

            ApplyScreenMetrics();
        }

        /// <summary>Actualiza la distribución cuando cambia la resolución o el área segura.</summary>
        private void Update()
        {
            if (lastSize.x != Screen.width ||
                lastSize.y != Screen.height ||
                lastSafeArea != Screen.safeArea)
                ApplyScreenMetrics();
        }

        /// <summary>Calcula la escala del panel y los márgenes que protegen la interfaz del recorte físico.</summary>
        private void ApplyScreenMetrics()
        {
            lastSize = new Vector2Int(Screen.width, Screen.height);
            lastSafeArea = Screen.safeArea;

            float scale = Mathf.Clamp(
                Screen.height / (Application.isMobilePlatform ? 450f : 900f),
                1f,
                3f);

            runtimeSettings.scaleMode = PanelScaleMode.ConstantPixelSize;
            runtimeSettings.scale = scale;

            var safeArea = Application.isMobilePlatform
                ? lastSafeArea
                : new Rect(0, 0, Screen.width, Screen.height);

            safeArea = Rect.MinMaxRect(
                Mathf.Clamp(safeArea.xMin, 0, Screen.width),
                Mathf.Clamp(safeArea.yMin, 0, Screen.height),
                Mathf.Clamp(safeArea.xMax, 0, Screen.width),
                Mathf.Clamp(safeArea.yMax, 0, Screen.height));

            // Hardware-dependent insets cannot be represented by fixed USS classes.
            screen.style.left = safeArea.xMin / scale;
            screen.style.right = (Screen.width - safeArea.xMax) / scale;
            screen.style.top = (Screen.height - safeArea.yMax) / scale;
            screen.style.bottom = safeArea.yMin / scale;
        }

        /// <summary>Activa las clases USS correspondientes al espacio disponible.</summary>
        /// <param name="evt">Evento de cambio de geometría enviado por UI Toolkit.</param>
        private void OnGeometryChanged(GeometryChangedEvent evt)
        {
            screen.EnableInClassList(
                "compact",
                screen.resolvedStyle.height < 580 || screen.resolvedStyle.width < 1100);
            screen.EnableInClassList("narrow", screen.resolvedStyle.width < 760);
        }

        /// <summary>Elimina los eventos, restaura la configuración original y destruye la copia temporal.</summary>
        private void OnDisable()
        {
            screen?.UnregisterCallback<GeometryChangedEvent>(OnGeometryChanged);

            if (document != null)
                document.panelSettings = originalSettings;

            if (runtimeSettings != null)
                Destroy(runtimeSettings);
        }
    }
}
