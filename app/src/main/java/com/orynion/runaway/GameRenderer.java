package com.orynion.runaway;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 2.0 Renderer orchestrating 3D scene rendering, lighting, fog, and updates.
 */
public class GameRenderer implements GLSurfaceView.Renderer {
    public final Camera camera = new Camera();
    public final GameState gameState;
    public final SoundManager soundManager;

    public World world;
    public Weapon weapon;
    private Shader shader;

    public int viewWidth = 1;
    public int viewHeight = 1;

    public final float[] projMatrix = new float[16];
    private long lastTimeNs = 0;

    // Raycast buffers
    private final float[] rayOrigin = new float[3];
    private final float[] rayDir = new float[3];

    // Lighting and fog constants
    private final float[] lightPos = new float[]{0.0f, 80.0f, 50.0f};
    private final float[] lightColor = new float[]{1.0f, 0.95f, 0.85f};
    private final float[] ambientColor = new float[]{0.48f, 0.44f, 0.40f};
    private final float[] fogColor = new float[]{0.88f, 0.72f, 0.52f, 1.0f};

    public GameRenderer(GameState gameState, SoundManager soundManager) {
        this.gameState = gameState;
        this.soundManager = soundManager;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set OpenGL flags
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Sky / fog desert horizon clear color
        GLES20.glClearColor(fogColor[0], fogColor[1], fogColor[2], 1.0f);

        // Build Shaders and procedural assets
        shader = new Shader();
        world = new World();
        weapon = new Weapon();

        lastTimeNs = System.nanoTime();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;

        GLES20.glViewport(0, 0, width, height);

        float aspect = (float) width / (height > 0 ? height : 1);
        Matrix.perspectiveM(projMatrix, 0, 60.0f, aspect, 0.2f, 350.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long nowNs = System.nanoTime();
        if (lastTimeNs == 0) lastTimeNs = nowNs;
        float dt = (nowNs - lastTimeNs) / 1_000_000_000.0f;
        lastTimeNs = nowNs;

        // Clamp delta time to avoid jumps after pause or resume
        if (dt < 0.001f) dt = 0.001f;
        if (dt > 0.050f) dt = 0.050f;

        // Update Game State, World, and Weapon
        gameState.update(dt);
        if (world != null) {
            world.update(dt, gameState, soundManager);
        }
        if (weapon != null) {
            weapon.update(dt, soundManager);
        }

        // Synchronize Camera with Train and Player strafe position
        if (world != null) {
            float bounce = (float) Math.sin(world.trainZ * 2.2f) * 0.015f;
            camera.setPosition(gameState.playerStrafeX, 2.15f + bounce, world.trainZ);
        }

        // Clear Color and Depth
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (shader == null || world == null || weapon == null) return;

        shader.use();

        // Pass Lighting & Fog Uniforms
        GLES20.glUniform3fv(shader.uLightPosLoc, 1, lightPos, 0);
        GLES20.glUniform3fv(shader.uLightColorLoc, 1, lightColor, 0);
        GLES20.glUniform3fv(shader.uAmbientColorLoc, 1, ambientColor, 0);
        GLES20.glUniform4fv(shader.uFogColorLoc, 1, fogColor, 0);
        GLES20.glUniform1f(shader.uFogStartLoc, 35.0f);
        GLES20.glUniform1f(shader.uFogEndLoc, 270.0f);

        float[] viewMatrix = camera.getViewMatrix();

        // 1. Render 3D World (terrain, tracks, poles, rocks, train, targets, particles)
        world.render(shader, viewMatrix, projMatrix, camera, gameState);

        // 2. Render First-Person Weapon attached to camera
        weapon.render(shader, projMatrix, camera);
    }

    /**
     * Executes firing the weapon through center crosshair reticle.
     */
    public void triggerFire() {
        if (weapon == null || world == null || gameState.mode != GameState.MODE_PLAYING) {
            return;
        }

        if (weapon.fire(soundManager)) {
            // Get center ray from camera
            camera.getCenterRay(rayOrigin, rayDir);
            world.performShot(rayOrigin[0], rayOrigin[1], rayOrigin[2],
                    rayDir[0], rayDir[1], rayDir[2], gameState, soundManager);
        }
    }

    /**
     * Executes reloading the weapon.
     */
    public void triggerReload() {
        if (weapon != null && gameState.mode == GameState.MODE_PLAYING) {
            weapon.reload(soundManager);
        }
    }

    public void resetGame() {
        if (world != null) world.reset();
        if (weapon != null) weapon.reset();
        gameState.startNewGame();
    }
}

