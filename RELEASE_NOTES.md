# Kitsugi v2.4.47 Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🌐 Medya Başlık Lokalizasyonu Standardizasyonu (Media Title Localization)
- **Kullanıcı Dil Tercihi Entegrasyonu**: Personel (Staff) ve Stüdyo (Studio) detay sayfalarındaki medya başlıkları, kullanıcının uygulama ayarlarındaki başlık dili tercihine (İngilizce, Japonca/Native, Romaji) göre dinamik olarak görüntülenecek şekilde güncellendi.
- **Veri Katmanı & Model Yenilemesi**: `KitsugiStaffMediaWork` modeline `titleEnglish`, `titleJapanese` ve `titleRomaji` alanları eklendi; AniList GraphQL sorguları ve `KitsugiStaffClient`/`KitsugiStudioClient` veri eşlemeleri tektipleştirildi.
- **TV & Mobil Uyumluluğu**: Mobil ve Android TV (`TvRootScreen`) arayüzlerinde tüm stüdyo ve personel iş listeleri `getDisplayTitle` yardımcı fonksiyonu ile tam lokalizasyon desteğine kavuştu.

### 👤 Harici Kullanıcı Profili ve Sosyal Ekosistem Paritesi
- **Tam Fonksiyonel Kullanıcı Profilleri**: Harici kullanıcı sayfalarında medya listeleri, sosyal takipçi/takip edilen listeleri ve istatistik sekmeleri kişisel profil ekranı ile %100 görsel ve fonksiyonel uyuma ulaştırıldı.
- **Gelişmiş Favoriler ve Medya Navigasyonu**: Favori öğeleri ve kullanıcı listelerindeki kartlara tıklandığında ilgili detay sayfalarına doğru AniList/MAL ID eşleşmeleriyle sorunsuz yönlendirme sağlandı.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🌐 Media Title Localization Standardization
- **User Preference Alignment**: Media titles across Staff and Studio detail pages are now dynamically rendered according to the user's preferred title language setting (English, Japanese/Native, Romaji).
- **Data Layer & Schema Update**: Enhanced `KitsugiStaffMediaWork` with `titleEnglish`, `titleJapanese`, and `titleRomaji` fields. Updated AniList GraphQL queries and network mappers across `KitsugiStaffClient` and `KitsugiStudioClient`.
- **Mobile & Android TV Support**: Standardized title rendering via `getDisplayTitle` extension across both mobile views and Android TV (`TvRootScreen`) components.

### 👤 External User Profile & Social Ecosystem Parity
- **Feature-Complete External Profiles**: Synchronized external user profile pages with personal profiles, supporting grid/list media browsing, status filtering, follower/following social lists, and statistics parity.
- **Seamless Media Navigation**: Resolved ID mapping edge cases for AniList favorited items, ensuring smooth navigation to media detail pages.
