package com.nwalsh.gradle.relaxng.util

import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException

class CollectingErrorHandler implements ErrorHandler {
  private int errCount = 0
  private int fatalErrCount = 0
  private int warnCount = 0
  private final List<SAXParseException> exList
  private final PrintStream output

  CollectingErrorHandler() {
    this(null)
  }

  CollectingErrorHandler(PrintStream output) {
    this.output = output
    exList = new ArrayList<>()
  }

  public int errorCount() {
    return fatalErrCount + errCount
  }

  public int fatalErrorCount() {
    return fatalErrCount
  }

  public int warningCount() {
    return warnCount
  }

  public List<SAXParseException> getExceptions() {
    return exList
  }

  public void error(SAXParseException ex) {
    errCount++
    exList.add(ex)
    if (output != null) {
      output.println(ex.getMessage())
    }
  }

  public void fatalError(SAXParseException ex) {
    fatalErrCount++
    exList.add(ex)
    if (output != null) {
      output.println(ex.getMessage())
    }
  }

  public void warning(SAXParseException ex) {
    warnCount++
    exList.add(ex)
    if (output != null) {
      output.println(ex.getMessage())
    }
  }
}
