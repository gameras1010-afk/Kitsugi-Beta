# Kitsugi-Beta — Sürüm Notları / Release Notes

---

## 🇹🇷 Türkçe

### 🔍 Arama Motoru — Jikan & Kitsu Fallback Desteği

- **MAL (Jikan) Fallback:** MAL araması başarısız olduğunda sistem otomatik olarak **Shikimori** API'sine geçer. Shikimori'den dönen `Double` türündeki skor değerleri, `Int` aralığına (`0–10`) dönüştürülerek tip uyumsuzluk hatası giderildi.
- **AniList Fallback:** AniList araması başarısız olduğunda sistem otomatik olarak **Kitsu.io** API'sine geçer. `KitsuExploreClient` içine `searchAnime` fonksiyonu eklenerek Kitsu arama desteği hayata geçirildi.
- **"Tümü" Modu Kapsamlı Fallback:** `SearchPlatform.All` (Tümü) modunda her iki kaynak için de zincirli fallback çalışır; hiçbir arama sonuçsuz kalmaz.

---

### 🖥️ Ayarlar Sayfası — Tam Ekran Panel Geçişi

Ayarlar sayfasındaki tüm alttan açılan pencereler (bottom sheet) artık **tam ekran dialog** olarak açılıyor. Önceki davranışta pencereler ekranın yalnızca %85–95'ini kaplıyor ve aşağı sürükleyerek kapatılabiliyordu; bu durum yanlışlıkla kapanmalara yol açıyordu.

**Tam ekrana geçirilen paneller:**
- 🎨 Tercihler & Tema Ayarları
- ⚙️ Sistem & Veri Ayarları
- 🔗 Entegrasyon Ayarları (TMDB, MDBList, AniSkip, Fanart.tv)
- 🧩 Eklenti & Akış Ayarları (Stremio, Cloudstream, Manga)
- 🎬 Oynatıcı Ayarları
- 👤 Hesap Bağlantıları
- 💬 Geri Bildirim Formu

**Dokunulmayan sheet'ler** (hafif, bağlamsal): Arama filtreleri, tür seçiciler, poster seçenekleri, akış seçici vb. eskisi gibi bottom sheet olarak açılmaya devam eder.

---

## 🇬🇧 English

### 🔍 Search Engine — Jikan & Kitsu Fallback Support

- **MAL (Jikan) Fallback:** When a MAL search fails, the system automatically falls back to the **Shikimori** API. A `Double`-to-`Int` score coercion fix (`0–10` range) was applied to resolve a type mismatch compilation error in `KitsugiShikimoriClient`.
- **AniList Fallback:** When an AniList search fails, the system automatically falls back to **Kitsu.io**. A `searchAnime` function was added to `KitsuExploreClient` to enable Kitsu as a search source.
- **"All" Mode Cascaded Fallback:** In `SearchPlatform.All` mode, both MAL and AniList deferred blocks now include their respective fallback sources, ensuring no search query returns empty results.

---

### 🖥️ Settings — Full-Screen Panel Navigation

All bottom sheets in the Settings section are now rendered as **full-screen Dialogs**. Previously, panels covered only 85–95% of the screen and could be accidentally dismissed by swiping down.

**Panels migrated to full-screen:**
- 🎨 Preferences & Theme Settings
- ⚙️ System & Data Settings
- 🔗 Integration Settings (TMDB, MDBList, AniSkip, Fanart.tv)
- 🧩 Addons & Stream Settings (Stremio, Cloudstream, Manga)
- 🎬 Player Settings
- 👤 Account Connections
- 💬 Feedback Form

**Unaffected sheets** (lightweight, contextual): Search filters, genre/tag pickers, poster options, stream selector, etc. remain as bottom sheets.
