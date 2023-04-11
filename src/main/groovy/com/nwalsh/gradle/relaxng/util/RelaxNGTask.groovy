package com.nwalsh.gradle.relaxng.util

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

import org.gradle.internal.os.OperatingSystem;

import org.xml.sax.ErrorHandler

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
@SuppressWarnings('ConfusingMethodName')
@SuppressWarnings('MethodCount')
abstract class RelaxNGTask extends DefaultTask {
  protected static final String SEMICOLON = ';'
  protected static final String FILE_SCHEME = 'file'

  protected static final String CWD = System.getProperty("user.dir")
  protected URI theBaseURI = new File(CWD).toURI()

  protected static final Boolean IS_WINDOWS = OperatingSystem.current().isWindows()
  protected static final Pattern WINDOWS_BROKEN_FILE_URI
    = Pattern.compile('^file://([A-Za-z]:.*)$')
  protected static final Pattern WINDOWS_FILENAME
    = Pattern.compile('^[A-Za-z]:.*$')
  protected boolean debugStarted = false
  protected ErrorHandler errorHandler

  protected Object outputFile = null
  protected String catalogList = null
  protected boolean debug = false
  protected String inputEncoding = null

  void output(Object output) {
    outputFile = resolveResource(output)
    // The output must be a File, not a URI
    if (outputFile instanceof URI) {
      if (outputFile.getScheme() == FILE_SCHEME) {
        outputFile = new File(outputFile.getPath())
      } else {
        throw new GradleException("Output must be a file.")
      }
    }
    show("Out: ${outputFile}")
  }

  void catalog(Object catalog) {
    if (catalogList == null) {
      catalogList = ""
    } else {
      catalogList += SEMICOLON
    }
    if (catalog in List) {
      catalogList += catalog.join(SEMICOLON)
    } else {
      catalogList += catalog.toString()
    }
    show("Opt: catalog=${catalogList}")
  }

  void debug(Boolean debug) {
    this.debug = debug
    // We don't show this change. If you put it at the end of a task
    // configuration, then you'll see debug information for the
    // execution phase but not the config phase. If you put it first,
    // you can get debug information for both.
  }

  void errorHandler(Object handler) {
    if (handler instanceof ErrorHandler) {
      errorHandler = handler
    } else {
      throw new GradleException("Error handler must be a org.xml.sax.ErrorHandler.")
    }
    show("Err: ${errorHandler.getClass().getName()}")
  }

  void encoding(String encoding) {
    inputEncoding = encoding
    show("Opt: (input)encoding=${inputEncoding}")
  }

  // ============================================================

  protected void show(String message) {
    if (debug) {
      if (!debugStarted) {
        println("Configuring RELAX NG validator task ${this.getName()}:")
        debugStarted = true
      }
      println(message)
    }
  }

  protected Object resolveResource(Object input) {
    if (input == null) {
      return null
    }

    if (input instanceof File) {
      return input
    }

    if (input instanceof URI) {
      useURIs = true
      return input
    }

    String path = input.toString()
    if (IS_WINDOWS) {
      path = fixWindowsPath(path)
      Matcher match = WINDOWS_BROKEN_FILE_URI.matcher(path)
      if (match.find()) {
        path = makeFileURI(match.group(1))
      } else {
        match = WINDOWS_FILENAME.matcher(path)
        if (match.find()) {
          path = makeFileURI(path)
        }
      }
    }

    URI uri
    try {
      uri = new URI(path)
      if (!uri.isAbsolute()) {
        uri = theBaseURI.resolve(path)
      }
    } catch (URISyntaxException ex) {
      uri = theBaseURI.resolve(path)      
    }

    if (uri.getScheme() == FILE_SCHEME) {
      return new File(uri.getPath())
    }

    useURIs = true
    return uri
  }

  protected File resolveFile(Object input) {
    if (input instanceof File) {
      return input
    }

    if (input instanceof URI) {
      if (input.getScheme() == FILE_SCHEME) {
        return new File(input)
      }
    }

    return null
  }
}
