package com.orynion.runaway;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Encapsulates vertex data (position, normal, color) and index buffer for drawing.
 */
public class Mesh {
    public static final int POSITION_SIZE = 3;
    public static final int NORMAL_SIZE = 3;
    public static final int COLOR_SIZE = 4;
    public static final int STRIDE_FLOATS = POSITION_SIZE + NORMAL_SIZE + COLOR_SIZE; // 10
    public static final int STRIDE_BYTES = STRIDE_FLOATS * 4; // 40 bytes

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    public Mesh(float[] vertices, short[] indices) {
        this.indexCount = indices.length;

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        this.vertexBuffer = vbb.asFloatBuffer();
        this.vertexBuffer.put(vertices);
        this.vertexBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        this.indexBuffer = ibb.asShortBuffer();
        this.indexBuffer.put(indices);
        this.indexBuffer.position(0);
    }

    public void draw(Shader shader) {
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(shader.aPositionLoc);
        GLES20.glVertexAttribPointer(shader.aPositionLoc, POSITION_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer);

        vertexBuffer.position(POSITION_SIZE);
        GLES20.glEnableVertexAttribArray(shader.aNormalLoc);
        GLES20.glVertexAttribPointer(shader.aNormalLoc, NORMAL_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer);

        vertexBuffer.position(POSITION_SIZE + NORMAL_SIZE);
        GLES20.glEnableVertexAttribArray(shader.aColorLoc);
        GLES20.glVertexAttribPointer(shader.aColorLoc, COLOR_SIZE, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer);

        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(shader.aPositionLoc);
        GLES20.glDisableVertexAttribArray(shader.aNormalLoc);
        GLES20.glDisableVertexAttribArray(shader.aColorLoc);
    }
}

