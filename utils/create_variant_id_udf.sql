CREATE
GLOBAL FUNCTION GET_VARIANT_ID
(
    string,
    bigint,
    string,
    string
) RETURNS bigint
    PROPERTIES
(
    "symbol" =
    "org.radiant.VariantIdUDF",
    "type" =
    "StarrocksJar",
    "file" =
    "http://host.docker.internal:8000/radiant-starrocks-udf-1.0.1-jar-with-dependencies.jar"
);

-- Note:
-- Change the hostname if starrocks is not running in a MacOS docker container.