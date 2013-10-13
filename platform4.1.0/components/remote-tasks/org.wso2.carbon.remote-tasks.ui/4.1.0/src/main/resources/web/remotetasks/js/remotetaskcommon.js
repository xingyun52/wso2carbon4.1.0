function forward(destinationJSP) {
    location.href = destinationJSP;
}

function editRow(name, targetURI, taskCount, taskInterval, cronExpression, startTime, endTime) {
    document.location.href = "editTask.jsp?" + "taskName=" + name + "&targetUrl=" + targetURI +
            "&taskCount=" + taskCount + "&taskInterval=" + taskInterval  + "&cronExpression=" + 
            cronExpression + "&startTime=" + startTime + "&endTime=" + endTime + "&saveMode=edit";
}

function deleteRow(name, msg){
	CARBON.showConfirmationDialog(msg + "' " + name + " ' ?", function() {
        document.location.href = "saveTask.jsp?" + "taskName=" + name + "&saveMode=delete";
    });
}

function pauseRow(name, msg){
	CARBON.showConfirmationDialog(msg + "' " + name + " ' ?", function() {
        document.location.href = "saveTask.jsp?" + "taskName=" + name + "&saveMode=pause";
    });
}

function resumeRow(name, msg){
	CARBON.showConfirmationDialog(msg + "' " + name + " ' ?", function() {
        document.location.href = "saveTask.jsp?" + "taskName=" + name + "&saveMode=resume";
    });
}

function goBackOnePage() {
    history.go(-1);
}

function validateTaskInputs() {
    var taskName = document.getElementById('taskName').value;
    var targetUrl = document.getElementById('targetUrl').value;
    var triggerCount = document.getElementById('triggerCount').value;
    var triggerInterval = document.getElementById('triggerInterval').value;
    var cronExp = document.getElementById('cronExp').value;
    
    if (taskName == "" || taskName == null) {
        CARBON.showWarningDialog("Task name cannot be empty");
        return false;
    }
    
    if (!isNameValid(taskName)) {
        CARBON.showWarningDialog(namemsg);
        return false;
    }
    
    if (targetUrl == "" || targetUrl == null) {
        CARBON.showWarningDialog("Task target URL cannot be empty");
        return false;
    }
    
    if ((cronExp == "" || cronExp == null) && (triggerCount == "" || triggerCount == null)) {
    	CARBON.showWarningDialog("Either the trigger count or a cron expression must be given");
    	return false;
    }
    
    if (!(triggerCount == "" || triggerCount == null) && (triggerInterval == "" || triggerInterval == null)) {
    	CARBON.showWarningDialog("Task interval cannot be empty when the trigger count is given");
        return false;
    }
    
    return true;
}

function isNameValid(namestring) {
    if (namestring != null && namestring != "") {
        for (var j = 0; j < namestring.length; j++)
        {
            var ch = namestring.charAt(j);
            var code = ch.charCodeAt(0);
            if ((code > 47 && code < 59) // number
                    || (code > 64 && code < 91)// capital 
                    || (code > 96 && code < 123)// simple
                    || (code == 46) //dot
                    || (code == 95) // underscore
                    || (code == 2013) // en dash
                    || (code == 2014) // em dash
                    || (code == 45)) // minus sign - hyphen
            {
            }
            else {
                return false;
            }
        }
        return true;
    } else {
        return false;
    }
}