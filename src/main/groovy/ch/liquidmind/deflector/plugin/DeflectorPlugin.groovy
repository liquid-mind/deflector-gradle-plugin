package ch.liquidmind.deflector.plugin

import org.gradle.api.Project
import org.gradle.api.Plugin
import ch.liquidmind.deflector.Main

class DeflectorPlugin implements Plugin<Project>
{
    private Project project
    private jarsToDeflect = []

	void apply(Project project)
	{
        this.project = project

        // Create extension point for user input. This is used for
        // general deflector settings such as the output path for
        // deflected jars.
        project.extensions.create("deflector", DeflectorExtension)

        // Add the __rt.jar dependency with default options
        def rtOption = new DeflectOption()
        rtOption.jar(System.env.'JAVA_HOME' + '/jre/lib/rt.jar')
        rtOption.includes(~/java\..* javax\..* org\.ietf\..* org\.omg\..* org\.w3c\..* org\.xml\..*/)
        rtOption.excludes(~/javax\.smartcardio\..* org\.omg\.stub\.javax\..* java\.awt\.peer\..*/)
        jarsToDeflect << rtOption

        // Extend the buildScript dependency notation with a deflected() option
        extendDependencyNotation()

        // Add the folder with the deflected libs as a repository
        // This can be only done after the evaluation since the deflectedJarPath might
        // have been set in the buildscript
        project.afterEvaluate {
            project.repositories {
                flatDir {
                    dir project.deflector.deflectedJarsPath
                }
            }
            project.dependencies {
                compile project.fileTree(dir: project.deflector.deflectedJarsPath, include: '*.jar')
            }
        }

        // The deflect task
        project.tasks.create('deflectAll') << {
            // Make sure output directory exists
            def outputDir = new File(project.projectDir, project.deflector.deflectedJarsPath)
            if (!outputDir.exists())
                outputDir.mkdir()

            Main deflector = new Main()
            def deflectorClasspaths = []
            jarsToDeflect.each { DeflectOption opt ->

                // If option has no jar defined, find it by searching
                // existing runtime dependencies
                if ( !opt.getJarPath() ) {
                    def (group, name, version) = opt.originalDependency.split(":")
                    def jarPath = project.configurations.runtime.find {
                        it.absolutePath.matches(~/.*${group}.*${name}.*${version}.*\.jar/)
                    }
                    opt.jar(jarPath.getAbsolutePath())
                }

                deflectorClasspaths << opt.getJarPath()

                // If classpath was not specified, take classpath off all previous jars
                if ( !opt.getClasspath() && deflectorClasspaths.size() > 0) {
                        opt.classpath(deflectorClasspaths.join(":"))
                }

                // Add output folder option
                opt.output(project.deflector.deflectedJarsPath)

                // Actual deflection
                def args = opt.getDeflectorArgs()
                //println "Deflecting with arguments:" + args
                deflector.main(args as String[])

                def outputJarPath =  new File(outputDir, "__" + new File(opt.getJarPath()).getName() ).getAbsolutePath()
                deflectorClasspaths << outputJarPath
            }

        }

        // Make the compileJava task dependend on the deflectAll task
        project.tasks.compileJava.dependsOn project.tasks.deflectAll

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
