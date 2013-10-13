var numberOfSlides = 4;
var currentSlide = 1;
var sliderTimer;
var slideItems = function() {
	clearInterval(sliderTimer);
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
	currentSlide = index;
    goto(index,element);
	

};

function rotateSlides(){
	$('#slider-buttons li').removeClass('active');
	if (currentSlide >= numberOfSlides) {
        currentSlide = 1;
		$('.slide-left').addClass('active');
    }else if (currentSlide < 1) {
        currentSlide = numberOfSlides;
		$('.slide-right').addClass('active');
    }else{
		currentSlide++;
	}
	
	$('#slider-buttons li').each(function(index){
			if(index == currentSlide -1){
				$(this).addClass("active");
			}
	});
    goto(currentSlide);
}
$(document).ready(function() {
    $('.slide-left').click(slideItems);
    $('.slide-right').click(slideItems);

    if ($.browser.msie && $.browser.version == 7.0) {
        $('.contentbox').hide();
        $("#slide_1").show();
        $("#slide_image_1").show();
    }
	
	sliderTimer=setInterval(rotateSlides,5000);
});

function goto(index, t) {
    //animate to the div id.
	$('#slider-buttons li').removeClass('active');
	$(t).parent().addClass('active');
    if (index == numberOfSlides) {
        $('.slide-left').addClass("active");
    } else if (index == 1) {
        $('.slide-right').addClass("active");
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

    
}

