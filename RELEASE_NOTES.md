# Kitsugi v2.4.22 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### ⚡ Profil ve Arama Ekranı Modernizasyonu
- **Göreceli Arama & Çoklu Platform Harmanlama**: Arama sayfasında "Tümü" seçildiğinde MAL, AniList ve TMDB sonuçlarının adil harmanlanması sağlandı. AniList ve TMDB sonuçlarının gizlenmesine yol açan katı isim filtresi kaldırılarak platform etiketli (MAL, AniList, TMDB) tam arama listesi sunuldu.
- **Akıllı Esnek Arama & Yazım Toleransı (Fuzzy Search)**: İsimlerin birleşik, tireli veya yazım hatalı girildiği durumlarda (`attackontitan`, `demon-slayer`, `OnePiece`) otomatik sorgu temizleme ve ikinci şans (fallback) araması eklendi.
- **AniList Alaka Düzeyi (Relevance) Sıralaması**: Metin araması yapıldığında AniList sıralaması alaka düzeyine göre düzenlenerek nokta atışı sonuçlar elde etmesi sağlandı.
- **Profil Kartları Etkileşimi (Deep-Linking)**: AniList aktivite akışındaki medya kartları ile Simkl ve MAL profil sekmelerindeki favori/geçmiş kartları tıklanabilir hale getirildi ve doğrudan medya detayına bağlandı.
- **Görsel İstatistik Dağılımı (`SegmentedDistributionBar`)**: Profil sekmelerindeki izleme durumları (İzlenen, Tamamlanan, Planlanan vb.) modern ve oranlı renk çubuğuyla görselleştirildi.

### 🛠️ Hata Düzeltmeleri
- **Simkl ve MAL Profil Navigasyon Hataları Düzeltildi**: Eksik callback parametreleri giderilerek derleme ve gezinme kararlılığı sağlandı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### ⚡ Profile & Search Dashboard Modernization
- **Multi-Platform Search Blending**: Fixed "All" search tab logic to fairly blend and present results across MAL, AniList, and TMDB without discarding AniList or TMDB entries due to aggressive title deduplication.
- **Smart Fuzzy Search & Typo Tolerance**: Added automated query splitting and fallback execution for concatenated, hyphenated, or misspelled search queries (e.g. `attackontitan`, `demon-slayer`, `OnePiece`).
- **AniList Search Relevance Ranking**: Improved AniList text query sorting by utilizing native GraphQL relevance search match instead of strict popularity overrides.
- **Interactive Profile Media Cards (Deep-Linking)**: Enabled direct navigation to detail pages across AniList activity feeds, Simkl recent history, and MAL favorites.
- **Segmented Stats Distribution**: Visualized watch status breakdown across all platform profiles using responsive `SegmentedDistributionBar` components.

### 🛠️ Bug Fixes
- **Profile Navigation Callbacks Resolved**: Fixed missing navigation callback parameters in Simkl and MAL profile views to ensure clean compilation and smooth UI interactions.

---

> **📥 APK İndir / Download APK**: [Releases Sayfası / Releases Page](https://github.com/gameras1010-afk/Kitsugi-Beta/releases/latest)
