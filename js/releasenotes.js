(function ($) {
    $(".issue-list").each(function () {
        var $placeholder = $(this);
        var milestone = $placeholder.data("milestone"),
            labels = $placeholder.data("labels");

        $.ajax({
            url: "https://api.github.com/repos/unic/neba/issues",
            data: {
                "state" : "closed",
                "labels" : labels,
                "milestone" : milestone
            },
            success: function(issues) {
                issues.forEach(function (issue) {
                    $placeholder.append($('<li><a href="' + issue.url + '">#' + issue.number + ': ' + issue.title + '</a></li>'));
                });
            },
            dataType: "json"
        });
    });
})(jQuery);