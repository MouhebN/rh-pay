#!/usr/bin/env python3
"""Local NLP API used by Spring: CamemBERT intent (/predict) and embeddings (/embed)."""

from pathlib import Path

import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from encoder import ENCODER_NAME, encode_texts, get_encoder

ROOT = Path(__file__).parent
MODEL_PATH = ROOT / "model.joblib"

if not MODEL_PATH.exists():
    raise RuntimeError("Missing model.joblib. Run: python train.py")

bundle = joblib.load(MODEL_PATH)
if bundle.get("backend") != "camembert":
    raise RuntimeError("model.joblib is not a CamemBERT model. Run: python train.py")

classifier = bundle["classifier"]
get_encoder()

app = FastAPI(title="RH Paie CamemBERT NLP")


class PredictRequest(BaseModel):
    text: str


class PredictResponse(BaseModel):
    intent: str
    confidence: float


class EmbedRequest(BaseModel):
    texts: list[str]


class EmbedResponse(BaseModel):
    vectors: list[list[float]]
    encoder: str = ENCODER_NAME


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "backend": "camembert", "encoder": ENCODER_NAME}


@app.post("/predict", response_model=PredictResponse)
def predict(payload: PredictRequest) -> PredictResponse:
    text = (payload.text or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="text is required")
    vector = encode_texts([text])[0]
    probabilities = classifier.predict_proba([vector])[0]
    index = int(probabilities.argmax())
    return PredictResponse(
        intent=str(classifier.classes_[index]),
        confidence=float(probabilities[index]),
    )


@app.post("/embed", response_model=EmbedResponse)
def embed(payload: EmbedRequest) -> EmbedResponse:
    texts = payload.texts or []
    if not texts:
        return EmbedResponse(vectors=[])
    if len(texts) > 200:
        raise HTTPException(status_code=400, detail="too many texts")
    return EmbedResponse(vectors=encode_texts(texts))
