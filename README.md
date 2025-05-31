# Radiant StarRocks Java UDFs

This project contains a suite of custom Java UDFs for [StarRocks](https://starrocks.io/), optimized for genomics, analytics, and high-performance transformation use cases.

---

## ✨ Included UDFs

### ✅ `VariantIdUDF`

Encodes genomic variants (SNV, deletion, micro-insertion) into 64-bit signed integers.

**Signature:**

```java
Long evaluate(String chrom, Long start, String ref, String alt)
```

###  ✅ `SequencingExperimentPartitionUDF`

Computes the partition ID for sequencing experiments based on the current partition (containing the type mask) and current partition count.

**Signature:**

```java
Integer evaluate(Integer currentPartitionId, Integer partitionCount)
```


# Local Development

The `utils` directory contains a python script to serve the UDFs JAR locally for testing.

First, copy the JAR file to the `utils` directory.

Then, from the `utils` directly, launch the `python http_server.py` script and it will serve the JAR at `http://localhost:8000/radiant-starrocks-udf-{version}-jar-with-dependencies.jar`.

> **Note:**
> Use `http://host.docker.internal:8000/radiant-starrocks-udf-{version}-jar-with-dependencies.jar` in Docker containers to access the local server.

Also included are `.sql` files to help create the UDFs in StarRocks.
