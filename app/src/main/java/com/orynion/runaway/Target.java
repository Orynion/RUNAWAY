package com.orynion.runaway;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * Hostile NPC target (drones and robotic bandits) running or hovering alongside the railway.
 */
public class Target {
    public static final int TYPE_DRONE = 0;
    public static final int TYPE_BANDIT = 1;

    public static final int STATE_INACTIVE = 0;
    public static final int STATE_ACTIVE = 1;
    public static final int STATE_DYING = 2;

    public int type = TYPE_DRONE;
    public int state = STATE_INACTIVE;

    public float x, y, z;
    public float baseY;
    public float speedZ; // Forward movement speed in world
    public float swaySpeed;
    public float swayAmplitude;
    public float animTime;

    public int maxHp = 1;
    public int hp = 1;
    public float hitFlashTimer = 0.0f;
    public float hitRadius = 1.1f;

    // Death animation
    public float deathTimer = 0.0f;
    private static final float DEATH_DURATION = 0.45f;
    public float spinYaw = 0.0f;

    // Reusable matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] colorMod = new float[4];

    public void spawn(int targetType, float spawnX, float spawnY, float spawnZ, float targetSpeedZ) {
        this.type = targetType;
        this.x = spawnX;
        this.baseY = spawnY;
        this.y = spawnY;
        this.z = spawnZ;
        this.speedZ = targetSpeedZ;
        this.animTime = (float) (Math.random() * 10.0);
        this.swaySpeed = 2.0f + (float) Math.random() * 2.0f;
        this.swayAmplitude = 0.35f + (float) Math.random() * 0.35f;

        if (targetType == TYPE_DRONE) {
            maxHp = 1;
            hitRadius = 0.95f;
        } else {
            maxHp = 2;
            hitRadius = 1.25f;
        }
        this.hp = maxHp;
        this.hitFlashTimer = 0.0f;
        this.deathTimer = 0.0f;
        this.spinYaw = 0.0f;
        this.state = STATE_ACTIVE;
    }

    public void update(float dt, float trainZ, ParticleSystem particleSystem, SoundManager soundManager) {
        if (state == STATE_INACTIVE) return;

        animTime += dt;

        if (hitFlashTimer > 0) {
            hitFlashTimer -= dt;
        }

        if (state == STATE_DYING) {
            deathTimer += dt;
            spinYaw += 540.0f * dt;
            y -= 2.5f * dt;
            if (deathTimer >= DEATH_DURATION) {
                state = STATE_INACTIVE;
            }
            return;
        }

        // Move target along the landscape
        z += speedZ * dt;

        // Hover / bobbing
        if (type == TYPE_DRONE) {
            y = baseY + (float) Math.sin(animTime * swaySpeed) * swayAmplitude;
        } else {
            // Running bob
            y = baseY + Math.abs((float) Math.sin(animTime * 6.0f)) * 0.22f;
        }

        // If target falls far behind the train (player passed it), deactivate
        if (z < trainZ - 25.0f) {
            state = STATE_INACTIVE;
        }
    }

    /**
     * Mathematical 3D ray-sphere intersection test.
     * Returns intersection distance t >= 0 if hit, or -1.0f if missed.
     */
    public float testRayHit(float rOx, float rOy, float rOz, float rDx, float rDy, float rDz) {
        if (state != STATE_ACTIVE) return -1.0f;

        // Target center (offset slightly for torso/drone center)
        float targetCenterY = (type == TYPE_BANDIT) ? (y + 1.0f) : y;

        float vx = x - rOx;
        float vy = targetCenterY - rOy;
        float vz = z - rOz;

        // Project vector onto ray direction
        float proj = vx * rDx + vy * rDy + vz * rDz;
        if (proj < 0.0f) {
            // Behind ray origin
            return -1.0f;
        }

        // Distance squared from sphere center to ray
        float vSq = vx * vx + vy * vy + vz * vz;
        float distSq = vSq - (proj * proj);
        float radSq = hitRadius * hitRadius;

        if (distSq > radSq) {
            return -1.0f; // Miss
        }

        // Hit!
        float t = proj - (float) Math.sqrt(radSq - distSq);
        return (t >= 0.0f) ? t : 0.0f;
    }

    /**
     * Apply damage. Returns true if target was destroyed.
     */
    public boolean takeDamage(int damage, ParticleSystem particleSystem, SoundManager soundManager) {
        if (state != STATE_ACTIVE) return false;

        hp -= damage;
        hitFlashTimer = 0.15f;

        float hitCenterY = (type == TYPE_BANDIT) ? (y + 1.0f) : y;
        particleSystem.spawnSparks(x, hitCenterY, z, 14);

        if (hp <= 0) {
            state = STATE_DYING;
            particleSystem.spawnExplosion(x, hitCenterY, z);
            soundManager.playTargetExplosion();
            return true;
        } else {
            soundManager.playHitMarker();
            return false;
        }
    }

    public void render(Shader shader, float[] viewMatrix, float[] projMatrix,
                       Mesh droneMesh, Mesh banditMesh) {
        if (state == STATE_INACTIVE) return;

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);

        if (state == STATE_DYING) {
            Matrix.rotateM(modelMatrix, 0, spinYaw, 0, 1, 0);
            Matrix.rotateM(modelMatrix, 0, spinYaw * 0.5f, 1, 0, 0);
        } else {
            // Face slightly towards the tracks / train (center X is 0)
            float angleToTrack = (x < 0) ? 25.0f : -25.0f;
            Matrix.rotateM(modelMatrix, 0, angleToTrack, 0, 1, 0);
        }

        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);

        GLES20.glUniform1i(shader.uUseLightingLoc, 1);
        GLES20.glUniformMatrix4fv(shader.uMVMatrixLoc, 1, false, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0);

        if (hitFlashTimer > 0) {
            // Flash bright red-white on hit
            colorMod[0] = 2.0f;
            colorMod[1] = 0.4f;
            colorMod[2] = 0.4f;
            colorMod[3] = 1.0f;
        } else {
            colorMod[0] = 1.0f;
            colorMod[1] = 1.0f;
            colorMod[2] = 1.0f;
            colorMod[3] = 1.0f;
        }
        GLES20.glUniform4fv(shader.uColorModLoc, 1, colorMod, 0);

        if (type == TYPE_DRONE) {
            droneMesh.draw(shader);
        } else {
            banditMesh.draw(shader);
        }

        // Reset color modulation
        colorMod[0] = 1.0f;
        colorMod[1] = 1.0f;
        colorMod[2] = 1.0f;
        colorMod[3] = 1.0f;
        GLES20.glUniform4fv(shader.uColorModLoc, 1, colorMod, 0);
    }
}

