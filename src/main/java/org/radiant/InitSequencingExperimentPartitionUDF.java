package org.radiant;

public class InitSequencingExperimentPartitionUDF {

    public Integer evaluate(String experimentalStrategy) {
        if (experimentalStrategy == null || experimentalStrategy.isEmpty()) {
            return null;
        }

        switch (experimentalStrategy.toLowerCase()) {
            case "wgs":
                return 0x00000000; // Initialize WGS partition ID
            case "wxs":
                return 0x00010000; // Initialize WXS partition ID
            default:
                return null; // Unsupported analysis type

        }
    }

}
