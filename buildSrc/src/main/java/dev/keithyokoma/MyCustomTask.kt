package dev.keithyokoma

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class MyCustomTask : DefaultTask() {

    @Input
    lateinit var personName: String

    @TaskAction
    fun sayHello() {
        println("Hello, $personName")
    }
}