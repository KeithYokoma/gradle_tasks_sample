repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(gradleApi())

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.3.0.201903130848-r")
    implementation("org.kohsuke:github-api:1.99")
}
