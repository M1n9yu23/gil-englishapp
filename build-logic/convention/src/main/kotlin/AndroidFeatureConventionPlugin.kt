import com.gyugle.gil.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** For `feature:<name>:impl:presentation`. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(AndroidLibraryComposeConventionPlugin::class.java)
            pluginManager.apply(HiltConventionPlugin::class.java)

            dependencies {
                "implementation"(project(":core:presentation"))
                "implementation"(project(":core:designsystem"))
                // navigation3-ui (NavKey, entry, scenes) arrives through this api edge.
                "api"(project(":core:navigation"))

                "implementation"(libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            }
        }
    }
}
