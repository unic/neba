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
    var KEY_ENTER = 13,
        KEY_A = 65,
        FILENAME_AMOUNT_SEPARATOR = '::',
        HASH_PATTERN = /#(.*)::([0-9]+)(kb|mb)/,
        textDecoder = new TextDecoder("UTF-8"),
        tailSocket,
        tailDomNode = document.getElementById("tail"),
        focusedViewDomNode = document.getElementById("focusedView"),
        $logfile = $("#logfile"),
        $logfiles = $logfile.children().clone(),
        $amount = $("#amount"),
        $unit = $("#unit"),
        $followButton = $("#followButton"),
        $hideRotated = $("#hideRotated"),
        $downloadButton = $("#downloadButton"),
        $focusOnErrorsButton = $("#focusOnErrors"),
        $focusOnErrorsCount = $("#numberOfDetectedErrors");

    /**
     * Represents the Tail views (tail and error focused) and the
     * related operations.
     */
    var Tail = {
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
        reset: function () {
            tailDomNode.innerHTML = "";
            focusedViewDomNode.innerHTML = "";
            this.buffer = "";
            this.errorSection = undefined;
            this.errorSectionNodes = [];

            if (this.followMode) {
                this.toggleFollowMode();
            }

            if (this.errorFocused) {
                this.toggleErrorFocus();
            }
        },

        /**
         * Send meta-info about the tail process itself to the tail, e.g. a notification
         * when log file rotation occurs.
         */
        info: function (text) {
            tailDomNode.appendChild(
                document.createTextNode('\r\n ----------------- ' + text + ' ----------------- \r\n\r\n')
            );
            this.follow();
        },

        /**
         * Add a message to the tail.
         */
        add: function (text) {
            var lines = (this.buffer + text).split('\n');

            for (var i = 0; i < lines.length - 1; ++i) {
                /**
                 * Convert each line of text into a new text DOM node. This non-normalized approach
                 * (many small text nodes instead of one large text node) has many advantages: It significantly increases performance
                 * as it allows the browser to only render the text nodes currently in view and allows to easily take single lines
                 * of text and highlight them, e.g. for highlighting errors.
                 *
                 * @type {Text}
                 */
                var textNode = document.createTextNode(lines[i] + '\n');

                // An error statement was detected before and is not yet finished
                if (this.errorSection) {
                    var firstChar = textNode.data.charAt(0);
                    // The first character is a tab or not a number -> consider it part of a stack trace.
                    if (firstChar === '\t' || (firstChar * 0) !== 0) {
                        // Add the node to the existing error section
                        this.errorSection.appendChild(textNode);
                        this.updateErrorFocusedView();
                        this.notifyErrorUpdateListeners();
                        continue;
                    }
                    // The text is not part of the current error section -> end the error section
                    this.errorSection = undefined;
                }

                // An error is detected.
                if (textNode.data.indexOf("*ERROR*") !== -1) {
                    // Create a new div that will hold all elements of the logged error, including stack traces
                    this.errorSection = document.createElement("div");
                    this.errorSection.className = "error";
                    this.errorSectionNodes.push(this.errorSection);

                    // Add the newly created error section to the log view
                    tailDomNode.appendChild(this.errorSection);
                    // Add the current text to the newly created error section
                    this.errorSection.appendChild(textNode);
                    this.addErrorToErrorFocusedView();
                    this.notifyNewErrorListeners();
                    continue;
                }

                // No error section is opened -> simply append the line
                tailDomNode.appendChild(textNode);
            }

            if (lines[lines.length - 1]) {
                this.buffer = lines[lines.length - 1];
            } else {
                this.buffer = "";
            }

            this.follow();
        },

        /**
         * When follow mode is on, scroll to the bottom of the views.
         */
        follow: function () {
            this.followMode &&
            (tailDomNode.scrollTop = tailDomNode.scrollHeight) &&
            (focusedViewDomNode.scrollTop = focusedViewDomNode.scrollHeight);
        },

        /**
         * Whether to follow the log file additions.
         * @returns {boolean} whether follow mode is on.
         */
        toggleFollowMode: function () {
            this.followMode = !this.followMode;
            this.follow();
            return this.followMode;
        },

        /**
         * Whether to only show errors.
         * @returns {boolean} whether only show errors is on.
         */
        toggleErrorFocus: function () {
            focusedViewDomNode.innerHTML = "";

            this.errorFocused = !this.errorFocused;
            if (this.errorFocused) {
                this.errorSectionNodes.forEach(function (node) {
                    focusedViewDomNode.appendChild(node.cloneNode(true));
                });
                this.follow();
                focusedViewDomNode.style.zIndex = 1001;
            } else {
                focusedViewDomNode.style.zIndex = -1;
            }

            return this.errorFocused;
        },

        /**
         * Updates the error focused view with a piece of text belonging to the last
         * error section.
         */
        updateErrorFocusedView: function () {
            if (this.errorFocused) {
                var oldNode = focusedViewDomNode.childNodes[focusedViewDomNode.childNodes.length - 1];
                focusedViewDomNode.replaceChild(this.errorSection.cloneNode(true), oldNode);
            }
        },

        /**
         * Updates the error focused view with a new error section.
         */
        addErrorToErrorFocusedView: function () {
            if (this.errorFocused) {
                focusedViewDomNode.appendChild(this.errorSection.cloneNode(true));
            }
        },

        /**
         * @returns {Element} the DOM node representing the current view on the log data, i.e.
         *           the tail or focused view node.
         */
        view : function () {
            return this.errorFocused ? focusedViewDomNode : tailDomNode;
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
    toggleRotatedLogfiles();
    restrictCopyAllToLogView();

    try {
        tailSocket = createSocket();
        tailSocket.onopen = function () {
            initUiBehavior();
            selectLogfileAndAmountFromHash();
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
                tailSelectedLogFile();
            }
            return false;
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
                tailSelectedLogFile();
                return false;
            }
            return true;
        });

        $unit.change(function() {
            tailSelectedLogFile();
            return false;
        });

        $hideRotated.change(function () {
            toggleRotatedLogfiles();
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
        var socket = new WebSocket((window.location.protocol === "https" ? "wss" : "ws") + "://" + window.location.host + "/system/console/logviewer/tail");

        socket.onclose = function () {
            Tail.info("Connection to server lost. Trying to reconnect ...");
            window.setTimeout(function () {
                try {
                    tailSocket = createSocket();
                    tailSocket.onopen = tailSelectedLogFile;
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
     * Load a pre-selected logfile from the hash, e.g. #/my/file::100kb
     */
    function selectLogfileAndAmountFromHash() {
        var hash = document.location.hash;
        if (!hash) {
            return;
        }

        var match = HASH_PATTERN.exec(hash);
        if (match === null) {
            return;
        }

        var file = match[1],
            amount = match[2],
            unit = match[3];

        $amount.val(amount);

        $unit.find("option").each(function (_, v) {
            if (v.value === unit) {
                v.selected = true
            }
        });

        // The log was not found in the non-rotated log files - perhaps it is amongs the rotated files?
        if (!selectLogFile(file) && $hideRotated.is(":checked")) {
            var found;
            $logfiles.each(function (_, v) {
                if (v.value === file) {
                    found = true;
                }
            });

            if (found) {
                $hideRotated.click();
                selectLogFile(file);
            }
        }

        tailSelectedLogFile();
    }

    function selectLogFile(file) {
        var found = false;
        $logfile.find("option").each(function (_, v) {
            if (v.value === file) {
                v.selected = true;
                found = true;
            }
        });
        return found;
    }

    /**
     * Clears UI internal state, such as the tail data and error count.
     */
    function resetUi() {
        Tail.reset();
        $focusOnErrorsButton.css("display", "none");
        $focusOnErrorsCount.html("");
        inactiveStyle($followButton);
        inactiveStyle($focusOnErrorsButton);
        focusedViewDomNode.innerHTML = "";
    }

    /**
     * Starts tailing the selected log file.
     */
    function tailSelectedLogFile() {
        var file = $logfile.val(),
            amount = $amount.val(),
            unit = $unit.val(),
            href = document.location.href,
            anchorPos = href.indexOf("#"),
            endPos = anchorPos === -1 ? href.length : anchorPos;

        if (!(file && amount)) {
            return;
        }

        resetUi();
        document.location.href = href.substr(0, endPos) + "#" + file + FILENAME_AMOUNT_SEPARATOR + amount + unit;
        tailSocket.send("tail:" + amount + unit + ':' + file);
    }

    function adjustViewsToScreenHeight() {
        tailDomNode.style.height = (screen.height * 0.65) + "px";
        focusedViewDomNode.style.height = tailDomNode.style.height;
    }

    function toggleRotatedLogfiles() {
        $logfile.children().remove();
        $logfile.append(
            $hideRotated.is(":checked") ? $logfiles.filter("[value$='\\.log'],[value='']") : $logfiles
        );
    }

    /**
     * Intercepts ctrl + a to create a text selection exclusively spanning the current
     * log data view.
     */
    function restrictCopyAllToLogView() {
        $(document).keydown(function(e) {
            if (e.keyCode == KEY_A && e.ctrlKey) {
                e.preventDefault();
                var range = document.createRange();
                range.selectNode(Tail.view());
                window.getSelection().addRange(range);
            }
        });
    }
});
