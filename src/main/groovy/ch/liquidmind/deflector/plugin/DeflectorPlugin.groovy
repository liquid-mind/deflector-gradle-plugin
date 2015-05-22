package ch.liquidmind.deflector.plugin

import org.gradle.api.Project
import org.gradle.api.Plugin
import ch.liquidmind.deflector.Main

class DeflectorPlugin implements Plugin<Project>
{

	void apply(Project project)
	{
        project.extensions.create("deflectOptions", DeflectorExtension)

        // Add the folder with the deflected libs as a repository
        // This can be only done after the evaluation since the deflectedJarPath might
        // have been set in the buildscript
        project.afterEvaluate {
            project.repositories {
                flatDir {
                    dir project.deflectOptions.deflectedJarsPath
                }
            }
        }

        project.tasks.create("deflectAll") << {
            // Make sure output directory exists
            def outputDir = new File(project.projectDir, project.deflectOptions.deflectedJarsPath)
            if (!outputDir.exists())
                outputDir.mkdir()

            // Deflect all JARs defined by the build script
            Main deflector = new Main()
            project.deflectOptions.jarsToDeflect.each {
                def args = it.getDeflectorArgs()
                args << "--output"
                args << project.deflectOptions.deflectedJarsPath
                deflector.main(args as String[])
            }
        }

        project.tasks.create(name: "addDeflectedDependencies", dependsOn: project.tasks.deflectAll) << {
            project.dependencies {
                compile project.fileTree(dir: project.deflectOptions.deflectedJarsPath, include: '*.jar')
            }
        }

        // Make the compileJava task dependend on the deflectAll task
        project.tasks.compileJava.dependsOn project.tasks.addDeflectedDependencies


	}
}
