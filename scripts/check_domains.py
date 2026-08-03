#!/usr/bin/env python3
"""
Kitsugi AI Destekli Domain Tarayıcı
=====================================
Türk CS3 eklentilerinin domainlerini otomatik olarak keşfeder ve günceller.

Çalışma akışı:
  1. Tüm Türk CS3 repolarından eklenti listesini çeker
  2. Her eklentinin Kotlin kaynak kodundan mainUrl'i okur
  3. HTTP probe ile domain'in yaşıyor mu kontrol eder
  4. Domain ÖLÜYSE → OpenRouter AI'a "Bu sitenin yeni adresi ne?" diye sorar
  5. AI'ın önerdiği adres probe edilir, geçerliyse domain_fixes.json'a yazılır
  6. Değişiklikler commit edilmek üzere hazırlanır

Kullanım:
  pip install requests
  python scripts/check_domains.py

GitHub Actions Secret: OPENROUTER_API_KEYS (satır satır key listesi)
"""

import json
import os
import random
import re
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed

# Reconfigure stdout/stderr for unicode/emoji support in terminal
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except AttributeError:
        pass

try:
    import requests
except ImportError:
    print("❌ 'requests' modülü bulunamadı. Lütfen: pip install requests")
    sys.exit(1)

# ── Yapılandırma ─────────────────────────────────────────────────────────────

DOMAIN_FIXES_PATH = Path(__file__).parent.parent / "domain_fixes.json"
API_KEYS_PATH     = Path(__file__).parent / "openrouter_keys.txt"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                  "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
}

TIMEOUT      = 12   # saniye (HTTP probe)
AI_TIMEOUT   = 30   # saniye (AI isteği)
MAX_WORKERS  = 16   # paralel HTTP probe sayısı

# AI Model — ücretsiz ve hızlı olanlar (OpenRouter üzerinden)
AI_MODELS = [
    "meta-llama/llama-3.3-70b-instruct:free",
    "google/gemma-3-27b-it:free",
    "mistralai/mistral-7b-instruct:free",
    "microsoft/phi-3-mini-128k-instruct:free",
]

# Tüm Türk CS3 plugin repoları
REPOS = [
    ("maarrem/cs-Kekik",
     "https://raw.githubusercontent.com/maarrem/cs-Kekik/master/repo.json"),
    ("feroxx/Kekik-cloudstream",
     "https://raw.githubusercontent.com/feroxx/Kekik-cloudstream/refs/heads/builds/repo.json"),
    ("Kraptor123/cs-kraptor",
     "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/refs/heads/master/repo.json"),
    ("Kraptor123/Cs-Karma",
     "https://raw.githubusercontent.com/Kraptor123/Cs-Karma/refs/heads/master/repo.json"),
    ("nikyokki/nik-cloudstream",
     "https://raw.githubusercontent.com/nikyokki/nik-cloudstream/master/repo.json"),
    ("ByAyzen/AyzenCS3",
     "https://raw.githubusercontent.com/ByAyzen/AyzenCS3/refs/heads/builds/repo.json"),
    ("Kraptor123/cs-kekikanime",
     "https://raw.githubusercontent.com/Kraptor123/cs-kekikanime/master/repo.json"),
    ("sarapcanagii/Pitipitii",
     "https://raw.githubusercontent.com/sarapcanagii/Pitipitii/main/repo.json"),
    ("Kraptor123/Cs-GizliKeyif",
     "https://raw.githubusercontent.com/Kraptor123/Cs-GizliKeyif/refs/heads/master/repo.json"),
    ("Sertel392/Makotogecici",
     "https://raw.githubusercontent.com/Sertel392/Makotogecici/main/repo.json"),
    ("caca1403/cloudstream-cagi-eklenti",
     "https://raw.githubusercontent.com/caca1403/cloudstream-cagi-eklenti/main/repo.json"),
]

REPO_SOURCE_BASES = {
    "maarrem/cs-Kekik":
        "https://raw.githubusercontent.com/maarrem/cs-Kekik/master",
    "feroxx/Kekik-cloudstream":
        "https://raw.githubusercontent.com/feroxx/Kekik-cloudstream/master",
    "Kraptor123/cs-kraptor":
        "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/master",
    "Kraptor123/Cs-Karma":
        "https://raw.githubusercontent.com/Kraptor123/Cs-Karma/master",
    "nikyokki/nik-cloudstream":
        "https://raw.githubusercontent.com/nikyokki/nik-cloudstream/master",
    "ByAyzen/AyzenCS3":
        "https://raw.githubusercontent.com/ByAyzen/AyzenCS3/main",
    "Kraptor123/cs-kekikanime":
        "https://raw.githubusercontent.com/Kraptor123/cs-kekikanime/master",
    "sarapcanagii/Pitipitii":
        "https://raw.githubusercontent.com/sarapcanagii/Pitipitii/main",
    "Kraptor123/Cs-GizliKeyif":
        "https://raw.githubusercontent.com/Kraptor123/Cs-GizliKeyif/master",
    "Sertel392/Makotogecici":
        "https://raw.githubusercontent.com/Sertel392/Makotogecici/main",
    "caca1403/cloudstream-cagi-eklenti":
        "https://raw.githubusercontent.com/caca1403/cloudstream-cagi-eklenti/main",
}

SEARCH_UNSUPPORTED = {
    "CanliTV", "M3UPlayer", "Vavoo", "Syncler", "KickTR", "NeonSpor",
    "vavooSpor", "powerSinema", "powerDizi", "TLCtr", "TLC", "DMax",
    "GinikoCanli", "Atv", "KanalD", "NowTv", "ShowTv", "StarTv",
    "Teve2", "Trt1", "TrtCocuk", "Tv8", "OnePaceTr",
}

ADULT_PLUGINS = {
    "FullPorner", "HdAbla", "Hqporner", "IfsaLog", "Kalite18",
    "PornoAnne", "XNXX", "Xhamster", "HentaizmManga",
}

MAIN_URL_PATTERN = re.compile(
    r'override\s+var\s+mainUrl\s*=\s*["\']([^"\']+)["\']',
    re.IGNORECASE
)
URL_PATTERN = re.compile(r'https?://[^\s\'"<>]+')

# ── OpenRouter API Key Yönetimi ──────────────────────────────────────────────

class KeyPool:
    """122 API key'i round-robin olarak yönetir."""
    def __init__(self):
        self.keys = self._load_keys()
        self._index = 0
        print(f"🔑 {len(self.keys)} OpenRouter API key yüklendi.")

    def _load_keys(self) -> list[str]:
        keys = []

        # 1. GitHub Actions Secret (OPENROUTER_API_KEYS env değişkeni)
        env_keys = os.environ.get("OPENROUTER_API_KEYS", "")
        if env_keys:
            for line in env_keys.splitlines():
                k = line.strip()
                if k.startswith("sk-or-"):
                    keys.append(k)
            if keys:
                return keys

        # 2. Yerel dosyadan oku (scripts/openrouter_keys.txt)
        if API_KEYS_PATH.exists():
            with open(API_KEYS_PATH, encoding="utf-8") as f:
                for line in f:
                    k = line.strip()
                    if k.startswith("sk-or-"):
                        keys.append(k)
            if keys:
                return keys

        print("⚠️  OpenRouter API key bulunamadı — AI desteği devre dışı.")
        return []

    def next_key(self) -> str | None:
        if not self.keys:
            return None
        key = self.keys[self._index % len(self.keys)]
        self._index += 1
        return key

    def has_keys(self) -> bool:
        return len(self.keys) > 0


# ── AI Domain Keşfetme ───────────────────────────────────────────────────────

def ask_ai_for_domain(plugin_name: str, old_url: str, key_pool: KeyPool) -> str | None:
    """
    OpenRouter üzerinden AI'a ölü domain'in yeni adresini sorar.
    Birden fazla key ve model dener.
    """
    if not key_pool.has_keys():
        return None

    prompt = (
        f"Sen Türk dizi/film/anime sitelerini takip eden bir uzmansın.\n\n"
        f"Cloudstream eklentisi '{plugin_name}' için eski site adresi: {old_url}\n\n"
        f"Bu site erişilemiyor veya kapanmış olabilir. "
        f"Bu sitenin güncel çalışan yeni adresi nedir?\n\n"
        f"SADECE URL'yi ver, başka hiçbir şey yazma. "
        f"Eğer kesin olarak bilmiyorsan 'BILINMIYOR' yaz.\n"
        f"Örnek cevap: https://yeniadres.com"
    )

    for model in AI_MODELS:
        key = key_pool.next_key()
        if not key:
            break
        try:
            resp = requests.post(
                "https://openrouter.ai/api/v1/chat/completions",
                headers={
                    "Authorization": f"Bearer {key}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "https://github.com/gameras1010-afk/Kitsugi-Beta",
                    "X-Title": "Kitsugi Domain Updater",
                },
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": prompt}],
                    "max_tokens": 60,
                    "temperature": 0.1,
                },
                timeout=AI_TIMEOUT,
            )
            if resp.status_code != 200:
                continue

            answer = resp.json()["choices"][0]["message"]["content"].strip()
            if "BILINMIYOR" in answer.upper() or not answer.startswith("http"):
                # URL içeriyor mu kontrol et
                urls = URL_PATTERN.findall(answer)
                if urls:
                    return urls[0].rstrip(".,;)")
                return None

            return answer.split()[0].rstrip(".,;)")

        except Exception as e:
            print(f"    ⚠️  AI isteği başarısız [{model}]: {e}")
            continue

    return None


# ── HTTP Yardımcıları ────────────────────────────────────────────────────────

def fetch_json(url: str) -> dict | list | None:
    try:
        r = requests.get(url, headers=HEADERS, timeout=TIMEOUT)
        r.raise_for_status()
        return r.json()
    except Exception:
        return None


def fetch_text(url: str) -> str | None:
    try:
        r = requests.get(url, headers=HEADERS, timeout=TIMEOUT)
        r.raise_for_status()
        return r.text
    except Exception:
        return None


def probe_domain(url: str) -> tuple[bool, int]:
    """Domain'e istek atar. (alive, http_code) döndürür."""
    if not url or not url.startswith("http"):
        return False, -1
    try:
        r = requests.head(url, headers=HEADERS, timeout=TIMEOUT,
                          allow_redirects=True)
        code = r.status_code
        # 403 = Cloudflare var ama site ayakta → alive
        # 522/523 = CF origin down → dead
        alive = code < 500 or code == 403
        return alive, code
    except requests.exceptions.ConnectionError:
        return False, -1
    except requests.exceptions.Timeout:
        return False, -2
    except Exception:
        return False, -3


# ── Repo & Plugin Toplama ────────────────────────────────────────────────────

def get_plugins_from_repo(repo_name: str, repo_json_url: str) -> list[dict]:
    plugins = []
    data = fetch_json(repo_json_url)
    if not data:
        return plugins

    plugin_list_urls = data.get("pluginLists", []) if isinstance(data, dict) else []

    for list_url in plugin_list_urls:
        items = fetch_json(list_url)
        if not isinstance(items, list):
            continue
        for item in items:
            name     = (item.get("name") or item.get("internalName") or "").strip()
            internal = (item.get("internalName") or name).strip()
            status   = item.get("status", 1)

            if status == 3 or not name:
                continue
            if name in ADULT_PLUGINS or internal in ADULT_PLUGINS:
                continue
            if name in SEARCH_UNSUPPORTED or internal in SEARCH_UNSUPPORTED:
                continue

            plugins.append({
                "repo": repo_name,
                "name": name,
                "internal": internal,
                "tv_types": item.get("tvTypes", []),
            })
    return plugins


def extract_main_url_from_source(repo: str, plugin_name: str) -> tuple[str | None, str | None]:
    """
    Kotlin kaynak kodundan mainUrl çıkarır.
    Döndürür: (url, kaynak_kod_metni)
    """
    base = REPO_SOURCE_BASES.get(repo)
    if not base:
        return None, None

    candidates = [
        f"{base}/{plugin_name}/{plugin_name}.kt",
        f"{base}/{plugin_name}/src/main/kotlin/{plugin_name}.kt",
        f"{base}/{plugin_name}/src/{plugin_name}.kt",
        f"{base}/{plugin_name}/{plugin_name}Plugin.kt",
    ]

    for url in candidates:
        text = fetch_text(url)
        if not text:
            continue
        match = MAIN_URL_PATTERN.search(text)
        if match:
            found = match.group(1).strip().rstrip("/")
            if found.startswith("http"):
                return found, text

    return None, None


# ── Ana Domain Çözümleme ─────────────────────────────────────────────────────

def resolve_domain(key: str, info: dict, existing: dict,
                   key_pool: KeyPool) -> tuple[str, str | None, str]:
    """
    Tek bir eklenti için güncel domain belirler:
    1. Kaynak koddan oku → probe et
    2. Ölüyse → AI'a sor → probe et
    3. Hâlâ yoksa mevcut kaydı koru
    """
    name  = info["name"]
    repos = info["repos"]

    source_url = None
    for repo in repos:
        u, _ = extract_main_url_from_source(repo, name)
        if u:
            source_url = u
            break

    # 1. Kaynak koddan bulunan URL'yi probe et
    if source_url:
        alive, code = probe_domain(source_url)
        if alive:
            return key, source_url, f"✅ [{code}] {source_url}"

        # Ölü → AI'a sor
        ai_url = ask_ai_for_domain(name, source_url, key_pool)
        if ai_url:
            ai_alive, ai_code = probe_domain(ai_url)
            if ai_alive:
                return key, ai_url, f"🤖 AI buldu [{ai_code}] {ai_url}"
            else:
                return key, None, f"🤖 AI önerdi ama dead [{ai_code}]: {ai_url}"

        # AI de bulamadı → mevcut kaydı dene
        existing_url = existing.get(key)
        if existing_url and existing_url != source_url:
            alive2, code2 = probe_domain(existing_url)
            if alive2:
                return key, existing_url, f"⚡ Mevcut canlı [{code2}] {existing_url}"

        return key, None, f"💀 Dead [{code}], AI bulamadı: {source_url}"

    # 2. Kaynak kodu bulunamadı → mevcut kaydı kullan
    existing_url = existing.get(key)
    if existing_url:
        alive, code = probe_domain(existing_url)
        if alive:
            return key, existing_url, f"🔒 Kaynak yok, mevcut canlı [{code}]"
        else:
            return key, None, f"💀 Kaynak yok + mevcut dead [{code}]"

    return key, None, "❓ Kaynak yok, mevcut kayıt yok"


# ── Ana Program ──────────────────────────────────────────────────────────────

def load_existing() -> dict:
    if DOMAIN_FIXES_PATH.exists():
        with open(DOMAIN_FIXES_PATH, encoding="utf-8") as f:
            return json.load(f).get("domains", {})
    return {}


def run():
    print("=" * 68)
    print("  🤖 Kitsugi AI Destekli Domain Tarayıcı")
    print(f"  Başlangıç: {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}")
    print("=" * 68)

    key_pool = KeyPool()
    existing = load_existing()
    print(f"\n📄 Mevcut domain_fixes.json: {len(existing)} kayıt\n")

    # Tüm repolardan eklenti topla
    print("── 1. Repo Taraması " + "─" * 48)
    plugin_map: dict[str, dict] = {}
    for repo_name, repo_url in REPOS:
        print(f"  📦 {repo_name}")
        plugins = get_plugins_from_repo(repo_name, repo_url)
        print(f"     → {len(plugins)} eklenti")
        for p in plugins:
            k = p["name"].lower()
            if k not in plugin_map:
                plugin_map[k] = {"name": p["name"], "repos": [p["repo"]], "tv_types": p["tv_types"]}
            elif p["repo"] not in plugin_map[k]["repos"]:
                plugin_map[k]["repos"].append(p["repo"])

    print(f"\n📊 Toplam {len(plugin_map)} benzersiz eklenti bulundu\n")

    # Domain çözümleme (paralel HTTP probe, seri AI sorguları)
    print("── 2. Domain Doğrulama " + "─" * 44)
    new_domains: dict[str, str] = {}

    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as ex:
        futures = {
            ex.submit(resolve_domain, k, info, existing, key_pool): k
            for k, info in plugin_map.items()
        }
        for future in as_completed(futures):
            try:
                k, url, note = future.result()
                icon = "✅" if url else ("🤖" if "AI" in note else "💀")
                print(f"  {icon} {k:<32} {note}")
                if url:
                    new_domains[k] = url
            except Exception as e:
                print(f"  ❌ {futures[future]}: {e}")

    # Repoda artık olmayan ama hâlâ canlı olan mevcut kayıtları koru
    for k, url in existing.items():
        if k not in new_domains:
            alive, code = probe_domain(url)
            if alive:
                new_domains[k] = url
                print(f"  🔒 {k:<32} Repoda yok ama canlı [{code}] → korundu")

    # JSON güncelle
    print("\n── 3. Sonuç " + "─" * 54)
    added   = [k for k in new_domains if k not in existing]
    removed = [k for k in existing   if k not in new_domains]
    changed = [k for k in new_domains if k in existing and new_domains[k] != existing[k]]
    blocked = sorted([k for k in plugin_map if k not in new_domains])

    print(f"  Önceki: {len(existing)} | Yeni: {len(new_domains)} | "
          f"➕{len(added)} ➖{len(removed)} 🔄{len(changed)} | 🚫{len(blocked)} blocked")

    if changed:
        print("\n  🔄 Değişen domainler:")
        for k in changed:
            print(f"     {k}: {existing[k]}\n     {'':5}→ {new_domains[k]}")

    output = {
        "_comment": "Kitsugi Eklenti Domain Listesi — GitHub Actions + AI tarafından otomatik güncellenir.",
        "_format":  "eklentiAdi (küçük harf) → güncel ana URL",
        "_updated": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
        "_stats": {
            "total": len(new_domains),
            "added": len(added),
            "removed": len(removed),
            "changed": len(changed),
            "ai_assisted": sum(1 for k in new_domains if k not in existing),
            "blocked": len(blocked),
        },
        "domains": dict(sorted(new_domains.items())),
        "blocked": blocked,
    }

    with open(DOMAIN_FIXES_PATH, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print(f"\n✅ domain_fixes.json güncellendi — {len(new_domains)} eklenti kayıtlı.")
    print("=" * 68)


if __name__ == "__main__":
    run()
