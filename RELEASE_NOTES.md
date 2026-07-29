# Kitsugi Release Notes

## v2.4.114 – Oynatıcı Görsel ve İşlevsel Uyumlaştırma (Aniyomi Pariteği)

### 🎬 Oynatıcı Arayüzü ve Görsel İyileştirmeler

#### 1. Yerel Tam Ekran Kayar Menüler (Custom Slide-Up Sheets)
- **ModalBottomSheet** kullanımı tamamen kaldırıldı. Bu sayede Android sistem durum çubuğu (status bar) ile alt navigasyon çubuğunun (navigation bar) durup dururken açılması ve sürükleyici tam ekran modunun (immersive mode) bozulması engellendi.
- Tüm alt sayfalar (`PlayerSheet.kt`), oynatıcının kendi yerel görünüm ağacında çalışan ve sayfa dışına tıklandığında yumuşak geçişlerle (`AnimatedVisibility` - dikey kayma ve opaklık animasyonları) kapanan modern bir yapıya dönüştürüldü.
- Yatay (landscape) mod ve tabletler için maksimum `600.dp` genişlik sınırı getirilerek ekran ortasında şık bir kart şeklinde konumlandırılması sağlandı.

#### 2. Reaktif Kontrol Yönetimi (OSD Auto-Hiding)
- Herhangi bir sheet (menü), panel (altyazı/ses gecikme vb.) veya diyalog açık olduğunda oynatıcı üzerindeki tüm OSD kontrolleri (üst/alt barlar, oynat/duraklat butonları, seekbar ve kilit butonu) otomatik ve reaktif bir şekilde gizlenerek ekran kirliliği önlendi.

#### 3. Akıllı Jest Kilitleme ve Menü Kapatma (Gesture Handling)
- Ayar sayfaları veya paneller açıkken yatay arama (seek), dikey ses/parlaklık sürüklemeleri, uzun basma (hızlandırma) ve çift dokunma (atlama) jestleri tamamen devre dışı bırakıldı.
- Menü açıkken arka plandaki boş alana tek dokunulduğunda açık olan tüm menülerin ve panellerin otomatik olarak kapatılması sağlandı.

#### 4. Arabellek Görünümü Temizliği (Clean Buffering)
- Oynatıcı duraklatıldığında (paused) ekranı karartıp "Duraklatıldı" yazan büyük, eski yükleme kutusu kaldırıldı.
- Arabellek göstergesi artık sadece oynatma aktifken (playing) yükleme yapıldığında ekranın tam ortasında dairesel bir halka olarak görünecek; duraklatıldığında ise temiz bir donma görüntüsü sağlanacaktır.
