var numberOfSlides = 5;
var currentSlide = 1;
var slideItems = function() {
    var index = parseInt($(this).attr('data-value'));
    if ($(this).hasClass('slide-right')) {
        index++;
    } else {
        index--;
    }
    if (index > numberOfSlides) {
        index = 1;
    }
    if (index < 1) {
        index = numberOfSlides;
    }


    goto(index);


};
$(document).ready(function() {
    $('.slide-left').click(slideItems);
    $('.slide-right').click(slideItems);

    if ($.browser.msie && $.browser.version == 7.0) {
        $('.contentbox').hide();
        $("#slide_1").show();
        $("#slide_image_1").show();
    }
});

function goto(index, t) {
    //animate to the div id.
     if (index == numberOfSlides) {
        $('.slide-left').show();
        $('.slide-right').hide();
    } else if (index == 1) {
        $('.slide-left').hide();
        $('.slide-right').show();
    } else {
        $('.slide-left').show();
        $('.slide-right').show();
    }

    $('.slide-left').attr('data-value', index);
    $('.slide-right').attr('data-value', index);

    if ($.browser.msie && $.browser.version == 7.0) {
        $('.contentbox').hide();
        $("#slide_" + index).show();
        $("#slide_image_" + index).show();
    }
    $(".contentbox-wrapper").delay(100).animate({"left": -($("#slide_" + index).position().left)}, 600);
    $(".contentbox-image-wrapper").delay(300).animate({"left": -($("#slide_" + index).position().left)}, 800);


    $("#small-clouds").animate({"left": -100 * index}, 700);
    $("#big-clouds").animate({"left": -350 * index}, 700);

    //setting the header..
    currentSlide = index;
    $('.slide-headings h2').hide();
    $('.slide-headings h2').each(function(index) {
        if (currentSlide == index + 1) {
            $(this).show();
            $('span', this).hide();
        } else {
            $(this).hide();
        }
    });

    $('.slide-headings h2').each(function(index) {
        if (currentSlide == index + 1) {
            $("span:first-child", this).show(100, function () {
                // use callee so don't have to name the function
                $(this).next().show(100, arguments.callee);
            });
        } else {
            $(this).hide();
        }
    });
}

