package com.orynion.runaway;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.util.Random;

/**
 * Manages the moving 3D desert environment, procedural scenery recycling,
 * railway tracks, train carriage, targets, and particle effects.
 */
public class World {
    private static final int NUM_TRACK_SEGMENTS = 12;
    private static final float TRACK_SEGMENT_LENGTH = 30.0f; // Total track length = 360m

    private static final int NUM_POLES = 16;
    private static final float POLE_SPACING = 24.0f;

    private static final int NUM_SCENERY = 24;
    private static final int NUM_MESAS = 8;
    private static final int MAX_TARGETS = 16;

    // Static Scenery Item
    private static class SceneryItem {
        int type; // 0 = rock, 1 = cactus
        float x, y, z;
        float scale;
        float rotY;
    }

    private static class MesaItem {
        float x, y, z;
        float w, h, d;
    }

    // World state
    public float trainZ = 0.0f;
    private float chugTimer = 0.0f;
    private float targetSpawnTimer = 0.0f;

    // Meshes
    private final Mesh groundMesh;
    private final Mesh trackMesh;
    private final Mesh carriageMesh;
    private final Mesh poleMesh;
    private final Mesh mesaMesh;
    private final Mesh rockMesh;
    private final Mesh cactusMesh;
    private final Mesh droneMesh;
    private final Mesh banditMesh;

    // Scenery instances
    private final float[] poleZ = new float[NUM_POLES];
    private final SceneryItem[] sceneryItems = new SceneryItem[NUM_SCENERY];
    private final MesaItem[] mesas = new MesaItem[NUM_MESAS];

    // Targets and particles
    public final Target[] targets = new Target[MAX_TARGETS];
    public final ParticleSystem particles = new ParticleSystem();

    private final Random rand = new Random(1337); // Deterministic seed

    // Reusable matrices for zero-GC rendering
    private final float[] modelMatrix = new float[16];
    private final float[] mvMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] colorMod = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

    public World() {
        groundMesh = MeshFactory.createGroundTile(400.0f, TRACK_SEGMENT_LENGTH);
        trackMesh = MeshFactory.createTrackSegment(TRACK_SEGMENT_LENGTH);
        carriageMesh = MeshFactory.createTrainCarriage();
        poleMesh = MeshFactory.createTelegraphPole();
        mesaMesh = MeshFactory.createMesa(50.0f, 35.0f, 60.0f);
        rockMesh = MeshFactory.createRock(2.5f);
        cactusMesh = MeshFactory.createCactus();
        droneMesh = MeshFactory.createTargetDrone();
        banditMesh = MeshFactory.createTargetBandit();

        // Initialize targets
        for (int i = 0; i < MAX_TARGETS; i++) {
            targets[i] = new Target();
        }

        // Initialize telegraph poles along right side of tracks
        for (int i = 0; i < NUM_POLES; i++) {
            poleZ[i] = i * POLE_SPACING - 30.0f;
        }

        // Initialize rocks and cacti along flanks
        for (int i = 0; i < NUM_SCENERY; i++) {
            sceneryItems[i] = new SceneryItem();
            sceneryItems[i].type = (i % 2 == 0) ? 0 : 1;
            sceneryItems[i].scale = 0.8f + rand.nextFloat() * 0.9f;
            sceneryItems[i].rotY = rand.nextFloat() * 360.0f;

            // Distribute on left and right sides
            boolean leftSide = (i % 4 < 2);
            float minX = leftSide ? -45.0f : 8.0f;
            float maxX = leftSide ? -8.0f : 45.0f;
            sceneryItems[i].x = minX + rand.nextFloat() * (maxX - minX);
            sceneryItems[i].y = 0.0f;
            sceneryItems[i].z = (i * 15.0f) - 30.0f;
        }

        // Initialize distant desert mesas
        for (int i = 0; i < NUM_MESAS; i++) {
            mesas[i] = new MesaItem();
            boolean left = (i % 2 == 0);
            mesas[i].x = left ? (-90.0f - rand.nextFloat() * 60.0f) : (90.0f + rand.nextFloat() * 60.0f);
            mesas[i].y = 0.0f;
            mesas[i].z = i * 65.0f;
            mesas[i].w = 55.0f + rand.nextFloat() * 35.0f;
            mesas[i].h = 30.0f + rand.nextFloat() * 25.0f;
            mesas[i].d = 65.0f + rand.nextFloat() * 40.0f;
        }
    }

    public void reset() {
        trainZ = 0.0f;
        chugTimer = 0.0f;
        targetSpawnTimer = 0.0f;
        for (Target t : targets) {
            t.state = Target.STATE_INACTIVE;
        }
        for (int i = 0; i < NUM_POLES; i++) {
            poleZ[i] = i * POLE_SPACING - 30.0f;
        }
        for (int i = 0; i < NUM_SCENERY; i++) {
            sceneryItems[i].z = (i * 15.0f) - 30.0f;
        }
        for (int i = 0; i < NUM_MESAS; i++) {
            mesas[i].z = i * 65.0f;
        }
    }

    public void update(float dt, GameState gameState, SoundManager soundManager) {
        if (gameState.mode != GameState.MODE_PLAYING) return;

        // Advance train along Z axis
        trainZ += gameState.trainSpeed * dt;

        // Periodic train chug audio & wheel dust
        chugTimer += dt;
        if (chugTimer >= 0.42f) {
            chugTimer = 0.0f;
            soundManager.playTrainChug();

            // Spawn wheel dust particles behind carriage
            particles.spawn(
                    gameState.playerStrafeX + (rand.nextFloat() - 0.5f) * 2.0f,
                    0.25f,
                    trainZ - 4.5f,
                    (rand.nextFloat() - 0.5f) * 1.5f,
                    rand.nextFloat() * 0.8f + 0.3f,
                    -gameState.trainSpeed * 0.4f,
                    0.82f, 0.70f, 0.45f, 0.55f, 0.40f, 0.8f);
        }

        // Recycle scenery items as the train passes them
        recycleScenery();

        // Update targets
        for (Target t : targets) {
            t.update(dt, trainZ, particles, soundManager);
        }

        // Spawn new targets ahead
        targetSpawnTimer += dt;
        float spawnInterval = Math.max(1.2f, 2.8f - (gameState.distanceMeters / 1500.0f));
        if (targetSpawnTimer >= spawnInterval) {
            targetSpawnTimer = 0.0f;
            spawnTargetAhead(gameState);
        }

        // Update particles
        particles.update(dt);
    }

    private void recycleScenery() {
        // Recycle telegraph poles
        for (int i = 0; i < NUM_POLES; i++) {
            if (poleZ[i] < trainZ - 40.0f) {
                poleZ[i] += NUM_POLES * POLE_SPACING;
            }
        }

        // Recycle rocks & cacti
        for (int i = 0; i < NUM_SCENERY; i++) {
            if (sceneryItems[i].z < trainZ - 45.0f) {
                sceneryItems[i].z += NUM_SCENERY * 15.0f;
                // Re-randomize slightly
                sceneryItems[i].rotY = rand.nextFloat() * 360.0f;
                boolean leftSide = (i % 4 < 2);
                float minX = leftSide ? -45.0f : 8.0f;
                float maxX = leftSide ? -8.0f : 45.0f;
                sceneryItems[i].x = minX + rand.nextFloat() * (maxX - minX);
            }
        }

        // Recycle mesas
        for (int i = 0; i < NUM_MESAS; i++) {
            if (mesas[i].z < trainZ - 90.0f) {
                mesas[i].z += NUM_MESAS * 65.0f;
            }
        }
    }

    private void spawnTargetAhead(GameState gameState) {
        // Find inactive target slot
        for (Target t : targets) {
            if (t.state == Target.STATE_INACTIVE) {
                // Determine target type (more bandits as distance increases)
                int type = (rand.nextFloat() < 0.55f) ? Target.TYPE_DRONE : Target.TYPE_BANDIT;

                // Position ahead of train
                float spawnZ = trainZ + 85.0f + rand.nextFloat() * 45.0f;

                // Flank (left or right side of track)
                boolean leftSide = rand.nextBoolean();
                float spawnX = leftSide ? (-6.5f - rand.nextFloat() * 9.5f) : (6.5f + rand.nextFloat() * 9.5f);
                float spawnY = (type == Target.TYPE_DRONE) ? (2.2f + rand.nextFloat() * 2.8f) : 0.0f;

                // Target moves slightly slower than train so train overtakes it
                float targetSpeed = gameState.trainSpeed * (0.65f + rand.nextFloat() * 0.25f);

                t.spawn(type, spawnX, spawnY, spawnZ, targetSpeed);
                break;
            }
        }
    }

    /**
     * Executes bullet raycast against all active targets.
     */
    public boolean performShot(float rOx, float rOy, float rOz,
                               float rDx, float rDy, float rDz,
                               GameState gameState, SoundManager soundManager) {
        float closestDist = Float.MAX_VALUE;
        Target hitTarget = null;

        for (Target t : targets) {
            if (t.state == Target.STATE_ACTIVE) {
                float dist = t.testRayHit(rOx, rOy, rOz, rDx, rDy, rDz);
                if (dist >= 0.0f && dist < closestDist) {
                    closestDist = dist;
                    hitTarget = t;
                }
            }
        }

        // Spawn bullet trail / muzzle smoke
        particles.spawnMuzzleSmoke(rOx + rDx * 0.6f, rOy + rDy * 0.6f, rOz + rDz * 0.6f, rDx, rDy, rDz);

        if (hitTarget != null) {
            boolean killed = hitTarget.takeDamage(1, particles, soundManager);
            gameState.registerShot(true, killed);
            return true;
        } else {
            gameState.registerShot(false, false);
            return false;
        }
    }

    public void render(Shader shader, float[] viewMatrix, float[] projMatrix,
                       Camera camera, GameState gameState) {
        // Set standard lighting
        GLES20.glUniform1i(shader.uUseLightingLoc, 1);
        GLES20.glUniform4fv(shader.uColorModLoc, 1, colorMod, 0);

        // 1. Render Ground Terrain Tiles & Track Segments
        // Find base segment index closest to trainZ
        int centerSeg = (int) Math.floor(trainZ / TRACK_SEGMENT_LENGTH);
        int halfSegs = NUM_TRACK_SEGMENTS / 2;

        for (int i = centerSeg - 3; i <= centerSeg + halfSegs + 4; i++) {
            float segZ = i * TRACK_SEGMENT_LENGTH;

            // Draw Ground Plane
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, 0, 0, segZ);
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);
            GLES20.glUniformMatrix4fv(shader.uMVMatrixLoc, 1, false, mvMatrix, 0);
            GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0);
            groundMesh.draw(shader);

            // Draw Track Segment
            trackMesh.draw(shader);
        }

        // 2. Render Distant Meshes
        for (MesaItem m : mesas) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, m.x, m.y, m.z);
            Matrix.scaleM(modelMatrix, 0, m.w / 50.0f, m.h / 35.0f, m.d / 60.0f);
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);
            GLES20.glUniformMatrix4fv(shader.uMVMatrixLoc, 1, false, mvMatrix, 0);
            GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0);
            mesaMesh.draw(shader);
        }

        // 3. Render Telegraph Poles along tracks (x = 4.4m on right)
        for (int i = 0; i < NUM_POLES; i++) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, 4.4f, 0, poleZ[i]);
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);
            GLES20.glUniformMatrix4fv(shader.uMVMatrixLoc, 1, false, mvMatrix, 0);
            GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0);
            poleMesh.draw(shader);
        }

        // 4. Render Rocks and Cacti
        for (SceneryItem s : sceneryItems) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, s.x, s.y, s.z);
            Matrix.rotateM(modelMatrix, 0, s.rotY, 0, 1, 0);
            Matrix.scaleM(modelMatrix, 0, s.scale, s.scale, s.scale);
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);
            GLES20.glUniformMatrix4fv(shader.uMVMatrixLoc, 1, false, mvMatrix, 0);
            GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0);
            if (s.type == 0) {
                rockMesh.draw(shader);
            } else {
                cactusMesh.draw(shader);
            }
        }

        // 5. Render Train Carriage Platform (player's train)
        Matrix.setIdentityM(modelMatrix, 0);
        // Suspension vertical bounce
        float carriageBounce = (float) Math.sin(trainZ * 2.2f) * 0.015f;
        Matrix.translateM(modelMatrix, 0, 0, carriageBounce, trainZ);
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(shader.uMVMatrixLoc, 1, false, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0);
        carriageMesh.draw(shader);

        // 6. Render Active Targets
        for (Target t : targets) {
            t.render(shader, viewMatrix, projMatrix, droneMesh, banditMesh);
        }

        // 7. Render Particles
        particles.render(shader, viewMatrix, projMatrix, camera);
    }
}

