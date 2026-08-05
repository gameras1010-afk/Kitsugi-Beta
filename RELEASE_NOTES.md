# Kitsugi-Beta v2.4.141 — Sürüm Notları

Bu sürümde; eklenti arama deneyimi, detay sayfası navigasyonu, ağ kararlılığı, TV arayüzü ve video extractor katmanı kapsamlı biçimde yenilendi.

---

### 🔍 Arama & Keşif Deneyimi

- **"Info First" Detay Akışı:** Arama ekranı (`SearchScreen`), eklenti keşif diyaloğu (`AddonExploreDialog`, `AddonSearchDialog`), tam ekran grid (`AddonFullScreenGridPage`) ve satır içi keşif (`AddonExploreInline`) artık medyaya tıklandığında doğrudan oynatma yerine `KitsugiAddonDetailDialog` üzerinden geçiyor. Kullanıcı metadata ve bölüm seçimini gördükten sonra akışı başlatıyor.
- **KitsugiAddonDetailDialog:** Tüm discovery yüzeylerinde yeni standart detay diyaloğu; `CsStreamRunner.safeLoad` ile asenkron metadata yükleme, `CsEpisodeMatcher` ile doğru bölüm eşleştirme desteğiyle entegre edildi.
- **Akıllı Bölüm Yönlendirme:** Çoklu bölüm içeriklerinde otomatik bölüm eşleştirme yapılarak kullanıcı doğru bölüme yönlendiriliyor.

---

### 🌐 Ağ Kararlılığı & Retry Altyapısı

- **RetryInterceptor:** Tüm HTTP istekleri artık `RetryInterceptor` ile korunuyor:
  - **5xx Sunucu Hataları** → 300ms / 600ms üstel geri çekilme ile 2 retry
  - **429 Too Many Requests** → `Retry-After` başlığına uygun bekleme (maks. 30s)
  - **IOException** → 400ms / 800ms gecikmeyle 2 retry
  - İptal edilen çağrılarda anında sonlanma (call.isCanceled() kontrolü)
- **NuvioOkHttpProvider & KitsugiHttpClient:** Her iki HTTP istemci de `RetryInterceptor(maxRetries=2)` ile donatıldı.

---

### 🎬 Video Extractor Katmanı

- **StreamTapeWrapper (FP-36):** 3 farklı yöntemle stream URL'si çıkarılabiliyor: robotlink+token birleştirme, get_video URL parse etme ve doğrudan .mp4 regex. Tüm streamtape domain varyantları destekleniyor (streamtape.com/net/xyz, shavetape.cash).
- **Mp4UploadWrapper (FP-32):** JWPlayer sources array'inden `file` + `label` parse edilerek çözünürlük etiketli video listesi oluşturuluyor. Yedek olarak doğrudan `"file"` regex kullanılıyor.
- **McloudWrapper (FP-30):** HiAnime ve benzeri sitelerde kullanılan Mcloud embed player için 3 yöntemli extraction: sources JSON array, file regex ve Base64 payload decode.

---

### 🗄️ Veritabanı & Eklenti Sistemi

- **Room DB v28 Migrasyonu:** Eklenti deposu takibi (`plugin_repository_id` foreign key) ve cascade delete mekanizması eklendi. `CsPluginEntity` artık kaynak deposuyla ilişkilendirilmiş durumda.
- **ManagedAddonEntity Güncellemesi:** Yeni sütunlar ve cascade silme desteği eklendi; eklenti kaldırıldığında ilişkili kayıtlar otomatik temizleniyor.
- **CsPluginLoader — Asenkron Yükleme:** Eklenti yükleme/kaldırma işlemleri `Dispatchers.IO`'ya taşındı, başlangıçta 50ms kademeli gecikme ile thread pool darboğazı önlendi.

---

### 📡 Dinamik Domain & Eklenti Sağlık Sistemi

- **CsCfWarmupManager Entegrasyonu:** Cloudflare ısınma (warmup) işlemleri artık dinamik domain çözüm sistemiyle senkronize; `isDomainListReady()` beklendikten sonra başlıyor.
- **AI Destekli Otomatik Domain Güncelleme:** GitHub Actions ile `domain_fixes.json` otomatik güncelleniyor; 22+ eklenti için ölü domain'ler canlı alternatiflerine yönlendirildi.
- **CsPluginDiagnosticRunner:** Eklenti sağlık tanılaması; tamamen çalışmayan eklentiler dinamik olarak engelleniyor ve kullanıcıya bilgi gösteriliyor.

---

### 📺 TV Arayüzü

- **TvHomeScreen Yenilemesi:** Klasik ve modern ana sayfa içerik modları (`TvClassicHomeContent`, `TvModernHomeContent`) yeniden yapılandırıldı.
- **TvNavigationState:** TV navigasyonu artık daha tutarlı state yönetimiyle çalışıyor.
- **TvMangaSourceHealthScreen:** Manga kaynak sağlık ekranı TV arayüzüne eklendi.

---

### 🔧 Geliştirici & Ayarlar

- **DeveloperLogsDialog:** Gerçek zamanlı logcat akışı ve filtre desteği geliştirildi.
- **SettingsScreen Güncellemesi:** Sistem ayarları ve parametre sayfaları (`SettingsScreenParameters`) yeniden düzenlendi.
- **CsStreamRunner.safeLoad:** Güvenli metadata yükleme metodu; hata yönetimi ve timeout koruması eklendi.
- **CloudstreamUrlHelper:** URL temizleme ve normalize etme yardımcı metotları genişletildi.
- **WatchHistoryScreen:** İzleme geçmişi ekranı yeni veri modeli (`WatchHistoryEntry`) ile güncellendi.
