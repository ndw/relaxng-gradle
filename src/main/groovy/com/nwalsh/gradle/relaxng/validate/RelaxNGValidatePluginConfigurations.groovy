package com.nwalsh.gradle.relaxng.validate

@Singleton
class RelaxNGValidatePluginConfigurations {
  // A singleton that holds the global configuration options.
  static final String DEFAULT = "RelaxNGValidatePluginConfigurations.DEFAULT"

  protected final Map<String, Map<String,Object>> options = [:]
  protected final Map<String, Map<String,Object>> pluginOptions = [:]

  void setOption(String name, String property, Object value) {
    if (!(name in options)) {
      options[name] = [:]
    }
    options[name][property] = value
  }

  void setPluginOption(String name, String property, Object value) {
    if (!(name in pluginOptions)) {
      pluginOptions[name] = [:]
    }
    pluginOptions[name][property] = value
  }
  
  Map<String,Object> getOptions() {
    return getOptions(DEFAULT)
  }

  Map<String,Object> getOptions(String name) {
    name in options ? options[name] : [:]
  }

  Map<String,Object> getPluginOptions() {
    return getPluginOptions(DEFAULT)
  }

  Map<String,Object> getPluginOptions(String name) {
    name in pluginOptions ? pluginOptions[name] : [:]
  }

  Boolean knownConfiguration(String name) {
    return name in options || name in advancedOptions || name in pluginOptions
  }
}

class RelaxNGValidatePluginConfiguration implements RelaxNGValidatePluginOptions {
  private String configname
  RelaxNGValidatePluginConfiguration(String name) {
    configname = name
  }

  void setOption(String name, Object value) {
    RelaxNGValidatePluginConfigurations.instance.setOption(configname, name, value)
  }

  void setPluginOption(String name, Object value) {
    RelaxNGValidatePluginConfigurations.instance.setPluginOption(configname, name, value)
  }
}
