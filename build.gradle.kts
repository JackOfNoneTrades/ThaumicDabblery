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
