package com.nwalsh.gradle.relaxng.validate

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.InvalidUserDataException
import com.thaiopensource.xml.sax.ErrorHandlerImpl

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

class StandaloneRelaxNGValidate implements RelaxNGValidatePluginOptions {
  private final Project project
  protected String pluginConfig = RelaxNGValidatePluginExtension.DEFAULT
  protected final Map<String, Object> options = [:]
  protected final Map<String, Object> pluginOptions = [:]

  StandaloneRelaxNGValidate(Project project) {
    this.project = project
  }

  void input(Object input) {
    setOption("input", input)
  }

  void output(Object output) {
    setOption("output", project.file(output))
  }

  void schema(Object schema) {
    setOption("schema", schema)
  }

  void classpath(Object cp) {
    throw new InvalidUserDataException("Inline validation cannot have a different classpath")
  }

  void parallel(Boolean par) {
    if (par) {
      throw new InvalidUserDataException("Inline validation cannot run in parallel")
    }
  }

  void setOption(String name, Object value) {
    options[name] = value
  }

  void setPluginOption(String name, Object value) {
    pluginOptions[name] = value
  }

  Object getOption(String name) {
    return name in options ? options[name] :
      project.relaxng_validator.getOptions(pluginConfig)[name]
  }

  Object getPluginOption(String name) {
    return name in pluginOptions ? pluginOptions[name] :
      project.relaxng_validator.getPluginOptions(pluginConfig)[name]
  }

  void pluginConfiguration(String name) {
    if (name in project.relaxng_validator.configurations()) {
      pluginConfig = name
    } else {
      throw new InvalidUserDataException("Unknown RelaxNG plugin configuration: ${name}")
    }
  }

  void run() {
    Object handler = null
    Map<String,String> args = [:]
    project.relaxng_validator.getOptions(pluginConfig).each { name, value ->
      if (name == "errorHandler") {
        handler = value
      } else {
        args[name] = value.toString()
      }
    }

    this.options.each { name, value ->
      if (name == "errorHandler") {
        handler = value
      } else {
        args[name] = value.toString()
      }
    }

    if (getOption("input") != null) {
      if (handler == null) {
        handler = new ErrorHandlerImpl(System.out)
      }

      project.files(getOption("input")).each {
        RelaxNGValidateImpl impl = new RelaxNGValidateImpl(args, handler)
        impl.execute()
      }
    }

    options.clear()
    pluginOptions.clear()
  }
}

class RelaxNGValidatePlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.extensions.create("relaxng_validator", RelaxNGValidatePluginExtension)
    project.ext.relaxng_validate = { Closure cl ->
      StandaloneRelaxNGValidate validator = new StandaloneRelaxNGValidate(project)
      cl.delegate = validator
      cl()
      validator.run()
    }
  }
}
