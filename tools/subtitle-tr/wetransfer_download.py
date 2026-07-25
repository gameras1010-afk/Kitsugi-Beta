#!/usr/bin/env python3
"""
wetransfer_download.py
======================
WeTransfer (we.tl / wetransfer.com) linkinden dosyayi indirir ve arsivi acar.

Kullanim:
    python3 wetransfer_download.py "https://we.tl/t-XXXXXXXX" -o ./indirilen

Notlar
------
WeTransfer indirme linki iki adimlidir:
  1) Kisa link (we.tl/t-...) -> https://wetransfer.com/downloads/<transfer_id>/<security_hash>
     (bazen .../downloads/<transfer_id>/<recipient_id>/<security_hash>)
  2) POST /api/v4/transfers/<transfer_id>/download  ->  {"direct_link": "https://..."}
     Bu POST icin sayfadan alinan CSRF token + cookie gerekir.

Bu script her iki adimi da otomatik yapar. Sadece `requests` gerekir:
    pip install requests
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import zipfile
from pathlib import Path

try:
    import requests
except ImportError:  # pragma: no cover
    sys.exit("HATA: 'requests' kurulu degil.  ->  pip install requests")


UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
)

# .../downloads/<transfer_id>[/<recipient_id>]/<security_hash>
DL_RE = re.compile(
    r"/downloads/(?P<tid>[0-9a-zA-Z]+)"
    r"(?:/(?P<rid>[0-9a-zA-Z]+))?"
    r"/(?P<hash>[0-9a-zA-Z]+)"
)

CSRF_RE = re.compile(
    r"""<meta[^>]+name=["']csrf-token["'][^>]+content=["']([^"']+)["']""",
    re.IGNORECASE,
)


def resolve(session: requests.Session, url: str) -> str:
    """Kisa linki (we.tl) tam indirme sayfasi URL'sine cevirir."""
    r = session.get(url, allow_redirects=True, timeout=60)
    r.raise_for_status()
    return r.url


def direct_link(session: requests.Session, page_url: str) -> str:
    """Indirme sayfasindan gercek (S3) dosya linkini uretir."""
    m = DL_RE.search(page_url)
    if not m:
        raise RuntimeError(f"Link taninamadi: {page_url}")
    tid, rid, shash = m.group("tid"), m.group("rid"), m.group("hash")

    page = session.get(page_url, timeout=60)
    page.raise_for_status()

    csrf_match = CSRF_RE.search(page.text)
    csrf = csrf_match.group(1) if csrf_match else ""

    payload: dict[str, object] = {"intent": "entire_transfer", "security_hash": shash}
    if rid:
        payload["recipient_id"] = rid

    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Origin": "https://wetransfer.com",
        "Referer": page_url,
        "x-requested-with": "XMLHttpRequest",
    }
    if csrf:
        headers["x-csrf-token"] = csrf

    api = f"https://wetransfer.com/api/v4/transfers/{tid}/download"
    r = session.post(api, data=json.dumps(payload), headers=headers, timeout=60)
    r.raise_for_status()
    data = r.json()

    link = data.get("direct_link") or data.get("url")
    if not link:
        raise RuntimeError(f"direct_link alinamadi. Yanit: {data}")
    return link


def download(session: requests.Session, url: str, outdir: Path) -> Path:
    outdir.mkdir(parents=True, exist_ok=True)
    with session.get(url, stream=True, timeout=300) as r:
        r.raise_for_status()

        name = "transfer.zip"
        cd = r.headers.get("content-disposition", "")
        fn = re.search(r'filename\*?=(?:UTF-8\'\')?"?([^";]+)"?', cd)
        if fn:
            name = os.path.basename(fn.group(1))
        elif "/" in url:
            cand = os.path.basename(url.split("?")[0])
            if cand:
                name = cand

        dest = outdir / name
        total = int(r.headers.get("content-length") or 0)
        done = 0
        with open(dest, "wb") as f:
            for chunk in r.iter_content(1 << 20):
                if not chunk:
                    continue
                f.write(chunk)
                done += len(chunk)
                if total:
                    pct = done * 100 // total
                    print(f"\r  indiriliyor: {pct:3d}%  ({done/1e6:.1f} MB)", end="")
        print()
    return dest


def extract(archive: Path, outdir: Path) -> None:
    """Zip ise acar; ic ice zip'leri de acar."""
    if not zipfile.is_zipfile(archive):
        print(f"  (arsiv degil, oldugu gibi birakildi: {archive.name})")
        return
    target = outdir / archive.stem
    target.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive) as z:
        z.extractall(target)
    print(f"  acildi -> {target}")
    for inner in target.rglob("*.zip"):
        extract(inner, inner.parent)


def main() -> int:
    ap = argparse.ArgumentParser(description="WeTransfer indirici")
    ap.add_argument("url", help="we.tl veya wetransfer.com/downloads linki")
    ap.add_argument("-o", "--out", default="./indirilen", help="hedef klasor")
    args = ap.parse_args()

    outdir = Path(args.out)
    s = requests.Session()
    s.headers.update({"User-Agent": UA, "Accept-Language": "tr,en;q=0.9"})

    print("[1/4] Link cozuluyor...")
    page = resolve(s, args.url)
    print(f"      {page}")

    print("[2/4] Dogrudan indirme linki aliniyor...")
    dl = direct_link(s, page)
    print(f"      {dl[:90]}...")

    print("[3/4] Indiriliyor...")
    archive = download(s, dl, outdir)
    print(f"      kaydedildi -> {archive}")

    print("[4/4] Arsiv aciliyor...")
    extract(archive, outdir)

    subs = [
        p for p in outdir.rglob("*")
        if p.suffix.lower() in {".srt", ".ass", ".ssa", ".vtt", ".sub"}
    ]
    print(f"\nBulunan altyazi dosyasi: {len(subs)}")
    for p in sorted(subs):
        print("   ", p.relative_to(outdir))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
