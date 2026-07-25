# Zom 100 — Altyazı Çeviri Seti (EN → TR)

Bu klasör, **Zom 100: Bucket List of the Dead** altyazılarını İngilizceden Türkçeye
altyazı dosya yapısını **hiç bozmadan** çevirmek için hazırlanmış araç setidir.

> **Durum:** Araçlar hazır ve test edildi. Ancak WeTransfer linkindeki dosyalar
> **indirilemedi** — ayrıntı için aşağıdaki "Bilinen engel" bölümüne bakın.

---

## Bilinen engel: dosyalar indirilemedi

`https://we.tl/t-gCdCrXvuE1vLKhG1` linki geçerli ve şuraya çözülüyor:

```
https://wetransfer.com/downloads/ed0dc7dedd262cd7955f3dd3455a28d120260725033033/afad40
```

Ancak bu ortamın ağı **kısıtlı**: yalnızca paket deposu adresleri (pypi.org,
registry.npmjs.org, github.com) açık. `wetransfer.com` dahil genel internet
erişimi güvenlik duvarı tarafından engelleniyor:

```
$ curl -o /dev/null -w "%{http_code}" https://wetransfer.com
000                          # bağlantı TLS aşamasında kesiliyor
$ curl -o /dev/null -w "%{http_code}" https://pypi.org
200                          # sadece paket depoları açık
```

Ayrıca WeTransfer indirmesi, sayfadan alınan CSRF token'ı ile yapılan bir
`POST /api/v4/transfers/<id>/download` isteği gerektirir; salt-okunur sayfa
getirme yöntemleriyle bu adım tamamlanamaz.

**Çözüm:** Altyazı dosyalarını doğrudan bu depoya (örn. `tools/subtitle-tr/kaynak/`)
yükleyin ya da bir `.zip` olarak sohbete ekleyin. Dosyalar elime geçtiği anda
aşağıdaki iş akışıyla bölüm bölüm, sırayla çeviririm.

Ağ erişimi olan bir makinede indirmek için hazır script:

```bash
pip install requests
python3 wetransfer_download.py "https://we.tl/t-gCdCrXvuE1vLKhG1" -o ./kaynak
```

---

## Neden bu araç seti?

Altyazı çevirisinde en sık yapılan hata, çeviriyi doğrudan dosya üzerinde
yapıp **zaman kodlarını, ASS etiketlerini veya stil bloklarını bozmaktır.**
Bu set bunu yapısal olarak imkânsız kılar:

* Çevirmen **yalnızca düz metni** görür (JSON içinde `en` → `tr`).
* Zaman kodları, `{\an8}` / `{\i1}` / `{\pos(...)}` etiketleri, `[V4+ Styles]`
  bloğu, dosya kodlaması (UTF-8 BOM) ve satır sonu biçimi (CRLF/LF)
  **hiç ellenmeden** program tarafından geri yazılır.
* Teslimden önce `qa_check.py` orijinalle satır satır karşılaştırır.

### Doğrulanmış davranış

| Test | Sonuç |
|---|---|
| ASS oku → yaz (değişiklik yok) | **bayt bayt birebir aynı** |
| SRT oku → yaz (değişiklik yok) | **bayt bayt birebir aynı** |
| UTF-8 BOM + CRLF korunması | korunuyor |
| `{\an8}`, `{\i1}...{\i0}` etiketleri | korunuyor |
| Bozuk dosyayı QA yakalıyor mu? | zaman kayması, silinen etiket, değişen stil, eklenen satır — **hepsi yakalandı** |

---

## İş akışı

```bash
# 1) Elimizde ne var?
python3 workbench.py info kaynak/

# 2) Bölümün metinlerini çıkar
python3 workbench.py extract kaynak/ep01.ass -o calisma/ep01.json

# 3) calisma/ep01.json içindeki "tr" alanlarını doldur
#    (her kayıtta konuşmacı adı, süre ve saniye bilgisi de var)

# 4) Çeviriyi orijinal dosyaya geri işle
python3 workbench.py apply kaynak/ep01.ass calisma/ep01.json -o cikti/ep01.tr.ass

# 5) Teknik bütünlük kontrolü — teslimden önce ZORUNLU
python3 qa_check.py kaynak/ep01.ass cikti/ep01.tr.ass

# Toplu kontrol
python3 qa_check.py --dir kaynak/ --dir-tr cikti/
```

`apply`, çevrilmemiş replik kalırsa **yazmayı reddeder** (`--allow-partial` ile
zorlanabilir) — yani yarım çeviri yanlışlıkla teslim edilemez.

---

## Dosyalar

| Dosya | İşlevi |
|---|---|
| `subtitle_io.py` | ASS/SSA/SRT/VTT ayrıştırıcı. Etiket-metin ayrımı ve kayıpsız geri yazma. |
| `workbench.py` | `info` / `extract` / `apply` komutları. |
| `qa_check.py` | Orijinal ↔ çeviri bütünlük denetimi. |
| `glossary_zom100.json` | Özel ad ve terim sözlüğü + üslup kuralları. |
| `wetransfer_download.py` | WeTransfer indirici (ağ erişimi olan makine için). |

---

## Çeviri ilkeleri

`glossary_zom100.json` içindeki kurallar Wikipedia ve dizi wiki'lerinden
doğrulanmış künye bilgisine dayanır:

**Özel adlar** — Kişi ve yer adları çevrilmez, Türkçe ekler kesme işaretiyle
ayrılır: *Akira'nın, Kencho'yla, Shizuka'ya, Beatrix'in*. Makronlar altyazıda
kullanılmaz: Tendō → **Tendo**, Ryūzaki → **Ryuzaki**.

**Ana kadro** — Akira Tendo (天道 輝), Shizuka Mikazuki (三日月 閑),
Kenichiro "Kencho" Ryuzaki (竜崎 憲一朗), Beatrix "Bea" Amerhauser,
Takeru "Takemina" Minakata, Izuna Tokage.

**Kilit terimler** — *Bucket List* → **Yapılacaklar Listesi** (asla "kova
listesi"), *corporate slave* → **şirket kölesi**, *black company* → **insan
öğüten şirket**, *zombie* → **zombi** (tek i), *Akirager* korunur.

**Bölüm başlıkları** — "… of the Dead" kalıbı dizinin markasıdır; 12 bölümün
tamamında tutarlı biçimde korunur.

**Karakter sesleri** — Akira coşkulu ve samimi; Shizuka kısa, kesik, soğuk;
Kencho laubali ve şakacı; Beatrix aşırı kibar, kitabi bir Türkçe konuşur.
Bu ayrım çeviride korunur — herkes aynı ağızdan konuşmaz.

**Teknik ölçüt** — satır başına ~42 karakter, replik başına en fazla 2 satır,
okuma hızı en fazla ~21 karakter/saniye. İngilizce cümle uzunsa Türkçede
anlamı koruyarak sıkıştırılır; kelimesi kelimesine çeviri yapılmaz.
