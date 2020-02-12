Node.prototype.tag = function(name) {
    return this.getElementsByTagName(name)[0];
};

var releases = undefined;

upgradePathForm.onsubmit = function () {
    showUpgradePath();
    return false;
};

function showUpgradePath() {
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
    while (requiredChanges.lastChild) {
        requiredChanges.removeChild(requiredChanges.lastChild);
    }

    var from = fromVersion.options[fromVersion.selectedIndex].value,
        to =   toVersion.options[toVersion.selectedIndex].value,
        include = false;

    for (i = 0; i < releases.length; ++i) {
        let name = releases[i].tag("name").textContent;
        if (include && releases[i].tag("requiresChange").textContent === "true") {
            var section = document.createElement("section");
            var header = document.createElement("header");
            var h2 = document.createElement("h2");
            var a = document.createElement("a");
            a.setAttribute("href", releases[i].tag("url").textContent);
            a.textContent = "Complete release notes of " + name;
            section.setAttribute("class", "info");
            h2.textContent = "Changes introduced in " + name;
            header.appendChild(h2);
            header.appendChild(a);
            section.appendChild(header);

            var div = document.createElement("div");
            div.innerHTML = releases[i].tag("changeDescription").textContent;
            section.appendChild(div);
            requiredChanges.appendChild(section);
        }
        if (name === to) {
            break;
        }
        if (name === from) {
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
