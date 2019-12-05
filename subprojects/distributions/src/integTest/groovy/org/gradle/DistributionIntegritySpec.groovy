/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle

import org.gradle.test.fixtures.file.TestFile
import spock.lang.Issue

import java.util.zip.ZipFile

class DistributionIntegritySpec extends DistributionIntegrationSpec {

    /*
     * Integration test to verify the integrity of the dependencies. The goal is to be able to check the dependencies
     * even we assume that the Gradle binaries are compromised. Ultimately this test should run outside of the Gradle.
     */

    @Override
    String getDistributionLabel() {
        'bin'
    }

    def "verify 3rd-party dependencies jar hashes"() {
        setup:
        // dependencies produced by Gradle and cannot be verified by this test
        def excluded = ['gradle-', 'fastutil-8.3.0-min', 'kotlin-compiler-embeddable-1.3.61-patched']

        def libDir = unpackDistribution().file('lib')
        def jars = collectJars(libDir)
        def filtered = jars.grep { jar ->
            // Filter out any excluded jars
            !excluded.any { jar.name.startsWith(it) }
        }
        Map<String, TestFile> depJars = filtered.collectEntries { [libDir.relativePath(it), it] }

        when:
        def errors = []
        depJars.each { String jarPath, TestFile jar ->
            def expected = THIRD_PARTY_LIBS[jarPath]
            def actual = jar.sha256Hash
            if (expected != actual) {
                errors << """SHA-256 hash does not match for ${jarPath}:
  expected=${expected}
  actual=${actual}
"""
            }
        }
        then:
        errors.empty

        when:
        def added = depJars.keySet() - THIRD_PARTY_LIBS.keySet()
        def removed = THIRD_PARTY_LIBS.keySet() - depJars.keySet()
        then:
        (added + removed).isEmpty()
    }

    @Issue(['https://github.com/gradle/gradle/issues/9990', 'https://github.com/gradle/gradle/issues/10038'])
    def "validate dependency archives"() {
        when:
        def jars = collectJars(unpackDistribution())
        then:
        jars != []

        when:
        def invalidArchives = jars.findAll {
            new ZipFile(it).withCloseable {
                def names = it.entries()*.name
                names.size() != names.toUnique().size()
            }
        }
        then:
        invalidArchives == []
    }

    private static def collectJars(TestFile file, Collection<File> acc = []) {
        if (file.name.endsWith('.jar')) {
            acc.add(file)
        }
        if (file.isDirectory()) {
            file.listFiles().each { f -> collectJars(f, acc) }
        }
        acc
    }
}
