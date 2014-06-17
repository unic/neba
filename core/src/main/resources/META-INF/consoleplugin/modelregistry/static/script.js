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
    var showOnlyUnresolved = false;
    var $table = $("#plugin_table");

    /**
     * @param source a unique identifier for the source altering the visibility.
     *        Used to asses the visibility alteration permissions later on.
     * @returns {Function}
     */
    function visibility(source) {
        return function(elem) {
            var domElement = elem[0];
            return {
                show : function () {
                    if (!domElement.hiddenBy || domElement.hiddenBy == source) {
                        elem.show();
                        domElement.hiddenBy = undefined;
                    }
                },

                hide : function () {
                    if (!domElement.hiddenBy) {
                        elem.hide();
                        domElement.hiddenBy = source;
                    }
                }
            };
        } ;
    }

    /**
     * Toggles the exclusive display of unresolvable resource types.
     */
    function toggleUnresolvedResourceTypes() {
        var toggle = visibility(toggleUnresolvedResourceTypes);
        showOnlyUnresolved = !showOnlyUnresolved;

        $("a.crxdelink").each(function(idx, elem) {
            var $tr = $(elem).closest("tr");
            showOnlyUnresolved ? toggle($tr).hide() : toggle($tr).show();
        });
        if (showOnlyUnresolved) {
            $("#unresolvedToggle").text("Show all mappings");
        } else {
            $("#unresolvedToggle").text("Only show mappings to unresolvable resource types");
        }

        $("#plugin_table").tablesorter();
    }

    /**
     * Hides all table rows representing resource types not in the
     * requested JSON array of resource model type names.
     */
    function filterTable()  {
        var toggle = visibility(filterTable);
        $.getJSON("modelregistry/api/filter", {"path": $("#fromResource").val(), "modelTypeName": $("#toType").val()}, function (typeNames) {
            $table.find("tr").each(function (idx, elem) {
                var $elem = $(elem);
                var attr = $elem.attr("data-modeltype");
                if (attr) {
                    $.inArray(attr, typeNames) != -1 ? toggle($elem).show() : toggle($elem).hide();
                }
            });
            $table.tablesorter();
        });
    }

    function filterTableOnReturn(event) {
        if (event.which == ENTER) {
            filterTable();
            return false;
        }
        return true;
    }

    $table.tablesorter();

    $("span.unresolved").each(function(idx, elem) {
	    $(elem).attr("title", "This resource type cannot be resolved to a node via the resource resolver")
	});
	$("a.crxdelink").each(function(idx, elem) {
	    $(elem).attr("title", "Open this resource type in CRXDE light")
	});
    $table.find("td").hover(function(elem) {
        $(this).parent().children().css("background-color", "#c0c0c0");
    }, function(elem) {
        $(this).parent().children().css("background-color", "");
    });

    // Initialize the static completion of all known resource model types.
    $.getJSON("modelregistry/api/modeltypes", function(modelTypeNames) {
        $("#toType").autocomplete({
            minLength: 2,
            source: modelTypeNames
        });
    });

    // Initialize the dynamic completion of the selected resource
    $("#fromResource").autocomplete({
        minLength: 1,
        source: function(request, response) {
            $.getJSON("modelregistry/api/resources", {path: $("#fromResource").val()}, function(resourceModelTypes) {
                response(resourceModelTypes);
            });
        }
    });

    $("#fromResource").keydown(filterTableOnReturn);
    $("#toType").keydown(filterTableOnReturn);

    $("#unresolvedToggle").click(toggleUnresolvedResourceTypes);
    $("#applyFilter").click(filterTable);
});