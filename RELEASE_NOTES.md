# Kitsugi Release Notes - v2.4.119-beta

Bu sürümde arama doğruluğu artırılmış, puanlama kartlarındaki görsel tutarsızlıklar giderilmiş, izleme geçmişi ekranı zenginleştirilmiş ve kaldığı yerden devam etme mekanizmaları kaynağa duyarlı hale getirilerek kararlılık artırılmıştır.

### 🔍 Arama Temizliği ve Platform Filtreleme
- **Akıllı Arama Filtreleme:** Arama ekranında "Anime" veya "Manga" platformları seçildiğinde, TMDB sonuçlarının getirilmesi engellenerek keşif alakasızlığı ortadan kaldırıldı.

### 📊 Kart Arayüzü ve Puanlama İyileştirmeleri
- **Tutarlı Puanlama Gösterimi:** Puanı henüz bulunmayan veya oylanmamış yapımlarda puan alanının boş kalması yerine standart bir "—" rozeti gösterilerek görsel boşluklar ve hizalama kaymaları giderildi.
- **Sıralama Göstergeleri:** Tüm ekranlardaki keşif listelerinde ve çift resimli sıralı kategorilerde sıralama rozetleri (#1, #2 vb.) eklenerek görsel tutarlılık sağlandı.

### 📺 Yatay Modda İzleme Geçmişi Butonu
- **Tasarım Eşitliği:** Oynatıcı bilgi ekranının yatay yerleşim modunda eksik olan "İzleme Geçmişi" butonu dikey modla eşitlenerek tüm ekran yönelimlerinde erişilebilir hale getirildi.

### ⏳ İnteraktif İzleme Geçmişi ve İlerleme Çubukları
- **İlerleme Durum Göstergesi:** Geçmiş ekranındaki her bir ögeye, videonun yüzde kaçının izlendiğini ve bitmesine kaç dakika kaldığını gösteren hassas ilerleme çubuğu (`ContinueWatchingProgressLabel`) entegre edildi.
- **Kaldığı Yerden Doğrudan Devam Etme:** Geçmiş ekranındaki ögelere tıklama özelliği eklenerek; tercih edilen kaynak bilgisini kaydeden ve oynatıcıyı otomatik oynatma (autoplay) aktif olarak doğrudan başlatan derin bağlantı (deep-linking) sistemi kuruldu.

### 🔄 Kaynak Bazlı (Contextual) Oynatma Resimilasyonu
- **Akıllı Devam Etme Uyarısı:** Oynatıcıdaki "kaldığınız yerden devam edin" uyarı penceresi, sadece videonun izlendiği kaynak eklentisi (addon) eşleştiğinde görünecek şekilde sınırlandırıldı. Bu sayede farklı kaynaklar arası geçişte yanlış sürelerden başlama sorunu çözüldü.
