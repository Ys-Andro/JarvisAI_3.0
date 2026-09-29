plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.jetbrains.compose)
  alias(libs.plugins.kotlin.compose)
}

dependencies {
  implementation(compose.desktop.currentOs)
  implementation(compose.material3)
  implementation(compose.materialIconsExtended)
}

kotlin {
  jvmToolchain(17)
}

compose.desktop {
  application {
    mainClass = "com.example.jarvisdesktop.MainKt"
    nativeDistributions {
      targetFormats(
        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
      )
      packageName = "JARVIS"
      packageVersion = "1.0.0"
      description = "JARVIS desktop assistant"
    }
  }
}
