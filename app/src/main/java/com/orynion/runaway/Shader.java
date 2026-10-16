package com.orynion.runaway;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Handles GLSL shader compilation, program linking, and uniform/attribute location caching.
 */
public class Shader {
    private static final String TAG = "Shader";

    public final int programId;

    // Uniform locations
    public final int uMVPMatrixLoc;
    public final int uMVMatrixLoc;
    public final int uLightPosLoc;
    public final int uLightColorLoc;
    public final int uAmbientColorLoc;
    public final int uFogColorLoc;
    public final int uFogStartLoc;
    public final int uFogEndLoc;
    public final int uUseLightingLoc;
    public final int uColorModLoc;

    // Attribute locations
    public final int aPositionLoc;
    public final int aNormalLoc;
    public final int aColorLoc;

    public static final String VERTEX_SHADER_CODE =
            "uniform mat4 u_MVPMatrix;\n" +
            "uniform mat4 u_MVMatrix;\n" +
            "uniform vec3 u_LightPos;\n" +
            "uniform vec3 u_LightColor;\n" +
            "uniform vec3 u_AmbientColor;\n" +
            "uniform float u_FogStart;\n" +
            "uniform float u_FogEnd;\n" +
            "uniform int u_UseLighting;\n" +
            "uniform vec4 u_ColorMod;\n" +
            "\n" +
            "attribute vec3 a_Position;\n" +
            "attribute vec3 a_Normal;\n" +
            "attribute vec4 a_Color;\n" +
            "\n" +
            "varying vec4 v_Color;\n" +
            "varying float v_FogFactor;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 posEye = u_MVMatrix * vec4(a_Position, 1.0);\n" +
            "    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);\n" +
            "\n" +
            "    vec4 baseColor = a_Color * u_ColorMod;\n" +
            "    if (u_UseLighting == 1) {\n" +
            "        vec3 normEye = normalize(mat3(u_MVMatrix) * a_Normal);\n" +
            "        vec3 lightDir = normalize(u_LightPos - posEye.xyz);\n" +
            "        float diff = max(dot(normEye, lightDir), 0.0);\n" +
            "        vec3 lighting = u_AmbientColor + (u_LightColor * diff);\n" +
            "        v_Color = vec4(baseColor.rgb * lighting, baseColor.a);\n" +
            "    } else {\n" +
            "        v_Color = baseColor;\n" +
            "    }\n" +
            "\n" +
            "    float dist = length(posEye.xyz);\n" +
            "    v_FogFactor = clamp((u_FogEnd - dist) / (u_FogEnd - u_FogStart), 0.0, 1.0);\n" +
            "}\n";

    public static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;\n" +
            "uniform vec4 u_FogColor;\n" +
            "varying vec4 v_Color;\n" +
            "varying float v_FogFactor;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 col = mix(u_FogColor, v_Color, v_FogFactor);\n" +
            "    col.a = v_Color.a;\n" +
            "    gl_FragColor = col;\n" +
            "}\n";

    public Shader() {
        int vShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vShader);
        GLES20.glAttachShader(programId, fShader);
        GLES20.glLinkProgram(programId);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            String log = GLES20.glGetProgramInfoLog(programId);
            GLES20.glDeleteProgram(programId);
            throw new RuntimeException("Shader link failed: " + log);
        }

        uMVPMatrixLoc = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");
        uMVMatrixLoc = GLES20.glGetUniformLocation(programId, "u_MVMatrix");
        uLightPosLoc = GLES20.glGetUniformLocation(programId, "u_LightPos");
        uLightColorLoc = GLES20.glGetUniformLocation(programId, "u_LightColor");
        uAmbientColorLoc = GLES20.glGetUniformLocation(programId, "u_AmbientColor");
        uFogColorLoc = GLES20.glGetUniformLocation(programId, "u_FogColor");
        uFogStartLoc = GLES20.glGetUniformLocation(programId, "u_FogStart");
        uFogEndLoc = GLES20.glGetUniformLocation(programId, "u_FogEnd");
        uUseLightingLoc = GLES20.glGetUniformLocation(programId, "u_UseLighting");
        uColorModLoc = GLES20.glGetUniformLocation(programId, "u_ColorMod");

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aNormalLoc = GLES20.glGetAttribLocation(programId, "a_Normal");
        aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");
    }

    public void use() {
        GLES20.glUseProgram(programId);
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] != GLES20.GL_TRUE) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed for " + type + ": " + log);
        }
        return shader;
    }
}

