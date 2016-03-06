/**
 * Copyright 2013 the original author or authors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.neba.core.spring.web.filter;

import org.apache.sling.bgservlets.BackgroundHttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.reflect.core.Reflection.field;
import static org.fest.reflect.core.Reflection.method;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import static org.springframework.context.i18n.LocaleContextHolder.getLocaleContext;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.context.request.RequestContextHolder.getRequestAttributes;


/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class NebaRequestContextFilterTest {
    private final ExecutorService executorService = newSingleThreadExecutor();

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private Runnable destructionCallback;

    private final Locale locale = Locale.ENGLISH;

    private RequestAttributes requestAttributes;
    private LocaleContext localeContext;
    private RequestAttributes inheritedRequestAttributes;
    private LocaleContext inheritedLocalContext;

    @InjectMocks
    private NebaRequestContextFilter testee;

    @Before
    public void setUp() throws IOException, ServletException {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                requestAttributes = getRequestAttributes();

                if (requestAttributes != null) {
                    requestAttributes.registerDestructionCallback("TEST CALLBACK", destructionCallback, SCOPE_REQUEST);
                }

                localeContext = getLocaleContext();

                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        inheritedRequestAttributes = getRequestAttributes();
                        inheritedLocalContext = getLocaleContext();
                    }
                }).get();

                return null;
            }
        }).when(chain).doFilter(isA(HttpServletRequest.class), isA(HttpServletResponse.class));

        doReturn(locale).when(request).getLocale();
    }

    @After
    public void assertThreadLocalesAreRemoved() throws Exception {
        assertThat(getRequestAttributes()).isNull();
        assertThat(getLocaleContext()).isNull();
    }

    @After
    public void verifyServletAttributesAreCompleted() throws Exception {
        verify(destructionCallback).run();
    }

    @Test
    public void testForegroundRequestAreNotWrappedWithBackgroundRequestWrapper() throws Exception {
        doFilter();
        assertRequestAttributesAreProvided();
        assertExposedRequestIsNotModified();
    }

    @Test
    public void testBackgroundRequestsAreWrappedWithBackgroundRequestWrapper() throws Exception {
        withBackgroundRequest();
        doFilter();
        assertRequestAttributesAreProvided();
        assertExposedRequestIsBackgroundRequestWrapper();
    }

    @Test
    public void testFilterProvidesRequestLocaleInLocaleContext() throws Exception {
        doFilter();
        assertLocaleContextProvidesRequestLocale();
    }

    @Test
    public void testContextsAreInheritedWhenThreadContextInheritanceIsTrue() throws Exception {
        withThreadContextInheritable();
        doFilter();
        assertContextsAreInherited();
    }

    @Test
    public void testContextAreNotInheritedByDefault() throws Exception {
        doFilter();
        assertContextsAreNotInherited();
    }

    @Test
    public void testFilterToleratesAbsenceOfOptionalDependencyToBgHttpServletRequest() throws Exception {
        ClassLoader classLoaderWithoutBgServlets = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (BackgroundHttpServletRequest.class.getName().equals(name)) {
                    // This optional dependency is not present on the class path in this test scenario.
                    throw new ClassNotFoundException("THIS IS AN EXPECTED TEST EXCEPTION. The dependency to bgservlets is optional.");
                }
                if (NebaRequestContextFilter.class.getName().equals(name)) {
                    // Define the test subject's class class in this class loader, thus its dependencies -
                    // such as the background servlet request - are also loaded via this class loader.
                    try {
                        byte[] classFileData = toByteArray(getResourceAsStream(name.replace('.', '/').concat(".class")));
                        return defineClass(name, classFileData, 0, classFileData.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException("Unable to load " + name + ".", e);
                    }
                }

                return super.loadClass(name);
            }
        };

        Class<?> filterClass = classLoaderWithoutBgServlets.loadClass(NebaRequestContextFilter.class.getName());

        assertThat(field("IS_BGSERVLETS_PRESENT").ofType(boolean.class).in(filterClass).get()).isFalse();

        Object filter = filterClass.newInstance();

        method("doFilter")
                .withParameterTypes(ServletRequest.class, ServletResponse.class, FilterChain.class)
                .in(filter)
                .invoke(request, response, chain);
    }

    private void assertContextsAreNotInherited() {
        assertThat(inheritedRequestAttributes).isNull();
        assertThat(inheritedLocalContext).isNull();
    }

    private void assertContextsAreInherited() {
        assertThat(inheritedRequestAttributes).isEqualTo(requestAttributes);
        assertThat(inheritedLocalContext).isEqualTo(localeContext);
    }

    private void withThreadContextInheritable() {
        testee.setThreadContextInheritable(true);
    }

    private void assertLocaleContextProvidesRequestLocale() {
        assertThat(localeContext).isNotNull();
        assertThat(localeContext.getLocale()).isSameAs(locale);
    }

    private void assertExposedRequestIsBackgroundRequestWrapper() {
        assertThat(((ServletRequestAttributes) requestAttributes).getRequest()).isInstanceOf(BackgroundServletRequestWrapper.class);
    }

    private void assertRequestAttributesAreProvided() {
        assertThat(requestAttributes).isInstanceOf(ServletRequestAttributes.class);
    }

    private void withBackgroundRequest() {
        request = mock(BackgroundHttpServletRequest.class);
    }

    private void doFilter() throws ServletException, IOException {
        testee.doFilter(request, response, chain);
    }

    private void assertExposedRequestIsNotModified() {
        assertThat(((ServletRequestAttributes) requestAttributes).getRequest()).isSameAs(request);
    }
}