using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Gestiona los emisores del aura preparados en el prefab sin crear partículas ni materiales por código.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class DementorAura : MonoBehaviour
    {
        [SerializeField] private ParticleSystem[] emitters;

        /// <summary>Detiene la emisión; las partículas existentes terminan suavemente su vida.</summary>
        public void StopEmission()
        {
            foreach (var emitter in emitters)
            {
                if (emitter == null)
                    continue;

                emitter.Stop(false, ParticleSystemStopBehavior.StopEmitting);
            }
        }

        /// <summary>Elimina los restos del intento anterior y reactiva el aura en el punto de inicio.</summary>
        public void Restart()
        {
            foreach (var emitter in emitters)
            {
                if (emitter == null)
                    continue;

                emitter.Stop(false, ParticleSystemStopBehavior.StopEmittingAndClear);
                emitter.Play(false);
            }
        }
    }
}
