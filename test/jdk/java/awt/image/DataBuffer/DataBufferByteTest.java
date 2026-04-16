import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;

/*
 * @test
 * @bug 8377568
 * @summary Tests for constructor and methods of DataBuffer classes
 * @run junit DataBufferByteTest
 */

public final class DataBufferByteTest extends DataBufferTest {
    @Override
    protected DataBuffer createDataBufferSize(int size) {
        return new DataBufferByte(size);
    }

    @Override
    protected DataBuffer createDataBufferSizeBanks(int size, int numBanks) {
        return new DataBufferByte(size, numBanks);
    }

    @Override
    protected DataBuffer createDataBufferArraySize(int size) {
        return new DataBufferByte(new byte[DATA_ARRAY_SIZE], size);
    }

    @Override
    protected void createDataBufferArrayNullSize() {
        new DataBufferByte((byte[]) null, -1);
    }

    @Override
    protected DataBuffer createDataBufferArraySizeOffset(int size, int offset) {
        return new DataBufferByte(new byte[DATA_ARRAY_SIZE], size, offset);
    }
}
