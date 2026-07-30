# Kitsugi Release Notes - v2.4.118-beta

Bu sürümde, görsel galeri etkileşimi iyileştirilmiş, çapraz platform senkronizasyon altyapısı güçlendirilmiş ve Simkl entegrasyonuna otomatik akıllı ID eşleştirme desteği eklenmiştir.

### 🖼️ Görsel Galeri Etkileşimi İyileştirildi
- **Akıllı Poster Tıklama Mantığı:** Detay ekranlarında poster görseline tıklandığında resim galerisinin her zaman ilk resimden (0. indeks) başlaması yerine, tıklanan görselin URL'si üzerinden doğru galeri indeksinin hesaplanıp o görselle açılması sağlandı.

### 🔄 Simkl Entegrasyonuna Akıllı ID Çözümleme Desteği
- **Çalışma Zamanı Simkl ID Eşleme:** Veritabanında Simkl ID'si bulunmayan veya Simkl hesabı sonradan bağlanan içeriklerin güncellenmesi veya silinmesi durumunda, MyAnimeList ID veya TMDB ID kullanılarak Simkl ID'si çalışma zamanında (on-the-fly) API üzerinden çözümlenir.
- **Veri Tutarlılığı ve Otomatik Kayıt:** Çözümlenen Simkl ID'si veritabanındaki yerel kayda otomatik olarak yazılarak sonraki senkronizasyonların doğrudan ve hızlı yapılması sağlanır.
- **Gelişmiş Senkronizasyon Akışı:** Simkl hesabı bağlı olduğunda, yerel veritabanında Simkl ID'si olmasa dahi MAL veya TMDB ID'leri mevcut olduğu sürece senkronizasyon işlemleri Simkl ile anında gerçekleştirilir.

### ⚙️ Genel Entegrasyon ve Kararlılık
- **Platformlar Arası Veri Tutarlılığı:** MAL, AniList ve Simkl platformları arasındaki üç yönlü senkronizasyon ve akıllı içe aktarma (`smartImport`) işlemleri optimize edilerek kütüphanedeki veri bütünlüğü korundu.
