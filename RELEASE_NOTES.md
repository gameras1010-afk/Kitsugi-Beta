# Kitsugi Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI — Yeni Güncelleme

### 🔍 Keşfet Modülü — MAL / AniList Veri Akışı Düzeltmeleri
- **"Yeni Eklenen Anime" Bölümü Geri Döndü**: Jikan API'sinin 504/503 hatası vermesi durumunda devreye giren AniList GraphQL yedek mekanizması artık Keşfet ekranına düzgün yansıyor; bölüm artık boş görünmüyor.
- **Boş Ekran Hatasının Giderilmesi**: Bazı Jikan uç noktaları başarısız olsa bile diğer listeler (filteredNewlyAddedAnime, filteredNewlyAddedManga, airingSoonAnime, filteredUpcomingAnimeTmdb) veri taşıyorsa "İçerik Yok" ekranı bir daha yanlışlıkla gösterilmiyor.
- **Temiz Boş Bölüm Mantığı**: Yükleme tamamlandığında içerik gelmeyen medya bölümleri (başlık ve "Tümünü Gör" butonu dahil) artık tamamen gizleniyor; boşa kayan boş alanlar arayüzden temizlendi.

### 📱 Keşfet Ekranı — Yatay Mod Kart Tasarımı
- **Daha Dikine Poster Görselleri**: Yatay ekranda Keşfet kartlarındaki resimler artık `160×100 dp` (yatay thumbnail) yerine `90×130 dp` (dikine poster oranı) boyutunda; görseller çok daha net ve tanınabilir.
- **Optimize Kart Genişliği**: Landscape modda kart genişliği `320 dp`'den `260 dp`'ye indirildi; poster ile yazı alanı artık dengeli — ne resim ezilir ne de metin sıkışır.

### 🎨 Kullanıcı Arayüzü — Genel İyileştirmeler
- **AniList Favori Butonu Parity**: Kütüphaneye eklenmemiş medyalarda da Favori butonu görünür ve çalışır hâle getirildi; otomatik "Planlanıyor" girişi oluşturularak uzak sunucuyla senkronize ediliyor.
- **TMDB Yakında Yayında Sayfalama**: "Yakında Yayında" bölümü artık bağımsız bir kategori türüyle yönetildiği için sonsuz kaydırma ve sayfalama düzgün çalışıyor.
- **AniList Medya Editörü Geliştirmeleri**: Özel listeler (customLists) ve gelişmiş puanlama (advancedScores) desteği tam olarak entegre edildi; tarih sıfırlama ve dinamik puan girişi eklendi.
- **Sosyal Kullanıcı Profili Navigasyonu**: Forum konuları, aktivite kartları ve yorum bölümlerindeki yazar avatarları ve kullanıcı adlarına tıklayınca artık ilgili kullanıcının profiline gidiliyor.

---

## 🇬🇧 ENGLISH RELEASE NOTES — Latest Update

### 🔍 Explore Module — MAL / AniList Data Feed Fixes
- **"Newly Added Anime" Section Restored**: The AniList GraphQL fallback that activates upon Jikan 504/503 errors now correctly surfaces in the Explore screen; the section no longer appears empty.
- **Empty Screen Bug Fixed**: Even if some Jikan endpoints fail, the "No Content" empty state is no longer shown when other lists (filteredNewlyAddedAnime, filteredNewlyAddedManga, airingSoonAnime, filteredUpcomingAnimeTmdb) still hold data.
- **Clean Empty Section Logic**: Media sections with no results after loading now fully hide themselves (title and "See All" button included), removing dead blank space from the layout.

### 📱 Explore Screen — Landscape Card Design
- **Taller Poster Thumbnails**: Explore cards in landscape mode now show images at `90×130 dp` (portrait poster ratio) instead of `160×100 dp` (landscape thumbnail); visuals are significantly clearer and more recognizable.
- **Optimized Card Width**: Landscape card width reduced from `320 dp` to `260 dp`, giving a balanced split between the poster and the text column — no more squished text or wasted space.

### 🎨 UI — General Improvements
- **AniList Favorite Button Parity**: Favorite button is now visible and functional for media not yet added to the local library; a "Planned" entry is auto-created and synced with the remote API.
- **TMDB Upcoming Anime Pagination**: "Upcoming Anime" section now managed via a dedicated category type, ensuring correct infinite scroll and pagination behavior.
- **AniList Media Editor Enhancements**: Full integration of custom lists (customLists) and advanced scoring (advancedScores); added date clearing and dynamic advanced score inputs.
- **Social User Profile Navigation**: Tapping author avatars and usernames in forum topics, activity cards, and comment sections now navigates to the respective user profile.
