using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Orden de los niveles y reglas compartidas de la campaña, configurados en el Editor.</summary>
    /// <remarks>@author koldo</remarks>
    [CreateAssetMenu(menuName = "Enigma/Campaña César")]
    public sealed class CampaignSettings : ScriptableObject
    {
        [SerializeField] private LevelDefinition[] levels;
        [SerializeField, Min(1)] private int errorsPerAttempt = 4;
        [SerializeField, Min(0f)] private float resolutionSeconds = 1.15f;

        public LevelDefinition[] Levels => levels;
        public int ErrorsPerAttempt => errorsPerAttempt;
        public float ResolutionSeconds => resolutionSeconds;
    }
}
