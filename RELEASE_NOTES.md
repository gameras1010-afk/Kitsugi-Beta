# Kitsugi Release Notes - v2.4.131-beta

Bu sürümde Kitsugi medya galerisine karakter ve görsel meta veri entegrasyonu yapıldı, galeri ve küçük resim (thumbnail) bileşenleri zenginleştirildi ve video akış seçim ekranına indirme yöneticisi için hızlı erişim kısayolu eklendi.

### 🎭 Karakter & Görsel Meta Veri Entegrasyonu
- **Galeri Veri Modeli Genişletmesi:** `GalleryItem` modeline görsel açıklaması (`description`) desteği eklendi.
- **Fanart.tv URL Ayrıştırıcı:** Fanart.tv'den gelen görsellerin URL'lerinden (örn. `monkey-d-luffy-xxx.png`) karakter isimleri otomatik olarak çıkartılarak okunabilir formatta (`Monkey D Luffy`) kaydedilir.
- **Karakter, Kadro & Stüdyo Desteği:** Karakter, Staff ve Studio detay sayfalarında ilgili entity adları otomatik olarak galeri görsellerine açıklama olarak atandı.

### 🖼️ Galeri Arayüzü & Görsel Badgeleri
- **Detaylı Tam Ekran Görünüm:** Tam ekran galeri penceresinde (`KitsugiImageGalleryDialog`) görselin açıklaması/karakter ismi sol alt kısımda kaynak ve kategori badge'lerinin yanına estetik bir dikey çizgi ile ayrılmış badge olarak eklendi.
- **Thumbnail Üzerinde Overlay:** Detay sayfalarındaki galeri listesinde (`DetailGalleryCard`) her küçük resmin sol üst köşesine yarı şeffaf badge olarak karakter adı/görsel açıklaması yerleştirildi.

### 📥 Akış Seçim Sayfasına İndirme Kısayolu
- **İndirilenler Kısayolu:** Akış seçim sayfasında (yatay ve dikey görünümlerde) izleme geçmişi butonunun hemen sol yanına doğrudan İndirilenler (`DownloadsActivity`) sayfasına yönlendirme yapan İndirme Kısayol butonu (`Icons.Rounded.Download`) eklendi.
