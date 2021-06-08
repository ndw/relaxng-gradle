package com.nwalsh.gradle.relaxng.translate

// This trait is for initializing options. It's used by both the
// global configuration singleton and each individual instance of a
// RelaxNG task.

@SuppressWarnings('MethodCount')
trait RelaxNGTranslatePluginOptions {
  abstract void setOption(String name, Object value)
  abstract void setPluginOption(String name, Object value)
  private final List<String> catalogList = new ArrayList<>()
  private final List<String> xmlns = new ArrayList<>()

  // ============================================================

  void classpath(Object cp) {
    setPluginOption('classpath', cp)
  }

  void parallel(Boolean par) {
    setPluginOption('parallel', par)
  }

  // ============================================================

  void debug(Boolean debug) {
    setOption('debug', debug)
  }

  void inputType(String inputType) {
    setOption('inputType', inputType)
  }

  void outputType(String outputType) {
    setOption('outputType', outputType)
  }

  void encoding(String encoding) {
    setOption('encoding', encoding)
  }

  void inputEncoding(String inputEncoding) {
    setOption('inputEncoding', inputEncoding)
  }

  void outputEncoding(String outputEncoding) {
    setOption('outputEncoding', outputEncoding)
  }

  void namespace(String namespace) {
    xmlns.add(namespace)
    setOption('namespace', xmlns)
  }

  void colonReplacement(String colonReplacement) {
    setOption('colon-replacement', colonReplacement)
  }

  void elementDefine(String elementDefine) {
    setOption('element-define', elementDefine)
  }

  void attlistDefine(String attlistDefine) {
    setOption('attlist-define', attlistDefine)
  }

  void anyName(String anyName) {
    setOption('any-name', anyName)
  }

  void strictAny(Boolean strictAny) {
    setOption('strict-any', strictAny)
  }

  void annotationPrefix(String annotationPrefix) {
    setOption('annotation-prefix', annotationPrefix)
  }

  void inlineAttlist(Boolean inlineAttlist) {
    setOption('inline-attlist', inlineAttlist)
  }

  void generateStart(Boolean generateStart) {
    setOption('generate-start', generateStart)
  }

  void indent(Integer indent) {
    setOption('indent', indent)
  }

  void lineLength(Integer lineLength) {
    setOption('lineLength', lineLength)
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

  void errorHandler(Object handler) {
    setOption('errorHandler', handler)
  }
}
