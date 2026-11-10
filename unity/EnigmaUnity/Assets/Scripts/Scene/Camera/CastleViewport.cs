using UnityEngine;
using UnityEngine.UIElements;

namespace Enigma.Caesar
{
    /// <summary>
    /// Encuadra el pasillo completo dentro de su zona de UI sin mostrar el exterior de sus límites.
    /// </summary>
    /// <remarks>@author koldo</remarks>
    [RequireComponent(typeof(Camera))]
    public sealed class CastleViewport : MonoBehaviour
    {
        [SerializeField] private UIDocument interfaceDocument;
        [SerializeField] private Transform corridor;
        [SerializeField] private Transform corridorWalls;

        private Camera sceneCamera;
        private Bounds corridorBounds;

        /// <summary>Calcula los límites del escenario visible y los ajusta a los bordes de las paredes.</summary>
        private void Awake()
        {
            sceneCamera = GetComponent<Camera>();
            corridorBounds = new Bounds(Vector3.zero, new Vector3(27.5f, 12f, 1f));

            if (corridor == null)
                return;

            bool initialized = false;

            foreach (var renderer in corridor.GetComponentsInChildren<SpriteRenderer>())
            {
                if (!renderer.enabled || !renderer.gameObject.activeInHierarchy)
                    continue;

                if (!initialized)
                {
                    corridorBounds = renderer.bounds;
                    initialized = true;
                }
                else
                    corridorBounds.Encapsulate(renderer.bounds);
            }

            if (corridorWalls != null)
            {
                bool hasWalls = false;
                Bounds walls = default;

                foreach (var renderer in corridorWalls.GetComponentsInChildren<SpriteRenderer>())
                {
                    if (!renderer.enabled || !renderer.gameObject.activeInHierarchy)
                        continue;

                    if (!hasWalls)
                    {
                        walls = renderer.bounds;
                        hasWalls = true;
                    }
                    else
                        walls.Encapsulate(renderer.bounds);
                }

                if (hasWalls)
                    corridorBounds.SetMinMax(
                        new Vector3(walls.min.x, corridorBounds.min.y, corridorBounds.min.z),
                        new Vector3(walls.max.x, corridorBounds.max.y, corridorBounds.max.z));
            }
        }

        /// <summary>
        /// Ajusta la posición, el tamaño ortográfico y el rectángulo de cámara al espacio de la interfaz,
        /// conservando las proporciones del pasillo.
        /// </summary>
        private void LateUpdate()
        {
            var viewport = interfaceDocument.rootVisualElement.Q<VisualElement>("sceneViewport");

            if (viewport?.panel == null)
                return;

            var rootBounds = interfaceDocument.rootVisualElement.worldBound;
            var bounds = viewport.worldBound;

            if (bounds.width <= 0 || bounds.height <= 0 || rootBounds.width <= 0 || rootBounds.height <= 0)
                return;

            var rect = new Rect(
                bounds.xMin / rootBounds.width,
                1f - bounds.yMax / rootBounds.height,
                bounds.width / rootBounds.width,
                bounds.height / rootBounds.height);

            float aspect = Screen.width * rect.width / (Screen.height * rect.height);
            float corridorAspect = corridorBounds.size.x / corridorBounds.size.y;

            if (aspect > corridorAspect)
            {
                float width = rect.width * corridorAspect / aspect;
                rect.x += (rect.width - width) * 0.5f;
                rect.width = width;
            }
            else
            {
                float height = rect.height * aspect / corridorAspect;
                rect.y += (rect.height - height) * 0.5f;
                rect.height = height;
            }

            sceneCamera.rect = rect;
            sceneCamera.orthographicSize = corridorBounds.extents.y;

            var position = sceneCamera.transform.position;
            sceneCamera.transform.position = new Vector3(
                corridorBounds.center.x,
                corridorBounds.center.y,
                position.z);
        }

        /// <summary>Restaura el rectángulo de cámara a la pantalla completa al desactivar el componente.</summary>
        private void OnDisable()
        {
            if (sceneCamera != null)
                sceneCamera.rect = new Rect(0, 0, 1, 1);
        }
    }
}
