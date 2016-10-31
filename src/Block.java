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

    private static int shaders[];

    private int arrayBuffer;
    private double vertices[];

    private static final int indices[] = {
        0, 1, 2,
        0, 2, 3
    };

    private int vao;

    private static final int[][] colors = {
        { 0xab, 0x46, 0x42 }, // red
        { 0xdc, 0x96, 0x56 }, // orange
        { 0xf7, 0xca, 0x88 }, // yellow
        { 0xa1, 0xb5, 0x6c }, // green
        { 0x7c, 0xaf, 0xc2 }, // blue
        { 0xba, 0x8b, 0xaf }  // purple
    };
    public int color = -1;
    public int rot;
    public double dist;

    public void draw() {
        if (color == -1) return;
        // System.out.println("drawing");

        glUseProgram(shaders[color]);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 12, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public static void compileShaders() {
        shaders = new int[colors.length];
        for (int c = 0; c < colors.length; ++c) {
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
                        colors[c][0], colors[c][1], colors[c][2]));
            glCompileShader(fragmentShader);
            glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, success);
            if (success.get(0) == 0) {
                System.err.println(glGetShaderInfoLog(fragmentShader));
            }

            shaders[c] = glCreateProgram();
            glAttachShader(shaders[c], vertexShader);
            glAttachShader(shaders[c], fragmentShader);
            glLinkProgram(shaders[c]);
            glGetProgramiv(shaders[c], GL_COMPILE_STATUS, success);
            if (success.get(0) == 0) {
                System.err.println(glGetProgramInfoLog(shaders[c]));
            }

            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
        }
    }

    public void genVAO(int rot, double dist, int usage) {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        this.rot = rot;
        this.dist = dist;
        updateVAO(true);

        arrayBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, arrayBuffer);
        glBufferData(GL_ARRAY_BUFFER, vertices, usage);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_DOUBLE, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void updateVAO(boolean initial) {
        double x1 = Math.cos(rot * Math.PI / 3),
               y1 = Math.sin(rot * Math.PI / 3),
               x2 = Math.cos((rot+1) * Math.PI / 3),
               y2 = Math.sin((rot+1) * Math.PI / 3);

        vertices = new double[] {
            x1 * (Jexx.HEX_SIZE + dist     * Jexx.BLOCK_SIZE), y1 * (Jexx.HEX_SIZE + dist     * Jexx.BLOCK_SIZE), 0,
            x1 * (Jexx.HEX_SIZE + (dist+1) * Jexx.BLOCK_SIZE), y1 * (Jexx.HEX_SIZE + (dist+1) * Jexx.BLOCK_SIZE), 0,
            x2 * (Jexx.HEX_SIZE + (dist+1) * Jexx.BLOCK_SIZE), y2 * (Jexx.HEX_SIZE + (dist+1) * Jexx.BLOCK_SIZE), 0,
            x2 * (Jexx.HEX_SIZE + dist     * Jexx.BLOCK_SIZE), y2 * (Jexx.HEX_SIZE + dist     * Jexx.BLOCK_SIZE), 0
        };

        if (!initial) {
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, arrayBuffer);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            glBindVertexArray(0);
        }
    }

    public void updateVAO() {
        updateVAO(false);
    }

}
