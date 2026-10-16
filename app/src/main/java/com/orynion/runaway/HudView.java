package com.orynion.runaway;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * 2D Heads-Up Display and multi-touch controller rendered on top of GLSurfaceView.
 * Handles aiming gestures, touch buttons (Fire, Reload, Left, Right, Pause), and HUD graphics.
 */
public class HudView extends View {
    private final GameRenderer renderer;
    private final GameState gameState;

    // Paints
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hitMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint();

    // Button Bounding Regions (in pixels)
    private final RectF fireBtnRect = new RectF();
    private final RectF reloadBtnRect = new RectF();
    private final RectF leftBtnRect = new RectF();
    private final RectF rightBtnRect = new RectF();
    private final RectF pauseBtnRect = new RectF();

    // Menu button bounds
    private final RectF menuBtn1Rect = new RectF();
    private final RectF menuBtn2Rect = new RectF();

    // Touch pointer tracking
    private int aimPointerId = -1;
    private float lastAimX = 0.0f;
    private float lastAimY = 0.0f;

    private boolean firePressed = false;
    private boolean reloadPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private float density = 1.0f;

    public HudView(Context context, GameRenderer renderer, GameState gameState) {
        super(context);
        this.renderer = renderer;
        this.gameState = gameState;
        setFocusable(true);

        density = getResources().getDisplayMetrics().density;
        initPaints();
    }

    private void initPaints() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18.0f * density);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setShadowLayer(4.0f, 2.0f, 2.0f, Color.BLACK);

        titlePaint.setColor(Color.parseColor("#FFA726"));
        titlePaint.setTextSize(46.0f * density);
        titlePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        titlePaint.setShadowLayer(8.0f, 3.0f, 3.0f, Color.BLACK);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(Color.argb(170, 40, 40, 45));

        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextSize(16.0f * density);
        buttonTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);

        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(2.5f * density);
        crosshairPaint.setColor(Color.argb(220, 255, 255, 255));

        hitMarkerPaint.setStyle(Paint.Style.STROKE);
        hitMarkerPaint.setStrokeWidth(3.5f * density);
        hitMarkerPaint.setColor(Color.parseColor("#FF3D00"));

        overlayPaint.setStyle(Paint.Style.FILL);
        overlayPaint.setColor(Color.argb(190, 15, 12, 10));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Configure responsive button positions
        float pad = 24.0f * density;

        // Large FIRE button bottom right
        float fireRadius = 52.0f * density;
        float fireCenterX = w - pad - fireRadius;
        float fireCenterY = h - pad - fireRadius;
        fireBtnRect.set(fireCenterX - fireRadius, fireCenterY - fireRadius,
                fireCenterX + fireRadius, fireCenterY + fireRadius);

        // RELOAD button above FIRE
        float reloadRadius = 34.0f * density;
        float reloadCenterX = fireCenterX;
        float reloadCenterY = fireCenterY - fireRadius - reloadRadius - 16.0f * density;
        reloadBtnRect.set(reloadCenterX - reloadRadius, reloadCenterY - reloadRadius,
                reloadCenterX + reloadRadius, reloadCenterY + reloadRadius);

        // LEFT (<) button bottom left
        float btnW = 68.0f * density;
        float btnH = 58.0f * density;
        float leftX = pad;
        float bottomY = h - pad - btnH;
        leftBtnRect.set(leftX, bottomY, leftX + btnW, bottomY + btnH);

        // RIGHT (>) button next to LEFT
        float rightX = leftX + btnW + 14.0f * density;
        rightBtnRect.set(rightX, bottomY, rightX + btnW, bottomY + btnH);

        // PAUSE button top right
        float pauseW = 44.0f * density;
        float pauseH = 38.0f * density;
        pauseBtnRect.set(w - pad - pauseW, pad, w - pad, pad + pauseH);

        // Center Menu Buttons for Title / Pause / GameOver
        float menuW = 220.0f * density;
        float menuH = 50.0f * density;
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        menuBtn1Rect.set(cx - menuW * 0.5f, cy + 20.0f * density, cx + menuW * 0.5f, cy + 20.0f * density + menuH);
        menuBtn2Rect.set(cx - menuW * 0.5f, cy + 85.0f * density, cx + menuW * 0.5f, cy + 85.0f * density + menuH);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (gameState.mode == GameState.MODE_TITLE) {
                    if (menuBtn1Rect.contains(x, y)) {
                        renderer.resetGame();
                    }
                    invalidate();
                    return true;
                } else if (gameState.mode == GameState.MODE_PAUSED) {
                    if (menuBtn1Rect.contains(x, y)) {
                        gameState.resume();
                    } else if (menuBtn2Rect.contains(x, y)) {
                        renderer.resetGame();
                    }
                    invalidate();
                    return true;
                } else if (gameState.mode == GameState.MODE_GAME_OVER) {
                    if (menuBtn1Rect.contains(x, y)) {
                        renderer.resetGame();
                    }
                    invalidate();
                    return true;
                }

                // Playing mode: check touch buttons first
                if (fireBtnRect.contains(x, y)) {
                    firePressed = true;
                    renderer.triggerFire();
                    invalidate();
                    return true;
                } else if (reloadBtnRect.contains(x, y)) {
                    reloadPressed = true;
                    renderer.triggerReload();
                    invalidate();
                    return true;
                } else if (leftBtnRect.contains(x, y)) {
                    leftPressed = true;
                    gameState.moveLeft = true;
                    invalidate();
                    return true;
                } else if (rightBtnRect.contains(x, y)) {
                    rightPressed = true;
                    gameState.moveRight = true;
                    invalidate();
                    return true;
                } else if (pauseBtnRect.contains(x, y)) {
                    gameState.pause();
                    invalidate();
                    return true;
                } else {
                    // Touch in open screen area -> Aiming pointer!
                    if (aimPointerId == -1) {
                        aimPointerId = pointerId;
                        lastAimX = x;
                        lastAimY = y;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // Handle aim dragging
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    if (pId == aimPointerId) {
                        float curX = event.getX(i);
                        float curY = event.getY(i);
                        float dx = curX - lastAimX;
                        float dy = curY - lastAimY;

                        // Rotate camera (sensitivity factor)
                        float sensitivity = 0.16f;
                        renderer.camera.rotate(dx * sensitivity, -dy * sensitivity);

                        lastAimX = curX;
                        lastAimY = curY;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (pointerId == aimPointerId) {
                    aimPointerId = -1;
                }

                // Check button releases
                if (leftPressed && leftBtnRect.contains(x, y)) {
                    leftPressed = false;
                    gameState.moveLeft = false;
                }
                if (rightPressed && rightBtnRect.contains(x, y)) {
                    rightPressed = false;
                    gameState.moveRight = false;
                }
                if (firePressed && fireBtnRect.contains(x, y)) {
                    firePressed = false;
                }
                if (reloadPressed && reloadBtnRect.contains(x, y)) {
                    reloadPressed = false;
                }

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    leftPressed = false;
                    rightPressed = false;
                    firePressed = false;
                    reloadPressed = false;
                    gameState.moveLeft = false;
                    gameState.moveRight = false;
                    aimPointerId = -1;
                }
                break;
            }
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        if (gameState.mode == GameState.MODE_TITLE) {
            drawTitleScreen(canvas, w, h);
        } else if (gameState.mode == GameState.MODE_PAUSED) {
            drawPauseScreen(canvas, w, h);
        } else if (gameState.mode == GameState.MODE_GAME_OVER) {
            drawGameOverScreen(canvas, w, h);
        } else {
            drawGameplayHUD(canvas, w, h);
        }

        // Continuously refresh HUD for animations (crosshair, ammo, score updates)
        invalidate();
    }

    private void drawGameplayHUD(Canvas canvas, int w, int h) {
        float cx = w * 0.5f;
        float cy = h * 0.5f;

        // 1. Center Crosshair
        float chRadius = 14.0f * density;
        canvas.drawCircle(cx, cy, chRadius, crosshairPaint);
        float tickLen = 8.0f * density;
        canvas.drawLine(cx - chRadius - tickLen, cy, cx - chRadius, cy, crosshairPaint);
        canvas.drawLine(cx + chRadius, cy, cx + chRadius + tickLen, cy, crosshairPaint);
        canvas.drawLine(cx, cy - chRadius - tickLen, cx, cy - chRadius, crosshairPaint);
        canvas.drawLine(cx, cy + chRadius, cx, cy + chRadius + tickLen, crosshairPaint);
        canvas.drawPoint(cx, cy, crosshairPaint);

        // 2. Hit Marker (Red X when hit confirmed)
        if (gameState.hitMarkerTimer > 0.0f) {
            float hmSize = 16.0f * density;
            canvas.drawLine(cx - hmSize, cy - hmSize, cx - 6.0f * density, cy - 6.0f * density, hitMarkerPaint);
            canvas.drawLine(cx + hmSize, cy - hmSize, cx + 6.0f * density, cy - 6.0f * density, hitMarkerPaint);
            canvas.drawLine(cx - hmSize, cy + hmSize, cx - 6.0f * density, cy + 6.0f * density, hitMarkerPaint);
            canvas.drawLine(cx + hmSize, cy + hmSize, cx + 6.0f * density, cy + 6.0f * density, hitMarkerPaint);
        }

        // 3. Top Left: Score & Streak
        float pad = 24.0f * density;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.parseColor("#FFD54F"));
        textPaint.setTextSize(20.0f * density);
        canvas.drawText("SCORE: " + gameState.score, pad, pad + 20.0f * density, textPaint);

        textPaint.setColor(Color.LTGRAY);
        textPaint.setTextSize(14.0f * density);
        canvas.drawText("BEST: " + gameState.highScore, pad, pad + 40.0f * density, textPaint);

        if (gameState.comboStreak > 1) {
            textPaint.setColor(Color.parseColor("#FF7043"));
            textPaint.setTextSize(18.0f * density);
            canvas.drawText("x" + gameState.multiplier + " STREAK (" + gameState.comboStreak + ")",
                    pad, pad + 65.0f * density, textPaint);
        }

        // 4. Top Center: Distance & Speed
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(16.0f * density);
        int kmh = (int) (gameState.trainSpeed * 3.6f);
        canvas.drawText("TRAIN SPEED: " + kmh + " KM/H  |  DISTANCE: " + (int) gameState.distanceMeters + " M",
                cx, pad + 18.0f * density, textPaint);

        // 5. Bottom Right: Ammo Status
        if (renderer.weapon != null) {
            textPaint.setTextAlign(Paint.Align.RIGHT);
            float ammoX = fireBtnRect.right;
            float ammoY = reloadBtnRect.top - 12.0f * density;

            if (renderer.weapon.isReloading()) {
                textPaint.setColor(Color.parseColor("#29B6F6"));
                textPaint.setTextSize(16.0f * density);
                canvas.drawText("RELOADING...", ammoX, ammoY, textPaint);

                // Reload Progress Bar
                float barW = 120.0f * density;
                float barH = 6.0f * density;
                buttonPaint.setColor(Color.DKGRAY);
                canvas.drawRect(ammoX - barW, ammoY + 6.0f * density, ammoX, ammoY + 6.0f * density + barH, buttonPaint);
                buttonPaint.setColor(Color.parseColor("#29B6F6"));
                float progress = renderer.weapon.getReloadProgress();
                canvas.drawRect(ammoX - barW, ammoY + 6.0f * density, ammoX - barW + (barW * progress),
                        ammoY + 6.0f * density + barH, buttonPaint);
            } else if (renderer.weapon.magazineAmmo == 0) {
                textPaint.setColor(Color.parseColor("#EF5350"));
                textPaint.setTextSize(18.0f * density);
                canvas.drawText("EMPTY - RELOAD!", ammoX, ammoY, textPaint);
            } else {
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(22.0f * density);
                canvas.drawText("AMMO: " + renderer.weapon.magazineAmmo + " / " + renderer.weapon.reserveAmmo,
                        ammoX, ammoY, textPaint);
            }
        }

        // 6. Draw Control Buttons
        drawButton(canvas, fireBtnRect, firePressed ? Color.parseColor("#E65100") : Color.argb(190, 230, 81, 0), "FIRE", 22.0f);
        drawButton(canvas, reloadBtnRect, reloadPressed ? Color.parseColor("#1565C0") : Color.argb(180, 30, 100, 180), "RELOAD", 14.0f);
        drawButton(canvas, leftBtnRect, leftPressed ? Color.parseColor("#455A64") : Color.argb(170, 55, 71, 79), "< LEFT", 16.0f);
        drawButton(canvas, rightBtnRect, rightPressed ? Color.parseColor("#455A64") : Color.argb(170, 55, 71, 79), "RIGHT >", 16.0f);
        drawButton(canvas, pauseBtnRect, Color.argb(160, 60, 60, 65), "PAUSE", 13.0f);
    }

    private void drawButton(Canvas canvas, RectF rect, int color, String label, float fontSizeDp) {
        buttonPaint.setColor(color);
        canvas.drawRoundRect(rect, 14.0f * density, 14.0f * density, buttonPaint);

        buttonTextPaint.setTextSize(fontSizeDp * density);
        float textY = rect.centerY() - (buttonTextPaint.descent() + buttonTextPaint.ascent()) * 0.5f;
        canvas.drawText(label, rect.centerX(), textY, buttonTextPaint);
    }

    private void drawTitleScreen(Canvas canvas, int w, int h) {
        canvas.drawRect(0, 0, w, h, overlayPaint);
        float cx = w * 0.5f;
        float cy = h * 0.5f;

        titlePaint.setColor(Color.parseColor("#FFA726"));
        titlePaint.setTextSize(52.0f * density);
        canvas.drawText("RUNAWAY", cx, cy - 70.0f * density, titlePaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#FFE082"));
        textPaint.setTextSize(18.0f * density);
        canvas.drawText("FIRST-PERSON FRONTIER TRAIN SHOOTER", cx, cy - 35.0f * density, textPaint);

        textPaint.setColor(Color.LTGRAY);
        textPaint.setTextSize(14.0f * density);
        canvas.drawText("Drag to Aim  |  FIRE to Shoot Hostile Drones  |  LEFT/RIGHT to Move",
                cx, cy - 2.0f * density, textPaint);

        drawButton(canvas, menuBtn1Rect, Color.parseColor("#E65100"), "TAP TO START", 20.0f);
    }

    private void drawPauseScreen(Canvas canvas, int w, int h) {
        canvas.drawRect(0, 0, w, h, overlayPaint);
        float cx = w * 0.5f;
        float cy = h * 0.5f;

        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(38.0f * density);
        canvas.drawText("GAME PAUSED", cx, cy - 40.0f * density, titlePaint);

        drawButton(canvas, menuBtn1Rect, Color.parseColor("#2E7D32"), "RESUME", 18.0f);
        drawButton(canvas, menuBtn2Rect, Color.parseColor("#C62828"), "RESTART", 18.0f);
    }

    private void drawGameOverScreen(Canvas canvas, int w, int h) {
        canvas.drawRect(0, 0, w, h, overlayPaint);
        float cx = w * 0.5f;
        float cy = h * 0.5f;

        titlePaint.setColor(Color.parseColor("#FF5722"));
        titlePaint.setTextSize(42.0f * density);
        canvas.drawText("RUN COMPLETED", cx, cy - 80.0f * density, titlePaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18.0f * density);
        canvas.drawText("FINAL SCORE: " + gameState.score + "  |  HIGH SCORE: " + gameState.highScore,
                cx, cy - 40.0f * density, textPaint);
        canvas.drawText("TARGETS DESTROYED: " + gameState.targetsDestroyed + "  |  ACCURACY: " + gameState.getAccuracyPercent() + "%",
                cx, cy - 15.0f * density, textPaint);
        canvas.drawText("DISTANCE: " + (int) gameState.distanceMeters + " M",
                cx, cy + 10.0f * density, textPaint);

        drawButton(canvas, menuBtn1Rect, Color.parseColor("#E65100"), "PLAY AGAIN", 20.0f);
    }
}

