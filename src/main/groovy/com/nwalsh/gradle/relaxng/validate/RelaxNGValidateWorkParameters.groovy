package com.nwalsh.gradle.relaxng.validate

import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkParameters

interface RelaxNGValidateWorkParameters extends WorkParameters {
    ListProperty<String> getArguments()
}
