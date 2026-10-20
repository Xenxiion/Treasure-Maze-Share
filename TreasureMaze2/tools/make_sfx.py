# Generates the short sound effects used by the game (16-bit mono PCM WAV, 44100 Hz).
# These sounds are synthesized by this script, so they are original to this project:
# no third-party audio and no license restrictions.
# Run from the project folder:  python3 tools/make_sfx.py
import os
import wave
import numpy as np

RATE = 44100  # samples per second
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "assets", "sfx")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(7)  # fixed seed so the files are reproducible


def t_axis(seconds):
    """Time values for a sound of the given length."""
    return np.arange(int(RATE * seconds)) / RATE


def lowpass(x, k):
    """Very simple low-pass filter: moving average over k samples."""
    if k <= 1:
        return x
    kernel = np.ones(k) / k
    return np.convolve(x, kernel, mode="same")


def fade(x, attack=0.004, release=0.02):
    """Short fade in/out so the sound never clicks."""
    n = len(x)
    a = max(1, int(RATE * attack))
    r = max(1, int(RATE * release))
    env = np.ones(n)
    env[:a] = np.linspace(0, 1, a)
    env[-r:] = np.linspace(1, 0, r)
    return x * env


def save(name, x, peak=0.8):
    """Normalize to the given peak level and write a 16-bit mono WAV."""
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    data = (x * 32767).astype("<i2")
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data.tobytes())
    print("wrote", name + ".wav", round(len(x) / RATE, 2), "s")


# --- player footstep: soft thud plus a little filtered noise ---------------
t = t_axis(0.09)
step = np.sin(2 * np.pi * 110 * t) * np.exp(-t * 45) + lowpass(rng.normal(size=len(t)), 24) * np.exp(-t * 60) * 0.8
save("step", fade(step), 0.55)

# --- mimic footstep: lower, heavier and wetter than the player's ----------
t = t_axis(0.13)
mstep = np.sin(2 * np.pi * 70 * t) * np.exp(-t * 28) + lowpass(rng.normal(size=len(t)), 40) * np.exp(-t * 35) * 0.9
mstep += 0.4 * np.sin(2 * np.pi * 150 * t) * np.exp(-t * 60)
save("mimic_step", fade(mstep), 0.6)

# --- bomb: noise burst that gets duller, plus a falling low boom ----------
t = t_axis(0.8)
noise = rng.normal(size=len(t))
dull = np.zeros_like(noise)
for i, k in enumerate([2, 6, 14, 30]):  # progressively stronger filtering = rumble
    seg = slice(int(len(t) * i / 4), int(len(t) * (i + 1) / 4))
    dull[seg] = lowpass(noise, k)[seg]
boom = np.sin(2 * np.pi * np.cumsum(np.linspace(150, 38, len(t))) / RATE)
bomb = (dull * 1.2 + boom * 1.0) * np.exp(-t * 4.5)
save("bomb", fade(bomb, 0.002, 0.08), 0.9)

# --- mimic catches the player: growling falling saw wave with tremolo ----
t = t_axis(0.65)
freq = np.linspace(260, 70, len(t))
phase = np.cumsum(freq) / RATE
saw = 2 * (phase - np.floor(phase + 0.5))
growl = saw * (0.6 + 0.4 * np.sin(2 * np.pi * 28 * t)) * np.exp(-t * 3.0)
bite = lowpass(rng.normal(size=len(t)), 4) * np.exp(-t * 18) * 0.7
save("mimic_catch", fade(growl + bite, 0.003, 0.1), 0.85)

# --- chest opening: short wooden creak, then a rising chime ---------------
t1 = t_axis(0.22)
creak_f = 180 + 120 * np.sin(2 * np.pi * 9 * t1)  # wobbling pitch = creaking hinge
creak = np.sin(np.cumsum(creak_f) * 2 * np.pi / RATE) * (0.5 + 0.5 * np.sin(2 * np.pi * 31 * t1)) * np.exp(-t1 * 6)
chime = np.zeros(int(RATE * 0.55))
for i, f in enumerate([523.25, 659.25, 783.99, 1046.5]):  # C5 E5 G5 C6
    start = int(RATE * 0.07 * i)
    tt = t_axis(0.55)[: len(chime) - start]
    tone = (np.sin(2 * np.pi * f * tt) + 0.35 * np.sin(2 * np.pi * 2 * f * tt)) * np.exp(-tt * 7)
    chime[start:] += tone
save("chest", fade(np.concatenate([creak * 0.7, chime])), 0.8)

# --- coin pickup: two quick bright blips ----------------------------------
t = t_axis(0.22)
coin = np.where(t < 0.07, np.sign(np.sin(2 * np.pi * 988 * t)), np.sign(np.sin(2 * np.pi * 1319 * t))) * 0.5
coin = lowpass(coin, 2) * np.exp(-t * 11)
save("coin", fade(coin, 0.002, 0.05), 0.6)

# --- countdown tick: one clean short blip ---------------------------------
t = t_axis(0.07)
tick = np.sin(2 * np.pi * 880 * t) * np.exp(-t * 40)
save("tick", fade(tick, 0.002, 0.02), 0.5)
