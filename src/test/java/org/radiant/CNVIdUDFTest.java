package org.radiant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CNVIdUDFTest {

    private final CNVIdUDF udf = new CNVIdUDF();

    private static final long MAX_START = 268_435_455L;
    private static final long MAX_LENGTH = 268_435_455L;

    public static int getType(long encoded) {
        return (int) ((encoded >>> 61) & 0x7L);
    }

    public static int getChromNum(long encoded) {
        return (int) ((encoded >>> 56) & 0x1FL);
    }

    public static long getStart(long encoded) {
        return (encoded >>> 28) & 0xFFFFFFFL;
    }

    public static long getLength(long encoded) {
        return encoded & 0xFFFFFFFL;
    }

    @Test
    public void testTypeCodes() {
        assertEquals(0, getType(udf.evaluate("1", 1234567L, 1000L, "LOSS")));
        assertEquals(1, getType(udf.evaluate("1", 1234567L, 1000L, "GAIN")));
        assertEquals(2, getType(udf.evaluate("1", 1234567L, 1000L, "CNLOH")));
        assertEquals(3, getType(udf.evaluate("1", 1234567L, 1000L, "GAINLOH")));
    }

    @Test
    public void testAllIdsArePositive() {
        // Codes 0-3 leave bit 63 clear, so no CNV id collides with a VariantIdUDF id (which always sets it).
        for (String type : new String[]{"LOSS", "GAIN", "CNLOH", "GAINLOH"}) {
            Long id = udf.evaluate("M", MAX_START, MAX_LENGTH, type);
            assertNotNull(id, type);
            assertTrue(id > 0, type + " should encode to a positive id, got " + id);
        }
    }

    @Test
    public void testKnownEncoding() {
        // GAIN (code 1) on chr1, start 1000, length 500
        long expected = (1L << 61) | (1L << 56) | (1000L << 28) | 500L;
        assertEquals(2377900871687078388L, expected); // guards the layout itself
        assertEquals(expected, udf.evaluate("1", 1000L, 500L, "GAIN"));
    }

    @Test
    public void testEncodeDecodeRoundTrip() {
        long start = 123456789;
        long length = 54321;

        long encoded = udf.evaluate("X", start, length, "LOSS");

        assertEquals(0, getType(encoded));
        assertEquals(23, getChromNum(encoded));
        assertEquals(start, getStart(encoded));
        assertEquals(length, getLength(encoded));
    }

    @Test
    public void testMitochondrialChromosome() {
        assertEquals(25, getChromNum(udf.evaluate("M", 100L, 10L, "GAIN")));
        assertEquals(25, getChromNum(udf.evaluate("MT", 100L, 10L, "GAIN")));
    }

    @Test
    public void testAlternateAllelesAreNoLongerAccepted() {
        // The 4th argument is `type`, not `alternate`. If the JAR ships before the SQL call site is
        // updated, the load must fail on the NOT NULL key rather than write silently wrong ids.
        assertNull(udf.evaluate("1", 1234567L, 1000L, "<DEL>"));
        assertNull(udf.evaluate("1", 1234567L, 1000L, "<DUP>"));
        assertNull(udf.evaluate("1", 1234567L, 1000L, "<LOH>"));
    }

    @Test
    public void testUnsupportedType() {
        assertNull(udf.evaluate("1", 1234567L, 1000L, "UNKNOWN")); // what germline extraction emits today
        assertNull(udf.evaluate("1", 1234567L, 1000L, ""));
        assertNull(udf.evaluate("1", 1234567L, 1000L, "gain")); // matching is case-sensitive
        assertNull(udf.evaluate("1", 1234567L, 1000L, "LOH"));
    }

    @Test
    public void testNullInputs() {
        assertNull(udf.evaluate(null, 1L, 1L, "GAIN"));
        assertNull(udf.evaluate("1", null, 1L, "GAIN"));
        assertNull(udf.evaluate("1", 1L, null, "GAIN"));
        assertNull(udf.evaluate("1", 1L, 1L, null));
    }

    @Test
    public void testInvalidChromosome() {
        assertNull(udf.evaluate("Z", 123L, 10L, "GAIN"));
        assertNull(udf.evaluate("25", 123L, 10L, "GAIN"));
        assertNull(udf.evaluate("0", 123L, 10L, "GAIN"));
        assertNull(udf.evaluate("chr1", 123L, 10L, "GAIN"));
    }

    @Test
    public void testMaxStart() {
        assertNotNull(udf.evaluate("1", 1L, 10L, "GAIN"));
        assertNotNull(udf.evaluate("1", MAX_START, 10L, "GAIN"));
        assertNull(udf.evaluate("1", MAX_START + 1, 10L, "GAIN"));
        assertNull(udf.evaluate("1", 0L, 10L, "GAIN"));
        assertNull(udf.evaluate("1", -1L, 10L, "GAIN"));
        // valid under the previous 30-bit layout, out of range now
        assertNull(udf.evaluate("1", 999_000_000L, 10L, "GAIN"));
    }

    @Test
    public void testMaxLength() {
        assertNotNull(udf.evaluate("1", 123L, 0L, "GAIN"));
        assertNotNull(udf.evaluate("1", 123L, MAX_LENGTH, "GAIN"));
        assertNull(udf.evaluate("1", 123L, MAX_LENGTH + 1, "GAIN"));
        assertNull(udf.evaluate("1", 123L, -1L, "GAIN"));
    }

    @Test
    public void testFieldsDoNotBleedAtBoundaries() {
        // A length above 2^28 used to overflow silently into the start field.
        long encoded = udf.evaluate("22", 1000L, MAX_LENGTH, "GAINLOH");
        assertEquals(3, getType(encoded));
        assertEquals(22, getChromNum(encoded));
        assertEquals(1000L, getStart(encoded));
        assertEquals(MAX_LENGTH, getLength(encoded));

        long saturated = udf.evaluate("M", MAX_START, MAX_LENGTH, "GAINLOH");
        assertEquals(8_791_026_472_627_208_191L, saturated); // largest encodable id
        assertTrue(saturated < Long.MAX_VALUE);
        assertEquals(3, getType(saturated));
        assertEquals(25, getChromNum(saturated));
        assertEquals(MAX_START, getStart(saturated));
        assertEquals(MAX_LENGTH, getLength(saturated));
    }

    @Test
    public void testTypesAreDistinctAtSameCoordinates() {
        Long loss = udf.evaluate("1", 1234567L, 1000L, "LOSS");
        Long gain = udf.evaluate("1", 1234567L, 1000L, "GAIN");
        Long cnloh = udf.evaluate("1", 1234567L, 1000L, "CNLOH");
        Long gainloh = udf.evaluate("1", 1234567L, 1000L, "GAINLOH");

        assertEquals(4, java.util.stream.Stream.of(loss, gain, cnloh, gainloh).distinct().count());
    }
}
