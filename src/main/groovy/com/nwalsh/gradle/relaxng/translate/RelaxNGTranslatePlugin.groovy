package com.nwalsh.gradle.relaxng.translate

import org.gradle.api.Project
import org.gradle.api.Plugin

abstract class RelaxNGTranslatePluginExtension {
  public static final String DEFAULT = "com.nwalsh.gradle.relaxng-validate.DEFAULT"
  private Map<String,RelaxNGTranslatePluginConfiguration> configs = new HashMap<>()

  void configure(Closure cl) {
    this.configure(DEFAULT, cl)
  }
  
  void configure(String name, Closure cl) {
    if (!(name in configs)) {
      configs[name] = new RelaxNGTranslatePluginConfiguration(name)
    }
    cl.delegate = configs[name]
    cl()
  }

  Map<String,Object> getOptions(String name) {
    Map<String,Object> opts = [:]
    if (DEFAULT in configs) {
      configs[DEFAULT].getOptions().each { key, val ->
        opts[key] = val
      }
    }
    if (name in configs) {
      configs[name].getOptions().each { key, val ->
        opts[key] = val
      }
    }
    return opts
  }

  Map<String,Object> getPluginOptions(String name) {
    Map<String,Object> opts = [:]
    if (DEFAULT in configs) {
      configs[DEFAULT].getPluginOptions().each { key, val ->
        opts[key] = val
      }
    }
    if (name in configs) {
      configs[name].getPluginOptions().each { key, val ->
        opts[key] = val
      }
    }
    return opts
  }

  Set<String> configurations() {
    return configs.keySet()
  }
}

class RelaxNGTranslatePlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.extensions.create("relaxng_translate", RelaxNGTranslatePluginExtension)
    //project.task("relaxng_translate", type: RelaxNGTranslateTask)
  }
}
