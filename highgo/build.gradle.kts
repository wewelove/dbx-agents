plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":common"))
    implementation(fileTree("libs") { include("*.jar") })
    implementation("com.highgo:HgdbJdbc:6.2.4")
    testImplementation(project(":test-support"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    archiveBaseName.set("dbx-agent-highgo")
    archiveClassifier.set("")
    manifest {
        attributes("Agent-Label" to "瀚高 HighGo", "Main-Class" to "com.dbx.agent.highgo.HighgoAgent")
    }
}

tasks.test {
    useJUnitPlatform()
}
