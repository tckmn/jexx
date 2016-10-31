/*
 * Copyright (C) 2016  KeyboardFire <andy@keyboardfire.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

public class Jexx {

    public static final double HEX_SIZE = 0.3;
    public static final double BLOCK_SIZE = 0.1;

    private final int WIDTH = 600, HEIGHT = 600;
    private final int NUM_BLOCKS = 10;

    private Hex hex = new Hex();

    private Block[][] blocks = new Block[6][NUM_BLOCKS];

    private long window;

    public void run() {
        try {
            init();
            loop();

            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        } finally {
            glfwTerminate();
            glfwSetErrorCallback(null).free();
        }
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("glfwInit() failed");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "Jexx", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("glfwCreateWindow() failed");
        }
        glfwMakeContextCurrent(window);

        GL.createCapabilities();

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        glfwGetFramebufferSize(window, w, h);
        glViewport(0, 0, w.get(0), h.get(0));

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        glfwSwapInterval(1); // v-sync

        hex.compileShader();
        hex.genVAO();

        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < NUM_BLOCKS; ++j) {
                blocks[i][j] = new Block();
                blocks[i][j].genVAO(i, j);
            }
        }
        blocks[0][0].color = 0; // temporary
        blocks[0][0].compileShader();

        glfwShowWindow(window);
    }

    private void loop() {
        glClearColor(0x18 / 255f, 0x18 / 255f, 0x18 / 255f, 1);

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            glClear(GL_COLOR_BUFFER_BIT);

            hex.draw();

            for (int i = 0; i < 6; ++i) {
                for (int j = 0; j < NUM_BLOCKS; ++j) {
                    blocks[i][j].draw();
                }
            }

            glfwSwapBuffers(window);
        }
    }

    public static void main(String[] args) {
        new Jexx().run();
    }

}
