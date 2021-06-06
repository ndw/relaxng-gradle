package com.nwalsh.gradle.relaxng.translate

import org.gradle.workers.WorkAction

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class RelaxNGTranslate implements WorkAction<RelaxNGTranslateWorkParameters> {
  @Override
  void execute() {
    RelaxNGTranslateImpl impl = new RelaxNGTranslateImpl(parameters.arguments.get())
    impl.execute()
  }
}
