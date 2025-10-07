package org.radiant;

public class Utils {
    public static int parseChromosome(String chrom) {
        switch (chrom.toUpperCase()) {
            case "X":
                return 23;
            case "Y":
                return 24;
            case "M":
            case "MT":
                return 25;
            default:
                try {
                    int n = Integer.parseInt(chrom);
                    return (n >= 1 && n <= 22) ? n : -1;
                } catch (NumberFormatException e) {
                    return -1;
                }
        }
    }
}
