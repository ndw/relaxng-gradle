package com.nwalsh.gradle.relaxng.validate

import org.gradle.api.provider.MapProperty
import org.gradle.workers.WorkParameters

interface RelaxNGValidateWorkParameters extends WorkParameters {
  MapProperty<String,String> getArguments()
}
