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
    }
}