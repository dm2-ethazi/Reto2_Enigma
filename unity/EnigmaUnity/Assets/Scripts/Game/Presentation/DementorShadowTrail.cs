using Enigma.Physics;
using UnityEngine;

namespace Enigma.Game
{
    /// <summary>Deja siluetas oscuras en posiciones del mundo durante los avances del dementor.</summary>
    /// <remarks>@author koldo</remarks>
    [DisallowMultipleComponent]
    public sealed class DementorShadowTrail : MonoBehaviour
    {
        [Header("Referencias preparadas en el Editor")]
        [SerializeField] private DementorMotor2D motor;
        [SerializeField] private SpriteRenderer artwork;
        [SerializeField] private ParticleSystem shadows;

        [Header("Estela")]
        [SerializeField, Min(0.05f)] private float spacing = 0.48f;
        [SerializeField, Min(0.1f)] private float lifetime = 0.9f;
        [SerializeField] private Color tint = new Color(0.025f, 0.035f, 0.05f, 0.58f);

        private Vector3 lastPosition;
        private float distanceSinceShadow;
        private bool emitting = true;

        /// <summary>Recuerda la posicion tras el reinicio de la fisica sin emitir una silueta inicial.</summary>
        private void Start() => Restart();

        /// <summary>Muestrea distancias, no fotogramas; no crea objetos ni materiales durante la partida.</summary>
        private void LateUpdate()
        {
            Vector3 current = artwork.bounds.center;
            Vector3 displacement = current - lastPosition;
            if (!emitting || !motor.enabled || !motor.IsMoving || !artwork.enabled || Time.deltaTime <= 0f)
            {
                lastPosition = current;
                distanceSinceShadow = 0f;
                return;
            }

            float distance = displacement.magnitude;
            // Un traslado de reinicio nunca debe dibujar una estela a lo largo de todo el pasillo.
            if (distance > 2f || distance <= Mathf.Epsilon)
            {
                lastPosition = current;
                distanceSinceShadow = 0f;
                return;
            }

            float next = spacing - distanceSinceShadow;
            while (next <= distance)
            {
                EmitShadow(lastPosition + displacement * (next / distance));
                next += spacing;
            }

            distanceSinceShadow = (distanceSinceShadow + distance) % spacing;
            lastPosition = current;
        }

        /// <summary>Emite una silueta inmovil con el tamano real del sprite, independiente de su jerarquia.</summary>
        private void EmitShadow(Vector3 position)
        {
            var particle = new ParticleSystem.EmitParams
            {
                position = position,
                velocity = Vector3.zero,
                startLifetime = lifetime,
                startSize3D = new Vector3(artwork.bounds.size.x, artwork.bounds.size.y, 1f),
                startColor = tint,
                rotation = 0f
            };
            shadows.Emit(particle, 1);
        }

        /// <summary>Interrumpe nuevas siluetas; las existentes se desvanecen de forma natural.</summary>
        public void StopEmission() => emitting = false;

        /// <summary>Limpia la estela del intento anterior antes de volver al punto de inicio.</summary>
        public void Restart()
        {
            shadows.Stop(false, ParticleSystemStopBehavior.StopEmittingAndClear);
            shadows.Play(false);
            lastPosition = artwork.bounds.center;
            distanceSinceShadow = 0f;
            emitting = true;
        }

        /// <summary>Evita restos al desactivar el componente o abandonar la escena.</summary>
        private void OnDisable()
        {
            if (shadows != null)
                shadows.Stop(false, ParticleSystemStopBehavior.StopEmittingAndClear);
        }
    }
}
