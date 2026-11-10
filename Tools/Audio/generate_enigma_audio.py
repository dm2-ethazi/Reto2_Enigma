"""Original procedural audio for Enigma. @author koldo."""
from pathlib import Path
import json
import wave
import numpy as np

OUTPUT = Path(__file__).resolve().parents[2] / 'unity/EnigmaUnity/Assets/Audio'
RATE = 44100
RNG = np.random.default_rng(31033)


def noise(seconds, low, high):
    count = int(seconds * RATE)
    frequencies = np.fft.rfftfreq(count, 1 / RATE)
    spectrum = RNG.normal(size=len(frequencies)) + 1j * RNG.normal(size=len(frequencies))
    response = (1 - np.exp(-(frequencies / low) ** 4)) * np.exp(-(frequencies / high) ** 4)
    response /= np.sqrt(np.maximum(frequencies, low))
    signal = np.fft.irfft(spectrum * response, n=count)
    return signal / max(np.std(signal), 1e-9)


def save(relative, stereo, peak):
    stereo -= np.mean(stereo, axis=0)
    stereo *= peak / max(np.max(np.abs(stereo)), 1e-9)
    path = OUTPUT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = np.round(np.clip(stereo, -1, 1) * 32767).astype('<i2')
    with wave.open(str(path), 'wb') as audio:
        audio.setnchannels(2)
        audio.setsampwidth(2)
        audio.setframerate(RATE)
        audio.writeframes(pcm.tobytes())
    return {'file': relative, 'seconds': len(stereo) / RATE,
            'peak_dbfs': float(20 * np.log10(np.max(np.abs(stereo)))),
            'rms_dbfs': float(20 * np.log10(np.sqrt(np.mean(stereo ** 2)))),
            'loop_boundary_step': float(np.max(np.abs(stereo[-1] - stereo[0]))),
            'finite': bool(np.isfinite(stereo).all())}


# Preserve the original effect noise sequence. @author koldo.
RNG.bit_generator.state = {'bit_generator': 'PCG64', 'state': {'state': 73925364023946896647352960708756202942, 'inc': 96879182929147072029232308373140613095}, 'has_uint32': 0, 'uinteger': 0}
reports = []

# A short reverse breath collapses, followed by a restrained open-fifth chime.
duration = 3.2
time = np.arange(int(duration * RATE)) / RATE
breath = noise(duration, 180, 2600)
envelope = np.where(time < .35, (np.maximum(time, 0) / .35) ** 1.4,
                    np.exp(-(time - .35) * 4.8))
vanish = .12 * breath * envelope
phase = 2 * np.pi * (160 * time - 35 * time ** 2)
vanish += .14 * np.sin(phase) * np.exp(-time * 4) * np.minimum(time / .025, 1)
chime = np.zeros_like(time)
for frequency, onset, strength in [(587.33, .28, .15), (880, .36, .09), (1174.66, .44, .065)]:
    age = np.maximum(time - onset, 0)
    attack = 1 - np.exp(-age * 110)
    chime += strength * (np.sin(2 * np.pi * frequency * age)
        + .15 * np.sin(2 * np.pi * frequency * 2.003 * age)) * attack * np.exp(-age * 2.8) * (time >= onset)
base = vanish + chime
left = base.copy()
right = base.copy()
for delay, gain in [(.11, .21), (.23, .14), (.37, .085), (.53, .045)]:
    samples = int(delay * RATE)
    left[samples:] += base[:-samples] * gain
    alternate = int((delay + .023) * RATE)
    right[alternate:] += base[:-alternate] * gain
stereo = np.column_stack([left, right])
stereo *= np.minimum(time / .012, 1)[:, None]
stereo *= np.minimum(np.maximum(duration - time, 0) / .25, 1)[:, None]
reports.append(save('Effects/DementorDefeated.wav', stereo, .68))
(OUTPUT / 'AudioValidation.json').write_text(json.dumps(reports, indent=2), encoding='utf-8')
print(json.dumps(reports, indent=2))
