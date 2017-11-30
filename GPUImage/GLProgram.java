package GPUImage;

import android.opengl.GLES20;

import java.util.Vector;

/**
 * Created by j on 8/9/2017.
 */

public class GLProgram {
    protected int program;
    protected int vertShader;
    protected int fragShader;

    protected boolean initialized;
    protected String vertexShaderLog;
    protected String fragmentShaderLog;
    protected String programLog;

    protected Vector<String> uniforms = new Vector<>();

    public GLProgram(String vShaderString, String fShaderString) {
        initialized = false;
        program = GLES20.glCreateProgram();

        if ((vertShader = compileShader(GLES20.GL_VERTEX_SHADER, vShaderString)) == 0) {
            GLog.e("Failed to compile vertex shader");
        }
        if ((fragShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fShaderString)) == 0) {
            GLog.e("Failed to compile fragment shader");
        }

        GLES20.glAttachShader(program, vertShader);
        GLES20.glAttachShader(program, fragShader);
    }

    private int compileShader(int type, String shaderString) {

        int[] status = new int[1];
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderString);
        GLES20.glCompileShader(shader);

        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String log = GLES20.glGetShaderInfoLog(shader);
            if (type == GLES20.GL_VERTEX_SHADER) {
                vertexShaderLog = log;
            } else if (type == GLES20.GL_FRAGMENT_SHADER) {
                fragmentShaderLog = log;
            }

            GLog.e("Load Shader Failed: " + "Compilation\n" + log);
            return 0;
        }
        return shader;
    }

    public int attributeIndex(String attributeName) {
        return GLES20.glGetAttribLocation(program, attributeName);
    }

    public int uniformIndex(String uniformName) {
        return GLES20.glGetUniformLocation(program, uniformName);
    }

    public boolean link() {
        int[] status = new int[1];

        GLES20.glLinkProgram(program);

        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES20.GL_FALSE) {
            return false;
        }

        if (vertShader > 0) {
            GLES20.glDeleteShader(vertShader);
            vertShader = 0;
        }

        if (fragShader > 0) {
            GLES20.glDeleteShader(fragShader);
            fragShader = 0;
        }

        initialized = true;

        return true;
    }

    public void use() {
        GLES20.glUseProgram(program);
    }

    public void validate() {
        int logLength[] = new int[1];

        GLES20.glValidateProgram(program);
        GLES20.glGetProgramiv(program, GLES20.GL_INFO_LOG_LENGTH, logLength, 0);
        if (logLength[0] > 0) {
            programLog = GLES20.glGetProgramInfoLog(program);
        }
    }

    public void destroy() {
        if (vertShader > 0) {
            GLES20.glDeleteShader(vertShader);
            vertShader = 0;
        }
        if (fragShader > 0) {
            GLES20.glDeleteShader(fragShader);
            fragShader = 0;
        }
        if (program > 0) {
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        initialized = false;
    }
}
