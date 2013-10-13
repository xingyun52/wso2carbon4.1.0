$(document).ready(function() {
    tinyMCE.init({
                     mode : "textareas",
                     theme : "advanced",
                     theme_advanced_buttons1 : "newdocument,|,bold,italic,underline,|,justifyleft,justifycenter,justifyright,fontselect,fontsizeselect,formatselect",
                     theme_advanced_buttons2 : "cut,copy,paste,|,bullist,numlist,|,outdent,indent,|,undo,redo,|,forecolor,backcolor",
                     theme_advanced_buttons3 : "insertdate,inserttime,|,spellchecker,advhr,,removeformat,|,sub,sup,|,charmap,emotions",
                     theme_advanced_toolbar_location : "top",
                     theme_advanced_toolbar_align : "left",
                     theme_advanced_resizing : true

                 });
});


function loadDefaultTinyMCEContent(provider,apiName, version, docName) {
    jagg.post("/site/blocks/documentation/ajax/docs.jag", { action:"getInlineContent", provider:provider,apiName:apiName,version:version,docName:docName },
              function (json) {
                  if (!json.error) {
                      var docName = json.doc.provider.docName;
                      var apiName = json.doc.provider.apiName;
                      var docContent = json.doc.provider.content;
                      $('#apiDeatils').empty().html('<p><h1> ' + docName + '</h1></p>');
                      tinyMCE.activeEditor.setContent(docContent);
                  } else {
                      $('#inlineError').show('fast');
                      $('#inlineSpan').html('<strong>'+ i18n.t('errorMsgs.inlineContent')+'</strong><br />'+result.message);
                  }
              }, "json");



}

function saveContent(provider, apiName, apiVersion, docName, mode) {
    var contentDoc = tinyMCE.get('inlineEditor').getContent();
    jagg.post("/site/blocks/documentation/ajax/docs.jag", { action:"addInlineContent",provider:provider,apiName:apiName,version:apiVersion,docName:docName,content:contentDoc},
              function (result) {
                  if (result.error) {
                      if (result.message == "AuthenticateError") {
                          jagg.showLogin();
                      } else {
                          jagg.message({content:result.message,type:"error"});
                      }
                  } else {
                      if (mode == "save") {
                         /* $('#messageModal').html($('#confirmation-data').html());
                          $('#messageModal h3.modal-title').html('Document Content Addition Successful');
                          $('#messageModal div.modal-body').html('\n\n Successfully saved the documentation content and you will be moved away from this tab.');
                          $('#messageModal a.btn-primary').html('OK');
                          $('#messageModal a.btn-other').hide();
                          $('#messageModal a.btn-primary').click(function() {*/
                              window.close();
                          /*});
                          $('#messageModal').modal();*/
                      } else {
                           $('#docAddMessage').show();
                           setTimeout("alert()", 3000);
                      }
                  }
              }, "json");
}

var alert=function alertMsg() {
    $('#docAddMessage').hide("fast");
}