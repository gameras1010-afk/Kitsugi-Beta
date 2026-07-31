# Kitsugi Release Notes - v2.4.122-beta

Bu sürümde izleme geçmişinden anında oynatma desteği (Direct Play), oynatıcı ayarlarında ExoPlayer'ın geri getirilmesi ve indirme/bildirim performansı iyileştirmeleri yapılmıştır.

### 📜 Geçmiş Sayfası İyileştirmeleri (Anında Oynatma)
- **Geçmişten Direkt Oynatma (Direct Play):** Geçmiş sayfasındaki kayıtlar için çözümlenmiş kaynak URL'si ve HTTP başlıkları önbelleğe alınarak, tüm kaynakları arama adımı atlanıp oynatıcı doğrudan başlatılabilmektedir.
- **Anında Oynatma Rozeti:** Doğrudan oynatılabilecek geçmiş ögeleri için görsel bir ⚡ **Anında** rozeti eklenmiştir.

### 📺 Oynatıcı Ayarları
- **ExoPlayer Geri Getirildi:** Oynatıcı ayarlarından yanlışlıkla kaldırılmış olan Dahili Oynatıcı (ExoPlayer) seçeneği tekrar listeye eklenmiştir.

### 📥 İndirme ve Bildirim İyileştirmeleri
- **Akıllı Bildirim Güncellemesi (Throttling):** Video indirilirken bildirim panelini kilitleyen yüksek frekanslı ilerleme güncellemeleri saniyede en fazla 1 kez çalışacak şekilde sınırlanmıştır.
- **Akıcı İndirme Navigasyonu:** Bir video indirilmeye başlandığı an kullanıcı otomatik olarak "İndirmeler" ekranına yönlendirilir; geri çıkıldığında ise kalınan video veri çekme ekranına geri dönülür.
