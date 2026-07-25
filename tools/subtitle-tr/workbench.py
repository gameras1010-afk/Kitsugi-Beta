#!/usr/bin/env python3
"""
workbench.py
============
Ceviri is akisinin iki ucu:

  extract : altyazi dosyasindan cevrilecek DUZ METINLERI json'a cikarir
  apply   : cevrilmis json'u orijinal dosyaya geri isler (etiket/zaman korunur)
  info    : klasordeki tum altyazilarin ozetini verir

Neden json?
-----------
Cevirmen (insan ya da model) sadece metni gorur; zaman kodlarina, ASS
etiketlerine, stil satirlarina hic dokunmaz. Boylece dosyanin bozulmasi
teknik olarak imkansiz hale gelir: geri yazma islemini her zaman
subtitle_io yapar.

Kullanim
--------
  python3 workbench.py info    kaynak/
  python3 workbench.py extract kaynak/ep01.ass -o calisma/ep01.json
  #  -> calisma/ep01.json icindeki "tr" alanlarini doldur
  python3 workbench.py apply   kaynak/ep01.ass calisma/ep01.json -o cikti/ep01.tr.ass
  python3 qa_check.py kaynak/ep01.ass cikti/ep01.tr.ass
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from subtitle_io import load, stats

EXTS = {".srt", ".ass", ".ssa", ".vtt", ".sub"}


def cmd_info(args) -> int:
    root = Path(args.path)
    files = (
        [root] if root.is_file()
        else sorted(p for p in root.rglob("*") if p.suffix.lower() in EXTS)
    )
    if not files:
        print("Altyazi dosyasi bulunamadi.")
        return 1
    total_seg = total_chr = 0
    print(f"{'dosya':<38} {'fmt':<5} {'kod':<10} {'nl':<5} {'replik':>7} {'karakter':>9}")
    print("-" * 80)
    for f in files:
        s = stats(load(f))
        total_seg += s["translatable"]
        total_chr += s["chars"]
        print(f"{f.name:<38} {s['format']:<5} {s['encoding']:<10} "
              f"{s['newline']:<5} {s['translatable']:>7} {s['chars']:>9}")
    print("-" * 80)
    print(f"{'TOPLAM':<38} {'':<5} {'':<10} {'':<5} {total_seg:>7} {total_chr:>9}")
    print(f"\nDosya sayisi: {len(files)}")
    return 0


def cmd_extract(args) -> int:
    src = Path(args.src)
    doc = load(src)
    items = []
    for s in doc.segments:
        if not s.translatable:
            continue
        items.append({
            "i": s.index,
            "style": s.style,
            "name": s.name,
            "start": s.start,
            "end": s.end,
            "dur": round(s.duration_s(), 2),
            "en": s.text,
            "tr": "",
        })
    out = Path(args.out) if args.out else src.with_suffix(".json")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(
        json.dumps(
            {"source": src.name, "format": doc.fmt, "count": len(items),
             "segments": items},
            ensure_ascii=False, indent=1,
        ),
        encoding="utf-8",
    )
    print(f"{len(items)} replik cikarildi -> {out}")
    return 0


def cmd_apply(args) -> int:
    src, jf = Path(args.src), Path(args.json)
    doc = load(src)
    data = json.loads(jf.read_text(encoding="utf-8"))

    tr, missing = {}, []
    for it in data["segments"]:
        val = (it.get("tr") or "").strip()
        if val:
            tr[it["i"]] = val
        else:
            missing.append(it["i"])

    if missing and not args.allow_partial:
        print(f"HATA: {len(missing)} replik cevrilmemis: {missing[:20]}")
        print("Kismi yazmak icin --allow-partial kullanin.")
        return 1

    doc.apply(tr)
    out = Path(args.out) if args.out else src.with_suffix(".tr" + src.suffix)
    doc.save(out)
    print(f"{len(tr)} replik islendi -> {out}")
    if missing:
        print(f"UYARI: {len(missing)} replik orijinal dilde birakildi.")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description="Altyazi ceviri tezgahi")
    sub = ap.add_subparsers(dest="cmd", required=True)

    p = sub.add_parser("info", help="altyazi ozeti")
    p.add_argument("path")
    p.set_defaults(fn=cmd_info)

    p = sub.add_parser("extract", help="cevrilecek metinleri cikar")
    p.add_argument("src")
    p.add_argument("-o", "--out")
    p.set_defaults(fn=cmd_extract)

    p = sub.add_parser("apply", help="ceviriyi geri isle")
    p.add_argument("src")
    p.add_argument("json")
    p.add_argument("-o", "--out")
    p.add_argument("--allow-partial", action="store_true")
    p.set_defaults(fn=cmd_apply)

    args = ap.parse_args()
    return args.fn(args)


if __name__ == "__main__":
    raise SystemExit(main())
