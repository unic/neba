/**
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.neba.core.util;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static io.neba.core.util.ReflectionUtil.getCollectionComponentType;
import static io.neba.core.util.ReflectionUtil.instantiateCollectionType;
import static io.neba.core.util.ReflectionUtil.isInstantiableCollectionType;
import static org.fest.assertions.Assertions.assertThat;
import static org.springframework.util.ReflectionUtils.findField;

/**
 * @author Olaf Otto
 */
public class ReflectionUtilTest {
    /**
     * Declares a member with a type variable.
     */
    private abstract static  class Root<T> {
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

    private Class<?> componentType;
    private Object collectionInstance;
    private Class type = getClass();

    @Test(expected = IllegalArgumentException.class)
    public void testResolutionOfRawCollectionType() throws Exception {
        getComponentTypeOf("rawCollection");
    }

    @Test
    public void testResolutionOfSimpleCollectionType() throws Exception {
        getComponentTypeOf("stringCollection");
        assertComponentTypeIs(String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolutionOfReadOnlyCollectionType() throws Exception {
        getComponentTypeOf("readOnlyCollection");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolutionOfUnknownCollectionType() throws Exception {
        getComponentTypeOf("unknownCollection");
    }

    @Test
    public void testResolutionOfBoundCollectionType() throws Exception {
        getComponentTypeOf("boundCollection");
        assertComponentTypeIs(ReflectionUtilTest.class);
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
    public void testResolutionOfComponentTypeWithTypeVariableInParent() throws Exception {
        this.type = GenericModelImpl.class;
        getComponentTypeOf("genericModelList");
        assertComponentTypeIs(Boolean.class);
    }

    @Test
    public void testResolutionOfComponentTypeWithTypeVariableInParentsParent() throws Exception {
        this.type = GenericModelImpl.class;
        getComponentTypeOf("rootList");
        assertComponentTypeIs(Integer.class);
    }

    @Test
    public void testResolutionOfComponentTypeWithTypeVariableInDeclaringClass() throws Exception {
        this.type = GenericModelImpl.class;
        getComponentTypeOf("booleans");
        assertComponentTypeIs(Boolean.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Collection> void instantiate(Class<T> collectionType) {
        this.collectionInstance = instantiateCollectionType(collectionType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Collection> void instantiate(Class<T> collectionType, int length) {
        this.collectionInstance = instantiateCollectionType(collectionType, length);
    }

    private void getComponentTypeOf(String name) throws NoSuchFieldException {
        this.componentType = getCollectionComponentType(this.type, getField(name));
    }

    private Field getField(String name) {
        return findField(this.type, name);
    }

    private void assertComponentTypeIs(Class<?> expected) {
        assertThat(this.componentType).isEqualTo(expected);
    }

    private void assertInstanceIsOfType(Class<?> type) {
        assertThat(this.collectionInstance).isNotNull();
        assertThat(this.collectionInstance).isInstanceOf(type);
    }
}
