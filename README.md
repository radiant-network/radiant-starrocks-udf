# Radiant StarRocks Java UDFs

This project contains a suite of custom Java UDFs for [StarRocks](https://starrocks.io/), optimized for genomics, analytics, and high-performance transformation use cases.

---

# 🌟 Variant ID Encoding Function (`VariantIdUDF`)

## Overview

The **`VariantIdUDF`** is a **StarRocks user-defined function (UDF)** designed to generate **deterministic 63-bit integer identifiers** for genomic variants.  
It enables **high-performance joins and aggregations** by replacing complex string-based variant keys (e.g., `1-12345-A-T`) with compact integer IDs.

This UDF is optimized for **SNVs, all deletions, and micro-insertions (1 bp)** — together covering the vast majority of observed variants in large-scale genomic datasets (~95%).

---

## 💡 Motivation

In genomic databases, variants are typically represented using strings such as:

```
<chromosome>:<position>:<reference_allele>:<alternate_allele>
```

While easy to interpret, this representation is **inefficient** for analytical workloads:
- Strings consume more memory and disk space.
- String comparisons are slower than integer comparisons.
- Joins and aggregations scale poorly on large datasets.

To overcome these limitations, `VariantIdUDF` provides a **compact, deterministic, and sortable 63‑bit encoding** of variants, enabling:
- Fast numeric joins and filtering.
- Efficient storage (8 bytes per variant ID).
- Deterministic consistency across systems.

---

## ⚙️ How It Works

The `VariantIdUDF` packs variant information into a **63‑bit signed integer** using bitwise encoding.

| Bit Range | Field          | Description                                                                                                 |
|-----------|----------------|-------------------------------------------------------------------------------------------------------------|
| 0–24      | **Length**     | Encoded variant length using 25‑bit. Max length = 33,554,431 bp                                             |
| 25–27     | **Alt allele** | Same 3‑bit‑per‑base encoding. For insertions, only the first base (1 bp) is stored.                         |
| 28-57     | **Start**      | 30‑bit position within chromosome.                                                                          |
| 58–62     | **Chromosome** | Encodes `1`–`22`, `X`, `Y`, `M` using 5 bits.                                                               |
| 63        | **MSB Flag**   | Always set to `0` for this encoding; reserved to distinguish from other ID methods (e.g., large insertions). |

---

### 🧠 Why 63 Bits Instead of 64?

The **most significant bit (MSB)** is reserved as a **discriminator flag**.  
This bit is unused (`1`) in `VariantIdUDF` IDs, while other encoding methods (such as insertions > 1bp) will set it to `0`.

This design allows multiple encoding strategies to **coexist safely** within the same database column:
- `1xxxx…` → Standard small variant ID (`VariantIdUDF`)
- `0xxxx…` → Alternative or extended encoding (e.g., lookup table or long variant reference)

**Performance Consideration:**

We limit the encoding to **63 bits** because CPU with **64-bit architectures** can compare 64-bit integers using **a single CPU instruction**.
If the encoding exceeded 64 bits, comparisons would require multiple instructions, resulting in slower joins, sorts, and aggregations in the database.

---

### 🧩 Bit Layout Diagram

```
 63                                                           0
 +-----+---------+---------------------+---------+------------+
 | MSB |  CHROM  |      START          |  ALT    |  LENGTH    |
 +-----+---------+---------------------+---------+------------+
   1b      5b            30b               3b        25b
```

(REF and ALT bases are packed into the allele bits depending on the variant type.)

---

## 🧬 Supported Variant Types

| Variant Type               | Example                             | Supported? | Notes                                      |
|----------------------------|-------------------------------------|-------------|--------------------------------------------|
| **SNV**                    | `1-12345-A-T`                       | ✅ | Fully supported.                           |
| **Deletion**               | `1-12345-ATG-A`                     | ✅ | Any length deletion supported.             |
| **Micro‑Insertion (1 bp)** | `1-12345-A-AT`                      | ✅ | Single‑base insertion only.                |
| **Insertion >1 bp**        | `1-12345-A-ATG`                     | ❌ | Too large for encoding; handled by lookup. |
| **Others chromosome**      | Others cromosome than 1-22, X, Y, M | ❌ | Too large for encoding; handled by lookup. |

**Note:**  
Variants that are not supported by this encoding (e.g., insertions >1 bp or non-standard chromosomes) will result in a `NULL` return value.  
You can use this to detect unsupported variants and handle them via the lookup table or alternative encoding method.

---

## 📊 Coverage Estimation

✅ **≈ 95 % of all observed variants** can be represented directly with `VariantIdUDF`.

Variants exceeding encoding limits (e.g., multi‑base insertions) are managed through a **lookup table** while preserving deterministic IDs.

---

## 🧠 Determinism & Portability

`VariantIdUDF` guarantees that:
- The same input always yields the same 63‑bit integer.
- IDs are portable across databases and environments.
- Numeric ordering approximates genomic coordinate order.

This makes it ideal for:
- Cross‑dataset joins
- Deduplication
- Efficient partitioning and clustering keys

---

## 🚀 Usage

In StarRocks SQL, you can install udf with :
```sql
CREATE OR REPLACE
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
    "https://github.com/radiant-network/radiant-starrocks-udf/releases/download/v1.1.0/radiant-starrocks-udf-1.1.0-jar-with-dependencies.jar"
);
```

Then, use it as follows:
```sql
SELECT GET_VARIANT_ID(
  '1',     -- chromosome
  12345,      -- position
  'A',        -- reference allele
  'T'         -- alternate allele
) AS variant_int_id;
```

Example output:
```
variant_int_id
---------------
-8935138346800250880
```

Use it seamlessly in joins and filters:

```sql
SELECT *
FROM variants v
JOIN annotations a
  ON v.variant_int_id = a.variant_int_id;
```

Variants that are not supported by this encoding (e.g., insertions > 1 bp or non-standard alleles) will return null :

```sql
SELECT GET_VARIANT_ID(
  '1',     -- chromosome
  12345,      -- position
  'A',        -- reference allele
  'ATCG'         -- alternate allele - insertion larger than 1bp
) AS variant_int_id;
```

Result :
```
variant_int_id
---------------
NULL
```
---

## 🧩 Integration with Lookup Table

Variants not directly encodable (e.g., long insertions or complex events) are stored in a **lookup table** that maps textual variant keys to extended integer IDs (MSB = 0):

| variant_key | variant_int_id |
|--------------|----------------|
| `5-179283942-G-GATT` | `675750` |

This allows both UDF‑encoded and lookup‑encoded IDs to coexist in the same schema.

---

## 🛠️ Performance Benefits

| Operation | String Key | Encoded ID                   |
|------------|-------------|------------------------------|
| Join | Slow (string compare) | **Fast (integer compare)**   |
| Index Size | Large | **Compact (8 bytes)**        |
| Group By | High CPU | **Efficient (integer hash)** |
| Storage | KBs per row | **Bytes per row**            |


