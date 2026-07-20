import os
import re
import sys

# Yollar
APP_ROOT = r"c:\NuvioAnimeList"
REF_ROOT = r"C:\Users\Administrator\Desktop\py kodlar\nuvio araştırma\Anime\NuvioTV-dev"
PLAN_DIR = r"C:\Users\Administrator\Desktop\py kodlar\nuvio araştırma\Düzgün tv uyarlama plan dosyaları\NuvioTV_Plan_Task_Walkthrough_Dosyalari"

def validate_references():
    print("NuvioTV Plan Referans Doğrulayıcı Başlatılıyor...")
    print(f"APP: {APP_ROOT}")
    print(f"REF: {REF_ROOT}")
    print(f"PLAN: {PLAN_DIR}\n")

    if not os.path.exists(PLAN_DIR):
        print(f"HATA: Plan dizini bulunamadı: {PLAN_DIR}")
        return False

    success = True
    
    # MD dosyalarını tara
    for filename in os.listdir(PLAN_DIR):
        if not filename.endswith(".md"):
            continue
            
        filepath = os.path.join(PLAN_DIR, filename)
        print(f"İnceleniyor: {filename}...")
        
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
            
        # File links bul: file:///...
        links = re.findall(r"file:///([^\s\)]+)", content)
        for link in links:
            # URL encode'ları çöz
            cleaned_link = link.replace("%20", " ")
            # Windows dosya yoluna dönüştür
            target_path = cleaned_link.replace("/", "\\")
            
            # Bazı linkler line range içerir: #L123-145
            if "#" in target_path:
                target_path = target_path.split("#")[0]
                
            if not os.path.exists(target_path):
                # Bazı yollar local makinedeki yollarla tam eşleşmeyebilir, kontrol et
                print(f"  [UYARI] Eksik Referans Linki: {target_path}")
                success = False
                
    if success:
        print("\nTüm dosya referansları başarıyla doğrulandı! (BUILD SUCCESSFUL)")
    else:
        print("\nBazı referanslar eksik veya doğrulanamadı. Lütfen kontrol edin.")
        
    return success

if __name__ == "__main__":
    validate_references()
