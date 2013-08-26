package org.github.mansur.oozie.plugin

import org.github.mansur.oozie.extensions.OozieWorkflowExtension
import org.github.mansur.oozie.tasks.OozieWorkflowTask
import org.github.mansur.oozie.beans.*
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * @author Muhammad Ashraf
 * @since 7/27/13
 */
class OozieWorkflowPlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = 'oozie'

    @Override
    void apply(Project project) {
      project.extensions.create(EXTENSION_NAME, OozieWorkflowExtension)
      addTask(project)
    }

    private void addTask(Project project) {
        project.tasks.withType(OozieWorkflowTask).whenTaskAdded { OozieWorkflowTask task ->
            def ext = project.extensions.findByName(EXTENSION_NAME)
            task.conventionMapping.workflowActions = { ext.actions }
            task.conventionMapping.common = { ext.common == null ? [:] : ext.common }
            task.conventionMapping.end = { ext.end }
            task.conventionMapping.workflowName = { ext.name }
            task.conventionMapping.namespace = { ext.namespace }
            task.conventionMapping.jobXML = { ext.jobXML == null ? [:] : ext.jobXML }
            task.conventionMapping.outputDir = { ext.outputDir == null ? project.buildDir : ext.outputDir }
            task.conventionMapping.credentials = { ext.credentials }
        }
    }
}
