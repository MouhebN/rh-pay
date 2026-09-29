# Leave-date robustness evaluation (MASSIVE)

Evaluation / data-preparation tooling only. **Not** a Spring Boot runtime dependency.

## Purpose

Measure whether the deterministic leave-date parser (~95.7% on the internal 46-case regression) generalizes beyond those phrases.

Production parsers (`JavaLeaveDateExtractor`, `TextNormalize`, `LeaveDraftSlotFiller`, …) must **not** be changed based on this step alone.

## Source

- Dataset: [Amazon MASSIVE 1.1](https://github.com/alexa/massive)
- Download: `https://amazon-massive-nlu-dataset.s3.amazonaws.com/amazon-massive-dataset-1.1.tar.gz`
- Locales used: `fr-FR` (primary), `en-US` (English temporal vocabulary for mixed FR/EN)
- We ignore MASSIVE intents/scenarios; only `date` slot text is reused as temporal language.

## Reference date

All expected relative dates use fixed clock:

`2026-09-16` (Wednesday)

## How candidates were extracted

```bash
cd tools/leave-date-eval
curl -L -o amazon-massive-dataset-1.1.tar.gz \
  https://amazon-massive-nlu-dataset.s3.amazonaws.com/amazon-massive-dataset-1.1.tar.gz
tar -xzf amazon-massive-dataset-1.1.tar.gz --wildcards '*/fr-FR.jsonl' '*/en-US.jsonl'
python3 extract_massive_date_slots.py
```

Filter: utterances whose `annot_utt` contains one or more `[date : …]` slots.
Deduplicated by `(locale, date_slots, utt)`.

Approximate counts from MASSIVE 1.1:

| Locale | Date-slot utterances | Deduped (both locales) |
|--------|----------------------|-------------------------|
| fr-FR  | ~2515                | ~4950 combined          |
| en-US  | ~2523                |                         |

Unique date expressions: see `candidates/unique_date_expressions.txt` (regenerated locally; large raw JSONL is gitignored).

## How HR cases were built

1. Prefer leave-relevant temporal concepts (today/tomorrow/weekdays/ranges/weekends/relative phrases).
2. Drop time-of-day-only wording and phrases that cannot be labeled confidently for leave dates.
3. Adapt retained temporal expressions into short HR leave utterances (e.g. `vendredi` → `je voudrais poser vendredi`).
4. Add a small number of synthetic typo and mixed FR/EN variants for chatbot realism.
5. Ambiguous phrases (`cette semaine`, `next week`, …) are labeled as clarification (no concrete dates).

Rebuild curated JSON:

```bash
python3 build_hr_cases.py
```

Copies into:

- `tools/leave-date-eval/cases/`
- `backend/src/test/resources/leave-date-eval/`

## Split

| Set | Approx size | Policy |
|-----|-------------|--------|
| Existing regression | 46 | Frozen internal suite |
| DEV | ~100 | Inspectable during later work |
| HOLDOUT | ~50 | Generalization estimate only — **do not tune** production on individual holdout failures |

Java entry points:

- `LeaveDateRobustnessDevCases`
- `LeaveDateRobustnessHoldoutCases`
- `LeaveDateRobustnessEvalTest`

## What is committed vs local-only

Committed:

- extraction/build scripts
- this README
- curated DEV/HOLDOUT JSON under `backend/src/test/resources/leave-date-eval/`
- compact `cases/` copies under this folder

Gitignored / local-only:

- `amazon-massive-dataset-1.1.tar.gz`
- `1.1/data/*.jsonl`
- full `candidates/date_slot_candidates.jsonl` (optional local artifact)

## Run evaluation

```bash
cd backend
mvn -Dtest=LeaveDateUnderstandingBaselineTest,LeaveDateRobustnessEvalTest test
cat target/leave-date-robustness-summary.txt
```
