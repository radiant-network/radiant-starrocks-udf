package org.radiant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VariantIdUDFTest {

    private final VariantIdUDF udf = new VariantIdUDF();

    @Test
    public void testSNV() {
        Long id = udf.evaluate("1", 1234567L, "A", "T");
        assertNotNull(id);
        assertTrue(id < 0); // MSB set → negative
    }

    @Test
    public void testDeletion() {
        Long id = udf.evaluate("2", 7654321L, "ATG", "A");
        assertNotNull(id);
        assertTrue(id < 0);
    }

    @Test
    public void testMicroInsertion() {
        Long id = udf.evaluate("X", 100L, "G", "GA");
        assertNotNull(id);
        assertTrue(id < 0);
    }

    @Test
    public void testInvalidChromosome() {
        assertNull(udf.evaluate("Z", 123L, "A", "T"));
        assertNull(udf.evaluate("25", 123L, "A", "T"));
    }

    @Test
    public void testNullInputs() {
        assertNull(udf.evaluate(null, 1L, "A", "T"));
        assertNull(udf.evaluate("1", null, "A", "T"));
        assertNull(udf.evaluate("1", 1L, null, "T"));
        assertNull(udf.evaluate("1", 1L, "A", null));
    }

    @Test
    public void testUnsupportedVariant() {
        // insertion > 1bp (should be assigned a sequence ID instead)
        assertNull(udf.evaluate("1", 1L, "A", "AGTC"));
        // ref and alt of same length > 1 (e.g., MNV)
        assertNull(udf.evaluate("1", 1L, "AG", "TC"));

        // Indel
        assertNull(udf.evaluate("1", 1L, "ATTTCG", "T"));
        assertNull(udf.evaluate("1", 1L, "ATTTCG", "TC"));

        // Unsupported allele
        assertNull(udf.evaluate("1", 1L, "A", "*"));
        assertNull(udf.evaluate("1", 1L, "AG", "*"));
    }

    @Test
    public void testMaxStart() {
        assertNotNull(udf.evaluate("1", 999_000_000L, "A", "T")); // max start
        assertNull(udf.evaluate("1", 999_000_001L, "A", "T"));    // out of range
    }


    @Test
    public void testMaxLength() {
        String ref = "A".repeat(33_554_431); // Max length for 25 bits
        assertNotNull(udf.evaluate("1", 123L, ref, "A"));

        String tooLong = "A".repeat(33_554_432); // Too large
        assertNull(udf.evaluate("1", 123L, tooLong, "A"));
    }

    @Test
    public void testRealVariant() {
        //SELECT GET_VARIANT_ID('8', 83072965, 'G', 'A');
        Long id = udf.evaluate("8", 83072965L, "G", "A");
        assertNotNull(id);
        assertTrue(id < 0);
    }
}

