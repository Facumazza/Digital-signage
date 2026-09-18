plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Si el repo vive dentro de OneDrive (o Dropbox, Drive), el cliente de
// sincronizacion bloquea los archivos intermedios mientras Gradle los escribe
// y la compilacion falla con AccessDeniedException. Definiendo
// GRENLUS_BUILD_DIR, el build va a una carpeta fuera de la sincronizada.
// Sin la variable, el comportamiento es el de siempre.
System.getenv("GRENLUS_BUILD_DIR")?.let { dir ->
    layout.buildDirectory.set(file("$dir/app"))
}

android {
    namespace = "com.grenlus.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.grenlus.player"

        // Android 5.0. Los TV Box economicos que se usan en carteleria suelen
        // tener Android viejo, y Media3 lo soporta desde aca.
        minSdk = 21
        targetSdk = 35
        versionCode = 3
        versionName = "0.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        // Los tests corren en la PC, donde las clases de Android (Log) no
        // existen: devuelven valores por defecto en vez de fallar.
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
