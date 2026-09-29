#!/usr/bin/env python3
"""Dev-only: extract MASSIVE date-slot utterances (fr-FR, en-US).

Not a Spring Boot dependency. Download MASSIVE 1.1 first:

  curl -L -o amazon-massive-dataset-1.1.tar.gz \\
    https://amazon-massive-nlu-dataset.s3.amazonaws.com/amazon-massive-dataset-1.1.tar.gz
  tar -xzf amazon-massive-dataset-1.1.tar.gz --wildcards '*/fr-FR.jsonl' '*/en-US.jsonl'

Then:

  python3 extract_massive_date_slots.py
"""

from __future__ import annotations

import argparse
import collections
import json
import re
from pathlib import Path

DATE_RE = re.compile(r"\[date\s*:\s*([^\]]+)\]", re.I)


def extract(path: Path, locale: str) -> list[dict]:
    rows: list[dict] = []
    with path.open(encoding="utf-8") as handle:
        for line in handle:
            obj = json.loads(line)
            annot = obj.get("annot_utt") or ""
            slots = [s.strip() for s in DATE_RE.findall(annot)]
            if not slots:
                continue
            rows.append(
                {
                    "id": obj.get("id"),
                    "locale": locale,
                    "utt": obj.get("utt"),
                    "annot_utt": annot,
                    "date_slots": slots,
                    "intent": obj.get("intent"),
                    "scenario": obj.get("scenario"),
                    "partition": obj.get("partition"),
                }
            )
    return rows


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--data-dir",
        type=Path,
        default=Path(__file__).resolve().parent / "1.1" / "data",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=Path(__file__).resolve().parent / "candidates",
    )
    args = parser.parse_args()

    fr = extract(args.data_dir / "fr-FR.jsonl", "fr-FR")
    en = extract(args.data_dir / "en-US.jsonl", "en-US")
    print(f"fr-FR date-slot utterances: {len(fr)}")
    print(f"en-US date-slot utterances: {len(en)}")

    fr_vals: collections.Counter[str] = collections.Counter()
    for row in fr:
        for slot in row["date_slots"]:
            fr_vals[slot.lower()] += 1
    en_vals: collections.Counter[str] = collections.Counter()
    for row in en:
        for slot in row["date_slots"]:
            en_vals[slot.lower()] += 1

    seen: set[tuple] = set()
    candidates: list[dict] = []
    for row in fr + en:
        key = (
            row["locale"],
            " | ".join(row["date_slots"]).lower(),
            (row["utt"] or "").lower(),
        )
        if key in seen:
            continue
        seen.add(key)
        candidates.append(row)

    args.out_dir.mkdir(parents=True, exist_ok=True)
    out_jsonl = args.out_dir / "date_slot_candidates.jsonl"
    with out_jsonl.open("w", encoding="utf-8") as handle:
        for row in candidates:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")

    out_expr = args.out_dir / "unique_date_expressions.txt"
    with out_expr.open("w", encoding="utf-8") as handle:
        for value, count in sorted(fr_vals.items(), key=lambda item: (-item[1], item[0])):
            handle.write(f"fr\t{count}\t{value}\n")
        for value, count in sorted(en_vals.items(), key=lambda item: (-item[1], item[0])):
            handle.write(f"en\t{count}\t{value}\n")

    meta = {
        "source": "Amazon MASSIVE 1.1",
        "download": "https://amazon-massive-nlu-dataset.s3.amazonaws.com/amazon-massive-dataset-1.1.tar.gz",
        "locales": ["fr-FR", "en-US"],
        "fr_date_slot_utterances": len(fr),
        "en_date_slot_utterances": len(en),
        "deduped_candidates": len(candidates),
        "unique_fr_expressions": len(fr_vals),
        "unique_en_expressions": len(en_vals),
    }
    (args.out_dir / "extraction_meta.json").write_text(
        json.dumps(meta, indent=2) + "\n", encoding="utf-8"
    )
    print(f"Deduped candidates: {len(candidates)}")
    print(f"Wrote {out_jsonl}")
    print(f"Wrote {out_expr}")


if __name__ == "__main__":
    main()
