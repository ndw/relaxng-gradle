package com.nwalsh.gradle.relaxng.translate

class RelaxNGTranslatePluginConfiguration implements RelaxNGTranslatePluginOptions {
  private final String configname
  protected final Map<String,Object> options = [:]
  protected final Map<String,Object> pluginOptions = [:]
  
  RelaxNGTranslatePluginConfiguration(String name) {
        configname = name
    }

  Map<String,Object> getOptions() {
    return options
  }

  Map<String,Object> getPluginOptions() {
    return pluginOptions
  }

  void setOption(String name, Object value) {
    options[name] = value
  }

  void setPluginOption(String name, Object value) {
    pluginOptions[name] = value
  }
}
