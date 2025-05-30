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

Computes the partition ID for sequencing experiments based on sequencing type and current partition count.

**Signature:**

```java
Integer evaluate(String sequencingType, Integer currentPartitionId, Integer partitionCount)
```