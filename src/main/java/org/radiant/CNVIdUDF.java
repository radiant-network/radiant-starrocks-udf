package org.radiant;

public class CNVIdUDF {

    /**
     * StarRocks UDF-compatible method
     * Encodes CNV variants into signed 64-bit IDs
     *
     * @param chrom  Chromosome string (1–22, X, Y, M)
     * @param start  1-based start position (≤ 999,000,000)
     * @param length length of the CNV
     * @param alt    Alternate allele (e.g. "<DEL>" or "<DUP>")
     * @return Encoded ID (MSB = 1 for LOSS, 0 for GAIN), or null
     */
    public Long evaluate(String chrom, Long start, Long length, String alt) {

        if (alt == null || chrom == null || start == null || length == null) return null;

        int chromNum = Utils.parseChromosome(chrom);
        if (chromNum < 1 || chromNum > 25) return null;
        if (start < 1 || start > 999_000_000L) return null;

        int isGain;
        if (alt.equals("<DEL>")) {
            isGain = 1;
        } else if (alt.equals("<DUP>")) {
            isGain = 0;
        } else {
            return null; // unsupported alt
        }


        long encoded = 0L;
        // Bit layout: Type(1) | chrom (5) | start (30) | length (28) = 64 bits
        encoded |= ((long) isGain) << 63;
        encoded |= ((long) chromNum) << (30 + 28);
        encoded |= start << 28;
        encoded |= length;

        return encoded;
    }

}