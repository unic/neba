(function () {
    var issueLists = document.getElementsByClassName("issue-list");
    Array.prototype.forEach.call(issueLists, function (element) {
        var milestone = element.getAttribute("data-milestone");
        var labels = element.getAttribute("data-labels");

        fetch("https://api.github.com/repos/unic/neba/issues?state=closed&labels=" + labels + "&milestone=" + milestone)
            .then(response => response.json())
            .then(issues => {
                if (!issues.length) {
                    element.innerHTML = "<li>None</li>";
                    return;
                }
                issues.forEach(function (issue) {
                    element.innerHTML += '<li><a href="' + issue.html_url + '">#' + issue.number + ': ' + issue.title + '</a></li>';
                });
            });
    });
})();