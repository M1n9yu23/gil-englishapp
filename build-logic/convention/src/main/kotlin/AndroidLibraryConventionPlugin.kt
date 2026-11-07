import com.android.build.api.dsl.LibraryExtension
import com.gyugle.gil.convention.catalogVersionInt
import com.gyugle.gil.convention.configureInstrumentedTests
import com.gyugle.gil.convention.configureKotlinJvmTarget
import com.gyugle.gil.convention.configureUnitTests
import com.gyugle.gil.convention.libs
import com.gyugle.gil.convention.pathToPackageName
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** The base for every Android library module. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Not org.jetbrains.kotlin.android: AGP 9 registers the `kotlin` extension itself.
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<LibraryExtension> {
                namespace = pathToPackageName()
                compileSdk = catalogVersionInt("projectCompileSdkVersion")
                defaultConfig {
                    minSdk = catalogVersionInt("projectMinSdkVersion")
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
            configureKotlinJvmTarget()
            configureUnitTests()
            configureInstrumentedTests()

            dependencies {
                "implementation"(libs.findLibrary("kotlinx-coroutines-core").get())
                "implementation"(libs.findLibrary("kotlinx-serialization-json").get())
            }
        }
    }
}
