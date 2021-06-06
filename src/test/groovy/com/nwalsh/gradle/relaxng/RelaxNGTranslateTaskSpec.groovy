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

class RelaxNGTranslateTaskSpec extends Specification {
  File systemTempDir = new File(new File(System.getProperty("java.io.tmpdir")).getCanonicalPath())

  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder(systemTempDir)

  File outputDir

  File gradle
  File dtd
  File rng
  File rnc
  File xml1
  File xml2
  File expresult

  String unixPath(File file) {
    file.getPath().replace('\\', '/')
  }

  @SuppressWarnings(['DuplicateStringLiteral'])
  void setup() {
    gradle = testProjectDir.newFile('build.gradle')
    dtd = testProjectDir.newFile('schema.dtd')
    rng = testProjectDir.newFile('schema.rng')
    rnc = testProjectDir.newFile('schema.rnc')
    expresult = testProjectDir.newFile('expected-result')

    testProjectDir.newFolder('input')
    xml1 = testProjectDir.newFile('input/xml1.xml')
    xml2 = testProjectDir.newFile('input/xml2.xml')

    outputDir = testProjectDir.newFolder('build')
  }

  String fileAsString(File file) {
    String s = new String(Files.readAllBytes(Paths.get(file.toURI())))
    //println("s::${s}::")
    return s
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
      .withArguments(':translate')
      .forwardOutput()
      .build()
  }

  def 'Transform RNG to RNC'() {
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

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng.translate'
        }

        import com.nwalsh.gradle.relaxng.translate.RelaxNGTranslateTask

        task translate(type: RelaxNGTranslateTask) {
            input '${unixPath(rng)}'
            output 'build/output.rnc'
        }
        """

    expresult << '''start = doc
doc = element doc { p+ }
p = element p { text }
'''

    when:
    def result = execute()

    then:
    result.task(':translate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('output.rnc')).equals(fileAsString(expresult))
  }

  def 'Transform RNC to RNG'() {
    given:
    rnc << '''
            start = doc
            doc = element doc { p+ }
            p = element p { text }
        '''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng.translate'
        }

        import com.nwalsh.gradle.relaxng.translate.RelaxNGTranslateTask

        task translate(type: RelaxNGTranslateTask) {
            input '${unixPath(rnc)}'
            output 'build/output.rng'
        }
        """

    expresult << '''<?xml version="1.0" encoding="UTF-8"?>
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

    when:
    def result = execute()

    then:
    result.task(':translate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('output.rng')).equals(fileAsString(expresult))
  }

  def 'Transform DTD to RNC, inline attlist'() {
    given:
    dtd << '''
            <!ELEMENT doc (p)+>
            <!ELEMENT p (#PCDATA)>
        '''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng.translate'
        }

        import com.nwalsh.gradle.relaxng.translate.RelaxNGTranslateTask

        task translate(type: RelaxNGTranslateTask) {
            inlineAttlist true
            input '${unixPath(dtd)}'
            output 'build/output.rnc'
        }
        """

    expresult << '''doc = element doc { p+ }
p = element p { text }
start = doc
'''

    when:
    def result = execute()

    then:
    result.task(':translate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('output.rnc')).equals(fileAsString(expresult))
  }

  def 'Transform DTD to RNC'() {
    given:
    dtd << '''
            <!ELEMENT doc (p)+>
            <!ELEMENT p (#PCDATA)>
        '''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng.translate'
        }

        import com.nwalsh.gradle.relaxng.translate.RelaxNGTranslateTask

        task translate(type: RelaxNGTranslateTask) {
            input '${unixPath(dtd)}'
            output 'build/output.rnc'
        }
        """

    expresult << '''doc = element doc { attlist.doc, p+ }
attlist.doc &= empty
p = element p { attlist.p, text }
attlist.p &= empty
start = doc
'''

    when:
    def result = execute()

    then:
    result.task(':translate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('output.rnc')).equals(fileAsString(expresult))
  }

  def 'Transform DTD to XSD'() {
    given:
    dtd << '''
            <!ELEMENT doc (p)+>
            <!ELEMENT p (#PCDATA)>
        '''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng.translate'
        }

        import com.nwalsh.gradle.relaxng.translate.RelaxNGTranslateTask

        task translate(type: RelaxNGTranslateTask) {
debug true
            input '${unixPath(dtd)}'
            output 'build/output.xsd'
        }
        """

    expresult << '''<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified">
  <xs:element name="doc">
    <xs:complexType>
      <xs:sequence>
        <xs:element maxOccurs="unbounded" ref="p"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
  <xs:element name="p" type="xs:string"/>
</xs:schema>
'''

    when:
    def result = execute()

    then:
    result.task(':translate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('output.xsd')).equals(fileAsString(expresult))
  }

  def 'Transform XML to RNC'() {
    given:
    xml1 << '''
            <doc>
              <p>This is some text.</p>
            </doc>
            '''
    xml2 << '''
            <doc>
              <div>
                <p>This is some other text.</p>
              </div>
            </doc>
            '''

    gradle << """
        plugins {
            id 'com.nwalsh.gradle.relaxng.translate'
        }

        import com.nwalsh.gradle.relaxng.translate.RelaxNGTranslateTask

        task translate(type: RelaxNGTranslateTask) {
debug true
            input '${unixPath(xml1)}'
            input '${unixPath(xml2)}'
            output 'build/output.rnc'
        }
        """

    expresult << '''default namespace = ""

start =
  element doc {
    p
    | element div { p }
  }
p = element p { text }
'''

    when:
    def result = execute()

    then:
    result.task(':translate').outcome == TaskOutcome.SUCCESS
    fileAsString(outputFile('output.rnc')).equals(fileAsString(expresult))
  }

}
