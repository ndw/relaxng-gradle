package com.nwalsh.gradle.relaxng.validate

import org.gradle.api.Project
import org.gradle.api.Plugin

abstract class RelaxNGValidatePluginExtension {
  public static final String DEFAULT = "com.nwalsh.gradle.relaxng-validate.DEFAULT"
  private Map<String,RelaxNGValidatePluginConfiguration> configs = new HashMap<>()

  void configure(Closure cl) {
    this.configure(DEFAULT, cl)
  }
  
  void configure(String name, Closure cl) {
    if (!(name in configs)) {
      configs[name] = new RelaxNGValidatePluginConfiguration(name)
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

class RelaxNGValidatePlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.extensions.create("relaxng_validate", RelaxNGValidatePluginExtension)
    //project.task("relaxng_validate", type: RelaxNGValidateTask)
  }
}
