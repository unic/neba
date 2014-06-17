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

import org.osgi.framework.Bundle;

/**
 * @author Olaf Otto
 */
public class BundleUtil {
    
    /**
     * @param bundle must not be null.
     * @return the {@link Bundle#getSymbolicName() symbolic name} and
     * {@link Bundle#getVersion() version} as a string, never <code>null</code>.
     */
    public static String displayNameOf(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("Method argument bundle must not be null.");
        }
        return bundle.getSymbolicName() + ' ' + bundle.getVersion();
    }
    
    private BundleUtil() {}
}
