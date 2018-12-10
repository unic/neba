package io.neba.core.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import static io.neba.core.util.NodeUtil.geMixinTypes;
import static io.neba.core.util.NodeUtil.getPrimaryType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeUtilTest {
    @Mock
    private Node node;

    @Test
    public void testRetrievalOfPrimaryNodeTypeViaApi() throws RepositoryException {
        withPrimaryType("nt:file");
        assertThat(getPrimaryType(this.node)).isEqualTo("nt:file");
    }

    @Test
    public void testRetrievalOfPrimaryNodeTypeViaNodeProperty() throws RepositoryException {
        withJcrPrimaryTypeNodeProperty("nt:file");
        assertThat(getPrimaryType(this.node)).isEqualTo("nt:file");
    }

    @Test
    public void testRetrievalOfMixinTypesViaApi() throws RepositoryException {
        withMixinTypes("nt:file", "rep:AccessControllable");
        assertThat(geMixinTypes(this.node)).isEqualTo("nt:file,rep:AccessControllable");
    }

    @Test
    public void testRetrievalOfMixinTypesViaNodeProperty() throws RepositoryException {
        withJcrMixinTypesNodeProperty("nt:file", "rep:AccessControllable");
        assertThat(geMixinTypes(this.node)).isEqualTo("nt:file,rep:AccessControllable");
    }

    @Test
    public void testHandlingOfMissingMixinTypes() throws RepositoryException {
        assertThat(geMixinTypes(this.node)).isNull();
    }

    @Test
    public void testHandlingOfEmptyMixinTypes() throws RepositoryException {
        withJcrMixinTypesNodeProperty();
        assertThat(geMixinTypes(this.node)).isNull();
    }

    @Test
    public void testEfficientLookupOfPrimaryTypeViaPropertyTakesPrecedenceOverApiUsage() throws RepositoryException {
        withPrimaryType("via:Api");
        withJcrPrimaryTypeNodeProperty("via:Property");

        assertThat(getPrimaryType(this.node)).isEqualTo("via:Property");
    }

    @Test
    public void testEfficientLookupOfMixinTypesViaPropertyTakesPrecedenceOverApiUsage() throws RepositoryException {
        withMixinTypes("via:Api");
        withJcrMixinTypesNodeProperty("via:Property");

        assertThat(geMixinTypes(this.node)).isEqualTo("via:Property");
    }

    private void withJcrPrimaryTypeNodeProperty(String nodeTypeName) throws RepositoryException {
        Property nodeType = mock(Property.class);
        doReturn(nodeTypeName).when(nodeType).getString();
        when(node.hasProperty("jcr:primaryType")).thenReturn(true);
        when(node.getProperty("jcr:primaryType")).thenReturn(nodeType);
    }

    private void withJcrMixinTypesNodeProperty(String... types) throws RepositoryException {
        Value[] mixinTypes = new Value[types.length];

        for (int i = 0; i < types.length; ++i) {
            mixinTypes[i] = spy(mock(Value.class));
            doReturn(types[i]).when(mixinTypes[i]).toString();
        }

        Property mixinTypesProperty = mock(Property.class);

        doReturn(mixinTypes).when(mixinTypesProperty).getValues();
        doReturn(true).when(node).hasProperty("jcr:mixinTypes");
        doReturn(mixinTypesProperty).when(node).getProperty("jcr:mixinTypes");
    }


    private void withPrimaryType(String nodeTypeName) throws RepositoryException {
        NodeType nodeType = mock(NodeType.class);
        when(node.getPrimaryNodeType()).thenReturn(nodeType);
        when(nodeType.getName()).thenReturn(nodeTypeName);
    }

    private void withMixinTypes(String... types) throws RepositoryException {
        NodeType[] mixinTypes = new NodeType[types.length];
        for (int i = 0; i < types.length; ++i) {
            mixinTypes[i] = spy(mock(NodeType.class));
            doReturn(types[i]).when(mixinTypes[i]).toString();
        }
        when(node.getMixinNodeTypes()).thenReturn(mixinTypes);
    }
}