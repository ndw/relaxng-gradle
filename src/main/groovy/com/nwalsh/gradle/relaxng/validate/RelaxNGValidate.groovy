package com.nwalsh.gradle.relaxng.validate

import org.gradle.workers.WorkAction
import com.thaiopensource.util.UriOrFile
import com.thaiopensource.resolver.catalog.CatalogResolver
import com.thaiopensource.relaxng.util.Driver
import com.thaiopensource.xml.sax.ErrorHandlerImpl
import com.thaiopensource.util.PropertyMapBuilder
import com.thaiopensource.util.PropertyMap
import com.thaiopensource.validate.SchemaReader
import com.thaiopensource.validate.ValidateProperty
import com.thaiopensource.validate.prop.rng.RngProperty
import com.thaiopensource.validate.rng.CompactSchemaReader
import com.thaiopensource.validate.ValidationDriver
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.nwalsh.gradle.relaxng.util.JingResolver

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class RelaxNGValidate implements WorkAction<RelaxNGValidateWorkParameters> {
  @Override
  void execute() {
    RelaxNGValidateImpl impl = new RelaxNGValidateImpl(parameters.arguments.get())
    impl.execute()
  }
}
