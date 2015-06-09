package io.neba.core.blueprint;

import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;

import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;

/**
 * In Sling Engine 2.3.3, the semantics of the <code>service.ranking</code> property was adjusted
 * to align to the overall use of service rankings (SLING-2920).
 * Prior to 2.3.3 a low (e.g. negative) value indicated a <em>higher</em> filter priority, i.e. an earlier place in the filter chain.
 * Since 2.3.3, the opposite is the case. Thus, the ranking of filters published by the NEBA must be adjusted
 * with regard to the sling engine version. This is what this post processor does.
 *
 * @author Olaf Otto
 */
@Service
public class SlingEngineVersionSpecificFilterOrderConfiguration implements BeanFactoryPostProcessor, BundleContextAware {
    // This property is automatically converted to "service.ranking" by the gemini blueprint service factory.
    private static final String PROPERTY_RANKING = "ranking";

    private BundleContext context;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        BeanDefinition requestScopedResourceModelCacheService = configurableListableBeanFactory.getBeanDefinition("requestScopedResourceModelCacheService");
        if (requestScopedResourceModelCacheService == null) {
            throw new IllegalStateException("Unable to adjust the service.ranking property of the requestScopedResourceModelCache. " +
                    "Could not locate the bean definition fo the corresponding service in the application context.");
        }
        BeanDefinition requestContextFilterService = configurableListableBeanFactory.getBeanDefinition("requestContextFilterService");
        if (requestContextFilterService == null) {
            throw new IllegalStateException("Unable to adjust the service.ranking property of the requestContextFilter. " +
                    "Could not locate the bean definition fo the corresponding service in the application context.");
        }

        if (isSlingEngineGreater232()) {
            requestContextFilterService.getPropertyValues().add(PROPERTY_RANKING, 10000);
            requestScopedResourceModelCacheService.getPropertyValues().add(PROPERTY_RANKING, 9999);
        } else {
            requestContextFilterService.getPropertyValues().add(PROPERTY_RANKING, -10000);
            requestScopedResourceModelCacheService.getPropertyValues().add(PROPERTY_RANKING, -9999);
        }
    }

    private boolean isSlingEngineGreater232() {
        for (Bundle bundle : this.context.getBundles()) {
            if ("org.apache.sling.engine".equals(bundle.getHeaders().get(BUNDLE_SYMBOLICNAME))) {
                return bundle.getVersion().compareTo(new Version(2, 3, 2)) > 0;
            }
        }

        return false;
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.context = bundleContext;
    }
}
