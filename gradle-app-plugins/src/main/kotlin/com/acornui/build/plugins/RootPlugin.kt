@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins

import com.acornui.build.AcornDependencies
import com.acornui.build.plugins.util.preventSnapshotDependencyCaching
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.*

@Suppress("unused")
class RootPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		target.extensions.create<AcornUiRootExtension>("acornuiRoot")

		val acornVersion: String by target.extra
		target.preventSnapshotDependencyCaching()

		target.pluginManager.apply("org.jetbrains.dokka")

		target.allprojects {
			AcornDependencies.addVersionProperties(project.extra)
			repositories {
				mavenLocal()
				jcenter()
				maven {
					url = project.uri("https://dl.bintray.com/kotlin/kotlin-dev/")
				}
				maven {
					url = project.uri("http://artifacts.acornui.com/mvn/")
				}
			}

			project.configurations.all {
				resolutionStrategy {
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


open class AcornUiRootExtension {

	/**
	 * The ant-style patterns for which resource files will have token replacement.
	 */
	var textFilePatterns = listOf("asp", "aspx", "cfm", "cshtml", "css", "go", "htm", "html", "json", "jsp", "jspx",
			"php", "php3", "php4", "phtml", "rhtml", "txt").map { "*.$it" }
}

fun Project.acornuiRoot(init: AcornUiRootExtension.() -> Unit) {
	the<AcornUiRootExtension>().apply(init)
}

val Project.acornuiRoot
	get() : AcornUiRootExtension {
		return the()
	}