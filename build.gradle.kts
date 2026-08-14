import net.darkhax.curseforgegradle.TaskPublishCurseForge

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

apply(
    from =
        "https://raw.githubusercontent.com/JackOfNoneTrades/67minecraft-gradle-publish/" +
            "${property("publish67ScriptTag")}/67minecraft-publish.gradle.kts",
)

tasks.withType<TaskPublishCurseForge>().configureEach {
    uploadArtifacts.forEach { it.addEnvironment("Client", "Server") }
}

tasks.withType<JavaExec>().configureEach {
    if (name.startsWith("runServer")) {
        // Angelica is client-only. Strip it immediately before launch because
        // GTNHGradle appends the runtime classpath after task configuration.
        doFirst("stripClientOnlyMods") {
            classpath = classpath.filter { file ->
                !file.name.contains("angelica", ignoreCase = true)
            }
        }
    }
}
