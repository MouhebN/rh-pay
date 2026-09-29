#!/usr/bin/env python3
"""Merge Avature HR intents into RH Paie dataset.csv (French).

Source: intent-classification-main (Avature test set, English).
We map only HR-procedure-like intents to FAQ_*; recruiting/small-talk → OUT_OF_SCOPE.
English utterances are translated to French (MyMemory) and cached.
"""

from __future__ import annotations

import csv
import json
import re
import time
from collections import Counter
from pathlib import Path
import random

from deep_translator import MyMemoryTranslator

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parent.parent
AVATURE = REPO / "intent-classification-main" / "test_set" / "intent_classification_fg_test_set.json"
CACHE = ROOT / "avature_fr_cache.json"
OUT = ROOT / "dataset.csv"
BASE_BUILDER = ROOT / "build_dataset.py"
# Avoid drowning tool intents: keep at most this many OUT_OF_SCOPE rows after merge.
OUT_OF_SCOPE_CAP = 180
RANDOM_SEED = 42

# Fine-grained Avature intent → RH Paie ChatIntent
INTENT_MAP: dict[str, str] = {
    "faq_pto_policy": "FAQ_LEAVE",
    "faq_maternity_paternity_policy": "FAQ_LEAVE",
    "faq_remote_work_policy": "FAQ_LEAVE",
    "faq_company_benefits": "FAQ_PAYROLL",
    "faq_salary_conversation": "FAQ_PAYROLL",
}


def normalize(text: str) -> str:
    value = (text or "").strip().lower()
    value = re.sub(r"\s+", " ", value)
    return value


def load_base_rows() -> list[tuple[str, str]]:
    """Run build_dataset.py logic without rewriting CSV yet."""
    ns: dict = {"ROWS": [], "add": None}
    code = BASE_BUILDER.read_text(encoding="utf-8")
    # Strip the final write block; keep only ROWS construction.
    trimmed = code.split("path = Path(__file__)")[0]
    exec(compile(trimmed, str(BASE_BUILDER), "exec"), ns)
    return list(ns["ROWS"])


def translate_batch(texts: list[str], cache: dict[str, str]) -> dict[str, str]:
    translator = MyMemoryTranslator(source="en-US", target="fr-FR")
    missing = [t for t in texts if t not in cache]
    print(f"Translating {len(missing)} / {len(texts)} utterances (cache hits: {len(texts) - len(missing)})…")
    for index, text in enumerate(missing, start=1):
        fr = None
        for attempt in range(4):
            try:
                fr = translator.translate(text[:450])
                break
            except Exception as exc:  # noqa: BLE001
                wait = 1.5 * (attempt + 1)
                print(f"  retry {attempt + 1} after {exc} (sleep {wait}s)")
                time.sleep(wait)
        if not fr or not fr.strip():
            fr = text
        cache[text] = fr.strip()
        if index % 25 == 0 or index == len(missing):
            CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"  {index}/{len(missing)} saved")
        time.sleep(0.35)
    return cache


def main() -> None:
    if not AVATURE.exists():
        raise SystemExit(f"Missing Avature file: {AVATURE}")

    raw = json.loads(AVATURE.read_text(encoding="utf-8"))
    cache: dict[str, str] = {}
    if CACHE.exists():
        cache = json.loads(CACHE.read_text(encoding="utf-8"))

    to_translate: list[str] = []
    planned: list[tuple[str, str]] = []  # (en_text, label)
    for avature_intent, samples in raw.items():
        label = INTENT_MAP.get(avature_intent, "OUT_OF_SCOPE")
        for sample in samples:
            text = (sample or "").strip()
            if not text:
                continue
            planned.append((text, label))
            # Translate only procedure-like FAQs (French CamemBERT). Keep EN for OUT_OF_SCOPE.
            if label != "OUT_OF_SCOPE":
                to_translate.append(text)

    unique_en = list(dict.fromkeys(to_translate))
    cache = translate_batch(unique_en, cache)
    CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=2), encoding="utf-8")

    merged: list[tuple[str, str]] = []
    seen: set[str] = set()

    def push(text: str, intent: str) -> None:
        key = normalize(text)
        if not key or key in seen:
            return
        seen.add(key)
        merged.append((text.strip(), intent))

    for text, intent in load_base_rows():
        push(text, intent)

    mapped_counts: Counter[str] = Counter()
    for en_text, label in planned:
        if label == "OUT_OF_SCOPE":
            push(en_text, label)
        else:
            fr = cache.get(en_text, en_text)
            push(fr, label)
            push(en_text, label)  # keep EN variant for bilingual questions
        mapped_counts[label] += 1

    # Downsample OUT_OF_SCOPE so CamemBERT does not collapse to that class.
    base_texts = {normalize(text) for text, _ in load_base_rows()}
    kept: list[tuple[str, str]] = []
    oos_base: list[tuple[str, str]] = []
    oos_external: list[tuple[str, str]] = []
    for text, intent in merged:
        if intent != "OUT_OF_SCOPE":
            kept.append((text, intent))
            continue
        if normalize(text) in base_texts:
            oos_base.append((text, intent))
        else:
            oos_external.append((text, intent))
    random.seed(RANDOM_SEED)
    random.shuffle(oos_external)
    remaining = max(0, OUT_OF_SCOPE_CAP - len(oos_base))
    selected_oos = oos_base + oos_external[:remaining]
    merged = kept + selected_oos
    random.shuffle(merged)

    with OUT.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["text", "intent"])
        writer.writerows(merged)

    by_intent = Counter(intent for _, intent in merged)
    print(f"\nWrote {len(merged)} rows → {OUT}")
    print("Per intent:")
    for intent, count in sorted(by_intent.items(), key=lambda item: (-item[1], item[0])):
        print(f"  {count:4d}  {intent}")
    print("Avature contribution (pre-dedupe pairs):", dict(mapped_counts))


if __name__ == "__main__":
    main()
