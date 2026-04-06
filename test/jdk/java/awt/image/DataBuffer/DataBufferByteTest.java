import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;

/*
 * @test
 * @bug 8377568
 * @summary Tests for constructor and methods of DataBuffer classes
 * @run junit DataBufferByteTest
 */

public class DataBufferByteTest extends DataBufferTest {
    @Override
    protected DataBuffer createDataBufferInt(int size) {
        return new DataBufferByte(size);
    }

    @Override
    protected DataBuffer createDataBufferArraySizeOffset(int size, int offset) {
        return new DataBufferByte(new byte[DATA_ARRAY_SIZE], size, offset);
    }
}
