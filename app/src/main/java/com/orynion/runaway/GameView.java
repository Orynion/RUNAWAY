package com.orynion.runaway;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.KeyEvent;

/**
 * GLSurfaceView hosting the OpenGL ES 2.0 renderer and physical keyboard inputs.
 */
public class GameView extends GLSurfaceView {
    public final GameRenderer renderer;
    private final GameState gameState;

    public GameView(Context context, GameState gameState, SoundManager soundManager) {
        super(context);
        this.gameState = gameState;

        // Require OpenGL ES 2.0
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);

        renderer = new GameRenderer(gameState, soundManager);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (gameState.mode == GameState.MODE_PLAYING) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    gameState.moveLeft = true;
                    return true;
                case KeyEvent.KEYCODE_D:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    gameState.moveRight = true;
                    return true;
                case KeyEvent.KEYCODE_SPACE:
                    renderer.triggerFire();
                    return true;
                case KeyEvent.KEYCODE_R:
                    renderer.triggerReload();
                    return true;
                case KeyEvent.KEYCODE_ESCAPE:
                    gameState.pause();
                    return true;
            }
        } else if (gameState.mode == GameState.MODE_PAUSED) {
            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                gameState.resume();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                gameState.moveLeft = false;
                return true;
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                gameState.moveRight = false;
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}

