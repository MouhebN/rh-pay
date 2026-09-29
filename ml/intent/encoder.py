"""Shared CamemBERT sentence encoder for intent classification and RAG embeddings."""

from __future__ import annotations

from pathlib import Path

LOCAL_ENCODER = Path(__file__).parent / "models" / "sentence-camembert-base"
HF_ENCODER_NAME = "Lajavaness/sentence-camembert-base"
ENCODER_NAME = str(LOCAL_ENCODER) if (LOCAL_ENCODER / "model.safetensors").exists() else HF_ENCODER_NAME
MAX_CHARS = 1500

_encoder = None


def get_encoder():
    global _encoder
    if _encoder is None:
        from sentence_transformers import SentenceTransformer

        _encoder = SentenceTransformer(ENCODER_NAME, device="cpu")
    return _encoder


def encode_texts(texts: list[str]) -> list[list[float]]:
    cleaned = [(text or "")[:MAX_CHARS] for text in texts]
    vectors = get_encoder().encode(
        cleaned,
        normalize_embeddings=True,
        show_progress_bar=False,
    )
    return [vector.tolist() for vector in vectors]
