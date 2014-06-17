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

package io.neba.core.blueprint;

import static org.springframework.util.Assert.notNull;

import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.stereotype.Service;

import io.neba.core.util.OsgiBeanReference;
import io.neba.core.util.OsgiBeanSource;

/**
 * Determines whether references to elements of a bundle are valid.<br />
 * Valid references are references to {@link Bundle#ACTIVE active} bundles.
 * 
 * @author Olaf Otto
 */
@Service
public class ReferenceConsistencyChecker implements BundleContextAware {
    private BundleContext context;

    public boolean isValid(OsgiBeanSource<?> source) {
        notNull(source, "Method argument source must not be null.");
        final Bundle bundle = getBundle(source.getBundleId());
        return isValid(bundle);
    }

    public boolean isValid(OsgiBeanReference<?> reference) {
        notNull(reference, "Method argument reference must not be null.");
        final Bundle bundle = getBundle(reference.getBundleId());
        return isValid(bundle);
    }

    private Bundle getBundle(long bundleId) {
        return this.context.getBundle(bundleId);
    }
    
    private boolean isValid(Bundle bundle) {
        return bundle != null && (bundle.getState() == Bundle.ACTIVE);
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.context = bundleContext;
    }
}
