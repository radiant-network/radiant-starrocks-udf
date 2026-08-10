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

| Variant Type               | Example                             | Supported? | Notes                                       |
|----------------------------|-------------------------------------|------------|---------------------------------------------|
| **SNV**                    | `1-12345-A-T`                       | ✅          | Fully supported.                            |
| **Deletion**               | `1-12345-ATG-A`                     | ✅          | Any length deletion supported.              |
| **Micro‑Insertion (1 bp)** | `1-12345-A-AT`                      | ✅          | Single‑base insertion only.                 |
| **Insertion >1 bp**        | `1-12345-A-ATG`                     | ❌          | Too large for encoding; handled by lookup.  |
| **Others chromosome**      | Others cromosome than 1-22, X, Y, M | ❌          | Too large for encoding; handled by lookup.  |
| **Indel**                  | `G` -> `TT`                         | ❌          | Special cases.                              | 

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

---

# 🧬 CNV ID Encoding Function (`CNVIdUDF`)

## Overview

The **`CNVIdUDF`** generates **deterministic 64-bit integer identifiers** for copy-number variant
segments, playing the same role for CNV that `VariantIdUDF` plays for small variants.

A CNV segment is identified by its **chromosome, start, length and type** — there is no ref/alt pair
to encode, and the segment can span an entire chromosome arm.

---

## ⚙️ How It Works

| Bit Range | Field          | Description                                                                        |
|-----------|----------------|------------------------------------------------------------------------------------|
| 0–27      | **Length**     | 28‑bit segment length. Max = 268,435,455 bp                                        |
| 28–55     | **Start**      | 28‑bit position within chromosome. Max = 268,435,455                               |
| 56–60     | **Chromosome** | Encodes `1`–`22`, `X`, `Y`, `M` using 5 bits.                                       |
| 61–63     | **Type**       | 3‑bit CNV type. Values `0`–`3` are assigned; `4`–`7` are reserved for future types. |

### 🧩 Bit Layout Diagram

```
 63          61 60      56 55                     28 27                      0
 +-------------+----------+-------------------------+------------------------+
 |    TYPE     |  CHROM   |          START          |        LENGTH          |
 +-------------+----------+-------------------------+------------------------+
       3b           5b               28b                      28b
```

### 🧠 Why 28 Bits for Both Coordinates?

`start` and `length` are bounded by the same biological constant — the largest human chromosome,
chr1 at 248,956,422 bp — so 28 bits (268,435,455) is the natural size for each, with ~7.8 % headroom
against a value that does not grow.

The other fields have no slack to give:

- **`chromosome` cannot shrink** — 25 values are needed (`1`–`22`, `X`, `Y`, `M`); 4 bits gives 16.
- **`length` cannot shrink.** At 27 bits the ceiling is 134,217,727 (~134 Mb), which truncates
  whole-chromosome events on chr1–9, chr11 and chrX, plus arm-level events on chr2 q and chr4 q —
  precisely the aneuploidy and arm-level events somatic CNV is full of. The boundary also falls
  mid-karyotype (chr11 overflows while chr10 and chr12 fit), so such a bug would pass or fail
  depending on which chromosome was tested.

---

## 🧬 Supported CNV Types

The type field is keyed on the **resolved CNV type**, not on the VCF alternate allele. DRAGEN 4.2
spells an LOH event as multi-allelic `<DEL>,<DUP>` while 4.4 spells it `<LOH>` — an ALT-keyed ID
would give the same biological segment two different IDs depending on the caller version.

| `type`    | Code | Typical ALT              | Notes                            |
|-----------|------|--------------------------|----------------------------------|
| `LOSS`    | `0`  | `<DEL>`                  | Copy-number loss.                |
| `GAIN`    | `1`  | `<DUP>`                  | Copy-number gain.                |
| `CNLOH`   | `2`  | `<LOH>` / `<DEL>,<DUP>`  | Copy-neutral loss of heterozygosity. |
| `GAINLOH` | `3`  | `<LOH>` / `<DEL>,<DUP>`  | Gain with loss of heterozygosity. |

Matching is **case-sensitive** and expects the upper-case form.

### ✳️ Coexistence with `VariantIdUDF`

Because only codes `0`–`3` are assigned, **bit 63 stays clear and every CNV ID is positive**.
`VariantIdUDF` always sets bit 63, so its IDs are negative. The two encodings are therefore disjoint
and can safely share a column. Only a future type in the `4`–`7` range would set the sign bit.

---

## 🚀 Usage

In StarRocks SQL, you can install the udf with:
```sql
CREATE OR REPLACE
    GLOBAL FUNCTION GET_CNV_ID
(
    string,
    bigint,
    bigint,
    string
) RETURNS bigint
    PROPERTIES
(
    "symbol" =
    "org.radiant.CNVIdUDF",
    "type" =
    "StarrocksJar",
    "file" =
    "https://github.com/radiant-network/radiant-starrocks-udf/releases/download/v1.3.0/radiant-starrocks-udf-1.3.0-jar-with-dependencies.jar"
);
```

Then, use it as follows:
```sql
SELECT GET_CNV_ID(
  '1',        -- chromosome
  1000,       -- start position
  500,        -- length
  'GAIN'      -- CNV type
) AS cnv_id;
```

Example output:
```
cnv_id
---------------------
2377900871687078388
```

Segments whose type is not recognised, or whose coordinates fall outside the encodable range, return
null:

```sql
SELECT GET_CNV_ID('1', 1000, 500, 'UNKNOWN') AS cnv_id;
```

Result :
```
cnv_id
---------------
NULL
```

**Handle nulls upstream.** `cnv_id` is typically declared `bigint NOT NULL` and used in a
`DUPLICATE KEY`, so a null does not degrade to an "unknown" row — it fails the load. Callers should
skip records whose type cannot be resolved rather than rely on the UDF to absorb them.

---

## ⚠️ Breaking Change in v1.3.0

Prior to `v1.3.0` the type field was a **single bit** derived from the **alternate allele**
(`<DEL>` → 1, `<DUP>` → 0), with `start` occupying 30 bits:

```
encoded = (isLoss << 63) | (chrom << 58) | (start << 28) | length
```

`v1.3.0` widens the type field to 3 bits, taking the 2 extra bits from `start`, and switches the
4th argument from `alternate` to `type`. Consequences:

- **IDs produced by ≤ `v1.2.1` are not comparable to `v1.3.0` IDs.** Every existing `cnv_id` changes,
  from both the layout move and the sign convention. Stored or bookmarked IDs go stale and need a
  backfill.
- **`<DEL>` / `<DUP>` are no longer accepted** and return null. The SQL argument list is unchanged
  (`string, bigint, bigint, string`), so call sites must be updated to pass `type` in the 4th slot.
- **`start` is now bounded at 268,435,455**, down from the previous 999,000,000 guard.
- **`length` is now range-checked.** Earlier versions had no upper bound, so a length ≥ 2²⁸ silently
  overflowed into the `start` field and produced a wrong, colliding ID.


