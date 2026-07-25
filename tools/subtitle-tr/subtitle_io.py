#!/usr/bin/env python3
"""
subtitle_io.py
==============
Altyazi dosyalarini (.srt / .ass / .ssa / .vtt) BOZMADAN okuyup yazmak icin
yardimci kutuphane.

Temel fikir: bir altyazi dosyasinda cevrilmesi gereken tek sey, replik
metnindeki *duz kelimelerdir*. Zaman kodlari, stil satirlari, ASS override
etiketleri ({\an8}, {\i1}, {\pos(...)}), cizim komutlari, HTML etiketleri
(<i>, <b>) ve satir sonu isaretleri (\\N, \\n, \\h) AYNEN korunmalidir.

Bu modul:
  * dosyayi ayristirir,
  * cevrilebilir metin parcalarini (segment) cikarir,
  * cevrilmis metinleri geri yerine koyar,
  * orijinal satir sirasi/bicimi/kodlamasi ile yeniden yazar.

Kullanim:
    from subtitle_io import load, Segment

    doc = load("bolum01.ass")
    for seg in doc.segments:
        print(seg.index, seg.text)      # sadece duz metin
    doc.apply({0: "Merhaba", 1: "Nasilsin?"})
    doc.save("bolum01.tr.ass")
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path


# --------------------------------------------------------------------------
# ASS override etiketleri ve korunacak kontrol dizileri
#   {...}            -> ASS override blogu   ({\an8}, {\i1}, {\pos(320,10)})
#   \N \n \h         -> ASS satir sonu / bosluk
#   <...>            -> HTML/SRT etiketi     (<i>, </i>, <font color="...">)
#   {\p1}...{\p0}    -> cizim modu (icindeki sayilar metin DEGILDIR)
# --------------------------------------------------------------------------
#
# ONEMLI TASARIM KARARI
# ---------------------
# Skeleton'a SADECE bicimlendirme etiketleri girer: {...} ve <...>
# Satir sonu isaretleri (\N, \n, \h) CEVIRININ PARCASIDIR ve duz metinde
# birakilir; cunku Turkce cumle uzunlugu farkli oldugu icin satir bolme
# yerinin degismesi normaldir. Cevirmen \N'leri kendisi konumlandirir.
TAG_RE = re.compile(
    r"(\{[^}]*\}"      # ASS override blogu  {\an8} {\i1} {\pos(..)}
    r"|<[^>]+>)"       # HTML/SRT etiketi    <i> </i> <font ...>
)

# Satir sonu isaretleri: skeleton'a girmez ama QA'de sayilir
BREAK_RE = re.compile(r"\\[Nnh]")

TIME_ASS = re.compile(r"^\d+:\d{2}:\d{2}\.\d{2}$")
TIME_SRT = re.compile(
    r"^\d{2}:\d{2}:\d{2}[,.]\d{3}\s*-->\s*\d{2}:\d{2}:\d{2}[,.]\d{3}"
)


@dataclass
class Segment:
    """Cevrilecek tek bir metin parcasi."""

    index: int
    text: str                       # sadece duz metin (etiketsiz)
    style: str = ""                 # ASS stil adi (Default, Sign, OP/ED...)
    name: str = ""                  # ASS konusmaci alani
    start: str = ""
    end: str = ""
    is_drawing: bool = False        # {\p1} cizim -> cevrilmez
    _skeleton: list = field(default_factory=list, repr=False)
    _line_no: int = -1

    @property
    def translatable(self) -> bool:
        if self.is_drawing:
            return False
        return bool(self.text.strip())

    def duration_s(self) -> float:
        def parse(t: str) -> float:
            t = t.replace(",", ".")
            try:
                h, m, s = t.split(":")
                return int(h) * 3600 + int(m) * 60 + float(s)
            except Exception:
                return 0.0
        if not self.start or not self.end:
            return 0.0
        return max(0.0, parse(self.end) - parse(self.start))


def _split_tags(raw: str) -> tuple[list, str]:
    """
    Ham replik metnini iskelete + duz metne ayirir.

    Iskelet: ['{\\an8}', None, '\\N', None]  -> None'lar metin yuvalari
    Duz metin: yuvalarin ' ' ile degil, ozel ayrac ile birlestirilmis hali
    degildir; her yuva ayri tutulur ama cevirmene TEK string verilir.

    Basitlik ve guvenlik icin: etiketler korunur, metin yuvalari sirayla
    doldurulur.
    """
    parts = TAG_RE.split(raw)
    skeleton: list = []
    texts: list[str] = []
    for p in parts:
        if p == "":
            continue
        if TAG_RE.fullmatch(p):
            skeleton.append(p)
        else:
            skeleton.append(None)
            texts.append(p)
    return skeleton, "".join(texts)


def _rebuild(skeleton: list, new_text: str) -> str:
    """
    Ceviriyi iskelete geri koyar.

    Metin yuvasi birden fazlaysa (ornegin '{\\i1}merhaba{\\i0} dunya'),
    ceviri TEK parca oldugu icin ilk yuvaya yazilir, digerleri bosaltilir.
    Bu, etiket sirasini bozmadan en guvenli davranistir. Cevirmenin etiket
    ici vurgulari korumasi gerekiyorsa segment 'raw' uzerinden islenmelidir.
    """
    out: list[str] = []
    placed = False
    for item in skeleton:
        if item is None:
            if not placed:
                out.append(new_text)
                placed = True
        else:
            out.append(item)
    if not placed:
        out.append(new_text)
    return "".join(out)


class SubtitleDoc:
    """Ayristirilmis altyazi dosyasi."""

    def __init__(self, path: Path, encoding: str, newline: str, fmt: str):
        self.path = path
        self.encoding = encoding
        self.newline = newline
        self.fmt = fmt                 # 'ass' | 'srt' | 'vtt'
        self.lines: list[str] = []
        self.segments: list[Segment] = []

    # ---------------- ceviri uygulama ----------------
    def apply(self, translations: dict[int, str]) -> None:
        """translations: {segment.index: 'turkce metin'}"""
        for seg in self.segments:
            if seg.index not in translations:
                continue
            new = translations[seg.index]
            if new is None:
                continue
            rebuilt = _rebuild(seg._skeleton, new)
            line = self.lines[seg._line_no]
            if self.fmt == "ass":
                # Dialogue satirinda metin 10. alandan sonrasidir
                head, sep, _ = _ass_split(line)
                self.lines[seg._line_no] = head + sep + rebuilt
            else:
                self.lines[seg._line_no] = rebuilt

    def save(self, out: str | Path) -> None:
        out = Path(out)
        out.parent.mkdir(parents=True, exist_ok=True)
        data = self.newline.join(self.lines)
        # Orijinal dosya newline ile bitiyorduysa koru
        if self._trailing_newline:
            data += self.newline
        out.write_text(data, encoding=self.encoding, newline="")

    _trailing_newline = True


def _ass_split(line: str) -> tuple[str, str, str]:
    """
    'Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Merhaba'
    -> ('Dialogue: 0,...,,0,0,0,', ',', 'Merhaba')
    ASS'te Dialogue satirinda 9 virgul vardir, metin 10. alandir.
    """
    prefix, _, rest = line.partition(":")
    fields = rest.split(",", 9)
    if len(fields) < 10:
        return line, "", ""
    head = prefix + ":" + ",".join(fields[:9])
    return head, ",", fields[9]


def load(path: str | Path) -> SubtitleDoc:
    path = Path(path)
    raw_bytes = path.read_bytes()

    # Kodlama tespiti: BOM -> utf-8-sig, degilse utf-8, olmazsa cp1254
    if raw_bytes.startswith(b"\xef\xbb\xbf"):
        encoding = "utf-8-sig"
    else:
        encoding = "utf-8"
    try:
        text = raw_bytes.decode(encoding)
    except UnicodeDecodeError:
        encoding = "cp1254"
        text = raw_bytes.decode(encoding, errors="replace")

    newline = "\r\n" if "\r\n" in text else "\n"
    trailing = text.endswith(newline)
    lines = text.split(newline)
    if trailing and lines and lines[-1] == "":
        lines.pop()

    suffix = path.suffix.lower()
    fmt = {"ass": "ass", "ssa": "ass", "srt": "srt", "vtt": "vtt"}.get(
        suffix.lstrip("."), "srt"
    )

    doc = SubtitleDoc(path, encoding, newline, fmt)
    doc.lines = lines
    doc._trailing_newline = trailing

    if fmt == "ass":
        _parse_ass(doc)
    else:
        _parse_srt(doc)
    return doc


def _parse_ass(doc: SubtitleDoc) -> None:
    idx = 0
    fmt_fields: list[str] = []
    for i, line in enumerate(doc.lines):
        stripped = line.strip()
        if stripped.lower().startswith("format:") and fmt_fields == []:
            fmt_fields = [f.strip().lower() for f in stripped[7:].split(",")]
        if not stripped.startswith("Dialogue:"):
            continue
        head, sep, text = _ass_split(line)
        if not sep:
            continue
        fields = head.partition(":")[2].split(",")
        # Standart ASS sirasi: Layer,Start,End,Style,Name,ML,MR,MV,Effect
        start = fields[1].strip() if len(fields) > 1 else ""
        end = fields[2].strip() if len(fields) > 2 else ""
        style = fields[3].strip() if len(fields) > 3 else ""
        name = fields[4].strip() if len(fields) > 4 else ""

        skeleton, plain = _split_tags(text)
        is_drawing = bool(re.search(r"\\p[1-9]", text))

        doc.segments.append(
            Segment(
                index=idx,
                text=plain,
                style=style,
                name=name,
                start=start,
                end=end,
                is_drawing=is_drawing,
                _skeleton=skeleton,
                _line_no=i,
            )
        )
        idx += 1


def _parse_srt(doc: SubtitleDoc) -> None:
    """
    SRT/VTT: her replik blogunda zaman satirindan sonraki satirlar metindir.
    Her METIN SATIRI ayri segment olur; boylece satir bolunmeleri korunur.
    """
    idx = 0
    in_text = False
    for i, line in enumerate(doc.lines):
        s = line.strip()
        if TIME_SRT.match(s) or ("-->" in s and doc.fmt == "vtt"):
            in_text = True
            continue
        if s == "":
            in_text = False
            continue
        if not in_text:
            continue
        skeleton, plain = _split_tags(line)
        doc.segments.append(
            Segment(
                index=idx,
                text=plain,
                _skeleton=skeleton,
                _line_no=i,
            )
        )
        idx += 1


def stats(doc: SubtitleDoc) -> dict:
    tr = [s for s in doc.segments if s.translatable]
    return {
        "format": doc.fmt,
        "encoding": doc.encoding,
        "newline": "CRLF" if doc.newline == "\r\n" else "LF",
        "lines": len(doc.lines),
        "segments": len(doc.segments),
        "translatable": len(tr),
        "chars": sum(len(s.text) for s in tr),
        "styles": sorted({s.style for s in doc.segments if s.style}),
    }


if __name__ == "__main__":
    import json
    import sys

    for arg in sys.argv[1:]:
        d = load(arg)
        print(arg)
        print(json.dumps(stats(d), ensure_ascii=False, indent=2))
