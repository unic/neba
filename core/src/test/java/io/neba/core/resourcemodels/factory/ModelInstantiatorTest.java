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

import javax.inject.Inject;
import java.lang.annotation.Retention;
import java.util.List;
import java.util.Optional;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ModelInstantiatorTest {
    @Mock
    private BundleContext context;

    private TestModel modelInstance;

    private ModelInstantiator<? extends TestModel> testee;

    @Test
    public void testModelIsInstantiatedViaDefaultConstructorIfNoInjectConstructorIsPresent() throws ReflectiveOperationException {
        withMetadataFor(TestModel.class);
        createModelInstance();
        assertModelIsInstantiatedUsing("TestModel()");
    }

    @Test
    public void testModelIsInstantiatedViaInjectConstructorIfInjectConstructorIsPresent() throws ReflectiveOperationException {
        ServiceInterface s1 = withOsgiService(ServiceInterface.class);
        OtherServiceInterface s2 = withOsgiService(OtherServiceInterface.class);
        withMetadataFor(TestModelWithInjectConstructor.class);

        createModelInstance();

        assertModelIsInstantiatedUsing("TestModelWithInjectConstructor(ServiceInterface s1, OtherServiceInterface s2)");

        assertModelHasProperty("s1", s1);
        assertModelHasProperty("s2", s2);
    }

    @Test
    public void testModelInstantiationFailsIfARequiredServiceDependencyForConstructorInjectionIsMissing() throws ReflectiveOperationException {
        withOsgiService(ServiceInterface.class);
        withMetadataFor(TestModelWithInjectConstructor.class);
        try {
            createModelInstance();
        } catch (ModelInstantiationException e) {
            assertThat(e).hasMessage(
                    "Unable to instantiate the model using " +
                            "'public io.neba.core.resourcemodels.factory.ModelInstantiatorTest$TestModelWithInjectConstructor(io.neba.core.resourcemodels.factory.ModelInstantiatorTest$ServiceInterface,io.neba.core.resourcemodels.factory.ModelInstantiatorTest$OtherServiceInterface)'. " +
                            "The Service dependency 'ServiceDependency{serviceType=interface io.neba.core.resourcemodels.factory.ModelInstantiatorTest$OtherServiceInterface, filter=null}' resolved to null.");
            return;
        }

        fail("Since the service dependency " + OtherServiceInterface.class + " is missing, instantiating the model must fail.");
    }

    @Test
    public void testModelWithoutPublicConstructorsLeadsToInvalidModelException() {
        try {
            withMetadataFor(TestModelWithoutPublicConstructor.class);
        } catch (InvalidModelException e) {
            assertThat(e).hasMessage("The model class io.neba.core.resourcemodels.factory.ModelInstantiatorTest$TestModelWithoutPublicConstructor " +
                    "has neither a public default constructor nor a public constructor annotated with @Inject.");
            return;
        }
        fail("Since the model has no public constructor, creating instantiation meta    data must fail.");
    }

    @Test
    public void testModelWithMultipleAtInjectConstructorsLeadsToInvalidModelException() {
        try {
            withMetadataFor(TestModelWithMultipleInjectConstructors.class);
        } catch (InvalidModelException e) {
            assertThat(e).hasMessage(
                    "Unable to instantiate model class io.neba.core.resourcemodels.factory.ModelInstantiatorTest$TestModelWithMultipleInjectConstructors. " +
                            "Found more than one constructor annotated with @Inject: " +
                            "public io.neba.core.resourcemodels.factory.ModelInstantiatorTest$TestModelWithMultipleInjectConstructors(io.neba.core.resourcemodels.factory.ModelInstantiatorTest$ServiceInterface), " +
                            "public io.neba.core.resourcemodels.factory.ModelInstantiatorTest$TestModelWithMultipleInjectConstructors(io.neba.core.resourcemodels.factory.ModelInstantiatorTest$OtherServiceInterface)");
            return;
        }
        fail("Since the model has multiple @Inject constructors, creating instantiation metadata must fail.");
    }

    @Test
    public void testResolutionOfServiceDependency() throws ReflectiveOperationException {
        ServiceInterface serviceInstance = withOsgiService(ServiceInterface.class);
        withMetadataFor(TestModelWithSetterDependency.class);
        createModelInstance();
        assertModelHasProperty("serviceInterface", serviceInstance);
    }

    @Test
    public void testResolutionOfExistingOptionalSetterDependency() throws ReflectiveOperationException {
        ServiceInterface serviceInstance = withOsgiService(ServiceInterface.class);
        withMetadataFor(TestModelWithOptionalSetterDependency.class);
        createModelInstance();
        assertModelHasProperty("serviceInterface", Optional.of(serviceInstance));
    }

    @Test
    public void testResolutionOfMissingOptionalSetterDependency() throws ReflectiveOperationException {
        withMetadataFor(TestModelWithOptionalSetterDependency.class);
        createModelInstance();
        assertModelHasProperty("serviceInterface", Optional.empty());
    }

    @Test
    public void testResolutionOfExistingOptionalConstructorDependency() throws ReflectiveOperationException {
        ServiceInterface serviceInstance = withOsgiService(ServiceInterface.class);
        withMetadataFor(TestModelWithOptionalConstructorDependency.class);
        createModelInstance();
        assertModelHasProperty("serviceInterface", Optional.of(serviceInstance));
    }

    @Test
    public void testResolutionOfMissingOptionalConstructorDependency() throws ReflectiveOperationException {
        withMetadataFor(TestModelWithOptionalConstructorDependency.class);
        createModelInstance();
        assertModelHasProperty("serviceInterface", Optional.empty());
    }

    @Test
    public void testResolutionOfFilteredSetterDependency() throws Exception {
        ServiceInterface serviceInstance = withOsgiService(ServiceInterface.class, "(property=1)");
        withMetadataFor(TestModelWithFilteredSetterDependency.class);
        createModelInstance();
        assertModelHasProperty("serviceInterface", serviceInstance);
    }

    @Test
    public void testResolutionOfFilteredConstructorDependency() throws Exception {
        ServiceInterface serviceInstance = withOsgiService(ServiceInterface.class, "(property=1)");
        withMetadataFor(TestModelWithFilteredConstructorDependency.class);
        createModelInstance();
        assertModelHasProperty("serviceInterface", serviceInstance);
    }

    @Test
    public void testResolutionOfFilteredSetterDependencyUsingMetaAnnotation() throws Exception {
        ServiceInterface serviceInstance = withOsgiService(ServiceInterface.class, "(property=1)");
        withMetadataFor(TestModelWithFilteredSetterDependencyViaMetaAnnotation.class);
        createModelInstance();
        assertModelHasProperty("serviceInterface", serviceInstance);
    }

    @Test
    public void testResolutionOfFilteredConstructorDependencyUsingMetaAnnotation() throws Exception {
        ServiceInterface serviceInstance = withOsgiService(ServiceInterface.class, "(property=1)");
        withMetadataFor(TestModelWithFilteredConstructorDependencyViaMetaAnnotation.class);
        createModelInstance();
        assertModelHasProperty("serviceInterface", serviceInstance);
    }

    @Test
    public void testDependenciesWithFilterSupportsListType() throws Exception {
        ServiceInterface serviceInstance = withOsgiService(ServiceInterface.class, "(property=1)");
        withMetadataFor(TestModelWithFilteredListOfDependencies.class);
        createModelInstance();
        assertModelWasInjectedWithListOf(serviceInstance);
    }

    @Test
    public void testDependenciesWithFilterAndListTypeYieldsEmptyListIfNoServiceInstancesExist() throws Exception {
        withMetadataFor(TestModelWithFilteredListOfDependencies.class);
        createModelInstance();
        assertModelWasInjectedWithListOf();
    }

    @Test
    public void testModelWithSetterWithMultipleArgumentsLeadsToInvalidModelException() {
        try {
            withMetadataFor(TestModelWithInvalidSetter.class);
        } catch (InvalidModelException e) {
            assertThat(e).hasMessage(
                    "The method public void io.neba.core.resourcemodels.factory.ModelInstantiatorTest$TestModelWithInvalidSetter.setServiceInterface(io.neba.core.resourcemodels.factory.ModelInstantiatorTest$ServiceInterface,io.neba.core.resourcemodels.factory.ModelInstantiatorTest$OtherServiceInterface) " +
                            "is annotated with @Inject and must thus take exactly one argument.");
            return;
        }
        fail("Since the model has an @Inject setter with multiple arguments, creating instantiation metadata must fail.");
    }

    @Test
    public void testModelWithInvalidFilterLeadsToInvalidModelException() {
        try {
            withMetadataFor(TestModelWithInvalidFilterDeclaration.class);
        } catch (InvalidModelException e) {
            assertThat(e).hasMessage(
                    "The syntax of the filter " +
                            "'not valid' " +
                            "for the service dependency " +
                            "'interface io.neba.core.resourcemodels.factory.ModelInstantiatorTest$ServiceInterface' " +
                            "of the model " +
                            "'class io.neba.core.resourcemodels.factory.ModelInstantiatorTest$TestModelWithInvalidFilterDeclaration' " +
                            "is invalid");
            return;
        }
        fail("Since the model has an invalid @Filter setter argument annotation, creating instantiation metadata must fail.");
    }

    private void assertModelHasProperty(String name, Object serviceInstance) {
        assertThat(this.modelInstance).hasFieldOrPropertyWithValue(name, serviceInstance);
    }

    private <T> T withOsgiService(Class<T> serviceType) {
        @SuppressWarnings("unchecked")
        ServiceReference<T> reference = mock(ServiceReference.class);
        T instance = mock(serviceType);
        doReturn(reference).when(this.context).getServiceReference(serviceType);
        doReturn(instance).when(this.context).getService(reference);
        return instance;
    }

    private <T> T withOsgiService(Class<T> serviceType, String filter) throws InvalidSyntaxException {
        @SuppressWarnings("unchecked")
        ServiceReference<T> reference = mock(ServiceReference.class);
        T instance = mock(serviceType);
        doReturn(singleton(reference)).when(this.context).getServiceReferences(serviceType, filter);
        doReturn(instance).when(this.context).getService(reference);
        return instance;
    }

    private void assertModelIsInstantiatedUsing(String name) {
        assertThat(this.modelInstance.getConstructorSignature()).isEqualTo(name);
    }

    @SuppressWarnings("unchecked")
    private void assertModelWasInjectedWithListOf(Object... serviceInstance) {
        assertThat(this.modelInstance.getInjected()).isInstanceOf(List.class);
        assertThat((List) this.modelInstance.getInjected()).containsExactly(serviceInstance);
    }

    private void createModelInstance() throws ReflectiveOperationException {
        this.modelInstance = this.testee.create(this.context);
    }

    private <T extends TestModel> void withMetadataFor(Class<T> modelType) {
        this.testee = new ModelInstantiator<>(modelType);
    }

    public static class TestModel {
        private final String constructorSignature;
        private Object injected;

        TestModel(String signature) {
            this.constructorSignature = signature;
        }

        public TestModel() {
            this("TestModel()");
        }

        @SuppressWarnings("unused")
        public TestModel(Object something) {
            this("TestModel(something)");
        }

        String getConstructorSignature() {
            return constructorSignature;
        }

        Object getInjected() {
            return injected;
        }

        void setInjected(Object injected) {
            this.injected = injected;
        }
    }

    @SuppressWarnings("unused")
    public static class TestModelWithInjectConstructor extends TestModel {

        private final ServiceInterface s1;
        private final OtherServiceInterface s2;

        @Inject
        public TestModelWithInjectConstructor(ServiceInterface s1, OtherServiceInterface s2) {
            super("TestModelWithInjectConstructor(ServiceInterface s1, OtherServiceInterface s2)");
            this.s1 = s1;
            this.s2 = s2;
        }

        public ServiceInterface getS1() {
            return s1;
        }

        public OtherServiceInterface getS2() {
            return s2;
        }

    }

    public static class TestModelWithoutPublicConstructor extends TestModel {
        private TestModelWithoutPublicConstructor() {
        }
    }

    @SuppressWarnings({"MultipleInjectedConstructorsForClass", "unused"})
    public static class TestModelWithMultipleInjectConstructors extends TestModel {

        @Inject
        public TestModelWithMultipleInjectConstructors(ServiceInterface s1) {
            new TestModelWithSetterDependency();
        }

        @Inject
        public TestModelWithMultipleInjectConstructors(OtherServiceInterface s2) {
        }

    }

    @SuppressWarnings("unused")
    public static class TestModelWithSetterDependency extends TestModel {

        private ServiceInterface serviceInterface;

        @Inject
        public void setServiceInterface(ServiceInterface serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        public void setSomethingElse(Object o) {

        }

        public ServiceInterface getServiceInterface() {
            return serviceInterface;
        }

    }

    @SuppressWarnings("unused")
    public static class TestModelWithInvalidSetter extends TestModel {

        @Inject
        public void setServiceInterface(ServiceInterface serviceInterface, OtherServiceInterface otherServiceInterface) {
        }
    }

    @SuppressWarnings("unused")
    public static class TestModelWithOptionalSetterDependency extends TestModel {
        private Optional<ServiceInterface> serviceInterface;

        @Inject
        public void setServiceInterface(Optional<ServiceInterface> serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        public Optional<ServiceInterface> getServiceInterface() {
            return serviceInterface;
        }
    }

    @SuppressWarnings("unused")
    public static class TestModelWithOptionalConstructorDependency extends TestModel {
        private final Optional<ServiceInterface> serviceInterface;

        @Inject
        public TestModelWithOptionalConstructorDependency(Optional<ServiceInterface> serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        public Optional<ServiceInterface> getServiceInterface() {
            return serviceInterface;
        }
    }

    @SuppressWarnings("unused")
    public static class TestModelWithFilteredSetterDependency extends TestModel {
        private ServiceInterface serviceInterface;

        @Inject
        public void setServiceInterface(@Filter("(property=1)") ServiceInterface serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        public ServiceInterface getServiceInterface() {
            return serviceInterface;
        }
    }

    @SuppressWarnings("unused")
    public static class TestModelWithFilteredConstructorDependency extends TestModel {
        private final ServiceInterface serviceInterface;

        @Inject
        public TestModelWithFilteredConstructorDependency(@Filter("(property=1)") ServiceInterface serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        public ServiceInterface getServiceInterface() {
            return serviceInterface;
        }
    }

    @SuppressWarnings("unused")
    public static class TestModelWithFilteredSetterDependencyViaMetaAnnotation extends TestModel {
        private ServiceInterface serviceInterface;

        @Inject
        public void setServiceInterface(@CustomFilterAnnotation ServiceInterface serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        public ServiceInterface getServiceInterface() {
            return serviceInterface;
        }
    }

    @SuppressWarnings("unused")
    public static class TestModelWithFilteredConstructorDependencyViaMetaAnnotation extends TestModel {
        private final ServiceInterface serviceInterface;

        @Inject
        public TestModelWithFilteredConstructorDependencyViaMetaAnnotation(@CustomFilterAnnotation ServiceInterface serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        public ServiceInterface getServiceInterface() {
            return serviceInterface;
        }
    }

    @SuppressWarnings("unused")
    public static class TestModelWithFilteredListOfDependencies extends TestModel {
        private final List<ServiceInterface> serviceInterface;

        @Inject
        public TestModelWithFilteredListOfDependencies(@Filter("(property=1)") List<ServiceInterface> serviceInterface) {
            this.serviceInterface = serviceInterface;
            setInjected(serviceInterface);
        }

        public List<ServiceInterface> getServiceInterface() {
            return serviceInterface;
        }
    }


    @SuppressWarnings("unused")
    public static class TestModelWithInvalidFilterDeclaration extends TestModel {
        private final ServiceInterface serviceInterface;

        @Inject
        public TestModelWithInvalidFilterDeclaration(@Filter("not valid") ServiceInterface serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        public ServiceInterface getServiceInterface() {
            return serviceInterface;
        }
    }


    private interface ServiceInterface {
    }

    private interface OtherServiceInterface {
    }

    @Filter("(property=1)")
    @Retention(RUNTIME)
    private @interface CustomFilterAnnotation {
    }
}
