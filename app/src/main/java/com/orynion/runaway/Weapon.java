package com.orynion.runaway;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * First-person weapon model with recoil animation, procedural muzzle flash,
 * ammo management, and reload mechanics.
 */
public class Weapon {
    public static final int MAX_MAGAZINE = 10;
    public static final int INITIAL_RESERVE = 60;
    public static final float FIRE_COOLDOWN = 0.22f; // Seconds between shots
    public static final float RELOAD_TIME = 1.25f;   // Seconds to reload
    public static final float FLASH_DURATION = 0.05f;

    public int magazineAmmo = MAX_MAGAZINE;
    public int reserveAmmo = INITIAL_RESERVE;

    private float cooldownTimer = 0.0f;
    private float reloadTimer = 0.0f;
    private boolean isReloading = false;

    // Recoil state
    private float recoilZ = 0.0f;
    private float recoilPitch = 0.0f;
    private float flashTimer = 0.0f;

    // Train chug / idle sway
    private float swayTime = 0.0f;

    // Meshes
    private final Mesh weaponMesh;
    private final Mesh flashMesh;

    // Reusable matrices for zero-GC rendering
    private final float[] modelMatrix = new float[16];
    private final float[] mvMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] flashModel = new float[16];
    private final float[] flashMVP = new float[16];
    private final float[] colorMod = new float[4];

    public Weapon() {
        weaponMesh = MeshFactory.createWeapon();
        flashMesh = MeshFactory.createMuzzleFlash();
    }

    public void reset() {
        magazineAmmo = MAX_MAGAZINE;
        reserveAmmo = INITIAL_RESERVE;
        cooldownTimer = 0.0f;
        reloadTimer = 0.0f;
        isReloading = false;
        recoilZ = 0.0f;
        recoilPitch = 0.0f;
        flashTimer = 0.0f;
    }

    public void update(float dt, SoundManager soundManager) {
        swayTime += dt;

        if (cooldownTimer > 0) {
            cooldownTimer -= dt;
        }

        if (flashTimer > 0) {
            flashTimer -= dt;
        }

        // Recoil recovery
        recoilZ = Math.max(0.0f, recoilZ - dt * 0.45f);
        recoilPitch = Math.max(0.0f, recoilPitch - dt * 45.0f);

        // Reload logic
        if (isReloading) {
            reloadTimer -= dt;
            if (reloadTimer <= 0.0f) {
                // Complete reload
                int needed = MAX_MAGAZINE - magazineAmmo;
                int transfer = Math.min(needed, reserveAmmo);
                magazineAmmo += transfer;
                reserveAmmo -= transfer;
                isReloading = false;
                soundManager.playReloadComplete();
            }
        }
    }

    public boolean canFire() {
        return cooldownTimer <= 0.0f && !isReloading;
    }

    public boolean isReloading() {
        return isReloading;
    }

    public float getReloadProgress() {
        if (!isReloading) return 1.0f;
        return 1.0f - (reloadTimer / RELOAD_TIME);
    }

    /**
     * Attempts to fire the weapon. Returns true if bullet fired.
     */
    public boolean fire(SoundManager soundManager) {
        if (!canFire()) return false;

        if (magazineAmmo <= 0) {
            soundManager.playEmptyClick();
            // Automatically trigger reload if we have reserve ammo
            if (reserveAmmo > 0) {
                reload(soundManager);
            }
            return false;
        }

        magazineAmmo--;
        cooldownTimer = FIRE_COOLDOWN;
        flashTimer = FLASH_DURATION;

        // Kick recoil
        recoilZ = 0.08f;
        recoilPitch = 7.5f;

        soundManager.playGunshot();
        return true;
    }

    public boolean reload(SoundManager soundManager) {
        if (isReloading || magazineAmmo >= MAX_MAGAZINE || reserveAmmo <= 0) {
            return false;
        }

        isReloading = true;
        reloadTimer = RELOAD_TIME;
        soundManager.playReloadStart();
        return true;
    }

    /**
     * Renders weapon directly in camera / view space so it stays fixed to the first-person perspective.
     */
    public void render(Shader shader, float[] projMatrix, Camera camera) {
        // Base view-space weapon anchor (lower right of screen)
        float baseWeaponX = 0.20f;
        float baseWeaponY = -0.19f;
        float baseWeaponZ = -0.48f;

        // Train bounce / rhythm sway
        float bobY = (float) Math.sin(swayTime * 9.0f) * 0.006f;
        float bobX = (float) Math.cos(swayTime * 4.5f) * 0.004f;

        // Reload animation: drop weapon down and raise back up
        float reloadOffsetY = 0.0f;
        float reloadRotPitch = 0.0f;
        if (isReloading) {
            float progress = 1.0f - (reloadTimer / RELOAD_TIME);
            // Parabolic dip
            float dip = (float) Math.sin(progress * Math.PI);
            reloadOffsetY = -0.22f * dip;
            reloadRotPitch = -25.0f * dip;
        }

        Matrix.setIdentityM(modelMatrix, 0);
        // Position relative to eye
        Matrix.translateM(modelMatrix, 0,
                baseWeaponX + bobX,
                baseWeaponY + bobY + reloadOffsetY,
                baseWeaponZ + recoilZ);

        // Weapon rotations: pitch up from recoil and reload
        Matrix.rotateM(modelMatrix, 0, recoilPitch + reloadRotPitch, 1, 0, 0);
        // Slight inward angle to point towards center crosshair
        Matrix.rotateM(modelMatrix, 0, -4.0f, 0, 1, 0);

        // View space: modelMatrix is already in camera eye-space!
        // So MV = modelMatrix, MVP = Proj * modelMatrix
        System.arraycopy(modelMatrix, 0, mvMatrix, 0, 16);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);

        GLES20.glUniform1i(shader.uUseLightingLoc, 1);
        GLES20.glUniformMatrix4fv(shader.uMVMatrixLoc, 1, false, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0);

        colorMod[0] = 1.0f;
        colorMod[1] = 1.0f;
        colorMod[2] = 1.0f;
        colorMod[3] = 1.0f;
        GLES20.glUniform4fv(shader.uColorModLoc, 1, colorMod, 0);

        // Draw weapon
        weaponMesh.draw(shader);

        // Draw muzzle flash if active
        if (flashTimer > 0.0f) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
            GLES20.glDepthMask(false);

            System.arraycopy(modelMatrix, 0, flashModel, 0, 16);
            // Position at end of barrel
            Matrix.translateM(flashModel, 0, 0.0f, 0.032f, -0.70f);
            // Random spin angle for vibrant flash variation
            Matrix.rotateM(flashModel, 0, (float) (Math.random() * 360.0), 0, 0, 1);

            Matrix.multiplyMM(flashMVP, 0, projMatrix, 0, flashModel, 0);

            GLES20.glUniform1i(shader.uUseLightingLoc, 0); // Unlit flash
            GLES20.glUniformMatrix4fv(shader.uMVMatrixLoc, 1, false, flashModel, 0);
            GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, flashMVP, 0);

            flashMesh.draw(shader);

            GLES20.glDepthMask(true);
            GLES20.glDisable(GLES20.GL_BLEND);
        }
    }
}

