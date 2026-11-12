using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Controla la desintegración del prefab enemigo antes de retirar su instancia.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class DementorDefeat : MonoBehaviour
    {
        [SerializeField] private Animator animator;
        [SerializeField] private Collider2D actorCollider;
        [SerializeField] private string fallTrigger = "Caer";
        [SerializeField] private string floatingState = "Flotando";

        /// <summary>Desactiva contactos y reproduce la animación existente.</summary>
        public void Play()
        {
            actorCollider.enabled = false;
            animator.SetTrigger(fallTrigger);
        }

        /// <summary>Recupera la animación flotante y los contactos del actor.</summary>
        public void ResetVisual()
        {
            animator.Rebind();
            animator.ResetTrigger(fallTrigger);
            animator.Play(floatingState, 0, 0f);
            actorCollider.enabled = true;
        }
    }
}
