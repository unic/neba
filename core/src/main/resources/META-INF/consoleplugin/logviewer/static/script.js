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
                global : false,
                error: function() {
                    if (++ failures > 10) {
                        window.clearInterval(timerId);
                    }
                },
                success: function(data) {
                    $("#serverTime").text(data);
                }
            });
        }, 1000);
    })();

    /**
     * Enable chosen-select on the logfile dropdown.
     */
    $(".chosen-select").chosen({width : "16em"});

    var KEY_ENTER = 13,
        KEY_A = 65,
        NEWLINE = /\r?\n/,
        LINES_PER_MB = 14000, // approximate number of log viewer lines per MB log data.
        textDecoder = new TextDecoder("UTF-8"),
        tailSocket,
        tailDomNode = document.getElementById("tail"),
        focusedViewDomNode = document.getElementById("focusedView"),
        $logfile = $("#logfile"),
        $logfiles = $logfile.children().clone(),
        $amount = $("#amount"),
        $followButton = $("#followButton"),
        $hideRotated = $("#hideRotated"),
        $downloadButton = $("#downloadButton"),
        $focusOnErrorsButton = $("#focusOnErrors"),
        $focusOnErrorsCount = $("#numberOfDetectedErrors"),
        $grep = $("#grep");

    /**
     * Represents the Tail views (tail and error focused) and the
     * related operations.
     */
    var Tail = {
        /**
         * The known types of logfiles.
         */
        LogType: Object.freeze({
            ERROR: 0,
            REQUEST: 1,
            ACCESS: 2
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
         * Whether the error focused view is active.
         */
        errorFocused: false,

        /**
         * The currently open error section, if any.
         */
        errorSection: undefined,

        /**
         * The nodes representing error sections.
         */
        errorSectionNodes: [],

        /**
         * Event listeners to be notified when an error is added or an error section is updated.
         */
        errorUpdateListeners: [],

        /**
         * Event listeners to be notified when an new error section is added.
         */
        newErrorListeners: [],

        followMode: false,

        numberOfErrors: function () {
            return this.errorSectionNodes.length;
        },

        onNewError: function (callback) {
            this.newErrorListeners.push(callback);
        },

        notifyErrorUpdateListeners: function () {
            var section = this.errorSection;
            this.errorUpdateListeners.forEach(function (listener) {
                listener(section);
            });
        },

        notifyNewErrorListeners: function () {
            var section = this.errorSection;
            this.newErrorListeners.forEach(function (listener) {
                listener(section)
            });
        },

        /**
         * Clears the tail and re-sets any associated state.
         */
        clear: function () {
            tailDomNode.innerHTML = "";
            focusedViewDomNode.innerHTML = "";
            this.buffer = "";
            this.errorSection = undefined;
            this.errorSectionNodes = [];
            if (this.errorFocused) {
                this.toggleErrorFocus();
            }
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

            function linkRequestLog(match, linkAttribute) {
                var link = document.createElement("a"),
                    div = document.createElement("div");

                link.textContent = '[' + match[2] + ']';
                link.setAttribute(linkAttribute.name, linkAttribute.value);

                div.appendChild(document.createTextNode(match[1]));
                div.appendChild(link);
                div.appendChild(document.createTextNode(match[3]));
                div.appendChild(document.createElement("br"));
                return div;
            }

            function requestStartPattern() {
                return /(.* )\[([0-9]+)]( -> (GET|POST|PUT|HEAD|DELETE) .*)/g;
            }

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

                if (this.logType === Tail.LogType.ERROR || this.logType === undefined) {

                    // An error statement was detected before and is not yet finished
                    if (this.errorSection) {
                        var firstChar = textNode.nodeValue.charAt(0);
                        // The first character is a tab or not a number -> consider it part of a stack trace.
                        if (firstChar === '\t' || (firstChar * 0) !== 0) {
                            // Add the node to the existing error section
                            this.errorSection.appendChild(textNode);
                            this.errorSection.appendChild(document.createElement("br"));
                            // The error section might be hidden as it did not yet contain a match for the grep expression.
                            // Make it visible in case the grep expression  matches after new data is added.
                            this.filterExpression && this.errorSection.style.display === "none" && this.filterExpression.test(this.errorSection.textContent) && (this.errorSection.style.display = "");
                            this.updateErrorFocusedView();
                            this.notifyErrorUpdateListeners();
                            continue;
                        }
                        // The text is not part of the current error section -> end the error section
                        this.errorSection = undefined;
                    }

                    // An error is detected.
                    if (textNode.nodeValue.indexOf("*ERROR*") !== -1) {
                        this.logType = Tail.LogType.ERROR;
                        // Create a new div that will hold all elements of the logged error, including stack traces
                        this.errorSection = document.createElement("div");
                        this.errorSection.className = "error";
                        this.errorSectionNodes.push(this.errorSection);
                        // Add the current text to the newly created error section
                        this.errorSection.appendChild(textNode);
                        this.errorSection.appendChild(document.createElement("br"));
                        // Add the newly created error section to the log view
                        this.addToTail(this.errorSection);
                        this.addErrorToErrorFocusedView();
                        this.notifyNewErrorListeners();
                        continue;
                    }
                }

                if (this.logType === Tail.LogType.REQUEST || this.logType === undefined) {
                    var match = requestStartPattern().exec(textNode.nodeValue);
                    if (match != null) {
                        this.logType = Tail.LogType.REQUEST;
                        this.addToTail(linkRequestLog(match, {name : "href", value: '#r' + match[2]}));
                        continue;
                    }

                    match = requestEndPattern().exec(textNode.nodeValue);
                    if (match != null) {
                        this.logType = Tail.LogType.REQUEST;
                        this.addToTail(linkRequestLog(match, {name : "name", value: '#r' + match[2]}));
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
            (tailDomNode.scrollTop = tailDomNode.scrollHeight) &&
            (focusedViewDomNode.scrollTop = focusedViewDomNode.scrollHeight);
        },

        /**
         * Whether to follow the log file additions.
         * @returns {boolean} whether follow mode is on.
         */
        toggleFollowMode: function () {
            Tail.followMode = !Tail.followMode;
            if (Tail.followMode) {
                // If "focus on errors" is on, deactivate it since we are re-loading the entire view.
                if (Tail.errorFocused) {
                    Tail.toggleErrorFocus();
                    inactiveStyle($focusOnErrorsButton);
                }
                Tail.clear();
                followSelectedLogFile();
            } else {
                stopFollowing();
            }
            Tail.followUp();
            return Tail.followMode;
        },

        /**
         * Whether to only show errors.
         * @returns {boolean} whether only show errors is on.
         */
        toggleErrorFocus: function () {
            focusedViewDomNode.innerHTML = "";

            Tail.errorFocused = !Tail.errorFocused;
            if (Tail.errorFocused) {
                Tail.errorSectionNodes.forEach(function (node) {
                    focusedViewDomNode.appendChild(node.cloneNode(true));
                });
                Tail.followUp();
                focusedViewDomNode.style.zIndex = 0;
            } else {
                focusedViewDomNode.style.zIndex = -1;
            }

            return Tail.errorFocused;
        },

        /**
         * Updates the error focused view with a piece of text belonging to the last
         * error section.
         */
        updateErrorFocusedView: function () {
            if (Tail.errorFocused) {
                var oldNode = focusedViewDomNode.childNodes[focusedViewDomNode.childNodes.length - 1];
                focusedViewDomNode.replaceChild(Tail.errorSection.cloneNode(true), oldNode);
            }
        },

        /**
         * Updates the error focused view with a new error section.
         */
        addErrorToErrorFocusedView: function () {
            if (Tail.errorFocused) {
                focusedViewDomNode.appendChild(Tail.errorSection.cloneNode(true));
            }
        },

        /**
         * @returns {Element} the DOM node representing the current view on the log data, i.e.
         *           the tail or focused view node.
         */
        view: function () {
            return Tail.errorFocused ? focusedViewDomNode : tailDomNode;
        }
    };

    /**
     * Show and / or update the error focus button.
     */
    Tail.onNewError(function () {
        if (Tail.numberOfErrors() === 1) {
            $focusOnErrorsCount.fadeIn();
        }
        $focusOnErrorsCount.html(Tail.numberOfErrors());
        if ($focusOnErrorsButton.inAnimation) {
            return;
        }
        $focusOnErrorsButton.inAnimation = true;
        $focusOnErrorsButton.effect("highlight", {color: "#883B26"}, 600, function () {
            $focusOnErrorsButton.inAnimation = false;
        })
    });

    adjustViewsToScreenHeight();
    filterLogFiles();
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

        $amount.change(function() {
            Tail.scrollBackBuffer = (parseFloat($amount.val()) || 0.1) * LINES_PER_MB;
        });

        $followButton.click(function () {
            Tail.toggleFollowMode() ? activeStyle($followButton) : inactiveStyle($followButton);
            return false;
        });

        $downloadButton.click(function () {
            window.location.href = pluginRoot + "/download";
        });

        $amount.keydown(function (event) {
            if (event.which === KEY_ENTER) {
                logfileParametersChanged();
                return false;
            }
            return true;
        });

        $hideRotated.change(function () {
            filterLogFiles();
            return false;
        });

        $focusOnErrorsButton.click(function () {
            Tail.toggleErrorFocus() ? activeStyle($focusOnErrorsButton) : inactiveStyle($focusOnErrorsButton);
            return false;
        })
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
            opts[match[2]] = match[3];
        }

        opts.amount && (parseFloat(opts.amount) > 0) && $amount.val(opts.amount);

        if (opts.file) {
            // The log was not found in the non-rotated log files - perhaps it is in the rotated files?
            if ($hideRotated.is(":checked")) {
                var found = false;

                $logfile.find("option").each(function (_, v) {
                    if (v.value === opts.file) {
                        found = true;
                    }
                });

                // The logfile is in the list on non-rotated logfiles, done.
                if (!found) {
                    $logfiles.each(function (_, v) {
                        if (v.value === opts.file) {
                            found = true;
                        }
                    });

                    // The logfile is in the list on rotated logfiles, update the selection.
                    if (found) {
                        $hideRotated.prop("checked", false);
                        filterLogFiles();
                        $logfile.val(opts.file);
                        $logfile.trigger("chosen:updated");
                    }
                }
            }

            tailDomNode.innerHTML = "";
            tailSelectedLogFile();
        }

        opts.grep && $grep.val(opts.grep) && Tail.updateFilterExpressionFromUserInput();
    }

    /**
     * Re-loads the page with the selected parameters.
     */
    function logfileParametersChanged() {
        var file = $logfile.val(),
            amount = $amount.val(),
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

        tailSocket.send("tail:" + amount + 'mb:' + file);
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

        tailSocket.send("follow:" + amount + 'mb:' + file);
    }

    /**
     * Stops following any logfile.
     */
    function stopFollowing() {
        tailSocket.send("stop");
    }

    function adjustViewsToScreenHeight() {
        tailDomNode.style.height = (screen.height * 0.65) + "px";
        focusedViewDomNode.style.height = tailDomNode.style.height;
    }

    function filterLogFiles() {
        // E.g. 2029-01-01.log
        var currentDateSuffix = new Date().toISOString().slice(0,10) + ".log";
        // E.g. some-log-2020-01-01.log
        var logFileWithDateSuffix = /^.+-[0-9]{4}-[0-9]{2}-[0-9]{2}\.log$/;

        var currentlySelectedFile = $logfile.val();

        $logfile.children().remove();

        $logfile.append(
            $hideRotated.is(":checked") ? $logfiles.filter("[value$='\\.log'],[value='']").filter(function(idx, elem) {
                return !logFileWithDateSuffix.test(elem.value) ||
                        elem.value.endsWith(currentDateSuffix)
            }) : $logfiles
        );

        if (currentlySelectedFile) $logfile.val(currentlySelectedFile);

        $logfile.trigger("chosen:updated");
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
                range.selectNode(Tail.view());
                var selection = window.getSelection();
                selection.removeAllRanges();
                selection.addRange(range);
            }
        });
    }
});
