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
import java.util.ArrayList;
import java.util.Iterator;

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
    public static final double BLOCK_SPEED = 1.5;
    public static final double BLOCK_DELAY = 3;

    private final int WIDTH = 600, HEIGHT = 600;
    private final int NUM_BLOCKS = 5;

    private Hex hex = new Hex();

    private Block[][] blocks = new Block[6][NUM_BLOCKS];
    private ArrayList<Block> fallingBlocks = new ArrayList<>();

    private long window;

    private int mod(int x, int y) {
        return x % y + (x < 0 ? y : 0);
    }

    private int floodFill(int i, int j, int color) {
        if (j < 0 || j >= NUM_BLOCKS) return 0;
        boolean rightColor = blocks[i][j].color == color;
        if (rightColor) blocks[i][j].color += 100;
        return rightColor ?  (1 +
            floodFill(mod(i+1, 6), j, color) +
            floodFill(mod(i-1, 6), j, color) +
            floodFill(i, j+1, color) +
            floodFill(i, j-1, color)) : 0;
    }

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
            } else if (key == GLFW_KEY_RIGHT && action == GLFW_PRESS) {
                for (int i = 0; i < 5; ++i) {
                    for (int j = 0; j < NUM_BLOCKS; ++j) {
                        // swap color with block immediately to the left
                        blocks[i][j].color += blocks[i+1][j].color -
                            (blocks[i+1][j].color = blocks[i][j].color);
                    }
                }
            } else if (key == GLFW_KEY_LEFT && action == GLFW_PRESS) {
                for (int i = 4; i >= 0; --i) {
                    for (int j = 0; j < NUM_BLOCKS; ++j) {
                        // swap color with block immediately to the left
                        blocks[i][j].color += blocks[i+1][j].color -
                            (blocks[i+1][j].color = blocks[i][j].color);
                    }
                }
            }
        });

        glfwSwapInterval(1); // v-sync

        hex.compileShader();
        hex.genVAO();

        Block.compileShaders();
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < NUM_BLOCKS; ++j) {
                blocks[i][j] = new Block();
                blocks[i][j].genVAO(i, j, GL_STATIC_DRAW);
            }
        }
        blocks[0][0].color = 0; // temporary

        glfwShowWindow(window);
    }

    private void loop() {
        glClearColor(0x18 / 255f, 0x18 / 255f, 0x18 / 255f, 1);

        double lastTime = glfwGetTime();
        double timeSinceBlock = 0;

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            glClear(GL_COLOR_BUFFER_BIT);

            double time = glfwGetTime();
            double deltaTime = time - lastTime;
            lastTime = time;

            timeSinceBlock += deltaTime;
            if (timeSinceBlock >= BLOCK_DELAY) {
                timeSinceBlock -= BLOCK_DELAY;
                Block block = new Block();
                block.color = (int)(Math.random() * 6);
                block.genVAO((int)(Math.random() * 6), NUM_BLOCKS, GL_DYNAMIC_DRAW);
                fallingBlocks.add(block);
            }

            hex.draw();

            for (Iterator<Block> it = fallingBlocks.iterator(); it.hasNext();) {
                Block block = it.next();
                block.dist -= BLOCK_SPEED * deltaTime;
                if (block.dist <= 0 || blocks[block.rot][(int)(block.dist)].color != -1) {
                    for (int i = 0; i < NUM_BLOCKS; ++i) {
                        if (blocks[block.rot][i].color == -1) {
                            blocks[block.rot][i].color = block.color;
                            int numTouching = floodFill(block.rot, i, block.color);
                            for (int r = 0; r < 6; ++r) {
                                for (int d = 0; d < NUM_BLOCKS; ++d) {
                                    if (blocks[r][d].color >= 100) {
                                        if (numTouching >= 3) {
                                            blocks[r][d].color = -1;
                                        } else {
                                            blocks[r][d].color -= 100;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        // TODO check for losing
                    }
                    it.remove();
                } else {
                    block.updateVAO();
                    block.draw();
                }
            }
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
