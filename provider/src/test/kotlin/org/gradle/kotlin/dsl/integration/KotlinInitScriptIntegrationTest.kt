package org.gradle.kotlin.dsl.integration

import org.gradle.kotlin.dsl.fixtures.AbstractIntegrationTest
import org.gradle.kotlin.dsl.fixtures.DeepThought
import org.gradle.kotlin.dsl.fixtures.LeaksFileHandles
import org.gradle.kotlin.dsl.fixtures.withFolders
import org.gradle.kotlin.dsl.fixtures.withIsolatedTestKitDir

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat

import org.junit.Test


class KotlinInitScriptIntegrationTest : AbstractIntegrationTest() {

    @Test
    @LeaksFileHandles
    fun `initscript classpath`() {

        withClassJar("fixture.jar", DeepThought::class.java)

        val initScript =
            withFile("init.gradle.kts", """

                initscript {
                    dependencies { classpath(files("fixture.jar")) }
                }

                val computer = ${DeepThought::class.qualifiedName}()
                val answer = computer.compute()
                println("*" + answer + "*")
            """)

        assert(
            build("-I", initScript.canonicalPath)
                .output.contains("*42*"))
    }

    @Test
    fun `initscript file path is resolved relative to parent script dir`() {

        val initScript =
            withFile("gradle/init.gradle.kts", """
                apply { from("./answer.gradle.kts") }
            """)

        withFile("gradle/answer.gradle.kts", """
            rootProject {
                val answer by extra { "42" }
            }
        """)

        withBuildScript("""
            val answer: String by extra
            println("*" + answer + "*")
        """)

        assert(
            build("-I", initScript.canonicalPath)
                .output.contains("*42*"))
    }

    @Test
    @LeaksFileHandles
    fun `Kotlin init scripts from init dir can add buildscript repositories to projects`() {

        val testRepositoryDir = temporaryFolder.newFolder("test-repository")

        val isolatedTestKitDir = temporaryFolder.newFolder("test-kit")
        isolatedTestKitDir.withFolders {
            "init.d" {
                withFile("init.gradle.kts", """
                    allprojects {
                        buildscript.repositories {
                            maven {
                                name = "test-repository"
                                url = uri("${testRepositoryDir.toURI()}")
                            }
                        }
                    }
                """)
            }
        }

        withBuildScript("""
            buildscript {
                repositories.forEach {
                    println("*" + it.name + "*")
                }
            }
        """)

        withIsolatedTestKitDir(isolatedTestKitDir) {
            assertThat(
                build().output,
                containsString("*test-repository*"))
        }
    }
}

