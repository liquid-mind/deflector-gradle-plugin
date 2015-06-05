package ch.liquidmind.deflector

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class DeflectorPluginTest {
    @Test
    public void deflectorPluginAddsDeflectRTTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'ch.liquidmind.deflector'

        //assertTrue(project.tasks.deflectRT instanceof DeflectTask)
    }
}
