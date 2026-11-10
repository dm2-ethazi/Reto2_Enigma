using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Datos de una ronda editables como asset en el Inspector.</summary>
    /// <remarks>@author koldo</remarks>
    [CreateAssetMenu(menuName = "Enigma/Nivel César")]
    public sealed class LevelDefinition : ScriptableObject
    {
        [SerializeField] private string[] phrases;
        [SerializeField, Range(1, 25)] private int minimumShift = 1;
        [SerializeField, Range(1, 25)] private int maximumShift = 3;
        [SerializeField, Min(1)] private int lifeLimit = 3;
        [SerializeField, Min(1f)] private float timeLimitSeconds = 50f;

        public string[] Phrases => phrases;
        public int MinimumShift => minimumShift;
        public int MaximumShift => maximumShift;
        public int LifeLimit => lifeLimit;

        /// <summary>Segundos disponibles entre avances; el valor se configura por ronda en el Inspector.</summary>
        public float TimeLimitSeconds => Mathf.Max(1f, timeLimitSeconds);
    }
}
