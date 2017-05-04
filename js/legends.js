(function ($) {
    var legendNo = 1;
    $(".listing").find(".legend").each(function (idx, elem) {
        elem.textContent = "Listing " + (idx + 1) + ": " + elem.textContent;
    });
})(jQuery);