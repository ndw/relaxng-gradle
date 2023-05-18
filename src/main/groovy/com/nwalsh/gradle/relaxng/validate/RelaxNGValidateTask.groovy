package com.nwalsh.gradle.relaxng.validate

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.file.FileCollection

import com.nwalsh.gradle.relaxng.util.RelaxNGTask
import com.nwalsh.gradle.relaxng.util.JingResolver

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.util.UriOrFile
import com.thaiopensource.validate.SchemaReader
import com.thaiopensource.validate.ValidateProperty
import com.thaiopensource.validate.ValidationDriver
import com.thaiopensource.validate.prop.rng.RngProperty
import com.thaiopensource.validate.rng.CompactSchemaReader
import com.thaiopensource.xml.sax.ErrorHandlerImpl

@SuppressWarnings('MethodCount')
class RelaxNGValidateTask extends RelaxNGTask {
  private Object inputPath = null
  private Object schemaPath = null
  private boolean assertInputValid = true
  private Boolean compactSchema = null
  private boolean feasiblyValid = false
  private boolean idrefChecking = true
  private OutputStream errorOutput = System.err
  private File errorOutputFile = null

  void input(Object input) {
    inputPath = resolveResource(input)
    if (inputPath instanceof File) {
      inputPath = inputPath.toString()
    }
    show("Inp: ${inputPath}")
  }

  void errorOutput(Object output) {
    if (output instanceof OutputStream) {
      errorOutput = output
      show("Err: ${output}")
      return
    }

    def errout = resolveResource(output)

    // The output must be a File, not a URI
    if (errout instanceof URI) {
      if (errout.getScheme() == FILE_SCHEME) {
        errorOutputFile = new File(errout.getPath())
        return
      } else {
        throw new GradleException("Error output must be a file or output stream.")
      }
    }

    errorOutputFile = errout
    show("Err: ${errout}")
  }


  void schema(Object input) {
    schemaPath = resolveResource(input)
    if (schemaPath instanceof File) {
      schemaPath = schemaPath.toString()
    }
    show("Sch: ${schemaPath}")
  }

  void assertValid(Boolean valid) {
    assertInputValid = valid
    show("Opt: assertValid=${assertInputValid}")
  }

  void compact(Boolean compact) {
    compactSchema = compact
    show("Opt: compact=${compactSchema}")
  }

  void feasible(Boolean feas) {
    feasiblyValid = feas
    show("Opt: feasible=${feasiblyValid}")
  }

  void idref(Boolean check) {
    idrefChecking = check
    show("Opt: idref=${idrefChecking}")
  }

  void options(Map optmap) {
    optmap.each { entry ->
      switch (entry.key) {
        case 'input':
          input(entry.value)
          break
        case 'schema':
          schema(entry.value)
          break
        case 'output':
          output(entry.value)
          break
        case 'errorOutput':
          errorOutput(entry.value)
          break
        case 'catalog':
          catalog(entry.value)
          break
        case 'assertValid':
          assertValid(entry.value)
          break
        case 'compact':
          compact(entry.value)
          break
        case 'debug':
          debug(entry.value)
          break
        case 'encoding':
          encoding(entry.value)
          break
        case 'feasible':
          feasible(entry.value)
          break
        case 'idref':
          idref(entry.value)
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
    FileCollection files = project.files()
    if (inputPath != null) {
      files += project.files(inputPath)
    }
    if (schemaPath != null) {
      files += project.files(schemaPath)
    }
    return files
  }

  @Optional
  @OutputFiles
  FileCollection getOutputFiles() {
    FileCollection files = project.files([])
    File output = resolveFile(outputFile)
    if (output != null) {
      files += project.files(output)
    }
    return files
  }

  @TaskAction
  @SuppressWarnings('AbcMetric')
  @SuppressWarnings('MethodSize')
  @SuppressWarnings('CyclomaticComplexity')
  void run() {
    boolean userSuppliedErrorHandler = true
    if (errorHandler == null) {
      errorHandler = new ErrorHandlerImpl(errorOutput)
      userSuppliedErrorHandler = false
    }

    if (errorOutputFile != null) {
      errorOutput = new FileOutputStream(errorOutputFile)
    }

    if (debug) {
      println("RELAX NG Validation task:")
      println("  Input source: ${inputPath}")
      println("  Input schema: ${schemaPath}")
      println("  Options:")
      println("    Check idrefs? ${idrefChecking}")
      println("    Compact syntax? ${compactSchema == null ? 'implicit' : compactSchema}")
      println("    Feasibly valid? ${feasiblyValid}")
      println("    Encoding: ${inputEncoding == null ? 'unspecified' : inputEncoding}")
      println("    Fail task if invalid? ${assertInputValid}")
      if (catalogList == null) {
        println("    Catalogs: (system default catalogs)")
      } else {
        println("    Catalogs: ${catalogList}")
      }
      if (!errorHandler instanceof ErrorHandlerImpl) {
        println("    Error handler: ${errorHandler.getClass().getName()}")
      }
      if (outputFile == null) {
        println("  No output will be produced")
      } else {
        println("  Output result: ${outputFile}")
      }
    }

    PropertyMapBuilder properties = new PropertyMapBuilder();
    properties.put(ValidateProperty.ERROR_HANDLER, errorHandler);

    if (schemaPath == null) {
      throw new IllegalArgumentException("No schema provided")
    }

    RngProperty.CHECK_ID_IDREF.add(properties);
    if (!idrefChecking) {
      properties.put(RngProperty.CHECK_ID_IDREF, null);
    }

    if (feasiblyValid) {
      RngProperty.FEASIBLE.add(properties);
    }

    JingResolver resolver = new JingResolver(catalogList)
    properties.put(ValidateProperty.RESOLVER, resolver)

    SchemaReader sr = null
    if (compactSchema != null && compactSchema) {
      sr = CompactSchemaReader.getInstance()
    }

    try {
      ValidationDriver driver = new ValidationDriver(properties.toPropertyMap(),
                                                     properties.toPropertyMap(),
                                                     sr)
      InputSource insrc = ValidationDriver.uriOrFileInputSource(schemaPath)
      //println("SCHEMA1: ${insrc.getSystemId()}")
      if (inputEncoding != null) {
        insrc.setEncoding(inputEncoding)
      }
      boolean loaded = false
      try {
        loaded = driver.loadSchema(insrc)
      } catch (SAXException se) {
        // If we got a SAX exception loading the schema, and the user didn't
        // explicitly specify a compact setting, and the schema filename
        // ends with ".rnc", try again using the compact schema parser
        if (compactSchema == null && schemaPath.toString().endsWith(".rnc")) {
          if (debug) {
            println("  Failed to load schema as XML, retrying as a compact syntax schema")
          }

          if (!userSuppliedErrorHandler) {
            // If we got errors from the attempt to load the RNC file as
            // an RNG file, and if we aren't using an error handler supplied
            // by the user, toss out the error handler and make a new one.
            errorHandler = new ErrorHandlerImpl(System.out)
            properties.put(ValidateProperty.ERROR_HANDLER, errorHandler);
          }

          sr = CompactSchemaReader.getInstance()
          driver = new ValidationDriver(properties.toPropertyMap(),
                                        properties.toPropertyMap(),
                                        sr)
          insrc = ValidationDriver.uriOrFileInputSource(schemaPath)
          //println("SCHEMA2: ${insrc.getSystemId()}")
          if (inputEncoding != null) {
            insrc.setEncoding(inputEncoding)
          }
          loaded = driver.loadSchema(insrc)
        }
      }
      if (loaded) {
        insrc = ValidationDriver.uriOrFileInputSource(inputPath)
        //println("INPUT: ${insrc.getSystemId()}")
        if (!driver.validate(insrc)) {
          if (assertInputValid) {
            throw new IllegalArgumentException("Document is invalid: ${inputPath}")
          }
        }
      } else {
        throw new IllegalArgumentException("Failed to load schema: ${schemaPath}")
      }
    } catch (SAXException se) {
      errorHandler.error(new SAXParseException(se.getMessage(), null, se))
      if (assertInputValid) {
        throw se
      }
    } catch (IOException ioe) {
      errorHandler.error(new SAXParseException(ioe.getMessage(), null, ioe))
      if (assertInputValid) {
        throw ioe
      }
    }

    if (outputFile != null) {
      URL asUrl = new URL(UriOrFile.toUri(inputPath))
      //println("COPY: ${asUrl}")
      InputStream is = new BufferedInputStream(asUrl.openStream())
      OutputStream os = new FileOutputStream(outputFile)
      byte[] buffer = new byte[8192]
      int len = is.read(buffer, 0, buffer.size())
      while (len >= 0) {
        os.write(buffer, 0, len)
        len = is.read(buffer, 0, buffer.size())
      }
      os.close()
      is.close()
    }
  }
}

