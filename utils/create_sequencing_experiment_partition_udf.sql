CREATE
GLOBAL FUNCTION GET_SEQ_EXP_PARTITION
(
    int,
    int
) RETURNS int
    PROPERTIES
(
    "symbol" =
    "org.radiant.SequencingExperimentPartitionUDF",
    "type" =
    "StarrocksJar",
    "file" =
    "http://host.docker.internal:8000/radiant-starrocks-udf-1.0.1-jar-with-dependencies.jar"
);

-- Note:
-- Change the hostname if starrocks is not running in a MacOS docker container.