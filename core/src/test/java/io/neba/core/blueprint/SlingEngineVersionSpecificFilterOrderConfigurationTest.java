package io.neba.core.blueprint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SlingEngineVersionSpecificFilterOrderConfigurationTest {
    @Mock
    private BundleContext context;
    @Mock
    private Bundle
            slingEngineBundle,
            otherBundle;

    private Version slingEngineVersion;

    @Mock
    private BeanDefinition
            requestScopedCacheService,
            requestContextFilterService;

    @Mock
    private MutablePropertyValues
            requestScopedCacheServiceProperties,
            requestContextFilterServiceProperties;

    @Mock
    private ConfigurableListableBeanFactory factory;

    @InjectMocks
    private SlingEngineVersionSpecificFilterOrderConfiguration testee;

    @Before
    public void setUp() throws Exception {
        doAnswer(invocationOnMock -> slingEngineVersion).when(this.slingEngineBundle).getVersion();

        doReturn(new Bundle[]{this.slingEngineBundle, this.otherBundle}).when(this.context).getBundles();

        Dictionary<String, String> otherManifest = new Hashtable<>();
        Dictionary<String, String> slingEngineManifest = new Hashtable<>();

        otherManifest.put(BUNDLE_SYMBOLICNAME, "some.other.bundle");
        slingEngineManifest.put(BUNDLE_SYMBOLICNAME, "org.apache.sling.engine");

        doReturn(otherManifest).when(this.otherBundle).getHeaders();
        doReturn(slingEngineManifest).when(this.slingEngineBundle).getHeaders();

        doReturn(this.requestContextFilterService).when(this.factory).getBeanDefinition(eq("requestContextFilterService"));
        doReturn(this.requestScopedCacheService).when(this.factory).getBeanDefinition(eq("requestScopedResourceModelCacheService"));

        doReturn(this.requestContextFilterServiceProperties).when(this.requestContextFilterService).getPropertyValues();
        doReturn(this.requestScopedCacheServiceProperties).when(this.requestScopedCacheService).getPropertyValues();
    }

    @Test
    public void testServiceRankingIsNegativeForSlingEngine22() throws Exception {
        withSlingEngineVersion(2, 3, 2);
        postProcessBeanFactory();

        verifyRequestContextFilterRankingIs(-10000);
        verifyRequestScopedCacheFilterRankingIs(-9999);
    }

    @Test
    public void testServiceRankingIsPositiveForSlingEngine23() throws Exception {
        withSlingEngineVersion(2, 3, 3);
        postProcessBeanFactory();

        verifyRequestContextFilterRankingIs(10000);
        verifyRequestScopedCacheFilterRankingIs(9999);
    }

    @Test(expected = IllegalStateException.class)
    public void testHandlingOfMissingRequestContextFilterService() throws Exception {
        withMissingRequestContextFilterServiceBeanDefinition();
        postProcessBeanFactory();
    }

    @Test(expected = IllegalStateException.class)
    public void testHandlingOfMissingRequestScopedCacheService() throws Exception {
        withMissingRequestScopedCacheServiceBeanDefinition();
        postProcessBeanFactory();
    }

    private void withMissingRequestScopedCacheServiceBeanDefinition() {
        doReturn(null).when(this.factory).getBeanDefinition(eq("requestContextFilterService"));
    }

    private void withMissingRequestContextFilterServiceBeanDefinition() {
        doReturn(null).when(this.factory).getBeanDefinition(eq("requestScopedResourceModelCacheService"));
    }

    private void verifyRequestScopedCacheFilterRankingIs(int value) {
        verify(this.requestScopedCacheServiceProperties).add(eq("ranking"), eq(value));
    }

    private void verifyRequestContextFilterRankingIs(int value) {
        verify(this.requestContextFilterServiceProperties).add(eq("ranking"), eq(value));
    }

    private void postProcessBeanFactory() {
        this.testee.postProcessBeanFactory(this.factory);
    }

    private void withSlingEngineVersion(int major, int minor, int micro) {
        this.slingEngineVersion = new Version(major, minor, micro);
    }
}