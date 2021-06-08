package com.nwalsh.gradle.relaxng.validate

// This trait is for initializing options. It's used by both the
// global configuration singleton and each individual instance of a
// RelaxNG task.

@SuppressWarnings('MethodCount')
trait RelaxNGValidatePluginOptions {
  abstract void setOption(String name, Object value)
  abstract void setPluginOption(String name, Object value)
  private String catalogList = ""

  // ============================================================

  void classpath(Object cp) {
    setPluginOption('classpath', cp)
  }

  void parallel(Boolean par) {
    setPluginOption('parallel', par)
  }

  // ============================================================

  // FIXME: there are more options to Jing

  void debug(Boolean debug) {
    setOption('debug', debug)
  }

  void assertValid(Boolean assertvalid) {
    setOption('assert', assertvalid)
  }

  void idref(Boolean check) {
    setOption('idref', check)
  }

  void catalog(Object catalog) {
    if (catalogList != "") {
      catalogList += ";"
    }
    if (catalog in List) {
      catalogList += catalog.join(";")
    } else {
      catalogList += catalog.toString()
    }
    setOption('catalog', catalogList)
  }

  void compact(Boolean compact) {
    setOption('compact', compact)
  }

  void encoding(String encoding) {
    setOption('encoding', encoding)
  }

  void feasible(Boolean feasible) {
    setOption('feasible', feasible)
  }

  void errorHandler(Object handler) {
    setOption('errorHandler', handler)
  }
}
