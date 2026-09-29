#!/usr/bin/env python3
"""Evaluate in-memory FAQ retrieval. Mirrors RagService ranking; does not call Ollama."""

from __future__ import annotations

import csv
import sys
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "ml" / "intent"))
from encoder import encode_texts  # noqa: E402

FAQ_DIR = ROOT / "backend" / "src" / "main" / "resources" / "chat" / "faq"
EVAL_CSV = Path(__file__).with_name("rag_eval.csv")
METRICS = Path(__file__).with_name("rag_metrics.txt")

# Same constant as RagService.VECTOR_MIN_SCORE. Abstain when boosted score <= this value.
VECTOR_MIN_SCORE = 0.32
# Frozen from the dev comparison below. Do not retune on the final split.
CHOSEN_WEAK_CEILING = 0.34
CHOSEN_MIN_MARGIN = 0.08
INTENT_BOOST = 0.06
FILE_INTENT = {
    "conges.md": "FAQ_LEAVE",
    "paie.md": "FAQ_PAYROLL",
    "pointage.md": "FAQ_ATTENDANCE",
}


def chunks_from_markdown(content: str, intent: str, filename: str) -> list[dict]:
    found = []
    if "## " not in content:
        return found
    for part in content.split("## ")[1:]:
        trimmed = part.strip()
        if not trimmed:
            continue
        line_break = trimmed.find("\n")
        title = trimmed if line_break < 0 else trimmed[:line_break].strip()
        body = "" if line_break < 0 else trimmed[line_break + 1 :].strip()
        if title.startswith("# "):
            continue
        if not body:
            body = title
        found.append(
            {
                "title": title,
                "body": body,
                "intent": intent,
                "file": filename,
                "text": title + "\n" + body,
            }
        )
    return found


def load_chunks() -> list[dict]:
    loaded = []
    for path in sorted(FAQ_DIR.glob("*.md")):
        intent = FILE_INTENT.get(path.name, "UNKNOWN")
        loaded.extend(chunks_from_markdown(path.read_text(encoding="utf-8"), intent, path.name))
    return loaded


def cosine(left: np.ndarray, right: np.ndarray) -> float:
    left_norm = np.linalg.norm(left)
    right_norm = np.linalg.norm(right)
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return float(np.dot(left, right) / (left_norm * right_norm))


def rank(question: str, intent: str, query_vector: np.ndarray, chunks: list[dict], vectors: np.ndarray) -> list[dict]:
    scored = []
    for chunk, vector in zip(chunks, vectors, strict=True):
        raw = cosine(query_vector, vector)
        boost = INTENT_BOOST if chunk["intent"] == intent else 0.0
        scored.append({**chunk, "raw": raw, "score": raw + boost})
    scored.sort(key=lambda item: item["score"], reverse=True)
    return scored


def kept(ranked: list[dict]) -> list[dict]:
    """Rank uses the intent boost. Abstention uses raw cosine only."""
    return [item for item in ranked[:3] if item["raw"] > VECTOR_MIN_SCORE]


def margin_of(ranked: list[dict]) -> tuple[float, float, float]:
    top = ranked[:2]
    raw1 = top[0]["raw"] if top else 0.0
    raw2 = top[1]["raw"] if len(top) > 1 else 0.0
    return raw1, raw2, raw1 - raw2


def accepts_margin(ranked: list[dict], weak_ceiling: float, min_margin: float) -> bool:
    """Keep the current cutoff. In the weak band, also require a top1-top2 raw margin.

    The band is applied to the strongest raw score inside the ranked top 3, so a
    second chunk cannot survive alone when the lead evidence is weak and close.
    """
    visible = kept(ranked)
    if not visible:
        return False
    lead = max(item["raw"] for item in visible)
    if lead > weak_ceiling:
        return True
    raw1, _raw2, gap = margin_of(ranked)
    return gap >= min_margin and raw1 > VECTOR_MIN_SCORE


def load_rows() -> list[dict]:
    with EVAL_CSV.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def recall(rows: list[dict], details: list[dict]) -> tuple[float, float, int, int]:
    positives = [item for item in details if item["negative"] == "0"]
    if not positives:
        return 0.0, 0.0, 0, 0
    top1 = sum(1 for item in positives if item["hit1"])
    top3 = sum(1 for item in positives if item["hit3"])
    return top1 / len(positives), top3 / len(positives), top1, top3


def main() -> None:
    chunks = load_chunks()
    if len(chunks) > 3 and any(len(kept(rank("x", "FAQ_LEAVE", np.zeros(4), chunks[:1], np.zeros((1, 4))))) > 3 for _ in [0]):
        raise SystemExit("top-k check setup failed")
    rows = load_rows()
    chunk_vectors = np.asarray(encode_texts([chunk["text"] for chunk in chunks]), dtype=np.float64)
    query_vectors = np.asarray(encode_texts([row["query"] for row in rows]), dtype=np.float64)

    details = []
    for row, query_vector in zip(rows, query_vectors, strict=True):
        ranked = rank(row["query"], row["intent"], query_vector, chunks, chunk_vectors)
        visible = kept(ranked)
        if len(visible) > 3:
            raise SystemExit("retrieval returned more than top 3")
        titles = [item["title"] for item in visible]
        expected = row["expected_section"]
        hit1 = bool(expected) and bool(titles) and titles[0] == expected
        hit3 = bool(expected) and expected in titles
        top = ranked[:3]
        raw1, raw2, gap = margin_of(ranked)
        details.append(
            {
                **row,
                "hit1": hit1,
                "hit3": hit3,
                "visible": titles,
                "false_answer": row["negative"] == "1" and bool(titles),
                "current_accept": bool(titles),
                "raw1": raw1,
                "raw2": raw2,
                "margin": gap,
                "top": top,
                "ranked": ranked,
            }
        )

    dev_rows = [item for item in details if item["split"] == "dev"]
    candidates = []
    for ceiling in (0.34, 0.36, 0.40):
        for min_margin in (0.02, 0.04, 0.05, 0.08):
            accepted = []
            for item in dev_rows:
                keep = accepts_margin(item["ranked"], ceiling, min_margin)
                titles = [hit["title"] for hit in kept(item["ranked"])] if keep else []
                expected = item["expected_section"]
                accepted.append(
                    {
                        "negative": item["negative"],
                        "hit1": bool(expected) and bool(titles) and titles[0] == expected,
                        "hit3": bool(expected) and expected in titles,
                        "false_answer": item["negative"] == "1" and bool(titles),
                    }
                )
            positives = [item for item in accepted if item["negative"] == "0"]
            negatives = [item for item in accepted if item["negative"] == "1"]
            r1, r3, n1, n3 = recall(positives, positives)
            false_answers = sum(1 for item in negatives if item["false_answer"])
            candidates.append((ceiling, min_margin, r1, n1, len(positives), r3, n3, false_answers, len(negatives)))

    lines = [
        "FAQ retrieval evaluation",
        "Encoder: pretrained Lajavaness/sentence-camembert-base (not trained here)",
        "Index: in-memory chunks, cosine similarity, no vector database",
        f"Rank: cosine + {INTENT_BOOST} when the chunk intent matches",
        f"Cutoff: raw cosine > {VECTOR_MIN_SCORE} (the intent boost does not keep a weak chunk)",
        f"Questions: {len(rows)}  positives: {sum(1 for row in rows if row['negative'] == '0')}  negatives: {sum(1 for row in rows if row['negative'] == '1')}",
        "",
        "DEV margin candidates (not applied). Accept if raw > 0.32, and if the strongest kept raw is <= ceiling then top1-top2 raw margin must be >= min.",
    ]
    base_pos = [item for item in dev_rows if item["negative"] == "0"]
    base_neg = [item for item in dev_rows if item["negative"] == "1"]
    base_r1, base_r3, base_n1, base_n3 = recall(base_pos, base_pos)
    base_false = sum(1 for item in base_neg if item["false_answer"])
    lines.append(
        f"  current  Recall@1 {base_r1:.4f} ({base_n1}/{len(base_pos)})  Recall@3 {base_r3:.4f} ({base_n3}/{len(base_pos)})  false answers {base_false}/{len(base_neg)}"
    )
    for ceiling, min_margin, r1, n1, npos, r3, n3, false_answers, nneg in candidates:
        lines.append(
            f"  ceiling {ceiling:.2f} margin {min_margin:.2f}  Recall@1 {r1:.4f} ({n1}/{npos})  Recall@3 {r3:.4f} ({n3}/{npos})  false answers {false_answers}/{nneg}"
        )
    lines.append("")
    lines.append(
        f"Frozen rule from DEV only: strongest kept raw > {CHOSEN_WEAK_CEILING} "
        f"or (top1 raw > {VECTOR_MIN_SCORE} and top1-top2 margin >= {CHOSEN_MIN_MARGIN}). Not retuned on final."
    )
    for split in ("dev", "final", "all"):
        subset = details if split == "all" else [item for item in details if item["split"] == split]
        positives = [item for item in subset if item["negative"] == "0"]
        negatives = [item for item in subset if item["negative"] == "1"]
        r1, r3, n1, n3 = recall(positives, positives)
        false_answers = sum(1 for item in negatives if item["false_answer"])
        chosen = []
        for item in subset:
            keep = accepts_margin(item["ranked"], CHOSEN_WEAK_CEILING, CHOSEN_MIN_MARGIN)
            titles = [hit["title"] for hit in kept(item["ranked"])] if keep else []
            expected = item["expected_section"]
            chosen.append(
                {
                    "negative": item["negative"],
                    "hit1": bool(expected) and bool(titles) and titles[0] == expected,
                    "hit3": bool(expected) and expected in titles,
                    "false_answer": item["negative"] == "1" and bool(titles),
                }
            )
        chosen_pos = [item for item in chosen if item["negative"] == "0"]
        chosen_neg = [item for item in chosen if item["negative"] == "1"]
        c1, c3, cn1, cn3 = recall(chosen_pos, chosen_pos)
        chosen_false = sum(1 for item in chosen_neg if item["false_answer"])
        lines.append(f"== {split} ==")
        lines.append(f"before  Recall@1 {r1:.4f} ({n1}/{len(positives) or 1})  Recall@3 {r3:.4f} ({n3}/{len(positives) or 1})  false answers {false_answers}/{len(negatives)}")
        lines.append(f"frozen  Recall@1 {c1:.4f} ({cn1}/{len(chosen_pos) or 1})  Recall@3 {c3:.4f} ({cn3}/{len(chosen_pos) or 1})  false answers {chosen_false}/{len(chosen_neg)}")
        misses = [item for item in positives if not item["hit3"]]
        if misses:
            lines.append("misses (expected section absent from the returned top 3):")
            for item in misses:
                top = item["top"][0]
                lines.append(
                    f"  [{item['expected_section']} | got {item['visible'] or 'ABSTAIN'}] {item['query']}"
                    f"  raw1={top['raw']:.3f} score1={top['score']:.3f} ({top['title']})"
                )
        lines.append("top scores:")
        for item in subset:
            bits = " | ".join(
                f"{hit['title'][:28]} raw={hit['raw']:.3f} score={hit['score']:.3f}" for hit in item["top"]
            )
            bits += f"  margin={item['margin']:.3f} current={'yes' if item['current_accept'] else 'no'}"
            flag = "NEG" if item["negative"] == "1" else ("OK" if item["hit1"] else "MISS")
            lines.append(f"  {flag} {item['query'][:60]} :: {bits}")
        lines.append("")

    text = "\n".join(lines) + "\n"
    METRICS.write_text(text, encoding="utf-8")
    print(text)
    print(f"Wrote {METRICS}")


if __name__ == "__main__":
    main()
