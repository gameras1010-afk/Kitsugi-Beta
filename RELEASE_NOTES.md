# Kitsugi Release Notes

## v2.4.113 – Aniyomi Oynatıcı Pariteği (Tamamlandı)

### 🎬 Oynatıcı Geliştirmeleri

#### İstatistik Sayfası (MPV Stats)
- `MoreSheet` içine 0–5 arası sayfa seçici chip'leri eklendi
- Sayfa 0 = Kapalı, sayfa 1–5 = MPV stats overlay
- Aniyomi `stats/display-stats-toggle` ve `stats/display-page-N` script-binding komutları ile birebir uyumlu
- Seçim `SettingsDataStore.setPlayerStatisticsPage` ile kalıcı olarak saklanır

#### Ses Kanalları (Audio Channels)
- `MoreSheet` içine `AudioChannels` enum tabanlı chip seçici eklendi
- Desteklenen modlar: Otomatik, Güvenli Otomatik, Mono, Stereo, Ters Stereo
- Aniyomi'nin `af` filtresi + `audio-channels` property yaklaşımı ile birebir uyumlu
- MPV motoruna anlık uygulanır, `SettingsDataStore.setAudioChannels` ile kalıcı saklanır

#### Kod Çözücü (Decoder) İyileştirmesi
- `updateDecoder()` artık MPV motoruna anlık `hwdec` property atıyor
- `SettingsDataStore.setMpvHwdecMode` ile otururlar arasında kalıcı

#### Picture-in-Picture (PiP) – Tam Entegrasyon
- `BottomRightPlayerControls`'daki PiP butonu artık `settings.pipEnabled` ayarına reaktif
- Tıklandığında `PlayerPipHelper.enterPipSafe(activity, playerEngine, isPlaying, hasNext)` çağrılır
- `LocalContext.current` composable scope'da doğru yakalanıyor

### 🏗️ Mimari
- `KitsugiPlayerViewModel`: `statisticsPage`, `audioChannels` state akışları eklendi
- `PlayerSheetsHost`: yeni parametreler prop-drilling zinciriyle `MoreSheet`'e aktarıldı
- `KitsugiFullscreenPlayerScreen`: tüm yeni state'ler collect ediliyor ve PlayerSheetsHost'a geçiriliyor
- `SettingsDataStore`: `setMpvHwdecMode` setter eklendi (diğerleri zaten mevcuttu)

### 🐛 Düzeltilen Derleme Hataları
- `MPVLib` bağımlılığı `MoreSheet`'ten kaldırıldı; komutlar ViewModel üzerinden yürütülüyor
- `items(count = 6) { page: Int -> }` tip çıkarımı hatası giderildi
- `LocalContext.current` composable olmayan lambda içinde çağrılan hata düzeltildi
- `setMpvHwdecMode` duplikasyon çakışması çözüldü
