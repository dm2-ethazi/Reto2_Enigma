using UnityEngine;
using UnityEngine.Events;

namespace Enigma.Physics
{
    /// <summary>Detecta la llegada del dementor mediante un trigger 2D independiente del arte de la puerta.</summary>
    /// <remarks>@author koldo</remarks>
    [DisallowMultipleComponent]
    [RequireComponent(typeof(BoxCollider2D))]
    public sealed class DoorTrigger2D : MonoBehaviour
    {
        [SerializeField] private DementorMotor2D dementor;
        [SerializeField] private UnityEvent onDementorReached = new UnityEvent();

        /// <summary>Evento para conectar más adelante la resolución del intento desde el Inspector.</summary>
        public UnityEvent DementorReached => onDementorReached;

        /// <summary>Asigna la nueva instancia del enemigo sin guardar referencias externas en el prefab de puerta.</summary>
        public void Bind(DementorMotor2D actor) => dementor = actor;

        /// <summary>Detiene al actor asignado y emite una sola llegada por intento.</summary>
        /// <param name="other">Collider que ha entrado en el área de llegada.</param>
        private void OnTriggerEnter2D(Collider2D other)
        {
            TryArrival(other);
        }

        /// <summary>Confirma la llegada al punto preparado en el Editor, sin resolverla en el borde del trigger.</summary>
        private void TryArrival(Collider2D other)
        {
            if (dementor == null || dementor.HasReachedDoor || other.attachedRigidbody == null)
                return;

            if (other.attachedRigidbody.GetComponent<DementorMotor2D>() != dementor)
                return;

            dementor.ReachDoor();
            onDementorReached.Invoke();
        }
    }
}
