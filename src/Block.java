import java.nio.IntBuffer;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Block {

    private static final String vertexShaderSource =
        "#version 330 core\n"
      + "layout (location = 0) in vec3 position;\n"
      + "void main() {\n"
      + "    gl_Position = vec4(position.x, position.y, position.z, 1.0);\n"
      + "}";

    private static final String fragmentShaderSource =
        "#version 330 core\n"
      + "out vec4 color;\n"
      + "void main() {\n"
      + "    color = vec4(%d / 255.0, %d / 255.0, %d / 255.0, 1.0f);\n"
      + "}";

    private static final int indices[] = {
        0, 1, 2,
        0, 2, 3
    };

    private int shaderProgram, vao;

    private static final int[][] colors = {
        { 0xab, 0x46, 0x42 }, // red
        { 0xdc, 0x96, 0x56 }, // orange
        { 0xf7, 0xca, 0x88 }, // yellow
        { 0xa1, 0xb5, 0x6c }, // green
        { 0x7c, 0xaf, 0xc2 }, // blue
        { 0xba, 0x8b, 0xaf }  // purple
    };
    public int color = -1;

    public void draw() {
        if (color == -1) return;
        // System.out.println("drawing");

        glUseProgram(shaderProgram);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 12, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void compileShader() {
        if (color == -1) return;

        IntBuffer success = BufferUtils.createIntBuffer(1);

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        glGetShaderiv(vertexShader, GL_COMPILE_STATUS, success);
        if (success.get(0) == 0) {
            System.err.println(glGetShaderInfoLog(vertexShader));
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, String.format(fragmentShaderSource,
                    colors[color][0], colors[color][1], colors[color][2]));
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

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public void genVAO(int rot, int dist) {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        double x1 = Math.cos(rot * Math.PI / 3),
               y1 = Math.sin(rot * Math.PI / 3),
               x2 = Math.cos((rot+1) * Math.PI / 3),
               y2 = Math.sin((rot+1) * Math.PI / 3);

        double vertices[] = {
            x1 * (Jexx.HEX_SIZE + dist     * Jexx.BLOCK_SIZE), y1 * (Jexx.HEX_SIZE + dist     * Jexx.BLOCK_SIZE), 0,
            x1 * (Jexx.HEX_SIZE + (dist+1) * Jexx.BLOCK_SIZE), y1 * (Jexx.HEX_SIZE + (dist+1) * Jexx.BLOCK_SIZE), 0,
            x2 * (Jexx.HEX_SIZE + (dist+1) * Jexx.BLOCK_SIZE), y2 * (Jexx.HEX_SIZE + (dist+1) * Jexx.BLOCK_SIZE), 0,
            x2 * (Jexx.HEX_SIZE + dist     * Jexx.BLOCK_SIZE), y2 * (Jexx.HEX_SIZE + dist     * Jexx.BLOCK_SIZE), 0
        };

        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_DOUBLE, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

}
