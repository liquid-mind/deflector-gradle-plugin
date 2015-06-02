package ch.liquidmind.deflector.plugin

import org.gradle.api.Project

/**
 * Created by sebastian on 22/05/15.
 */
class DeflectorExtension {
    def deflectedJarsPath = "deflected-jars"
    def jarsToDeflect = []

    private Project project;

    DeflectorExtension(Project project) {
        this.project = project;
    }

}
