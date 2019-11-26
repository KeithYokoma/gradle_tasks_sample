import dev.keithyokoma.MyCustomTask

buildscript {
    repositories {
        google()
        jcenter()
        
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.60")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        
    }
}

tasks.register<MyCustomTask>("myCustomTask") {
    group = "Sample Group"
    description = "Print Hello"
    personName = properties["personName"] as String? ?: "no name"
}

tasks.register<dev.keithyokoma.GitHubTask>("mergeReleaseBranch") {
    group = "Automation Group"
    description = "Merge release branch into master"
    token = properties["token"] as String? ?: ""
}