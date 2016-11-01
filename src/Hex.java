import java.nio.IntBuffer;

import org.lwjgl.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Hex {

    private static final double SQRT_3_4 = Math.sqrt(3) / 2;

    private static final String vertexShaderSource =
        "#version 330 core\n"
      + "layout (location = 0) in vec3 position;\n"
      + "uniform float rotationOffset;\n"
      + "void main() {\n"
      + "    gl_Position = vec4("
      + "        position.x * cos(rotationOffset) + position.y * sin(rotationOffset),"
      + "        position.y * cos(rotationOffset) - position.x * sin(rotationOffset),"
      + "        position.z, 1.0);\n"
      + "}";
    private int rotationOffset;

    private static final String fragmentShaderSource =
        "#version 330 core\n"
      + "out vec4 color;\n"
      + "void main() {\n"
      + "    color = vec4(0x58 / 255.0, 0x58 / 255.0, 0x58 / 255.0, 1.0f);\n"
      + "}";

    private static final double vertices[] = {
          -1 * Jexx.HEX_SIZE,         0 * Jexx.HEX_SIZE, 0,
        -0.5 * Jexx.HEX_SIZE,  SQRT_3_4 * Jexx.HEX_SIZE, 0,
         0.5 * Jexx.HEX_SIZE,  SQRT_3_4 * Jexx.HEX_SIZE, 0,
           1 * Jexx.HEX_SIZE,         0 * Jexx.HEX_SIZE, 0,
         0.5 * Jexx.HEX_SIZE, -SQRT_3_4 * Jexx.HEX_SIZE, 0,
        -0.5 * Jexx.HEX_SIZE, -SQRT_3_4 * Jexx.HEX_SIZE, 0
    };

    private static final int indices[] = {
        0, 1, 2,
        0, 2, 3,
        0, 3, 4,
        0, 4, 5
    };

    private int shaderProgram, vao;

    public void compileShader() {
        IntBuffer success = BufferUtils.createIntBuffer(1);

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        glGetShaderiv(vertexShader, GL_COMPILE_STATUS, success);
        if (success.get(0) == 0) {
            System.err.println(glGetShaderInfoLog(vertexShader));
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);
        glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, success);
        if (success.get(0) == 0) {
            System.err.println(glGetShaderInfoLog(fragmentShader));
        }

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        glGetProgramiv(shaderProgram, GL_COMPILE_STATUS, success);
        if (success.get(0) == 0) {
            System.err.println(glGetProgramInfoLog(shaderProgram));
        }

        rotationOffset = glGetUniformLocation(shaderProgram, "rotationOffset");

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public void genVAO() {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_DOUBLE, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void draw() {
        glUseProgram(shaderProgram);
        glBindVertexArray(vao);
        glUniform1f(rotationOffset, (float)Jexx.rotationOffset);
        glDrawElements(GL_TRIANGLES, 12, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

}
