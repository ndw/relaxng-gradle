package com.nwalsh.gradle.relaxng.validate

import org.gradle.api.Project
import org.gradle.api.Plugin

abstract class RelaxNGValidatePluginExtension {
  void configure(Closure cl) {
    this.configure(RelaxNGValidatePluginConfigurations.DEFAULT, cl)
  }
  
  void configure(String name, Closure cl) {
    cl.delegate = new RelaxNGValidatePluginConfiguration(name)
    cl()
  }
}

class RelaxNGValidatePlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.extensions.create("relaxng_validate", RelaxNGValidatePluginExtension)
    //project.task("relaxng_validate", type: RelaxNGValidateTask)
  }
}
