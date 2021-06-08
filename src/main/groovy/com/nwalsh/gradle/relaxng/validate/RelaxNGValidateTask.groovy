package com.nwalsh.gradle.relaxng.validate

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.file.FileCollection
import org.gradle.api.InvalidUserDataException

import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

import com.thaiopensource.xml.sax.ErrorHandlerImpl

import javax.inject.Inject

class RelaxNGValidateTask extends DefaultTask implements RelaxNGValidatePluginOptions {
  protected static final String INPUT_OPTION = 'input'
  protected static final String OUTPUT_OPTION = 'output'
  protected static final String SCHEMA_OPTION = 'schema'
  protected static final String PARALLEL_OPTION = 'parallel'

  protected final List<String> defaultArguments = [].asImmutable()

  protected final Map<String, Object> options = [:]
  protected final Map<String, Object> pluginOptions = [:]

  protected String pluginConfig = RelaxNGValidatePluginConfigurations.DEFAULT

  private final WorkerExecutor workerExecutor

  @Inject
  RelaxNGValidateTask(WorkerExecutor workerExecutor) {
    super()
    this.workerExecutor = workerExecutor
  }

  // ============================================================

  void setOption(String name, Object value) {
    options[name] = value
  }

  void setPluginOption(String name, Object value) {
    pluginOptions[name] = value
  }

  Object getOption(String name) {
    return name in options ? options[name] :
      RelaxNGValidatePluginConfigurations.instance.getOptions(pluginConfig)[name]
  }

  Object getPluginOption(String name) {
    return name in pluginOptions ? pluginOptions[name] :
      RelaxNGValidatePluginConfigurations.instance.getPluginOptions(pluginConfig)[name]
  }

  // ============================================================

  void pluginConfiguration(String name) {
    if (RelaxNGValidatePluginConfigurations.instance.knownConfiguration(name)) {
      pluginConfig = name
    } else {
      throw new InvalidUserDataException("Unknown RelaxNG plugin configuration: ${name}")
    }
  }

  void input(Object input) {
    setOption(INPUT_OPTION, input)
  }

  void output(Object output) {
    setOption(OUTPUT_OPTION, project.file(output))
  }

  void schema(Object schema) {
    setOption(SCHEMA_OPTION, schema)
  }

  @InputFiles
  @SkipWhenEmpty
  FileCollection getInputFiles() {
    FileCollection files = project.files()
    if (getOption(INPUT_OPTION) != null) {
      files += project.files(getOption(INPUT_OPTION))
    }
    if (getOption(SCHEMA_OPTION) != null) {
      files += project.files(getOption(SCHEMA_OPTION))
    }
    return files
  }

  @Optional
  @OutputFiles
  FileCollection getOutputFiles() {
    if (getOption(OUTPUT_OPTION) != null) {
      project.files(getOption(OUTPUT_OPTION))
    }
  }

  @TaskAction
  void run() {
    Object handler = null
    Map<String,String> args = [:]
    RelaxNGValidatePluginConfigurations.instance.getOptions(pluginConfig).findAll { name, value ->
      if (name == "errorHandler") {
        handler = value
      } else {
        args[name] = value.toString()
      }
    }

    this.options.findAll { name, value ->
      if (name == "errorHandler") {
        handler = value
      } else {
        args[name] = value.toString()
      }
    }

    boolean parallel = false
    // If there's a handler, you can't have parallelism. The handler object
    // doesn't get to the WorkQueue because it can't be serialized. Or something.
    // https://discuss.gradle.org/t/is-it-possible-to-pass-a-value-out-of-an-extension-task/40143
    if (handler == null && getPluginOption(PARALLEL_OPTION) != null) {
      parallel = getPluginOption(PARALLEL_OPTION)
    }

    if (getOption(INPUT_OPTION) != null) {
      WorkQueue workQueue = null;
      if (parallel) {
        workQueue = workerExecutor.classLoaderIsolation() {
          if (getPluginOption('classpath') != null) {
            it.getClasspath().from(getPluginOption('classpath'))
          }
        }
      } else {
        if (handler == null) {
          handler = new ErrorHandlerImpl(System.out)
        }
      }

      project.files(getOption(INPUT_OPTION)).each {
        if (parallel) {
          workQueue.submit(RelaxNGValidate) {
            it.arguments.set(args)
          }
        } else {
          RelaxNGValidateImpl impl = new RelaxNGValidateImpl(args, handler)
          impl.execute()
        }
      }
    }
  }
}
