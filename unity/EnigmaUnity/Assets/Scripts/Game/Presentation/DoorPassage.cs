using System.Collections;
using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Hace desaparecer al dementor hacia el centro de la puerta sin ocultarlo detrás de su sprite.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class DoorPassage : MonoBehaviour
    {
        [SerializeField] private Transform visual;
        [SerializeField] private SpriteRenderer artwork;
        [SerializeField] private Animator animator;
        [SerializeField] private Collider2D actorCollider;
        [SerializeField] private Transform portalCenter;
        [SerializeField, Min(0.1f)] private float duration = 0.75f;

        private Vector3 originalPosition;
        private Vector3 originalScale;
        private Color originalColor;

        public float Duration => duration;

        /// <summary>Recibe el centro de la puerta preparado en la escena al crear una nueva instancia.</summary>
        public void ConfigurePortal(Transform center) => portalCenter = center;

        /// <summary>Conserva la presentación preparada en el Editor para restaurar el siguiente intento.</summary>
        private void Awake()
        {
            originalPosition = visual.localPosition;
            originalScale = visual.localScale;
            originalColor = artwork.color;
        }

        /// <summary>Contrae y desvanece el personaje hacia el punto central del hueco de la puerta.</summary>
        public IEnumerator Play()
        {
            actorCollider.enabled = false;
            animator.enabled = false;
            Vector3 startCenter = artwork.bounds.center;
            Vector3 centerOffset = startCenter - visual.position;
            float elapsed = 0f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = Mathf.SmoothStep(0f, 1f, Mathf.Clamp01(elapsed / duration));
                float size = 1f - progress;
                Vector3 center = Vector3.Lerp(startCenter, portalCenter.position, progress);
                visual.localScale = originalScale * size;
                visual.position = center - centerOffset * size;
                artwork.color = new Color(originalColor.r, originalColor.g, originalColor.b, originalColor.a * size);
                yield return null;
            }

            artwork.enabled = false;
        }

        /// <summary>Restaura tamaño, posición, color y animación al iniciar otro intento.</summary>
        public void ResetVisual()
        {
            visual.localPosition = originalPosition;
            visual.localScale = originalScale;
            artwork.color = originalColor;
            artwork.enabled = true;
            animator.enabled = true;
        }
    }
}
