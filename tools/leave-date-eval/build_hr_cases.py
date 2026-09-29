#!/usr/bin/env python3
"""Build ~150 HR leave-date robustness cases from MASSIVE temporal vocabulary.

Fixed reference date: 2026-09-16 (Wednesday).
Only confidently labeled cases are emitted; underspecified MASSIVE phrases are excluded.

Outputs JSON consumed by Java evaluation tests (test resources).
Does NOT modify production parsers.
"""

from __future__ import annotations

import json
from pathlib import Path

REF = "2026-09-16"
# Calendar from REF (Wed):
D15, D16, D17, D18, D19, D20 = (
    "2026-09-15",
    "2026-09-16",
    "2026-09-17",
    "2026-09-18",
    "2026-09-19",
    "2026-09-20",
)
D21, D22, D23, D24, D25, D26 = (
    "2026-09-21",
    "2026-09-22",
    "2026-09-23",
    "2026-09-24",
    "2026-09-25",
    "2026-09-26",
)


def dates(name, category, input_, start, end, origin, note=""):
    return {
        "name": name,
        "category": category,
        "input": input_,
        "expectedStart": start,
        "expectedEnd": end,
        "shouldAskClarification": False,
        "origin": origin,
        "note": note,
    }


def clarify(name, category, input_, origin, note=""):
    return {
        "name": name,
        "category": category,
        "input": input_,
        "expectedStart": None,
        "expectedEnd": None,
        "shouldAskClarification": True,
        "origin": origin,
        "note": note,
    }


def build_all() -> list[dict]:
    cases: list[dict] = []

    # --- French natural (~65): MASSIVE date-slot wording adapted to HR leave ---
    natural = [
        ("nat_aujourdhui", "je veux poser un congé aujourd'hui", D16, D16, "massive:fr-FR:date=aujourd'hui"),
        ("nat_demain", "je voudrais prendre demain", D17, D17, "massive:fr-FR:date=demain"),
        ("nat_apres_demain", "je veux un congé après-demain", D18, D18, "massive:fr-FR:date=après-demain"),
        ("nat_apres_demain_space", "congé après demain", D18, D18, "massive:fr-FR:date=après demain"),
        ("nat_pour_demain", "je veux poser pour demain", D17, D17, "massive:fr-FR:date=pour demain"),
        ("nat_de_demain", "congé de demain s'il vous plaît", D17, D17, "massive:fr-FR:date=de demain"),
        ("nat_vendredi", "je voudrais poser vendredi", D18, D18, "massive:fr-FR:date=vendredi"),
        ("nat_ce_vendredi", "je veux un congé ce vendredi", D18, D18, "massive:fr-FR:date=ce vendredi"),
        ("nat_mardi", "je prends mardi", D22, D22, "massive:fr-FR:date=mardi"),
        ("nat_lundi", "pose un congé lundi", D21, D21, "massive:fr-FR:date=lundi"),
        ("nat_jeudi", "je veux jeudi en congé", D17, D17, "massive:fr-FR:date=jeudi"),
        ("nat_mercredi_today", "congé mercredi", D16, D16, "massive:fr-FR:date=mercredi"),
        ("nat_samedi", "je veux congé samedi", D19, D19, "massive:fr-FR:date=samedi"),
        ("nat_dimanche", "pose dimanche", D20, D20, "massive:fr-FR:date=dimanche"),
        ("nat_lundi_prochain", "je voudrais lundi prochain", D21, D21, "massive:fr-FR:date=lundi prochain"),
        ("nat_vendredi_prochain", "congé vendredi prochain", D18, D18, "massive:fr-FR:date=vendredi + prochain"),
        ("nat_samedi_prochain", "congé samedi prochain", D19, D19, "massive:fr-FR:date=samedi prochain"),
        ("nat_dimanche_prochain", "je prends dimanche prochain", D20, D20, "massive:fr-FR:date=dimanche prochain"),
        ("nat_ce_samedi", "off ce samedi", D19, D19, "massive:fr-FR:date=ce samedi"),
        ("nat_ce_dimanche", "congé ce dimanche", D20, D20, "massive:fr-FR:date=ce dimanche"),
        ("nat_ce_lundi", "je veux ce lundi", D21, D21, "massive:fr-FR:date=ce lundi"),
        ("nat_weekend", "je veux le week-end", D19, D20, "massive:fr-FR:date=week-end"),
        ("nat_ce_weekend", "congé ce weekend", D19, D20, "massive:fr-FR:date=ce weekend"),
        ("nat_ce_week_end", "je prends ce week-end", D19, D20, "massive:fr-FR:date=ce week-end"),
        ("nat_ce_week_end_space", "pose ce week end", D19, D20, "massive:fr-FR:date=ce week end"),
        ("nat_hier", "j'étais absent hier", D15, D15, "massive:fr-FR:date=hier"),
        ("nat_dans_trois_jours", "je veux un congé dans trois jours", D19, D19, "massive:fr-FR:date=dans trois jours"),
        ("nat_du_lundi_au_vendredi", "je veux du lundi au vendredi", D21, D25, "massive:fr-FR:date=du lundi au vendredi"),
        ("nat_de_mardi_a_jeudi", "de mardi à jeudi en congé", D22, D24, "adapted:weekday-range"),
        ("nat_demain_jusqua_vendredi", "demain jusqu'à vendredi", D17, D18, "adapted:jusqua"),
        ("nat_de_demain_a_apres", "de demain à après demain", D17, D18, "adapted:relative-range"),
        ("nat_deux_prochains_jours", "les deux prochains jours", D17, D18, "massive:fr-FR:date=prochains jours"),
        ("nat_demain_et_apres", "demain et après demain", D17, D18, "adapted:conjunction"),
        ("nat_demain_pour_deux", "je prends demain pour deux jours", D17, D18, "adapted:duration"),
        ("nat_a_partir_demain_3", "à partir de demain pendant 3 jours", D17, D19, "adapted:duration"),
        ("nat_vendredi_cette_semaine", "vendredi de cette semaine", D18, D18, "massive:fr-FR:date=vendredi + cette semaine"),
        ("nat_lundi_cette_semaine_next", "lundi cette semaine", "2026-09-14", "2026-09-14", "massive:fr-FR:date=lundi cette semaine"),
        ("nat_je_veux_prendre_demain", "je veux prendre un congé demain", D17, D17, "adapted:hr-wrapper"),
        ("nat_poser_apres_demain", "je voudrais poser après-demain", D18, D18, "adapted:hr-wrapper"),
        ("nat_conge_vendredi", "congé vendredi s'il te plaît", D18, D18, "adapted:hr-wrapper"),
        ("nat_absence_mardi", "absence mardi", D22, D22, "adapted:hr-wrapper"),
        ("nat_off_jeudi", "je suis off jeudi", D17, D17, "adapted:hr-wrapper"),
        ("nat_repos_samedi_dimanche", "repos samedi et dimanche", D19, D20, "adapted:weekend-conjunction"),
        ("nat_du_17_au_19", "du 17 au 19 septembre", D17, D19, "adapted:calendar-range"),
        ("nat_le_17_septembre", "le 17 septembre", D17, D17, "adapted:calendar"),
        ("nat_17_septembre", "17 septembre en congé", D17, D17, "adapted:calendar"),
        ("nat_demain_matin_date", "congé demain", D17, D17, "massive:fr-FR:date=demain (ignore time-of-day)"),
        ("nat_aujourdhui_fold", "congé aujourdhui", D16, D16, "massive:fr-FR:date=aujourdhui"),
        ("nat_weekend_prochain", "le weekend prochain", D19, D20, "massive:fr-FR:date=week-end + prochain"),
        ("nat_fin_de_semaine", "en fin de semaine", D19, D20, "adapted:fin-de-semaine"),
        ("nat_de_lundi_a_mercredi", "de lundi à mercredi", D21, D23, "adapted:weekday-range"),
        ("nat_jusqua_vendredi_from_demain", "à partir de demain jusqu'à vendredi", D17, D18, "adapted:range"),
        ("nat_je_prends_vendredi", "je prends vendredi", D18, D18, "adapted:hr-wrapper"),
        ("nat_besoin_conge_demain", "j'ai besoin d'un congé demain", D17, D17, "adapted:hr-wrapper"),
        ("nat_souhaite_lundi", "je souhaite un congé lundi", D21, D21, "adapted:hr-wrapper"),
        ("nat_demander_apres_demain", "je demande un congé après demain", D18, D18, "adapted:hr-wrapper"),
        ("nat_poser_du_18_au_19", "poser du 18/09 au 19/09", D18, D19, "adapted:slash-range"),
        ("nat_17_slash_09", "congé 17/09", D17, D17, "adapted:slash"),
        ("nat_17_dash_09", "congé 17-09", D17, D17, "adapted:dash"),
        ("nat_deux_jours_demain", "demain et le jour d'après", D17, D18, "adapted:fr day-after"),
        ("nat_mardi_prochain", "mardi prochain", D22, D22, "adapted:weekday+prochain"),
        ("nat_jeudi_prochain", "jeudi prochain", D17, D17, "adapted:weekday+prochain"),
        ("nat_mercredi_prochain", "mercredi prochain", D23, D23, "adapted:weekday+prochain"),
        ("nat_conge_ce_jeudi", "congé ce jeudi", D17, D17, "adapted:ce + weekday"),
        ("nat_absence_ce_mardi", "absence ce mardi", D22, D22, "adapted:ce + weekday"),
        ("nat_je_pose_demain_apres", "je pose demain et après-demain", D17, D18, "adapted:conjunction"),
        ("nat_prendre_off_vendredi", "je veux prendre off vendredi", D18, D18, "adapted:hr-wrapper"),
        ("nat_conge_du_22_au_24", "congé du 22 au 24 septembre", D22, D24, "adapted:calendar-range"),
        ("nat_le_20_septembre", "le 20 septembre", D20, D20, "adapted:calendar"),
        ("nat_apres_demain_hyphen_hr", "je voudrais un congé après-demain", D18, D18, "massive:fr-FR:date=après-demain"),
    ]
    for name, inp, start, end, origin in natural:
        cases.append(dates(name, "natural", inp, start, end, origin))

    # --- Explicit / numeric / weekday variants (~22) ---
    explicit = [
        ("exp_iso_single", "2026-09-17", D17, D17, "synthetic:iso"),
        ("exp_iso_range", "2026-09-17 au 2026-09-19", D17, D19, "synthetic:iso-range"),
        ("exp_slash_range", "du 17/09/2026 au 19/09/2026", D17, D19, "synthetic:slash-year"),
        ("exp_le_18_septembre", "le 18 septembre 2026", D18, D18, "synthetic:month-year"),
        ("exp_19_septembre", "19 septembre", D19, D19, "synthetic:month"),
        ("exp_du_20_au_22", "du 20 septembre au 22 septembre", D20, D22, "synthetic:month-range"),
        ("exp_lundi_seul", "lundi", D21, D21, "massive:fr-FR:date=lundi"),
        ("exp_mardi_seul", "mardi", D22, D22, "massive:fr-FR:date=mardi"),
        ("exp_mercredi_seul", "mercredi", D16, D16, "massive:fr-FR:date=mercredi"),
        ("exp_jeudi_seul", "jeudi", D17, D17, "massive:fr-FR:date=jeudi"),
        ("exp_vendredi_seul", "vendredi", D18, D18, "massive:fr-FR:date=vendredi"),
        ("exp_samedi_seul", "samedi", D19, D19, "massive:fr-FR:date=samedi"),
        ("exp_dimanche_seul", "dimanche", D20, D20, "massive:fr-FR:date=dimanche"),
        ("exp_today_word", "aujourd'hui", D16, D16, "massive:fr-FR:date=aujourd'hui"),
        ("exp_tomorrow_word", "demain", D17, D17, "massive:fr-FR:date=demain"),
        ("exp_day_after", "après-demain", D18, D18, "massive:fr-FR:date=après-demain"),
        ("exp_17_09", "17/09", D17, D17, "synthetic:slash"),
        ("exp_18_09", "18-09", D18, D18, "synthetic:dash"),
        ("exp_du_17_au_18", "du 17 au 18 septembre", D17, D18, "synthetic:day-range-month"),
        ("exp_weekend_only", "weekend", D19, D20, "massive:fr-FR:date=weekend"),
        ("exp_week_end_only", "week-end", D19, D20, "massive:fr-FR:date=week-end"),
        ("exp_de_vendredi_a_dimanche", "de vendredi à dimanche", D18, D20, "adapted:weekday-range"),
    ]
    for name, inp, start, end, origin in explicit:
        cases.append(dates(name, "explicit", inp, start, end, origin))

    # --- Typo / noisy (~18) derived from otherwise valid examples ---
    typo = [
        ("typo_demian2", "je veux demian", D17, D17, "synthetic:typo of demain"),
        ("typo_demainn2", "congé demainn", D17, D17, "synthetic:typo"),
        ("typo_apre_demain2", "apré demain off", D18, D18, "synthetic:typo"),
        ("typo_vendredi_prochin2", "vendredi prochin", D18, D18, "synthetic:typo"),
        ("typo_septembr2", "le 17 septembr", D17, D17, "synthetic:typo"),
        ("typo_aujourdui", "aujourdui", D16, D16, "synthetic:typo of aujourd'hui"),
        ("typo_vendredii", "vendredii", D18, D18, "synthetic:typo"),
        ("typo_lundii", "lundii prochain", D21, D21, "synthetic:typo"),
        ("typo_mardii", "mardii", D22, D22, "synthetic:typo"),
        ("typo_jeudii", "jeudii", D17, D17, "synthetic:typo"),
        ("typo_samedii", "samedii", D19, D19, "synthetic:typo"),
        ("typo_dimanchhe", "dimanchhe", D20, D20, "synthetic:typo"),
        ("typo_apresdemain", "apresdemain", D18, D18, "synthetic:typo compact"),
        ("typo_week_endd", "week-endd", D19, D20, "synthetic:typo"),
        ("typo_deuux_jours", "les deuux prochains jours", D17, D18, "synthetic:typo"),
        ("typo_mercredii", "mercredii", D16, D16, "synthetic:typo"),
        ("typo_17_septmbre", "17 septmbre", D17, D17, "synthetic:typo"),
        ("typo_demain_et_apre", "demain et apré demain", D17, D18, "synthetic:typo"),
    ]
    for name, inp, start, end, origin in typo:
        cases.append(dates(name, "typo", inp, start, end, origin))

    # --- Mixed FR/EN (~18) using MASSIVE en-US date vocabulary ---
    mixed = [
        ("mix_tomorrow", "je veux congé tomorrow", D17, D17, "massive:en-US:date=tomorrow"),
        ("mix_today", "off today", D16, D16, "massive:en-US:date=today"),
        ("mix_friday", "congé friday", D18, D18, "massive:en-US:date=friday"),
        ("mix_monday", "je prends monday", D21, D21, "massive:en-US:date=monday"),
        ("mix_tuesday", "absence tuesday", D22, D22, "massive:en-US:date=tuesday"),
        ("mix_wednesday", "congé wednesday", D16, D16, "massive:en-US:date=wednesday"),
        ("mix_thursday", "off thursday", D17, D17, "massive:en-US:date=thursday"),
        ("mix_saturday", "congé saturday", D19, D19, "massive:en-US:date=saturday"),
        ("mix_sunday", "pose sunday", D20, D20, "massive:en-US:date=sunday"),
        ("mix_next_friday", "je veux next friday", D18, D18, "massive:en-US:date=this friday / next friday"),
        ("mix_day_after_tomorrow", "day after tomorrow en congé", D18, D18, "massive:en-US:date=day after tomorrow"),
        ("mix_tomorrow_and_day_after", "tomorrow and the day after", D17, D18, "massive:en-US:date=the day after"),
        ("mix_from_tomorrow_to_friday", "from tomorrow to friday", D17, D18, "adapted:from-to"),
        ("mix_from_demain_to_friday", "from demain to friday", D17, D18, "adapted:mixed anchors"),
        ("mix_weekend_en", "this weekend off", D19, D20, "massive:en-US:date=this weekend"),
        ("mix_yesterday", "absent yesterday", D15, D15, "massive:en-US:date=yesterday"),
        ("mix_je_veux_friday", "je veux friday", D18, D18, "adapted:mixed"),
        ("mix_conge_next_monday", "congé next monday", D21, D21, "massive:en-US:date=next monday"),
        ("mix_from_monday_to_wednesday", "from monday to wednesday", D21, D23, "adapted:from-to weekdays"),
        ("mix_tomorrow_jusqua_friday", "tomorrow jusqu'à friday", D17, D18, "adapted:mixed connector"),
    ]
    for name, inp, start, end, origin in mixed:
        cases.append(dates(name, "mixed", inp, start, end, origin))

    # --- Ambiguous / underspecified (~18): correct = no confident dates ---
    ambiguous = [
        ("amb_cette_semaine", "cette semaine", "massive:fr-FR:date=cette semaine"),
        ("amb_la_semaine_prochaine", "la semaine prochaine", "massive:fr-FR:date=la semaine prochaine"),
        ("amb_semaine_prochaine", "semaine prochaine", "massive:fr-FR:date=semaine prochaine"),
        ("amb_prochaine_semaine", "prochaine semaine", "massive:fr-FR:date=prochaine semaine"),
        ("amb_semaine_a_venir", "la semaine à venir", "massive:fr-FR:date=la semaine à venir"),
        ("amb_this_week", "this week", "massive:en-US:date=this week"),
        ("amb_next_week", "next week", "massive:en-US:date=next week"),
        ("amb_ce_mois", "ce mois", "massive:fr-FR:date=ce mois"),
        ("amb_ce_mois_ci", "ce mois-ci", "massive:fr-FR:date=ce mois-ci"),
        ("amb_le_mois_prochain", "le mois prochain", "massive:fr-FR:date=le mois prochain"),
        ("amb_next_month", "next month", "massive:en-US:date=next month"),
        ("amb_fin_du_mois", "fin du mois", "massive:fr-FR:date=fin du mois"),
        ("amb_bientot", "bientôt", "adapted:underspecified"),
        ("amb_quelques_jours", "quelques jours", "adapted:underspecified"),
        ("amb_apres_quelques_jours", "après quelques jours", "massive:fr-FR:date=après quelques jours"),
        ("amb_dans_deux_semaines", "dans deux semaines", "massive:fr-FR:date=dans deux semaines"),
        ("amb_prochains_jours_vague", "prochains jours", "massive:fr-FR:date=prochains jours"),
        ("amb_un_de_ces_jours", "un de ces jours", "adapted:underspecified"),
        ("amb_coming_week", "coming week", "massive:en-US:date=coming week"),
        ("amb_rest_of_week", "reste de la semaine", "massive:fr-FR:date=reste de la semaine"),
    ]
    for name, inp, origin in ambiguous:
        cases.append(clarify(name, "ambiguous", inp, origin))

    # uniqueness
    names = [c["name"] for c in cases]
    assert len(names) == len(set(names)), "duplicate case names"
    return cases


def split_dev_holdout(cases: list[dict]) -> tuple[list[dict], list[dict]]:
    """Deterministic split: ~100 DEV / ~50 HOLDOUT, stratified by category."""
    by_cat: dict[str, list[dict]] = {}
    for case in cases:
        by_cat.setdefault(case["category"], []).append(case)

    # Holdout ratios roughly 1/3 per category
    # Aim ~50 holdout / ~100 dev for ~150 cases.
    holdout_quota = {
        "natural": 23,
        "explicit": 7,
        "typo": 6,
        "mixed": 7,
        "ambiguous": 7,
    }
    while sum(holdout_quota.values()) < 50:
        for cat in ("natural", "explicit", "typo", "mixed", "ambiguous"):
            if sum(holdout_quota.values()) >= 50:
                break
            if holdout_quota[cat] < len(by_cat.get(cat, [])):
                holdout_quota[cat] += 1

    dev: list[dict] = []
    holdout: list[dict] = []
    for cat, items in by_cat.items():
        # Stable order by name
        items = sorted(items, key=lambda c: c["name"])
        n_hold = min(holdout_quota.get(cat, 0), len(items))
        # Take every k-th for holdout diversity
        if n_hold == 0:
            dev.extend(items)
            continue
        step = max(1, len(items) // n_hold)
        hold_idx = set()
        i = 0
        while len(hold_idx) < n_hold and i < len(items) * 3:
            hold_idx.add((i * step) % len(items))
            i += 1
            if len(hold_idx) >= n_hold:
                break
        # If still short, fill from end
        j = len(items) - 1
        while len(hold_idx) < n_hold and j >= 0:
            hold_idx.add(j)
            j -= 1
        for idx, case in enumerate(items):
            if idx in hold_idx:
                holdout.append(case)
            else:
                dev.append(case)

    dev = sorted(dev, key=lambda c: (c["category"], c["name"]))
    holdout = sorted(holdout, key=lambda c: (c["category"], c["name"]))
    return dev, holdout


def main() -> None:
    root = Path(__file__).resolve().parent
    cases = build_all()
    dev, holdout = split_dev_holdout(cases)

    out_dir = root / "cases"
    out_dir.mkdir(parents=True, exist_ok=True)

    def dump(path: Path, payload: list[dict]) -> None:
        path.write_text(
            json.dumps(
                {
                    "referenceDate": REF,
                    "source": "Amazon MASSIVE 1.1 temporal slots (fr-FR/en-US) adapted to HR leave utterances",
                    "cases": payload,
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )

    dump(out_dir / "robustness-all.json", cases)
    dump(out_dir / "robustness-dev.json", dev)
    dump(out_dir / "robustness-holdout.json", holdout)

    # Also copy into backend test resources
    res = Path(__file__).resolve().parents[2] / "backend" / "src" / "test" / "resources" / "leave-date-eval"
    res.mkdir(parents=True, exist_ok=True)
    dump(res / "robustness-dev.json", dev)
    dump(res / "robustness-holdout.json", holdout)

    cats: dict[str, int] = {}
    for c in cases:
        cats[c["category"]] = cats.get(c["category"], 0) + 1

    print(f"Total cases: {len(cases)}")
    print(f"DEV: {len(dev)}")
    print(f"HOLDOUT: {len(holdout)}")
    print("By category (all):", cats)
    print(f"Wrote {out_dir}")
    print(f"Wrote {res}")


if __name__ == "__main__":
    main()
