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

package io.neba.core.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.junit.Test;


import static io.neba.core.util.ReflectionUtil.findField;
import static io.neba.core.util.ReflectionUtil.getLowerBoundOfSingleTypeParameter;
import static io.neba.core.util.ReflectionUtil.instantiateCollectionType;
import static io.neba.core.util.ReflectionUtil.isInstantiableCollectionType;
import static io.neba.core.util.ReflectionUtil.methodsOf;
import static org.apache.commons.lang3.reflect.TypeUtils.getRawType;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class ReflectionUtilTest {
    /**
     * Declares a member with a type variable.
     */
    private abstract static class Root<T> {
        @SuppressWarnings("unused")
        private List<T> rootList;
    }

    /**
     * Declares a member with a type variable while introducing another type variable
     * for the generic super class.
     */
    private abstract static class GenericModel<T, K> extends Root<K> {
        @SuppressWarnings("unused")
        private List<T> genericModelList;
    }

    /**
     * Defines the type variables of its super types.
     */
    private static class GenericModelImpl extends GenericModel<Boolean, Integer> {
        @SuppressWarnings("unused")
        private List<Boolean> booleans;
    }

    private interface TestInterface {
        default void interfaceMethod() {
        }

        @Inject
        void abstractInterfaceMethod();
    }

    private static abstract class TestSuperClass {
        private String superClassField;

        private void superClassMethod() {}

        @Inject
        abstract void abstractSuperClassMethod();
    }

    private static class TestClass extends TestSuperClass implements TestInterface {
        public String testClassField;

        @Inject
        private void classMethod() {}

        @Override
        void abstractSuperClassMethod() {
        }

        @Override
        public void abstractInterfaceMethod() {

        }
    }

    @SuppressWarnings({"unused", "rawtypes"})
    private Collection rawCollection;
    @SuppressWarnings("unused")
    private Collection<String> stringCollection;
    @SuppressWarnings("unused")
    private Collection<?> unknownCollection;
    @SuppressWarnings("unused")
    private Collection<? extends ReflectionUtilTest> readOnlyCollection;
    @SuppressWarnings("unused")
    private Collection<? super ReflectionUtilTest> boundCollection;

    private Class<?> typeParameter;
    private Object collectionInstance;
    private Class<?> type = getClass();

    @Test(expected = IllegalArgumentException.class)
    public void testResolutionOfRawCollectionType() throws Exception {
        getGenericTypeParameterOf("rawCollection");
    }

    @Test
    public void testResolutionOfSimpleCollectionType() throws Exception {
        getGenericTypeParameterOf("stringCollection");
        assertTypeParameterIs(String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolutionOfReadOnlyCollectionType() throws Exception {
        getGenericTypeParameterOf("readOnlyCollection");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolutionOfUnknownCollectionType() throws Exception {
        getGenericTypeParameterOf("unknownCollection");
    }

    @Test
    public void testResolutionOfBoundCollectionType() throws Exception {
        getGenericTypeParameterOf("boundCollection");
        assertTypeParameterIs(ReflectionUtilTest.class);
    }

    @Test
    public void testInstantiationOfCollection() throws Exception {
        instantiate(Collection.class);
        assertInstanceIsOfType(ArrayList.class);
    }

    @Test
    public void testInstantiationOfCollectionWithLength() throws Exception {
        instantiate(Collection.class, 1);
        assertInstanceIsOfType(ArrayList.class);
    }

    @Test
    public void testInstantiationOfList() throws Exception {
        instantiate(List.class);
        assertInstanceIsOfType(ArrayList.class);
    }

    @Test
    public void testInstantiationOfSet() throws Exception {
        instantiate(Set.class);
        assertInstanceIsOfType(LinkedHashSet.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstantiationOfConcreteListImplementation() throws Exception {
        instantiate(LinkedList.class);
    }

    @Test
    public void testDetectionOfInstantiableCollectionType() throws Exception {
        assertThat(isInstantiableCollectionType(Collection.class));
        assertThat(isInstantiableCollectionType(Set.class));
        assertThat(isInstantiableCollectionType(List.class));
    }

    @Test
    public void testResolutionOfTypeParameterWithTypeVariableInParent() throws Exception {
        this.type = GenericModelImpl.class;
        getGenericTypeParameterOf("genericModelList");
        assertTypeParameterIs(Boolean.class);
    }

    @Test
    public void testResolutionOfTypeParameterWithTypeVariableInParentsParent() throws Exception {
        this.type = GenericModelImpl.class;
        getGenericTypeParameterOf("rootList");
        assertTypeParameterIs(Integer.class);
    }

    @Test
    public void testResolutionOfTypeParameterWithTypeVariableInDeclaringClass() throws Exception {
        this.type = GenericModelImpl.class;
        getGenericTypeParameterOf("booleans");
        assertTypeParameterIs(Boolean.class);
    }

    @Test
    public void testResolutionOfMethods() throws Exception {
        assertThat(methodsOf(TestClass.class))
                .contains(
                        TestClass.class.getDeclaredMethod("classMethod"),
                        TestClass.class.getDeclaredMethod("abstractSuperClassMethod"),
                        TestClass.class.getDeclaredMethod("abstractInterfaceMethod"),
                        TestSuperClass.class.getDeclaredMethod("superClassMethod"),
                        TestSuperClass.class.getDeclaredMethod("abstractSuperClassMethod"),
                        TestInterface.class.getMethod("interfaceMethod"),
                        TestInterface.class.getMethod("abstractInterfaceMethod"));
    }

    @Test
    public void testFindField() throws Exception {
        assertThat(findField(TestClass.class, "testClassField")).isEqualTo(TestClass.class.getDeclaredField("testClassField"));
        assertThat(findField(TestClass.class, "superClassField")).isEqualTo(TestSuperClass.class.getDeclaredField("superClassField"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Collection> void instantiate(Class<T> collectionType) {
        this.collectionInstance = instantiateCollectionType(collectionType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Collection> void instantiate(Class<T> collectionType, int length) {
        this.collectionInstance = instantiateCollectionType(collectionType, length);
    }

    private void getGenericTypeParameterOf(String name) throws NoSuchFieldException {
        this.typeParameter = getRawType(getLowerBoundOfSingleTypeParameter(getField(name).getGenericType()), this.type);
    }

    private Field getField(String name) {
        return findField(this.type, name);
    }

    private void assertTypeParameterIs(Class<?> expected) {
        assertThat(this.typeParameter).isEqualTo(expected);
    }

    private void assertInstanceIsOfType(Class<?> type) {
        assertThat(this.collectionInstance).isNotNull();
        assertThat(this.collectionInstance).isInstanceOf(type);
    }
}
