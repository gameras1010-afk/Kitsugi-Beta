# Kitsugi Modular Player Release Notes 🚀

---

## 🇹🇷 TÜRKÇE SÜRÜM NOTLARI

### 🎬 Yeni Modüler ve Reaktif Oynatıcı Arayüzü (Aniyomi Uyarımı)
- **Yeni Reaktif Mimari:** Eski monolitik ve imperatif oynatıcı yapısı yerine, `StateFlow` durum akışlarına dayalı, modüler koordinasyon katan `PlayerSheetsHost`, `PlayerPanelsHost` ve `PlayerDialogsHost` mimarisi getirilmiştir.
- **Dahili Oynatıcı Seçenekleri:** Gelişmiş MPV (`MpvPlayerEngine`) ve ExoPlayer (`Media3PlayerEngine`) oynatma motorları tamamen entegre edilmiştir.
- **Duraklatma Ekranı (Pause Overlay):** Video duraklatıldığında arka planda açılan şık bölüm özeti, sezon yılı ve oyuncu kadrosu (cast) detaylarını barındıran zengin içerikli pause arayüzü eklendi.

### ⚙️ Gelişmiş MPV Oynatıcı Seçenekleri
- **Donanım Kod Çözme (hwdec):** Otomatik güvenli, tam donanım hızlandırma (tüm formatlar) veya yazılımsal kod çözme modları eklendi.
- **Video Debanding:** Düşük bit hızı olan görüntülerdeki renk geçiş şeritlerini gidermek için CPU/GPU Debanding desteği.
- **Demuxer Önbellek Ayarı:** Ağ akışı sırasında önbellek boyutunu (8-256 MB) dinamik olarak ayarlama seçeneği.
- **Ses Güçlendirme (Volume Boost):** Oynatıcı sesini normal sınırların üstüne (100% - 200% arası) çıkarabilme desteği.

### 👆 Akıllı Jest Kontrolleri ve Mekanikler
- **Hold-to-Speedup (Basılı Tutunca Hızlandırma):** Video üzerinde herhangi bir yere basılı tutulduğunda oynatmayı geçici olarak hızlandıran (2.0x veya özel hız katsayısı) ve bırakıldığında eski hızına dönen akıllı jest.
- **Yatay Kaydırma ile Seek:** Ekran üzerinde yatay kaydırma hareketleriyle hızlıca video konumunu değiştirme desteği.
- **Ses / Parlaklık Yön Değişimi:** Sol el parlaklık / sağ el ses veya varsayılan yönler arasında geçiş yapabilme (`swipeVolumeBrightnessSides`) ayarı.
- **Hassas Kare Kare Arama (Precise Seeking):** Seek yaparken kare kare tam konumu arayan özel hassas arama modu seçeneği.
- **Zoom Jesti:** Ekran üzerinde iki parmakla kıstırma hareketi yaparak ekran sığdırma (FIT) veya yakınlaştırma (ZOOM/CROP) modları arasında geçiş yapabilme.

### 📝 Altyazı ve Ses Özelleştirmeleri
- **İkincil Altyazı (Secondary Subtitle):** MPV oynatıcıda aynı anda çift altyazı gösterme desteği ve ikincil altyazı gecikme süre ayarı.
- **Gelişmiş Altyazı Stili:** İtalik altyazı desteği, hizalama seçenekleri (sol, sağ, orta), gölge offseti ve kenarlık kalınlığı ayarlamaları.
- **Rotaya Duyarlı Ses Gecikmesi:** Bağlı kulaklık/hoparlör çıkış türüne göre ses senkronizasyon gecikmelerinin otomatik algılanması ve uygulanması.

---

## 🇬🇧 ENGLISH RELEASE NOTES

### 🎬 New Modular & Reactive Player UI (Aniyomi Inspired)
- **Modern Reactive Architecture:** Replaced the legacy monolithic player UI with a decoupled structure controlled by `PlayerSheetsHost`, `PlayerPanelsHost`, and `PlayerDialogsHost` running on reactive `StateFlow` state machines.
- **Dual Playback Engines:** Fully integrated and configured advanced `MpvPlayerEngine` and `Media3PlayerEngine` (ExoPlayer).
- **Pause Overlay:** A stunning overlay displaying episode description, cast profiles, season year, and poster artwork when the player is paused.

### ⚙️ Advanced MPV Settings
- **Hardware Decoding (hwdec):** Switch between auto-safe hardware decoding, full auto acceleration, or software decoding modes.
- **Video Debanding:** Eliminate color banding in low-bitrate streams using customizable CPU or GPU debanding filters.
- **Demuxer Cache Management:** Set the network demuxer cache size dynamically (ranging from 8MB to 256MB) to optimize streaming stability.
- **Volume Boost Cap:** Over-amplify audio signals up to 200% for quiet streams.

### 👆 Intelligent Gesture Controls & Mechanics
- **Hold-to-Speedup:** Long press anywhere on the screen during playback to temporarily accelerate the video (e.g., 2.0x), reverting to normal speed immediately upon release.
- **Horizontal Swipe Seek:** Slide horizontally across the video to scrub back and forth with customized on-screen seek progress notifications.
- **Volume & Brightness Swapping:** Swap the default gesture sides (left-side volume, right-side brightness) for personalized single-handed use.
- **Precise Seeking:** Seek frame-by-frame for exact positioning rather than fast keyframe jumping.
- **Pinch to Zoom:** Pinch in or out to toggle between Fit, Stretch, and Zoom display modes.

### 📝 Custom Subtitle & Audio Enhancements
- **Secondary Subtitles:** Dual simultaneous subtitle tracks support in MPV with independent secondary subtitle delay control.
- **Advanced Subtitle Styling:** Custom shadow offsets, border widths, italic toggling, and justification alignments (left, center, right).
- **Route-Aware Audio Delay:** Automatically detects and applies route-specific sync delays based on active output channels (e.g., Bluetooth, Speakers).
