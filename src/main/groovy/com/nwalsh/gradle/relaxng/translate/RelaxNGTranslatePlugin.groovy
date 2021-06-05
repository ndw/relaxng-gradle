package com.nwalsh.gradle.relaxng.translate

import org.gradle.api.Project
import org.gradle.api.Plugin

abstract class RelaxNGTranslatePluginExtension {
  void configure(Closure cl) {
    this.configure(RelaxNGTranslatePluginConfigurations.DEFAULT, cl)
  }
  
  void configure(String name, Closure cl) {
    cl.delegate = new RelaxNGTranslatePluginConfiguration(name)
    cl()
  }
}

class RelaxNGTranslatePlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.extensions.create("relaxng_translate", RelaxNGTranslatePluginExtension)
    //project.task("relaxng_translate", type: RelaxNGTranslateTask)
  }
}
