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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTAlignedQuad;
import static org.lwjgl.stb.STBTruetype.*;

import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.*;

public class Text {

    private static final String vertexShaderSource =
        "#version 330 core\n"
      + "layout (location = 0) in vec3 position;\n"
      + "layout (location = 1) in vec2 texCoord;\n"
      + "out vec2 TexCoord;\n"
      + "void main() {\n"
      + "    gl_Position = vec4(position.x, position.y, position.z, 1.0);\n"
      + "    TexCoord = texCoord;\n"
      + "}";

    private static final String fragmentShaderSource =
        "#version 330 core\n"
      + "in vec2 TexCoord;\n"
      + "out vec4 color;\n"
      + "uniform sampler2D textureSampler;\n"
      + "void main() {\n"
      + "    color = vec4(0xd8 / 255.0, 0xd8 / 255.0, 0xd8 / 255.0, texture(textureSampler, TexCoord).r);"
      + "}";

    private int shaderProgram, vao;

    private static final int FONT_HEIGHT = 24,
                             BITMAP_W = 512,
                             BITMAP_H = 512;

    private STBTTBakedChar.Buffer cdata;

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

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public void genVAO(String text) {
        try (MemoryStack stack = stackPush()) {
            FloatBuffer x = stack.floats(0.0f);
            FloatBuffer y = stack.floats(0.0f);

            STBTTAlignedQuad q = STBTTAlignedQuad.mallocStack(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    y.put(0, y.get(0) + FONT_HEIGHT);
                    x.put(0, 0.0f);
                    continue;
                } else if (c < 32 || c >= 128) continue;
                stbtt_GetBakedQuad(cdata, BITMAP_W, BITMAP_H, c - 32, x, y, q, true);

                final double vertices[] = {
                    // coordinates                     // textures
                    q.x0() * 0.005, -q.y1() * 0.005, 0, q.s0(), q.t1(),
                    q.x1() * 0.005, -q.y1() * 0.005, 0, q.s1(), q.t1(),
                    q.x1() * 0.005, -q.y0() * 0.005, 0, q.s1(), q.t0(),
                    q.x0() * 0.005, -q.y0() * 0.005, 0, q.s0(), q.t0()
                };

                final int indices[] = {
                    0, 1, 2,
                    0, 2, 3
                };

                vao = glGenVertexArrays();
                glBindVertexArray(vao);

                glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
                glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, glGenBuffers());
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

                glVertexAttribPointer(0, 3, GL_DOUBLE, false, 8 * 5, 0);
                glVertexAttribPointer(1, 2, GL_DOUBLE, false, 8 * 5, 8 * 3);
                glEnableVertexAttribArray(0);
                glEnableVertexAttribArray(1);

                glBindBuffer(GL_ARRAY_BUFFER, 0);
                //glBindVertexArray(0);

                glUseProgram(shaderProgram);
                //glBindVertexArray(vao);
                glUniform1i(glGetUniformLocation(shaderProgram, "textureSampler"), 0);
                glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
                glBindVertexArray(0);
            }
        }
    }

    public void draw() {
        glUseProgram(shaderProgram);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public STBTTBakedChar.Buffer loadFont(String filePath) {
        int texture = glGenTextures();
        cdata = STBTTBakedChar.malloc(96);

        try {
            ByteBuffer font = Util.readByteBuffer(filePath);

            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);
            stbtt_BakeFontBitmap(font, FONT_HEIGHT, bitmap, BITMAP_W, BITMAP_H, 32, cdata);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, BITMAP_W, BITMAP_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        } catch (java.io.IOException ex) {
            throw new RuntimeException(ex);
        }

        glEnable(GL_BLEND);
        glEnable(GL_ALPHA_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        return cdata;
    }

}
