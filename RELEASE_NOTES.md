# Kitsugi Release Notes - v2.4.132-beta

Bu sürümde uygulama açılışındaki kritik çökmeler giderildi, eklenti yükleme kararlılığı artırıldı ve Youtube eklentisi için gerekli bağımlılıklar sisteme entegre edildi.

### 🚀 Başlangıç Çökmeleri ve Syncler Eklenti Hatası Düzeltildi
- **Dinamik R Sınıfı Stub'ı:** Syncler eklentisinin (`SynclerPlugin`) açılışta `com.lagradost.cloudstream3.R$id` sınıfına ve onun altındaki `nav_host_fragment` ile `navigation_player` ID'lerine yaptığı doğrudan erişimlerin sebep olduğu `NoClassDefFoundError` hatası, dinamik R kaynak stub sınıfı oluşturularak tamamen çözüldü.

### 📺 Youtube Eklentisi Yükleme Hatası Çözüldü
- **NewPipe Extractor Bağımlılığı:** Youtube eklentisinin yüklenmesi esnasında oluşan `java.lang.NoClassDefFoundError: Failed resolution of: Lorg/schabi/newpipe/extractor/ServiceList;` hatasını gidermek amacıyla, Gradle konfigürasyonundaki `newpipeextractor` exclusion (hariç tutma) kuralı kaldırılarak gerekli bağımlılıklar tekrar etkinleştirildi.

### 🛡️ R8/ProGuard Kararlılık İyileştirmeleri
- ProGuard kuralları güncellenerek yeni eklenen `com.lagradost.cloudstream3.R` stub sınıfları ve `org.schabi.newpipe.extractor` paketlerindeki sınıfların R8 optimizasyonları/minify sırasında silinmesi veya şifrelenmesi engellendi. Böylece dinamik eklentilerin çalışma zamanında çökmesi önlendi.
