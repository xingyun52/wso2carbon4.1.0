$(document).ready(function() {
    var docUrlDiv=$('#docUrl');
    docUrlDiv.click(function() {
       docUrlDiv.removeClass('error');
       docUrlDiv.next().hide();
    });

    docUrlDiv.change(function() {
    validInputUrl(docUrlDiv);
    });

    $('input[name=optionsRadios1]:radio:checked').change(function() {
        if (getRadioValue($('input[name=optionsRadios1]:radio:checked')) == "inline") {
            $('#docUrl').removeClass('error');
            $('#docUrl').next().hide();
        }
    });

    var docId = $("#docName");
    docId.change(function () {
        var apiName = $("#docAPIName").val();
        //Check the doc name is duplicated
        var errorCondition = isAvailableDoc(apiName + "-" + docId.val());
        validInput(docId, 'Duplicate Document Name.', errorCondition);

    });

$('#saveDoc').click(function() {
        var sourceType = getRadioValue($('input[name=optionsRadios1]:radio:checked'));
        var docUrlDiv = $("#docUrl");
	var fileDiv = $("#docLocation");
        var apiName = $("#docAPIName").val();
        var errCondition = docUrlDiv.val() == "";
	var isFilePathEmpty = fileDiv.val() == "";
	var isOtherTypeNameEmpty = $('#specifyBox').val() == null || $('#specifyBox').val() == '';
	var docType = getRadioValue($('input[name=optionsRadios]:radio:checked'));

        var errorCondition = false;
        if($(this).val() != "Update"){
            errorCondition = isAvailableDoc(apiName + "-" + docId.val());
        }
        if (apiName && !validInput(docId, 'Duplicate Document Name.', errorCondition)) {
            return;
        } else if (sourceType == 'url' && !validInput(docUrlDiv, 'This field is required.', errCondition)) {
            return;
        } else if (sourceType == 'url' && !validInputUrl(docUrlDiv)) {
            return;
        }else if($(this).val() != "Update" && sourceType == 'file' && !validInput(fileDiv, 'This field is required.', isFilePathEmpty)) {
         		    return;
        }else if(docType.toLowerCase() == 'other' && !validInput($('#specifyBox'),'This field is required.', isOtherTypeNameEmpty)){
			return;	
		}

        if($(this).val() == "Update" && $("#docLocation").val() == ""){
            $("#docLocation").removeClass('required');
        }

        $("#addNewDoc").validate();
        if ($("#addNewDoc").valid()) {
            var version = $("#docAPIVersion").val();
            var provider = $("#spanProvider").text();
            var docName = $("#docName").val();
            var summary = $("#summary").val();            

            var docUrl = docUrlDiv.val();
            if (docUrl.indexOf("http") == -1) {
                docUrl = "http://" + docUrl;
            }

            var mode = $('#newDoc .btn-primary').val();
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'provider').attr('value', provider).prependTo('#addNewDoc');
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'action').attr('value', "addDocumentation").prependTo('#addNewDoc');
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'apiName').attr('value', apiName).prependTo('#addNewDoc');
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'version').attr('value', version).prependTo('#addNewDoc');
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'docName').attr('value', docName).prependTo('#addNewDoc');
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'docType').attr('value', docType).prependTo('#addNewDoc');
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'summary').attr('value', summary).prependTo('#addNewDoc');
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'sourceType').attr('value', sourceType).prependTo('#addNewDoc');
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'docUrl').attr('value', docUrl).prependTo('#addNewDoc');
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'mode').attr('value', mode).prependTo('#addNewDoc');
	if(docType.toLowerCase()=='other'){
	$('<input>').attr('type', 'hidden')
		    .attr('name', 'newType').attr('value', $('#specifyBox').val()).prependTo('#addNewDoc');
	}

	$('#addNewDoc').ajaxSubmit(function (result) {
                          if (!result.error) {
                              $.cookie("tab", "docsLink");
                              clearDocs();
                          } else {
                              if (result.message == "AuthenticateError") {
                                  jagg.showLogin();
                              } else {
                                  jagg.message({content:result.message,type:"error"});
                              }
                          }
                      });
        }
    });
});

var newDocFormToggle = function(){
    $('#newDoc').toggle('slow');
    $('#docName').removeAttr("disabled").val('');
    $('#summary').val('');
    $('#docUrl').val('');
    $('#specifyBox').val('');
    $('#optionsRadios7').attr("checked","checked");
    $('#optionsRadios1').attr("checked","checked");
    $('#sourceUrlDoc').hide();
};

var removeDocumentation = function (provider, apiName, version, docName, docType) {
    $('#messageModal').html($('#confirmation-data').html());
    $('#messageModal h3.modal-title').html(i18n.t('confirm.delete'));
    $('#messageModal div.modal-body').html('\n\n'+ i18n.t('confirm.deleteMsg')+'<b>"' + docName + '</b>"?');
    $('#messageModal a.btn-primary').html('Yes');
    $('#messageModal a.btn-other').html('No');
    $('#messageModal a.btn-primary').click(function() {
        jagg.post("/site/blocks/documentation/ajax/docs.jag", { action:"removeDocumentation",provider:provider,
            apiName:apiName, version:version,docName:docName,docType:docType},
                  function (result) {
                      if (!result.error) {
                          $('#messageModal').modal('hide');
                          $('#' + apiName + '-' + docName).remove();
                          if ($('#docTable tr').length == 1) {
                              $('#docTable').append($('<tr><td colspan="6">'+i18n.t('resultMsgs.noDocs')+'</td></tr>'));
                          }
                      } else {
                          if (result.message == "AuthenticateError") {
                              jagg.showLogin();
                          } else {
                              jagg.message({content:result.message,type:"error"});
                          }
                      }
                  }, "json");
    });
    $('#messageModal a.btn-other').click(function() {
        return;
    });
    $('#messageModal').modal();
};

var updateDocumentation = function (rowId, docName, docType, summary, sourceType, docUrl, filePath, otherTypeName) {
    $("#docTable").hide('fast');
    $('#newDoc .btn-primary').text('Update');
    $('#newDoc .btn-primary').val('Update');
    $('#addDoc').hide('fast');
    $('#updateDoc h4')[0].innerHTML = "Update Document - " + docName;
    $('#updateDoc').show('fast');
    $('#newDoc').show('fast');
    $('#newDoc #docName').val(docName);
    $('#newDoc #docName').attr('disabled', 'disabled');
    if (summary != "{}" && summary != 'null') {
        $('#newDoc #summary').val(summary);
    }
    if (sourceType == "INLINE") {
        $('#optionsRadios7').attr('checked', true);
        $('#sourceUrlDoc').hide('slow');
        $('#docUrl').val('');
    } else if(sourceType == "URL"){
        if (docUrl != "{}") {
            $('#newDoc #docUrl').val(docUrl);
            $('#optionsRadios8').attr('checked', true);
            $('#sourceUrlDoc').show('slow');
        }
    }else {
            $('#optionsRadios9').attr('checked', true);
	    $('#sourceFile').show('slow');	
	}

    for (var i = 1; i <= 6; i++) {
        if ($('#optionsRadios' + i).val().toUpperCase().indexOf(docType.toUpperCase()) >= 0) {
            $('#optionsRadios' + i).attr('checked', true);
	if(docType.toLowerCase() == 'other'){
		$('#specifyBox').val(otherTypeName);
		$('#otherTypeDiv').show();		
		}
        }
    }
};

var editInlineContent = function (provider, apiName, version, docName, mode) {
    var current = window.location.pathname;
    if (current.indexOf("item-info.jag") >= 0) {
        window.open("inline-editor.jag?docName=" + docName + "&apiName=" + apiName + "&version=" + version + "&provider=" + provider + "&mode=" + mode);
    } else {
        window.open("site/pages/inline-editor.jag?docName=" + docName + "&apiName=" + apiName + "&version=" + version + "&provider=" + provider + "&mode=" + mode);
    }

};

var clearDocs = function () {
    window.location.reload();

};

var getRadioValue = function (radioButton) {
    if (radioButton.length > 0) {
        return radioButton.val();
    }
    else {
        return 0;
    }
};

var disableInline = function(type) {
    if (type == 'forum') {
        document.getElementById("optionsRadios7").disabled = true;
        document.getElementById("optionsRadios8").checked = true;
        $('#sourceUrlDoc').show('slow');
    } else {
        document.getElementById("optionsRadios7").disabled = false;
        document.getElementById("optionsRadios7").checked = true;
        $('#sourceUrlDoc').hide('slow');
    }
};

var isAvailableDoc = function(id) {
    var docEntry = $("#docTable #" + id).text();
    if (docEntry != "") {
        return true;
    }
};

var validInput = function(divId, message, condition) {
    if (condition) {
        divId.addClass('error');
        if (!divId.next().hasClass('error')) {
            divId.parent().append('<label class="error">' + message + '</label>');
        } else {
            divId.next().show();
        }
        return false;
    } else {
        divId.removeClass('error');
        divId.next().hide();
        return true;
    }

};
var validUrl = function(url) {
    var invalid = true;
    var regex = /^(https?|ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&amp;'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i;
    if (regex.test(url)) {
        invalid= false;
    }
    return invalid;
};

var validInputUrl = function(docUrlDiv) {
    if (docUrlDiv) {
        var docUrlD;
        if (docUrlDiv.val().indexOf("http") == -1) {
            docUrlD = "http://" + docUrlDiv.val();
        } else {
            docUrlD = docUrlDiv.val();
        }
        var erCondition = validUrl(docUrlD);
        return validInput(docUrlDiv, i18n.t('errorMsgs.invalidDocUrl'), erCondition);
    }
};






