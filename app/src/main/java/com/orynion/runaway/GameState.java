package com.orynion.runaway;

/**
 * Manages game modes, scoring, accuracy, distance, and player carriage positioning.
 */
public class GameState {
    public static final int MODE_TITLE = 0;
    public static final int MODE_PLAYING = 1;
    public static final int MODE_PAUSED = 2;
    public static final int MODE_GAME_OVER = 3;

    public int mode = MODE_TITLE;

    public int score = 0;
    public int highScore = 0;
    public float distanceMeters = 0.0f;
    public float trainSpeed = 28.0f; // 28 m/s ~ 100 km/h

    public int shotsFired = 0;
    public int shotsHit = 0;
    public int targetsDestroyed = 0;
    public int comboStreak = 0;
    public int multiplier = 1;

    public float hitMarkerTimer = 0.0f;

    // Player position on train platform
    public float playerStrafeX = 0.0f;
    public boolean moveLeft = false;
    public boolean moveRight = false;

    public void startNewGame() {
        score = 0;
        distanceMeters = 0.0f;
        trainSpeed = 28.0f;
        shotsFired = 0;
        shotsHit = 0;
        targetsDestroyed = 0;
        comboStreak = 0;
        multiplier = 1;
        playerStrafeX = 0.0f;
        moveLeft = false;
        moveRight = false;
        hitMarkerTimer = 0.0f;
        mode = MODE_PLAYING;
    }

    public void pause() {
        if (mode == MODE_PLAYING) {
            mode = MODE_PAUSED;
        }
    }

    public void resume() {
        if (mode == MODE_PAUSED) {
            mode = MODE_PLAYING;
        }
    }

    public void gameOver() {
        if (score > highScore) {
            highScore = score;
        }
        mode = MODE_GAME_OVER;
    }

    public void update(float dt) {
        if (mode != MODE_PLAYING) return;

        // Advance distance
        distanceMeters += trainSpeed * dt;

        // Player strafe movement on carriage platform
        float moveSpeed = 3.2f;
        if (moveLeft) {
            playerStrafeX -= moveSpeed * dt;
        }
        if (moveRight) {
            playerStrafeX += moveSpeed * dt;
        }

        // Clamp to carriage deck width
        if (playerStrafeX < -1.35f) playerStrafeX = -1.35f;
        if (playerStrafeX > 1.35f) playerStrafeX = 1.35f;

        // Hit marker countdown
        if (hitMarkerTimer > 0) {
            hitMarkerTimer -= dt;
        }

        // Increase train speed and difficulty gradually over distance
        if (trainSpeed < 45.0f) {
            trainSpeed += dt * 0.15f;
        }
    }

    public void registerShot(boolean hit, boolean killed) {
        shotsFired++;
        if (hit) {
            shotsHit++;
            comboStreak++;
            multiplier = Math.min(5, 1 + (comboStreak / 3));

            int points = 100 * multiplier;
            if (killed) {
                targetsDestroyed++;
                points += 150 * multiplier;
            }
            score += points;
            hitMarkerTimer = 0.22f;

            if (score > highScore) {
                highScore = score;
            }
        } else {
            // Miss breaks combo streak
            comboStreak = 0;
            multiplier = 1;
        }
    }

    public int getAccuracyPercent() {
        if (shotsFired <= 0) return 0;
        return (shotsHit * 100) / shotsFired;
    }
}

