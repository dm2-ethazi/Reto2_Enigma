using Enigma.Physics;
using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Sustituye enemigos desde un prefab; conserva el recorrido y la puerta preparados en el Editor.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class DementorSpawner : MonoBehaviour
    {
        [SerializeField] private DementorActor prefab;
        [SerializeField] private DementorActor current;
        [SerializeField] private Transform actorParent;
        [SerializeField] private Transform startPoint;
        [SerializeField] private Transform[] advancePoints;
        [SerializeField] private Transform portalCenter;
        [SerializeField] private DoorTrigger2D door;

        public DementorActor Current => current;

        /// <summary>Conecta la instancia inicial visible en la escena, o crea una desde el prefab si falta.</summary>
        public void EnsureActor()
        {
            if (current == null)
                current = Instantiate(prefab, startPoint.position, prefab.transform.rotation, actorParent);

            current.Configure(startPoint, advancePoints, portalCenter);
            door.Bind(current.Motor);
        }

        /// <summary>Elimina el enemigo anterior y genera uno nuevo con su estado original.</summary>
        public void ReplaceActor()
        {
            RemoveActor();
            EnsureActor();
        }

        /// <summary>Retira contactos y efectos inmediatamente; Destroy libera la instancia al terminar el frame.</summary>
        public void RemoveActor()
        {
            door.Bind(null);
            if (current == null)
                return;

            current.gameObject.SetActive(false);
            Destroy(current.gameObject);
            current = null;
        }
    }
}
