@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins

import com.acornui.build.AcornDependencies
import com.acornui.build.plugins.util.preventSnapshotDependencyCaching
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

@Suppress("unused")
class RootPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		val acornUiHome: String? by target.extra
		val acornVersion: String by target.extra
		val isComposite = acornUiHome != null && target.file(acornUiHome!!).exists()
		target.logger.lifecycle("isComposite=$isComposite")

		target.preventSnapshotDependencyCaching()

		target.pluginManager.apply("org.jetbrains.dokka")

		target.allprojects {
			AcornDependencies.putVersionProperties(project.extra)
			repositories {
				mavenLocal()
				jcenter()
				maven {
					url = project.uri("https://dl.bintray.com/kotlin/kotlin-eap/")
				}
				maven {
					url = project.uri("http://artifacts.acornui.com/mvn/")
				}
			}

			project.configurations.all {
				resolutionStrategy {
					// A workaround to composite builds not working - https://youtrack.jetbrains.com/issue/KT-30285
					if (isComposite) {
						configurations.all {
							resolutionStrategy.dependencySubstitution {
								listOf("utils", "core", "game", "spine", "test-utils").forEach {
									val id = ":acornui-$it"
									if (findProject(id) != null) {
										substitute(module("com.acornui:acornui-$it")).with(project(id))
									}
								}
								listOf("lwjgl", "webgl").forEach {
									val id = ":acornui-$it-backend"
									if (findProject(id) != null) {
										substitute(module("com.acornui:acornui-$it-backend")).with(project(id))
									}
								}
							}
						}
					}
					eachDependency {
						when {
							requested.group.startsWith("com.acornui") -> {
								useVersion(acornVersion)
							}
						}
					}
				}
			}

			tasks.findByPath("jsBrowserDistribution")?.let {
				// In Kotlin 1.3.70 this isn't ready yet, it will be overridden in application projects.
				it.enabled = false
			}
		}
	}
}