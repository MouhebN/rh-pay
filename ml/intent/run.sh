#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export HF_HOME="${HF_HOME:-$PWD/.cache/huggingface}"
export HF_HUB_DISABLE_XET=1
export PYTHONUNBUFFERED=1
if [[ ! -d .venv ]]; then
  python3 -m venv .venv
fi
.venv/bin/pip install -q -r requirements.txt
need_train=0
if [[ ! -f model.joblib ]]; then
  need_train=1
elif ! .venv/bin/python -c "import joblib; assert joblib.load('model.joblib').get('backend')=='camembert'" >/dev/null 2>&1; then
  need_train=1
fi
if [[ "$need_train" == "1" ]]; then
  if [[ -f ../../intent-classification-main/test_set/intent_classification_fg_test_set.json ]]; then
    .venv/bin/python merge_avature.py
  else
    .venv/bin/python build_dataset.py
  fi
  .venv/bin/python train.py
fi
exec .venv/bin/uvicorn app:app --host 127.0.0.1 --port 8010
