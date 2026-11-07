import com.android.build.api.dsl.ApplicationExtension
import com.gyugle.gil.convention.hasDeviceTests
import com.gyugle.gil.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Application modules with a Compose UI. */
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(AndroidApplicationConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                buildFeatures { compose = true }
            }

            val bom = libs.findLibrary("androidx-compose-bom").get()
            dependencies {
                "implementation"(platform(bom))
                if (hasDeviceTests) "androidTestImplementation"(platform(bom))
                "implementation"(libs.findLibrary("androidx-activity-compose").get())
                "implementation"(libs.findLibrary("androidx-compose-ui").get())
                "implementation"(libs.findLibrary("androidx-compose-material3").get())
                // The app writes the entryDecorators list of NavDisplay, so it declares the decorator.
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-navigation3").get())
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
            }
        }
    }
}
