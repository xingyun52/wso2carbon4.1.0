var jagg = jagg || {};

(function () {
    jagg.post = function(url, data, callback, error) {
        return jQuery.ajax({
                               type:"POST",
                               url:url,
                               data:data,
                               async:true,
                               cache:false,
                               success:callback,                           
                               error:error
        });
    };

    jagg.syncPost = function(url, data, callback, type) {
        return jQuery.ajax({
                               type: "POST",
                               url: url,
                               data: data,
                               async:false,
                               success: callback,
                               dataType:"json"
        });
    };

    jagg.sessionExpired = function (){
        var sessionExpired = false;
        jagg.syncPost("/site/blocks/user/login/ajax/sessionCheck.jag", { action:"sessionCheck" },
                 function (result) {
                     if(result!=null){
                         if (result.message == "timeout") {
                             sessionExpired = true;
                         }
                     }
                 }, "json");
        return sessionExpired;
    };

    jagg.sessionAwareJS = function(params){


        if(jagg.sessionExpired()){
            if(params.e != undefined){  //Canceling the href call
                if ( params.e.preventDefault ) {
                    params.e.preventDefault();

                // otherwise set the returnValue property of the original event to false (IE)
                } else {
                    params.e.returnValue = false;
                }
            }

            jagg.showLogin(params);
        }else if(params.callback != undefined && typeof params.callback == "function"){
             params.callback();
         }
    };

    //New Google style message display method.

    jagg.messageDisplay = function (params) {
       $('#messageBlock').html(params.content).show();
        setInterval(function() {$('#messageBlock').html("").hide();},7000);
    };
     /*
    usage
    Show info dialog
    jagg.message({content:'foo',type:'info', cbk:function(){alert('Do something here.')} });

    Show warning
    dialog jagg.message({content:'foo',type:'warning', cbk:function(){alert('Do something here.')} });

    Show error dialog
    jagg.message({content:'foo',type:'error', cbk:function(){alert('Do something here.')} });

    Show confirm dialog
    jagg.message({content:'foo',type:'confirm',okCallback:function(){},cancelCallback:function(){}});

    Showing custom popup and registering a call back
    jagg.message({
            type:'custom',
            title:"Select what ever",
            content:'<div><select id="myWhatEver"><option value="foo">flls</option></select></div>',
            buttons:[
                        {cssClass:'btn',name:'Ok',cbk:function(){
                            alert($('#myWhatEver').val());
                            $('#messageModal').modal('hide');
                        }}
                    ]
        });
     */
    jagg.message = function(params){
        if(params.type == "custom"){
            jagg.popMessageDisplay(params);
            return;
        }
        if(params.type == "confirm"){ //Not breaking old stuff
            if( params.title == undefined ){ params.title = "WSO2 App Factory"}
            jagg.popMessageDisplay({content:params.content,title:params.title ,buttons:[
                {name:"Yes",cssClass:"btn btn-primary",cbk:function() {
                    $('#messageModal').modal('hide');
                    if(typeof params.okCallback == "function") {params.okCallback()};
                }},
                {name:"No",cssClass:"btn",cbk:function() {
                    $('#messageModal').modal('hide');
                    if(typeof params.cancelCallback  == "function") {params.cancelCallback()};
                }}
            ]
            });
            return;
        }

        if(params.cbk && typeof params.cbk == "function"){//Not breaking old messages
            params.content = '<table class="msg-table"><tr><td class="imageCell"><i class="icon-big-'+params.type+'"></i></td><td><span class="messageText"> '+params.content+'</span></td></tr></table>';
            var type = "";
            var additionalBtnClass = "";
            if(params.title == undefined){
                if(params.type == "info"){ type = "Notification"; additionalBtnClass = " btn-info"}
                if(params.type == "warning"){ type = "Warning"; additionalBtnClass = " btn-warning"}
                if(params.type == "error"){ type = "Error"; ; additionalBtnClass = " btn-danger"}
                if(params.type == "success"){ type = "Successful Operation"; ; additionalBtnClass = " btn-success"}
            }
            jagg.popMessageDisplay({content:params.content,title:'<span class="'+params.type+'-msg-title">WSO2 App Factory - ' + type + '</span>',buttons:[
                {name:"OK",cssClass:"btn"+additionalBtnClass,cbk:function() {
                    $('#messageModal').modal('hide');
                    if(params.cbk && typeof params.cbk == "function")
                            params.cbk();
                }}
            ]
            });
        }else{
            jagg.messageDisplay(params);
        }


    };
         /*
    usage
    Show info dialog
    jagg.popMessage({content:'foo',type:'info', cbk:function(){alert('Do something here.')} });

    Show warning
    dialog jagg.popMessage({content:'foo',type:'warning', cbk:function(){alert('Do something here.')} });

    Show error dialog
    jagg.popMessage({content:'foo',type:'error', cbk:function(){alert('Do something here.')} });

    Show confirm dialog
    jagg.popMessage({content:'foo',type:'confirm',okCallback:function(){},cancelCallback:function(){}});

    Showing custom popup and registering a call back
    jagg.popMessage({
            type:'custom',
            title:"Select what ever",
            content:'<div><select id="myWhatEver"><option value="foo">flls</option></select></div>',
            buttons:[
                        {cssClass:'btn',name:'Ok',cbk:function(){
                            alert($('#myWhatEver').val());
                            $('#messageModal').modal('hide');
                        }}
                    ]
        });
     */
    jagg.popMessageDisplay = function (params) {
       $('#messageModal').html($('#confirmation-data').html());
       if(params.title == undefined){
           $('#messageModal h3.modal-title').html('API Store');
       }else{
           $('#messageModal h3.modal-title').html(params.title);
       }
       $('#messageModal div.modal-body').html(params.content);
       if(params.buttons != undefined){
           $('#messageModal a.btn-primary').hide();
           for(var i=0;i<params.buttons.length;i++){
               $('#messageModal div.modal-footer').append($('<a class="btn '+params.buttons[i].cssClass+'">'+params.buttons[i].name+'</a>').click(params.buttons[i].cbk));
           }
       }else{
           $('#messageModal a.btn-primary').html('OK').click(function() {
               $('#messageModal').modal('hide');
           });
       }
       $('#messageModal a.btn-other').hide();
       $('#messageModal').modal();
    };

    jagg.popMessage = function(params){
        var siteRoot = "";
        if(params.type == "custom"){
            jagg.popMessageDisplay(params);
            return;
        }
        if(params.type == "confirm"){
            if( params.title == undefined ){ params.title = "WSO2 App Factory"}
            jagg.popMessageDisplay({content:params.content,title:params.title ,buttons:[
                {name:"Yes",cssClass:"btn btn-primary",cbk:function() {
                    $('#messageModal').modal('hide');
                    if(typeof params.okCallback == "function") {params.okCallback()};
                }},
                {name:"No",cssClass:"btn",cbk:function() {
                    $('#messageModal').modal('hide');
                    if(typeof params.cancelCallback  == "function") {params.cancelCallback()};
                }}
            ]
            });
            return;
        }
        params.content = '<table class="msg-table"><tr><td class="imageCell"><i class="icon-big-'+params.type+'"></i></td><td><span class="messageText"> '+params.content+'</span></td></tr></table>';
        var type = "";
        var additionalBtnClass = "";
        if(params.title == undefined){
            if(params.type == "info"){ type = "Notification"; additionalBtnClass = " btn-info"}
            if(params.type == "warning"){ type = "Warning"; additionalBtnClass = " btn-warning"}
            if(params.type == "error"){ type = "Error"; ; additionalBtnClass = " btn-danger"}
            if(params.type == "success"){ type = "Successful Operation"; ; additionalBtnClass = " btn-success"}
        }

        jagg.popMessageDisplay({content:params.content,title:'<span class="'+params.type+'-msg-title">WSO2 App Factory - ' + type + '</span>',buttons:[
            {name:"OK",cssClass:"btn"+additionalBtnClass,cbk:function() {
                $('#messageModal').modal('hide');
                if(params.cbk && typeof params.cbk == "function")
	                    params.cbk();
            }}
        ]
        });
    };


}());