package com.nwalsh.gradle.relaxng.validate

import org.gradle.workers.WorkAction

import com.nwalsh.gradle.relaxng.util.JingResolver

import com.thaiopensource.relaxng.util.Driver
import com.thaiopensource.resolver.catalog.CatalogResolver
import com.thaiopensource.util.PropertyMap
import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.util.UriOrFile
import com.thaiopensource.validate.SchemaReader
import com.thaiopensource.validate.ValidateProperty
import com.thaiopensource.validate.ValidationDriver
import com.thaiopensource.validate.prop.rng.RngProperty
import com.thaiopensource.validate.rng.CompactSchemaReader
import com.thaiopensource.xml.sax.ErrorHandlerImpl

import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// This class has been separated out because the validator gets run
// in two different ways: if an errorHandler is passed in, we can't
// run it as a worker (I can't figure out how to pass an object through
// the interface to workers) so we run it directly. If we aren't
// passed an errorHandler, we queue the job up and it gets executed
// in parallel.

class RelaxNGValidateImpl {
  private final Map<String,String> options
  private final ErrorHandler errorHandler

  public RelaxNGValidateImpl(Map<String,String> args, ErrorHandler handler) {
    options = args
    errorHandler = handler
  }

  public RelaxNGValidateImpl(Map<String,String> args) {
    this(args, new ErrorHandlerImpl(System.out))
  }

  public void execute() {
    def debug = options.getOrDefault("debug", "false") == "true"
    def checkIdRef = options.getOrDefault("idref", "true") == "true"
    def assertValid = options.getOrDefault("assert", "true") == "true"
    def compact = options.getOrDefault("compact", "false") == "true"
    def feasible = options.getOrDefault("feasible", "false") == "true"
    def input = options.get("input")
    def output = options.get("output")
    def schema = options.get("schema")
    def catalogs = options.getOrDefault("catalog", null)
    def encoding = options.getOrDefault("encoding", null)

    if (debug) {
      println("RELAX NG Validation task:")
      println("  Input source: ${input}")
      println("  Input schema: ${schema}")
      println("  Options:")
      println("    Check idrefs? ${showOpt('idref', 'true')}")
      println("    Compact syntax? ${showOpt('compact', 'false')}")
      println("    Feasibly valid? ${showOpt('feasible', 'false')}")
      println("    Encoding: ${encoding == null ? 'unspecified' : encoding}")
      println("    Fail task if invalid? ${showOpt('assert', 'true')}")
      if (catalogs == null) {
        println("    Catalogs: (system default catalogs)")
      } else {
        println("    Catalogs: ${catalogs}")
      }
      if (output == null) {
        println("  No output will be produced")
      } else {
        println("  Output result: ${output}")
      }
    }

    PropertyMapBuilder properties = new PropertyMapBuilder();
    properties.put(ValidateProperty.ERROR_HANDLER, errorHandler);

    if (schema == null) {
      throw new IllegalArgumentException("No schema provided")
    }

    RngProperty.CHECK_ID_IDREF.add(properties);
    if (!checkIdRef) {
      properties.put(RngProperty.CHECK_ID_IDREF, null);
    }

    if (feasible) {
      RngProperty.FEASIBLE.add(properties);
    }

    JingResolver resolver = new JingResolver(catalogs)
    properties.put(ValidateProperty.RESOLVER, resolver)

    SchemaReader sr = null
    if (compact) {
      sr = CompactSchemaReader.getInstance()
    }

    try {
      ValidationDriver driver = new ValidationDriver(properties.toPropertyMap(),
                                                     properties.toPropertyMap(),
                                                     sr)
      InputSource insrc = ValidationDriver.uriOrFileInputSource(schema)
      //println("SCHEMA1: ${insrc.getSystemId()}")
      if (encoding != null) {
        insrc.setEncoding(encoding)
      }
      boolean loaded = false
      try {
        loaded = driver.loadSchema(insrc)
      } catch (SAXException se) {
        // If we got a SAX exception loading the schema, and the user didn't
        // explicitly specify a compact setting, and the schema filename
        // ends with ".rnc", try again using the compact schema parser
        if (!options.containsKey("compact") && schema.endsWith(".rnc")) {
          if (debug) {
            println("  Failed to load schema as XML, retrying as a compact syntax schema")
          }
          sr = CompactSchemaReader.getInstance()
          driver = new ValidationDriver(properties.toPropertyMap(),
                                        properties.toPropertyMap(),
                                        sr)
          insrc = ValidationDriver.uriOrFileInputSource(schema)
          //println("SCHEMA2: ${insrc.getSystemId()}")
          if (encoding != null) {
            insrc.setEncoding(encoding)
          }
          loaded = driver.loadSchema(insrc)
        }
      }
      if (loaded) {
        insrc = ValidationDriver.uriOrFileInputSource(input)
        //println("INPUT: ${insrc.getSystemId()}")
        if (!driver.validate(insrc)) {
          if (assertValid) {
            throw new IllegalArgumentException("Document is invalid: ${input}")
          }
        }
      } else {
        throw new IllegalArgumentException("Failed to load schema: ${schema}")
      }
    } catch (SAXException se) {
      errorHandler.error(new SAXParseException(se.getMessage(), null, se))
      if (assertValid) {
        throw se
      }
    } catch (IOException ioe) {
      errorHandler.error(new SAXParseException(ioe.getMessage(), null, ioe))
      if (assertValid) {
        throw ioe
      }
    }

    if (output != null) {
      URL asUrl = new URL(UriOrFile.toUri(input))
      //println("COPY: ${asUrl}")
      InputStream is = new BufferedInputStream(asUrl.openStream())
      OutputStream os = new FileOutputStream(new File(output))
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

  private String showOpt(String name, String defvalue) {
    if (name in options) {
      return options[name]
    } else {
      return "${defvalue} (by default)"
    }
  }
}
