import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.kitsugi.animelist"

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }
    val tmdbApiKey        = localProperties.getProperty("tmdb_api_key")         ?: "YOUR_TMDB_API_KEY_HERE"
    val anilistClientId   = localProperties.getProperty("anilist_client_id")    ?: "32022"
    val anilistSecret     = localProperties.getProperty("anilist_client_secret") ?: "RwKJVnuh1B76AMKaWyEgsig6KwjGQpkpbMwJSi4j"
    val malClientId       = localProperties.getProperty("mal_client_id")         ?: "9dfa9b926eecef62128b6d464c7e33b9"
    val simklClientId     = localProperties.getProperty("simkl_client_id")       ?: "650ad7c96b7e1ab1e0056f745708ee0d05558480e8e0c6ae80f1061d2ae31496"
    val simklSecret       = localProperties.getProperty("simkl_client_secret")   ?: "81d3253f90d1f2c0c4ea55af6ca317861e5f40d43c16255eeabd57fc51c73f1c"
    val animeSkipClientId = localProperties.getProperty("anime_skip_client_id")  ?: "5mpKIMeowxmJ4UvAWacdPEzNbfXEjZDv"

    val appVersionName = "2.4.61"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.kitsugi.animelist"
        minSdk = 26
        targetSdk = 36
        // Her derlemede otomatik artan benzersiz sürüm kodu (Dakika bazlı zaman damgası)
        val timeVersionCode = (System.currentTimeMillis() / 60000).toInt()
        versionCode = timeVersionCode
        versionName = "$appVersionName-beta.$timeVersionCode"

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        // â”€â”€ T3-01: OAuth / API secret'larÄ± â€” local.properties'den okunur â”€â”€â”€â”€â”€â”€
        buildConfigField("String", "ANILIST_CLIENT_ID",      "\"$anilistClientId\"")
        buildConfigField("String", "ANILIST_CLIENT_SECRET",  "\"$anilistSecret\"")
        buildConfigField("String", "MAL_CLIENT_ID",          "\"$malClientId\"")
        buildConfigField("String", "SIMKL_CLIENT_ID",        "\"$simklClientId\"")
        buildConfigField("String", "SIMKL_CLIENT_SECRET",    "\"$simklSecret\"")
        buildConfigField("String", "ANIME_SKIP_CLIENT_ID",   "\"$animeSkipClientId\"")
        // Dolby Vision native bridge flags.
        // DOVI_NATIVE_ENABLED: stub so=true (always loads); flip to false to skip System.loadLibrary.
        // DOVI_EXTRACTOR_HOOK_READY: true once DolbyVisionExtractorsFactory is wired into the player.
        buildConfigField("boolean", "DOVI_NATIVE_ENABLED", "true")
        buildConfigField("boolean", "DOVI_EXTRACTOR_HOOK_READY", "true")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                // Stub mode: no libdovi static lib required.
                // To enable real libdovi, add: arguments("-DDOVI_ENABLE_LIBDOVI=ON")
                arguments("-DDOVI_ENABLE_LIBDOVI=OFF")
                abiFilters("arm64-v8a", "armeabi-v7a")
            }
        }

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "BUILD_TYPE_LABEL", "\"Debug\"")
            // Debug'ta minify kapalÄ± â€” hÄ±zlÄ± iterasyon
            isMinifyEnabled = false
        }

        release {
            // T3-02: R8 ile full obfuscation + shrinking aktif
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "BUILD_TYPE_LABEL", "\"Release\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("foss") {
            dimension = "version"
        }
        create("gms") {
            dimension = "version"
            versionNameSuffix = "-gms"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    applicationVariants.all {
        val flavor = flavorName
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "Kitsugi-Beta-v${appVersionName}-${flavor}.apk"
        }
    }

    packaging {
        // â”€â”€ 16 KB page-size uyumluluÄŸu â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // lib-decoder-iamf-release.aar iÃ§indeki prebuilt libiamf.so ve libiamfJNI.so
        // dosyalarÄ± henÃ¼z 16 KB ELF segment hizalamasÄ±na (PT_LOAD alignment=0x4000)
        // sahip deÄŸil. useLegacyPackaging=true ile SO'lar APK iÃ§inde *sÄ±kÄ±ÅŸtÄ±rÄ±lmÄ±ÅŸ*
        // paketlenir; bu durumda Android runtime mmap yerine decompress ederek yÃ¼kler
        // ve alignment kontrolÃ¼ atlanÄ±r. Google Play bu APK biÃ§imini kabul eder.
        //
        // Uzun vadeli Ã§Ã¶zÃ¼m: media3 IAMF decoder AAR'Ä± 16 KB uyumlu bir upstream
        // sÃ¼rÃ¼mÃ¼yle deÄŸiÅŸtirmek veya libiamf.so'yu objcopy ile yeniden hizalamak.
        jniLibs {
            useLegacyPackaging = true
            pickFirsts.add("**/libc++_shared.so")
        }

        resources {
            excludes += setOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
                "META-INF/versions/**"
            )
        }
    }
}

// â”€â”€ T3-03 / TASK-106: Room schema export â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// KSP'ye schema JSON'larÄ±nÄ±n yazÄ±lacaÄŸÄ± dizini bildir.
// KitsugiDatabase'deki exportSchema = true ile birlikte Ã§alÄ±ÅŸÄ±r.
// Schema dosyalarÄ± VCS'e eklenmeli â†’ migration testlerinde referans olarak kullanÄ±lÄ±r.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

configurations.all {
    exclude(group = "androidx.media3", module = "media3-exoplayer")
    exclude(group = "androidx.media3", module = "media3-common")
    exclude(group = "androidx.media3", module = "media3-datasource")
    exclude(group = "androidx.media3", module = "media3-datasource-okhttp")
    exclude(group = "androidx.media3", module = "media3-exoplayer-hls")
    exclude(group = "androidx.media3", module = "media3-extractor")

    // Jackson 2.13.x was compiled against Kotlin 1.6 stdlib.
    // Kotlin 2.0.x runtime throws AssertionError: Built-in class kotlin.Any is not found
    // when old jackson-module-kotlin uses reflection. Force 2.18.3 (Kotlin 2.0 compatible).
    resolutionStrategy {
        force("com.fasterxml.jackson.core:jackson-core:2.18.3")
        force("com.fasterxml.jackson.core:jackson-databind:2.18.3")
        force("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
        force("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.gif)

    implementation(libs.markdown.renderer.core)
    implementation(libs.markdown.renderer.m3)

    implementation(libs.youtube.player)
    
    // Transitive dependencies required by forked local AARs
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("androidx.media3:media3-database:1.8.0")
    implementation("androidx.media3:media3-container:1.8.0")
    implementation("androidx.media3:media3-decoder:1.8.0")
    implementation("androidx.annotation:annotation-experimental:1.3.1")
    compileOnly("org.checkerframework:checker-qual:3.43.0")

    // Kitsugi Engine local AARs (replaces lib-exoplayer, lib-common, lib-datasource, lib-datasource-okhttp, lib-exoplayer-hls, lib-extractor)
    implementation(files(
        "libs/lib-common-release.aar",
        "libs/lib-datasource-release.aar",
        "libs/lib-datasource-okhttp-release.aar",
        "libs/lib-exoplayer-release.aar",
        "libs/lib-exoplayer-hls-release.aar",
        "libs/lib-extractor-release.aar"
    ))
    implementation(libs.media3.ui)
    implementation("androidx.media3:media3-session:1.8.0")
    // T2.3: MediaSessionCompat + PlaybackStateCompat + MediaStyle (notification)
    implementation("androidx.media:media:1.7.0")

    // Local decoder AARs (AV1, IAMF, MPEG-H, FFmpeg)
    implementation(files(
        "libs/lib-decoder-av1-release.aar",
        "libs/lib-decoder-iamf-release.aar",
        "libs/lib-decoder-mpegh-release.aar",
        "libs/lib-decoder-ffmpeg-release.aar"
    ))

    implementation(libs.gson)

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Cloudstream3 library and dependencies
    implementation("com.lagradost.api:library-android:1.0.2-local") {
        exclude(group = "org.mozilla", module = "rhino")
        exclude(group = "com.github.AmaryllisVFX", module = "newpipeextractor")
        exclude(group = "info.debatty", module = "java-string-similarity")
        // Exclude old Jackson bundled in cloudstream library â€” we force 2.18.3 below
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.module")
    }
    implementation("org.jsoup:jsoup:1.17.2")
    // Jackson 2.18.3: first version with full Kotlin 2.0.x support.
    // Previous 2.13.1 compiled against Kotlin 1.6 â†’ AssertionError: Built-in class kotlin.Any is not found
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    implementation("com.github.Blatzar:NiceHttp:0.4.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

    // RxJava for dynamic Manga extension loading compatibility
    implementation("io.reactivex:rxjava:1.3.8")
    // androidx.preference: Mihon ConfigurableSource kaynaklarÄ±nÄ±n setupPreferenceScreen'i
    // gerÃ§ek androidx.preference.PreferenceScreen/EditTextPreference sÄ±nÄ±flarÄ±nÄ± kullanÄ±r.
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Subsampling and zoomable image support for manga pages
    implementation("me.saket.telephoto:zoomable-image-coil:0.8.0")

    // Kotlinx Serialization JSON for Manga Extension compatibility
    implementation(libs.kotlinx.serialization.json)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.7.3")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // TV Material
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tvprovider)
    implementation(libs.haze.android) {
        exclude(group = "org.jetbrains.compose.ui")
        exclude(group = "org.jetbrains.compose.foundation")
    }

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.okhttp)

    // ZXing
    implementation(libs.zxing.core)

    // NanoHTTPD â€“ TV Companion Server
    implementation(libs.nanohttpd)

    // â”€â”€ Kotatsu-Parsers-Redo (Futon) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // 1300+ built-in manga kaynaÄŸÄ± (TR dahil 74 TÃ¼rkÃ§e)
    // Futon'un kullandÄ±ÄŸÄ± aynÄ± entegrasyon: com.github.clquwu:kotatsu-parsers-redo
    // SHA gÃ¼ncellemek iÃ§in: https://api.github.com/repos/Kotatsu-Redo/kotatsu-parsers-redo/commits/master
    implementation("com.github.clquwu:kotatsu-parsers-redo:f287c414a6") {
        exclude(group = "org.json", module = "json")
    }

    // nextlib-mediainfo
    implementation(files("libs/nextlib-mediainfo-local.aar"))

    // mpv-android-lib
    implementation("io.github.abdallahmehiz:mpv-android-lib:0.1.12")

    // â”€â”€ T1.10 â€“ Libass / ASS Extractor â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // peerless2012/ass-android: Media3-uyumlu libass JNI wrapper.
    // Ä°Ã§inde arm64-v8a + armeabi-v7a iÃ§in pre-compiled libass.so bulunur.
    // Feature flag: AppSettings.enableAssExtractor (default=false) ile korunur.
    implementation("io.github.peerless2012:ass-media:0.4.0")

    "gmsImplementation"(libs.play.services.cast.framework)
}