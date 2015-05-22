package ch.liquidmind.deflector

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class GreetingTaskTest {
    @Test
    public void canAddTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('someDeflection', type: DeflectTask)
        assertTrue(task instanceof DeflectTask)
    }
}
