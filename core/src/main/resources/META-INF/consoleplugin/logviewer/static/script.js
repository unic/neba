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
$(function() {
	var ENTER = 13;
	var timeout = null;

    $("#logfile").change(updateSelectedLogExcerpt);
    $("#reloadButton").click(updateSelectedLogExcerpt);
    $("#followButton").click(toggleFollowMode);
    $("#downloadButton").click(function() {
        window.location.href = pluginRoot + "/download";
    });
    $("#showOptions").find("input").click(updateErrorLevelFilters);
    $("#numberOfLines").keydown(function(event) {
        if (event.which == ENTER) {
            updateSelectedLogExcerpt();
            return false;
        }
		return true;
    });

    $("#hideRotated").change(showOrHideRotatedLogfiles);

	updateErrorLevelFilters();
	adjustTailWindowToScreenHeight();
	observeFollowMode();
    showOrHideRotatedLogfiles();

	// Load a pre-selected logfile (#file:numberOfLines)
	var href = document.location.href;
	var stateHash = href.indexOf("#");
	if (stateHash != -1) {
		var spec = href.substr(stateHash + 1, href.length - stateHash).split(":");
		var file = spec[0];
		var numberOfLines = spec[1];
		$("#numberOfLines").val(numberOfLines);
		$("#logfile").find("option[value = '" + file +  "']").attr("selected", "true");
		updateSelectedLogExcerpt();
	}

	function toggleFollowMode() {
		if($("#followButton").toggleClass("ui-state-active").hasClass("ui-state-active")) {
            $("#tail").each(function(idx, elem) {
                $(elem).scrollTop(elem.scrollHeight);
            });
        }
		observeFollowMode();
		return false;
	}

	function observeFollowMode() {
		clearTimeout(timeout);
		if($("#followButton").hasClass("ui-state-active")) {
			updateSelectedLogExcerpt(function() {
				timeout = setTimeout(observeFollowMode, 1000);
			});
		} else {
			timeout = setTimeout(observeFollowMode, 1000);
		}
	}

	function updateSelectedLogExcerpt(callback) {
	    var file = $("#logfile").val();
	    var numberOfLines = $("#numberOfLines").val();
		var href = document.location.href;
		var anchorPos = href.indexOf("#");
        var endPos = anchorPos == -1 ? href.length : anchorPos;
		document.location.href =  href.substr(0, endPos) + "#" + file + ":" + numberOfLines;

	    $.ajax({
	        url: pluginRoot + "/tail/" + numberOfLines + "/" + file,
	        cache: false,
	        success: function(data) {
                var scrollPercentage = NaN;
                var $tail = $("#tail");
                if ($tail.size() == 1) {
                    scrollPercentage = $tail.scrollTop() / ($tail[0].scrollHeight - $tail.height());
                }
	           $("#tailwrapper").html(data);
	           if (isFinite(scrollPercentage)) {
                   $tail = $("#tail");
                   $tail.scrollTop(($tail[0].scrollHeight - $tail.height()) * scrollPercentage);
	           }
	           updateErrorLevelFilters();
	           if (callback && typeof callback == "function") {
	        	   callback();
	           }
	        }
	    });
	    return false;
	}

	function updateErrorLevelFilters() {
	    var $visible = {
	        "ERROR": false,
	        "WARN": false,
	        "INFO": false,
	        "DEBUG": false,
	        "TRACE": false
	    };

	    $("#showOptions").find("input:checked").each(function(idx, elem) {
	        $visible[elem.value] = true;
	    });

	    $("#tail").find("div").each(function(idx, elem) {
	        elem.style.display = $visible[elem.className] ? "block" : "none";
	    });
	}

	function adjustTailWindowToScreenHeight() {
		$("#tail").css("height", screen.height * 0.65)
	}

    function showOrHideRotatedLogfiles() {
        var display = $("#hideRotated").is(":checked") ? "none" : "block";
        $("#logfile").find("option").not("[value$='.log']").css("display", display)
    }
});
