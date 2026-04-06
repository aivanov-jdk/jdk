import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;

/*
 * @test
 * @bug 8377568
 * @summary Tests for constructor and methods of DataBuffer classes
 * @run junit DataBufferByteTest
 */

public class DataBufferIntTest extends DataBufferTest {
    @Override
    protected DataBuffer createDataBufferInt(int size) {
        return new DataBufferInt(size);
    }

    @Override
    protected DataBuffer createDataBufferArraySizeOffset(int size, int offset) {
        return new DataBufferInt(new int[DATA_ARRAY_SIZE], size, offset);
    }
}
