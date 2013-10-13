
function entitlementMediatorValidate(){
    if(document.getElementById('remoteServiceUrl').value == ''){
        CARBON.showWarningDialog(enti18n["valid.remoteservice.required"]);
        return false;
    }
    if(document.getElementById('remoteServiceUserName').value == ''){
        CARBON.showWarningDialog(enti18n["valid.remoteservice.user.required"]);
        return false;
    }
    if(document.getElementById('remoteServicePassword').value == ''){
        CARBON.showWarningDialog(enti18n["valid.remoteservice.password.required"]);
        return false;
    }
    return true;
}

function displaySetProperties(isDisply) {
    var toDisplayElement;
    displayElement("mediator.property.action_row", isDisply);
    displayElement("mediator.property.value_row", isDisply);
    toDisplayElement = document.getElementById("mediator.namespace.editor");
    if (toDisplayElement != null) {
        if (isDisply) {
            toDisplayElement.style.display = '';
        } else {
            toDisplayElement.style.display = 'none';
        }
    }
}

function displayElement(elementId, isDisplay) {
    var toDisplayElement = document.getElementById(elementId);
    if (toDisplayElement != null) {
        if (isDisplay) {
            toDisplayElement.style.display = '';
        } else {
            toDisplayElement.style.display = 'none';
        }
    }
}

function createNamespaceEditor(elementId, id, prefix, uri) {
    var ele = document.getElementById(elementId);
    if (ele != null) {
        var createEle = document.getElementById(id);
        if (createEle != null) {
            if (createEle.style.display == 'none') {
                createEle.style.display = '';
            } else {
                createEle.style.display = 'none';
            }
        } else {
            ele.innerHTML = '<div id=\"' + id + '\">' +
                            '<table><tbody><tr><td>Prefix</td><td><input width="80" type="text" id=\"' + prefix + '\"+ ' +
                            'name=\"' + prefix + '\" value=""/></td></tr><tr><td>URI</td><td><input width="80" ' +
                            'type="text" id=\"' + uri + '\"+ name=\"' + uri + '\"+ value=""/></td></tr></tbody></table></div>';
        }
    }
}
