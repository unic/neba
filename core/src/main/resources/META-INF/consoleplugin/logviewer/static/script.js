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
$(function () {
    /**
     * Polyfills
     */
    (function(){
        if (!String.prototype.endsWith) {
            String.prototype.endsWith = function(search, this_len) {
                if (this_len === undefined || this_len > this.length) {
                    this_len = this.length;
                }
                return this.substring(this_len - search.length, this_len) === search;
            };
        }

        if (window.NodeList && !NodeList.prototype.forEach) {
            NodeList.prototype.forEach = Array.prototype.forEach;
        }
    })();

    /**
     * Continuously update the server time view with the actual backend time.
     */
    (function() {
        var failures = 0;
        var timerId = window.setInterval(function() {
            $.ajax({
                url: "logviewer/serverTime",
                dataType: "json",
                global : false,
                error: function() {
                    if (++ failures > 10) {
                        // Bail after 10 successive error responses from the server.
                        window.clearInterval(timerId);
                    }
                },
                success: function(data) {
                    failures = 0;
                    $("#serverTime").text(data.time);
                }
            });
        }, 1000);
    })();

    var KEY_ENTER = 13,
        KEY_A = 65,
        NEWLINE = /\r?\n/,
        LINES_PER_MB = 14000, // approximate number of log viewer lines per MB log data.
        PATTERN_SECTION_START = /\*(TRACE|DEBUG|INFO|WARN|ERROR)\*/,
        textDecoder = new TextDecoder("UTF-8"),
        tailSocket,
        tailDomNode = document.getElementById("tail"),
        $logfile = $("#logfile"),
        $amount = $("#amount"),
        $followButton = $("#followButton"),
        $downloadCurrentLogfile = $("#downloadCurrentLogfile"),
        $downloadAllLogfiles = $("#downloadAllLogfiles"),
        $grep = $("#grep");

    /**
     * Enable chosen-select on the logfile dropdown.
     */
    $logfile.chosen({width : "16em"});

    /**
     * Enable download behaviors for the download buttons.
     */
    $downloadCurrentLogfile.click(function() {
        if ($logfile.val()) {
            window.open("logviewer/download?file=" + $logfile.val());
        }
        return false;
    });
    $downloadAllLogfiles.click(function() {
        window.open("logviewer/download");
        return false;
    });

    /**
     * Represents the Tail view and the related operations.
     */
    var Tail = {
        /**
         * The known types of logfiles.
         */
        LogType: Object.freeze({
            ERROR_LOG: 0,
            REQUEST_LOG: 1,
            ACCESS_LOG: 2
        }),

        /**
         * The regular expression to apply to all log entries (hide elements not matching)
         */
        filterExpression : undefined,

        /**
         * The amount of entries in the tail view to keep.
         */
        scrollBackBuffer : (parseFloat($amount.val()) || 0.1) * LINES_PER_MB,

        /**
         * The currently tailed log file type.
         */
        logType: undefined,

        /**
         * The buffered remained of a line not yet terminated.
         */
        buffer: "",

        /**
         * The currently open error section, if any.
         */
        currentSection: undefined,

        followMode: false,

        /**
         * Clears the tail and re-sets any associated state.
         */
        clear: function () {
            tailDomNode.innerHTML = "";
            this.buffer = "";
            this.currentSection = undefined;
        },

        /**
         * Send meta-info about the tail process itself to the tail, e.g. a notification
         * when log file rotation occurs.
         */
        info: function (text) {
            tailDomNode.appendChild(document.createElement("br"));
            tailDomNode.appendChild(
                document.createTextNode(' ----------------- ' + text + ' ----------------- ')
            );
            tailDomNode.appendChild(document.createElement("br"));
            tailDomNode.appendChild(document.createElement("br"));
            this.followUp();
        },

        /**
         * Add a message to the tail.
         */
        add: function (text) {

            function linkRequestLog(match, config) {
                var link = document.createElement("a"),
                    div = document.createElement("div");

                link.textContent = '[' + match[2] + ']';
                config.attributes.forEach(function(attribute) {
                    link.setAttribute(attribute.name, attribute.value);
                });
                div.appendChild(document.createTextNode(match[1]));
                div.appendChild(link);
                div.appendChild(document.createTextNode(match[3]));
                div.appendChild(document.createElement("br"));
                return div;
            }

            /**
             * Regexp instances have state and must the be re-created upon each usage.
             */
            function requestStartPattern() {
                return /(.* )\[([0-9]+)]( -> (GET|POST|PUT|HEAD|DELETE) .*)/g;
            }

            /**
             * Regexp instances have state and must the be re-created upon each usage.
             */
            function requestEndPattern() {
                return /(.* )\[([0-9]+)]( <- [0-9]+ .*)/g;
            }

            var lines = (this.buffer + text).split(NEWLINE);


            for (var i = 0; i < lines.length - 1; ++i) {
                /**
                 * Convert each line of text into a new text DOM node. This non-normalized approach
                 * (many small text nodes instead of one large text node) has many advantages: It significantly increases performance
                 * as it allows the browser to only render the text nodes currently in view and allows to easily take single lines
                 * of text and highlight them, e.g. for highlighting errors.
                 *
                 * @type {Text}
                 */
                var textNode = document.createTextNode(lines[i]);

                // Enable section detection if we know we are in an error.log or the logfile type is not known.
                if (this.logType === Tail.LogType.ERROR_LOG || this.logType === undefined) {

                    // An interrelated entry, e.g. an error stack trace, was detected before and is not yet finished
                    if (this.currentSection) {
                        var firstChar = textNode.nodeValue.charAt(0);
                        // The first character is a tab or not a number -> consider it part of a stack trace.
                        if (firstChar === '\t' || (firstChar * 0) !== 0) {
                            // Add the node to the existing section
                            this.currentSection.appendChild(textNode);
                            this.currentSection.appendChild(document.createElement("br"));
                            // The section might be hidden as it did not yet contain a match for the grep expression.
                            // Make it visible in case the grep expression  matches after new data is added.
                            this.filterExpression &&
                                this.currentSection.style.display === "none" &&
                                this.filterExpression.test(this.currentSection.textContent) &&
                                (this.currentSection.style.display = "");
                            continue;
                        }
                        // The text is not part of the current section -> end the section
                        this.currentSection = undefined;
                    }

                    // A new interrelated section is detected is detected.
                    var match = PATTERN_SECTION_START.exec(textNode.nodeValue);
                    if (match !== null) {
                        this.logType = Tail.LogType.ERROR_LOG;
                        // Create a new div that will hold all elements of the logged entry, including stack traces
                        this.currentSection = document.createElement("div");
                        this.currentSection.className = match[1].toLowerCase();
                        // Add the current text to the newly created section
                        this.currentSection.appendChild(textNode);
                        this.currentSection.appendChild(document.createElement("br"));
                        // Add the newly created section to the log view
                        this.addToTail(this.currentSection);
                        continue;
                    }
                }

                if (this.logType === Tail.LogType.REQUEST_LOG || this.logType === undefined) {
                    var requestStartMatch = requestStartPattern().exec(textNode.nodeValue);
                    if (requestStartMatch != null) {
                        this.logType = Tail.LogType.REQUEST_LOG;
                        var id = requestStartMatch[2];
                        var logLine = linkRequestLog(requestStartMatch, { attributes: [
                                {name : "href", value: '#res' + id},
                                {name : "name", value: 'req' + id}
                            ]});
                        logLine.id = 'req' + id;
                        logLine.nebaRequestId = 'res' + id;
                        logLine.onmouseover = function () {
                            var responseLine = document.getElementById(this.nebaRequestId);
                            this.title = responseLine ? responseLine.textContent : "No response for this request found in the current log excerpt.";
                        };

                        this.addToTail(logLine);
                        continue;
                    }

                    var requestEndMatch = requestEndPattern().exec(textNode.nodeValue);
                    if (requestEndMatch != null) {
                        this.logType = Tail.LogType.REQUEST_LOG;
                        var id = requestEndMatch[2];
                        var requestLine = document.getElementById('req' + id);
                        var logLine = linkRequestLog(requestEndMatch, { attributes: [
                                {name : "href", value: '#req' + id},
                                {name : "name", value: 'res' + id}
                            ]});
                        logLine.id = 'res' + id;
                        logLine.title = requestLine ? requestLine.textContent : "No request for this response found in the current log excerpt.";

                        this.addToTail(logLine);
                        continue;
                    }
                }

                // Simply append the line
                var div = document.createElement("div");
                div.appendChild(textNode);
                this.addToTail(div);
            }

            if (lines[lines.length - 1]) {
                this.buffer = lines[lines.length - 1];
            } else {
                this.buffer = "";
            }

            this.followUp();
        },

        /**
         * Adds a new node to the tail view. removes old nodes with regards to the
         * scroll back buffer and calculates the nodes visibility based on the grep regex.
         */
        addToTail: function(node) {
            Tail.filterExpression && node.style && (node.style.display = Tail.filterExpression.test(node.textContent) ? "" : "none");
            tailDomNode.appendChild(node);
            // Limit number of entries to scrollback buffer
            while (tailDomNode.childNodes.length > this.scrollBackBuffer) {
                tailDomNode.childNodes.item(0).remove()
            }
        },

        updateFilterExpressionFromUserInput : function() {
            if ($grep.val() === "") {
                Tail.filterExpression = undefined;
                $grep.css("border", "1px solid transparent")
                    .attr("title", "");
                Tail.showAllTailEntries();
                return;
            }

            try {
                Tail.filterExpression = new RegExp($grep.val());
                Tail.applyFilterExpression();
                $grep.css("border", "1px solid green")
                    .attr("title", "")
            } catch (e) {
                Tail.filterExpression = undefined;
                $grep.css("border", "1px solid red")
                    .attr("title", "Invalid regular expression: " + e.message);
            }
        },

        /**
         * Applies a new or changed filter expression to all nodes
         * of the tail view.
         */
        applyFilterExpression: function() {
            var expression = Tail.filterExpression;
            expression && tailDomNode.childNodes.forEach(function(node) {
                node.style && (node.style.display = expression.test(node.textContent) ? "" : "none");
            });
        },

        showAllTailEntries : function() {
            tailDomNode.childNodes.forEach(function(node) {
                node.style && (node.style.display = "");
            });
        },

        /**
         * When follow mode is on, scroll to the bottom of the views.
         */
        followUp: function () {
            Tail.followMode &&
            (tailDomNode.scrollTop = tailDomNode.scrollHeight);
        },

        /**
         * Whether to follow the log file additions.
         * @returns {boolean} whether follow mode is on.
         */
        toggleFollowMode: function () {
            Tail.followMode = !Tail.followMode;
            if (Tail.followMode) {
                Tail.clear();
                followSelectedLogFile();
            } else {
                stopFollowing();
            }
            Tail.followUp();
            return Tail.followMode;
        },

        updateScrollbackBufferSize: function() {
            Tail.scrollBackBuffer = (parseFloat($amount.val()) || 0.1) * LINES_PER_MB;
        }
    };

    adjustViewsToScreenHeight();
    restrictCopyAllToLogView();

    try {
        tailSocket = createSocket();
        tailSocket.onopen = function () {
            initUiBehavior();
            updateFromRequestParameters();
        }
    } catch (e) {
        console && console.log(e);
        Tail.info("Unable to open server connection: " + e.message)
    }

    /**
     * Binds the log viewer behavior to the UI elements (such as buttons) once
     * a websocket connection was successfully established.
     */
    function initUiBehavior() {
        $logfile.change(function () {
            if ($logfile.val()) {
                logfileParametersChanged();
            }
            return false;
        });

        $grep.keydown(function (event) {
            return event.which !== KEY_ENTER;
        });

        $grep.keyup(function() {
            // De-bounce handling: Do not apply the filter expression upon
            // every character that is being typed, but a certain amount of time after
            // the last key press
            $grep.timer && window.clearTimeout($grep.timer);
            $grep.timer = window.setTimeout(Tail.updateFilterExpressionFromUserInput, 300);
        });

        Tail.updateFilterExpressionFromUserInput();

        $amount.change(Tail.updateScrollbackBufferSize);

        $followButton.click(function () {
            Tail.toggleFollowMode() ? activeStyle($followButton) : inactiveStyle($followButton);
            return false;
        });

        $amount.keydown(function (event) {
            if (event.which === KEY_ENTER) {
                logfileParametersChanged();
                return false;
            }
            return true;
        });
    }

    function activeStyle($button) {
        $button.css("background", "palegreen").css("font-weight", "bold")
    }

    function inactiveStyle($button) {
        $button.css("background", "").css("font-weight", "")
    }

    /**
     * Creates a new websocket and initializes message and error handling.
     *
     * @returns {WebSocket}
     */
    function createSocket() {
        var socket = new WebSocket((window.location.protocol === "https:" ? "wss" : "ws") + "://" + window.location.host + "/system/console/logviewer/tail");

        socket.onclose = function () {
            Tail.info("Connection to server lost. Trying to reconnect ...");
            window.setTimeout(function () {
                try {
                    tailSocket = createSocket();
                    tailSocket.onopen = function() {
                        Tail.clear();
                        tailSelectedLogFile();
                    };
                } catch (e) {
                    console && console.log(e);
                    Tail.info("Unable to open server connection: " + e.message);
                }
            }, 2000);
        };

        socket.onmessage = function (event) {
            var data = event.data;
            if (data instanceof ArrayBuffer) {
                Tail.add(textDecoder.decode(event.data));
            } else if (typeof data === 'string') {
                if ("pong" === data) {
                    return;
                }
                Tail.info(data);
            } else if (console) {
                console.error("Unsupported data format of websocket response " + data + ".")
            }
        };

        socket.binaryType = "arraybuffer";

        window.setInterval(function () {
            socket.readyState === WebSocket.OPEN &&
            socket.send("ping")
        }, 1000);

        window.onunload = function () {
            if (socket) {
                socket.onclose = undefined;
                socket.close();
            }
        };

        return socket;
    }

    /**
     * Load selected logfile from the request parameters, e.g. ?file=/my/file&amount=100
     */
    function updateFromRequestParameters() {

        var queryString = document.location.search;
        if (!queryString) {
            return;
        }

        var opts = {};

        var requestParameterPattern = /([?&])([^=]+)=([^&?]*)/g,
            match;

        while ((match = requestParameterPattern.exec(queryString)) !== null) {
            opts[match[2]] = decodeURI(match[3]);
        }

        opts.amount && (parseFloat(opts.amount) > 0) && $amount.val(opts.amount) && Tail.updateScrollbackBufferSize();

        if (opts.file) {
            $logfile.children().each(function (_, v) {
                if (v.value === opts.file) {
                    $logfile.val(opts.file);
                    $logfile.trigger("chosen:updated");
                }
            });
        }

        tailDomNode.innerHTML = "";
        tailSelectedLogFile();

        opts.grep && $grep.val(opts.grep) && Tail.updateFilterExpressionFromUserInput();
    }

    /**
     * Re-loads the page with the selected parameters.
     */
    function logfileParametersChanged() {
        var file = $logfile.val(),
            amount = parseFloat($amount.val()),
            grep = $grep.val(),
            href = document.location.href,
            queryPos = Math.max(href.indexOf("?"), href.indexOf("#")),
            endPos = queryPos === -1 ? href.length : queryPos;

        if (!(file && amount)) {
            return;
        }

        document.location.href = href.substr(0, endPos) + "?file=" + file + '&amount=' + amount + "&grep=" + (grep || "");
    }

    /**
     * Starts tailing the selected log file.
     */
    function tailSelectedLogFile() {
        var file = $logfile.val(),
            amount = $amount.val();

        if (!(file && amount)) {
            return;
        }

        tailSocket.send("tail:" + parseFloat(amount) + 'mb:' + file);
    }

    /**
     * Starts following the selected log file.
     */
    function followSelectedLogFile() {
        var file = $logfile.val(),
            amount = $amount.val();

        if (!(file && amount)) {
            return;
        }

        tailSocket.send("follow:" + parseFloat(amount) + 'mb:' + file);
    }

    /**
     * Stops following any logfile.
     */
    function stopFollowing() {
        tailSocket.send("stop");
    }

    function adjustViewsToScreenHeight() {
        tailDomNode.style.height = (screen.height * 0.65) + "px";
    }

    /**
     * Intercepts ctrl + a to create a text selection exclusively spanning the current
     * log data view.
     */
    function restrictCopyAllToLogView() {
        $(document).keydown(function (e) {
            if (e.keyCode === KEY_A && e.ctrlKey) {
                e.preventDefault();
                var range = document.createRange();
                range.selectNode(tailDomNode);
                var selection = window.getSelection();
                selection.removeAllRanges();
                selection.addRange(range);
            }
        });
    }
});
