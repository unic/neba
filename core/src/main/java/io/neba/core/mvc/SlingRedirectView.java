package io.neba.core.mvc;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Does not {@link javax.servlet.http.HttpServletResponse#encodeRedirectURL(String) encode}
 * the target URL: Sling is RESTful, i.e. requests should be stateless. Also, an appended session URL
 * is not part of the Sling URL spec. Lastly, sling incorrectly encodes external URLs, resulting in
 * "http(s)://" to be encoded as "_http(s)_".
 *
 * @author Olaf Otto
 */
public class SlingRedirectView extends RedirectView {

    public SlingRedirectView(String url, boolean contextRelative, boolean http10Compatible) {
        super(url, contextRelative, http10Compatible);
    }

    @Override
    protected void sendRedirect(HttpServletRequest request,
                                HttpServletResponse response,
                                String targetUrl, boolean http10Compatible) throws IOException {
        if (http10Compatible) {
            // Send status code 302 by default.
            response.sendRedirect(targetUrl);
        } else {
            HttpStatus statusCode = getHttp11StatusCode(request, response, targetUrl);
            response.setStatus(statusCode.value());
            response.setHeader("Location", targetUrl);
        }
    }
}
