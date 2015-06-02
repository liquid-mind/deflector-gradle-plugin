package ch.liquidmind.deflector.plugin

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
            println "No options set for deflect task '${this.name}'."
        }
    }
}
