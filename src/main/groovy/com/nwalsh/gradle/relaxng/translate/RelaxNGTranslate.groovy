package com.nwalsh.gradle.relaxng.translate

import org.gradle.workers.WorkAction
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
import com.nwalsh.gradle.relaxng.util.JingResolver

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class RelaxNGTranslate implements WorkAction<RelaxNGTranslateWorkParameters> {
  private static final Set<String> VALID_INPUT_TYPES = ["rng", "rnc", "dtd", "xml"]
  private static final Set<String> VALID_OUTPUT_TYPES = ["rng", "rnc", "xsd", "dtd"]
  private ErrorHandlerImpl errorHandler = new ErrorHandlerImpl()
  private final Map<String,Object> options = [:]
  private boolean debug = false
  private JingResolver resolver = null;
  private String outputEncoding = "UTF-8"
  private Integer outputIndent = 2
  private Integer outputLineLength = 72

  @Override
  void execute() {
    String[] args = parameters.arguments.get() as String[]
    options["input"] = []
    options["namespace"] = []
    int pos = 0
    while (pos < args.length) {
      def name = args[pos].substring(1)
      if (options.containsKey(name) && options[name] in List) {
        options[name].add(args[pos+1])
      } else {
        options[name] = args[pos+1]
      }
      pos += 2
    }

    // THEY'RE ALL STRINGS NOW!
    debug = options.getOrDefault("debug", "false") == "true"
    outputEncoding = options.getOrDefault("outputEncoding", outputEncoding)
    outputIndent = Integer.parseInt(options.getOrDefault("indent", outputIndent.toString()))
    outputLineLength = Integer.parseInt(options.getOrDefault("lineLength", outputLineLength.toString()))
    resolver = new JingResolver(options.getOrDefault("catalog", null))

    if (!("outputEncoding" in options) && "encoding" in options) {
      outputEncoding = options["encoding"]
    }

    def input = options.get("input")
    if (input == null) {
      throw new IllegalArgumentException("Input is required")
    }
    def inputType = options.getOrDefault("inputType", null)
    if (inputType == null) {
      pos = input.get(0).lastIndexOf(".")
      if (pos > 0) {
        inputType = input.get(0).substring(pos+1)
      }
    }
    if (inputType == null || !VALID_INPUT_TYPES.contains(inputType)) {
      throw new IllegalArgumentException("Cannot determine input type from ${input.get(0)}")
    }
    if (input.size() > 1 && inputType != "xml") {
      throw new IllegalArgumentException("Multiple inputs are only allowed for the 'xml' input type")
    }

    def output = options.get("output")
    if (output == null) {
      throw new IllegalArgumentException("Output is required")
    }
    def outputType = options.getOrDefault("outputType", null)
    if (outputType == null) {
      pos = output.lastIndexOf(".")
      if (pos > 0) {
        outputType = output.substring(pos+1)
      }
    }
    if (outputType == null || !VALID_OUTPUT_TYPES.contains(outputType)) {
      throw new IllegalArgumentException("Cannot determine output type from ${output}")
    }

    if (debug) {
      println("RELAX NG Translation task:")
      if (input.size() > 1) {
        println("  Input sources: ${input}")
      } else {
        println("  Input source: ${input.get(0)}")
      }
      println("  Input type: ${inputType}")
      println("  Output result: ${output}")
      println("  Output type: ${outputType}")
    }

    if (inputType == "rnc" || inputType == "rng") {
      translateRx(inputType, input.get(0), output, outputType)
    } else if (inputType == "dtd") {
      translateDtd(input.get(0), output, outputType)
    } else if (inputType == "xml") {
      translateXml(input, output, outputType)
    } else {
      throw new UnsupportedOperationException("Unexpected input type: ${inputType}")
    }
  }

  private void translateRx(String inputType, String input, String output, String outputType) {
    InputFormat inputFormat = (inputType == "rnc"
                               ? new CompactParseInputFormat()
                               : new SAXParseInputFormat())
    OutputFormat outputFormat = createOutputFormat(outputType)

    List<String> inputParams = new ArrayList<>()
    if (options.containsKey("inputEncoding") || options.containsKey("encoding")) {
      String encoding = options.getOrDefault("inputEncoding",
                                             options.getOrDefault("encoding", "utf-8"))
      inputParams.add("encoding=${encoding}")
    }

    List<String> outputParams = new ArrayList<>()
    outputParams.add("encoding=${outputEncoding}")
    outputParams.add("indent=${outputIndent}")
    outputParams.add("lineLength=${outputLineLength}")

    if (debug) {
      println("  Input parameters: ${inputParams}")
      println("  Output parameters: ${outputParams}")
    }

    SchemaCollection sc = inputFormat.load(UriOrFile.toUri(input),
                                           inputParams as String[],
                                           outputType, errorHandler, resolver)
    OutputDirectory od = new LocalOutputDirectory(sc.getMainUri(),
                                                  new File(output), ".${outputType}",
                                                  "UTF-8", 72, 2)
    outputFormat.output(sc, od, outputParams as String[], "rnc", errorHandler)
  }

  private void translateDtd(String input, String output, String outputType) {
    InputFormat inputFormat = new DtdInputFormat();
    OutputFormat outputFormat = createOutputFormat(outputType)
    List<String> inputParams = new ArrayList<>()
    // The DTD module doesn't accept an encoding parameter ¯\_(ツ)_/¯
    inputParams.addAll(options.get("namespace"))

    // Can't put this in [].each{} closure because the options field
    // goes out of scope. There must be a way around that...
    for (String opt : ['colon-replacement', 'element-define', 'attlist-define',
                       'any-name', 'annotation-prefix']) {
      if (options.containsKey(opt)) {
        inputParams.add("${opt}=${options.get(opt)}")
      }
    }

    if (options.getOrDefault('strict-any', 'false') == 'true') {
      inputParams.add('strict-any')
    }

    if (options.containsKey('inline-attlist')) {
      if (options.get('inline-attlist') == "true") {
        inputParams.add('inline-attlist')
      } else {
        inputParams.add('no-inline-attlist')
      }
    }

    if (options.containsKey('generate-start')) {
      if (options.get('generate-start') == "true") {
        inputParams.add('generate-start')
      } else {
        inputParams.add('no-generate-start')
      }
    }

    List<String> outputParams = new ArrayList<>()
    outputParams.add("encoding=${outputEncoding}")
    outputParams.add("indent=${outputIndent}")
    outputParams.add("lineLength=${outputLineLength}")

    if (debug) {
      println("  Input parameters: ${inputParams}")
      println("  Output parameters: ${outputParams}")
    }

    SchemaCollection sc = inputFormat.load(UriOrFile.toUri(input),
                                           inputParams as String[],
                                           outputType, errorHandler, resolver)

    OutputDirectory od = new LocalOutputDirectory(sc.getMainUri(),
                                                  new File(output), ".${outputType}",
                                                  "UTF-8", 72, 2)
    outputFormat.output(sc, od, outputParams as String[], "rnc", errorHandler)
  }

  private void translateXml(List<String> input, String output, String outputType) {
    MultiInputFormat inputFormat = new XmlInputFormat();
    OutputFormat outputFormat = createOutputFormat(outputType)
    List<String> inputParams = new ArrayList<>()
    if (options.containsKey("inputEncoding") || options.containsKey("encoding")) {
      String encoding = options.getOrDefault("inputEncoding", options.getOrDefault("encoding", "utf-8"))
      inputParams.add("encoding=${encoding}")
    }

    List<String> outputParams = new ArrayList<>()
    outputParams.add("encoding=${outputEncoding}")
    outputParams.add("indent=${outputIndent}")
    outputParams.add("lineLength=${outputLineLength}")

    if (debug) {
      println("  Input parameters: ${inputParams}")
      println("  Output parameters: ${outputParams}")
    }

    List<String> uris = []
    for (String src : input) {
      uris.add(UriOrFile.toUri(src))
    }
    SchemaCollection sc = inputFormat.load(uris as String[],
                                           inputParams as String[],
                                           outputType, errorHandler, resolver)
    OutputDirectory od = new LocalOutputDirectory(sc.getMainUri(),
                                                  new File(output), "${outputType}",
                                                  "UTF-8", 72, 2)
    outputFormat.output(sc, od, outputParams as String[], "rnc", errorHandler)
  }

  static private OutputFormat createOutputFormat(String name) {
    if (name.equalsIgnoreCase("dtd"))
      return new DtdOutputFormat();
    else if (name.equalsIgnoreCase("rng"))
      return new RngOutputFormat();
    else if (name.equalsIgnoreCase("xsd"))
      return new XsdOutputFormat();
    else if (name.equalsIgnoreCase("rnc"))
      return new RncOutputFormat();
    return null;
  }

}
