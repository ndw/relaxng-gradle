package com.nwalsh.gradle.relaxng.translate

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.InvalidUserDataException

import com.thaiopensource.xml.sax.ErrorHandlerImpl

abstract class RelaxNGTranslatePluginExtension {
  public static final String DEFAULT = "com.nwalsh.gradle.relaxng-translate.DEFAULT"
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

class StandaloneRelaxNGTranslate implements RelaxNGTranslatePluginOptions {
  private final Project project
  protected String pluginConfig = RelaxNGTranslatePluginExtension.DEFAULT
  protected final Map<String, Object> options = [:]
  protected final Map<String, Object> pluginOptions = [:]
  protected final List<String> inputFiles = new ArrayList<>()

  StandaloneRelaxNGTranslate(Project project) {
    this.project = project
  }

  void input(Object input) {
    inputFiles.add(input.toString())
    setOption("input", inputFiles)
  }

  void output(Object output) {
    setOption("output", project.file(output).toString())
  }

  void classpath(Object cp) {
    throw new InvalidUserDataException("Inline translation cannot have a different classpath")
  }

  void parallel(Boolean par) {
    if (par) {
      throw new InvalidUserDataException("Inline translation cannot run in parallel")
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
      project.relaxng_translator.getOptions(pluginConfig)[name]
  }

  Object getPluginOption(String name) {
    return name in pluginOptions ? pluginOptions[name] :
      project.relaxng_translator.getPluginOptions(pluginConfig)[name]
  }

  void pluginConfiguration(String name) {
    if (name in project.relaxng_translator.configurations()) {
      pluginConfig = name
    } else {
      throw new InvalidUserDataException("Unknown RelaxNG plugin configuration: ${name}")
    }
  }

  void run() {
    Object handler = null
    Map<String,Object> args = [:]

    args["input"] = new ArrayList<String>()
    args["namespace"] = new ArrayList<String>()

    project.relaxng_translator.getOptions(pluginConfig).each { name, value ->
      if (name == "errorHandler") {
        handler = value
      } else {
        args[name] = value
      }
    }

    this.options.each { name, value ->
      if (name == "errorHandler") {
        handler = value
      } else {
        args[name] = value
      }
    }

    if (getOption("input") != null) {
      if (handler == null) {
        handler = new ErrorHandlerImpl(System.out)
      }

      project.files(getOption("input")).each {
        RelaxNGTranslateImpl impl = new RelaxNGTranslateImpl(args, handler)
        impl.execute()
      }
    }

    options.clear()
    pluginOptions.clear()
  }
}

class RelaxNGTranslatePlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.extensions.create("relaxng_translator", RelaxNGTranslatePluginExtension)
    project.ext.relaxng_translate = { Closure cl ->
      StandaloneRelaxNGTranslate translator = new StandaloneRelaxNGTranslate(project)
      cl.delegate = translator
      cl()
      translator.run()
    }
  }
}
