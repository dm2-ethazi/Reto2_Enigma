using System;

namespace Enigma.Game
{
    /// <summary>Cuenta el tiempo disponible sin depender de Unity, de la fisica ni de la interfaz.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class RoundTimer
    {
        public float RemainingSeconds { get; private set; }

        /// <summary>Prepara una cuenta completa para el siguiente periodo de respuesta.</summary>
        public void Reset(float seconds) => RemainingSeconds = Math.Max(1f, seconds);

        /// <summary>Consume tiempo y avisa una sola vez al llegar a cero, sin acumular penalizaciones.</summary>
        public bool Tick(float deltaSeconds)
        {
            if (deltaSeconds <= 0f || RemainingSeconds <= 0f)
                return false;

            RemainingSeconds = Math.Max(0f, RemainingSeconds - deltaSeconds);
            return RemainingSeconds <= 0f;
        }
    }
}
