using UnityEngine;

namespace Enigma.Physics
{
    /// <summary>
    /// Mueve el dementor por las posiciones preparadas en la escena mediante un cuerpo cinemático 2D.
    /// </summary>
    /// <remarks>@author koldo</remarks>
    [DisallowMultipleComponent]
    [RequireComponent(typeof(Rigidbody2D), typeof(CapsuleCollider2D))]
    public sealed class DementorMotor2D : MonoBehaviour
    {
        [Header("Referencias de la escena")]
        [SerializeField] private Rigidbody2D body;
        [SerializeField] private Transform startPoint;
        [SerializeField] private Transform[] advancePoints;

        [Header("Movimiento")]
        [SerializeField, Min(0.1f)] private float speed = 8f;
        [SerializeField, Range(0f, 0.5f)] private float speedPenaltyPerError = 0.1f;
        [SerializeField, Min(0.1f)] private float maximumSpeed = 16f;

        private Vector2 targetPosition;
        private int nextPointIndex;
        private int penaltyLevel;

        /// <summary>Velocidad efectiva; la penalizacion no cambia las posiciones discretas de llegada.</summary>
        public float CurrentSpeed => Mathf.Min(maximumSpeed, speed * (1f + speedPenaltyPerError * penaltyLevel));

        /// <summary>Conecta el recorrido preparado en la escena al instanciar un prefab reutilizable.</summary>
        public void ConfigureRoute(Transform start, Transform[] points)
        {
            startPoint = start;
            advancePoints = points;
            ResetToStart();
        }

        /// <summary>Aumenta la velocidad de forma acotada sin modificar el paso de fisica ni acumular ordenes.</summary>
        public void SetPenaltyLevel(int errors) => penaltyLevel = Mathf.Max(0, errors);

        /// <summary>Indica si el cuerpo está avanzando hacia la siguiente posición.</summary>
        public bool IsMoving { get; private set; }

        /// <summary>Indica si la puerta ha detectado al dementor en este intento.</summary>
        public bool HasReachedDoor { get; private set; }

        /// <summary>Número de avances iniciados desde el último reinicio.</summary>
        public int StepsTaken => nextPointIndex;

        /// <summary>Coloca el cuerpo en la posición inicial configurada en el Editor.</summary>
        private void Awake()
        {
            if (body == null)
            {
                Debug.LogError("Configura el cuerpo del dementor en el Inspector.", this);
                enabled = false;
                return;
            }

            ResetToStart();
        }

        /// <summary>Avanza el cuerpo en pasos de física sin modificar el Transform de la animación.</summary>
        private void FixedUpdate()
        {
            if (!IsMoving)
                return;

            Vector2 nextPosition = Vector2.MoveTowards(body.position, targetPosition, CurrentSpeed * Time.fixedDeltaTime);
            body.MovePosition(nextPosition);

            if (nextPosition == targetPosition)
                IsMoving = false;
        }

        /// <summary>Inicia un avance; no acumula órdenes mientras hay otro movimiento en curso.</summary>
        [ContextMenu("Física/Avanzar una posición")]
        public void AdvanceOneStep()
        {
            if (!Application.isPlaying || !enabled || IsMoving || HasReachedDoor ||
                advancePoints == null || nextPointIndex >= advancePoints.Length)
                return;

            targetPosition = advancePoints[nextPointIndex].position;
            nextPointIndex++;
            IsMoving = true;
        }

        /// <summary>Detiene el avance cuando la puerta confirma el contacto físico.</summary>
        public void ReachDoor()
        {
            IsMoving = false;
            HasReachedDoor = true;
            body.linearVelocity = Vector2.zero;
        }

        /// <summary>Reinicia el intento en el punto de inicio, conservando los objetos de la escena.</summary>
        [ContextMenu("Física/Volver al inicio")]
        public void ResetToStart()
        {
            if (!Application.isPlaying || body == null || startPoint == null)
                return;

            IsMoving = false;
            HasReachedDoor = false;
            nextPointIndex = 0;
            penaltyLevel = 0;
            body.linearVelocity = Vector2.zero;
            body.angularVelocity = 0f;
            body.position = startPoint.position;
        }
    }
}
