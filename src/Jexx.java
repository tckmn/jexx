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

package jexx;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.OptionalInt;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.openal.*;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.STBVorbisInfo;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Jexx {

    public static final double HEX_SIZE = 0.2,
                               BLOCK_SIZE = 0.08,
                               BLOCK_SPEED = 3,
                               BLOCK_DELAY = 3;

    static double rotationOffset;

    private static final int WIDTH = 600,
                             HEIGHT = 600,
                             NUM_BLOCKS = 8;

    private Hex hex = new Hex();

    protected Block[][] blocks = new Block[6][NUM_BLOCKS];
    private ArrayList<Block> fallingBlocks = new ArrayList<>();

    private long window;

    private long alcContext, alcDevice;
    private int clickSource, slideSource, popSource;

    private int mod(final int x, final int y) {
        return x % y + (x < 0 ? y : 0);
    }

    private int floodFill(final int i, final int j, final int color) {
        if (j < 0 || j >= NUM_BLOCKS) {
            return 0;
        }
        final boolean rightColor = blocks[i][j].color == color;
        if (rightColor) {
            blocks[i][j].color += 100;
        }
        return rightColor ? (1
            + floodFill(mod(i + 1, 6), j, color)
            + floodFill(mod(i - 1, 6), j, color)
            + floodFill(i, j + 1, color)
            + floodFill(i, j - 1, color)) : 0;
    }

    private void postFloodFill(final ListIterator<Block> it, final int numTouching) {
        for (int r = 0; r < 6; ++r) {
            for (int d = 0; d < NUM_BLOCKS; ++d) {
                if (blocks[r][d].color >= 100) {
                    if (numTouching >= 3) {
                        blocks[r][d].color = -1;
                        for (int d2 = d + 1; d2 < NUM_BLOCKS; ++d2) {
                            if (blocks[r][d2].color != -1 && blocks[r][d2].color < 100) {
                                final Block fall = new Block();
                                fall.color = blocks[r][d2].color;
                                fall.genVAO(r, d2, GL_DYNAMIC_DRAW);
                                it.add(fall);
                                it.previous();
                                blocks[r][d2].color = -1;
                            }
                        }
                    } else {
                        blocks[r][d].color -= 100;
                    }
                }
            }
        }
    }

    private void spawnBlocks() {
        final Random rand = new Random();
        final Block block = new Block();
        block.color = (int) (rand.nextInt(6));
        block.genVAO((int) (rand.nextInt(6)), NUM_BLOCKS, GL_DYNAMIC_DRAW);
        fallingBlocks.add(block);
    }

    private int loadAudioSource(final String filePath) {
        final int buffer = alGenBuffers();
        final int source = alGenSources();

        final ByteBuffer vorbis;
        try {
            try (SeekableByteChannel fc = Files.newByteChannel(Paths.get(filePath))) {
                vorbis = BufferUtils.createByteBuffer((int) fc.size() + 1);
                while (fc.read(vorbis) != -1);
            }
            vorbis.flip();
        } catch (java.io.IOException ex) {
            throw new RuntimeException(ex);
        }

        final IntBuffer error = BufferUtils.createIntBuffer(1);
        final long decoder = stb_vorbis_open_memory(vorbis, error, null);
        if (decoder == NULL) {
            System.err.println("stb_vorbis_open_memory: " + error.get(0));
        }
        final STBVorbisInfo info = STBVorbisInfo.malloc();
        stb_vorbis_get_info(decoder, info);
        final int channels = info.channels();
        final int lengthSamples = stb_vorbis_stream_length_in_samples(decoder);
        final ShortBuffer pcm = BufferUtils.createShortBuffer(lengthSamples);
        pcm.limit(stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm) * channels);
        stb_vorbis_close(decoder);

        alBufferData(buffer, info.channels() == 1 ?
                AL_FORMAT_MONO16 : AL_FORMAT_STEREO16,
                pcm, info.sample_rate());
        alSourcei(source, AL_BUFFER, buffer);

        return source;
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

            alcDestroyContext(alcContext);
            alcCloseDevice(alcDevice);
        }
    }

    private void init() {
        // OpenAL stuff

        alcDevice = alcOpenDevice((ByteBuffer) null);
        alcContext = alcCreateContext(alcDevice, (IntBuffer) null);
        alcMakeContextCurrent(alcContext);
        AL.createCapabilities(ALC.createCapabilities(alcDevice));

        clickSource = loadAudioSource("click.ogg");
        slideSource = loadAudioSource("slide.ogg");
        popSource = loadAudioSource("pop.ogg");

        // OpenGL stuff

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

        final IntBuffer w = BufferUtils.createIntBuffer(1);
        final IntBuffer h = BufferUtils.createIntBuffer(1);
        glfwGetFramebufferSize(window, w, h);
        glViewport(0, 0, w.get(0), h.get(0));

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            } else if (key == GLFW_KEY_RIGHT && action == GLFW_PRESS) {
                for (int i = 0; i < 5; ++i) {
                    for (int j = 0; j < NUM_BLOCKS; ++j) {
                        // swap color with block immediately to the left
                        blocks[i][j].color += blocks[i + 1][j].color
                            - (blocks[i + 1][j].color = blocks[i][j].color);
                    }
                }
                rotationOffset -= Math.PI / 3;
                alSourcePlay(popSource);
            } else if (key == GLFW_KEY_LEFT && action == GLFW_PRESS) {
                for (int i = 4; i >= 0; --i) {
                    for (int j = 0; j < NUM_BLOCKS; ++j) {
                        // swap color with block immediately to the left
                        blocks[i][j].color += blocks[i + 1][j].color
                            - (blocks[i + 1][j].color = blocks[i][j].color);
                    }
                }
                rotationOffset += Math.PI / 3;
                alSourcePlay(popSource);
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

        glfwShowWindow(window);
    }

    private void loop() {
        glClearColor(0x18 / 255f, 0x18 / 255f, 0x18 / 255f, 1);

        double lastTime = glfwGetTime();
        double timeSinceBlock = 0;

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            glClear(GL_COLOR_BUFFER_BIT);

            final double time = glfwGetTime();
            final double deltaTime = time - lastTime;
            lastTime = time;

            rotationOffset *= Math.pow(0.003, deltaTime);
            if (Math.abs(rotationOffset) < 0.01) {
                rotationOffset = 0;
            }

            timeSinceBlock += deltaTime;
            if (timeSinceBlock >= BLOCK_DELAY) {
                timeSinceBlock -= BLOCK_DELAY;
                spawnBlocks();
            }

            hex.draw();

            boolean playClick = false, playSlide = false;
            for (ListIterator<Block> it = fallingBlocks.listIterator(); it.hasNext();) {
                final Block block = it.next();
                block.dist -= BLOCK_SPEED * deltaTime;
                if (block.dist <= 0 || blocks[block.rot][(int) block.dist].color != -1) {
                    it.remove();
                    playClick = true;
                    final OptionalInt d = IntStream.range(0, NUM_BLOCKS)
                        .filter(x -> { return blocks[block.rot][x].color == -1; })
                        .findFirst();
                    if (d.isPresent()) {
                        blocks[block.rot][d.getAsInt()].color = block.color;
                        final int numTouching = floodFill(block.rot, d.getAsInt(), block.color);
                        if (numTouching >= 3) {
                            playSlide = true;
                        }
                        postFloodFill(it, numTouching);
                    } else {
                        // TODO game is lost
                    }
                } else {
                    block.updateVAO();
                    block.draw(true);
                }
            }
            Stream.of(blocks).flatMap(Stream::of).forEach(Block::draw);

            if (playSlide) {
                alSourcePlay(slideSource);
            } else if (playClick) {
                alSourcePlay(clickSource);
            }

            glfwSwapBuffers(window);
        }
    }

    public static void main(final String[] args) {
        new Jexx().run();
    }

}
