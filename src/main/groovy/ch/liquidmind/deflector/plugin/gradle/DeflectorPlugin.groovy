package ch.liquidmind.deflector.plugin.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task

class DeflectorPlugin implements Plugin<Project>
{
    private Project project
    private Map<String, DeflectOption> jarsToDeflect = [:]
    private File outputDir
    private deflectTasks = [:]
    private rtJarFile = new File(System.env.'JAVA_HOME' + '/jre/lib/rt.jar')

	void apply(Project project)
	{
        if ( !rtJarFile.exists() )
            throw new FileNotFoundException( "Couldn't find rt.jar. Is The system variable JAVA_HOME set?")

        this.project = project

        outputDir = new File(project.buildDir, "deflected-libs")

        // Extend the buildScript dependency notation with a deflected() option
        extendDependencyNotation()

        project.afterEvaluate {
            // Add the output dir  as a repository
            project.repositories {
                flatDir {
                    dir outputDir.absolutePath
                }
            }

            // Add all all jars from the output dir as dependencies
            project.dependencies {
                compile project.fileTree(dir: outputDir.absolutePath, include: '*.jar')
            }

            // The deflect task
            project.tasks.create('deflectAll') {
                // Make the compileJava task dependend on the deflectAll task
                project.tasks.compileJava.dependsOn project.tasks.deflectAll

                // Add the RT deflect config
                def rtDeflectOption = new DeflectOption()
                rtDeflectOption.jar(rtJarFile.absolutePath)
                rtDeflectOption.includes(~/java\..* javax\..* org\.ietf\..* org\.omg\..* org\.w3c\..* org\.xml\..*/)
                rtDeflectOption.excludes(~/javax\.smartcardio\..* org\.omg\.stub\.javax\..* java\.awt\.peer\..*/)
                rtDeflectOption.output(outputDir.absolutePath)
                jarsToDeflect['rt.jar'] = rtDeflectOption

                // Create a new deflect task for each jar that we want to deflect
                jarsToDeflect.each { String jarName, DeflectOption opt ->
                    def File    inputJarFile
                    def File    outputJarFile
                    def classpath = []

                    // Get the input jar file from either the 'jar' option given by the user or from
                    // finding the jar in the runtime dependencies via maven notation
                    if (!opt.getDeflectorOption('jar')) {
                        inputJarFile = getJarFromMavenNotation(jarName)
                        // TODO check if found
                        opt.jar(inputJarFile.absolutePath)
                    } else {
                        inputJarFile = new File(opt.getDeflectorOption('jar'))
                        // TODO check if it exists
                    }

                    outputJarFile = new File(new File(opt.getDeflectorOption('output')), "__" + inputJarFile.getName())

                    // rt.jar is hard dependency for every user defined jar
                    if (jarName != "rt.jar")
                        classpath << new File(outputDir, "__rt.jar").absolutePath

                    opt.dependsOn.each { String mavenNotation ->
                        def (String group, String name, String version) = mavenNotation.split(":")

                        // Add non-deflected jar to classpath
                        classpath << getJarFromMavenNotation(mavenNotation).absolutePath

                        // If there is a deflected version of this, add it to the classpath
                        classpath << new File(outputDir, "__" + name + "-" + version + ".jar").absolutePath
                    }

                    // Set deflector classpath option or extend it
                    if (classpath.size() > 0) {
                        if (!opt.getDeflectorOption('classpath')) {
                            opt.classpath(classpath.join(":"))
                        } else {
                            opt.classpath(opt.getDeflectorOption('classpath') + ":" + classpath.join(":"))
                        }
                    }

                    // Create deflect task for this jar
                    def deflectTask = project.tasks.create(name: jarName, type: DeflectTask) {
                        options = opt
                        outputDir = outputDir

                        // For up-to-date comparison:
                        inputs.file inputJarFile
                        outputs.file outputJarFile
                    }

                    deflectTasks[jarName] = deflectTask

                    // ... and make the deflectAll task dependent on it
                    project.tasks.deflectAll.dependsOn deflectTask
                }

                // Set dependencies between tasks
                jarsToDeflect.each { String dependencyName, DeflectOption opt ->
                    Task task =  project.tasks[dependencyName]
                    if (dependencyName != 'rt.jar') task.dependsOn deflectTasks['rt.jar']
                    opt.dependsOn.each { String mavenNotation ->
                        Task targetTask = deflectTasks[mavenNotation]
                        if (targetTask)
                            task.dependsOn targetTask
                    }
                }
            }

        }
    }

    private File getJarFromMavenNotation(String mavenNotation)
    {
        def (group, name, version) = mavenNotation.split(":")
        def jar =project.configurations.runtime.find {
            it.name.equals(name + '-' + version + '.jar')
        }
        if (!jar) throw new RuntimeException("Could not find jar of ${mavenNotation}.")
        return jar
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
     *                              dependsOn   'com.google:something:1.0.0',
     *                                          '...'
     *                          } )
     * }
     */
    private void extendDependencyNotation() {
        project.dependencies.ext.deflected = { String original, Closure options = null ->
            def (group, name, version) = original.split(":")

            // Generate the nativeDeflectorOptions for the deflect task
            def deflectOption = new DeflectOption()

            // Set output folder
            deflectOption.output(outputDir.absolutePath)

            // Run the provided option closure against the DeflectOption object
            if (options) {
                options = options.rehydrate(deflectOption, this, this)
                options.resolveStrategy = Closure.DELEGATE_ONLY
                options()
            }

            // Add the nativeDeflectorOptions Object to the map of jarsToDeflect
            jarsToDeflect[original] = deflectOption

            // Return the 'original' dependency, so that it will be added
            // as dependency by gradle
            return original
        }
    }
}
