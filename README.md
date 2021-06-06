# relaxng-gradle

Gradle plugins to perform RELAX NG validation and translation
(wrappers for Jing and Trang, basically).

This repository contains a set of two Gradle tasks bundled in a single
plugin.

To use either of the plugins, you must load them in your
`build.gradle` file:

```
plugins {
  id 'com.nwalsh.gradle.relaxng.validate' version '0.0.3'
  id 'com.nwalsh.gradle.relaxng.translate' version '0.0.3'
}
```

The plugins use the latest 3.0.0 release of
[XML Resolver](https://github.com/xmlresolver/xmlresolver). You’ll need
to make sure that you force resolution to use that version, if you have
other, transitive dependencies on older versions.

```
configurations.all {
  resolutionStrategy {
    force 'org.xmlresolver:xmlresolver:3.0.0'
  }
}
```

You’ll need to include the latest resolver in your dependencies and
some logging framework, for example:

```
dependencies {
  implementation (
    [group: 'org.xmlresolver', name: 'xmlresolver', version: '3.0.0'],
    [group: 'org.slf4j', name: 'slf4j-api', version: '1.7.25' ],
    [group: 'org.apache.logging.log4j', name: 'log4j-slf4j-impl', version: '2.1'],
    [group: 'org.apache.logging.log4j', name: 'log4j-core', version: '2.1']
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

If you want the same options to apply to all (or many) tasks, you can create
global configurations:

```
relaxng_validate.configure {
  // global configuration options for validation
}

relaxng_translate.configure {
  // global configuration options for translation
}
```

If you have multiple sets of tasks with similar options, you can create
named configurations as well:

```
relaxng_validate.configure("compact") {
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

## Validation

The validation properties are:

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

A note about the `output` property. If you specify an output location, what appears there
is an exact copy of the input document. RELAX NG validation doesn’t augment the instance
document. This option is only provided because it makes the up-to-date checking features
of Gradle more convenient.

## Translation

The translation properties are:

* `input` (File, required) The input file to translate.
* `output` (File, required) The location where the translated schema should be written.
* `annotationPrefix` () FIXME: describe.
* `anyName` () FIXME: describe.
* `attlistDefine` () FIXME: describe.
* `colonReplacement` () FIXME: describe.
* `debug` (Boolean) Print debugging information about the translation configuration
* `elementDefine` () FIXME: describe.
* `encoding` (String) The input (and output) encoding, defaults to “unspecified”.
* `generateStart (Boolean) FIXME: describe.
* `indent` (Integer) Output line indent distance.
* `inlineAttlist (Boolean) FIXME: describe.
* `inputEncoding` (String) The input encoding, defaults to `encoding`.
* `inputType` (String) The input schema type, taken from the file extension by default.
* `lineLength` (Integer) Maximum output line length.
* `namespace` (String) Namespace bindings.
* `outputEncoding` (String) The output encoding, defaults to `encoding`.
* `outputType` (String) The output schema type, taken from the file extension by default.
* `strictAny` () FIXME: describe.

The `namespace` property may be repeated. Each namespace should be encoded like an XML
namespace binding, for example:

```
  namespace("xmlns=http://docbook.org/ns/docbook")
  namespace("xmlns:xlink=http://www.w3.org/1999/xlink")
```

## Examples

You’ll find complete examples in the `examples` directory.
