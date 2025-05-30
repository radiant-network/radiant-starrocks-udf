package org.radiant;

public class SequencingExperimentPartitionUDF {

    private static final short WGS_TYPE_ID = 1;
    private static final short WXS_TYPE_ID = 2;

    private static final java.util.Map<String, Short> SEQUENCING_TYPE_ALIAS_MAP = java.util.Map.of(
            "wgs", WGS_TYPE_ID,
            "wxs", WXS_TYPE_ID
    );

    private static final java.util.Map<Short, Integer> SEQUENCING_TYPE_LIMIT_MAP = java.util.Map.of(
            WGS_TYPE_ID, 100,
            WXS_TYPE_ID, 1000
    );

    public Integer evaluate(String sequencingType, Integer currentPartitionId, Integer currentPartitionCount) {
        // Validate inputs
        if (sequencingType == null || currentPartitionId == null || currentPartitionCount == null) { return null; }

        // Validate sequencing type
        Short sequencingTypeId = SEQUENCING_TYPE_ALIAS_MAP.get(sequencingType.toLowerCase());
        if (sequencingTypeId == null) { return null; }
        if ((currentPartitionId >> 16) != sequencingTypeId) { return null; }

        if (currentPartitionCount >= SEQUENCING_TYPE_LIMIT_MAP.get(sequencingTypeId)) {
            short body = (short) ((currentPartitionId & 0xFFFF) + 1);
            return (currentPartitionId & 0xFFFF0000) | body;
        }
        return currentPartitionId;
    }
}
