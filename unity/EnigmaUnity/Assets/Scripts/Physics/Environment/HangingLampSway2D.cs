using UnityEngine;

namespace Enigma.Physics
{
    /// <summary>
    /// Aplica una brisa suave a una lámpara suspendida; la articulación del Editor limita su oscilación.
    /// </summary>
    /// <remarks>@author koldo</remarks>
    [DisallowMultipleComponent]
    [RequireComponent(typeof(Rigidbody2D), typeof(HingeJoint2D))]
    public sealed class HangingLampSway2D : MonoBehaviour
    {
        [SerializeField] private Rigidbody2D body;

        [Header("Brisa")]
        [SerializeField, Min(0f)] private float torque = 0.12f;
        [SerializeField, Min(0.01f)] private float frequency = 0.35f;
        [SerializeField, Range(0f, 360f)] private float phaseDegrees;
        [SerializeField, Range(-5f, 5f)] private float initialAngularVelocity = 1.5f;

        /// <summary>Inicia una oscilación pequeña sin cambiar los anclajes preparados en la escena.</summary>
        private void Awake()
        {
            if (body == null)
            {
                Debug.LogError("Asigna el cuerpo de la lámpara en el Inspector.", this);
                enabled = false;
                return;
            }

            body.angularVelocity = initialAngularVelocity;
        }

        /// <summary>Añade un par periódico; la gravedad y la articulación producen el balanceo.</summary>
        private void FixedUpdate()
        {
            float phase = Time.fixedTime * frequency * 2f * Mathf.PI + phaseDegrees * Mathf.Deg2Rad;
            body.AddTorque(Mathf.Sin(phase) * torque, ForceMode2D.Force);
        }
    }
}
