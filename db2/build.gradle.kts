plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":common"))
    implementation(fileTree("libs") { include("*.jar") })
    implementation("com.ibm.db2:jcc:11.5.9.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    archiveBaseName.set("dbx-agent-db2")
    archiveClassifier.set("")
    manifest {
        attributes("Main-Class" to "com.dbx.agent.db2.Db2AgentKt")
    }
}
