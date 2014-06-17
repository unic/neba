package io.neba.core.mvc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SlingRedirectViewTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private boolean http10Compatible = true;

    private SlingRedirectView testee = new SlingRedirectView(null, false, false);

    @Test
    public void testHttp10Redirection() throws Exception {
        redirect("/test.html");
        verifyRedirectUrlIsNotEncoded();
        verifyViewRedirectsTo("/test.html");
    }

    @Test
    public void testHttp11Redirection() throws Exception {
        withoutHttp10Support();
        redirect("/test.html");
        verifyRedirectUrlIsNotEncoded();
        verifyResponseStatusIs303();
        verifyLocationHeaderIs("/test.html");
    }

    private void verifyLocationHeaderIs(String url) {
        verify(this.response).setHeader("Location", url);
    }

    private void verifyResponseStatusIs303() {
        verify(this.response).setStatus(eq(303));
    }

    private void withoutHttp10Support() {
        this.http10Compatible = false;
    }

    private void verifyViewRedirectsTo(String url) throws IOException {
        verify(this.response).sendRedirect(eq(url));
    }

    private void verifyRedirectUrlIsNotEncoded() {
        verify(this.response, never()).encodeRedirectURL(anyString());
    }

    private void redirect(String target) throws IOException {
        this.testee.sendRedirect(this.request, this.response, target, http10Compatible);
    }
}
