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
        if (chrom == null || start == null || ref == null || alt == null) return null;

        int chromNum = parseChromosome(chrom);
        if (chromNum < 1 || chromNum > 25) return null;
        if (start < 1 || start > 9_999_999) return null;

        int altCode = 0;
        int lengthCode = 0;

        int refLen = ref.length();
        int altLen = alt.length();

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
        encoded |= ((long) chromNum) << (24 + 3 + 17);
        encoded |= (start << (3 + 17));
        encoded |= ((long) altCode) << 17;
        encoded |= lengthCode;

        return SNV_FLAG | encoded;
    }

    private int parseChromosome(String chrom) {
        switch (chrom.toUpperCase()) {
            case "X": return 23;
            case "Y": return 24;
            case "M": case "MT": return 25; // Mitochondrial fallback (if needed)
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