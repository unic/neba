package io.neba.core.resourcemodels.registration;

import io.neba.api.annotations.ResourceModel;
import io.neba.api.spi.ResourceModelFactory;
import io.neba.api.spi.ResourceModelFactory.ModelDefinition;
import io.neba.core.resourcemodels.adaptation.ResourceToModelAdapterUpdater;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.util.OsgiModelSource;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;


import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.osgi.framework.ServiceEvent.MODIFIED;
import static org.osgi.framework.ServiceEvent.UNREGISTERING;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelRegistrarTest {
    @Mock
    private ModelRegistry modelRegistry;
    @Mock
    private ResourceToModelAdapterUpdater resourceToModelAdapterUpdater;
    @Mock
    private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;
    @Mock
    private BundleContext context;
    @Mock
    private ServiceReference referenceToModelFactory;
    @Mock
    private Bundle bundle;

    private String[] modelResourceTypes;
    private ServiceListener listener;

    @InjectMocks
    private ModelRegistrar testee;

    @Before
    public void setUp() throws Exception {
        doReturn("test.bundle").when(bundle).getSymbolicName();
        doReturn(new Version(1, 0, 0)).when(bundle).getVersion();

        doAnswer(i -> {
            listener = (ServiceListener) i.getArguments()[0];
            return null;
        }).when(this.context).addServiceListener(any(), any());
    }

    @Test
    public void testRegistrationOfResourceModelFromTrackedModelFactory() throws Exception {
        withExistingResourceModelInModelFactoryService();

        activate();

        verifyResourceModelIsRegistered();
    }

    @Test
    public void testNoModelRegistrationOccursWhenNoModelFactoriesAreAvailable() throws Exception {
        activate();

        verifyNoResourceModelIsRegistered();
    }

    @Test
    public void testResourceModelsAreRemovedWhenResourceModelFactoryIsUnregistered() throws Exception {
        withExistingResourceModelInModelFactoryService();
        activate();

        unregisterModelFactory();
        verifyResourceModelIsRemoved();
    }

    @Test
    public void testResourceModelsAreRemovedAndReAddedWhenAModelFactoryChanges() throws Exception {
        withExistingResourceModelInModelFactoryService();
        activate();

        changeModelFactoryService();

        verifyResourceModelIsRemoved();
        verifyResourceModelIsRegistered();
    }

    private void changeModelFactoryService() {
        this.listener.serviceChanged(new ServiceEvent(MODIFIED, this.referenceToModelFactory));
    }

    @Test
    public void testServiceRegistrationIsAddedAndRemovedWhenRegistrarIsActivatedAndDeactivated() throws Exception {
        activate();
        verifyServiceListenerIsAdded();

        deactivate();
        verifyServiceListenerIsRemoved();
    }

    private void unregisterModelFactory() {
        this.listener.serviceChanged(new ServiceEvent(UNREGISTERING, this.referenceToModelFactory));
    }

    private void verifyServiceListenerIsAdded() throws InvalidSyntaxException {
        verify(this.context).addServiceListener(any(), eq("(objectClass=" + ResourceModelFactory.class.getName() + ")"));
    }

    private void verifyServiceListenerIsRemoved() {
        verify(this.context).removeServiceListener(any());
    }

    private void deactivate() {
        this.testee.deactivate();
    }

    private void verifyNoResourceModelIsRegistered() {
        verify(this.resourceModelMetaDataRegistrar, never()).register(any());
        verify(this.modelRegistry, never()).add(any(), any());
        verify(this.resourceToModelAdapterUpdater, never()).refresh();
    }

    /**
     * When a model is registered, the following steps have to be executed in order:
     * <ol>
     *     <li>Registration of model metadata (mappable fields et. al.)</li>
     *     <li>Addition of the model to the model registry for lookup</li>
     *     <li>Refresh the resource to model adapter factory to include the model</li>
     * </ol>
     */
    private void verifyResourceModelIsRegistered() {
        InOrder inOrder = Mockito.inOrder(this.resourceModelMetaDataRegistrar, this.modelRegistry, this.resourceToModelAdapterUpdater);

        inOrder.verify(this.resourceModelMetaDataRegistrar).register(isA(OsgiModelSource.class));
        inOrder.verify(this.modelRegistry).add(eq(this.modelResourceTypes), isA(OsgiModelSource.class));
        inOrder.verify(this.resourceToModelAdapterUpdater).refresh();
    }

    /**
     * When a model is unregistered, the following steps have to be executed in order:
     * <ol>
     *     <li>Removal of the model from the model registry for lookup</li>
     *     <li>Removal of model metadata (mappable fields et. al.)</li>
     *     <li>Refresh the resource to model adapter factory to reflect the removed model</li>
     * </ol>
     */
    private void verifyResourceModelIsRemoved() {
        InOrder inOrder = Mockito.inOrder(this.modelRegistry, this.resourceModelMetaDataRegistrar, this.resourceToModelAdapterUpdater);
        inOrder.verify(this.modelRegistry).removeResourceModels(this.bundle);
        inOrder.verify(this.resourceModelMetaDataRegistrar).removeMetadataForModelsIn(this.bundle);
        inOrder.verify(this.resourceToModelAdapterUpdater).refresh();
    }

    private void withExistingResourceModelInModelFactoryService() throws InvalidSyntaxException {
        this.modelResourceTypes = new String[]{"some/resource/type"};

        ResourceModelFactory factory = mock(ResourceModelFactory.class);
        ResourceModel resourceModel = mock(ResourceModel.class);
        ModelDefinition modelDefinition = mock(ModelDefinition.class);

        doReturn(factory).when(this.context).getService(this.referenceToModelFactory);
        doReturn(this.modelResourceTypes).when(resourceModel).types();
        doReturn(resourceModel).when(modelDefinition).getResourceModel();
        doReturn(bundle).when(referenceToModelFactory).getBundle();
        doReturn(new ServiceReference[]{referenceToModelFactory}).when(this.context).getAllServiceReferences(any(), any());
        doReturn(factory).when(this.context).getService(referenceToModelFactory);
        List<ModelDefinition> modelDefinitions = singletonList(modelDefinition);
        doReturn(modelDefinitions).when(factory).getModelDefinitions();
    }

    private void activate() throws InvalidSyntaxException {
        this.testee.activate(this.context);
    }
}