package com.dbtenkaichi.client.cinematic;

import java.util.ArrayList;
import java.util.List;

public final class CinematicScript {
    public final String id;
    final List<Shot> shots = new ArrayList<>();
    final List<Event> events = new ArrayList<>();
    final List<Track> darken = new ArrayList<>();
    final List<Track> flash = new ArrayList<>();
    final List<Track> shake = new ArrayList<>();
    final List<Track> aura = new ArrayList<>();
    final List<Track> rocks = new ArrayList<>();
    final List<Track> flicker = new ArrayList<>();
    final List<Track> streak = new ArrayList<>();
    final List<Track> cloudRush = new ArrayList<>();
    final List<Track> steam = new ArrayList<>();
    final List<Track> grow = new ArrayList<>();
    final List<Smoke> smoke = new ArrayList<>();
    final List<Backdrop> backdrops = new ArrayList<>();
    float formFrom = 1.0f;
    float revealAt = 0.6f;
    int flashColor = 0xFFFFFF;
    int darkColor = 0x10180C;
    int lightningColor = 0xCFE8FF;

    CinematicScript(String id) {
        this.id = id;
    }

    public static CinematicScript create(String id) {
        return new CinematicScript(id);
    }

    public record Camera(float yaw, float pitch, float distance, float height) {
    }

    public record Shot(float from, float to, Camera start, Camera end, boolean ease) {
    }

    public record Track(float from, float to, float a, float b) {
    }

    /** A billowing cloud around the actor: {@code height} is in actor heights above the feet, {@code radius} in blocks. */
    public record Smoke(float from, float to, int color, float height, float radius, float a, float b) {
    }

    public record Backdrop(float from, float to, int color, float zoom, float centerY, float yaw) {
    }

    public enum EventKind {
        LIGHTNING,
        GROUND_BREAK,
        BURST,
        SOUND_THUNDER,
        SOUND_RUMBLE,
        SOUND_SCREAM,
        SOUND_BURST,
        SOUND_POWER,
        SOUND_STEAM,
        SOUND_LAUGH,
        SOUND_CACKLE,
        SOUND_GULP,
        SOUND_STRETCH,
        CHOCOLATE,
        SMOKE_BURST
    }

    public record Event(float at, EventKind kind, float strength) {
    }

    public CinematicScript shot(float from, float to, Camera start, Camera end) {
        shots.add(new Shot(from, to, start, end, true));
        return this;
    }

    public CinematicScript cut(float from, float to, Camera start, Camera end) {
        shots.add(new Shot(from, to, start, end, false));
        return this;
    }

    public CinematicScript event(float at, EventKind kind, float strength) {
        events.add(new Event(at, kind, strength));
        return this;
    }

    public CinematicScript darken(float from, float to, float a, float b) {
        darken.add(new Track(from, to, a, b));
        return this;
    }

    public CinematicScript flash(float from, float to, float a, float b) {
        flash.add(new Track(from, to, a, b));
        return this;
    }

    public CinematicScript shake(float from, float to, float a, float b) {
        shake.add(new Track(from, to, a, b));
        return this;
    }

    public CinematicScript aura(float from, float to, float a, float b) {
        aura.add(new Track(from, to, a, b));
        return this;
    }

    public CinematicScript rocks(float from, float to, float a, float b) {
        rocks.add(new Track(from, to, a, b));
        return this;
    }

    public CinematicScript flicker(float from, float to) {
        flicker.add(new Track(from, to, 1.0f, 1.0f));
        return this;
    }

    public CinematicScript streak(float from, float to) {
        streak.add(new Track(from, to, 0.0f, 1.0f));
        return this;
    }

    public CinematicScript cloudRush(float from, float to, float a, float b) {
        cloudRush.add(new Track(from, to, a, b));
        return this;
    }

    /** Steam jets from the holes on the sides of a Majin's head. */
    public CinematicScript steam(float from, float to, float a, float b) {
        steam.add(new Track(from, to, a, b));
        return this;
    }

    /** Multiplies the actor's size, for bodies that swell or shrink on screen. */
    public CinematicScript grow(float from, float to, float a, float b) {
        grow.add(new Track(from, to, a, b));
        return this;
    }

    public CinematicScript smoke(float from, float to, int color, float height, float radius, float a, float b) {
        smoke.add(new Smoke(from, to, color, height, radius, a, b));
        return this;
    }

    static float sampleOr(List<Track> tracks, float t, float fallback) {
        for (Track track : tracks) {
            if (t >= track.from && t <= track.to) {
                float k = track.to <= track.from ? 1.0f : (t - track.from) / (track.to - track.from);
                return track.a + (track.b - track.a) * k;
            }
        }
        return fallback;
    }

    public CinematicScript backdrop(float from, float to, int color, float zoom, float centerY, float yaw) {
        backdrops.add(new Backdrop(from, to, color, zoom, centerY, yaw));
        return this;
    }

    Backdrop backdropAt(float t) {
        for (Backdrop backdrop : backdrops) {
            if (t >= backdrop.from && t < backdrop.to) {
                return backdrop;
            }
        }
        return null;
    }

    static float progressIn(List<Track> tracks, float t) {
        for (Track track : tracks) {
            if (t >= track.from && t <= track.to) {
                return track.to <= track.from ? 1.0f : (t - track.from) / (track.to - track.from);
            }
        }
        return -1.0f;
    }

    public CinematicScript formFrom(float at) {
        formFrom = at;
        return this;
    }

    public CinematicScript revealAt(float at) {
        revealAt = at;
        return this;
    }

    public CinematicScript colors(int flashColor, int darkColor, int lightningColor) {
        this.flashColor = flashColor;
        this.darkColor = darkColor;
        this.lightningColor = lightningColor;
        return this;
    }

    static float sample(List<Track> tracks, float t) {
        float value = 0.0f;
        for (Track track : tracks) {
            if (t >= track.from && t <= track.to) {
                float k = track.to <= track.from ? 1.0f : (t - track.from) / (track.to - track.from);
                value = Math.max(value, track.a + (track.b - track.a) * k);
            }
        }
        return value;
    }

    Shot shotAt(float t) {
        Shot found = null;
        for (Shot shot : shots) {
            if (t >= shot.from && t < shot.to) {
                found = shot;
            }
        }
        return found;
    }
}
