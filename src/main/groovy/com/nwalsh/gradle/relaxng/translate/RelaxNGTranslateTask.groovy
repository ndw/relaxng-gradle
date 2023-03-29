package com.nwalsh.gradle.relaxng.translate

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.file.FileCollection

import com.nwalsh.gradle.relaxng.util.RelaxNGTask
import com.nwalsh.gradle.relaxng.util.JingResolver

import com.thaiopensource.relaxng.edit.SchemaCollection;
import com.thaiopensource.relaxng.input.InputFormat;
import com.thaiopensource.relaxng.input.MultiInputFormat;
import com.thaiopensource.relaxng.input.dtd.DtdInputFormat;
import com.thaiopensource.relaxng.input.parse.compact.CompactParseInputFormat;
import com.thaiopensource.relaxng.input.parse.sax.SAXParseInputFormat;
import com.thaiopensource.relaxng.input.xml.XmlInputFormat;
import com.thaiopensource.relaxng.output.LocalOutputDirectory;
import com.thaiopensource.relaxng.output.OutputDirectory;
import com.thaiopensource.relaxng.output.OutputFormat;
import com.thaiopensource.relaxng.output.dtd.DtdOutputFormat;
import com.thaiopensource.relaxng.output.rnc.RncOutputFormat;
import com.thaiopensource.relaxng.output.rng.RngOutputFormat;
import com.thaiopensource.relaxng.output.xsd.XsdOutputFormat;
import com.thaiopensource.util.UriOrFile;
import com.thaiopensource.xml.sax.ErrorHandlerImpl;

@SuppressWarnings('MethodCount')
@SuppressWarnings('ConfusingMethodName')
class RelaxNGTranslateTask extends RelaxNGTask {
  private static final String RNG = 'rng'
  private static final String RNC = 'rnc'
  private static final String DTD = 'dtd'
  private static final String XML = 'xml'
  private static final String XSD = 'xsd'
  private static final String EQ = '='
  private static final String PERIOD = '.'
  private static final String UTF_8 = 'UTF-8'
  private static final int DEFAULT_LINE_LENGTH = 72
  private static final int DEFAULT_INDENT = 2

  private static final Set<String> VALID_INPUT_TYPES = [RNG, RNC, DTD, XML]
  private static final Set<String> VALID_OUTPUT_TYPES = [RNG, RNC, XSD, DTD]

  protected static final String INPUT_OPTION = 'input'
  protected static final String OUTPUT_OPTION = 'output'
  protected static final String SCHEMA_OPTION = 'schema'
  protected static final String PARALLEL_OPTION = 'parallel'

  private final def inputPaths = []
  private final def namespace = []
  private String annotationPrefix = null
  private String anyName = null
  private String attlistDefine = null
  private String colonReplacement = null
  private String elementDefine = null
  private Boolean generateStart = null
  private Integer indent = DEFAULT_INDENT
  private Boolean inlineAttlist = null
  private String inputType = null
  private Integer lineLength = DEFAULT_LINE_LENGTH
  private String outputEncoding = null
  private String outputType = null
  private Boolean strictAny = null

  private JingResolver resolver = null;

  void input(Object input) {
    def inputPath = resolveResource(input)
    if (inputPath instanceof File) {
      inputPath = inputPath.toString()
    }
    show("Inp: ${inputPath}")
    if (inputPath != null) {
      inputPaths.add(inputPath)
    }
  }
  
  void annotationPrefix(String prefix) {
    annotationPrefix = prefix
    show("Opt: annotationPrefix=${annotationPrefix}")
  }

  void anyName(String name) {
    anyName = name
    show("Opt: anyName=${anyName}")
  }

  void attlistDefine(String define) {
    attlistDefine = define
    show("Opt: attlistDefine=${attlistDefine}")
  }

  void colonReplacement(String colon) {
    colonReplacement = colon
    show("Opt: colonReplacement=${colonReplacement}")
  }

  void elementDefine(String define) {
    elementDefine = define
    show("Opt: elementDefine=${elementDefine}")
  }

  void generateStart(Boolean start) {
    generateStart = start
    show("Opt: generateStart=${generateStart}")
  }

  void indent(Integer indent) {
    this.indent = indent
    show("Opt: indent=${indent}")
  }

  void inlineAttlist(Boolean inline) {
    inlineAttlist = inline
    show("Opt: inlineAttlist=${inlineAttlist}")
  }

  void inputType(String itype) {
    inputType = itype
    show("Opt: inputType=${inputType}")
  }

  void lineLength(Integer llength) {
    lineLength = llength
    show("Opt: lineLength=${lineLength}")
  }

  void namespace(String ns) {
    if (ns.contains(EQ)) {
      int pos = ns.indexOf(EQ)
      String prefix = ns.substring(0, pos)
      if (prefix != "xmlns" && !prefix.startsWith("xmlns:")) {
        ns = "xmlns:${ns}"
      }
    } else {
      ns = "xmlns=${ns}"
    }      

    namespace.add(ns)
    show("Opt: namespace=${ns}")
  }

  // Synonym for encoding
  void inputEncoding(String enc) {
    encoding(enc)
  }

  void outputEncoding(String encoding) {
    outputEncoding = encoding
    show("Opt: outputEncoding=${outputEncoding}")
  }

  void outputType(String otype) {
    outputType = otype
    show("Opt: outputType=${outputType}")
  }

  void strictAny(Boolean strict) {
    strictAny = strict
    show("Opt: strictAny=${strictAny}")
  }

  @SuppressWarnings('CyclomaticComplexity')
  void options(Map optmap) {
    optmap.each { entry ->
      switch (entry.key) {
        case 'input':
          input(entry.value)
          break
        case 'output':
          output(entry.value)
          break
        case 'catalog':
          catalog(entry.value)
          break
        case 'debug':
          debug(entry.value)
          break
        case 'encoding':
          encoding(entry.value)
          break
        case 'annotationPrefix':
          annotationPrefix(entry.value)
          break
        case 'anyName':
          anyName(entry.value)
          break
        case 'attlistDefine':
          attlistDefine(entry.value)
          break
        case 'colonReplacement':
          colonReplacement(entry.value)
          break
        case 'elementDefine':
          elementDefine(entry.value)
          break
        case 'generateStart':
          generateStart(entry.value)
          break
        case 'indent':
          indent(entry.value)
          break
        case 'inlineAttlist':
          inlineAttlist(entry.value)
          break
        case 'inputType':
          inputType(entry.value)
          break
        case 'lineLength':
          lineLength(entry.value)
          break
        case 'namespace':
          namespace(entry.value)
          break
        case 'outputEncoding':
          outputEncoding(entry.value)
          break
        case 'outputType':
          outputType(entry.value)
          break
        case 'strictAny':
          strictAny(entry.value)
          break
        case 'errorHandler':
          errorHandler(entry.value)
          break
        default:
          show("Unknown option name; ignored: ${entry.key}")
          break
      }
    }
  }

  // ============================================================

  @InputFiles
  @SkipWhenEmpty
  FileCollection getInputFiles() {
    FileCollection files = project.files([])
    files += project.files(inputPaths)
    return files
  }

  @OutputFiles
  FileCollection getOutputFiles() {
    FileCollection files = project.files([])
    File output = resolveFile(outputFile)
    if (output == null) {
      throw new GradleException("An output file is required for RELAX NG translate")
    }
    files += project.files(output)
    return files
  }

  @SuppressWarnings('CyclomaticComplexity')
  @TaskAction
  void run() {
    if (errorHandler == null) {
      errorHandler = new ErrorHandlerImpl(System.out)
    }

    resolver = new JingResolver(catalogList)
    int pos = 0

    if (outputEncoding == null && inputEncoding != null) {
      outputEncoding = inputEncoding
    }
    if (inputEncoding == null) {
      inputEncoding = UTF_8
    } 
    if (outputEncoding == null) {
      outputEncoding = UTF_8
    } 

    if (inputPaths.isEmpty()) {
      throw new GradleException("Input is required")
    }

    if (inputType == null) {
      pos = inputPaths[0].lastIndexOf(PERIOD)
      if (pos > 0) {
        inputType = inputPaths[0].substring(pos+1)
      }
    }
    if (inputType == null || !VALID_INPUT_TYPES.contains(inputType)) {
      throw new GradleException("Cannot determine input type from ${input.get(0)}")
    }
    if (inputPaths.size() > 1 && inputType != XML) {
      throw new GradleException("Multiple inputs are only allowed for the 'xml' input type")
    }

    if (outputFile == null) {
      throw new IllegalArgumentException("Output is required")
    }
    if (outputType == null) {
      pos = outputFile.toString().lastIndexOf(PERIOD)
      if (pos > 0) {
        outputType = outputFile.toString().substring(pos+1)
      }
    }
    if (outputType == null || !VALID_OUTPUT_TYPES.contains(outputType)) {
      throw new GradleException("Cannot determine output type from ${output}")
    }

    if (debug) {
      println("RELAX NG Translation task:")
      if (inputPaths.size() > 1) {
        println("  Input sources: ${inputPaths}")
      } else {
        println("  Input source: ${inputPaths[0]}")
      }
      println("  Input type: ${inputType}")
      println("  Output result: ${outputFile}")
      println("  Output type: ${outputType}")
    }

    if (inputType == RNC || inputType == RNG) {
      translateRx()
    } else if (inputType == DTD) {
      translateDtd()
    } else if (inputType == XML) {
      translateXml()
    } else {
      throw new UnsupportedOperationException("Unexpected input type: ${inputType}")
    }
  }

  private void translateRx() {
    InputFormat inputFormat = (inputType == RNC
                               ? new CompactParseInputFormat()
                               : new SAXParseInputFormat())
    OutputFormat outputFormat = getOutputFormat(outputType)

    List<String> inputParams = new ArrayList<>()
    inputParams.add("encoding=${inputEncoding}")

    List<String> outputParams = new ArrayList<>()
    outputParams.add("encoding=${outputEncoding}")
    outputParams.add("indent=${indent}")
    outputParams.add("lineLength=${lineLength}")

    if (debug) {
      println("  Input parameters: ${inputParams}")
      println("  Output parameters: ${outputParams}")
    }

    SchemaCollection sc = inputFormat.load(UriOrFile.toUri(inputPaths[0]),
                                           inputParams as String[],
                                           outputType, errorHandler, resolver)
    OutputDirectory od = new LocalOutputDirectory(sc.getMainUri(),
                                                  outputFile, ".${outputType}",
                                                  UTF_8, DEFAULT_LINE_LENGTH, DEFAULT_INDENT)
    outputFormat.output(sc, od, outputParams as String[], RNC, errorHandler)
  }

  private void translateDtd() {
    InputFormat inputFormat = new DtdInputFormat();
    OutputFormat outputFormat = getOutputFormat(outputType)
    List<String> inputParams = new ArrayList<>()
    // The DTD module doesn't accept an encoding parameter ¯\_(ツ)_/¯
    inputParams.addAll(namespace)

    if (colonReplacement != null) {
      inputParams.add("colon-replacement=${colonReplacement}")
    }

    if (elementDefine != null) {
      inputParams.add("element-define=${elementDefine}")
    }

    if (attlistDefine != null) {
      inputParams.add("attlist-define=${attlistDefine}")
    }

    if (anyName != null) {
      inputParams.add("any-name=${anyName}")
    }

    if (annotationPrefix != null) {
      inputParams.add("annotation-prefix=${annotationPrefix}")
    }

    if (strictAny != null && strictAny) {
      inputParams.add('strict-any')
    }

    if (inlineAttlist != null && inlineAttlist) {
      inputParams.add('inline-attlist')
    } else {
      inputParams.add('no-inline-attlist')
    }

    if (generateStart != null && generateStart) {
      inputParams.add('generate-start')
    } else {
      inputParams.add('no-generate-start')
    }

    List<String> outputParams = new ArrayList<>()
    outputParams.add("encoding=${outputEncoding}")
    outputParams.add("indent=${indent}")
    outputParams.add("lineLength=${lineLength}")

    if (debug) {
      println("  Input parameters: ${inputParams}")
      println("  Output parameters: ${outputParams}")
    }

    SchemaCollection sc = inputFormat.load(UriOrFile.toUri(inputPaths[0]),
                                           inputParams as String[],
                                           outputType, errorHandler, resolver)

    OutputDirectory od = new LocalOutputDirectory(sc.getMainUri(),
                                                  outputFile, ".${outputType}",
                                                  UTF_8, DEFAULT_LINE_LENGTH, DEFAULT_INDENT)
    outputFormat.output(sc, od, outputParams as String[], RNC, errorHandler)
  }

  private void translateXml() {
    MultiInputFormat inputFormat = new XmlInputFormat();
    OutputFormat outputFormat = getOutputFormat(outputType)
    List<String> inputParams = new ArrayList<>()
    
    inputParams.add("encoding=${inputEncoding}")

    List<String> outputParams = new ArrayList<>()
    outputParams.add("encoding=${outputEncoding}")
    outputParams.add("indent=${indent}")
    outputParams.add("lineLength=${lineLength}")

    if (debug) {
      println("  Input parameters: ${inputParams}")
      println("  Output parameters: ${outputParams}")
    }

    List<String> uris = []
    inputPaths.each { path ->
      uris.add(UriOrFile.toUri(path.toString()))
    }

    SchemaCollection sc = inputFormat.load(uris as String[],
                                           inputParams as String[],
                                           outputType, errorHandler, resolver)
    OutputDirectory od = new LocalOutputDirectory(sc.getMainUri(),
                                                  outputFile, ".${outputType}",
                                                  UTF_8, DEFAULT_LINE_LENGTH, DEFAULT_INDENT)
    outputFormat.output(sc, od, outputParams as String[], RNC, errorHandler)
  }

  static private OutputFormat getOutputFormat(String name) {
    switch (name.toLowerCase()) {
      case RNC:
        return new RncOutputFormat()
      case RNG:
        return new RngOutputFormat()
      case XSD:
        return new XsdOutputFormat()
      case DTD:
        return new DtdOutputFormat()
      default:
        return null
    }
  }
}
