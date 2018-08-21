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
package io.neba.core.resourcemodels.factory;

import io.neba.api.annotations.Filter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceDependencyTest {
    @Mock
    private BundleContext context;

    private Filter filter;
    private Type serviceType;
    private Object expectedDependency;
    private Object resolvedDependency;

    private ServiceDependency testee;

    @Test
    public void testResolutionOfServiceInterface() {
        withDependencyTo(ServiceInterface.class);
        withExistingService(ServiceInterface.class);

        createDependency();
        resolveService();
        assertDependencyWasResolved();
    }

    @Test
    public void testResolutionOfOptionalServiceInterface() {
        withDependencyTo(parameterOf("setOptionalDependency"));
        withExistingService(ServiceInterface.class);

        createDependency();
        resolveService();
        assertDependencyWasResolvedToOptional();
    }

    @Test
    public void testResolutionOfDependencyWithFilter() throws InvalidSyntaxException {
        withDependencyTo(parameterOf("setDependencyWithFilter"));
        withFilter(filterOf("setDependencyWithFilter"));
        withExistingService(ServiceInterface.class, "(property=name)");

        createDependency();
        resolveService();
    }

    @Test
    public void testResolutionOfDependencyWithFilterWithMissingServices() throws InvalidSyntaxException {
        withDependencyTo(parameterOf("setDependencyWithFilter"));
        withFilter(filterOf("setDependencyWithFilter"));

        withExistingService(ServiceInterface.class, "(property=name)", 0);

        createDependency();
        resolveService();
        assertResolvedDependencyIsNull();
    }

    @Test
    public void testResolutionOfDependencyWithFilterFailsIfMoreThanOneServiceIsAvailable() throws InvalidSyntaxException {
        withDependencyTo(parameterOf("setDependencyWithFilter"));
        withFilter(filterOf("setDependencyWithFilter"));

        withExistingService(ServiceInterface.class, "(property=name)", 2);

        createDependency();
        try {
            resolveService();
        } catch (ModelInstantiationException e) {
            assertThat(e.getMessage()).startsWith(
                    "Unable to resolve the service dependency " +
                            "ServiceDependency{serviceType=class io.neba.core.resourcemodels.factory.ServiceDependencyTest$ServiceInterface, filter=@io.neba.api.annotations.Filter(value=(property=name))}, " +
                            "got more than one matching service instance:");
            return;
        }
        fail("There are more that two service instances available for injection, but only one is expected, thus instantiating the model must fail.");
    }

    @Test
    public void testResolutionOfListOfServiceInterfaceWithFilter() throws InvalidSyntaxException {
        withDependencyTo(parameterOf("setDependencyList"));
        withFilter(filterOf("setDependencyList"));

        withExistingService(ServiceInterface.class, "(property=name)");

        createDependency();
        resolveService();
        assertDependenciesAreProvidedInList();
    }

    @Test
    public void testResolutionOfListOfServiceInterface() throws InvalidSyntaxException {
        withDependencyTo(parameterOf("setDependencyList"));
        withExistingService(ServiceInterface.class, null);

        createDependency();
        resolveService();
        assertDependenciesAreProvidedInList();
    }

    @Test
    public void testResolutionOfNonexistentOptionalServiceInterface() {
        withDependencyTo(parameterOf("setOptionalDependency"));

        createDependency();
        resolveService();
        assertDependencyResolvedToEmptyOptional();
    }

    @Test
    public void testResolutionOfNonexistentListOfServiceInterface() {
        withDependencyTo(parameterOf("setDependencyList"));
        withFilter(filterOf("setDependencyList"));

        createDependency();
        resolveService();
        assertDependencyIsResolvedToEmptyList();
    }

    @Test(expected = InvalidModelException.class)
    public void testMissingTypeParameterInServiceInterface() {
        withDependencyTo(parameterOf("setOptionalDependencyWithMissingTypeParameter"));
        createDependency();
    }

    @Test
    public void testInvalidFilterSyntaxInFilterAnnotation() {
        try {
            withDependencyTo(parameterOf("setDependencyListWithInvalidFilter"));
            withFilter(filterOf("setDependencyListWithInvalidFilter"));
            createDependency();
        } catch (InvalidModelException e) {
            assertThat(e).hasMessage(
                    "The syntax of the filter " +
                            "'not valid' " +
                            "for the service dependency " +
                            "'java.util.List<io.neba.core.resourcemodels.factory.ServiceDependencyTest$ServiceInterface>' " +
                            "of the model " +
                            "'" + getClass() + "' " +
                            "is invalid");
            return;
        }
        fail("Since the model has an invalid @Filter setter argument annotation, creating instantiation metadata must fail.");
    }

    @SuppressWarnings("unchecked")
    private void assertDependencyIsResolvedToEmptyList() {
        assertThat(this.resolvedDependency).isInstanceOf(List.class);
        assertThat((List<?>) this.resolvedDependency).isEmpty();
    }

    private void assertResolvedDependencyIsNull() {
        assertThat(this.resolvedDependency).isNull();
    }

    @SuppressWarnings("unchecked")
    private void assertDependencyResolvedToEmptyOptional() {
        assertThat(this.resolvedDependency).isInstanceOf(Optional.class);
        assertThat((Optional) this.resolvedDependency).isEmpty();
    }

    private void withFilter(Filter f) {
        this.filter = f;
    }

    @SuppressWarnings("unchecked")
    private void assertDependenciesAreProvidedInList() {
        assertThat(this.resolvedDependency)
                .describedAs("The resolved service dependencies must be a non-null list")
                .isInstanceOf(List.class);
        assertThat((List) this.resolvedDependency)
                .describedAs("The list of service dependencies must contain the expected dependency")
                .containsExactly(this.expectedDependency);
    }

    @SuppressWarnings("unchecked")
    private void assertDependencyWasResolvedToOptional() {
        assertThat(this.resolvedDependency).isInstanceOf(Optional.class);
        assertThat((Optional) this.resolvedDependency).hasValue(this.expectedDependency);
    }

    private Type parameterOf(String methodName) {
        for (Method method : getClass().getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                return method.getGenericParameterTypes()[0];
            }
        }
        return null;
    }

    private Filter filterOf(String methodName) {
        for (Method method : getClass().getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                return (Filter) method.getParameterAnnotations()[0][0];
            }
        }
        return null;
    }

    private void assertDependencyWasResolved() {
        assertThat(this.resolvedDependency).isSameAs(this.expectedDependency);
    }

    private void withExistingService(Class<?> serviceInterface) {
        ServiceReference<?> reference = mock(ServiceReference.class);
        doReturn(reference).when(this.context).getServiceReference(serviceInterface);
        this.expectedDependency = mock(serviceInterface);
        doReturn(this.expectedDependency).when(this.context).getService(reference);
    }

    private void withExistingService(Class<?> serviceInterface, String filter) throws InvalidSyntaxException {
        withExistingService(serviceInterface, filter, 1);
    }

    private void withExistingService(Class<?> serviceInterface, String filter, int numberOfServices) throws InvalidSyntaxException {
        List<ServiceReference<?>> references = new ArrayList<>();
        for (int i = 0; i < numberOfServices; ++i) {
            references.add(mock(ServiceReference.class));
        }
        doReturn(references).when(this.context).getServiceReferences(serviceInterface, filter);
        references.forEach(r -> {
            doReturn(mock(serviceInterface)).when(this.context).getService(r);
        });

        this.expectedDependency = references.isEmpty() ? null : this.context.getService(references.get(0));
    }

    private void resolveService() {
        this.resolvedDependency = this.testee.resolve(this.context);
    }

    private void createDependency() {
        this.testee = new ServiceDependency(this.serviceType, getClass(), this.filter);
    }

    private void withDependencyTo(Type serviceType) {
        this.serviceType = serviceType;
    }

    private static class ServiceInterface {
    }

    @SuppressWarnings("unused")
    private void setOptionalDependency(Optional<ServiceInterface> optionalDependency) {
    }

    @SuppressWarnings("unused")
    private void setOptionalDependencyWithMissingTypeParameter(Optional<?> optionalDependency) {
    }

    @SuppressWarnings("unused")
    private void setDependencyList(@Filter("(property=name)") List<ServiceInterface> dependencies) {
    }

    @SuppressWarnings("unused")
    private void setDependencyListWithInvalidFilter(@Filter("not valid") List<ServiceInterface> dependencies) {
    }

    @SuppressWarnings("unused")
    private void setDependencyWithFilter(@Filter("(property=name)") ServiceInterface dependency) {
    }
}