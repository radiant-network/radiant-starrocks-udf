package org.radiant;

public class GetSequencingExperimentPartitionUDF {

    private static final short WGS_TYPE_ID = 0;
    private static final short WXS_TYPE_ID = 1;

    private static final java.util.Map<Short, Integer> SEQUENCING_TYPE_LIMIT_MAP = java.util.Map.of(
            WGS_TYPE_ID, 100,
            WXS_TYPE_ID, 1000
    );

    public Integer evaluate(Integer currentPartitionId, Integer currentPartitionCount) {
        // Validate inputs
        if (currentPartitionId == null || currentPartitionCount == null) { return null; }

        // Validate sequencing type
        Integer sequencingTypeLimit = SEQUENCING_TYPE_LIMIT_MAP.get((short) (currentPartitionId >> 16));
        if (sequencingTypeLimit == null) { return null; }

        if (currentPartitionCount >= sequencingTypeLimit) {
            // Sanity check for max range
            if ((currentPartitionId & 0xFFFF) == 0xFFFF) { return null; }

            return (currentPartitionId & 0xFFFF0000) | ((currentPartitionId & 0xFFFF) + 1);
        }
        return currentPartitionId;
    }
}
