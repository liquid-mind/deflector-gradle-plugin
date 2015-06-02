package ch.liquidmind.deflector.plugin

import org.gradle.api.Project
import org.gradle.api.Plugin
import ch.liquidmind.deflector.Main

class DeflectorPlugin implements Plugin<Project>
{
    private Project project
    private jarsToDeflect = []
    private outputDir

	void apply(Project project)
	{
        this.project = project

        // Create extension point for user input. This is used for
        // general deflector settings such as the output path for
        // deflected jars.
        project.extensions.create("deflector", DeflectorExtension)

        // Add the __rt.jar deflect option
        def rtOption = new DeflectOption()
        rtOption.jar(System.env.'JAVA_HOME' + '/jre/lib/rt.jar')
        rtOption.includes(~/java\..* javax\..* org\.ietf\..* org\.omg\..* org\.w3c\..* org\.xml\..*/)
        rtOption.excludes(~/javax\.smartcardio\..* org\.omg\.stub\.javax\..* java\.awt\.peer\..*/)
        jarsToDeflect << rtOption

        // Extend the buildScript dependency notation with a deflected() option
        extendDependencyNotation()

        project.afterEvaluate {
            // Add the output dir  as a repository
            project.repositories {
                flatDir {
                    dir project.deflector.deflectedJarsPath
                }
            }

            // Add all all jars from the output dir as dependencies
            project.dependencies {
                compile project.fileTree(dir: project.deflector.deflectedJarsPath, include: '*.jar')
            }

            // The deflect task
            project.tasks.create('deflectAll') {
                // Make the compileJava task dependend on the deflectAll task
                project.tasks.compileJava.dependsOn project.tasks.deflectAll

                // Here we create all the deflect tasks that this task will depend on
                def deflectorClasspaths = []
                def deflectTasks = [:]

                // Create the output dir
                outputDir = new File(project.projectDir, project.deflector.deflectedJarsPath)
                if (!outputDir.exists())
                    outputDir.mkdir()

                jarsToDeflect.each { DeflectOption opt ->
                    // If option has no jar defined, find it by searching
                    // existing runtime dependencies
                    if (!opt.getJarPath()) {
                        def (group, name, version) = opt.originalDependency.split(":")
                        def jarPath = project.configurations.runtime.find {
                            it.absolutePath.matches(~/.*${group}.*${name}.*${version}.*\.jar/)
                        }
                        opt.jar(jarPath.getAbsolutePath())
                    }
                    deflectorClasspaths << opt.getJarPath()

                    // If classpath was not specified, take classpath off all previous jars
                    if (!opt.getClasspath() && deflectorClasspaths.size() > 0) {
                        opt.classpath(deflectorClasspaths.join(":"))
                    }

                    // Add output folder option
                    opt.output(project.deflector.deflectedJarsPath)

                    // Remember classpath of the soon to be deflected jar file
                    def originalJar = new File(opt.getJarPath())
                    def outputJarPath = new File(outputDir, "__" + originalJar.getName()).getAbsolutePath()
                    deflectorClasspaths << outputJarPath

                    // Create deflect task
                    def taskName = originalJar.getName()
                    println "creating task ${taskName}"
                    def deflectTask = project.tasks.create(name: taskName, type: DeflectTask) {
                        options = opt
                        inputs.file originalJar
                        outputs.file outputJarPath
                    }

                    // This task depends on all previous deflect tasks
                    // until option is added to configure
                    // dependencies on each other by the user
                    deflectTasks.values().each {
                        deflectTask.dependsOn it
                    }
                    deflectTasks[taskName] = deflectTask

                    // ... and make the deflect All task dependend on it
                    project.tasks.deflectAll.dependsOn deflectTask

                }

            }
        }
    }

    /**
     * Extend dependency notation in the build script with a
     * deflected() method. This will add the original dependency
     * along with a deflection version of it. Parameters for deflector
     * can be defined in a closure
     * Usage:  deflected(  'com.original:dependency:1.0.0',
     *                     { deflectorOptions } )
     *
     * Example:
     *
     * dependencies {
     *     compile: 'com.something:not-deflected:1.0.0'
     *     compile: deflected( 'com.google.guava:guava:18.0',
     *                          {
     *                              includes ~/com\..* /
     *                              excludes ~//
     *                              classpath 'some.classpath'
     *                          } )
     * }
     */
    private void extendDependencyNotation() {
        project.dependencies.ext.deflected = { original, Closure options = null ->
            def (group, name, version) = original.split(":")

            // Generate the options for the deflect task
            def deflectOption = new DeflectOption()
            deflectOption.originalDependency = original

            // Run the provided option closure against the DeflectOption object
            if (options) {
                options = options.rehydrate(deflectOption, this, this)
                options.resolveStrategy = Closure.DELEGATE_ONLY
                options()
            }

            // Add the options Object to the list of jarsToDeflect
            jarsToDeflect << deflectOption

            // Return the 'original' dependency, so that it will be added
            // as dependency
            return original
        }
    }
}
