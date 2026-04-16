import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;

/*
 * @test
 * @bug 8377568
 * @summary Tests for constructor and methods of DataBuffer classes
 * @run junit DataBufferByteTest
 */

public final class DataBufferIntTest extends DataBufferTest {
    @Override
    protected DataBuffer createDataBufferSize(int size) {
        return new DataBufferInt(size);
    }

    @Override
    protected DataBuffer createDataBufferSizeBanks(int size, int numBanks) {
        return new DataBufferInt(size, numBanks);
    }

    @Override
    protected DataBuffer createDataBufferArraySize(int size) {
        return new DataBufferInt(new int[DATA_ARRAY_SIZE], size);
    }

    @Override
    protected void createDataBufferArrayNullSize() {
        new DataBufferInt((int[]) null, -1);
    }

    @Override
    protected DataBuffer createDataBufferArraySizeOffset(int size, int offset) {
        return new DataBufferInt(new int[DATA_ARRAY_SIZE], size, offset);
    }
}
