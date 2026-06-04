// ─── buildTypes e productFlavors para diferentes ambientes ───────────────────
// Adicione ao androidApp/build.gradle.kts

android {
    // ... configurações anteriores ...

    // Variantes de build
    buildTypes {
        debug {
            debuggable = true
            minifyEnabled = false
            buildConfigField("String", "WS_HOST", "\"10.0.2.2\"")  // Emulador
            buildConfigField("int", "WS_PORT", "8080")
            buildConfigField("String", "WS_SCHEME", "\"ws\"")
        }

        release {
            debuggable = false
            minifyEnabled = true
            shrinkResources = true
            buildConfigField("String", "WS_HOST", "\"transparencia.caninde.ce.gov.br\"")
            buildConfigField("int", "WS_PORT", "443")
            buildConfigField("String", "WS_SCHEME", "\"wss\"")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // Flavores de build (dev, staging, prod)
    flavorDimensions.add("environment")
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "WS_HOST", "\"10.0.2.2\"")
            buildConfigField("int", "WS_PORT", "8080")
            buildConfigField("String", "API_ENV", "\"development\"")
        }

        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "WS_HOST", "\"staging-api.caninde.ce.gov.br\"")
            buildConfigField("int", "WS_PORT", "443")
            buildConfigField("String", "WS_SCHEME", "\"wss\"")
            buildConfigField("String", "API_ENV", "\"staging\"")
        }

        create("prod") {
            dimension = "environment"
            buildConfigField("String", "WS_HOST", "\"transparencia.caninde.ce.gov.br\"")
            buildConfigField("int", "WS_PORT", "443")
            buildConfigField("String", "WS_SCHEME", "\"wss\"")
            buildConfigField("String", "API_ENV", "\"production\"")
        }
    }

    // Signing config
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────

// Build commands:
//
// Desenvolvimento (emulador):
// ./gradlew installDevDebug
// ./gradlew runDevDebug
//
// Staging:
// ./gradlew assembleStaging
// ./gradlew installStagingRelease
//
// Produção:
// ./gradlew assembleProdRelease
// ./gradlew bundleProdRelease
