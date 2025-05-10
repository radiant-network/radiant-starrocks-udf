package org.radiant;

public class VariantIdUDF {

    private static final long SNV_FLAG = 1L << 63;

    /**
     * StarRocks UDF-compatible method
     * Encodes SNV, deletion, and micro-insertion variants into signed 64-bit IDs
     *
     * @param chrom Chromosome string (1–22, X, Y, M)
     * @param start 1-based start position
     * @param ref Reference allele (uppercase, e.g. "A")
     * @param alt Alternate allele (uppercase, e.g. "T")
     * @return Encoded ID with MSB = 1 for SNV/deletion/micro-insertion, or null
     */
    public Long evaluate(String chrom, Long start, String ref, String alt) {
        if (alt == null ||  alt.length() > 2 || chrom == null || start == null || ref == null ) return null;

        int chromNum = parseChromosome(chrom);
        if (chromNum < 1 || chromNum > 25) return null;
        if (start < 1 || start > 999_000_000L) return null;

        int altCode = 0;
        int lengthCode = 0;

        int altLen = alt.length();
        int refLen = ref.length();

        if (altLen == 1 && refLen == 1) {
            // SNV
            altCode = baseCode(alt.charAt(0));
        } else if (altLen == 1 && refLen > 1) {
            // Deletion
            lengthCode = refLen;
        } else if (altLen == 2 && refLen == 1) {
            // Micro-insertion
            altCode = baseCode(alt.charAt(1));
            lengthCode = 1;
        } else {
            // Insertion or unhandled case
            return null;
        }

        if (altCode < 0 || altCode > 4) return null;

        long encoded = 0L;
        // Layout: | chrom (5) | start (30) | alt (3) | length (17) | = 55 bits (+ MSB)
        encoded |= ((long) chromNum) << (30 + 3 + 17);
        encoded |= (start << (3 + 17));
        encoded |= ((long) altCode) << 17;
        encoded |= lengthCode;

        return SNV_FLAG | encoded;
    }

    private int parseChromosome(String chrom) {
        switch (chrom.toUpperCase()) {
            case "X": return 23;
            case "Y": return 24;
            case "M":
            case "MT": return 25;
            default:
                try {
                    int n = Integer.parseInt(chrom);
                    return (n >= 1 && n <= 22) ? n : -1;
                } catch (NumberFormatException e) {
                    return -1;
                }
        }
    }

    private int baseCode(char base) {
        switch (base) {
            case 'A': return 1;
            case 'T': return 2;
            case 'C': return 3;
            case 'G': return 4;
            default: return -1;
        }
    }
}