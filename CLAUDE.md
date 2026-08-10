# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Custom Java UDFs for [StarRocks](https://starrocks.io/), focused on genomics data transformation. Encodes genomic variants into 64-bit signed integers using bit-packing for fast joins/aggregations vs. string keys like `1-12345-A-T`.

UDFs:
- `VariantIdUDF` — SNV, deletion, micro-insertion (1 bp)
- `CNVIdUDF` — copy-number variant segments, keyed on CNV type (`LOSS` / `GAIN` / `CNLOH` / `GAINLOH`)
- `Utils.parseChromosome` — shared chromosome-string → int (1–25)

## Build Commands

```bash
mvn test                    # Run all tests
mvn package                 # Build fat JAR (includes tests)
mvn package -DskipTests     # Build fat JAR without tests
mvn clean test              # Clean build + test
```

Output JAR: `target/radiant-starrocks-udf-<version>-jar-with-dependencies.jar`

## Architecture

- **Java 17**, Maven build, no runtime dependencies (standalone fat JAR for StarRocks)
- **Single UDF pattern**: each UDF class in `src/main/java/org/radiant/` exposes `public Long evaluate(...)` — StarRocks calls directly
- **Tests**: JUnit 5 in `src/test/java/org/radiant/`, one test class per UDF
- All UDFs return `null` on invalid/unsupported input — StarRocks treats null as "fall back to lookup table"
- Chromosomes (`Utils.parseChromosome`): 1–22 numeric, X→23, Y→24, M/MT→25; else `-1`
- Position bound: `start ∈ [1, 999_000_000]` in `VariantIdUDF` (30-bit field), `[1, 268_435_455]` in `CNVIdUDF` (28-bit field)

### VariantIdUDF — 64-bit layout

```
| MSB=1 | chrom (5) | start (30) | alt (3) | length (25) |
```

- MSB=1 distinguishes UDF-encoded IDs from lookup-table IDs (MSB=0) sharing same column
- `alt`: A=1, T=2, C=3, G=4 (SNV/micro-insertion only); 0 for deletions
- `length`: deletion ref-length or 1 for micro-insertion; 0 for SNV; max 33,554,431 (25 bits)
- Rejects: insertions >1 bp, MNVs, unknown bases, indel

### CNVIdUDF — 64-bit layout

```
| type (3) | chrom (5) | start (28) | length (28) |
```

- `type`: LOSS=0, GAIN=1, CNLOH=2, GAINLOH=3; codes 4–7 reserved. Case-sensitive, upper case only
- Keyed on the **resolved CNV type**, not the VCF `alternate` — DRAGEN 4.2 spells LOH as `<DEL>,<DUP>`
  and 4.4 as `<LOH>`, so an ALT-keyed ID would differ per caller version for the same segment
- Codes 0–3 leave bit 63 clear, so **every CNV ID is positive** while all `VariantIdUDF` IDs are
  negative (MSB=1) — the two encodings can share a column without colliding
- `start` and `length` both bounded at 268,435,455 (28 bits), sized to the largest human chromosome
- Changed in v1.3.0 (SJRA-1777): was `| type (1) | chrom (5) | start (30) | length (28) |` keyed on
  `<DEL>`/`<DUP>`. IDs from ≤ v1.2.1 are not comparable

## CI/CD

- **PRs to main**: `test.yml` runs `mvn test` + builds the fat JAR
- **Tags `v*`**: `deploy.yml` sets POM version from tag, tests, builds, deploys to GitHub Packages, and creates a GitHub Release with the fat JAR attached

## Releasing

```bash
git tag v1.0.1
git push origin v1.0.1    # Triggers deploy workflow
```
