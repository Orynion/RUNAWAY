package com.orynion.runaway;

import android.opengl.Matrix;

/**
 * First-person 3D camera with pitch/yaw control and ray generation.
 */
public class Camera {
    public float posX = 0.0f;
    public float posY = 2.1f;
    public float posZ = 0.0f;

    public float yaw = 0.0f;   // Degrees: 0 is forward (+Z), +90 is right (+X), -90 is left (-X)
    public float pitch = 0.0f; // Degrees: -45 looking down, +45 looking up

    // Cached look vectors
    public float forwardX = 0.0f;
    public float forwardY = 0.0f;
    public float forwardZ = 1.0f;

    public float rightX = 1.0f;
    public float rightY = 0.0f;
    public float rightZ = 0.0f;

    private final float[] viewMatrix = new float[16];
    private final float[] invertedVPMatrix = new float[16];
    private final float[] vpMatrix = new float[16];

    public Camera() {
        updateVectors();
    }

    public void rotate(float deltaYaw, float deltaPitch) {
        yaw += deltaYaw;
        pitch += deltaPitch;

        // Clamp pitch to avoid gimbal lock / flipping
        if (pitch > 48.0f) pitch = 48.0f;
        if (pitch < -48.0f) pitch = -48.0f;

        // Clamp yaw so player stays focused forward and towards the side horizons
        if (yaw > 95.0f) yaw = 95.0f;
        if (yaw < -95.0f) yaw = -95.0f;

        updateVectors();
    }

    public void setPosition(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public void updateVectors() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);

        // Forward (+Z when yaw=0, pitch=0)
        forwardX = sinYaw * cosPitch;
        forwardY = sinPitch;
        forwardZ = cosYaw * cosPitch;

        // Right (+X when yaw=0)
        rightX = cosYaw;
        rightY = 0.0f;
        rightZ = -sinYaw;
    }

    public float[] getViewMatrix() {
        Matrix.setLookAtM(viewMatrix, 0,
                posX, posY, posZ,
                posX + forwardX, posY + forwardY, posZ + forwardZ,
                0.0f, 1.0f, 0.0f);
        return viewMatrix;
    }

    /**
     * Get center reticle ray in world coordinates.
     */
    public void getCenterRay(float[] outOrigin, float[] outDir) {
        outOrigin[0] = posX;
        outOrigin[1] = posY;
        outOrigin[2] = posZ;

        outDir[0] = forwardX;
        outDir[1] = forwardY;
        outDir[2] = forwardZ;
    }

    /**
     * Unproject screen touch coordinate (x, y) into a 3D world ray.
     */
    public void getRayFromScreen(float screenX, float screenY, int viewWidth, int viewHeight,
                                 float[] projMatrix, float[] outOrigin, float[] outDir) {
        // Compute combined View-Projection matrix
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, getViewMatrix(), 0);

        // Invert VP Matrix
        if (!Matrix.invertM(invertedVPMatrix, 0, vpMatrix, 0)) {
            // Fallback to center ray if inversion fails
            getCenterRay(outOrigin, outDir);
            return;
        }

        // Convert screen pixel to Normalized Device Coordinates (NDC: [-1, 1])
        float ndcX = (2.0f * screenX / viewWidth) - 1.0f;
        float ndcY = 1.0f - (2.0f * screenY / viewHeight); // Invert Y

        // Near plane point (z = -1) and Far plane point (z = 1) in NDC
        float[] nearPoint = new float[]{ndcX, ndcY, -1.0f, 1.0f};
        float[] farPoint = new float[]{ndcX, ndcY, 1.0f, 1.0f};

        float[] worldNear = new float[4];
        float[] worldFar = new float[4];

        Matrix.multiplyMV(worldNear, 0, invertedVPMatrix, 0, nearPoint, 0);
        Matrix.multiplyMV(worldFar, 0, invertedVPMatrix, 0, farPoint, 0);

        if (worldNear[3] != 0.0f) {
            worldNear[0] /= worldNear[3];
            worldNear[1] /= worldNear[3];
            worldNear[2] /= worldNear[3];
        }
        if (worldFar[3] != 0.0f) {
            worldFar[0] /= worldFar[3];
            worldFar[1] /= worldFar[3];
            worldFar[2] /= worldFar[3];
        }

        outOrigin[0] = worldNear[0];
        outOrigin[1] = worldNear[1];
        outOrigin[2] = worldNear[2];

        float dx = worldFar[0] - worldNear[0];
        float dy = worldFar[1] - worldNear[1];
        float dz = worldFar[2] - worldNear[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0.0001f) {
            outDir[0] = dx / len;
            outDir[1] = dy / len;
            outDir[2] = dz / len;
        } else {
            outDir[0] = forwardX;
            outDir[1] = forwardY;
            outDir[2] = forwardZ;
        }
    }
}

