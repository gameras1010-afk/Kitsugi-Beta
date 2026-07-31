# Kitsugi Release Notes - v2.4.127-beta

Bu sürümde izleme geçmişi kararlılığı artırıldı, yayın kartı arayüzü mobil cihazlar için duyarlı hale getirildi ve internetsiz ortamda izleme için otomatik altyazı indirme desteği eklendi.

### ⏱️ İzleme Geçmişi Otomatik Yenileme (Auto-Refresher)
- **Oturum Bazlı Geçersiz Kılma:** Uygulama kapatılıp açıldığında veya oynatma bağlantısının üzerinden 30 dakika geçtiğinde, eskiyen/geçersizleşen izleme linkleri otomatik olarak algılanır.
- **Kesintisiz Oynatma:** Geçmiş sekmesinden eski bir bölüme tıklandığında oynatıcı siyah ekranda kalmaz; sistem arka planda yayını taze bağlantıyla otomatik olarak yeniden çözümler (Auto-Resolve) ve kaldığı yerden oynatır.

### 📱 Mobil Uyumlu ve Duyarlı Yayın Kartı (StreamCard)
- **Dinamik Ekran Adaptasyonu:** Yayın seçme ekranındaki kartlar `BoxWithConstraints` ile yeniden yazıldı.
- **Büyük Posterler:** Kapak görselleri daha belirgin ve seçilebilir olması için büyütüldü (küçük ekranlar için 95x135dp, büyük ekranlar için 115x162dp).
- **Esnek Yerleşim:** Dikey modda veya dar ekranlarda (genişlik < 400dp) butonların yazıyı ezmesini önlemek için "Oynat" ve "İndir" butonları otomatik olarak alt alta hizalanacak şekilde konumlandırıldı.

### 📥 Otomatik Altyazı İndirme Sistemi
- **Birlikte İndir:** Bir videoyu indirdiğinizde, o yayına ait tüm harici altyazılar da (eklenti kaynaklı veya OpenSubtitles eşleşmeli altyazılar) video ile birlikte otomatik olarak indirilir.
- **Yerel Altyazı Eşleşmesi:** İndirilen altyazılar yerel diskteki `subs/` klasörüne dil koduna göre (`tr_Turkce.srt` vb.) kaydedilir. Çevrimdışı oynatıcı bu dosyaları otomatik olarak tarayıp oynatıcıya yükler.
