import java.awt.image.DataBuffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class DataBufferTest {
    protected static final int DATA_ARRAY_SIZE = 10;

    protected abstract DataBuffer createDataBufferInt(int size);
    protected abstract DataBuffer createDataBufferArraySizeOffset(int size, int offset);


    @Test
    public void dataBufferSizeNegative() {
        var iae = assertThrows(IllegalArgumentException.class,
                                () -> createDataBufferInt(-1));
        assertEquals("Size must be > 0", iae.getMessage());
    }

    @Test
    public void dataBufferSizeZero() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferInt(0));
        assertEquals("Size must be > 0", iae.getMessage());
    }

    @Test
    public void dataBufferSizeOne() {
        DataBuffer db = createDataBufferInt(1);
        assertEquals(1, db.getSize());
    }


    protected static final String BAD_SIZE_OFFSET =
            "Bad size/offset. Size = %d, offset = %d, array length = %d";

    @Test
    public void dataBufferArraySizeOffset_NegativeSize() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(-1, 0));
        assertEquals(BAD_SIZE_OFFSET.formatted(-1, 0, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public void dataBufferArraySizeOffset_ZeroSize() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(0, 0));
        assertEquals(BAD_SIZE_OFFSET.formatted(0, 0, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public void dataBufferArraySizeOffset_NegativeOffset() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(0, 1));
        assertEquals(BAD_SIZE_OFFSET.formatted(0, 1, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public void dataBufferArraySizeOffset_SizeOffsetOverflow() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(Integer.MAX_VALUE, 1));
        assertEquals(BAD_SIZE_OFFSET.formatted(Integer.MAX_VALUE, 1, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public void dataBufferArraySizeOffset_SizeOffsetOverflowArrayLength() {
        var iae = assertThrows(IllegalArgumentException.class,
                               () -> createDataBufferArraySizeOffset(DATA_ARRAY_SIZE, 1));
        assertEquals(BAD_SIZE_OFFSET.formatted(DATA_ARRAY_SIZE, 1, DATA_ARRAY_SIZE),
                     iae.getMessage());
    }

    @Test
    public void dataBufferArraySizeOffset_SizeOffsetNoOverflowArrayLength() {
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
    public void dataBufferArraySizeOffset_SizeOffsetValid() {
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
