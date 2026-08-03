# Kitsugi Release Notes - v2.4.138-beta

Bu sürümde, Cloudstream eklenti yükleme performansına odaklanılmış, başlangıç kilitlenmeleri ve arayüz donmaları giderilmiştir.

### 🚀 Eklenti Performansı ve Donma Giderimleri
- **Asenkron Eklenti Yükleme ve Kaldırma:** Eklenti yükleme (`loadExtension`) ve kaldırma (`unloadExtension`) süreçleri tamamen arka plan iş parçacığına (`Dispatchers.IO`) taşınarak, eklenti kurulumu veya aç/kapat işlemleri sırasında uygulamanın donması (ANR / UI Freeze) tamamen önlenmiştir.
- **Kademeli Başlangıç Yüklemesi:** Uygulama açılışında kurulu eklentilerin yüklenmesi esnasında oluşan CPU/iş parçacığı havuzu (thread pool) daralmalarını engellemek amacıyla, eklentilerin yüklenme sıraları arasına 50ms'lik kademeli gecikmeler (`delay(50L)`) eklenmiş ve başlangıç akıcılığı artırılmıştır.

### 🌐 Kaynak ve Depo Güncellemeleri
- **Hexated Depo Bağlantısı:** Stremio Köprüsü (Hexated) eklenti deposunun adresi güncellenerek (`raw.githubusercontent.com` aynasına taşındı), eklentilerin ve güncellemelerin sorunsuz indirilmesi sağlanmıştır.
