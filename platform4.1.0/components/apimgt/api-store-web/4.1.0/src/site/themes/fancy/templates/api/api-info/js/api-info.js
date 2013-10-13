$(document).ready(function () {

    $("select[name='tiers-list']").change(function() {
        var selectedIndex = document.getElementById('tiers-list').selectedIndex;
        var api = jagg.api;
        var tierDescription = api.tierDescription;
        var tierDescList = tierDescription.split(",");
        for (var i = 0; i < tierDescList.length; i++) {
            var tierDesc = tierDescList[i];
            if (selectedIndex == i) {
                if (tierDesc != "null") {
                    $("#tierDesc").text(tierDesc);
                }
            }
        }

    });
    $("#subscribe-button").click(function () {
        if (!jagg.loggedIn) {
            return;
        }
        var applicationId = $("#application-list").val();
        if (applicationId == "-") {
            jagg.message({content:i18n.t('info.appSelect'),type:"info"});
            return;
        }
        var api = jagg.api;
        var tier=$("#tiers-list").val();
        $(this).html(i18n.t('info.wait')).attr('disabled', 'disabled');

        jagg.post("/site/blocks/subscription/subscription-add/ajax/subscription-add.jag", {
            action:"addSubscription",
            applicationId:applicationId,
            name:api.name,
            version:api.version,
            provider:api.provider,
            tier:tier
        }, function (result) {
            $("#subscribe-button").html('Subscribe');
            $("#subscribe-button").removeAttr('disabled');
            if (result.error == false) {
                $('#messageModal').html($('#confirmation-data').html());
                $('#messageModal h3.modal-title').html(i18n.t('info.subscription'));
                $('#messageModal div.modal-body').html('\n\n'+i18n.t('info.subscriptionSuccess'));
                $('#messageModal a.btn-primary').html(i18n.t('info.gotoSubsPage'));
                $('#messageModal a.btn-other').html(i18n.t('info.stayPage'));
                $('#messageModal a.btn-other').click(function() {
                    window.location.reload();
                });
                $('#messageModal a.btn-primary').click(function() {
                    location.href = "../site/pages/subscriptions.jag";
                });
                $('#messageModal').modal();


            } else {
                jagg.message({content:result.message,type:"error"});

                //$('#messageModal').html($('#confirmation-data').html());
                /*$('#messageModal h3.modal-title').html('API Provider');
                 $('#messageModal div.modal-body').html('\n\nSuccessfully subscribed to the API.\n Do you want to go to the subscription page?');
                 $('#messageModal a.btn-primary').html('Yes');
                 $('#messageModal a.btn-other').html('No');
                 */
                /*$('#messageModal a.btn-other').click(function(){
                 v.resetForm();
                 });*/
                /*
                 $('#messageModal a.btn-primary').click(function() {
                 var current = window.location.pathname;
                 if (current.indexOf(".jag") >= 0) {
                 location.href = "index.jag";
                 } else {
                 location.href = 'site/pages/index.jag';
                 }
                 });*/
//                        $('#messageModal').modal();


            }
        }, "json");

    });
    $('#application-list').change(
            function(){
                if($(this).val() == "createNewApp"){
                    //$.cookie('apiPath','foo');
                    window.location.href = '../site/pages/applications.jag?goBack=yes';
                }
            }
            );
    jagg.initStars($(".api-info"), function (rating, api) {
        jagg.post("/site/blocks/api/api-info/ajax/api-info.jag", {
            action:"addRating",
            name:api.name,
            version:api.version,
            provider:api.provider,
            rating:rating
        }, function (result) {
            if (result.error == false) {
                window.location.reload();
            } else {
                jagg.message({content:result.message,type:"error"});
            }
        }, "json");
    }, function (api) {

    }, jagg.api);


});

var removeRating = function(api) {
    jagg.post("/site/blocks/api/api-info/ajax/api-info.jag", {
        action:"addRating",
        name:api.name,
        version:api.version,
        provider:api.provider,
        rating:'0'
    }, function (result) {
        if (!result.error) {
            window.location.reload();
        } else {
            jagg.message({content:result.message,type:"error"});
        }
    }, "json");

};
