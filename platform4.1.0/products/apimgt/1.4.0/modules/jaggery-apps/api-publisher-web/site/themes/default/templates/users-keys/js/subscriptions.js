var updateSubscription = function (apiName, version, provider, appId) {
    var ahrefId = $('#' + apiName + provider + appId);
    var status = ahrefId.text();
    var newStatus;
    if (status.trim() == 'Unblock') {
        newStatus = 'UNBLOCKED';
    } else {
        newStatus = 'BLOCKED';
    }
    jagg.post("/site/blocks/users-keys/ajax/subscriptions.jag", {
        action:"updateSubscription",
        apiName:apiName,
        version:version,
        provider:provider,
        appId:appId,
        newStatus:newStatus
    }, function (result) {
        if (!result.error) {
            if (newStatus == 'UNBLOCKED') {
                ahrefId.html('<i class="icon-ban-circle"></i> Block');
            } else {
                ahrefId.html('<i class="icon-ok-circle"></i> Unblock');
            }

        } else {
            jagg.message({content:result.message, type:"error"});
        }


    }, "json");


}