package org.radiant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CNVIdUDFTest {

    private final CNVIdUDF udf = new CNVIdUDF();


    public static boolean isGain(long encoded) {
        return ((encoded >>> 63) & 0x1L) == 0;
    }

    public static int getChromNum(long encoded) {
        return (int) ((encoded >>> 58) & 0x1FL);
    }

    public static int getStart(long encoded) {
        return (int) ((encoded >>> 28) & 0x3FFFFFFFL);
    }

    public static int getLength(long encoded) {
        return (int) (encoded & 0xFFFFFFFL);
    }

    @Test
    public void testDeletion() {
        Long id = udf.evaluate("1", 1234567L, 1000L, "<DEL>");
        assertNotNull(id);
        assertTrue(id < 0); // MSB set → negative
    }

    @Test
    public void testDuplication() {
        Long id = udf.evaluate("1", 1234567L, 1000L, "<DUP>");
        assertNotNull(id);
        assertTrue(id > 0); // MSB set → negative
    }

    @Test
    void testEncodeDecodeRoundTrip() {
        String alt = "<DEL>";
        String chrom = "X";
        long start = 123456789;
        long length = 54321;

        long encoded = udf.evaluate(chrom, start, length, alt);

        assertFalse(isGain(encoded));
        assertEquals(23L, getChromNum(encoded));
        assertEquals(start, getStart(encoded));
        assertEquals(length, getLength(encoded));
    }

}

