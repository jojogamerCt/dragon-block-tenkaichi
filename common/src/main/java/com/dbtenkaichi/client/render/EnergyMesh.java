package com.dbtenkaichi.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

public final class EnergyMesh {
    private static final int SIDES = 12;

    private EnergyMesh() {
    }

    public static void beam(Matrix4fc pose, VertexConsumer buffer, Vec3 dir, float length, float radius, int core, int glow, float age, float fade, boolean clashing, int quality) {
        if (length < 0.05f || radius <= 0.0f) {
            return;
        }
        Vec3 d = dir.normalize();
        Vec3 helper = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = d.cross(helper).normalize();
        Vec3 v = d.cross(u).normalize();
        int lengthSegments = Math.max(2, Math.min(48, (int) (length / 1.5f)));
        float[][] layers = quality >= 3
                ? new float[][]{{0.42f, 0.95f, 0}, {0.8f, 0.55f, 1}, {1.3f, 0.26f, 1}, {1.9f, 0.1f, 1}}
                : quality == 2 ? new float[][]{{0.42f, 0.95f, 0}, {0.85f, 0.5f, 1}, {1.45f, 0.18f, 1}}
                : new float[][]{{0.45f, 0.95f, 0}, {1.1f, 0.35f, 1}};
        for (float[] layer : layers) {
            float r = radius * layer[0] * fade;
            float alpha = layer[1] * fade;
            int color = layer[2] == 0 ? core : glow;
            for (int s = 0; s < lengthSegments; s++) {
                float t0 = (float) s / lengthSegments;
                float t1 = (float) (s + 1) / lengthSegments;
                float wob0 = 1.0f + (float) Math.sin(age * 0.9f + t0 * length * 0.8f) * 0.06f;
                float wob1 = 1.0f + (float) Math.sin(age * 0.9f + t1 * length * 0.8f) * 0.06f;
                float band0 = 0.85f + 0.15f * (float) Math.sin(t0 * length * 1.1f - age * 0.9f);
                float band1 = 0.85f + 0.15f * (float) Math.sin(t1 * length * 1.1f - age * 0.9f);
                float taper0 = Math.min(1.0f, 0.55f + t0 * 3.0f);
                float taper1 = Math.min(1.0f, 0.55f + t1 * 3.0f);
                if (layer[2] == 0) {
                    prism(pose, buffer, d, u, v, t0 * length, t1 * length, r * wob0 * taper0, r * wob1 * taper1, color, alpha * band0, alpha * band1, age * 0.12f);
                } else {
                    tube(pose, buffer, d, u, v, t0 * length, t1 * length, r * wob0 * taper0, r * wob1 * taper1, color, alpha * band0, alpha * band1);
                }
            }
        }
        Vec3 head = d.scale(length);
        float headRadius = Math.min(1.6f, radius * (clashing ? 1.5f + (float) Math.sin(age * 0.8f) * 0.15f : 1.3f)) * fade;
        sphere(pose, buffer, head, headRadius * 0.55f, core, 0.95f * fade);
        sphere(pose, buffer, head, headRadius, glow, 0.35f * fade);
        if (quality >= 2) {
            sphere(pose, buffer, head, headRadius * 1.3f, glow, 0.1f * fade);
        }
        sphere(pose, buffer, Vec3.ZERO, Math.min(0.55f, radius * 0.55f) * fade, core, 0.7f * fade);
        sphere(pose, buffer, Vec3.ZERO, Math.min(0.9f, radius * 0.9f) * fade, glow, 0.18f * fade);
    }

    public static void orb(Matrix4fc pose, VertexConsumer buffer, Vec3 center, float radius, int core, int glow, Vec3 trail, float age) {
        float pulse = 1.0f + (float) Math.sin(age * 1.3f) * 0.08f;
        cube(pose, buffer, center, radius * 0.5f * pulse, age * 0.16f, core, 0.95f);
        cube(pose, buffer, center, radius * 0.72f * pulse, -age * 0.11f + 0.6f, glow, 0.5f);
        sphere(pose, buffer, center, radius * 1.1f * pulse, glow, 0.45f);
        sphere(pose, buffer, center, radius * 1.7f * pulse, glow, 0.15f);
        if (trail.lengthSqr() > 1.0e-4) {
            Vec3 d = trail.normalize();
            Vec3 helper = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 u = d.cross(helper).normalize();
            Vec3 v = d.cross(u).normalize();
            float len = (float) Math.min(2.5, trail.length() * 1.2);
            int segments = 4;
            for (int s = 0; s < segments; s++) {
                float t0 = (float) s / segments;
                float t1 = (float) (s + 1) / segments;
                tubeAt(pose, buffer, center, d, u, v, t0 * len, t1 * len, radius * 0.9f * (1 - t0), radius * 0.9f * (1 - t1), glow, 0.35f * (1 - t0), 0.35f * (1 - t1));
            }
        }
    }

    public static void spiral(Matrix4fc pose, VertexConsumer buffer, Vec3 dir, float length, float radius, int color, float age, float fade) {
        if (length < 0.05f) {
            return;
        }
        Vec3 d = dir.normalize();
        Vec3 helper = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = d.cross(helper).normalize();
        Vec3 v = d.cross(u).normalize();
        float coil = Math.max(0.3f, radius * 0.75f) * fade;
        float thickness = Math.max(0.07f, radius * 0.16f) * fade;
        float pitch = 1.2f;
        float step = 0.2f;
        int count = Math.min(480, (int) (length / step));
        for (int i = 0; i < count; i++) {
            float t0 = i * step;
            float t1 = t0 + step;
            Vec3 p0 = helix(d, u, v, t0, coil, pitch, age);
            Vec3 p1 = helix(d, u, v, t1, coil, pitch, age);
            Vec3 seg = p1.subtract(p0);
            double len = seg.length();
            if (len < 1.0e-4) {
                continue;
            }
            Vec3 sd = seg.scale(1.0 / len);
            Vec3 h2 = Math.abs(sd.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 su = sd.cross(h2).normalize();
            Vec3 sv = sd.cross(su).normalize();
            float taper = Math.min(1.0f, 0.35f + t0 * 0.4f);
            tubeAt(pose, buffer, p0, sd, su, sv, 0.0f, (float) len, thickness * taper, thickness * taper, color, 0.9f * fade, 0.9f * fade);
            tubeAt(pose, buffer, p0, sd, su, sv, 0.0f, (float) len, thickness * taper * 2.4f, thickness * taper * 2.4f, color, 0.22f * fade, 0.22f * fade);
        }
    }

    private static void prism(Matrix4fc pose, VertexConsumer buffer, Vec3 d, Vec3 u, Vec3 v, float from, float to, float r0, float r1, int color, float a0, float a1, float spin) {
        for (int i = 0; i < 4; i++) {
            double ang0 = Math.PI * 0.5 * i + spin;
            double ang1 = Math.PI * 0.5 * (i + 1) + spin;
            Vec3 n0 = u.scale(Math.cos(ang0)).add(v.scale(Math.sin(ang0)));
            Vec3 n1 = u.scale(Math.cos(ang1)).add(v.scale(Math.sin(ang1)));
            quad(pose, buffer, d.scale(from).add(n0.scale(r0)), d.scale(from).add(n1.scale(r0)), d.scale(to).add(n1.scale(r1)), d.scale(to).add(n0.scale(r1)), color, a0, a0, a1, a1);
        }
    }

    public static void cube(Matrix4fc pose, VertexConsumer buffer, Vec3 c, float half, float spin, int color, float alpha) {
        double cy = Math.cos(spin);
        double sy = Math.sin(spin);
        double tilt = 0.6154797;
        double ct = Math.cos(tilt);
        double st = Math.sin(tilt);
        Vec3 ax = new Vec3(cy, 0, sy);
        Vec3 az = new Vec3(-sy, 0, cy);
        Vec3 ay = new Vec3(0, 1, 0);
        Vec3 tx = ax.scale(ct).add(ay.scale(st));
        Vec3 ty = ay.scale(ct).subtract(ax.scale(st));
        Vec3 x = tx.scale(half);
        Vec3 y = ty.scale(half);
        Vec3 z = az.scale(half);
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = c.add(x.scale((i & 1) == 0 ? -1 : 1)).add(y.scale((i & 2) == 0 ? -1 : 1)).add(z.scale((i & 4) == 0 ? -1 : 1));
        }
        int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
        for (int[] f : faces) {
            quad(pose, buffer, corners[f[0]], corners[f[1]], corners[f[2]], corners[f[3]], color, alpha, alpha, alpha, alpha);
        }
    }

    private static Vec3 helix(Vec3 d, Vec3 u, Vec3 v, float t, float coil, float pitch, float age) {
        double a = t / pitch * Math.PI * 2 - age * 0.7;
        return d.scale(t).add(u.scale(Math.cos(a) * coil)).add(v.scale(Math.sin(a) * coil));
    }

    private static void tube(Matrix4fc pose, VertexConsumer buffer, Vec3 d, Vec3 u, Vec3 v, float from, float to, float r0, float r1, int color, float a0, float a1) {
        tubeAt(pose, buffer, Vec3.ZERO, d, u, v, from, to, r0, r1, color, a0, a1);
    }

    private static void tubeAt(Matrix4fc pose, VertexConsumer buffer, Vec3 origin, Vec3 d, Vec3 u, Vec3 v, float from, float to, float r0, float r1, int color, float a0, float a1) {
        for (int i = 0; i < SIDES; i++) {
            double ang0 = Math.PI * 2 * i / SIDES;
            double ang1 = Math.PI * 2 * (i + 1) / SIDES;
            Vec3 n0 = u.scale(Math.cos(ang0)).add(v.scale(Math.sin(ang0)));
            Vec3 n1 = u.scale(Math.cos(ang1)).add(v.scale(Math.sin(ang1)));
            Vec3 p0 = origin.add(d.scale(from)).add(n0.scale(r0));
            Vec3 p1 = origin.add(d.scale(from)).add(n1.scale(r0));
            Vec3 p2 = origin.add(d.scale(to)).add(n1.scale(r1));
            Vec3 p3 = origin.add(d.scale(to)).add(n0.scale(r1));
            quad(pose, buffer, p0, p1, p2, p3, color, a0, a0, a1, a1);
        }
    }

    public static void sphere(Matrix4fc pose, VertexConsumer buffer, Vec3 c, float r, int color, float alpha) {
        int lat = 6;
        int lon = 10;
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat - Math.PI / 2;
            double t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon;
                double p1 = Math.PI * 2 * (j + 1) / lon;
                Vec3 a = point(c, r, t0, p0);
                Vec3 b = point(c, r, t0, p1);
                Vec3 cc = point(c, r, t1, p1);
                Vec3 dd = point(c, r, t1, p0);
                quad(pose, buffer, a, b, cc, dd, color, alpha, alpha, alpha, alpha);
            }
        }
    }

    private static Vec3 point(Vec3 c, float r, double theta, double phi) {
        return new Vec3(c.x + r * Math.cos(theta) * Math.cos(phi), c.y + r * Math.sin(theta), c.z + r * Math.cos(theta) * Math.sin(phi));
    }

    private static void quad(Matrix4fc pose, VertexConsumer buffer, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color, float a0, float a1, float a2, float a3) {
        vertex(pose, buffer, p0, color, a0);
        vertex(pose, buffer, p1, color, a1);
        vertex(pose, buffer, p2, color, a2);
        vertex(pose, buffer, p3, color, a3);
        vertex(pose, buffer, p3, color, a3);
        vertex(pose, buffer, p2, color, a2);
        vertex(pose, buffer, p1, color, a1);
        vertex(pose, buffer, p0, color, a0);
    }

    private static void vertex(Matrix4fc pose, VertexConsumer buffer, Vec3 p, int rgb, float alpha) {
        buffer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor((rgb >> 16 & 255) / 255.0f, (rgb >> 8 & 255) / 255.0f, (rgb & 255) / 255.0f, Math.max(0.0f, Math.min(1.0f, alpha)));
    }
}
