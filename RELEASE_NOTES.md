# Kitsugi Release Notes - v2.4.120-beta

Bu sürümde oynatıcı stabilizasyonu tamamlanmış, en-boy oranı döngüsü ve reaktif kontroller entegre edilmiş, indirme sırasındaki bildirim kasması giderilmiş ve indirme başlangıcı navigasyon akışı optimize edilmiştir.

### 📺 Oynatıcı Stabilizasyonu ve Reaktif Kontroller
- **Reaktif Oynatma Hızı:** Oynatma hızı ayarları `StateFlow` mimarisine bağlanarak oynatıcı motoruyla anlık ve kararlı şekilde senkronize edildi.
- **Döngüsel En-Boy Oranı Kontrolü:** Kontrol paneline en-boy oranını (Orijinal, Sığdır, Doldur, 16:9, 4:3, Yakınlaştır) sırayla değiştiren döngüsel buton ve ekran üzeri geri bildirim (feedback) mesajları eklendi.
- **Ekstra Karartma Desteği:** Parlaklık durumuna bağlı çalışan ve ekranı karartabilen `BrightnessOverlay` oynatıcının üzerine konumlandırılarak geri getirildi.
- **Kararlılık Artırımı:** Oynatıcı üzerindeki kararsızlığa yol açan tepkisiz `PauseOverlay` tamamen kaldırıldı.
- **Buton Görünürlüğü:** Yatay modda "Girişi Atla" (Skip Intro) butonu aktifken diğer özel eylem butonlarının gizlenmesi önlendi, yan yana yerleşim sağlandı.

### 📥 İndirme ve Bildirim İyileştirmeleri
- **Akıllı Bildirim Güncellemesi (Throttling):** Video indirilirken bildirim panelini kilitleyen yüksek frekanslı ilerleme güncellemeleri saniyede en fazla 1 kez çalışacak şekilde optimize edildi.
- **Akıcı İndirme Navigasyonu:** Bir video indirilmeye başlandığı an, kullanıcı otomatik olarak "İndirmeler" ekranına yönlendirilir. Kullanıcı bu ekrandan geri çıktığında ise kaldığı video veri çekme ekranına kesintisiz olarak dönebilir.
