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

package io.neba.core.resourcemodels.metadata;

import io.neba.api.annotations.Unmapped;
import io.neba.core.util.Annotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

import static io.neba.core.util.Annotations.annotations;
import static org.springframework.util.ReflectionUtils.doWithFields;
import static org.springframework.util.ReflectionUtils.doWithMethods;

/**
 * Represents meta-data of a {@link io.neba.api.annotations.ResourceModel}. Used
 * to retain the result of costly reflection on resource models. Instances are model-scoped, i.e. there is exactly one
 * {@link ResourceModelMetaData} instance per detected {@link io.neba.api.annotations.ResourceModel}.
 *
 * @see ResourceModelMetaDataRegistrar
 *
 * @author Olaf Otto
 */
public class ResourceModelMetaData {
    /**
     * @author Olaf Otto
     */
    private static class MethodMetadataCreator implements ReflectionUtils.MethodCallback {
        private final Collection<MethodMetaData> postMappingMethods = new ArrayList<>(32);
        private final Collection<MethodMetaData> preMappingMethods = new ArrayList<>(32);

        @Override
        public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
            MethodMetaData methodMetaData = new MethodMetaData(method);
            if (methodMetaData.isPostMappingCallback()) {
                this.postMappingMethods.add(methodMetaData);
            }
            if (methodMetaData.isPreMappingCallback()) {
                this.preMappingMethods.add(methodMetaData);
            }
        }

        private MethodMetaData[] getPostMappingMethods() {
            return this.postMappingMethods.toArray(new MethodMetaData[this.postMappingMethods.size()]);
        }

        private MethodMetaData[] getPreMappingMethods() {
            return this.preMappingMethods.toArray(new MethodMetaData[this.preMappingMethods.size()]);
        }
    }

    /**
     * @author Olaf Otto
     */
    private static class FieldMetadataCreator implements ReflectionUtils.FieldCallback {
        private final Collection<MappedFieldMetaData> mappableFields = new ArrayList<>();
        private final Class<?> modelType;

        FieldMetadataCreator(Class<?> modelType) {
            if (modelType == null) {
                throw new IllegalArgumentException("Constructor parameter modelType must not be null.");
            }
            this.modelType = modelType;
        }

        @Override
        public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
            if (isMappingCandidate(field)) {
                MappedFieldMetaData fieldMetaData = new MappedFieldMetaData(field, this.modelType);
                this.mappableFields.add(fieldMetaData);
            }
        }

        private MappedFieldMetaData[] getMappableFields() {
            return this.mappableFields.toArray(new MappedFieldMetaData[this.mappableFields.size()]);
        }

        /**
         * Determines whether a given field's value
         * can be injected from either the current resource properties
         * or another (e.g. referenced) resource.
         */
        private boolean isMappingCandidate(Field field) {
            return !isStatic(field) &&
                   !isFinal(field) &&
                   !isUnmapped(field);
        }

        private boolean isFinal(Field field) {
            return Modifier.isFinal(field.getModifiers());
        }

        private boolean isStatic(Field field) {
            return Modifier.isStatic(field.getModifiers());
        }

        /**
         * @param field must not be <code>null</code>.
         * @return whether the field is explicitly excluded from OCM, e.g. via &#64;{@link Unmapped}.
         */
        private boolean isUnmapped(Field field) {
            final Annotations annotations = annotations(field);
            return annotations.contains(Unmapped.class) ||
                    annotations.containsName("javax.inject.Inject") || // @Inject is an optional dependency, thus using a name constant
                    annotations.containsName(Autowired.class.getName()) ||
                    annotations.containsName(Resource.class.getName());
        }
    }

    private final MappedFieldMetaData[] mappableFields;
    private final MethodMetaData[] postMappingMethods;
    private final MethodMetaData[] preMappingMethods;
    private final String typeName;

    private final ResourceModelStatistics statistics = new ResourceModelStatistics();

    public ResourceModelMetaData(Class<?> modelType) {
        FieldMetadataCreator fc = new FieldMetadataCreator(modelType);
        doWithFields(modelType, fc);

        MethodMetadataCreator mc = new MethodMetadataCreator();
        doWithMethods(modelType, mc);

        this.mappableFields = fc.getMappableFields();
        this.preMappingMethods = mc.getPreMappingMethods();
        this.postMappingMethods = mc.getPostMappingMethods();
        this.typeName = modelType.getName();
    }


    public MappedFieldMetaData[] getMappableFields() {
        return mappableFields;
    }

    public MethodMetaData[] getPostMappingMethods() {
        return postMappingMethods;
    }

    public MethodMetaData[] getPreMappingMethods() {
        return preMappingMethods;
    }

    public String getTypeName() {
        return typeName;
    }

    public ResourceModelStatistics getStatistics() {
        return statistics;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getTypeName() + ']';
    }
}
