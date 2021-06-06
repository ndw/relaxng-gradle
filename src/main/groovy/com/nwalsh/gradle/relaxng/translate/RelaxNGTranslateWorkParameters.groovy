package com.nwalsh.gradle.relaxng.translate

import org.gradle.api.provider.MapProperty
import org.gradle.workers.WorkParameters

interface RelaxNGTranslateWorkParameters extends WorkParameters {
    MapProperty<String,Object> getArguments()
}
