import com.gyugle.gil.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Applied by every module that declares `@Inject` classes or Hilt modules.
 *
 * The Hilt compiler runs through KSP in each such module; Android modules also get the Hilt plugin.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            dependencies { "ksp"(libs.findLibrary("hilt-compiler").get()) }

            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies { "implementation"(libs.findLibrary("hilt-core").get()) }
            }
            pluginManager.withPlugin("com.android.base") {
                pluginManager.apply("com.google.dagger.hilt.android")
                dependencies { "implementation"(libs.findLibrary("hilt-android").get()) }
            }
        }
    }
}
