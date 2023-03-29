# relaxng-gradle

Gradle plugins to perform RELAX NG validation and translation
(wrappers for Jing and Trang, basically).

This repository contains a set of two Gradle tasks bundled in a single
plugin.

To use either of the plugins, you must load them in your
`build.gradle` file:

```gradle
plugins {
  id 'com.nwalsh.gradle.relaxng.validate' version '0.10.0'
  id 'com.nwalsh.gradle.relaxng.translate' version '0.10.0'
}
```

The plugins require version 3.0.1 or later of the
[XML Resolver](https://github.com/xmlresolver/xmlresolver); version 5.1.1
is the latest at the time of this writing. You’ll need
to make sure that you force resolution to use that version, if you have
other, transitive dependencies on older versions.

```gradle
configurations.all {
  resolutionStrategy {
    force 'org.xmlresolver:xmlresolver:5.1.1'
  }
}
```

You’ll need to include the latest resolver in your dependencies and
some logging framework, for example:

```gradle
dependencies {
  implementation (
    [group: 'org.xmlresolver', name: 'xmlresolver', version: '5.1.1'],
    [group: 'org.slf4j', name: 'slf4j-api', version: '1.7.30' ],
    [group: 'org.slf4j', name: 'slf4j-simple', version: '1.7.30' ]
  )
}
```

Finally, import the task(s). Import `RelaxNgValidateTask` to perform
validation, `RelaxNgTranslateTask` to perform translation.

```gradle
import com.nwalsh.gradle.relaxng.validate.RelaxNGValidateTask
import com.nwalsh.gradle.relaxng.translate.RelaxNGTranslateTask
```

## Configuration

Earlier versions of these plugins attempted to provide a mechanism for
global configuration. That approach wasn’t good Gradle practice so
it’s been removed.

Both of the tasks now have an `options` method that accepts a map. You
can use global declaratiosn to share configuration information. For
example:

```gradle
def commonOpts = [
    'debug': true,
    'catalog': 'catalog.xml;src/catalog.xml'
]
…
task someTask(type: RelaxNGValidateTask) {
    input "${projectDir}/src/doc.xml"
    schema "${projectDir}/src/schema.rng"
    options(commonOpts)
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

## Examples

You’ll find complete examples in the `examples` directory.

## Change log

### Version 0.10.0

This is a significant refactor from the previous release. All of the
attempts to provide global configuration have been removed (they
didn’t play nice with Gradle best practices). Instead, there’s a
simple `options()` option that allows you to pass in a map of options.
You can use variables in the build script to share configurations
between tasks.

The attempts at parallelism have also been removed. I’m not convinced
they ever actually offered any functionality given that the tasks
don’t accept multiple inputs.
