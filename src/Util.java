import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.channels.SeekableByteChannel;
import org.lwjgl.BufferUtils;

class Util {

    public static ByteBuffer readByteBuffer(String path)
            throws java.io.IOException {
        ByteBuffer buf;
        try (SeekableByteChannel fc = Files.newByteChannel(Paths.get(path))) {
            buf = BufferUtils.createByteBuffer((int)fc.size() + 1);
            while (fc.read(buf) != -1);
        }
        buf.flip();
        return buf;
    }

}
