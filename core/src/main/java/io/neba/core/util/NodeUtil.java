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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Utilities for working with {@link javax.jcr.Node JCR nodes}.
 *
 * @author Olaf Otto
 */
public class NodeUtil {
    private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    private static final String JCR_MIXIN_TYPES = "jcr:mixinTypes";

    /**
     * @param node must not be <code>null</code>.
     * @return the <code>primary type</code> name of the given node, never <code>null</code>.
     * @throws RepositoryException if accessing the node fails due to an unrecoverable repository error.
     */
    public static @Nonnull
    String getPrimaryType(@Nonnull Node node) throws RepositoryException {
        return node.hasProperty(JCR_PRIMARY_TYPE) ?
                node.getProperty(JCR_PRIMARY_TYPE).getString() :
                node.getPrimaryNodeType().getName();
    }

    /**
     * @param node must not be <code>null</code>.
     * @return a String with the comma separated mixin type names assigned to the given node, or <code>null</code> if the node has no mixin types.
     * @throws RepositoryException if accessing the node fails due to an unrecoverable repository error.
     */
    public static @CheckForNull
    String geMixinTypes(@Nonnull Node node) throws RepositoryException {
        Object[] mixinTypes = node.hasProperty(JCR_MIXIN_TYPES) ?
                node.getProperty(JCR_MIXIN_TYPES).getValues() :
                node.getMixinNodeTypes();

        if (mixinTypes == null || mixinTypes.length == 0) {
            return null;
        }

        StringBuilder commaSeparatedMixinTypeNames = new StringBuilder(64);

        for (int i = 0; i < mixinTypes.length; ++i) {
            commaSeparatedMixinTypeNames.append(mixinTypes[i].toString());
            if (i < mixinTypes.length - 1) {
                commaSeparatedMixinTypeNames.append(',');
            }
        }

        return commaSeparatedMixinTypeNames.toString();
    }

    private NodeUtil() {
    }
}
