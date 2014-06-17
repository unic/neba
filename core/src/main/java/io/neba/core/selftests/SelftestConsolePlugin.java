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

package io.neba.core.selftests;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.sling.commons.json.io.JSONWriter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import static org.apache.commons.lang.StringEscapeUtils.escapeXml;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;

/**
 * Shows a list of all detected {@link io.neba.api.annotations.SelfTest self tests}
 * in the felix console and provides means to execute tests and obtain test results.
 * 
 * @author Olaf Otto
 */
@Service
public class SelftestConsolePlugin extends AbstractWebConsolePlugin {
    private static final long serialVersionUID = -5152654100152618897L;
    public static final String LABEL = "selftests";
    @Inject
    private SelfTestRegistrar selfTestRegistrar;

    public String getCategory() {
        return "NEBA";
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getTitle() {
        return "Selftests";
    }

    public URL getResource(String path) {
        URL url = null;
        String internalPath = substringAfter(path, "/" + getLabel());
        if (startsWith(internalPath, "/static/")) {
            url = getClass().getResource("/META-INF/consoleplugin/selftests" + internalPath);
        }
        return url;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String suffix = substringAfter(req.getRequestURI(), req.getServletPath() + "/" + getLabel());
        if (!isBlank(suffix) && suffix.startsWith("/run/")) {
            runTest(suffix.substring(4), res);
            return;
        }
        if (!isBlank(suffix) && suffix.equals("/run.xml")) {
            runAll(res);
            return;
        }
        super.doGet(req, res);
    }

    private void runAll(HttpServletResponse response) throws IOException {
        response.setContentType("application/xml;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.write("<tests>");
        for (SelftestReference ref : getSelftestReferences()) {
            String result = execute(ref);
            boolean failed = !isEmpty(result);

            writer.write("<test id=\"");
            writer.write(ref.getId());
            writer.write("\" ");
            writer.write("description=\"");
            writer.write(escapeXml(ref.getDescription()));
            writer.write("\" ");
            writer.write("successMessage=\"");
            writer.write(escapeXml(ref.getSuccess()));
            writer.write("\" ");
            writer.write("failureMessage=\"");
            writer.write(escapeXml(ref.getFailure()));
            writer.write("\" ");
            writer.write("failed=\"" + failed + "\">");
            if (failed) {
                writer.write("<![CDATA[");
                writer.write(result.replace("]]>", "]]]]><![CDATA[>"));
                writer.write("]]>");
            }
            writer.write("</test>");
        }
        writer.write("</tests>");
    }

    private void runTest(String id, HttpServletResponse res) {
        SelftestReference reference = findReferenceById(id);
        String stacktrace = execute(reference);
        try {
            res.setContentType("application/json");
            JSONWriter jsonWriter = new JSONWriter(res.getWriter());
			jsonWriter.object().key("failed").value(stacktrace != null).key("trace").value(stacktrace).key("errorMsg")
				.value(reference.getFailure()).key("successMsg").value(reference.getSuccess()).endObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SelftestReference findReferenceById(String id) {
        SelftestReference reference = null;

        for (SelftestReference ref : getSelftestReferences()) {
            if (id.equals(ref.getId())) {
                reference = ref;
            }
        }

        return reference;
    }

    private String execute(SelftestReference reference) {
        String error = null;
        try {
            reference.execute();
        } catch (Exception e) {
            Throwable cause = getRootCause(e);
            if (cause != null) {
            	error = getStackTrace(cause);
            } else {
            	error = getStackTrace(e);
            }
        }
        return error;
    }

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writeScriptIncludes(request, response);
        writeHeadNavigation(response);

        writer.write("<table id=\"plugin_table\" class=\"nicetable tablesorter noauto\">");
        writer.write("<thead><tr><th>Name</th><th>Source bundle</th><th>Action</th><th>Status</th></tr></thead>");
        writer.write("<tbody>");
        int count = 0;
        for (SelftestReference reference : getSelftestReferences()) {
            writer.write("<tr id=\"row" + count + "\">");
            writer.write("<td>" + reference.getDescription() + "</td>");
            writer.write("<td><a href=\"bundles/" + reference.getBundleId() + "\">" +
            		reference.getBundleId() + "</a></td>");
            writer.write("<td><a href=\"#\" class=\"runlink\" onclick=\"run('" + reference.getId() + "', this, " + 
            		count + ");return false;\">run</a></td>");
            writer.write("<td><div id=\"signal" + count + "\" class=\"signal result" + count
                    + "\" style=\"width:14px;height:14px;background-color:gray;margin:2px 0 0 0;\"></div></td>");
            writer.write("</tr>");
            ++count;
        }
        writer.write("</tbody>");
        writer.write("</table>");

    }

    private void writeScriptIncludes(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().write("<script src=\"" + getLabel() + "/static/script.js\"></script>");
    }

    private void writeHeadNavigation(HttpServletResponse response) throws IOException {
        String template = readTemplateFile("/META-INF/consoleplugin/selftests/templates/head.html");
        response.getWriter().printf(template, getNumberOfSelftests());
    }

    private int getNumberOfSelftests() {
        return getSelftestReferences().size();
    }

    private List<SelftestReference> getSelftestReferences() {
        return this.selfTestRegistrar.getSelftestReferences();
    }

    public void setSelfTestRegistrar(SelfTestRegistrar registrar) {
        this.selfTestRegistrar = registrar;
    }
}
