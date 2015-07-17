package ch.liquidmind.deflector.plugin.gradle

import ch.liquidmind.deflector.Main
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.InvalidUserDataException

public class DeflectTask extends DefaultTask {
    Main deflectorMain = new Main()
    DeflectOption options
    File outputDir

    @TaskAction
    run() {
        if (options == null)
            throw new InvalidUserDataException("No nativeDeflectorOptions set for deflect task '${this.name}'.")

        if (outputDir == null)
            throw new InvalidUserDataException("No output directory set for deflect task '${this.name}'.")

        // Create folder structure
        outputDir.mkdirs()

        // Run deflector by calling main()
        deflectorMain.main(options.getDeflectorArgs());

    }
}
