# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Custom Java UDFs for [StarRocks](https://starrocks.io/), focused on genomics data transformation. Currently contains `VariantIdUDF`, which encodes genomic variants (SNV, deletion, micro-insertion) into 64-bit signed integers using bit-packing.

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
- **Single UDF pattern**: Each UDF class lives in `src/main/java/org/radiant/` and exposes a `public Long evaluate(...)` method that StarRocks calls directly
- **Tests**: JUnit 5 in `src/test/java/org/radiant/`, one test class per UDF

### VariantIdUDF Encoding (63 useful bits + MSB flag)

```
| MSB=1 | chrom (5 bits) | start (30 bits) | alt (3 bits) | length (25 bits) |
```

- Chromosomes: 1-22 numeric, X→23, Y→24, M/MT→25
- Returns `null` for any invalid or unsupported input (insertions >1bp, MNVs)

## CI/CD

- **PRs to main**: `test.yml` runs `mvn test` + builds the fat JAR
- **Tags `v*`**: `deploy.yml` sets POM version from tag, tests, builds, deploys to GitHub Packages, and creates a GitHub Release with the fat JAR attached

## Releasing

```bash
git tag v1.0.1
git push origin v1.0.1    # Triggers deploy workflow
```
