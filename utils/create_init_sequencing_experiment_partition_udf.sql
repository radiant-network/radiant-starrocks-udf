CREATE
GLOBAL FUNCTION INIT_SEQUENCING_EXPERIMENT_PARTITION
(
    string
) RETURNS int
    PROPERTIES
(
    "symbol" = "org.radiant.InitSequencingExperimentPartitionUDF",
    "type" = "StarrocksJar",
    "file" = "http://host.docker.internal:8000/radiant-starrocks-udf-1.0.1-jar-with-dependencies.jar"
);

-- Note:
-- Change the hostname if starrocks is not running in a MacOS docker container.