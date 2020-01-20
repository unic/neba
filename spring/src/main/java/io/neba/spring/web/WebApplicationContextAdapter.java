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
package io.neba.spring.web;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;

/**
 * @author Olaf Otto
 */
public class WebApplicationContextAdapter implements WebApplicationContext {
    private final ApplicationContext wrapped;
    private final ServletContext servletContext;

    public WebApplicationContextAdapter(ApplicationContext wrapped, ServletContext servletContext) {
        this.wrapped = wrapped;
        this.servletContext = servletContext;
    }

    @Override
    public String getId() {
        return wrapped.getId();
    }

    @Override
    @Nonnull
    public String getApplicationName() {
        return wrapped.getApplicationName();
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return wrapped.getDisplayName();
    }

    @Override
    public long getStartupDate() {
        return wrapped.getStartupDate();
    }

    @Override
    public ApplicationContext getParent() {
        return wrapped.getParent();
    }

    @Override
    @Nonnull
    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return wrapped.getAutowireCapableBeanFactory();
    }

    @Override
    @Nonnull
    public Environment getEnvironment() {
        return wrapped.getEnvironment();
    }

    @Override
    public boolean containsBeanDefinition(@Nonnull String beanName) {
        return wrapped.containsBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return wrapped.getBeanDefinitionCount();
    }

    @Override
    @Nonnull
    public String[] getBeanDefinitionNames() {
        return wrapped.getBeanDefinitionNames();
    }

    @Override
    @Nonnull
    public String[] getBeanNamesForType(@Nonnull ResolvableType type) {
        return wrapped.getBeanNamesForType(type);
    }

    @Override
    @Nonnull
    public String[] getBeanNamesForType(@Nonnull ResolvableType resolvableType, boolean includeNonSingletons, boolean allowEagerInit) {
        return wrapped.getBeanNamesForType(resolvableType, includeNonSingletons, allowEagerInit);
    }

    @Override
    @Nonnull
    public String[] getBeanNamesForType(Class<?> type) {
        return wrapped.getBeanNamesForType(type);
    }

    @Override
    @Nonnull
    public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        return wrapped.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    @Nonnull
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        return wrapped.getBeansOfType(type);
    }

    @Override
    @Nonnull
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
        return wrapped.getBeansOfType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    @Nonnull
    public String[] getBeanNamesForAnnotation(@Nonnull Class<? extends Annotation> annotationType) {
        return wrapped.getBeanNamesForAnnotation(annotationType);
    }

    @Override
    @Nonnull
    public Map<String, Object> getBeansWithAnnotation(@Nonnull Class<? extends Annotation> annotationType) throws BeansException {
        return wrapped.getBeansWithAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(@Nonnull String beanName, @Nonnull Class<A> annotationType) throws NoSuchBeanDefinitionException {
        return wrapped.findAnnotationOnBean(beanName, annotationType);
    }

    @Override
    @Nonnull
    public Object getBean(@Nonnull String name) throws BeansException {
        return wrapped.getBean(name);
    }

    @Override
    @Nonnull
    public <T> T getBean(@Nonnull String name, @Nonnull Class<T> requiredType) throws BeansException {
        return wrapped.getBean(name, requiredType);
    }

    @Override
    @Nonnull
    public <T> T getBean(@Nonnull Class<T> requiredType) throws BeansException {
        return wrapped.getBean(requiredType);
    }

    @Override
    @Nonnull
    public Object getBean(@Nonnull String name, @Nonnull Object... args) throws BeansException {
        return wrapped.getBean(name, args);
    }

    @Override
    @Nonnull
    public <T> T getBean(@Nonnull Class<T> requiredType, @Nonnull Object... args) throws BeansException {
        return wrapped.getBean(requiredType, args);
    }

    @Override
    @Nonnull
    public <T> ObjectProvider<T> getBeanProvider(@Nonnull Class<T> aClass) {
        return wrapped.getBeanProvider(aClass);
    }

    @Override
    @Nonnull
    public <T> ObjectProvider<T> getBeanProvider(@Nonnull ResolvableType resolvableType) {
        return wrapped.getBeanProvider(resolvableType);
    }

    @Override
    public boolean containsBean(@Nonnull String name) {
        return wrapped.containsBean(name);
    }

    @Override
    public boolean isSingleton(@Nonnull String name) throws NoSuchBeanDefinitionException {
        return wrapped.isSingleton(name);
    }

    @Override
    public boolean isPrototype(@Nonnull String name) throws NoSuchBeanDefinitionException {
        return wrapped.isPrototype(name);
    }

    @Override
    public boolean isTypeMatch(@Nonnull String name, @Nonnull ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return wrapped.isTypeMatch(name, typeToMatch);
    }

    @Override
    public boolean isTypeMatch(@Nonnull String name, @Nonnull Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return wrapped.isTypeMatch(name, typeToMatch);
    }

    @Override
    public Class<?> getType(@Nonnull String name) throws NoSuchBeanDefinitionException {
        return wrapped.getType(name);
    }

    @Override
    public Class<?> getType(@Nonnull String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
        return wrapped.getType(name, allowFactoryBeanInit);
    }

    @Override
    @Nonnull
    public String[] getAliases(@Nonnull String name) {
        return wrapped.getAliases(name);
    }

    @Override
    public BeanFactory getParentBeanFactory() {
        return wrapped.getParentBeanFactory();
    }

    @Override
    public boolean containsLocalBean(@Nonnull String name) {
        return wrapped.containsLocalBean(name);
    }

    @Override
    public String getMessage(@Nonnull String code, Object[] args, String defaultMessage, @Nonnull Locale locale) {
        return wrapped.getMessage(code, args, defaultMessage, locale);
    }

    @Override
    @Nonnull
    public String getMessage(@Nonnull String code, Object[] args, @Nonnull Locale locale) throws NoSuchMessageException {
        return wrapped.getMessage(code, args, locale);
    }

    @Override
    @Nonnull
    public String getMessage(@Nonnull MessageSourceResolvable resolvable, @Nonnull Locale locale) throws NoSuchMessageException {
        return wrapped.getMessage(resolvable, locale);
    }

    @Override
    public void publishEvent(@Nonnull ApplicationEvent event) {
        wrapped.publishEvent(event);
    }

    @Override
    public void publishEvent(@Nonnull Object event) {
        wrapped.publishEvent(event);
    }

    @Override
    @Nonnull
    public Resource[] getResources(@Nonnull String locationPattern) throws IOException {
        return wrapped.getResources(locationPattern);
    }

    @Override
    @Nonnull
    public Resource getResource(@Nonnull String location) {
        return wrapped.getResource(location);
    }

    @Override
    public ClassLoader getClassLoader() {
        return wrapped.getClassLoader();
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }
}
