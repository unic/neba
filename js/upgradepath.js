(function() {
    Node.prototype.tag = function(name) {
        return this.getElementsByTagName(name)[0].textContent;
    };

    function $(name) {
        return document.createElement(name);
    }

    this.releases = undefined;

    var handleSelection = function () {
        showUpgradePath();
        return false;
    };
    upgradePathForm.onsubmit = handleSelection;
    fromVersion.onchange = handleSelection;
    toVersion.onchange = handleSelection;

    function showUpgradePath() {
        while (requiredChanges.lastChild) {
            requiredChanges.removeChild(requiredChanges.lastChild);
        }

        if (!fromVersion.selectedIndex || !toVersion.selectedIndex) {
            return;
        }

        if (!releases) {
            loadUpgradePath();
        } else {
            renderUpgradePath();
        }
    }

    function renderUpgradePath() {

        var from = fromVersion.options[fromVersion.selectedIndex].value,
            to =   toVersion.options[toVersion.selectedIndex].value,
            include = false;

        for (i = 0; i < releases.length; ++i) {
            let versionName = releases[i].tag("name");
            if (include && releases[i].tag("requiresChange") === "true") {
                var section = $("section"),
                    header = $("header"),
                    h2 = $("h2"),
                    a = $("a"),
                    div = $("div");

                a.setAttribute("href", releases[i].tag("url"));
                a.textContent = "Complete release notes of " + versionName;
                h2.textContent = "Changes introduced in " + versionName;
                div.innerHTML = releases[i].tag("changeDescription");

                header.appendChild(h2);
                header.appendChild(a);

                section.appendChild(header);
                section.setAttribute("class", "info");
                section.appendChild(div);

                requiredChanges.appendChild(section);
            }
            if (versionName === to) {
                break;
            }
            if (versionName === from) {
                include = true;
            }
        }

        if (!requiredChanges.lastChild) {
            requiredChanges.innerHTML = "<section><header><h2>No changes are required when updating from " + from + " to " + to + ".</h2></header></section>";
        }
    }

    function loadUpgradePath() {
        var oReq = new XMLHttpRequest();
        oReq.addEventListener("load", function (_) {
            releases = oReq.responseXML.childNodes[0].childNodes;
            renderUpgradePath();
        });
        oReq.open("GET", "/releases.xml");
        oReq.send();
    }
})();
