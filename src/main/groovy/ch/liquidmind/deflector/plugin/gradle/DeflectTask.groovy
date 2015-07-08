package ch.liquidmind.deflector.plugin.gradle

import ch.liquidmind.deflector.Main
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public class DeflectTask extends DefaultTask {
    Main deflectorMain = new Main();
    DeflectOption options;

    @TaskAction
    run() {
        if (options != null) {
            deflectorMain.main(options.getDeflectorArgs());
        } else {
            println "No nativeDeflectorOptions set for deflect task '${this.name}'."
        }
    }
}
