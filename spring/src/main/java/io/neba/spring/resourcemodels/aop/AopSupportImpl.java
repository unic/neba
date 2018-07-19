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
