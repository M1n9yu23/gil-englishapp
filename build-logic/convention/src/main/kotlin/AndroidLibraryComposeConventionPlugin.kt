import com.android.build.api.dsl.LibraryExtension
import com.gyugle.gil.convention.hasDeviceTests
import com.gyugle.gil.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** For `core:presentation`, `core:designsystem` and `core:navigation`. */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension> {
                buildFeatures { compose = true }
            }

            val bom = libs.findLibrary("androidx-compose-bom").get()
            dependencies {
                "implementation"(platform(bom))
                "implementation"(libs.findLibrary("androidx-compose-ui").get())
                "implementation"(libs.findLibrary("androidx-compose-material3").get())
                "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
                if (hasDeviceTests) "androidTestImplementation"(platform(bom))
            }
        }
    }
}
