# Kitsugi v2.4.104 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎬 Sezonluk Akış Düzeltmeleri (KRİTİK)

- **Doğru Sezon Araması:** 2. sezon ve sonrasındaki bölümleri akış eklentilerinde ararken artık önce "2. Sezon", "Season 2", "S2" gibi mevsim-özelleştirilmiş sorgular deneniyor. Böylece eklentiler doğru sezonu buluyor; 1. sezon içerikleri varsayılan olarak yüklenmiyor.

- **Akıllı Sezon Eşleştirme (`CsTitleMatcher`):** Arama sonuçlarındaki başlıklar artık sezon bilgisine göre ödüllendiriliyor/cezalandırılıyor. "Jujutsu Kaisen 2. Sezon" gibi bir sonuç istenilen sezon 2 ise **+0.35 puan** alıyor; yanlış sezon ise **-0.60 puan** cezalandırılıyor. Sezon içermeyen başlıklar (örtük S1) istenilen sezon >1 ise **-0.50 puan** ceza alıyor.

- **Başlık Benzerliği İyileştirmesi:** Arama sonuçlarında sezon metni ("2. Sezon", "II", "2" vb.) temizlenmiş hali de artık benzerlik hesabına katılıyor; "Jujutsu Kaisen 2. Sezon" ile "Jujutsu Kaisen" benzerliği artık ~1.0 çıkıyor.

- **Roman Rakam ve Sondaki Sayı Tespiti:** "Anime II", "Anime III", "Anime 2" gibi sezon belirten sondaki ifadeler otomatik olarak sezon numarasına dönüştürülüyor.

### 🔗 MAL / AniList Yönlendirme Düzeltmeleri

- **MAL Anime Artık MAL Editörüyle Açılıyor:** MyAnimeList (Jikan) kaynaklı animeleri düzenlerken AniList değil, MAL editörü açılıyor. Her kaynağın verisi kendi servisine kaydediliyor.

- **Kaynak Öncelikli Çözümleme (`AppViewModel`, `AppDialogHost`):** `addApiSelectionToList` fonksiyonu artık `result.source` bilgisini esas alıyor. Jikan/MAL kaynakları daima `"mal"` olarak işleniyor; AniList yalnızca fallback olarak devreye giriyor.

- **Yinelenen Kayıt Engeli Kaynak Uyumlu Hale Getirildi:** Listeye ekleme sırasında tekrar engeli artık sadece aynı kaynak içinde çalışıyor; AniList ve MAL kayıtları birbirini engellemiyor.

- **`firstMatching` Çağrıları Kaynak Farkındası Yapıldı:** Navigasyon ve düzenleme akışlarında AniList/MAL çapraz kirlenmesi önlendi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎬 Seasonal Stream Fixes (CRITICAL)

- **Correct Season Search:** When fetching episodes from streaming plugins for Season 2+, the app now prepends season-specific query variants ("2. Sezon", "Season 2", "S2") so that plugins locate the right season instead of defaulting to Season 1 content.

- **Smart Season Matching (`CsTitleMatcher`):** Search result titles are now rewarded or penalized based on season alignment. A result explicitly matching the target season earns **+0.35**, a wrong season gets **-0.60**, and an unlabeled (implicit Season 1) result gets **-0.50** when a Season >1 is requested.

- **Title Similarity Improvement:** Season suffixes ("2. Sezon", "II", "2") are stripped before similarity scoring, so "Jujutsu Kaisen 2. Sezon" vs "Jujutsu Kaisen" now resolves to ~1.0 similarity.

- **Roman Numeral & Trailing Number Detection:** Season indicators like "Anime II", "Anime III", "Anime 2" are automatically parsed into season numbers for accurate matching.

### 🔗 MAL / AniList Routing Fixes

- **MAL Anime Now Opens in the MAL Editor:** Editing an anime sourced from MyAnimeList (Jikan) now always opens the MAL editor, preventing data from being saved to AniList by mistake.

- **Source-Priority Resolution (`AppViewModel`, `AppDialogHost`):** `addApiSelectionToList` uses `result.source` as the authoritative signal. `jikan`/`mal` results always map to `"mal"` regardless of which services are authenticated; AniList is used only as a fallback.

- **Duplicate Entry Check Is Now Source-Aware:** The duplicate guard when adding entries now checks within the same resolved target service. AniList and MAL entries no longer block each other.

- **`firstMatching` Calls Made Source-Aware:** Cross-contamination between AniList and MAL in navigation and editing flows has been eliminated.
