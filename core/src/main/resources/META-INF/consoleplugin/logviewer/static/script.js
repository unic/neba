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

	updateErrorLevelFilters();
	adjustTailWindowToScreenHeight();
	observeFollowMode();

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

});
