plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false     // 👈 CAMBIADO: Ahora usa el alias del catálogo
    alias(libs.plugins.kotlin.parcelize) apply false   // 👈 Mantiene el alias limpio
    id("com.google.gms.google-services") version "4.5.0" apply false
}