package com.orynion.runaway;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.util.Random;

/**
 * High-performance, zero-GC particle pool for sparks, smoke, and explosions.
 */
public class ParticleSystem {
    private static final int MAX_PARTICLES = 160;

    private static class Particle {
        boolean active = false;
        float x, y, z;
        float vx, vy, vz;
        float r, g, b, a;
        float size;
        float life;
        float maxLife;
    }

    private final Particle[] pool = new Particle[MAX_PARTICLES];
    private final Mesh quadMesh;
    private final Random rand = new Random();

    // Reusable matrices to prevent allocation during rendering
    private final float[] modelMatrix = new float[16];
    private final float[] mvMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] colorMod = new float[4];

    public ParticleSystem() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            pool[i] = new Particle();
        }
        quadMesh = MeshFactory.createQuad(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void spawn(float x, float y, float z,
                      float vx, float vy, float vz,
                      float r, float g, float b, float a,
                      float size, float life) {
        for (Particle p : pool) {
            if (!p.active) {
                p.active = true;
                p.x = x;
                p.y = y;
                p.z = z;
                p.vx = vx;
                p.vy = vy;
                p.vz = vz;
                p.r = r;
                p.g = g;
                p.b = b;
                p.a = a;
                p.size = size;
                p.life = life;
                p.maxLife = life;
                return;
            }
        }
    }

    public void spawnSparks(float x, float y, float z, int count) {
        for (int i = 0; i < count; i++) {
            float vx = (rand.nextFloat() - 0.5f) * 6.0f;
            float vy = rand.nextFloat() * 5.0f + 1.0f;
            float vz = (rand.nextFloat() - 0.5f) * 6.0f;
            spawn(x, y, z, vx, vy, vz, 1.0f, 0.85f, 0.25f, 1.0f, 0.12f, 0.35f + rand.nextFloat() * 0.25f);
        }
    }

    public void spawnExplosion(float x, float y, float z) {
        // Sparks
        spawnSparks(x, y, z, 20);

        // Fiery blast particles
        for (int i = 0; i < 12; i++) {
            float vx = (rand.nextFloat() - 0.5f) * 4.0f;
            float vy = (rand.nextFloat() - 0.2f) * 3.5f;
            float vz = (rand.nextFloat() - 0.5f) * 4.0f;
            spawn(x, y, z, vx, vy, vz, 1.0f, 0.45f, 0.10f, 0.9f, 0.45f + rand.nextFloat() * 0.35f, 0.6f);
        }

        // Dark smoke billows
        for (int i = 0; i < 8; i++) {
            float vx = (rand.nextFloat() - 0.5f) * 2.0f;
            float vy = rand.nextFloat() * 2.5f + 1.0f;
            float vz = (rand.nextFloat() - 0.5f) * 2.0f;
            spawn(x, y, z, vx, vy, vz, 0.35f, 0.33f, 0.32f, 0.7f, 0.60f + rand.nextFloat() * 0.40f, 1.1f);
        }
    }

    public void spawnMuzzleSmoke(float x, float y, float z, float dirX, float dirY, float dirZ) {
        for (int i = 0; i < 4; i++) {
            float speed = 2.0f + rand.nextFloat() * 3.0f;
            float vx = dirX * speed + (rand.nextFloat() - 0.5f) * 0.8f;
            float vy = dirY * speed + rand.nextFloat() * 0.8f;
            float vz = dirZ * speed + (rand.nextFloat() - 0.5f) * 0.8f;
            spawn(x, y, z, vx, vy, vz, 0.85f, 0.85f, 0.85f, 0.6f, 0.15f, 0.4f);
        }
    }

    public void update(float dt) {
        for (Particle p : pool) {
            if (p.active) {
                p.life -= dt;
                if (p.life <= 0) {
                    p.active = false;
                    continue;
                }

                // Physics update
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                p.z += p.vz * dt;

                // Gravity & air resistance
                p.vy -= 9.8f * dt * 0.5f;
                p.vx *= 0.96f;
                p.vz *= 0.96f;

                // Alpha fade
                p.a = Math.max(0.0f, p.life / p.maxLife);
            }
        }
    }

    public void render(Shader shader, float[] viewMatrix, float[] projMatrix, Camera camera) {
        // Enable blending for particles
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false); // Disable depth writing for translucent particles

        // Unlit
        GLES20.glUniform1i(shader.uUseLightingLoc, 0);

        // Precompute billboard rotation angles matching camera
        float yaw = camera.yaw;
        float pitch = camera.pitch;

        for (Particle p : pool) {
            if (!p.active) continue;

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z);
            Matrix.rotateM(modelMatrix, 0, yaw, 0, 1, 0);
            Matrix.rotateM(modelMatrix, 0, -pitch, 1, 0, 0);
            Matrix.scaleM(modelMatrix, 0, p.size, p.size, p.size);

            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);

            GLES20.glUniformMatrix4fv(shader.uMVMatrixLoc, 1, false, mvMatrix, 0);
            GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0);

            colorMod[0] = p.r;
            colorMod[1] = p.g;
            colorMod[2] = p.b;
            colorMod[3] = p.a;
            GLES20.glUniform4fv(shader.uColorModLoc, 1, colorMod, 0);

            quadMesh.draw(shader);
        }

        // Restore OpenGL state
        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
        colorMod[0] = 1.0f;
        colorMod[1] = 1.0f;
        colorMod[2] = 1.0f;
        colorMod[3] = 1.0f;
        GLES20.glUniform4fv(shader.uColorModLoc, 1, colorMod, 0);
    }
}

