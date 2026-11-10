using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UIElements;

namespace Enigma.Caesar
{
    /// <summary>
    /// Construye la cuadrícula del alfabeto y conecta sus dos botones globales al modelo César.
    /// </summary>
    /// <remarks>@author koldo</remarks>
    [RequireComponent(typeof(UIDocument))]
    public sealed class CaesarPanelController : MonoBehaviour
    {
        [SerializeField] private VisualTreeAsset letterCellTemplate;

        private readonly CaesarAlphabet alphabet = new CaesarAlphabet();
        private readonly List<CaesarLetterCell> cells = new List<CaesarLetterCell>();
        private Label feedback;
        private VisualElement boundRoot;
        private Button previous;
        private Button next;

        /// <summary>Detecta si UI Toolkit ha sustituido la raíz del documento para volver a vincularla.</summary>
        private void Update()
        {
            var root = GetComponent<UIDocument>().rootVisualElement;

            if (root != boundRoot)
                Bind(root);
        }

        /// <summary>Reconstruye las casillas y conecta los eventos de la interfaz.</summary>
        /// <param name="root">Nueva raíz del documento; puede ser nula mientras se reconstruye.</param>
        private void Bind(VisualElement root)
        {
            ReleaseCells();
            boundRoot = root;

            if (root == null)
                return;

            var grid = root.Q<VisualElement>("alphabetGrid");
            feedback = root.Q<Label>("shiftFeedback");

            if (grid == null || feedback == null || letterCellTemplate == null)
            {
                Debug.LogError("El panel César necesita su UXML y la plantilla de casilla.", this);
                return;
            }

            grid.Clear();

            for (int index = 0; index < CaesarAlphabet.Letters.Length; index++)
            {
                var slot = new VisualElement();
                slot.AddToClassList("letter-slot");
                letterCellTemplate.CloneTree(slot);
                grid.Add(slot);
                cells.Add(new CaesarLetterCell(slot, alphabet, index));
            }

            previous = root.Q<Button>("previousLetter");
            next = root.Q<Button>("nextLetter");
            previous.clicked += MoveLeft;
            next.clicked += MoveRight;
            alphabet.ShiftChanged += ShowShift;
            ShowShift();
        }

        /// <summary>Retrocede todas las letras una posición, de A a Z al alcanzar el inicio.</summary>
        private void MoveLeft() => alphabet.Shift(-1);

        /// <summary>Avanza todas las letras una posición, de Z a A al alcanzar el final.</summary>
        private void MoveRight() => alphabet.Shift(1);

        /// <summary>Muestra el desplazamiento actual y las correspondencias de A y Z.</summary>
        private void ShowShift()
        {
            feedback.text = $"Desplazamiento +{alphabet.Offset}   ·   A → {alphabet.GetShifted(0)}   ·   Z → {alphabet.GetShifted(25)}";
        }

        /// <summary>Desconecta la interfaz al desactivar el componente.</summary>
        private void OnDisable()
        {
            ReleaseCells();
            boundRoot = null;
        }

        /// <summary>Libera las casillas y elimina las suscripciones al modelo y a los botones.</summary>
        private void ReleaseCells()
        {
            alphabet.ShiftChanged -= ShowShift;

            if (previous != null)
                previous.clicked -= MoveLeft;

            if (next != null)
                next.clicked -= MoveRight;

            previous = null;
            next = null;

            foreach (var cell in cells)
                cell.Dispose();

            cells.Clear();
        }
    }
}
