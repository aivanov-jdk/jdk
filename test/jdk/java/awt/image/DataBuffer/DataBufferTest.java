import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class DataBufferTest {
    protected static final int DATA_ARRAY_SIZE = 10;

    /**
     * Creates a {@code DataBuffer} object passing only the size.
     * <p>
     * This method corresponds to {@code DataBufferByte(int size)}.
     *
     * @param size the size of the {@code DataBuffer}
     * @return an instance of {@code DataBuffer}
     *
     * @see DataBufferByte#DataBufferByte(int)
     */
    protected abstract DataBuffer createDataBufferSize(int size);

    /**
     * Creates a {@code DataBuffer} object passing the size and an offset.
     * <p>
     * This method corresponds to {@code DataBufferByte(int size, int numBanks)}.
     *
     * @param size the size of the data buffer
     * @param numBanks the number of banks in the data buffer
     * @return an instance of {@code DataBuffer}
     *
     * @see DataBufferByte#DataBufferByte(int, int)
     */
    protected abstract DataBuffer createDataBufferSizeBanks(int size, int numBanks);

    /**
     * Creates a {@code DataBuffer} a data array of size of
     * {@value DATA_ARRAY_SIZE} as well as specifying the size of the buffer.
     * <p>
     * This method corresponds to {@code DataBufferByte(byte[] dataArray, int size)}.
     *
     * @param size the size of the data buffer
     * @return an instance of {@code DataBuffer}
     *
     * @see DataBufferByte#DataBufferByte(byte[], int)
     */
    protected abstract DataBuffer createDataBufferArraySize(int size);

    /**
     * Creates a {@code DataBuffer} passing {@code null} as the data array.
     * <p>
     * This method corresponds to {@code DataBufferByte(byte[] dataArray, int size)}.
     *
     * @see DataBufferByte#DataBufferByte(byte[], int)
     */
    protected abstract void createDataBufferArrayNullSize();

    /**
     * Creates a {@code DataBuffer} object passing a data array of size of
     * {@value DATA_ARRAY_SIZE} as well as specifying the size of the buffer
     * and the offset into the data array.
     * <p>
     * This method corresponds to
     * {@code DataBufferByte(byte[] dataArray, int size, int offset)}.
     *
     * @param size the size of the data buffer
     * @param offset the offset into the data array
     * @return an instance of {@code DataBuffer}
     * @see DataBufferByte#DataBufferByte(byte[], int, int)
     */
    protected abstract DataBuffer createDataBufferArraySizeOffset(int size, int offset);


    /* Tests for DataBuffer*(int size) */

    @Test
    public final void size_SizeNegative() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferSize(-1));
        assertEquals("Size must be > 0", iae.getMessage());
    }

    @Test
    public final void size_SizeZero() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferSize(0));
        assertEquals("Size must be > 0", iae.getMessage());
    }

    @Test
    public final void size_SizeOne() {
        DataBuffer db = createDataBufferSize(1);
        assertEquals(1, db.getSize());
    }


    /* Tests for DataBuffer*(int size, int numBanks) */

    @Test
    public final void sizeBanks_SizeNegative() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferSizeBanks(-1, 0));
        assertEquals("Size must be > 0", iae.getMessage());
    }

    @Test
    public final void sizeBanks_SizeZero() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferSizeBanks(0, 0));
        assertEquals("Size must be > 0", iae.getMessage());
    }

    @Test
    public final void sizeBanks_SizeOne_BanksNegative() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferSizeBanks(1, -1));
        assertEquals("Must have at least one bank", iae.getMessage());
    }

    @Test
    public final void sizeBanks_SizeOne_BanksZero() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferSizeBanks(1, 0));
        assertEquals("Must have at least one bank", iae.getMessage());
    }

    @Test
    public final void sizeBanks_SizeOne_BanksOne() {
        DataBuffer db = createDataBufferSizeBanks(1, 1);
        assertEquals(1, db.getSize());
        assertEquals(1, db.getNumBanks());
    }


    /* Tests for DataBuffer*(*[] dataArray, int size) */

    @Test
    public final void arraySize_ArrayNull() {
        var iae = assertThrows(IllegalArgumentException.class,
                               this::createDataBufferArrayNullSize);
        assertEquals("dataArray must not be null", iae.getMessage());
    }

    @Test
    public final void arraySize_SizeNegative() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySize(-1));
        assertEquals("Size must be > 0", iae.getMessage());
    }

    @Test
    public final void arraySize_SizeZero() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySize(0));
        assertEquals("Size must be > 0", iae.getMessage());
    }

    @Test
    public final void arraySize_SizeOne() {
        DataBuffer db = createDataBufferArraySize(1);
        assertEquals(1, db.getSize());
    }

    @Test
    public final void arraySize_SizeLarge() {
        DataBuffer db = createDataBufferArraySize(DATA_ARRAY_SIZE);
        assertEquals(DATA_ARRAY_SIZE, db.getSize());
    }

    @Test
    public final void arraySize_SizeLarger() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySize(DATA_ARRAY_SIZE + 1));
        assertEquals("Bad size : " + (DATA_ARRAY_SIZE + 1),
                     iae.getMessage());
    }


    /* Tests for DataBuffer*(byte[] dataArray, int size, int offset) */

    protected static final String BAD_SIZE_OFFSET =
            "Bad size/offset. Size = %d, offset = %d, array length = %d";

    @Test
    public final void arraySizeOffset_NegativeSize() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(-1, 0));
        assertEquals(BAD_SIZE_OFFSET.formatted(-1, 0, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public final void arraySizeOffset_ZeroSize() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(0, 0));
        assertEquals(BAD_SIZE_OFFSET.formatted(0, 0, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public final void arraySizeOffset_NegativeOffset() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(1, -1));
        assertEquals(BAD_SIZE_OFFSET.formatted(1, -1, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public final void arraySizeOffset_SizeOffsetOverflow() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(Integer.MAX_VALUE, 1));
        assertEquals(BAD_SIZE_OFFSET.formatted(Integer.MAX_VALUE, 1, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public final void arraySizeOffset_SizeOffsetOverflowArrayLength() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(DATA_ARRAY_SIZE, 1));
        assertEquals(BAD_SIZE_OFFSET.formatted(DATA_ARRAY_SIZE, 1, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public final void arraySizeOffset_SizeOffsetNoOverflowArrayLength() {
        DataBuffer db = createDataBufferArraySizeOffset(DATA_ARRAY_SIZE, 0);
        assertEquals(DATA_ARRAY_SIZE, db.getSize());
        assertEquals(0, db.getOffset());
        assertEquals(0, db.getElem(0));

        var aioobe = assertThrows(ArrayIndexOutOfBoundsException.class,
                                  () -> db.getElem(DATA_ARRAY_SIZE));
        assertEquals("Invalid index (offset+i) is (0 + 10) which is too large for size : "
                     + DATA_ARRAY_SIZE,
                     aioobe.getMessage());
    }

    @Test
    public final void arraySizeOffset_SizeOffsetValid() {
        DataBuffer db = createDataBufferArraySizeOffset(1, 1);
        assertEquals(1, db.getSize());
        assertEquals(1, db.getOffset());
        // TODO Should it throw?
        assertEquals(0, db.getElem(0));

        var aioobe = assertThrows(ArrayIndexOutOfBoundsException.class,
                                  () -> db.getElem(1));
        assertEquals("Invalid index (offset+i) is (1 + 1) which is too large for size : 1",
                     aioobe.getMessage());
    }
}
