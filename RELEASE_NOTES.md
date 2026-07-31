# Kitsugi Release Notes - v2.4.128-beta

Bu sürümde indirme yönetimi kararlılığı artırıldı ve çevrimdışı oynatma esnasında altyazıların yüklenmeme sorunu tamamen giderildi.

### 📥 İndirme İptali ve Arka Plan Senkronizasyonu
- **Anlık İptal Desteği:** İndirme ekranından bir dosya silindiğinde veya duraklatıldığında arka plandaki servis (`AnimeDownloadService`) değişikliği anında algılar.
- **Kaynak Optimizasyonu:** Aktif indirme işi durdurularak ağ bağlantıları ve dosya yazma işlemleri anında sonlandırılır, bildirim panelinde hayalet indirmelerin kalması önlenir.

### 🎬 Çevrimdışı Altyazı Desteği
- **Otomatik Altyazı Entegrasyonu:** İndirilen videoların yanında bulunan `subs/` klasöründeki altyazılar (`srt`, `vtt`, `ass`, `ssa` formatları) çevrimdışı oynatma esnasında otomatik olarak taranır.
- **Yerel Yükleme:** İndirilen videolar yerel oynatıcıdan açıldığında tüm indirilen altyazılar oynatıcıya beslenerek sorunsuz bir şekilde gösterilir.
