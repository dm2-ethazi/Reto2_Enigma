using System;
using UnityEngine.UIElements;

namespace Enigma.Caesar
{
    /// <summary>
    /// Vincula una casilla UXML con su letra original fija y su letra desplazada.
    /// </summary>
    /// <remarks>@author koldo</remarks>
    public sealed class CaesarLetterCell : IDisposable
    {
        private readonly CaesarAlphabet alphabet;
        private readonly int index;
        private readonly Label shifted;

        /// <summary>Inicializa las etiquetas y se suscribe al desplazamiento global.</summary>
        /// <param name="root">Raíz de la casilla creada a partir de su plantilla UXML.</param>
        /// <param name="alphabet">Modelo compartido por todas las casillas.</param>
        /// <param name="index">Posición de esta letra en el alfabeto, entre 0 y 25.</param>
        public CaesarLetterCell(VisualElement root, CaesarAlphabet alphabet, int index)
        {
            this.alphabet = alphabet;
            this.index = index;

            root.Q<Label>("originalLetter").text = alphabet.GetOriginal(index).ToString();
            shifted = root.Q<Label>("shiftedLetter");

            alphabet.ShiftChanged += Refresh;
            Refresh();
        }

        /// <summary>Actualiza la letra grande conservando la letra original pequeña.</summary>
        private void Refresh()
        {
            shifted.text = alphabet.GetShifted(index).ToString();
        }

        /// <summary>Elimina la suscripción al modelo cuando se libera la casilla.</summary>
        public void Dispose()
        {
            alphabet.ShiftChanged -= Refresh;
        }
    }
}
