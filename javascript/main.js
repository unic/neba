jQuery(document).ready(function ($) {
    var color = "neba";
    var css_url = "css/colors/colors-" + color + ".css";
    $('head').append('<link rel="stylesheet" href="' + css_url + '" type="text/css" />');
});

function is_touch_device() {
    return !!('ontouchstart' in window);
}

/*--------------------------------------------------
 HEADER
 ---------------------------------------------------*/
$(document).ready(function () {
    var header_h = $("#header-wrapper").height() + 0;
    var menu_h = $("#menu").height();
    var speed = 500;
    var logo2_url = "images/logo-min.png";

    var scroll_critical = parseInt(header_h - menu_h);
    var window_y = 0;
    var menu_left_margin = parseInt($(".header").css("width")) - parseInt($("ul.menu").width());

    window_y = $(window).scrollTop();
    var $logo2_link = $("<a/>", {"href": "index.html"});
    var $logo2 = $("<img />", {"src": logo2_url, "class": "logo2"}).appendTo($logo2_link);


    if ((window_y > scroll_critical) && !(is_touch_device())) header_transform();

    function header_transform() {
        window_y = $(window).scrollTop();
        var $header = $("#header-wrapper");

        if (window_y > scroll_critical) {
            if (!($header.hasClass("fixed"))) {
                $header.hide();
                $("#wrapper").css("margin-top", header_h + "px");
                $header.addClass("fixed");
                $header.fadeIn(500);
                $logo2_link.fadeIn().appendTo(".header");
            }
        } else {
            if (($header.hasClass("fixed"))) {
                $header.fadeOut(500, function () {
                    $header.removeClass("fixed");
                    $("#wrapper").css("margin-top", "");
                    $header.fadeIn(300)
                });
                $logo2_link.fadeOut().remove();
            }
        }
    }

    $(window).scroll(function () {
        if (!(is_touch_device())) header_transform();
    })
});

/*--------------------------------------------------
 PRETTYPHOTO
 ---------------------------------------------------*/

$(window).load(function () {

    $('a[data-rel]').each(function () {
        $(this).attr('rel', $(this).data('rel'));
    });

    $("a[rel^='prettyPhoto']").prettyPhoto({
        animation_speed: 'fast', /* fast/slow/normal */
        slideshow: false, /* false OR interval time in ms */
        autoplay_slideshow: false, /* true/false */
        opacity: 0.80, /* Value between 0 and 1 */
        show_title: true, /* true/false */
        allow_resize: true, /* Resize the photos bigger than viewport. true/false */
        default_width: 500,
        default_height: 344,
        counter_separator_label: '/', /* The separator for the gallery counter 1 "of" 2 */
        theme: 'pp_default', /* light_rounded / dark_rounded / light_square / dark_square / facebook */
        hideflash: false, /* Hides all the flash object on a page, set to TRUE if flash appears over prettyPhoto */
        wmode: 'opaque', /* Set the flash wmode attribute */
        autoplay: true, /* Automatically start videos: True/False */
        modal: false, /* If set to true, only the close button will close the window */
        overlay_gallery: false, /* If set to true, a gallery will overlay the fullscreen image on mouse over */
        keyboard_shortcuts: true, /* Set to false if you open forms inside prettyPhoto */
        deeplinking: false
    });
});

/*--------------------------------------------------
 SLIDER
 ---------------------------------------------------*/

/*--------------------------------------------------
 ADDITIONAL CODE FOR HOME PAGE HEADER SLIDER
 ---------------------------------------------------*/
$(window).load(function () {
    $('#index-slider').flexslider({
        animation: "fade",
        slideDirection: "",
        slideshow: true,
        slideshowSpeed: 8000,
        animationDuration: 500,
        directionNav: true,
        controlNav: false
    });
});


/*--------------------------------------------------
 ADDITIONAL CODE FOR BLOG GALLERY
 ---------------------------------------------------*/
$(window).load(function () {
    $('#blog-slider').flexslider({
        animation: "fade",
        slideDirection: "",
        slideshow: true,
        slideshowSpeed: 5000,
        animationDuration: 500,
        directionNav: true,
        controlNav: false
    });
});


/*--------------------------------------------------
 ADDITIONAL CODE FOR BLOG GALLERY
 ---------------------------------------------------*/
$(window).load(function () {
    $('#portfolio-slider').flexslider({
        animation: "slide",
        slideDirection: "horizontal",
        slideshow: true,
        slideshowSpeed: 3500,
        animationDuration: 500,
        directionNav: true,
        controlNav: false
    });
});

/*--------------------------------------------------
 DROPDOWN MENU
 ---------------------------------------------------*/

/*--------------------------------------------------
 ADDITIONAL CODE FOR DROPDOWN MENU
 ---------------------------------------------------*/
jQuery(document).ready(function ($) {
    $('ul.menu').superfish({
        delay: 100,                            // one second delay on mouseout
        animation: {opacity: 'show', height: 'show'},  // fade-in and slide-down animation
        speed: 'fast',                          // faster animation speed
        autoArrows: false                           // disable generation of arrow mark-up
    });
});


/***************************************************
 PORTFOLIO ITEM IMAGE HOVER
 ***************************************************/
$(window).load(function () {

    $(".portfolio-grid ul li .item-info-overlay").hide();

    if (is_touch_device()) {
        $(".portfolio-grid ul li").click(function () {
            var count_before = $(this).closest("li").prevAll("li").length;
            var this_opacity = $(this).find(".item-info-overlay").css("opacity");
            var this_display = $(this).find(".item-info-overlay").css("display");

            if ((this_opacity == 0) || (this_display == "none")) {
                $(this).find(".item-info-overlay").fadeTo(250, 1);
            } else {
                $(this).find(".item-info-overlay").fadeTo(250, 0);
            }

            $(this).closest("ul").find("li:lt(" + count_before + ") .item-info-overlay").fadeTo(250, 0);
            $(this).closest("ul").find("li:gt(" + count_before + ") .item-info-overlay").fadeTo(250, 0);
        });
    } else {
        $(".portfolio-grid ul li").hover(function () {
            $(this).find(".item-info-overlay").fadeTo(250, 1);
        }, function () {
            $(this).find(".item-info-overlay").fadeTo(250, 0);
        });
    }
});

/***************************************************
 DUPLICATE H3 & H4 IN PORTFOLIO
 ***************************************************/
$(window).load(function () {
    $(".item-info").each(function (i) {
        $(this).next().prepend($(this).html())
    });
});

/***************************************************
 TOGGLE STYLE
 ***************************************************/
jQuery(document).ready(function ($) {

    $(".toggle-container").hide();
    var $trigger = $(".trigger");
    $trigger.toggle(function () {
        $(this).addClass("active");
    }, function () {
        $(this).removeClass("active");
    });
    $trigger.click(function () {
        $(this).next(".toggle-container").slideToggle();
    });
});

/***************************************************
 ACCORDION
 ***************************************************/
$(document).ready(function () {
    $('.trigger-button').click(function () {
        $(".trigger-button").removeClass("active");
        $('.accordion').slideUp('normal');
        if ($(this).next().is(':hidden') == true) {
            $(this).next().slideDown('normal');
            $(this).addClass("active");
        }
    });
});

/***************************************************
 SLIDING GRAPH
 ***************************************************/
jQuery(document).ready(function ($) {

    function isScrolledIntoView(id) {
        var elem = "#" + id;
        var docViewTop = $(window).scrollTop();
        var docViewBottom = docViewTop + $(window).height();

        if ($(elem).length > 0) {
            var elemTop = $(elem).offset().top;
            var elemBottom = elemTop + $(elem).height();
        }

        return ((elemBottom >= docViewTop) && (elemTop <= docViewBottom)
            && (elemBottom <= docViewBottom) && (elemTop >= docViewTop) );
    }


    function sliding_horizontal_graph(id, speed) {
        //alert(id);
        $("#" + id + " li span").each(function (i) {
            var j = i + 1;
            var cur_li = $("#" + id + " li:nth-child(" + j + ") span");
            var w = cur_li.attr("class");
            cur_li.animate({width: w + "%"}, speed);
        })
    }

    function graph_init(id, speed) {
        $(window).scroll(function () {
            if (isScrolledIntoView(id)) {
                sliding_horizontal_graph(id, speed);
            }
            else {
                //$("#" + id + " li span").css("width", "0");
            }
        });

        if (isScrolledIntoView(id)) {
            sliding_horizontal_graph(id, speed);
        }
    }

    graph_init("example-1", 1000);


});

/*--------------------------------------------------
 ADDITIONAL CODE GRID LIST
 ---------------------------------------------------*/
(function ($) {
    $.fn.extend({
        bra_last_last_row: function () {
            return this.each(function () {
                $(this).each(function () {
                    var no_of_items = $(this).find("li").length;
                    var no_of_cols = Math.round($(this).width() / $(this).find(":first").width());
                    var no_of_rows = Math.ceil(no_of_items / no_of_cols);
                    var last_row_start = (no_of_rows - 1) * no_of_cols - 1;
                    if (last_row_start < (no_of_cols - 1)) {
                        last_row_start = 0;
                        $(this).find("li:eq(" + last_row_start + ")").addClass("last-row");
                    }
                    $(this).find("li:nth-child(" + no_of_cols + "n+ " + no_of_cols + ")").addClass("last");
                    $(this).find("li:gt(" + last_row_start + ")").addClass("last-row");
                })
            }); // return this.each
        }
    });
})(jQuery);

jQuery(document).ready(function ($) {
    $('.grid').bra_last_last_row();
    //$(window).resize(function() {
    //$('.grid').bra_last_last_row();
    //});
});

/***************************************************
 SELECT MENU ON SMALL DEVICES
 ***************************************************/
jQuery(document).ready(function ($) {

    var $menu_select = $("<select />");
    $("<option />", {"selected": "selected", "value": "", "text": "Site Navigation"}).appendTo($menu_select);
    $menu_select.appendTo("#primary-menu");

    $("#primary-menu").find("ul li a").each(function () {
        var menu_url = $(this).attr("href");
        var menu_text = $(this).text();

        if ($(this).parents("li").length == 2) {
            menu_text = '- ' + menu_text;
        }
        if ($(this).parents("li").length == 3) {
            menu_text = "-- " + menu_text;
        }
        if ($(this).parents("li").length > 3) {
            menu_text = "--- " + menu_text;
        }
        $("<option />", {"value": menu_url, "text": menu_text}).appendTo($menu_select)
    });

    $("#primary-menu").find("select").change(function () {
        var location = $(this).find("option:selected").attr("value");
        window.location = location;
        return false;
    });
});


/***************************************************
 ADD MASK LAYER
 ***************************************************/
$(window).load(function () {
    var $item_mask = $("<div />", {"class": "item-mask"});
    $("ul.shaped .item-container, ul.comment-list .avatar").append($item_mask)
});

/***************************************************
 LINKS ON <DIV>s
 ***************************************************/
$(function () {
    $("div[data-href]").each(function (idx, elem) {
        var $self = $(elem);
        $self.click(function () {
            window.location.href = $self.attr("data-href");
            return false;
        })
            .css("cursor", "pointer");
    });
});


/***************************************************
 NAVIGATION HIGHLIGHTING
 ***************************************************/
$(function () {
    String.prototype.endsWith = function (suffix) {
        return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };

    $("#primary-menu").find("a").each(function (idx, elem) {
        var $self = $(elem);
        if (window.location.href.endsWith($self.attr("href"))) {
            $self.parents("li").children("a").attr("class", "current");
        }
    });
});

/***************************************************
 ACTIVITY STREAM
 ***************************************************/

$(function () {
    // Global for JSONP tweet callback
    window.loadTweets = window.loadTweets || [];

    var PATTERN_GITHUB_TIMESTAMP = /([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]+)Z/;
    var PATTERN_TWITTER_TIMESTAMP = /([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]+)\+[0-9]+/;

    function parseTime(pattern, value) {
        pattern.exec(value);
        // Date(year, month [, day [, hours[, minutes[, seconds[, ms]]]]])
        return new Date(RegExp.$1, RegExp.$2 - 1, RegExp.$3, RegExp.$4, RegExp.$5, RegExp.$6).getTime();
    }

    function htmlEscape(str) {
        return String(str)
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    // Provider for activities in the stream
    var Provider = function (config) {
        this.config = config;
        this.handle = config.handle;
    };

    // Default CORS implementation for activity retrieval
    Provider.prototype.load = function (callback) {
        var self = this;
        var xhr = window.XDomainRequest ? new XDomainRequest() : new XMLHttpRequest();
        if (!xhr) return;
        xhr.open("GET", self.config.source, true);
        xhr.onload = function (_) {
            var data = JSON.parse(xhr.responseText);
            callback(self.handle(data));
        };
        xhr.setRequestHeader("Accept", self.config.format || "application/json");
        xhr.send();
    };

    // Commits in an arbitrary github repo
    var GitHubCommit = function (owner, repo, branch) {
        return new Provider({
            source: "https://api.github.com/repos/" + owner + "/" + repo + "/commits?sha=" + branch,
            handle: function (data) {
                var result = [];
                $.each(data, function (_, change) {
                    result.push({
                        type: "commit-" + owner + "-" + repo + "-" + branch,
                        message: '<a href="' + change.html_url + '">' + htmlEscape(change.commit.message) + '</a>',
                        url: change.html_url,
                        time: parseTime(PATTERN_GITHUB_TIMESTAMP, change.commit.committer.date)
                    });
                });
                return result;
            }
        });
    };

    // Issue changes in an arbitrary github repo
    var GitHubIssueChange = function (owner, repo) {
        return new Provider({
            source: "https://api.github.com/repos/" + owner + "/" + repo + "/issues?state=all&sort=updated",
            handle: function (data) {
                var result = [];
                $.each(data, function (_, issue) {
                    result.push({
                        type: "issue",
                        message: '<a href="' + issue.url + '">' + issue.state + ' issue #' + issue.number + ": " + issue.title + '</a>',
                        url: issue.url,
                        time: parseTime(PATTERN_GITHUB_TIMESTAMP, issue.updated_at)
                    });
                });
                return result;
            }
        });
    };

    // Tweets. Since there is no authentication-free tweet search API and we've got
    // no server-side component, we'll use a twitter widget as the data source.
    // this means we have to parse the widget HTML to retrieve data.
    var TweetsForWidget = function (widgetId) {
        var result = [];

        loadTweets.push(function (data) {
            $(data.body).find("li.tweet").each(function (_, elem) {
                var tweet = {
                    type: "tweet"
                };
                var $elem = $(elem);
                $elem.find("time.dt-updated").each(function (_, elem) {
                    tweet.time = parseTime(PATTERN_TWITTER_TIMESTAMP, elem.getAttribute("datetime"));
                });
                $elem.find("a.u-url").each(function (_, elem) {
                    tweet.url = elem.href;
                });
                $elem.find("p.e-entry-title").each(function (_, elem) {
                    tweet.message = elem.innerHTML;
                });

                result.push(tweet);
            });

            return result;
        });

        var provider = new Provider({
            source: "https://cdn.syndication.twimg.com/widgets/timelines/" + widgetId + "?lang=en&t=1560138&callback=loadTweets[" + (loadTweets.length - 1) + "]&suppress_response_codes=true"
        });

        provider.load = function (callback) {
            $.ajax({
                url: provider.config.source,
                dataType: "script",
                success: function () {
                    callback(result);
                },
                error: function () {
                    throw new Error("Could not load script " + script);
                }
            });
        };
        return  provider;
    };

    // Register the providers that shall provide activities
    var providers = [
        GitHubCommit("unic", "neba", "develop"),
        GitHubCommit("unic", "neba", "gh-pages"),
        GitHubIssueChange("unic", "neba"),
        TweetsForWidget("479283277933457408")
    ];

    // Invokes a callback once a certain number of operations have finished
    var Barrier = function (i, callback) {
        return {
            expected: i,
            count: 0,
            done: function () {
                ++this.count;
                if (this.count == this.expected) {
                    callback()
                }
            }
        };
    };

    // Will contain a (sorted) list of activities
    var activities = [];

    // New barrier instance. Callback sorts and renders the activities.
    var b = Barrier(providers.length, function () {
        var $ul = $('<ul class="slides"></ul>');
        var $li = $("<li>");

        activities.sort(function (a, b) {
            return b.time - a.time;
        });

        $.each(activities, function (idx, activity) {
            var isFourthElement = (idx + 1) % 4 == 0;
            $li.append('<div class="one-fourth' + (isFourthElement ? ' last' : '') + '"><div class="' + activity.type + ' activity">' +
                '<a href="' + activity.url + '" class="activity-link"></a>' +
                '<p><span class="date">' + new Date(activity.time).toDateString() + '</span>' +
                '<br />' + activity.message +
                '</p>' +
                '</div></div>');

            if (isFourthElement) {
                $ul.append($li);
                $li = $("<li>");
            }
        });

        $("#activities").append($ul).flexslider({
            animation: "slide",
            slideDirection: "horizontal",
            slideshow: false,
            animationDuration: 500,
            directionNav: true,
            controlNav: false,
            randomize: false,
            startAt: 1
        });
    });

    // Invoke all providers, using the barrier created above
    $.each(providers, function (_, provider) {
        provider.load(function (a) {
            activities = activities.concat(a);
            b.done();
        });
    });

});