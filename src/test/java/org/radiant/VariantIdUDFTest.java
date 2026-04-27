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

    @Test
    public void testMicroInsertionAnchorMismatch() {
        // alt[0] must match ref[0] — micro-insertion anchor base
        assertNull(udf.evaluate("1", 100L, "A", "TG"));
        assertNull(udf.evaluate("1", 100L, "G", "AT"));
        assertNull(udf.evaluate("1", 100L, "C", "GA"));
    }

    @Test
    public void testDeletionAnchorMismatch() {
        // alt[0] must match ref[0] — deletion anchor base
        assertNull(udf.evaluate("1", 100L, "ATG", "T"));
        assertNull(udf.evaluate("1", 100L, "GCC", "C"));
    }

    @Test
    public void testMicroInsertionAnchorMatch() {
        // alt[0] == ref[0] — valid micro-insertion
        assertNotNull(udf.evaluate("1", 100L, "A", "AT"));
        assertNotNull(udf.evaluate("1", 100L, "G", "GC"));
        assertNotNull(udf.evaluate("1", 100L, "C", "CA"));
        assertNotNull(udf.evaluate("1", 100L, "T", "TG"));
    }

    @Test
    public void testDeterminism() {
        Long a = udf.evaluate("1", 12345L, "A", "T");
        Long b = udf.evaluate("1", 12345L, "A", "T");
        assertEquals(a, b);

        Long c = udf.evaluate("X", 999L, "ATG", "A");
        Long d = udf.evaluate("X", 999L, "ATG", "A");
        assertEquals(c, d);
    }

    @Test
    public void testDistinctVariantsDistinctIds() {
        Long snv = udf.evaluate("1", 100L, "A", "T");
        Long del = udf.evaluate("1", 100L, "AT", "A");
        Long ins = udf.evaluate("1", 100L, "A", "AT");
        Long otherChrom = udf.evaluate("2", 100L, "A", "T");
        Long otherPos = udf.evaluate("1", 101L, "A", "T");
        Long otherAlt = udf.evaluate("1", 100L, "A", "G");

        assertNotEquals(snv, del);
        assertNotEquals(snv, ins);
        assertNotEquals(snv, otherChrom);
        assertNotEquals(snv, otherPos);
        assertNotEquals(snv, otherAlt);
        assertNotEquals(del, ins);
    }

    @Test
    public void testBitLayoutSnv() {
        // Layout: | MSB=1 | chrom (5) | start (30) | alt (3) | length (25) |
        // chrom=1, start=12345, alt=T(2), length=0
        Long id = udf.evaluate("1", 12345L, "A", "T");
        assertNotNull(id);
        assertEquals(1L, (id >>> 63) & 0x1L);                   // MSB=1
        assertEquals(1L, (id >>> 58) & 0x1FL);                  // chrom
        assertEquals(12345L, (id >>> 28) & 0x3FFFFFFFL);        // start
        assertEquals(2L, (id >>> 25) & 0x7L);                   // alt = T
        assertEquals(0L, id & 0x1FFFFFFL);                      // length
    }

    @Test
    public void testBitLayoutDeletion() {
        // chrom=X(23), start=500, alt=0, length=3 (ref="ATG")
        Long id = udf.evaluate("X", 500L, "ATG", "A");
        assertNotNull(id);
        assertEquals(1L, (id >>> 63) & 0x1L);
        assertEquals(23L, (id >>> 58) & 0x1FL);
        assertEquals(500L, (id >>> 28) & 0x3FFFFFFFL);
        assertEquals(0L, (id >>> 25) & 0x7L);
        assertEquals(3L, id & 0x1FFFFFFL);
    }

    @Test
    public void testBitLayoutMicroInsertion() {
        // chrom=Y(24), start=42, alt=G(4), length=1
        Long id = udf.evaluate("Y", 42L, "C", "CG");
        assertNotNull(id);
        assertEquals(1L, (id >>> 63) & 0x1L);
        assertEquals(24L, (id >>> 58) & 0x1FL);
        assertEquals(42L, (id >>> 28) & 0x3FFFFFFFL);
        assertEquals(4L, (id >>> 25) & 0x7L);
        assertEquals(1L, id & 0x1FFFFFFL);
    }

    @Test
    public void testAllChromosomes() {
        for (int i = 1; i <= 22; i++) {
            assertNotNull(udf.evaluate(String.valueOf(i), 1L, "A", "T"));
        }
        assertNotNull(udf.evaluate("X", 1L, "A", "T"));
        assertNotNull(udf.evaluate("Y", 1L, "A", "T"));
        assertNotNull(udf.evaluate("M", 1L, "A", "T"));
        assertNotNull(udf.evaluate("MT", 1L, "A", "T"));
    }

    @Test
    public void testAllSnvBases() {
        assertNotNull(udf.evaluate("1", 1L, "A", "C"));
        assertNotNull(udf.evaluate("1", 1L, "A", "G"));
        assertNotNull(udf.evaluate("1", 1L, "A", "T"));
        assertNotNull(udf.evaluate("1", 1L, "C", "A"));
        assertNotNull(udf.evaluate("1", 1L, "G", "A"));
        assertNotNull(udf.evaluate("1", 1L, "T", "A"));
    }

    @Test
    public void testInvalidStart() {
        assertNull(udf.evaluate("1", 0L, "A", "T"));
        assertNull(udf.evaluate("1", -1L, "A", "T"));
    }

    @Test
    public void testEmptyAllele() {
        assertNull(udf.evaluate("1", 1L, "", "T"));
        assertNull(udf.evaluate("1", 1L, "A", ""));
    }
}

