import json
import os

import numpy as np
import soundfile as sf
from scipy import signal

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "dbtenkaichi")
SR = 44100


def n_of(seconds):
    return max(1, int(SR * seconds))


def tt(seconds):
    return np.arange(n_of(seconds)) / SR


def white(seconds, rng):
    return rng.standard_normal(n_of(seconds))


def pink(seconds, rng):
    x = rng.standard_normal(n_of(seconds))
    b = [0.049922035, -0.095993537, 0.050612699, -0.004408786]
    a = [1, -2.494956002, 2.017265875, -0.522189400]
    return signal.lfilter(b, a, x) * 4.0


def brown(seconds, rng):
    x = np.cumsum(rng.standard_normal(n_of(seconds)))
    x = signal.lfilter([1, -1], [1, -0.995], x)
    return x / (np.max(np.abs(x)) + 1e-9)


def lowpass(x, cutoff, order=4):
    sos = signal.butter(order, min(cutoff, SR * 0.45), "low", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def highpass(x, cutoff, order=4):
    sos = signal.butter(order, max(cutoff, 10), "high", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def bandpass(x, low, high, order=3):
    sos = signal.butter(order, [max(low, 10), min(high, SR * 0.45)], "band", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def resonator(x, freq, q):
    b, a = signal.iirpeak(min(freq, SR * 0.45), q, fs=SR)
    return signal.lfilter(b, a, x)


def sweep_filter(x, f_start, f_end, q=2.0, kind="band", blocks=64):
    out = np.zeros_like(x)
    edges = np.linspace(0, len(x), blocks + 1).astype(int)
    zi = None
    for i in range(blocks):
        k = i / max(1, blocks - 1)
        f = f_start * (f_end / f_start) ** k
        f = min(max(f, 30.0), SR * 0.44)
        if kind == "low":
            sos = signal.butter(2, f, "low", fs=SR, output="sos")
        else:
            bw = f / q
            sos = signal.butter(2, [max(f - bw / 2, 20), min(f + bw / 2, SR * 0.45)], "band", fs=SR, output="sos")
        if zi is None or zi.shape != (sos.shape[0], 2):
            zi = np.zeros((sos.shape[0], 2))
        seg, zi = signal.sosfilt(sos, x[edges[i]:edges[i + 1]], zi=zi)
        out[edges[i]:edges[i + 1]] = seg
    return out


def env_adsr(seconds, attack, decay, sustain, release, curve=2.0):
    n = n_of(seconds)
    a = n_of(attack)
    d = n_of(decay)
    r = n_of(release)
    s_len = max(0, n - a - d - r)
    parts = [np.linspace(0, 1, a) ** (1 / curve), 1 - (1 - sustain) * np.linspace(0, 1, d) ** (1 / curve),
             np.full(s_len, sustain), sustain * (1 - np.linspace(0, 1, r)) ** curve]
    e = np.concatenate(parts)
    return pad(e, seconds)


def env_exp(seconds, attack, tau):
    t = tt(seconds)
    a = max(attack, 1e-4)
    rise = np.clip(t / a, 0, 1)
    return rise * np.exp(-np.maximum(t - a, 0) / tau)


def pad(x, seconds):
    n = n_of(seconds)
    if len(x) >= n:
        return x[:n]
    return np.concatenate([x, np.zeros(n - len(x))])


def at(x, offset, total):
    out = np.zeros(n_of(total))
    o = n_of(offset)
    if o >= len(out):
        return out
    seg = x[:len(out) - o]
    out[o:o + len(seg)] = seg
    return out


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def peak(x, level=0.92):
    m = np.max(np.abs(x))
    return x if m < 1e-9 else x / m * level


def saturate(x, drive=2.0):
    return np.tanh(x * drive) / np.tanh(drive)


def compress(x, threshold=0.35, ratio=4.0, attack=0.003, release=0.08):
    level = np.abs(x)
    a = np.exp(-1.0 / (SR * attack))
    r = np.exp(-1.0 / (SR * release))
    env = np.zeros_like(x)
    g = 0.0
    for i in range(0, len(x), 32):
        v = level[i:i + 32].max()
        coeff = a if v > g else r
        g = coeff * g + (1 - coeff) * v
        env[i:i + 32] = g
    over = np.maximum(env / threshold, 1.0)
    gain = over ** (1.0 / ratio - 1.0)
    return x * gain


def make_ir(length, damping=4000.0, density=0.35, seed=7):
    rng = np.random.default_rng(seed)
    n = n_of(length)
    tail = rng.standard_normal(n) * np.exp(-np.linspace(0, 7.0, n))
    tail = lowpass(tail, damping, 2)
    early = np.zeros(n)
    for k in range(10):
        pos = int(rng.uniform(0.004, 0.06) * SR)
        if pos < n:
            early[pos] += rng.uniform(0.3, 0.8) * (1 if k % 2 == 0 else -1)
    ir = early * 0.6 + tail * density
    ir[0] = 0.0
    return ir / (np.sqrt(np.sum(ir ** 2)) + 1e-9)


IRS = {}


def reverb(x, wet=0.25, length=1.2, damping=4000.0):
    key = (length, damping)
    if key not in IRS:
        IRS[key] = make_ir(length, damping)
    w = signal.fftconvolve(x, IRS[key])[:len(x) + n_of(length) // 2]
    out = mix(x * (1 - wet * 0.5), w * wet)
    return out


def blocky(x, amount=0.16, bits=7, hold=2):
    held = np.repeat(x[::hold], hold)[:len(x)]
    if len(held) < len(x):
        held = np.concatenate([held, np.zeros(len(x) - len(held))])
    q = np.round(held * (2 ** bits)) / (2 ** bits)
    return x * (1 - amount) + q * amount


def finish(x, level=0.9, room=0.18, length=0.8, tail_fade=0.02):
    y = reverb(x, room * 0.6, length * 0.75) if room > 0 else x
    y = highpass(y, 32, 2)
    y = lowpass(y, 12500, 2)
    y = blocky(peak(y, 0.95))
    y = compress(y, 0.45, 3.0)
    y = peak(y, level)
    f = n_of(tail_fade)
    y[-f:] *= np.linspace(1, 0, f)
    return y


def osc(freq, seconds, shape="sine", phase=0.0):
    t = tt(seconds)
    f = np.broadcast_to(np.asarray(freq, dtype=float), t.shape) if np.ndim(freq) else np.full(t.shape, float(freq))
    ph = phase + 2 * np.pi * np.cumsum(f) / SR
    if shape == "saw":
        return 2.0 * ((ph / (2 * np.pi)) % 1.0) - 1.0
    if shape == "square":
        return np.tanh(np.sin(ph) * 6.0)
    if shape == "tri":
        return 2.0 * np.abs(2.0 * ((ph / (2 * np.pi)) % 1.0) - 1.0) - 1.0
    return np.sin(ph)


def glide(f0, f1, seconds, curve=1.0):
    k = np.linspace(0, 1, n_of(seconds)) ** curve
    return f0 * (f1 / f0) ** k


def supersaw(freq, seconds, voices=7, detune=0.012, rng=None):
    rng = rng or np.random.default_rng(1)
    out = np.zeros(n_of(seconds))
    for v in range(voices):
        d = 1.0 + detune * (v - voices // 2) / max(1, voices // 2)
        out += osc(np.asarray(freq) * d, seconds, "saw", rng.uniform(0, 6.28))
    return out / voices


def fm(carrier, ratio, index, seconds, index_env=None):
    t = tt(seconds)
    c = np.asarray(carrier) if np.ndim(carrier) else np.full(t.shape, float(carrier))
    mod_phase = 2 * np.pi * np.cumsum(c * ratio) / SR
    idx = index if index_env is None else index * index_env
    car_phase = 2 * np.pi * np.cumsum(c) / SR
    return np.sin(car_phase + idx * np.sin(mod_phase))


def crackle(seconds, rng, rate=120.0, low=1500, high=9000, decay=0.0025):
    n = n_of(seconds)
    imp = np.zeros(n)
    count = int(rate * seconds)
    pos = rng.integers(0, n, count)
    imp[pos] = rng.uniform(-1, 1, count) * rng.uniform(0.3, 1.0, count)
    k = np.exp(-np.arange(n_of(decay * 6)) / (SR * decay))
    sparks = signal.fftconvolve(imp, k)[:n]
    return bandpass(sparks, low, high)


def flanger(x, depth=0.003, rate=0.3, mix_amount=0.5):
    n = len(x)
    t = np.arange(n) / SR
    delay = (depth * 0.5 * (1 + np.sin(2 * np.pi * rate * t))) * SR + 1
    idx = np.arange(n) - delay
    idx0 = np.clip(np.floor(idx).astype(int), 0, n - 1)
    frac = idx - np.floor(idx)
    idx1 = np.clip(idx0 + 1, 0, n - 1)
    delayed = x[idx0] * (1 - frac) + x[idx1] * frac
    return x * (1 - mix_amount) + delayed * mix_amount


def loopable(x, fade=0.12):
    n = n_of(fade)
    head = x[:n].copy()
    out = x[n:].copy()
    ramp = np.linspace(0, 1, n)
    out[-n:] = out[-n:] * (1 - ramp) + head * ramp
    return out


def thump(f0, f1, seconds, tau):
    return osc(glide(f0, f1, seconds, 0.35), seconds) * env_exp(seconds, 0.0015, tau)


def punch(rng, heavy=False):
    s = 0.6 if heavy else 0.32
    pitch = rng.uniform(0.9, 1.12)
    body = thump(150 * pitch, 48, s, 0.09 if heavy else 0.05) * (1.1 if heavy else 0.8)
    smack = bandpass(white(s, rng), 700 * pitch, 3200) * env_exp(s, 0.0008, 0.018 if heavy else 0.012)
    slap = highpass(white(s, rng), 3500) * env_exp(s, 0.0003, 0.006) * 0.6
    click = resonator(white(s, rng) * env_exp(s, 0.0002, 0.0015), 2600 * pitch, 6) * 0.8
    cloth = bandpass(white(s, rng), 1200, 6000) * env_exp(s, 0.004, 0.04) * 0.18
    out = mix(body, smack * 0.9, slap, click, cloth)
    if heavy:
        sub = thump(70, 32, s, 0.16) * 0.8
        crunch = bandpass(crackle(s, rng, 400, 800, 5000, 0.004), 800, 5000) * env_exp(s, 0.002, 0.06) * 0.7
        out = mix(saturate(out, 1.8), sub, crunch)
    return finish(out, 0.92, 0.22 if heavy else 0.12, 0.6)


def whoosh(rng, seconds, f0, f1, peak_at=0.5, q=1.6, level=0.85):
    base = pink(seconds, rng)
    x = np.linspace(0, 1, len(base))
    e = np.where(x < peak_at, (x / peak_at) ** 2, ((1 - x) / (1 - peak_at)) ** 1.6)
    swept = sweep_filter(base, f0, f1, q)
    air = highpass(white(seconds, rng), 5000) * 0.08
    return finish(flanger((swept + air) * e, 0.002, 1.5, 0.35), level, 0.08, 0.4)


def explosion(rng, seconds=3.0, size=1.0):
    crack = highpass(white(seconds, rng), 1800) * env_exp(seconds, 0.0005, 0.012) * 0.9
    boom = thump(90, 28, seconds, 0.35) * 1.4 * size
    body = lowpass(brown(seconds, rng), 900) * env_exp(seconds, 0.004, 0.35) * 1.6
    roar = sweep_filter(pink(seconds, rng), 2500, 120, 1.2) * env_exp(seconds, 0.01, 0.6) * 0.8
    rumble = lowpass(brown(seconds, rng), 180) * env_adsr(seconds, 0.05, 0.4, 0.6, seconds * 0.6) * 1.5
    debris = crackle(seconds, rng, 70, 1500, 7000, 0.003) * env_exp(seconds, 0.05, 0.9) * 0.6
    out = saturate(mix(crack, boom, body, roar, rumble, debris), 1.5)
    return finish(out, 0.95, 0.3, 1.6)


def energy_hum(rng, seconds, base, brightness=1.0, wobble=5.0):
    t = tt(seconds)
    b = float(np.mean(brightness))
    wobble = float(np.mean(wobble))
    vib = 1.0 + 0.006 * np.sin(2 * np.pi * wobble * t) + 0.003 * np.sin(2 * np.pi * 0.7 * t)
    f = base * vib
    core = supersaw(f, seconds, 7, 0.01, rng) * 0.4
    sub = osc(f * 0.5, seconds) * 0.5
    shimmer = fm(f * 4, 1.5, 1.8, seconds) * 0.12 * brightness
    filt = sweep_filter(core, 400 * b, 1400 * b, 1.0, "low", 16)
    air = bandpass(pink(seconds, rng), 300, 3000 * b) * (0.25 + 0.1 * np.sin(2 * np.pi * wobble * 0.5 * t))
    sparks = crackle(seconds, rng, 25 * b, 2500, 9000) * 0.35 * np.asarray(brightness) / max(b, 1e-3)
    return mix(filt, sub, shimmer, air, sparks)


def tone_bell(freq, seconds, decay=0.4, ratio=3.5, index=2.5):
    e = env_exp(seconds, 0.002, decay)
    return fm(freq, ratio, index, seconds, env_exp(seconds, 0.001, decay * 0.4)) * e


def thunder(rng, seconds=3.6, distance=1.0):
    crack = highpass(white(seconds, rng), 2200) * env_exp(seconds, 0.0004, 0.01 + 0.02 * distance) * (1.2 - 0.5 * distance)
    snap = crackle(seconds, rng, 900, 1500, 9000, 0.0015) * env_exp(seconds, 0.001, 0.05) * 0.9
    roll = np.zeros(n_of(seconds))
    for _ in range(5):
        start = rng.uniform(0.02, 0.9)
        dur = seconds - start
        seg = lowpass(brown(dur, rng), rng.uniform(250, 600)) * env_exp(dur, rng.uniform(0.02, 0.2), rng.uniform(0.4, 1.0))
        roll += at(seg * rng.uniform(0.5, 1.0), start, seconds)
    boom = thump(70, 30, seconds, 0.4) * 0.8
    out = saturate(mix(crack, snap, roll * 1.6, boom), 1.4)
    return finish(out, 0.8, 0.35, 2.0)


def rumble(rng, seconds=3.4):
    base = lowpass(brown(seconds, rng), 140) * 2.0
    grind = bandpass(crackle(seconds, rng, 220, 300, 2500, 0.006), 200, 2500) * 0.5
    pebbles = crackle(seconds, rng, 40, 2000, 6000, 0.002) * 0.25
    e = env_adsr(seconds, 0.6, 0.2, 0.85, 0.9)
    trem = 0.8 + 0.2 * np.sin(2 * np.pi * 7.0 * tt(seconds))
    return finish(mix(base * trem, grind, pebbles) * e, 0.9, 0.25, 1.4)


VOWELS = {
    "a": [(800, 1.0, 80), (1150, 0.5, 90), (2900, 0.25, 120), (3900, 0.12, 130)],
    "aa": [(700, 1.0, 70), (1220, 0.55, 80), (2600, 0.3, 110), (3600, 0.15, 130)],
    "o": [(450, 1.0, 70), (800, 0.4, 80), (2830, 0.15, 100), (3500, 0.1, 130)],
}


def voice(rng, seconds, f_curve, vowel_from="o", vowel_to="aa", grit=1.0, breath=0.3):
    n = n_of(seconds)
    jitter = 1.0 + lowpass(rng.standard_normal(n), 30, 2) * 0.03
    vib = 1.0 + 0.018 * np.sin(2 * np.pi * 5.3 * tt(seconds)) * np.linspace(0.3, 1.0, n)
    f = f_curve * jitter * vib
    phase = np.cumsum(f) / SR
    frac = phase % 1.0
    glottal = np.where(frac < 0.6, 0.5 * (1 - np.cos(np.pi * frac / 0.6)), np.cos(np.pi * (frac - 0.6) / 0.8))
    glottal = np.diff(np.concatenate([[0.0], glottal]))
    shimmer = 1.0 + lowpass(rng.standard_normal(n), 40, 2) * 0.15
    src = glottal * shimmer * 40.0
    a = VOWELS[vowel_from]
    b = VOWELS[vowel_to]
    k = np.linspace(0, 1, n)
    out = np.zeros(n)
    for (f1, g1, bw1), (f2, g2, bw2) in zip(a, b):
        blocks = 24
        edges = np.linspace(0, n, blocks + 1).astype(int)
        seg_out = np.zeros(n)
        for i in range(blocks):
            kk = i / (blocks - 1)
            fc = f1 + (f2 - f1) * kk
            gain = g1 + (g2 - g1) * kk
            bw = bw1 + (bw2 - bw1) * kk
            bb, aa = signal.iirpeak(fc, fc / bw, fs=SR)
            lo = max(0, edges[i] - 2000)
            seg = signal.lfilter(bb, aa, src[lo:edges[i + 1]])[edges[i] - lo:]
            seg_out[edges[i]:edges[i + 1]] = seg * gain
        out += seg_out
    noise_breath = bandpass(white(seconds, rng), 900, 5000) * breath * (0.6 + 0.4 * k)
    raw = out + noise_breath * np.max(np.abs(out))
    return saturate(raw / (np.max(np.abs(raw)) + 1e-9), 1.0 + grit * 0.8)


def scream(rng, seconds, f0, f1):
    f = np.concatenate([glide(f0 * 0.8, f0, seconds * 0.25, 0.5), glide(f0, f1, seconds * 0.75, 0.6)])
    f = pad(f, seconds)
    f[f == 0] = f1
    main = voice(rng, seconds, f, "o", "aa", 1.0, 0.12)
    octave = voice(rng, seconds, f * 0.5, "o", "a", 0.8, 0.1) * 0.45
    e = env_adsr(seconds, 0.12, 0.3, 0.85, seconds * 0.25, 1.5)
    power = energy_hum(rng, seconds, 55, 1.2, 7.0) * np.linspace(0.0, 0.35, n_of(seconds))
    return finish(mix((main + octave) * e, power), 0.95, 0.3, 1.4)


def anime_punch(rng, heavy=False):
    s = 0.7 if heavy else 0.38
    pitch = rng.uniform(0.92, 1.1)
    crack = highpass(white(s, rng), 2500) * env_exp(s, 0.0003, 0.007) * 1.1
    smack = sweep_filter(white(s, rng), 2400 * pitch, 380, 3.0, "band", 24) * env_exp(s, 0.0008, 0.05 if heavy else 0.03) * 1.3
    body = thump(170 * pitch, 50, s, 0.11 if heavy else 0.06) * (1.3 if heavy else 0.9)
    snap = resonator(white(s, rng) * env_exp(s, 0.0002, 0.0012), 3100 * pitch, 7) * 0.7
    tail = whoosh(rng, s, 3000, 400, 0.08, 1.4, 0.25)
    out = mix(crack, smack, body, snap, tail * 0.5)
    if heavy:
        sub = thump(75, 30, s, 0.2) * 0.9
        crunch = crackle(s, rng, 500, 900, 6000, 0.003) * env_exp(s, 0.002, 0.08) * 0.6
        out = saturate(mix(out, sub, crunch), 2.0)
    return finish(out, 0.94, 0.16 if heavy else 0.08, 0.5)


def scouter_beep(freq, seconds, level=0.5):
    tone = osc(freq, seconds, "square") * 0.5 + osc(freq * 2.0, seconds, "sine") * 0.15
    return lowpass(tone, 6000, 2) * env_adsr(seconds, 0.002, 0.01, 0.85, min(0.03, seconds * 0.3)) * level


def aura_roar(rng, seconds, base=50.0, brightness=1.0):
    t = tt(seconds)
    flutter = 0.75 + 0.25 * np.sin(2 * np.pi * 9.0 * t) * np.sin(2 * np.pi * 0.7 * t)
    jet = bandpass(brown(seconds, rng), 70, 900) * 1.6 * flutter
    fire = bandpass(pink(seconds, rng), 600, 3200 * brightness) * 0.35 * (0.8 + 0.2 * np.sin(2 * np.pi * 4.3 * t))
    hum = energy_hum(rng, seconds, base, brightness, 6.0) * 0.55
    hiss = crackle(seconds, rng, 60 * brightness, 3000, 10000, 0.0015) * 0.45
    return mix(jet, fire, hum, hiss)


def generate():
    rng = np.random.default_rng(2026)
    sounds = {}

    def put(event, variants, subtitle=True):
        files = []
        folder = event.split(".")[0]
        name = event.split(".", 1)[1]
        for i, data in enumerate(variants):
            rel = "%s/%s_%d" % (folder, name, i + 1)
            out = os.path.join(ASSETS, "sounds", folder, "%s_%d.ogg" % (name, i + 1))
            os.makedirs(os.path.dirname(out), exist_ok=True)
            sf.write(out, np.clip(data, -1.0, 1.0).astype(np.float32), SR, format="OGG", subtype="VORBIS")
            files.append("dbtenkaichi:" + rel)
        entry = {"sounds": files}
        if subtitle:
            entry["subtitle"] = "subtitles.dbtenkaichi." + event
        sounds[event] = entry

    put("combat.punch_light", [anime_punch(rng) for _ in range(4)])
    put("combat.punch_heavy", [anime_punch(rng, True) for _ in range(3)])
    put("combat.kick", [finish(mix(whoosh(rng, 0.2, 500, 4000, 0.55, 1.4, 0.55), at(anime_punch(rng), 0.1, 0.6)), 0.93, 0.08, 0.5) for _ in range(3)])
    put("combat.swing", [whoosh(rng, rng.uniform(0.16, 0.22), rng.uniform(700, 1000), rng.uniform(5000, 8000), 0.35, 2.0) * 0.8 for _ in range(4)])

    def guard():
        s = 0.42
        body = thump(240, 100, s, 0.05)
        ring = resonator(white(s, rng) * env_exp(s, 0.0005, 0.003), 2100, 14) * 0.5
        ring2 = resonator(white(s, rng) * env_exp(s, 0.0005, 0.003), 3500, 20) * 0.3
        scrape = bandpass(white(s, rng), 800, 4000) * env_exp(s, 0.001, 0.035) * 0.6
        return finish(mix(body, ring, ring2, scrape), 0.9, 0.12, 0.5)

    put("combat.guard", [guard() for _ in range(3)])

    def perfect_guard():
        s = 1.0
        bell = mix(tone_bell(1568, s, 0.4, 2.0, 2.0), at(tone_bell(2093, s, 0.35, 2.0, 1.5) * 0.7, 0.05, s))
        shine = sweep_filter(highpass(white(s, rng), 3000), 4000, 12000, 2.0) * env_exp(s, 0.01, 0.12) * 0.3
        return finish(mix(bell, shine, guard() * 0.7), 0.9, 0.25, 1.0)

    put("combat.perfect_guard", [perfect_guard()])
    put("combat.guard_break", [finish(mix(anime_punch(rng, True), osc(glide(1400, 120, 0.6, 0.6), 0.6, "saw") * env_exp(0.6, 0.004, 0.14) * 0.35,
                                         crackle(0.6, rng, 400, 1500, 9000) * env_exp(0.6, 0.001, 0.1) * 0.7), 0.95, 0.2, 0.8)])

    def clash():
        s = 1.2
        a = anime_punch(rng, True)
        b = anime_punch(rng, True)
        shock = sweep_filter(pink(s, rng), 7000, 200, 1.4) * env_exp(s, 0.002, 0.2)
        sub = thump(64, 26, s, 0.35)
        return finish(mix(a, at(b, 0.03, s), shock * 0.9, sub), 0.95, 0.25, 1.1)

    put("combat.clash", [clash() for _ in range(2)])
    put("combat.dodge", [whoosh(rng, 0.2, 1800, 10000, 0.25, 2.2) for _ in range(3)])
    put("combat.dash", [finish(mix(whoosh(rng, 0.42, 300, 8000, 0.18, 1.6), thump(90, 40, 0.42, 0.06) * 0.4), 0.9, 0.08, 0.5) for _ in range(2)])

    def vanish():
        s = 0.5
        vib = 1.0 + 0.03 * np.sin(2 * np.pi * 38 * tt(s))
        zip1 = osc(glide(700, 5200, s, 0.45) * vib, s, "sine") * env_exp(s, 0.004, 0.06) * 0.6
        zip2 = at(osc(glide(1200, 7000, 0.3, 0.5), 0.3, "sine") * env_exp(0.3, 0.003, 0.04) * 0.4, 0.07, s)
        air = whoosh(rng, s, 3000, 12000, 0.15, 2.0, 0.6)
        return finish(mix(zip1, zip2, air), 0.9, 0.2, 0.7)

    put("combat.vanish", [vanish() for _ in range(2)])

    def charge_start():
        s = 1.5
        roar = aura_roar(rng, s, 52, 1.0)
        e = env_adsr(s, 0.25, 0.3, 0.75, 0.6)
        rise = sweep_filter(pink(s, rng), 200, 4000, 1.2) * env_adsr(s, 0.5, 0.2, 0.5, 0.5) * 0.6
        burst = thump(120, 45, s, 0.18) * 0.7
        return finish(mix(roar * e, rise, burst), 0.9, 0.2, 1.0)

    put("ki.charge_start", [charge_start()])
    put("ki.charge_loop", [loopable(finish(aura_roar(rng, 3.0, 50, 1.1), 0.8, 0.12, 0.8, 0.001))], False)

    def ki_blast():
        s = 0.55
        zap = osc(glide(1900, 260, s, 0.35), s, "square") * env_exp(s, 0.002, 0.06) * 0.45
        zap = lowpass(zap, 5000, 2)
        ring = osc(glide(900, 500, s, 0.5), s, "sine") * env_exp(s, 0.002, 0.09) * 0.3
        thrust = sweep_filter(pink(s, rng), 5000, 600, 1.6) * env_exp(s, 0.003, 0.1)
        pop = thump(160, 70, s, 0.04) * 0.6
        sparks = crackle(s, rng, 140, 3000, 10000) * env_exp(s, 0.002, 0.12) * 0.5
        return finish(mix(zap, ring, thrust, pop, sparks), 0.9, 0.14, 0.6)

    put("ki.blast", [ki_blast() for _ in range(4)])
    put("ki.impact", [explosion(rng, 1.1, 0.5) for _ in range(3)])

    def beam_charge():
        s = 3.0
        k = np.linspace(0, 1, n_of(s))
        roar = aura_roar(rng, s, 58, 1.2) * (0.25 + 0.75 * k)
        whine = lowpass(osc(glide(300, 1500, s, 1.3), s, "saw") * 0.08 * k ** 1.6, 4000, 2)
        chord = sum(osc(glide(f, f * 2.0, s, 1.4), s, "sine") for f in (220, 277, 330)) * 0.1 * k ** 1.5
        trem = 0.75 + 0.25 * np.sin(2 * np.pi * np.cumsum(4 + 22 * k ** 2) / SR)
        sparks = crackle(s, rng, 200, 2500, 10000) * k * 0.6
        return finish(mix(roar * trem, whine, chord, sparks), 0.92, 0.2, 1.2)

    put("ki.beam_charge", [beam_charge()])

    def beam_fire():
        s = 2.4
        blast = explosion(rng, s, 0.8) * 0.75
        jet = flanger(bandpass(pink(s, rng), 150, 6000), 0.004, 0.7, 0.5) * env_adsr(s, 0.01, 0.3, 0.75, 1.1)
        core = lowpass(supersaw(glide(120, 85, s, 0.5), s, 9, 0.02, rng), 2000) * env_adsr(s, 0.01, 0.4, 0.6, 1.0) * 0.45
        zap = osc(glide(2400, 400, 0.4, 0.4), 0.4, "square") * env_exp(0.4, 0.002, 0.08) * 0.25
        return finish(mix(blast, jet, core, zap), 0.95, 0.22, 1.3)

    put("ki.beam_fire", [beam_fire()])

    def beam_loop():
        s = 3.0
        jet = flanger(bandpass(pink(s, rng), 120, 5000), 0.004, 0.5, 0.5)
        core = lowpass(supersaw(85, s, 9, 0.02, rng), 1600) * 0.5
        roar = lowpass(brown(s, rng), 260) * 1.3
        sparks = crackle(s, rng, 110, 3000, 10000) * 0.45
        return loopable(finish(mix(jet, core, roar, sparks), 0.85, 0.12, 0.8, 0.001))

    put("ki.beam_loop", [beam_loop()], False)
    put("ki.explosion", [explosion(rng, 3.0, 1.0) for _ in range(3)])

    def clash_loop():
        s = 3.0
        t = tt(s)
        jets = pad(beam_loop(), s)
        grind = crackle(s, rng, 450, 800, 9000, 0.003) * (0.6 + 0.4 * np.sign(np.sin(2 * np.pi * 7 * t)))
        pulse = osc(55, s) * (0.5 + 0.5 * np.sin(2 * np.pi * 2.0 * t)) * 0.45
        return loopable(finish(mix(jets, grind, pulse), 0.88, 0.15, 0.8, 0.001))

    put("ki.clash_loop", [clash_loop()])

    def takeoff():
        s = 0.9
        return finish(mix(whoosh(rng, s, 200, 6000, 0.15, 1.3), thump(90, 38, s, 0.12) * 0.8, aura_roar(rng, s, 60, 0.8) * env_exp(s, 0.02, 0.25) * 0.5), 0.9, 0.12, 0.8)

    put("flight.takeoff", [takeoff() for _ in range(2)])

    def flight_loop():
        s = 3.0
        t = tt(s)
        gust = bandpass(pink(s, rng), 300, 3000) * (0.75 + 0.25 * np.sin(2 * np.pi * 0.66 * t) + 0.1 * np.sin(2 * np.pi * 2.3 * t))
        whistle = resonator(white(s, rng), 1900, 30) * 0.1 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.33 * t))
        return loopable(finish(mix(gust, whistle, lowpass(brown(s, rng), 120) * 0.6), 0.7, 0.0, 0.5, 0.001))

    put("flight.loop", [flight_loop()], False)

    def sonic():
        s = 2.0
        crack = highpass(white(s, rng), 900) * env_exp(s, 0.0003, 0.008) * 1.3
        boom = thump(62, 22, s, 0.3) * 1.4
        tail = lowpass(brown(s, rng), 400) * env_exp(s, 0.02, 0.4) * 1.2
        sweep_air = sweep_filter(pink(s, rng), 9000, 300, 1.2) * env_exp(s, 0.004, 0.25) * 0.7
        return finish(saturate(mix(crack, boom, tail, sweep_air), 1.7), 0.95, 0.3, 1.6)

    put("flight.sonic", [sonic()])

    def land():
        s = 0.9
        impact = thump(100, 32, s, 0.14) * 1.3
        dirt = bandpass(crackle(s, rng, 300, 400, 4000, 0.004), 300, 4000) * env_exp(s, 0.002, 0.15) * 0.7
        dust = bandpass(pink(s, rng), 200, 2500) * env_exp(s, 0.01, 0.22) * 0.5
        return finish(mix(impact, dirt, dust), 0.92, 0.15, 0.8)

    put("flight.land", [land() for _ in range(2)])

    def aura_burst():
        s = 1.9
        whoom = whoosh(rng, s, 100, 7000, 0.1, 1.2, 1.0)
        boom = thump(90, 30, s, 0.26) * 1.2
        roar = aura_roar(rng, s, 60, 1.3) * env_exp(s, 0.01, 0.6) * 0.8
        sparks = crackle(s, rng, 240, 2500, 10000) * env_exp(s, 0.01, 0.5) * 0.6
        return finish(mix(whoom, boom, roar, sparks), 0.95, 0.25, 1.3)

    put("aura.burst", [aura_burst() for _ in range(2)])
    put("aura.loop", [loopable(finish(aura_roar(rng, 3.0, 46, 0.9) * 0.8, 0.75, 0.12, 0.8, 0.001))], False)

    def power_up():
        s = 3.0
        k = np.linspace(0, 1, n_of(s))
        roar = aura_roar(rng, s, 48, 1.1) * (0.3 + 0.7 * k)
        rise = sweep_filter(pink(s, rng), 150, 6000, 1.2) * k ** 1.5 * 0.7
        chord = lowpass(sum(osc(glide(f, f * 1.5, s, 1.3), s, "saw") for f in (110, 165, 220)) * 0.08 * k, 2500)
        quake = lowpass(brown(s, rng), 90) * k * 0.8
        return finish(mix(roar, rise, chord, quake), 0.92, 0.22, 1.2)

    put("transform.power_up", [power_up()])
    put("transform.scream", [scream(rng, 3.0, 150, 290), scream(rng, 3.0, 190, 360), scream(rng, 3.0, 120, 250)])

    def revert():
        s = 1.4
        k = np.linspace(0, 1, n_of(s))
        roar = aura_roar(rng, s, 60, 1.0) * (1 - k) ** 1.8
        exhale = sweep_filter(pink(s, rng), 4000, 150, 1.2) * env_exp(s, 0.02, 0.45) * 0.6
        return finish(mix(roar, exhale), 0.85, 0.2, 1.0)

    put("transform.revert", [revert()])
    put("transform.thunder", [thunder(rng, 3.6, 0.2), thunder(rng, 4.0, 0.6), thunder(rng, 3.4, 0.4)])
    put("transform.rumble", [rumble(rng, 3.4), rumble(rng, 3.0)])

    def click():
        s = 0.09
        tick = resonator(white(s, rng) * env_exp(s, 0.0002, 0.0008), 4600, 10) * 0.5
        return finish(mix(scouter_beep(2637, 0.045, 0.45), tick), 0.55, 0.0, 0.2, 0.005)

    put("ui.click", [click()], False)

    def tab():
        s = 0.16
        return finish(mix(scouter_beep(2349, 0.05, 0.5), at(scouter_beep(3136, 0.05, 0.4), 0.06, s)), 0.5, 0.05, 0.3, 0.005)

    put("ui.tab", [tab()])

    def open_screen():
        s = 0.7
        beeps = [at(scouter_beep(f, 0.045, 0.45), i * 0.07, s) for i, f in enumerate((1976, 2349, 2637, 3136))]
        trill = at(lowpass(osc(3136 + 120 * np.sign(np.sin(2 * np.pi * 30 * tt(0.22))), 0.22, "square") * env_adsr(0.22, 0.005, 0.02, 0.7, 0.08) * 0.18, 6000, 2), 0.3, s)
        hum = bandpass(pink(s, rng), 800, 5000) * env_exp(s, 0.02, 0.2) * 0.12
        return finish(mix(*beeps, trill, hum), 0.55, 0.12, 0.5)

    put("ui.open", [open_screen()])

    def confirm():
        s = 0.8
        return finish(mix(tone_bell(988, s, 0.25, 2.0, 1.4), at(tone_bell(1480, s, 0.3, 2.0, 1.4), 0.07, s) * 0.8,
                          scouter_beep(2960, 0.05, 0.25)), 0.65, 0.2, 0.8)

    put("ui.confirm", [confirm()], False)

    def upgrade():
        s = 0.7
        notes = (1319, 1661, 1976, 2637)
        beeps = [at(scouter_beep(f, 0.05, 0.4), i * 0.055, s) for i, f in enumerate(notes)]
        shine = at(tone_bell(2637, 0.4, 0.25, 2.0, 1.0) * 0.4, 0.22, s)
        return finish(mix(*beeps, shine), 0.6, 0.15, 0.6)

    put("ui.upgrade", [upgrade()])

    def deny():
        s = 0.32
        a = osc(196, 0.12, "square") * env_adsr(0.12, 0.003, 0.02, 0.8, 0.03)
        b = at(osc(165, 0.14, "square") * env_adsr(0.14, 0.003, 0.02, 0.8, 0.04), 0.15, s)
        return finish(lowpass(mix(a, b), 3000, 2) * 0.5, 0.5, 0.05, 0.3, 0.005)

    put("ui.deny", [deny()])

    def beat():
        s = 0.35
        return finish(mix(thump(130, 60, s, 0.07), scouter_beep(1760, 0.04, 0.2)), 0.72, 0.1, 0.5)

    put("training.beat", [beat()], False)

    def success():
        s = 1.1
        notes = (1046.5, 1318.5, 1568.0, 2093.0)
        return finish(mix(*[at(tone_bell(f, s, 0.4, 2.0, 1.3), i * 0.06, s) * (1.0 - i * 0.15) for i, f in enumerate(notes)]), 0.72, 0.25, 1.0)

    put("training.success", [success()])

    def grunt():
        s = 0.45
        f = glide(rng.uniform(125, 155), rng.uniform(85, 105), s, 0.7)
        v = voice(rng, s, f, "aa", "o", 1.6, 0.4)
        return finish(v * env_adsr(s, 0.015, 0.1, 0.7, 0.22), 0.85, 0.1, 0.5)

    put("voice.grunt", [grunt() for _ in range(3)])

    def steam(seconds):
        k = np.linspace(0, 1, n_of(seconds))
        jet = bandpass(white(seconds, rng), 2200, 9000) * (0.75 + 0.25 * np.sin(2 * np.pi * 11.0 * tt(seconds)))
        body = bandpass(pink(seconds, rng), 500, 2400) * 0.45
        whistle = resonator(white(seconds, rng), 3400, 40) * 0.12
        e = env_adsr(seconds, 0.03, 0.15, 0.85, seconds * 0.45, 1.6)
        return finish(mix(jet, body, whistle) * e * (1.0 - 0.3 * k), 0.85, 0.12, 0.6)

    put("transform.steam", [steam(1.4), steam(1.9)])

    def laugh(f0, beats):
        s = 0.2 + beats * 0.24 + 0.5
        out = np.zeros(n_of(s))
        for i in range(beats):
            syl = 0.2
            f = glide(f0 * (1.12 - i * 0.03), f0 * (0.92 - i * 0.03), syl, 0.8)
            v = voice(rng, syl, f, "aa", "a", 1.3, 0.55) * env_adsr(syl, 0.012, 0.05, 0.7, 0.08, 1.4)
            breath = bandpass(white(0.06, rng), 900, 4000) * env_exp(0.06, 0.004, 0.02) * 0.25
            out = out + at(v, 0.05 + i * 0.24, s) + at(breath, 0.02 + i * 0.24, s)
        return finish(out, 0.9, 0.22, 0.9)

    put("transform.laugh", [laugh(118, 5), laugh(105, 6)])

    def gulp():
        s = 0.7
        swallow = sweep_filter(pink(s, rng), 900, 180, 4.0, "band", 24) * env_exp(s, 0.01, 0.12) * 1.2
        pop = thump(260, 90, s, 0.05) * 0.9
        drop = at(thump(140, 55, 0.4, 0.07) * 0.8, 0.16, s)
        return finish(mix(swallow, pop, drop), 0.85, 0.12, 0.5)

    put("transform.gulp", [gulp()])

    def stretch():
        s = 1.1
        f = glide(90, 260, s * 0.6, 1.4)
        f = pad(f, s)
        f[f == 0] = 260
        wobble = 1.0 + 0.08 * np.sin(2 * np.pi * 9.0 * tt(s))
        rubber = lowpass(osc(f * wobble, s, "saw"), 1800, 2) * env_adsr(s, 0.02, 0.2, 0.7, 0.35) * 0.5
        squeak = resonator(white(s, rng) * env_exp(s, 0.05, 0.25), 1300, 12) * 0.25
        mass = thump(70, 40, s, 0.3) * 0.6
        return finish(mix(rubber, squeak, mass), 0.85, 0.15, 0.6)

    put("transform.stretch", [stretch(), stretch()])

    with open(os.path.join(ASSETS, "sounds.json"), "w", encoding="utf-8") as f:
        json.dump(sounds, f, indent=2)
    print("generated %d sound events" % len(sounds))


if __name__ == "__main__":
    generate()
