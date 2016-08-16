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
package io.neba.core.web;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;

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
    public String getApplicationName() {
        return wrapped.getApplicationName();
    }

    @Override
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
    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return wrapped.getAutowireCapableBeanFactory();
    }

    @Override
    public Environment getEnvironment() {
        return wrapped.getEnvironment();
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return wrapped.containsBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return wrapped.getBeanDefinitionCount();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return wrapped.getBeanDefinitionNames();
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
        return wrapped.getBeanNamesForType(type);
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        return wrapped.getBeanNamesForType(type);
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        return wrapped.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        return wrapped.getBeansOfType(type);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
        return wrapped.getBeansOfType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        return wrapped.getBeanNamesForAnnotation(annotationType);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
        return wrapped.getBeansWithAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        return wrapped.findAnnotationOnBean(beanName, annotationType);
    }

    @Override
    public Object getBean(String name) throws BeansException {
        return wrapped.getBean(name);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return wrapped.getBean(name, requiredType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return wrapped.getBean(requiredType);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return wrapped.getBean(name, args);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        return wrapped.getBean(requiredType, args);
    }

    @Override
    public boolean containsBean(String name) {
        return wrapped.containsBean(name);
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return wrapped.isSingleton(name);
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        return wrapped.isPrototype(name);
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return wrapped.isTypeMatch(name, typeToMatch);
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return wrapped.isTypeMatch(name, typeToMatch);
    }

    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        return wrapped.getType(name);
    }

    @Override
    public String[] getAliases(String name) {
        return wrapped.getAliases(name);
    }

    @Override
    public BeanFactory getParentBeanFactory() {
        return wrapped.getParentBeanFactory();
    }

    @Override
    public boolean containsLocalBean(String name) {
        return wrapped.containsLocalBean(name);
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        return wrapped.getMessage(code, args, defaultMessage, locale);
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        return wrapped.getMessage(code, args, locale);
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        return wrapped.getMessage(resolvable, locale);
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        wrapped.publishEvent(event);
    }

    @Override
    public void publishEvent(Object event) {
        wrapped.publishEvent(event);
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        return wrapped.getResources(locationPattern);
    }

    @Override
    public Resource getResource(String location) {
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
