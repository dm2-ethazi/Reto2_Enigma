using System;

namespace Enigma.Caesar
{
    /// <summary>
    /// Mantiene el alfabeto A–Z y su desplazamiento César global.
    /// </summary>
    /// <remarks>@author koldo</remarks>
    public sealed class CaesarAlphabet
    {
        /// <summary>Letras originales, en el orden utilizado para el cifrado.</summary>
        public const string Letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        /// <summary>Desplazamiento actual, normalizado entre 0 y 25.</summary>
        public int Offset { get; private set; }

        /// <summary>Notifica a las vistas cuando cambia el desplazamiento global.</summary>
        public event Action ShiftChanged;

        /// <summary>Obtiene una letra del alfabeto sin desplazarla.</summary>
        /// <param name="index">Posición de la letra, entre 0 y 25.</param>
        /// <returns>Letra original de la posición indicada.</returns>
        public char GetOriginal(int index) => Letters[index];

        /// <summary>Obtiene una letra aplicando el desplazamiento y el retorno circular a A.</summary>
        /// <param name="index">Posición original de la letra, entre 0 y 25.</param>
        /// <returns>Letra resultante del desplazamiento César.</returns>
        public char GetShifted(int index) => Letters[(index + Offset) % Letters.Length];

        /// <summary>Desplaza todo el alfabeto y notifica el cambio a las vistas.</summary>
        /// <param name="direction">Número de posiciones; negativo para retroceder.</param>
        public void Shift(int direction)
        {
            int step = direction % Letters.Length;
            Offset = (Offset + step + Letters.Length) % Letters.Length;
            ShiftChanged?.Invoke();
        }
    }
}
