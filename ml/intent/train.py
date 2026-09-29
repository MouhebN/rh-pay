#!/usr/bin/env python3
"""Train the RH Paie intent classifier (CamemBERT embeddings + Logistic Regression)."""

from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, precision_recall_fscore_support
from sklearn.model_selection import train_test_split

from encoder import ENCODER_NAME, encode_texts

ROOT = Path(__file__).parent
DATASET = ROOT / "dataset.csv"
HOLDOUT = ROOT / "holdout.csv"
MODEL = ROOT / "model.joblib"
METRICS = ROOT / "metrics.txt"
CACHE = ROOT / "embedding_cache.npz"


def split_frame(frame: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame, str]:
    """Evaluate on the frozen holdout when it exists. New rows stay in train."""
    if HOLDOUT.exists():
        holdout = pd.read_csv(HOLDOUT)
        frozen = set(holdout["text"].astype(str))
        test = frame[frame["text"].astype(str).isin(frozen)].copy()
        train = frame[~frame["text"].astype(str).isin(frozen)].copy()
        missing = frozen - set(test["text"].astype(str))
        if missing:
            raise SystemExit(f"Frozen holdout texts missing from dataset.csv: {len(missing)}")
        return train, test, "frozen holdout.csv (stratified split of the 1280-row corpus, random_state=42)"
    texts = frame["text"].astype(str)
    labels = frame["intent"].astype(str)
    x_train, x_test, y_train, y_test = train_test_split(
        texts, labels, test_size=0.2, random_state=42, stratify=labels
    )
    train = pd.DataFrame({"text": x_train, "intent": y_train})
    test = pd.DataFrame({"text": x_test, "intent": y_test})
    return train, test, "stratified train_test_split test_size=0.2 random_state=42"


def encode_cached(texts: list[str]) -> np.ndarray:
    cached_text: list[str] = []
    cached_vectors = np.empty((0, 0))
    if CACHE.exists():
        blob = np.load(CACHE, allow_pickle=True)
        cached_text = blob["texts"].tolist()
        cached_vectors = blob["vectors"]
    index = {text: i for i, text in enumerate(cached_text)}
    missing = [text for text in texts if text not in index]
    if missing:
        print(f"Encoding {len(missing)} new texts with {ENCODER_NAME}…")
        fresh = np.asarray(encode_texts(missing), dtype=np.float32)
        if cached_vectors.size == 0:
            cached_vectors = fresh
            cached_text = list(missing)
        else:
            cached_vectors = np.vstack([cached_vectors, fresh])
            cached_text.extend(missing)
        np.savez(CACHE, texts=np.array(cached_text, dtype=object), vectors=cached_vectors)
        index = {text: i for i, text in enumerate(cached_text)}
    rows = [index[text] for text in texts]
    return cached_vectors[rows]


def confused_pairs(labels: list[str], matrix: np.ndarray, limit: int = 12) -> list[str]:
    pairs: list[tuple[int, str, str]] = []
    for i, true_label in enumerate(labels):
        for j, pred_label in enumerate(labels):
            if i == j:
                continue
            count = int(matrix[i, j])
            if count > 0:
                pairs.append((count, true_label, pred_label))
    pairs.sort(key=lambda item: (-item[0], item[1], item[2]))
    return [f"  {count:3d}  {true_label} → {pred_label}" for count, true_label, pred_label in pairs[:limit]]


def misclassified_lines(test: pd.DataFrame, predicted: np.ndarray, limit: int = 40) -> list[str]:
    lines = []
    for text, truth, guess in zip(test["text"].astype(str), test["intent"].astype(str), predicted, strict=True):
        if truth == guess:
            continue
        lines.append(f"  [{truth} → {guess}] {text}")
        if len(lines) >= limit:
            break
    return lines


def main() -> None:
    frame = pd.read_csv(DATASET)
    train, test, split_note = split_frame(frame)
    x_train = train["text"].astype(str).tolist()
    y_train = train["intent"].astype(str)
    x_test = test["text"].astype(str).tolist()
    y_test = test["intent"].astype(str)

    print(f"Split: {split_note}")
    print(f"Train / holdout: {len(x_train)} / {len(x_test)}")
    train_vectors = encode_cached(x_train)
    test_vectors = encode_cached(x_test)

    classifier = LogisticRegression(max_iter=400, class_weight="balanced")
    classifier.fit(train_vectors, y_train)
    predicted = classifier.predict(test_vectors)
    accuracy = accuracy_score(y_test, predicted)
    labels = list(classifier.classes_)
    report = classification_report(y_test, predicted, labels=labels, zero_division=0)
    matrix = confusion_matrix(y_test, predicted, labels=labels)
    precision, recall, f1, _ = precision_recall_fscore_support(
        y_test, predicted, labels=labels, average="macro", zero_division=0
    )
    class_counts = frame["intent"].astype(str).value_counts().sort_index()
    support = y_test.value_counts()
    pairs = confused_pairs(labels, matrix)
    errors = misclassified_lines(test, predicted)

    print(f"Holdout accuracy: {accuracy:.3f}  macro F1: {f1:.3f}")
    print("Most confused pairs:")
    print("\n".join(pairs))

    lines = [
        "RH Paie intent classifier — evaluation metrics",
        f"Encoder: {ENCODER_NAME} (pretrained; not trained from scratch)",
        "Backend: pretrained CamemBERT embeddings + LogisticRegression (class_weight=balanced, max_iter=400)",
        f"Dataset rows: {len(frame)}",
        f"Train / holdout: {len(x_train)} / {len(x_test)}",
        f"Split: {split_note}",
        f"Holdout accuracy: {accuracy:.4f}",
        f"Macro precision: {precision:.4f}",
        f"Macro recall: {recall:.4f}",
        f"Macro F1: {f1:.4f}",
        "",
        "Examples per intent (full dataset):",
    ]
    for intent, count in class_counts.items():
        lines.append(f"  {count:4d}  {intent}  (holdout support {int(support.get(intent, 0))})")
    lines.extend(["", "Classification report (holdout):", report.rstrip(), ""])
    lines.append("Most frequently confused pairs (true → predicted):")
    lines.extend(pairs or ["  none"])
    lines.extend(
        [
            "",
            "Confusion matrix labels (rows=true, cols=predicted):",
            "  " + " ".join(labels),
            "",
            "Confusion matrix:",
            np.array2string(matrix),
            "",
            "Misclassified holdout examples:",
            *errors,
            "",
            "Note: production model is re-fit on ALL rows after this evaluation.",
            "The holdout file is not used to choose labels or to tune the threshold.",
        ]
    )
    METRICS.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Saved metrics → {METRICS}")

    print(f"Encoding all {len(frame)} examples for the production model…")
    all_vectors = encode_cached(frame["text"].astype(str).tolist())
    classifier.fit(all_vectors, frame["intent"].astype(str))
    joblib.dump(
        {"backend": "camembert", "encoder_name": ENCODER_NAME, "classifier": classifier},
        MODEL,
    )
    print(f"Saved CamemBERT intent model on all {len(frame)} examples → {MODEL}")


if __name__ == "__main__":
    main()
