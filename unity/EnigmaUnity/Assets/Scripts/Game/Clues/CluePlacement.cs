using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Escoge un símbolo y uno de los seis puntos preparados en el Editor, sin crear objetos.</summary>
    /// <remarks>@author koldo</remarks>
    public sealed class CluePlacement : MonoBehaviour
    {
        [SerializeField] private SpriteRenderer artwork;
        [SerializeField] private Sprite[] symbols;
        [SerializeField] private Transform[] spots;
        [SerializeField, Min(0.1f)] private float worldSize = 1.4f;

        private int previousSymbol = -1;
        private int previousSpot = -1;

        public bool Revealed { get; private set; }
        public Vector3 Position => artwork.transform.position;
        public int SpotIndex => previousSpot;
        public int SymbolIndex => previousSymbol;

        /// <summary>Randomiza la pista al comenzar un nivel, evitando repetir posición y símbolo consecutivos.</summary>
        public void SelectForLevel()
        {
            previousSymbol = PickDifferent(symbols.Length, previousSymbol);
            previousSpot = PickDifferent(spots.Length, previousSpot);
            artwork.sprite = symbols[previousSymbol];
            artwork.transform.position = spots[previousSpot].position;
            artwork.transform.localScale = Vector3.one * worldSize /
                Mathf.Max(artwork.sprite.bounds.size.x, artwork.sprite.bounds.size.y);
            artwork.enabled = true;
            Revealed = false;
        }

        /// <summary>Alterna la nota de ayuda sin cambiar la pista del nivel.</summary>
        public void ToggleReveal() => Revealed = !Revealed;

        /// <summary>Oculta el símbolo al terminar la campaña.</summary>
        public void Hide() => artwork.enabled = false;

        /// <summary>Selecciona un índice distinto del anterior sin bucles de reintento.</summary>
        private static int PickDifferent(int count, int previous)
        {
            if (count <= 1)
                return 0;

            int index = Random.Range(0, previous < 0 ? count : count - 1);
            return previous >= 0 && index >= previous ? index + 1 : index;
        }
    }
}
