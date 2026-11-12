using Enigma.Physics;
using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Agrupa los componentes propios del prefab enemigo sin depender de objetos de la escena.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class DementorActor : MonoBehaviour
    {
        [SerializeField] private DementorMotor2D motor;
        [SerializeField] private DementorDefeat defeat;
        [SerializeField] private DoorPassage passage;
        [SerializeField] private DementorAura aura;
        [SerializeField] private DementorShadowTrail shadowTrail;

        public DementorMotor2D Motor => motor;
        public DoorPassage Passage => passage;

        /// <summary>Enlaza los puntos editables de la escena al comenzar una instancia.</summary>
        public void Configure(Transform start, Transform[] points, Transform portal)
        {
            motor.ConfigureRoute(start, points);
            passage.ConfigurePortal(portal);
        }

        /// <summary>Bloquea el movimiento y permite que las particulas existentes se disipen.</summary>
        public void StopEffects()
        {
            motor.enabled = false;
            aura.StopEmission();
            shadowTrail.StopEmission();
        }

        /// <summary>Reproduce la animacion de derrota antes de destruir el enemigo.</summary>
        public void PlayDefeat() => defeat.Play();
    }
}
