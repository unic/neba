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
package io.neba.core.resourcemodels.mapping;

import io.neba.api.resourcemodels.AnnotatedFieldMapper;
import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import io.neba.core.util.Annotations;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.commons.lang3.reflect.FieldUtils.getDeclaredField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class AnnotatedFieldMappersTest {
    private Field field1 = getDeclaredField(TestModel1.class, "resources");
    private Field field2 = getDeclaredField(TestModel2.class, "resource");
    private CustomAnnotation1 annotation1 = this.field1.getAnnotation(CustomAnnotation1.class);
    private CustomAnnotation2 annotation2 = this.field2.getAnnotation(CustomAnnotation2.class);
    private CustomAnnotation1 metaAnnotation1 = CustomAnnotation2.class.getAnnotation(CustomAnnotation1.class);

    @Retention(RUNTIME)
    private @interface CustomAnnotation1 {}

    @CustomAnnotation1
    @Retention(RUNTIME)
    private @interface CustomAnnotation2 {}

    private static class TestModel1 {
        @CustomAnnotation1
        public List<Resource> resources;
    }
    private static class TestModel2 {
        @CustomAnnotation2
        public Resource resource;
    }

    @Mock
    private MappedFieldMetaData metadata1, metadata2;

    @Mock
    private Annotations annotations1, annotations2;

    @Mock
    private AnnotatedFieldMapper mapper1, mapper2, mapper3;

    @InjectMocks
    private AnnotatedFieldMappers testee;

    @Before
    public void setUp() throws Exception {
        doReturn(CustomAnnotation1.class).when(this.mapper1).getAnnotationType();
        doReturn(Collection.class).when(this.mapper1).getFieldType();

        doReturn(CustomAnnotation2.class).when(this.mapper2).getAnnotationType();
        doReturn(Resource.class).when(this.mapper2).getFieldType();

        doReturn(CustomAnnotation2.class).when(this.mapper3).getAnnotationType();
        doReturn(Resource.class).when(this.mapper3).getFieldType();

        doReturn(this.field1).when(this.metadata1).getField();
        doReturn(this.field1.getType()).when(this.metadata1).getType();
        doReturn(this.field2).when(this.metadata2).getField();
        doReturn(this.field2.getType()).when(this.metadata2).getType();

        doReturn(this.annotations1).when(this.metadata1).getAnnotations();
        doReturn(this.annotations2).when(this.metadata2).getAnnotations();

        final List<Annotation> ann1 = new ArrayList<>();
        ann1.add(annotation1);
        doAnswer(invocationOnMock -> ann1.iterator()).when(this.annotations1).iterator();

        final List<Annotation> ann2 = new ArrayList<>();
        ann2.add(annotation2);
        ann2.add(metaAnnotation1);
        doAnswer(invocationOnMock -> ann2.iterator()).when(this.annotations2).iterator();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryingWithNullMetaData() throws Exception {
        this.testee.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdditionOfNullMapper() throws Exception {
        bind(null);
    }

    @Test
    public void testRemovalOfNullMapperDoesNotCauseException() throws Exception {
        unbind(null);
    }

    @Test
    public void testEmptyMappers() throws Exception {
        assertNoMapperExistFor(this.metadata1);
    }

    @Test
    public void testMapperResolutionWithDisjointMappers() throws Exception {
        bind(this.mapper1);
        bind(this.mapper2);

        assertMetadataHasMappers(this.metadata1, this.mapper1);
        assertMetadataHasAnnotations(this.metadata1, this.annotation1);
        assertMetadataHasMappers(this.metadata2, this.mapper2);
        assertMetadataHasAnnotations(this.metadata2, this.annotation2);
    }

    @Test
    public void testMapperResolutionWithOverlappingMappers() throws Exception {
        withMapperSupporting(this.mapper1, Resource.class);

        bind(this.mapper1);
        bind(this.mapper2);

        assertNoMapperExistFor(this.metadata1);
        assertMetadataHasMappers(this.metadata2, this.mapper1, this.mapper2);
        assertMetadataHasAnnotations(this.metadata2, this.annotation2, this.metaAnnotation1);
    }

    @Test
    public void testMappersForBoxedTypesAreaAppliedToTheirPrimitiveVariants() throws Exception {
        bind(this.mapper1);

        doReturn(Boolean.class).when(this.mapper1).getFieldType();
        doReturn(boolean.class).when(this.metadata1).getType();
        assertMetadataHasMappers(this.metadata1, this.mapper1);

        doReturn(Integer.class).when(this.mapper1).getFieldType();
        doReturn(int.class).when(this.metadata1).getType();
        assertMetadataHasMappers(this.metadata1, this.mapper1);
    }

    @Test
    public void testMapperRemoval() throws Exception {
        assertNoMapperExistFor(this.metadata1);

        bind(this.mapper1);
        assertMetadataHasMappers(this.metadata1, this.mapper1);

        unbind(this.mapper1);
        assertNoMapperExistFor(this.metadata1);
    }

    @Test
    public void testLookupCache() throws Exception {
        bind(this.mapper1);

        assertMetadataHasMappers(this.metadata1, this.mapper1);
        assertMetadataHasAnnotations(this.metadata1, this.annotation1);
        assertMetadataHasMappers(this.metadata1, this.mapper1);
        assertMetadataHasAnnotations(this.metadata1, this.annotation1);

        verifyAnnotationsWhereQueriedOnlyOnce();
    }

    @Test
    public void testAdditionAndRemovalOfMappersForSameAnnotationType() throws Exception {
        bind(this.mapper2);
        bind(this.mapper3);

        assertMetadataHasMappers(this.metadata2, this.mapper2, this.mapper3);

        unbind(mapper2);
        assertMetadataHasMappers(this.metadata2, this.mapper3);

        unbind(mapper3);
        assertMetadataHasMappers(this.metadata2);
    }

    private void verifyAnnotationsWhereQueriedOnlyOnce() {
        verify(this.metadata1).getAnnotations();
    }

    private void unbind(AnnotatedFieldMapper mapper) {
        this.testee.unbind(mapper);
    }

    private void withMapperSupporting(AnnotatedFieldMapper mapper, Class<?> type) {
        doReturn(type).when(mapper).getFieldType();
    }

    private void assertNoMapperExistFor(MappedFieldMetaData metadata) {
        assertThat(this.testee.get(metadata)).isEmpty();
    }

    private void assertMetadataHasMappers(MappedFieldMetaData metadata, AnnotatedFieldMapper... mappers) {
        assertThat(this.testee.get(metadata)).extracting("mapper").containsOnly(mappers);
    }

    private void assertMetadataHasAnnotations(MappedFieldMetaData metadata, Annotation... annotations) {
        assertThat(this.testee.get(metadata)).extracting("annotation").containsOnly(annotations);
    }

    private void bind(AnnotatedFieldMapper mapper) {
        this.testee.bind(mapper);
    }
}