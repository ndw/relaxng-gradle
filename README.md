# relaxng-gradle

Gradle plugins to perform RELAX NG validation and translation
(wrappers for Jing and Trang, basically).

This repository contains a set of two Gradle tasks bundled in a single
plugin.

To use either of the plugins, you must load them in your
`build.gradle` file:

```
plugins {
  id 'com.nwalsh.gradle.relaxng.validate' version '0.0.8'
  id 'com.nwalsh.gradle.relaxng.translate' version '0.0.8'
}
```

The plugins use the latest 3.0.1beta3 release of
[XML Resolver](https://github.com/xmlresolver/xmlresolver). You’ll need
to make sure that you force resolution to use that version, if you have
other, transitive dependencies on older versions.

```
configurations.all {
  resolutionStrategy {
    force 'org.xmlresolver:xmlresolver:3.0.1beta3'
  }
}
```

You’ll need to include the latest resolver in your dependencies and
some logging framework, for example:

```
dependencies {
  implementation (
    [group: 'org.xmlresolver', name: 'xmlresolver', version: '3.0.1beta3'],
    [group: 'org.slf4j', name: 'slf4j-api', version: '1.7.30' ],
    [group: 'org.slf4j', name: 'slf4j-simple', version: '1.7.30' ]
  )
}
```

Finally, import the task(s). Import `RelaxNgValidateTask` to perform
validation, `RelaxNgTranslateTask` to perform translation.

```
import com.nwalsh.gradle.relaxng.validate.RelaxNGValidateTask
import com.nwalsh.gradle.relaxng.translate.RelaxNGTranslateTask
```

## Global configuration

**NOTE:** The name of the global configuration objects changed in version 0.0.8.
They used to be `relaxng_validate` and `relaxng_translate`, they’re now
`relaxng_valdator` and `relaxng_translator`. That makes a little more sense
grammatically, but more importantly, it means I can use the former names for
standalone tasks.

If you want the same options to apply to all (or many) tasks, you can create
global configurations:

```
relaxng_validator.configure {
  // global configuration options for validation
}

relaxng_translator.configure {
  // global configuration options for translation
}
```

If you have multiple sets of tasks with similar options, you can create
named configurations as well:

```
relaxng_validator.configure("compact") {
    compact true
}
```

The `pluginConfiguration` property allows you to refer to that whole, named
group of options:

```
task schemaValidate(type: RelaxNgValidationTask) {
    pluginConfiguration("compact")
    // other properties here
}
```

The configurations form a hierarchy, local values override named configuration values
which, in turn, override the global (unnamed) configuration. Given:

```
relaxng_validator.configure {
    debug true
}

relaxng_validator.configure("test") {
    debug false
}

task firstCase(type: RelaxNgValidationTask) {
    // debug is true
}

task secondCase(type: RelaxNgValidationTask) {
    pluginConfiguration("test")
    // debug is false
}

task secondCase(type: RelaxNgValidationTask) {
    pluginConfiguration("test")
    debug true
    // debug is true
}
```

## Standalone tasks

Sometimes it’s convenient to just inline a few processes in a single task.
The plugin provides global `relaxng_validate` and `relaxng_translate` functions
for this purpose:

```
task translateAndValidate() {
    doLast {
        println("Translate")
    }

    doLast {
        relaxng_translate {
            input "src/schema.dtd"
            output "build/schema.rng"
        }
    }

    doLast {
        println("Validate")
    }

    doLast {
        relaxng_validate {
            input "src/document.xml"
            schema "build/schema.rng"
            output "build/valid/document.xml"
        }
    }
}
```

## Validation

The validation properties are listed below. Most of them correspond to
the *jing* command line options.

* `input` (File, required) The file to validate.
* `schema` (File, required) The schema to use for validation.
* `output` (File) The location where the validated document should be written.
* `assertValid` (Boolean) If true (the default), schema validation errors cause the task to fail.
* `catalog` (String) A catalog file to use during parsing. You can repeat this option.
* `classpath` (String) Specify a classpath (if you have a custom datatype library, for example).
* `compact` (Boolean) Use the RELAX NG compact syntax parser explicitly.
* `debug` (Boolean) Print debugging information about the validation configuration.
* `encoding` (String) The encoding to use when reading the (compact) schema.
* `feasible` (Boolean) Test if the document is feasibly valid.
* `idref` (Boolean) Perform ID/IDREF validation.
* `parallel` (Boolean) Run multiple validation jobs in parallel.

A note about the `output` property: if you specify an output location, what appears there
is an exact copy of the input document. RELAX NG validation doesn’t augment the instance
document. This option is only provided because it makes the up-to-date checking features
of Gradle more convenient.

## Translation

The translation properties are listed below. Most of them correspond to the
*trang* command line options. Many apply only when converting from a DTD.
See the *trang* documentation for more details.

* `input` (File, required) The input file to translate.
* `output` (File, required) The location where the translated schema should be written.
* `annotationPrefix` (String) The prefix to use for DTD compatibility annotations. Defaults
  to `a` unless that conflicts with another namespace.
* `anyName` (String) The of the definition generated for the content of elements in a
  DTD that have a content model of `ANY`.
* `attlistDefine` (String) Specifies how to construct the definitions representing
  attribute lists.
* `colonReplacement` (String) A list of characters to substitute for colons if a
  DTD contains element names with colons. (Colons are not allowed in RELAX-NG pattern names.)
* `debug` (Boolean) Print debugging information about the translation configuration
* `elementDefine` (String) Specifies how to construct the definitions representing
  elements.
* `encoding` (String) The input (and output) encoding, defaults to “unspecified”.
* `generateStart` (Boolean) If true, a `start` element will be generated.
* `indent` (Integer) Output line indent distance.
* `inlineAttlist` (Boolean) If true, attribute definitions will be inlined into element
  definitions instead of creating a separate pattern for the attribute lists.
* `inputEncoding` (String) The input encoding, defaults to `encoding`.
* `inputType` (String) The input schema type, taken from the file extension by default.
* `lineLength` (Integer) Maximum output line length.
* `namespace` (String) Namespace bindings.
* `outputEncoding` (String) The output encoding, defaults to `encoding`.
* `outputType` (String) The output schema type, taken from the file extension by default.
* `strictAny` (Boolean) If true, will preserve the exact content models of `ANY` elements,
  instead of using wildcards.
* `parallel` (Boolean) Run multiple validation jobs in parallel.

The `namespace` property may be repeated. Each namespace should be encoded like an XML
namespace binding, for example:

```
  namespace("xmlns=http://docbook.org/ns/docbook")
  namespace("xmlns:xlink=http://www.w3.org/1999/xlink")
```

Where *trang* has several pairs of options of the form `--something`
and `--no-something`, the plugin uses a boolean option with the name
`something`. A value of true corresponds to the former, false to the
latter.

## Parallelism

The plugins can use the Gradle work queue to run in parallel. This
seems to work fine for small-to-medium size builds. For very large
builds (hundreds of validation or translation tasks), it sometimes
generates spurious I/O errors.

I can’t work out why and I have some suspicion that the problem may
reside in the implementation of work queues, not these plugins. For
the meantime, I’ve added a `parallel` option. If you set it to true,
the jobs will run in parallel, otherwise they won’t. By default, jobs
are run sequentially, not in parallel.

## Examples

You’ll find complete examples in the `examples` directory.
