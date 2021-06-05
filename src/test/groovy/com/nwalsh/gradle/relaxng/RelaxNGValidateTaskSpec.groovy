package com.nwalsh.gradle.relaxng

import org.gradle.testkit.runner.BuildResult
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import java.nio.file.Files
import java.nio.file.Paths

import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification

class RelaxNGValidateTaskSpec extends Specification {
  File systemTempDir = new File(new File(System.getProperty("java.io.tmpdir")).getCanonicalPath())

  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder(systemTempDir)

  File outputDir

  File gradle
  File rng
  File rnc
  File xml1
  File xml2

  String unixPath(File file) {
    file.getPath().replace('\\', '/')
  }

  @SuppressWarnings(['DuplicateStringLiteral'])
  void setup() {
    gradle = testProjectDir.newFile('build.gradle')
    rng = testProjectDir.newFile('schema.rng')
    rnc = testProjectDir.newFile('schema.rnc')

    testProjectDir.newFolder('input')
    xml1 = testProjectDir.newFile('input/xml1.xml')

    outputDir = testProjectDir.newFolder('build')
  }

  String fileAsString(File file) {
    new String(Files.readAllBytes(Paths.get(file.toURI())))
  }

  File outputFile(String path) {
    new File(outputDir, path)
  }

  String outputPath(String path) {
    unixPath(outputFile(path))
  }

  private BuildResult execute() {
    GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withPluginClasspath()
      .withArguments(':validate')
      .forwardOutput()
      .build()
  }

  def 'Valid against RNG'() {
    given:
    rng << '''
            <grammar xmlns="http://relaxng.org/ns/structure/1.0">
              <start>
                <ref name="doc"/>
              </start>
              <define name="doc">
                <element name="doc">
                  <oneOrMore>
                    <ref name="p"/>
                  </oneOrMore>
                </element>
              </define>
              <define name="p">
                <element name="p">
                  <text/>
                </element>
              </define>
            </grammar>
        '''

    xml1 << '''<doc><p>Hello, world.</p></doc>'''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng'
        }

        import com.nwalsh.gradle.relaxng.validate.RelaxNGValidateTask

        task validate(type: RelaxNGValidateTask) {
            input '${unixPath(xml1)}'
            output 'build/out1.xml'
            schema '${unixPath(rng)}'
        }
        """

    when:
    def result = execute()

    then:
    result.task(':validate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('out1.xml')).equals('<doc><p>Hello, world.</p></doc>')
  }

  def 'Valid against RNC'() {
    given:
    rnc << '''
            start = doc
            doc = element doc { p+ }
            p = element p { text }
        '''

    xml1 << '''<doc><p>Hello, world.</p></doc>'''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng'
        }

        import com.nwalsh.gradle.relaxng.validate.RelaxNGValidateTask

        task validate(type: RelaxNGValidateTask) {
            input '${unixPath(xml1)}'
            compact true
            output 'build/out1.xml'
            schema '${unixPath(rnc)}'
        }
        """

    when:
    def result = execute()

    then:
    result.task(':validate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('out1.xml')).equals('<doc><p>Hello, world.</p></doc>')
  }

  def 'Valid against implicit RNC'() {
    given:
    rnc << '''
            start = doc
            doc = element doc { p+ }
            p = element p { text }
        '''

    xml1 << '''<doc><p>Hello, world.</p></doc>'''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng'
        }

        import com.nwalsh.gradle.relaxng.validate.RelaxNGValidateTask

        task validate(type: RelaxNGValidateTask) {
            input '${unixPath(xml1)}'
            output 'build/out1.xml'
            schema '${unixPath(rnc)}'
        }
        """

    when:
    def result = execute()

    then:
    result.task(':validate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('out1.xml')).equals('<doc><p>Hello, world.</p></doc>')
  }

  def 'Invalid against RNG'() {
    given:
    rnc << '''
            start = doc
            doc = element doc { p+ }
            p = element p { text }
        '''

    xml1 << '''<doc><WRONG>Hello, world.</WRONG></doc>'''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng'
        }

        import com.nwalsh.gradle.relaxng.validate.RelaxNGValidateTask

        task validate(type: RelaxNGValidateTask) {
            input '${unixPath(xml1)}'
            output 'build/out1.xml'
            assertValid false
            schema '${unixPath(rnc)}'
        }
        """

    when:
    def result = execute()

    then:
    result.task(':validate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('out1.xml')).equals('<doc><WRONG>Hello, world.</WRONG></doc>')
  }

  def 'Invalid against RNG, assertValid'() {
    given:
    rnc << '''
            start = doc
            doc = element doc { p+ }
            p = element p { text }
        '''

    xml1 << '''<doc><WRONG>Hello, world.</WRONG></doc>'''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng'
        }

        import com.nwalsh.gradle.relaxng.validate.RelaxNGValidateTask

        task validate(type: RelaxNGValidateTask) {
            input '${unixPath(xml1)}'
            output 'build/out1.xml'
            assertValid true
            schema '${unixPath(rnc)}'
        }
        """

    when:
    def result = execute()

    then:
    thrown UnexpectedBuildFailure // expected, actually, but ... :-)
  }

}
