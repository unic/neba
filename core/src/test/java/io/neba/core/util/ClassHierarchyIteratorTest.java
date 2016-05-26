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

import static io.neba.core.util.ClassHierarchyIterator.hierarchyOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class ClassHierarchyIteratorTest {
	//CHECKSTYLE:OFF
    public static interface If1 {}
    public static interface If2 {}
    public static interface If3 {}
    public static class Root implements If1 {}
    public static class Child extends Root implements If2, If3 {}
    //CHECKSTYLE:ON
    
    @Test
    public void testHierarchyTraversal() {
        ClassHierarchyIterator it = hierarchyOf(Child.class);
        
        assertThat(it.next()).isSameAs(Child.class);
        assertThat(it.next()).isSameAs(If2.class);
        assertThat(it.next()).isSameAs(If3.class);
        assertThat(it.next()).isSameAs(Root.class);
        assertThat(it.next()).isSameAs(If1.class);
    }
}