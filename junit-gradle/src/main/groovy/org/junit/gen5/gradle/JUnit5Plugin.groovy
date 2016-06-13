/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.junit.gen5.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.junit.gen5.console.ConsoleRunner

/**
 * @since 5.0
 */
class JUnit5Plugin implements Plugin<Project> {

	private static final String EXTENSION_NAME = 'junit5';
	private static final String TASK_NAME      = 'junit5Test';

	void apply(Project project) {
		def junitExtension = project.extensions.create(EXTENSION_NAME, JUnitExtension)
		junitExtension.extensions.create('tags', TagsExtension)
		junitExtension.extensions.create('engines', EnginesExtension)

		project.afterEvaluate {
			configure(project, junitExtension)
		}
	}

	private void configure(Project project, junitExtension) {
		project.task(TASK_NAME, group: 'verification', type: JavaExec) { junitTask ->
			junitTask.description = 'Runs tests on the JUnit Platform.'

			junitTask.inputs.property('disableStandardTestTask', junitExtension.disableStandardTestTask)
			junitTask.inputs.property('includedEngines', junitExtension.engines.include)
			junitTask.inputs.property('excludedEngines', junitExtension.engines.exclude)
			junitTask.inputs.property('includedTags', junitExtension.tags.include)
			junitTask.inputs.property('excludedTags', junitExtension.tags.exclude)
			junitTask.inputs.property('classNameFilter', junitExtension.classNameFilter)

			def reportsDir = junitExtension.reportsDir ?: project.file("build/test-results/junit5")
			junitTask.outputs.dir reportsDir

			if (junitExtension.logManager) {
				systemProperty 'java.util.logging.manager', junitExtension.logManager
			}

			defineTaskDependencies(project, junitTask, junitExtension)

			junitTask.classpath = project.sourceSets.test.runtimeClasspath
			junitTask.main = ConsoleRunner.class.getName()

			junitTask.args buildArgs(project, junitExtension, reportsDir)
		}
	}

	private void defineTaskDependencies(project, junitTask, junitExtension) {
		def testTask = project.tasks.getByName('test')
		def testClasses = project.tasks.getByName('testClasses')

		junitTask.dependsOn testClasses
		testTask.dependsOn junitTask

		if (junitExtension.disableStandardTestTask) {
			testTask.enabled = false
		}
	}

	private ArrayList<String> buildArgs(project, junitExtension, reportsDir) {

		def args = ['--enable-exit-code', '--hide-details', '--all']

		if (junitExtension.classNameFilter) {
			args.add('-n')
			args.add(junitExtension.classNameFilter)
		}

		junitExtension.tags.include.each { tag ->
			args.add('-t')
			args.add(tag)
		}

		junitExtension.tags.exclude.each { tag ->
			args.add('-T')
			args.add(tag)
		}

		junitExtension.engines.include.each { engineId ->
			args.add('-e')
			args.add(engineId)
		}

		junitExtension.engines.exclude.each { engineId ->
			args.add('-E')
			args.add(engineId)
		}

		args.add('-r')
		args.add(reportsDir.getAbsolutePath())

		def rootDirs = []
		project.sourceSets.each { sourceSet ->
			rootDirs.add(sourceSet.output.classesDir)
			rootDirs.add(sourceSet.output.resourcesDir)
			rootDirs.addAll(sourceSet.output.dirs.files)
		}

		rootDirs.each { File root ->
			args.add(root.getAbsolutePath())
		}

		return args
	}

}