package ch.liquidmind.deflector.plugin

import org.gradle.api.artifacts.Dependency

import java.util.regex.Pattern

/**
 * Created by sebastian on 22/05/15.
 */
class DeflectOption {
    // Map of Key: --<option> Value: 'someparameter'
    private options = [:]

    // Notation of original dependency
    def originalDependency

    def getJarPath() {
        return options['--jar']
    }

    def getClasspath() {
        return options['--classpath']
    }

    // Used to add options
    // Adds a --<methodname> <param> option
    // when calling any undefined method on this class
    def methodMissing(String name, args) {
        options['--' + name] = args[0]
    }

    // Returns array version of the options map
    def String[] getDeflectorArgs() {
        def args = []
        options.each { k,v ->
            args << k
            args << v
        }
        return args as String[]
    }
}
