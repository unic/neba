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
package io.neba.core.logviewer;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Bridges non-websocket servlets with websockets. Handles all requests using the {@link TailSocket}.
 *
 * @author Olaf Otto
 */
@Service
@Scope("prototype")
public class TailServlet extends WebSocketServlet {
    @Autowired
    private LogFiles logFiles;

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(SECONDS.toMillis(30));
        factory.setCreator((servletUpgradeRequest, servletUpgradeResponse) -> new TailSocket(logFiles));
    }
}
