# Kitsugi Release Notes

## v2.4.115 – Çevrimdışı Anime İndirme ve Yerel Oynatma (Offline Playback)

Kitsugi'nin bu sürümünde, kullanıcıların internete bağlı olmadıklarında bile anime izleyebilmelerini sağlayan tam özellikli çevrimdışı indirme ve yerel oynatma altyapısı entegre edilmiştir.

### 📥 Çevrimdışı İndirme ve Canlı Durum İzleme
- **Stream Kartı İndirme Butonu:** Yayın kaynağı seçim ekranındaki akış kartlarına doğrudan indirme seçeneği eklendi.
- **Bölüm Listesi İndirme Desteği:** Bölüm listelerindeki satırlara etkileşimli indirme butonları yerleştirilerek, istenen bölümlerin arka planda kuyruğa alınması sağlandı.
- **Canlı İndirme Durumu:** Bölüm satırlarında indirme durumları (Kuyrukta, İndiriliyor, Tamamlandı, Duraklatıldı, Hata) ve yüzde olarak canlı ilerleme oranı (CircularProgressIndicator eşliğinde) reaktif şekilde gösterilmektedir.

### 🎬 Sorunsuz Yerel Oynatma (Offline Playback)
- **Dosya Yolu Algılama:** İndirmesi tamamlanan bölümlere tıklandığında, uygulama doğrudan yerel depolama dizinini algılar.
- **Yerel Player Entegrasyonu:** Tamamlanan indirmeler, çevrimiçi akış yerine `file://` protokolü üzerinden mevcut gelişmiş video oynatıcı (`KitsugiFullscreenPlayerActivity`) ile kesintisiz ve yüksek performansla oynatılır.

### ⚙️ Altyapı ve Depolama Optimizasyonları
- **Aniyomi Paritesi:** Arka plan indirme kuyruğu, depolama organizasyonu, dosya isimlendirmesi ve Cloudflare korumalarını aşmak için kullanılan özel HTTP istek başlığı (FFmpeg entegrasyonu dahil) desteği Aniyomi standartlarına %100 uyumlu hale getirilmiştir.
- **Kararlılık:** Büyük boyutlu video dosyalarının arka planda kararlı şekilde indirilmesi ve uygulama içi depolama izinleri optimize edilmiştir.
