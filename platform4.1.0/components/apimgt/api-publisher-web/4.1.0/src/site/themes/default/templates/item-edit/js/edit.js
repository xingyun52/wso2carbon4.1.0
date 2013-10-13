$(document).ready(function() {
    $("select[name='editTier']").change(function() {
        // multipleValues will be an array
        var multipleValues = $(this).val() || [];
        var countLength = $('#tiersCollection').length;
        if (countLength == 0) {

            $('<input>').attr('type', 'hidden')
                    .attr('name', 'tiersCollection')
                    .attr('id', 'tiersCollection')
                    .attr('value', multipleValues)
                    .appendTo('#editAPIForm');
        } else {
            $('#tiersCollection').attr('value', multipleValues);

        }

    });

    enableDisableButtons();
    $('#context').change(function() {
        getContextValue();
    });
    $('#uriTemplate').click(function() {
        $('#resourceTableError').hide('fast');
    });


    $('#resourceTable tr.resourceRow').each(function(){
        $('input',this).unbind('change');
        $('input:checkbox',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });

        $('input:text',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });

        $('select',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });
    });

});

//var rowNums=new Array();
function loadTiers() {
    var target = document.getElementById("editTier");
    jagg.post("/site/blocks/item-add/ajax/add.jag", { action:"getTiers" },
              function (result) {
                  if (!result.error) {
                      var arr = [];
                      for (var i = 0; i < result.tiers.length; i++) {
                          arr.push(result.tiers[i].tierName);
                      }
                      for (var j = 0; j < arr.length; j++) {
                          option = new Option(arr[j], arr[j]);
                          target.options[j] = option;
                          target.options[j].title = result.tiers[j].tierDescription;
                      }
                      addSelectedTiers(target);

                  }
                  $('#saveMessage').hide(); $('#saveButtons').show();

              }, "json");
}


var addResourcesToApi = function () {
    $('#resourceRow').clone().addClass('resourceRow').insertAfter($('#resourceRow')).show();
    enableDisableButtons();
    $('#resourceTable tr.resourceRow').each(function(){
        $('input',this).unbind('change');
        $('input:checkbox',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });

        $('input:text',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });

        $('select',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });
    });
};
var enableDisableButtons = function(){
   $('#resourceTable tr').each(function(index){
        var allRows = $('#resourceTable tr');
        if(index > 0){
            if(index == 2){
                    $('.upButton',this).attr('disabled','disabled');
                    $('.downButton',this).removeAttr('disabled');
            }
            if(index > 2 && allRows.length-1 > index){
                    $('.downButton',this).removeAttr('disabled');
                    $('.upButton',this).removeAttr('disabled');
            }
            if(allRows.length-1 == index){
                $('.deleteButton',this).removeAttr('disabled','disabled');
                if(index != 2){
                    $('.upButton',this).removeAttr('disabled');
                }else {
                    $('.upButton',this).attr('disabled','disabled');
                    $('.deleteButton',this).attr('disabled','disabled');
                }
                $('.downButton',this).attr('disabled','disabled');
            }
        }
    });
};
var moveMe = function(moveButton){
    var action = "move-up";
    if($(moveButton).hasClass('downButton')){
        action = "move-down";
    }

    if(action == "move-up"){
        $(moveButton).parent().parent().insertBefore($(moveButton).parent().parent().prev());
    }
    if(action == "move-down"){
        $(moveButton).parent().parent().insertAfter($(moveButton).parent().parent().next());
    }

    enableDisableButtons();
    createHiddenForm();
};
var createHiddenForm = function(){
    $('#hiddenFormElements input').remove();

    $('#resourceTable tr').each(function(index){
        var resourcesCount = index - 2;
        var resourceMethodValues = "";
        var resourceMethodAuthValues = "";

        var tr = this;
        //read the checkbox values
       if($('.resource-get',tr).is(':checked')){
           if(resourceMethodValues == ""){resourceMethodValues += "GET"}else{resourceMethodValues += ",GET"}
           var selectedValue = $('.getAuthType',tr).val();
           if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue}
       }
        if($('.resource-put',tr).is(':checked')){
            if(resourceMethodValues == ""){resourceMethodValues += "PUT"}else{resourceMethodValues += ",PUT"}
            var selectedValue = $('.putAuthType',tr).val();
            if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue}
        }
        if($('.resource-post',tr).is(':checked')){
            if(resourceMethodValues == ""){resourceMethodValues += "POST"}else{resourceMethodValues += ",POST"}
            var selectedValue = $('.postAuthType',tr).val();
            if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue }
        }
        if($('.resource-delete',tr).is(':checked')){
            if(resourceMethodValues == ""){resourceMethodValues += "DELETE"}else{resourceMethodValues += ",DELETE"}
            var selectedValue = $('.deleteAuthType',tr).val();
            if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue}
        }
        if($('.resource-options',tr).is(':checked')){
            if(resourceMethodValues == ""){resourceMethodValues += "OPTIONS"}else{resourceMethodValues += ",OPTIONS"}
            var selectedValue = $('.optionsAuthType',tr).val();
            if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue}
        }

       if(resourcesCount >= 0){
           $('<input>').attr('type', 'hidden')
                   .attr('name', 'uriTemplate-' + resourcesCount).attr('id', 'uriTemplate-' + resourcesCount).attr('value', $('.resourceTemplate',tr).val())
                   .appendTo('#hiddenFormElements');

           $('<input>').attr('type', 'hidden')
                   .attr('name', 'resourceMethod-' + resourcesCount).attr('id', 'resourceMethod-' + resourcesCount).attr('value', resourceMethodValues)
                   .appendTo('#hiddenFormElements');

           $('<input>').attr('type', 'hidden')
                   .attr('name', 'resourceMethodAuthType-' + resourcesCount).attr('id', 'resourceMethodAuthType-' + resourcesCount).attr('value', resourceMethodAuthValues)
                   .appendTo('#hiddenFormElements');
       }
   });

   $('#resourceCount').val($('#resourceTable tr').length-2);
};
var deleteResource = function (deleteButton) {
    var count=$('#resourceTable tr').length;
    //Check whether only one defined resource remains before delete operation
    if(count==3){
        $('#resourceTableError').show('fast');
        $('#resourceTableError').html( i18n.t('errorMsgs.apiResource')+'<br />');
        return;
    }
    $('#resourceTableError').hide('fast');
    $(deleteButton).parent().parent().remove();

    enableDisableButtons();
    createHiddenForm();
};

var validateResourceTable = function(){
    var errors = "";

    $('.resourceRow input.resourceTemplate').each(function(){
        var myVal = $(this).val();
        var foundMyVal = 0;
        $('.resourceRow input.resourceTemplate').each(function(){
            if($(this).val()==myVal){
                foundMyVal++;
            }
        });
        if(foundMyVal > 1){
            errors +=  i18n.t('errorMsgs.uniqueUrlPattern')+"<strong>" + myVal + "</strong>"+ i18n.t('errorMsgs.duplicateUrlPattern') +"<br/>";
        }
        if(myVal == ""){
            errors += i18n.t('errorMsgs.emptyUrlPattern')+"<br />";
        }
    });

    var allRowsHas_at_least_one_check = true;
    $('.resourceRow').each(function(){
        var tr = this;
        var noneChecked = true;
        $('input:checkbox',tr).each(function(){
            if($(this).is(":checked")){
                noneChecked = false;
            }
        });

        if(noneChecked){
            allRowsHas_at_least_one_check = false;
        }
    });



    if(!allRowsHas_at_least_one_check){
        errors += i18n.t('errorMsgs.emptyVerb')+"<br />";
    }
    if(errors != ""){
        $('#resourceTableError').show('fast');
        $('#resourceTableError').html(errors);
    }else{
        $('#resourceTableError').hide('fast');
    }
    return errors;
};

function getContextValue() {
    var context = $('#context').val();
    var version = $('#apiVersion').val();

    if (context == "" && version != "") {
        $('#contextForUrl').html("/{context}/" + version);
        $('#contextForUrlDefault').html("/{context}/" + version);
    }
    if (context != "" && version == "") {
        if (context.charAt(0) != "/") {
            context = "/" + context;
        }
        $('#contextForUrl').html(context + "/{version}");
        $('#contextForUrlDefault').html(context + "/{version}");
    }
    if (context != "" && version != "") {
        if (context.charAt(0) != "/") {
            context = "/" + context;
        }
        $('.contextForUrl').html(context + "/" + version);
    }
}
function showHideRoles(){
    var visibility = $('#visibility').find(":selected").val();
    if(visibility == "public"){
        $('#roles').val('');
        $('#roles').hide();
        $('#rolesLabel').hide();
    }
    else{
        $('#rolesDiv').show();
        $('#roles').show();
        $('#rolesLabel').show();
    }
}


function showUTProductionURL(){
	var endpointType = $('#endpointType').find(":selected").val();
	if(endpointType == "secured"){
		$('#credentials').show();
	}
	else{
		$('#credentials').hide();
	}
	
}






