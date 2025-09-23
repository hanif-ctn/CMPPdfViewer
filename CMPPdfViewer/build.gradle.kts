import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
//    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            compileTaskProvider {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                    freeCompilerArgs.add("-Xjdk-release=${JavaVersion.VERSION_1_8}")
                }
            }
        }
        //https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        instrumentedTestVariant {
//            sourceSetTree.set(KotlinSourceSetTree.test)
//            dependencies {
//                debugImplementation(libs.androidx.testManifest)
//                implementation(libs.androidx.junit4)
//            }
//        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "cmpcrop"
            isStatic = true
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.accompanist.permissions)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        iosMain.dependencies {
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
        }
    }
}

android {
    namespace = "com.chaintechnetwork.cmppdfviewer"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

mavenPublishing {
    coordinates("network.chaintech", "cmp-image-pick-n-crop", "1.1.2")
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    pom {
        name.set("CMPImagePickNCrop")
        description.set("CMPImagePickNCrop Library for Compose Multiplatform")
        inceptionYear.set("2024")
        url.set("https://github.com/ChainTechNetwork/CMP-image-pick-n-crop.git")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("ctn-mobile-innovation")
                name.set("Mobile Innovation Network")
                email.set("rehan.t@chaintech.network")
                url.set("https://github.com/ChainTechNetwork")
            }
        }
        scm {
            url.set("https://github.com/ChainTechNetwork/CMP-image-pick-n-crop/tree/main")
            connection.set("scm:git:git://github.com/ChainTechNetwork/CMP-image-pick-n-crop.git")
            developerConnection.set("scm:git:ssh://git@github.com/ChainTechNetwork/CMP-image-pick-n-crop.git")
        }
    }
    configure(
        com.vanniktech.maven.publish.KotlinMultiplatform(
            javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
            sourcesJar = true,
            androidVariantsToPublish = listOf("release")
        )
    )
    signAllPublications()
}