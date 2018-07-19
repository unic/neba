(function () {
    var issueLists = document.getElementsByClassName("issue-list");
    Array.prototype.forEach.call(issueLists, function (element) {
        var milestone = element.getAttribute("data-milestone");
        var labels = element.getAttribute("data-labels");

        var oReq = new XMLHttpRequest();
        oReq.addEventListener("load", function (_) {
            var issues = JSON.parse(oReq.responseText);

            if (!issues.length) {
                element.innerHTML = "<li>None</li>";
                return;
            }
            issues.forEach(function (issue) {
                element.innerHTML += '<li><a href="' + issue.html_url + '">#' + issue.number + ': ' + issue.title + '</a></li>';
            });

        });
        oReq.open("GET", "https://api.github.com/repos/unic/neba/issues?state=closed&labels=" + labels + "&milestone=" + milestone);
        oReq.send();
    });
})();