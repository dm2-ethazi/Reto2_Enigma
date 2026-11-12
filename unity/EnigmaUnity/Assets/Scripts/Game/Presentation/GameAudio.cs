using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Reproduce el acierto con la fuente y el clip preparados en el Editor.</summary>
    /// <remarks>@author koldo</remarks>
    [DisallowMultipleComponent]
    public sealed class GameAudio : MonoBehaviour
    {
        [SerializeField] private AudioSource success;

        /// <summary>Reproduce la disipacion del dementor una sola vez al acertar la respuesta.</summary>
        public void PlaySuccess() => success.Play();

        /// <summary>Congela el efecto durante la pausa y conserva el punto de reproduccion.</summary>
        public void SetPaused(bool paused)
        {
            if (paused)
                success.Pause();
            else
                success.UnPause();
        }

        /// <summary>Elimina el efecto anterior al reiniciar la campana.</summary>
        public void Restart() => success.Stop();
    }
}
