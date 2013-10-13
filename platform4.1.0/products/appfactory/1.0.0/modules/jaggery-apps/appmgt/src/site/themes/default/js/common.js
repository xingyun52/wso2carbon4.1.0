$(document).ready(function() {
    // notification panel header click event hadler
    $("#event-streamer-panel-header").bind('click', function(event) {
        pushNotification(null, false);
    });

    bindTogglePanles();

});

/**
 * This method may be used to push any message in to the event stream panel.
 * @param {Object} message is the text you want to publish
 * @param {Object} autoCollapse - Set this as 'true', in order to make it auto dissapear after 4 seconds
 */
function pushNotification(message, autoCollapse) {
    $("#event-streamer").toggle(0, function() {
        var self = $(this);
        if (self.is(":visible") === false) {
            self.parent().css("width", "200px");
        } else {
            if (message !== null) {
                var ul = $("<li></li>");
                ul.text(message);
                ul.addClass("animate");
                self.prepend(ul);
                var animateInterval = setInterval(function() {
                    ul.toggleClass("animate");
                    clearInterval(animateInterval);
                    count++;
                }, 3000);
            }
            self.parent().css("width", "936px");
            if (autoCollapse === true) {
                var interval = setInterval(function() {
                    self.fadeOut('slow', function() {
                        self.parent().css("width", "200px");
                    });
                    clearInterval(interval);
                }, 4000);
            }
        }
    });
}

function bindTogglePanles(){
  // toggle panel actions
    var toggleBodies = $(".toggle-panel-body");
    for (var i = 1, j = toggleBodies.length; i < j; i++) {
        var toggleBody = $(toggleBodies[i]);
        toggleBody.css("display", "none");
    };
    $(".toggle-panel-header").bind('click', function() {
        var self = $(this);
        var currentToggleBody = self.next();
        currentToggleBody.fadeToggle('slow', function() {
            if ($(this).is(":visible") === false) {
                self.attr("title", "Click here to expand this section");
                self.find("span").attr("class", "icon-chevron-right");
            } else {
                self.attr("title", "Click here to collapse this section");
                self.find("span").attr("class", "icon-chevron-down");
            }
        });
    });
}