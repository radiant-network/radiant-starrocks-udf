package org.radiant;

public class VariantIdUDF {

    private static final long SNV_FLAG = 1L << 63;

    /**
     * StarRocks UDF-compatible method
     * Encodes SNV, deletion, and micro-insertion variants into signed 64-bit IDs
     *
     * @param chrom Chromosome string (1–22, X, Y, M)
     * @param start 1-based start position (≤ 999,000,000)
     * @param ref   Reference allele (e.g. "A")
     * @param alt   Alternate allele (e.g. "T")
     * @return Encoded ID (MSB = 1 for SNV/deletion/micro-insertion), or null
     */
    public Long evaluate(String chrom, Long start, String ref, String alt) {
        if (alt == null || alt.length() > 2 || chrom == null || start == null || ref == null) return null;

        int chromNum = Utils.parseChromosome(chrom);
        if (chromNum < 1 || chromNum > 25) return null;
        if (start < 1 || start > 999_000_000L) return null;

        int altCode = 0;
        int lengthCode = 0;

        int refLen = ref.length();
        int altLen = alt.length();

        if (altLen == 1 && refLen == 1) {
            // SNV
            altCode = baseCode(alt.charAt(0));
        } else if (altLen == 1 && refLen > 1) {
            // Deletion
            if (alt.charAt(0) != ref.charAt(0)) return null; // alt must match first base of ref
            lengthCode = refLen;
        } else if (altLen == 2 && refLen == 1) {
            // Micro-insertion
            altCode = baseCode(alt.charAt(1));
            lengthCode = 1;
        } else {
            // Insertion or unsupported case
            return null;
        }

        if (altCode < 0 || altCode > 4) return null;
        if (lengthCode > 33_554_431) return null;  // max for 25 bits

        long encoded = 0L;
        // Bit layout: | chrom (5) | start (30) | alt (3) | length (25) = 63 bits
        encoded |= ((long) chromNum) << (30 + 3 + 25);
        encoded |= start << (3 + 25);
        encoded |= ((long) altCode) << 25;
        encoded |= lengthCode;

        return SNV_FLAG | encoded;
    }



    private int baseCode(char base) {
        switch (base) {
            case 'A':
                return 1;
            case 'T':
                return 2;
            case 'C':
                return 3;
            case 'G':
                return 4;
            default:
                return -1;
        }
    }
}