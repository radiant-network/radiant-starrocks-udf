package org.radiant;

public class CNVIdUDF {

    private static final int START_SHIFT = 28; // length occupies bits 0-27
    private static final int CHROM_SHIFT = 56; // start occupies bits 28-55
    private static final int TYPE_SHIFT = 61;  // chrom occupies bits 56-60

    private static final long MAX_START = (1L << 28) - 1;  // 268,435,455
    private static final long MAX_LENGTH = (1L << 28) - 1; // 268,435,455

    /**
     * StarRocks UDF-compatible method
     * Encodes CNV segments into 64-bit IDs
     * <p>
     * Bit layout: type (3) | chrom (5) | start (28) | length (28) = 64 bits
     * <p>
     * The type is keyed on the resolved CNV type rather than on the VCF alternate allele: DRAGEN 4.2
     * spells an LOH event as multi-allelic {@code <DEL>,<DUP>} while 4.4 spells it {@code <LOH>}, so
     * an ALT-keyed ID would give the same segment two different IDs depending on the caller version.
     * <p>
     * The four known types take codes 0-3, which leaves bit 63 clear and so keeps every ID positive.
     * Codes 4-7 are reserved for future types and would set the sign bit. All {@code VariantIdUDF}
     * IDs set bit 63, so the two encodings never collide in the same column.
     *
     * @param chrom  Chromosome string (1–22, X, Y, M)
     * @param start  1-based start position (≤ 268,435,455)
     * @param length Length of the CNV (≤ 268,435,455)
     * @param type   Resolved CNV type: LOSS, GAIN, CNLOH or GAINLOH (upper case)
     * @return Encoded ID (always positive), or null when the segment is not encodable
     */
    public Long evaluate(String chrom, Long start, Long length, String type) {

        if (type == null || chrom == null || start == null || length == null) return null;

        int chromNum = Utils.parseChromosome(chrom);
        if (chromNum < 1 || chromNum > 25) return null;
        if (start < 1 || start > MAX_START) return null;
        if (length < 0 || length > MAX_LENGTH) return null;

        int typeCode = typeCode(type);
        if (typeCode < 0) return null; // unsupported type

        long encoded = 0L;
        encoded |= ((long) typeCode) << TYPE_SHIFT;
        encoded |= ((long) chromNum) << CHROM_SHIFT;
        encoded |= start << START_SHIFT;
        encoded |= length;

        return encoded;
    }

    private int typeCode(String type) {
        switch (type) {
            case "LOSS":
                return 0;
            case "GAIN":
                return 1;
            case "CNLOH":
                return 2;
            case "GAINLOH":
                return 3;
            default:
                return -1;
        }
    }
}
