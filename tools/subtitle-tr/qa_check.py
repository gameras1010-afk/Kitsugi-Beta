#!/usr/bin/env python3
"""
qa_check.py
===========
Cevrilmis altyazi dosyasini ORIJINALLE karsilastirip teknik butunlugu dogrular.
Ceviri teslim edilmeden once mutlaka calistirilmalidir.

Kontroller
----------
 1. Satir sayisi ayni mi?                    (yapisal bozulma)
 2. Zaman kodlari birebir ayni mi?           (senkron kaymasi)
 3. Stil / [V4+ Styles] blogu degismis mi?   (gorunum bozulmasi)
 4. ASS override etiketleri ({\an8} vb.) korunmus mu?
 5. Replik sayisi ayni mi?
 6. Bos kalan (cevrilmemis) replik var mi?
 7. Hala Ingilizce kalmis replik var mi?     (atlanan satir)
 8. Satir uzunlugu / okuma hizi (CPS) sinir asimi
 9. Kodlama ve satir sonu (CRLF/LF) korunmus mu?

Kullanim:
    python3 qa_check.py orijinal.ass cevrilmis.tr.ass
    python3 qa_check.py --dir kaynak/ --dir-tr cikti/
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from subtitle_io import BREAK_RE, TAG_RE, load

MAX_CPS = 21          # saniyede karakter
MAX_LINE_LEN = 45     # satir basina karakter

# Ingilizce kalinti tespiti icin sik kullanilan islev kelimeleri
EN_HINT = re.compile(
    r"\b(the|and|you|your|that|this|with|have|what|don't|it's|I'm|we're|"
    r"they|there|about|would|could|should|because|from|know|going)\b",
    re.IGNORECASE,
)
TR_HINT = re.compile(r"[çğıöşüÇĞİÖŞÜ]|\b(bir|ve|bu|için|değil|ama|çok)\b", re.I)


class Report:
    def __init__(self, name: str):
        self.name = name
        self.errors: list[str] = []
        self.warnings: list[str] = []

    def err(self, m: str) -> None:
        self.errors.append(m)

    def warn(self, m: str) -> None:
        self.warnings.append(m)

    @property
    def ok(self) -> bool:
        return not self.errors

    def render(self) -> str:
        icon = "OK  " if self.ok else "HATA"
        out = [f"[{icon}] {self.name}"]
        for e in self.errors:
            out.append(f"    x  {e}")
        for w in self.warnings[:15]:
            out.append(f"    !  {w}")
        if len(self.warnings) > 15:
            out.append(f"    !  ... ve {len(self.warnings)-15} uyari daha")
        return "\n".join(out)


def tags_of(raw_line: str) -> list[str]:
    return TAG_RE.findall(raw_line)


def check(src_path: Path, tr_path: Path) -> Report:
    r = Report(f"{src_path.name}  ->  {tr_path.name}")

    if not tr_path.exists():
        r.err("cevrilmis dosya bulunamadi")
        return r

    a = load(src_path)
    b = load(tr_path)

    # 1) satir sayisi
    if len(a.lines) != len(b.lines):
        r.err(f"satir sayisi farkli: {len(a.lines)} -> {len(b.lines)}")

    # 9) kodlama / satir sonu
    if a.newline != b.newline:
        r.err("satir sonu bicimi degismis (CRLF/LF)")
    if a.encoding != b.encoding:
        r.warn(f"kodlama degismis: {a.encoding} -> {b.encoding}")

    # 5) replik sayisi
    if len(a.segments) != len(b.segments):
        r.err(f"replik sayisi farkli: {len(a.segments)} -> {len(b.segments)}")
        return r

    # 2/3/4) zaman kodu, stil, etiket
    for sa, sb in zip(a.segments, b.segments):
        if sa.start != sb.start or sa.end != sb.end:
            r.err(f"#{sa.index}: zaman kodu degismis "
                  f"({sa.start}-{sa.end} -> {sb.start}-{sb.end})")
        if sa.style != sb.style:
            r.err(f"#{sa.index}: stil degismis ({sa.style} -> {sb.style})")

        ta = [t for t in sa._skeleton if t is not None]
        tb = [t for t in sb._skeleton if t is not None]
        if ta != tb:
            r.err(f"#{sa.index}: etiketler degismis {ta} -> {tb}")

        # Satir sonu sayisi: 2 satirdan fazlaya bolmek altyaziyi bozar
        nb = len(BREAK_RE.findall(sb.text))
        if nb > 1:
            r.warn(f"#{sa.index}: {nb+1} satira bolunmus (en fazla 2 olmali)")

    # stil/baslik bloklari birebir
    if a.fmt == "ass":
        def header(doc):
            out = []
            for ln in doc.lines:
                s = ln.strip()
                if s.startswith("Dialogue:"):
                    continue
                out.append(ln)
            return out
        ha, hb = header(a), header(b)
        if ha != hb:
            n = max(len(ha), len(hb))
            diff = [
                i for i in range(n)
                if (ha[i] if i < len(ha) else None)
                != (hb[i] if i < len(hb) else None)
            ]
            r.err(f"[Script Info]/[V4+ Styles] blogu degismis "
                  f"({len(diff)} satir farkli)")
            for i in diff[:3]:
                old = ha[i] if i < len(ha) else "(yok)"
                new = hb[i] if i < len(hb) else "(yok)"
                r.err(f"    satir {i}: {old[:50]!r} -> {new[:50]!r}")

    # 6/7/8) icerik kalitesi
    for sa, sb in zip(a.segments, b.segments):
        if not sa.translatable:
            continue
        if not sb.text.strip():
            r.err(f"#{sa.index}: ceviri bos birakilmis  ({sa.text[:40]!r})")
            continue
        if sb.text.strip() == sa.text.strip() and len(sa.text) > 12:
            if EN_HINT.search(sb.text) and not TR_HINT.search(sb.text):
                r.warn(f"#{sa.index}: cevrilmemis olabilir  ({sb.text[:45]!r})")

        for ln in re.split(r"\\N|\n", sb.text):
            if len(ln) > MAX_LINE_LEN:
                r.warn(f"#{sa.index}: satir {len(ln)} karakter "
                       f"(sinir {MAX_LINE_LEN})")
        dur = sb.duration_s()
        if dur > 0.3:
            cps = len(sb.text.replace("\\N", "")) / dur
            if cps > MAX_CPS:
                r.warn(f"#{sa.index}: okuma hizi {cps:.0f} cps "
                       f"(sinir {MAX_CPS})")
    return r


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("src", nargs="?", help="orijinal altyazi")
    ap.add_argument("tr", nargs="?", help="cevrilmis altyazi")
    ap.add_argument("--dir", help="orijinal klasor")
    ap.add_argument("--dir-tr", help="cevrilmis klasor")
    args = ap.parse_args()

    reports: list[Report] = []
    if args.dir:
        srcdir, trdir = Path(args.dir), Path(args.dir_tr)
        exts = {".srt", ".ass", ".ssa", ".vtt"}
        for p in sorted(srcdir.rglob("*")):
            if p.suffix.lower() not in exts:
                continue
            rel = p.relative_to(srcdir)
            cand = trdir / rel
            if not cand.exists():
                stem = rel.stem
                hits = list(trdir.rglob(f"{stem}*{p.suffix}"))
                if hits:
                    cand = hits[0]
            reports.append(check(p, cand))
    elif args.src and args.tr:
        reports.append(check(Path(args.src), Path(args.tr)))
    else:
        ap.error("ya (src tr) ya da (--dir --dir-tr) verin")

    for rep in reports:
        print(rep.render())

    bad = [r for r in reports if not r.ok]
    warns = sum(len(r.warnings) for r in reports)
    print(f"\n{'='*58}")
    print(f"Dosya: {len(reports)} | Temiz: {len(reports)-len(bad)} | "
          f"Hatali: {len(bad)} | Uyari: {warns}")
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
