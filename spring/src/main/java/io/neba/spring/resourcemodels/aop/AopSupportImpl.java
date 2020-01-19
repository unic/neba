/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package io.neba.spring.resourcemodels.aop;

import io.neba.api.spi.AopSupport;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service("aopSupport")
public class AopSupportImpl implements AopSupport {
    @Nonnull
    @Override
    public Object prepareForFieldInjection(@Nonnull Object model) {
        if (!(model instanceof Advised)) {
            return model;
        }
        Advised advised = (Advised) model;

        TargetSource targetSource = advised.getTargetSource();
        if (targetSource == null) {
            throw new IllegalStateException("Model " + advised + " is " + Advised.class.getName() + ", but its target source is null.");
        }
        Object target;
        try {
            target = targetSource.getTarget();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to obtain the target of the advised model " + advised + ".", e);
        }
        if (target == null) {
            throw new IllegalStateException("The advised target of bean " + advised + " must not be null.");
        }
        return target;
    }
}
