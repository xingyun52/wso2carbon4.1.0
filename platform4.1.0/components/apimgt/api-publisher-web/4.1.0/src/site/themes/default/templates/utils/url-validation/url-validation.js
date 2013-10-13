var checkURLValid = function(url,btn,type) {
    if($(btn).next().get(0).nodeName == "SPAN"){
        $(btn).next().remove();
    }
    $(btn).addClass("loadingButton-small");
    $(btn).val(i18n.t('validationMsgs.validating'));
    //$(btn).val("Test URI");

    if (url == '') {
        $(btn).after(' <span class="label label-important"><i class="icon-remove icon-white"></i>'+ i18n.t('validationMsgs.invalid')+'</span>');
        var toFade = $(btn).next();
        $(btn).removeClass("loadingButton-small");
        $(btn).val(i18n.t('validationMsgs.testUri'));
        var foo = setTimeout(function(){$(toFade).hide()},3000);
        return;
    }
    if (!type) {
        type = "";
    }
    jagg.post("/site/blocks/item-add/ajax/add.jag", { action:"isURLValid", type:type,url:url },
              function (result) {
                  if (!result.error) {

                      if (result.response == "success") {
                          $(btn).after(' <span class="label label-success"><i class="icon-ok icon-white"></i>'+ i18n.t('validationMsgs.valid')+'</span>');

                      } else {
                          $(btn).after(' <span class="label label-important"><i class="icon-remove icon-white"></i>'+ i18n.t('validationMsgs.invalid')+'</span>');
                      }
                      var toFade = $(btn).next();
                      var foo = setTimeout(function() {
                            $(toFade).hide();
                      }, 3000);

                  }
                  $(btn).removeClass("loadingButton-small");
                  $(btn).val(i18n.t('validationMsgs.testUri'));
              }, "json");
};

