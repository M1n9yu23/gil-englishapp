import androidx.room.gradle.RoomExtension
import com.gyugle.gil.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Applied by the module that owns the entities (`feature:<name>:impl:data`), never `core:database`. */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            pluginManager.apply("com.google.devtools.ksp")

            extensions.configure<RoomExtension> {
                // Committed: migration tests and auto-migrations need every previous schema.
                schemaDirectory("$projectDir/schemas")
            }
            dependencies {
                "implementation"(libs.findLibrary("androidx-room-runtime").get())
                "ksp"(libs.findLibrary("androidx-room-compiler").get())
            }
        }
    }
}
