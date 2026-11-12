# Sonido de acierto

@author koldo

`Effects/DementorDefeated.wav`: disipacion espectral y campanilla de acierto, 3,2 segundos.
Audio original sintetizado localmente, sin grabaciones ni muestras externas.

El generador reproducible esta en `Tools/Audio/generate_enigma_audio.py` y necesita Python con NumPy.
`AudioValidation.json` registra duracion, nivel de pico y RMS. El WAV es PCM
de 16 bits, estereo y 44,1 kHz, sin saturacion digital.

La fuente y el clip estan asignados a `Audio - Game/Dementor Defeated` en la escena.
El efecto se descomprime al cargar. Ajusta el volumen del AudioSource en el Inspector.
`GameAudio` maneja la pausa, el reinicio y el acierto; llegar a la puerta no reproduce el efecto.
