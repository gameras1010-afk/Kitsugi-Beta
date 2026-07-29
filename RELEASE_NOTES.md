# Kitsugi Çevrimdışı İndirme ve İzleme Geçmişi Güncellemesi Sürüm Notları 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 📥 Anime İndirme Sistemi (Aniyomi-Style)
- **FFmpeg Entegrasyonu:** M3U8 ve MP4 video akışlarını arka planda sorunsuzca birleştirebilen ve indirebilen FFmpeg tabanlı yüksek performanslı indirme motoru entegre edildi.
- **Arka Plan İndirme Servisi:** İndirmelerin yarıda kalmasını önlemek için ilerleme bildirimi sunan kararlı Android Foreground Service altyapısı geliştirildi.
- **Esnek Depolama Yönetimi:** SAF (Storage Access Framework) entegrasyonu ile harici/özel klasör seçimi eklendi. Ayrıca 1DM/ADM gibi harici indirme yöneticileri için yönlendirme desteği getirildi.
- **İndirmeler Ekranı:** İndirme ilerlemesini, tamamlanma durumlarını ve yerel dosyaları yönetebileceğiniz yeni `DownloadsScreen` arayüze entegre edildi.

### 📜 İzleme Geçmişi (Watch History)
- **Geçmiş Takibi:** İzlenen anime, bölüm ve oynatıcı detaylarını yerel olarak kaydeden `WatchHistoryManager` geliştirildi.
- **Geçmiş Ekranı:** Kullanıcıların izledikleri bölümleri takip edebileceği, arayabileceği ve tek tuşla temizleyebileceği `WatchHistoryScreen` arayüze eklendi.
- **Navigasyon İyileştirmeleri:** Akış seçim ekranından ve ana menüden izleme geçmişine doğrudan erişim sağlayan geçişler entegre edildi.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 📥 Anime Video Downloader Integration
- **FFmpeg Integration:** Integrated an FFmpeg-based download pipeline supporting high-performance muxing and compilation of M3U8 streams.
- **Background Download Service:** Implemented an Android Foreground Service with live notification progress updates to keep download tasks running reliably.
- **Storage & External Downloader Support:** Added SAF directory selection and custom intent-based routing to support external downloaders like 1DM, ADM, or default system options.
- **Downloads Screen:** Added a dedicated `DownloadsScreen` to view active download progress, pause/resume tasks, delete local files, and trigger offline playback.

### 📜 Watch History Management
- **Playback Tracking:** Introduced `WatchHistoryManager` to capture anime playback, episode details, quality metadata, and timestamps locally.
- **Watch History UI:** Developed a premium, responsive `WatchHistoryScreen` to view, search, and delete individual history entries or wipe history entirely.
- **Navigation Flow:** Wired the new screens into the core routing system (`AppRoot` and detail page backstacks) for smooth transition pathways.
