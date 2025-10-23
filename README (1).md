# Clinical Trial Information Retrieval System

A Java-based project for **indexing, searching, and evaluating clinical trial documents**. It combines **traditional IR** (lexical/keyword) with **semantic, embedding-based** techniques to improve biomedical text search and ranking.

---

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Components](#components)
- [Data Flow](#data-flow)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Build & Run](#build--run)
  - [Option A: Plain `javac` + local libs](#option-a-plain-javac--local-libs)
  - [Option B: Maven (recommended)](#option-b-maven-recommended)
- [Usage Examples](#usage-examples)
  - [Indexing](#indexing)
  - [Searching (lexical)](#searching-lexical)
  - [Searching (embeddings / rescoring)](#searching-embeddings--rescoring)
  - [Batch Search](#batch-search)
  - [Evaluation](#evaluation)
- [Configuration](#configuration)
- [Input/Output Formats](#inputoutput-formats)
- [Metrics](#metrics)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## Overview
This project implements a **Java-based information retrieval system** focused on clinical trial data. It supports:
- **Parsing and preprocessing** of trial documents
- **Inverted-indexing** for fast retrieval
- **Multiple ranking strategies** (lexical baseline, semantic embeddings, rescoring)
- **Batch and multi-index querying**
- **Evaluation** with standard IR metrics

The design targets **reproducibility**, **extensibility**, and **clear separation of concerns** between parsing, indexing, searching, and evaluation.

---

## Architecture
| Layer | Responsibility | Notes |
|------:|----------------|------|
| Data parsing | Read and normalize clinical trial fields (id, title, abstract, criteria, etc.) | `ClinicalTrialParser` |
| Indexing | Build inverted index, store key fields, analyze text | `ClinicalTrialIndexer*` |
| Search | Execute queries against one or multiple indices | `ClinicalTrial*Searcher` |
| Semantics | Embedding-based similarity, hybrid rescoring | `ClinicalTrialEmbeddingSearcher`, `ClinicalTrialRescoreSearcher` |
| Evaluation | Compute IR metrics over run files | `Evaluation` |

---

## Components
| Class/File | Role |
|-----------|------|
| `ClinicalTrial.java` | Data model for a clinical trial record. |
| `ClinicalTrialParser.java` | Loads raw data, normalizes fields, produces `ClinicalTrial` objects. |
| `ClinicalTrialIndexer3.java` | Indexes trials into an inverted index (e.g., Lucene). |
| `ClinicalTrialIndexReader.java` | Reads the index and exposes low-level search APIs. |
| `ClinicalTrialMultiSearcher.java` | Executes queries across multiple indices and merges results. |
| `ClinicalTrialUnifiedSearcher.java` | High-level interface that coordinates lexical/semantic search. |
| `ClinicalTrialRescoreSearcher.java` | Re-ranks lexical results using embedding-based similarity. |
| `ClinicalTrialFinalSearcher.java` | Production-ready search entry-point (final/hybrid pipeline). |
| `BatchSearcher.java` | Runs many queries from a file and writes a run file. |
| `Evaluation.java` | Computes metrics (e.g., Precision@K, MAP, nDCG) from run files. |

> Exact class names may vary depending on your branch; adapt commands accordingly.

---

## Data Flow
```
Raw JSON/CSV/XML --> Parser --> ClinicalTrial objects
      --> Indexer (text analysis, field selection) --> Inverted Index
      --> Searcher (lexical) --> Candidate set
      --> Embedding Similarity / Rescoring (optional) --> Final ranked list
      --> Evaluation (qrels + runs) --> Metrics
```

---

## Project Structure
```
├── src/
│   ├── ClinicalTrial.java
│   ├── ClinicalTrialParser.java
│   ├── ClinicalTrialIndexer3.java
│   ├── ClinicalTrialIndexReader.java
│   ├── ClinicalTrialMultiSearcher.java
│   ├── ClinicalTrialUnifiedSearcher.java
│   ├── ClinicalTrialRescoreSearcher.java
│   ├── ClinicalTrialFinalSearcher.java
│   ├── BatchSearcher.java
│   └── Evaluation.java
├── data/
│   ├── trials/               # Raw clinical trial documents (JSON/CSV/XML)
│   ├── queries.txt           # One query per line
│   └── qrels.txt             # Relevance judgements (query-id doc-id rel)
├── index/                    # Lucene indices will be written here
├── runs/                     # Output run files for evaluation
├── lib/                      # External jars (e.g., lucene-core, lucene-queryparser, embeddings)
└── README.md
```

---

## Requirements
- **Java 17+** (recommended) or 11+
- **Apache Lucene** (or similar IR library) jars on the classpath
- (Optional) **Maven** or **Gradle** for dependency management
- For embedding-based search, a sentence/word embedding library or exported vectors

---

## Build & Run

### Option A: Plain `javac` + local libs
Assuming sources in `src/` and jars under `lib/`:
```bash
# Compile
mkdir -p bin
javac -cp "lib/*" -d bin src/*.java

# Package (optional)
jar --create --file clinical-ir.jar -C bin .
```

Run examples:
```bash
# Index
java -cp "bin:lib/*" ClinicalTrialIndexer3 data/trials index/

# Lexical search
java -cp "bin:lib/*" ClinicalTrialFinalSearcher index/ "glioblastoma multiforme"

# Batch search
java -cp "bin:lib/*" BatchSearcher index/ data/queries.txt runs/lexical.tsv

# Evaluation
java -cp "bin:lib/*" Evaluation data/qrels.txt runs/lexical.tsv
```
On Windows, replace the classpath separator `:` with `;`:
```powershell
java -cp "bin;lib/*" ClinicalTrialFinalSearcher index/ "glioblastoma multiforme"
```

### Option B: Maven (recommended)
Create a `pom.xml` and declare dependencies (example for Lucene 9.x):
```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.example</groupId>
  <artifactId>clinical-ir</artifactId>
  <version>1.0.0</version>
  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.apache.lucene</groupId>
      <artifactId>lucene-core</artifactId>
      <version>9.10.0</version>
    </dependency>
    <dependency>
      <groupId>org.apache.lucene</groupId>
      <artifactId>lucene-queryparser</artifactId>
      <version>9.10.0</version>
    </dependency>
    <!-- Add embedding / NLP libraries if needed -->
  </dependencies>
</project>
```
Build and run:
```bash
mvn -q clean package
java -cp "target/clinical-ir-1.0.0.jar:~/.m2/repository/*" ClinicalTrialIndexer3 data/trials index/
```

---

## Usage Examples

### Indexing
```bash
java -cp "bin:lib/*" ClinicalTrialIndexer3 data/trials index/
```
**Arguments**
| Arg | Description | Example |
|----:|-------------|---------|
| `inputPath` | Folder with raw trials | `data/trials` |
| `indexPath` | Folder to create the index | `index/` |

### Searching (lexical)
```bash
java -cp "bin:lib/*" ClinicalTrialFinalSearcher index/ "pancreatic cancer immunotherapy"
```
**Arguments**
| Arg | Description |
|----:|-------------|
| `indexPath` | Index directory |
| `query` | Free text query |

### Searching (embeddings / rescoring)
```bash
java -cp "bin:lib/*" ClinicalTrialRescoreSearcher index/ "triple-negative breast cancer"
```
This first retrieves lexical candidates, then **re-scores** them using an embedding similarity function.

### Batch Search
```bash
java -cp "bin:lib/*" BatchSearcher index/ data/queries.txt runs/hybrid.tsv
```
**queries.txt** contains one query per line; output is a tab-separated **run file**.

### Evaluation
```bash
java -cp "bin:lib/*" Evaluation data/qrels.txt runs/hybrid.tsv
```

---

## Configuration
Common options (via flags or properties file):
| Option | Purpose | Default |
|------:|---------|--------|
| Analyzer | Text analyzer for indexing/search | StandardAnalyzer |
| Fields | Indexed fields (title, abstract, criteria) | title, abstract |
| TopK | Number of results to return | 100 |
| Embed model | Path to embeddings / encoder | `null` (disabled) |
| Rescore weight | Blend lexical vs semantic score | 0.5 |

---

## Input/Output Formats

**Trials (input)**: JSON/CSV/XML with fields like `id`, `title`, `abstract`, `criteria`.  
**Run file (output)**: a TSV with columns:
```
query_id    doc_id    rank    score    method
```

**Qrels (ground truth)**: a TSV with columns:
```
query_id    doc_id    relevance
```

---

## Metrics
| Metric | Meaning |
|------:|---------|
| Precision@K | Fraction of top-K results that are relevant |
| Recall@K | Fraction of relevant documents retrieved within top-K |
| MAP | Mean Average Precision across queries |
| nDCG | Rank-aware gain normalized by ideal ranking |

---

## Troubleshooting
- **`ClassNotFoundException`**: check the classpath (`-cp "bin:lib/*"`); on Windows use `;` instead of `:`.  
- **Empty results**: verify the analyzer and indexed fields; ensure index was created (`index/` not empty).  
- **Encoding issues**: ensure UTF-8 input; set `-Dfile.encoding=UTF-8`.  
- **Maven deps not found**: run `mvn -q -U clean package` to force dependency resolution.

---

## License
Educational/academic use. Adapt as needed for your coursework or research.
