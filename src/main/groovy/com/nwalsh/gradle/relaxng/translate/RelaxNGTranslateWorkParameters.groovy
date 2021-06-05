package com.nwalsh.gradle.relaxng.translate

import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkParameters

interface RelaxNGTranslateWorkParameters extends WorkParameters {
    ListProperty<String> getArguments()
}
