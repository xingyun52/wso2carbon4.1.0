/**
 * Input name space
 */
function addNSPrefix() {
    var propName = document.getElementById("NSPrefix");
    var propValue = document.getElementById("NSValue");
    var propertyTable = document.getElementById("xpathNamespacesTable");
    var noXpathDiv = document.getElementById("noXpathDiv");

    var error = "";

    if (propName.value == "") {
        error = "Name field is empty.\n";
    }
    if (propValue.value == "") {
        error = "Value field is empty.\n";
    }

    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    propertyTable.style.display = "";

    //Check for duplications
    var topicNamesArr = YAHOO.util.Dom.getElementsByClassName("nsprefix-names");
    var foundDuplication = false;
    for (var i = 0; i < topicNamesArr.length; i++) {
        if (topicNamesArr[i].innerHTML == propName.value) {
            foundDuplication = true;
            CARBON.showErrorDialog("Duplicated Entry");
            return;
        }
    }
    addNSprefixesToSession(propName.value, propValue.value);

    //add new row
    var newTableRow = propertyTable.insertRow(propertyTable.rows.length);
    var newCell = newTableRow.insertCell(0);
    newCell.innerHTML = propName.value;
    YAHOO.util.Dom.addClass(newCell, "nsprefix-names");

    var newCel2 = newTableRow.insertCell(1);
    newCel2.innerHTML = propValue.value;

    var newCel3 = newTableRow.insertCell(2);
    newCel3.innerHTML = ' <a class="icon-link" style="background-image:url(../admin/images/delete.gif)" onclick="removeNSPrefix(this)">Delete</a>';

    propName.value = "";
    propValue.value = "";
    noXpathDiv.style.display = "none";
    //    showAddNSPrefix();
}

function removeNSPrefix(link) {
    var rowToRemove = link.parentNode.parentNode;
    var topicToERemove = rowToRemove.cells[0].innerHTML.trim();
    var callback =
    {
        success:function (o) {
            if (o.responseText !== undefined) {

            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };


    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_delete_property.jsp", callback, "property=" + topicToERemove + "&type=ns");

    rowToRemove.parentNode.removeChild(rowToRemove);
    CARBON.showInfoDialog("Prefix removed successfully!!");
    return;
}

/**
 * Input property
 */
function addXMLInputProperty() {
    var propName = document.getElementById("inputXMLPropName");
    var propValue = document.getElementById("inputXMLPropValue");
    var propType = document.getElementById("inputXMLPropertyTypes")[document.getElementById("inputXMLPropertyTypes").selectedIndex];
    var propertyTable = document.getElementById("inputXMLPropertyTable");
    var noPropertyDiv = document.getElementById("noInputXMLPropertyDiv");

    var error = "";

    if (propName.value == "") {
        error = "Name field is empty.\n";
    }
    if (propValue.value == "") {
        error = "Value field is empty.\n";
    }
    if (propType.value == "") {
        error = "Type field is empty.\n";
    }

    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    propertyTable.style.display = "";

    //Check for duplications
    var topicNamesArr = YAHOO.util.Dom.getElementsByClassName("property-names");
    var foundDuplication = false;
    for (var i = 0; i < topicNamesArr.length; i++) {
        if (topicNamesArr[i].innerHTML == propName.value) {
            foundDuplication = true;
            CARBON.showErrorDialog("Duplicated Entry");
            return;
        }
    }

    addInputPropertyToSession(propName.value, propType.value, propValue.value, '', 'xml');
    //add new row
    var newTableRow = propertyTable.insertRow(propertyTable.rows.length);
    var newCell = newTableRow.insertCell(0);
    newCell.innerHTML = propName.value;
    YAHOO.util.Dom.addClass(newCell, "property-names");

    var newCel2 = newTableRow.insertCell(1);
    newCel2.innerHTML = propValue.value;

    var newCel3 = newTableRow.insertCell(2);
    newCel3.innerHTML = propType.value;

    var newCel4 = newTableRow.insertCell(3);
    newCel4.innerHTML = ' <a class="icon-link" style="background-image:url(../admin/images/delete.gif)" onclick="removeInputProperty(this,\'xml\')">Delete</a>';

    propName.value = "";
    propValue.value = "";
    noPropertyDiv.style.display = "none";
    //    propType.value = "";
    //    showAddProperty();
}

function addTupleInputProperty() {
    var propName = document.getElementById("inputTuplePropName");
    var propInputName = document.getElementById("inputTuplePropInputName");
    var propDataType = document.getElementById("inputTuplePropertyDataTypes")[document.getElementById("inputTuplePropertyDataTypes").selectedIndex];
    var propType = document.getElementById("inputTuplePropertyTypes")[document.getElementById("inputTuplePropertyTypes").selectedIndex];
    var propertyTable = document.getElementById("inputTuplePropertyTable");
    var noPropertyDiv = document.getElementById("noInputTuplePropertyDiv");

    var error = "";

    if (propName.value == "") {
        error = "Name field is empty.\n";
    }
    if (propInputName.value == "") {
        error = "Input Name field is empty.\n";
    }
    if (propDataType.value == "") {
        error = "Data Type field is empty.\n";
    }
    if (propType.value == "") {
        error = "Type field is empty.\n";
    }

    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    propertyTable.style.display = "";

    //Check for duplications
    var topicNamesArr = YAHOO.util.Dom.getElementsByClassName("property-names");
    var foundDuplication = false;
    for (var i = 0; i < topicNamesArr.length; i++) {
        if (topicNamesArr[i].innerHTML == propName.value) {
            foundDuplication = true;
            CARBON.showErrorDialog("Duplicated Entry");
            return;
        }
    }

    addInputPropertyToSession(propName.value,  propType.value,propInputName.value,propDataType.value, 'tuple');
    //add new row
    var newTableRow = propertyTable.insertRow(propertyTable.rows.length);
    var newCell = newTableRow.insertCell(0);
    newCell.innerHTML = propName.value;
    YAHOO.util.Dom.addClass(newCell, "property-names");

    var newCel2 = newTableRow.insertCell(1);
    newCel2.innerHTML = propInputName.value;

    var newCel3 = newTableRow.insertCell(2);
    newCel3.innerHTML = propDataType.value;

    var newCel4 = newTableRow.insertCell(3);
    newCel4.innerHTML = propType.value;

    var newCel5 = newTableRow.insertCell(4);
    newCel5.innerHTML = ' <a class="icon-link" style="background-image:url(../admin/images/delete.gif)" onclick="removeInputProperty(this,\'tuple\')">Delete</a>';

    propName.value = "";
    propInputName.value = "";
    noPropertyDiv.style.display = "none";
    //    showAddProperty();
}
function addMapInputProperty() {
    var propName = document.getElementById("inputMapPropName");
    var propInputName = document.getElementById("inputMapPropInputName");
    var propType = document.getElementById("inputMapPropertyTypes")[document.getElementById("inputMapPropertyTypes").selectedIndex];
    var propertyTable = document.getElementById("inputMapPropertyTable");
    var noPropertyDiv = document.getElementById("noInputMapPropertyDiv");

    var error = "";

    if (propName.value == "") {
        error = "Name field is empty.\n";
    }
    if (propInputName.value == "") {
        error = "Name field is empty.\n";
    }
    if (propType.value == "") {
        error = "Type field is empty.\n";
    }

    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    propertyTable.style.display = "";

    //Check for duplications
    var topicNamesArr = YAHOO.util.Dom.getElementsByClassName("property-names");
    var foundDuplication = false;
    for (var i = 0; i < topicNamesArr.length; i++) {
        if (topicNamesArr[i].innerHTML == propName.value) {
            foundDuplication = true;
            CARBON.showErrorDialog("Duplicated Entry");
            return;
        }
    }

    addInputPropertyToSession(propName.value, propType.value, propInputName.value,'', 'map');
    //add new row
    var newTableRow = propertyTable.insertRow(propertyTable.rows.length);
    var newCell = newTableRow.insertCell(0);
    newCell.innerHTML = propName.value;
    YAHOO.util.Dom.addClass(newCell, "property-names");

    var newCel2 = newTableRow.insertCell(1);
    newCel2.innerHTML = propInputName.value;

    var newCel3 = newTableRow.insertCell(2);
    newCel3.innerHTML = propType.value;

    var newCel4 = newTableRow.insertCell(3);
    newCel4.innerHTML = ' <a class="icon-link" style="background-image:url(../admin/images/delete.gif)" onclick="removeInputProperty(this,\'tuple\')">Delete</a>';

    propName.value = "";
    propInputName.value = "";
    noPropertyDiv.style.display = "none";
    //    showAddProperty();
}

function removeInputProperty(link, type) {
    var rowToRemove = link.parentNode.parentNode;
    var propertyToERemove = rowToRemove.cells[0].innerHTML.trim();
    removePropertyFromSession(propertyToERemove, type, 'input');
    rowToRemove.parentNode.removeChild(rowToRemove);
    CARBON.showInfoDialog("Property removed successfully!!");
    return;
}

/**
 * Show Added Inputs
 */
function showAddInput() {
    var addPropertyTable = document.getElementById("addInputTable");
    if (addPropertyTable.style.display == "none") {
        addPropertyTable.style.display = "";
    } else {
        addPropertyTable.style.display = "none";
    }
}

/**
 * Show added Queries
 */
function showAddQuery() {
    var addPropertyTable = document.getElementById("addQueryTable");
    if (addPropertyTable.style.display == "none") {
        addPropertyTable.style.display = "";
    } else {
        addPropertyTable.style.display = "none";
    }
}


function addOutputTupleProperty(dataType) {
    var propName = document.getElementById("output" + dataType + "DataPropName");
    var propValueOf = document.getElementById("output" + dataType + "DataPropValueOf");
    var propType = document.getElementById("output" + dataType + "DataPropType");
    var propertyTable = document.getElementById("output" + dataType + "DataTable");
    var noPropertyDiv = document.getElementById("noOutput" + dataType + "Data");

    var error = "";

    if (propName.value == "") {
        error = "Name field is empty.\n";
    }

    if (propValueOf.value == "") {
        error = "Value Of field is empty.\n";
    }

    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    propertyTable.style.display = "";

    //Check for duplications
//    var topicNamesArr = YAHOO.util.Dom.getElementsByClassName("property-names");
//    var foundDuplication = false;
//    for (var i = 0; i < topicNamesArr.length; i++) {
//        if (topicNamesArr[i].innerHTML == propName.value) {
//            foundDuplication = true;
//            CARBON.showErrorDialog("Duplicated Entry");
//            return;
//        }
//    }


    addOutputTupleDataPropertyToSession(propName.value, propValueOf.value, propType.value, dataType);
    //add new row
    var newTableRow = propertyTable.insertRow(propertyTable.rows.length);
    var newCell0 = newTableRow.insertCell(0);
    newCell0.innerHTML = propName.value;
    YAHOO.util.Dom.addClass(newCell0, "property-names");

    var newCell1 = newTableRow.insertCell(1);
    newCell1.innerHTML = propValueOf.value;

    var newCell2 = newTableRow.insertCell(2);
    newCell2.innerHTML = propType.value;

    var newCel3 = newTableRow.insertCell(3);
    newCel3.innerHTML = ' <a class="icon-link" style="background-image:url(../admin/images/delete.gif)" onclick="removeOutputProperty(this,\'' + dataType + '\')">Delete</a>';

    propName.value = "";
    propValueOf.value = "";
    noPropertyDiv.style.display = "none";
    //    propType.value = "";
    //    showAddProperty();
}

function addOutputMapProperty() {
    var propName = document.getElementById("outputMapPropName");
    var propValueOf = document.getElementById("outputMapPropValueOf");
    var propertyTable = document.getElementById("outputMapPropertiesTable");
    var noPropertyDiv = document.getElementById("noOutputMapProperties");

    var error = "";

    if (propName.value == "") {
        error = "Name field is empty.\n";
    }

    if (propValueOf.value == "") {
        error = "Value Of field is empty.\n";
    }

    if (error != "") {
        CARBON.showErrorDialog(error);
        return;
    }
    propertyTable.style.display = "";

    //Check for duplications
//    var topicNamesArr = YAHOO.util.Dom.getElementsByClassName("property-names");
//    var foundDuplication = false;
//    for (var i = 0; i < topicNamesArr.length; i++) {
//        if (topicNamesArr[i].innerHTML == propName.value) {
//            foundDuplication = true;
//            CARBON.showErrorDialog("Duplicated Entry");
//            return;
//        }
//    }


    addOutputMapDataPropertyToSession(propName.value, propValueOf.value);
    //add new row
    var newTableRow = propertyTable.insertRow(propertyTable.rows.length);
    var newCell0 = newTableRow.insertCell(0);
    newCell0.innerHTML = propName.value;
    YAHOO.util.Dom.addClass(newCell0, "property-names");

    var newCell1 = newTableRow.insertCell(1);
    newCell1.innerHTML = propValueOf.value;

    var newCel2 = newTableRow.insertCell(2);
    newCel2.innerHTML = ' <a class="icon-link" style="background-image:url(../admin/images/delete.gif)" onclick="removeOutputProperty(this,\'' + 'map' + '\')">Delete</a>';

    propName.value = "";
    propValueOf.value = "";
    noPropertyDiv.style.display = "none";
    //    propType.value = "";
    //    showAddProperty();
}

function removeOutputProperty(link, format) {
    var rowToRemove = link.parentNode.parentNode;
    var propertyToERemove = rowToRemove.cells[0].innerHTML.trim();
    removePropertyFromSession(propertyToERemove, format, 'output');
    rowToRemove.parentNode.removeChild(rowToRemove);
    CARBON.showInfoDialog("Output Property removed successfully!!");
    return;
}

/**
 * Inputs
 */
function addNewInputToList() {

    var topicsTable = document.getElementById("topicsTable");
    document.getElementById("noInputsDiv").style.display = "none";
    topicsTable.style.display = "";

    var topicName = document.getElementById("inputTopic");
    var brokerName = document.getElementById("inputBrokerName")[document.getElementById("inputBrokerName").selectedIndex];
    var mappingStream = document.getElementById("mappingStream");

    if (!addInputToList(topicsTable.rows.length - 1, false)) {
        return;

    }
    var newTableRow = topicsTable.insertRow(topicsTable.rows.length);
    var newInputTopicCell = newTableRow.insertCell(0);
    newInputTopicCell.innerHTML = '<input type="checkbox" name="inputs"' +
        'value="' + topicName.value + '"' +
        'onclick="resetInputVars()" class="chkBox"/>' +
        '<a href="cep_input.jsp?index=' + (topicsTable.rows.length - 2) + '">' + topicName.value + '</a>';
    var newInputStreamCell = newTableRow.insertCell(1);
    newInputStreamCell.innerHTML =  mappingStream.value ;
    var newInputBrokerCell = newTableRow.insertCell(2);
    newInputBrokerCell.innerHTML =  brokerName.value ;

    YAHOO.util.Dom.addClass(newInputTopicCell, "topic-names");
    YAHOO.util.Dom.addClass(newInputStreamCell, "input-stream-names");
    YAHOO.util.Dom.addClass(newInputBrokerCell, "input-broker-names");
    clearInputFields();
    showAddInput();
}
function addOldInputToList(index) {
    if (!addInputToList(index,true)) {
        return;
    }
    clearInputFields();
}
function addInputToList(index,edit) {
    if (!document.getElementById("inputBrokerName").length > 0) {
        CARBON.showErrorDialog("Please add a broker.");
        return false;
    }
    var topicName = document.getElementById("inputTopic");
    var brokerName = document.getElementById("inputBrokerName")[document.getElementById("inputBrokerName").selectedIndex];
    var mappingType = document.getElementById("inputMappingType")[document.getElementById("inputMappingType").selectedIndex];
    var mappingStream = document.getElementById("mappingStream");
    var eventClass = document.getElementById("eventClassName")[document.getElementById("eventClassName").selectedIndex];

    if (topicName.value == "") {
        CARBON.showErrorDialog("Topic is empty");
        return false;
    }
    if (eventClass.value == "Class") {
        eventClass = document.getElementById("inputEventClass");
    }
    var reWhiteSpace = new RegExp("^[a-zA-Z0-9_/#?.*-]+$");
    // Check for white space
    if (!reWhiteSpace.test(topicName.value)) {
        CARBON.showErrorDialog("Topic name : " + topicName.value + " is not a valid topic name. " +
                               "Only alphanumeric characters, hyphens (-), stars(*)," +
                               " hash(#) ,dot(.),question mark(?)" +
                               " and underscores (_) are allowed.");
        return false;
    }

    if (brokerName.value == "") {
        CARBON.showErrorDialog("Broker is empty, Its is essential to have a broker");
        return false;
    }

    if (mappingStream.value == "") {
        CARBON.showErrorDialog("Mapping Stream is empty");
        return false;
    }
    // Check for white space
    if (!reWhiteSpace.test(mappingStream.value)) {
        CARBON.showErrorDialog("Spaces are not allowed for the Mapping Stream");
        return false;
    }
    var xpathNamespacesTable = document.getElementById("xpathNamespacesTable");
    if (xmlMapping && xpathNamespacesTable.rows.length == 0) {
        CARBON.showErrorDialog("Xpath prefixes can not be empty");
        return false;
    }

    var xmlMapping = false;
    if (document.getElementsByName("inputXMLMapping")[0].style.display != 'none') {
        xmlMapping = true;
    }
    var mapMapping = false;
    if (document.getElementsByName("inputMapMapping")[0].style.display != 'none') {
        mapMapping = true;
    }

    if (xmlMapping) {
        var inputXMLPropertiesTable = document.getElementById("inputXMLPropertyTable");
        if (xmlMapping && inputXMLPropertiesTable.rows.length == 0) {
            CARBON.showErrorDialog("Input properties can not be empty in xml mapping");
            return false;
        }
    } else if (mapMapping) {
        var inputMapPropertiesTable = document.getElementById("inputMapPropertyTable");
        if (mapMapping && inputMapPropertiesTable.rows.length == 0) {
            CARBON.showErrorDialog("Input properties can not be empty in map mapping");
            return false;
        }
    } else {
        var inputTuplePropertyTable = document.getElementById("inputTuplePropertyTable");
        if (!xmlMapping && inputTuplePropertyTable.rows.length == 0) {
            CARBON.showErrorDialog("Input properties can not be empty in tupple mapping");
            return false;
        }
    }
    //Check for duplications
    var topicNamesArr = YAHOO.util.Dom.getElementsByClassName("topic-names");
    var foundDuplication = false;
    for (var i = 0; i < topicNamesArr.length; i++) {
        if (topicNamesArr[i].innerHTML == topicName.value) {
            foundDuplication = true;
            CARBON.showErrorDialog("Duplicated Entry");
            return false;
        }
    }

    addInputTopicToSession(topicName.value, brokerName.value, mappingStream.value, index, eventClass.value, mappingType.value, edit);
    return true;
}

function clearInputFields() {
    document.getElementById("inputTopic").value = "";
    document.getElementById("mappingStream").value = "";

    document.getElementById("NSPrefix").value = "";
    document.getElementById("NSValue").value = "";
    document.getElementById("inputXMLPropName").value = "";
    document.getElementById("inputXMLPropValue").value = "";
    document.getElementById("inputTuplePropName").value = "";
    document.getElementById("inputMapPropName").value = "";

    clearDataInTable("xpathNamespacesTable");
    clearDataInTable("inputTuplePropertyTable");
    clearDataInTable("inputMapPropertyTable");
    clearDataInTable("inputXMLPropertyTable");

    document.getElementById("noInputTuplePropertyDiv").style.display = "";
    document.getElementById("noInputMapPropertyDiv").style.display = "";
    document.getElementById("noInputXMLPropertyDiv").style.display = "";
    document.getElementById("noXpathDiv").style.display = "";
}

/**
 * Queries
 */
function addNewQueriesToList() {

    var queriesTable = document.getElementById("queriesTable");
    var queryName = document.getElementById("queryName");
    var topicName = document.getElementById("newTopic");
    var brokerName = document.getElementById("outputBrokerName")[document.getElementById("outputBrokerName").selectedIndex]
    if(!addQueryToList(queriesTable.rows.length - 1,false)){
        return;
    }

    document.getElementById("noQueryDiv").style.display = "none";
    queriesTable.style.display = "";

    var newTableRow = queriesTable.insertRow(queriesTable.rows.length);
    var newQueryNameCell = newTableRow.insertCell(0);
    newQueryNameCell.innerHTML = '<input type="checkbox" name="newQueries"' +
                        'value="' + queryName.value + '"' +
                        'onclick="resetVars()" class="chkBox"/>' +
                        '<a href="cep_query.jsp?index=' + (queriesTable.rows.length - 2) + '">' + queryName.value + '</a>';

    var newQueryTopicCell = newTableRow.insertCell(1);
    newQueryTopicCell.innerHTML =  topicName.value ;

    var newQueryBrokerCell = newTableRow.insertCell(2);
    newQueryBrokerCell.innerHTML =  brokerName.value ;

    YAHOO.util.Dom.addClass(newQueryNameCell, "query-names");
    YAHOO.util.Dom.addClass(newQueryTopicCell, "query-topic-names");
    YAHOO.util.Dom.addClass(newQueryBrokerCell, "query-broker-names");
    clearOutputFields();
    showAddQuery();
}

function addOldQueriesToList(index) {
    if(!addQueryToList(index,true)){
        return;
    }
    clearOutputFields();
}

function addQueryToList(index,edit) {
    if (!document.getElementById("outputBrokerName").length > 0) {
        CARBON.showErrorDialog("Please add a broker.");
        return false;
    }
    var query = document.getElementById("querySource");
    var type = "inline";

    var queryName = document.getElementById("queryName");
    var topicName = document.getElementById("newTopic");
    var brokerName = document.getElementById("outputBrokerName")[document.getElementById("outputBrokerName").selectedIndex];
    var outputMapping = document.getElementById("outputMapping")[document.getElementById("outputMapping").selectedIndex];

    var xmlMappingText = document.getElementById("xmlSourceText");
    var textMappingText = document.getElementById("textSourceText");

    if (queryName.value == "") {
        CARBON.showErrorDialog("Query Name is empty");
        return false;
    }

    var reWhiteSpace = new RegExp("^[a-zA-Z0-9_]+$");
    // Check for white space
    if (!reWhiteSpace.test(queryName.value)) {
        CARBON.showErrorDialog("Spaces are not allowed for the query Name");
        return false;
    }

    if (query.value == "") {
        CARBON.showErrorDialog("Query is empty.\n");
        return false;
    }


    /*if (topicName.value == "") {
     CARBON.showErrorDialog("Topic is empty");
     return;
     }*/
    addQueryToSession(type, queryName.value, query.value, topicName.value, brokerName.value, outputMapping.value, xmlMappingText.value, textMappingText.value, index,edit);
    return true;
}

function clearOutputFields() {
    document.getElementById("queryName").value = "";
    document.getElementById("newTopic").value = "";
    document.getElementById("xmlSourceText").value = "";
    document.getElementById("textSourceText").value = "";
    document.getElementById("querySource").value = "";

    clearDataInTable("outputMetaDataTable");
    clearDataInTable("outputCorrelationDataTable");
    clearDataInTable("outputPayloadDataTable");

    document.getElementById("noOutputMetaData").style.display = "";
    document.getElementById("noOutputCorrelationData").style.display = "";
    document.getElementById("noOutputPayloadData").style.display = "";
}

function finishAddBucketWizard() {
    var bucketName = document.getElementById("bucketName");
    var bucketDescription = document.getElementById("bucketDescription");
    var engineProvider = document.getElementById("engineProviders")[document.getElementById("engineProviders").selectedIndex];
    var providerConfigNames = document.getElementById("providerConfig::" + engineProvider.value + '::Names');
    var providerConfigValues = "";
    if (providerConfigNames.value != "") {
        var providerConfigNamesArray = providerConfigNames.value.substr(2).split('::');
        for (var i = 0; i < providerConfigNamesArray.length; i++) {
            if(providerConfigNamesArray[i]=="siddhi.enable.distributed.processing"){//siddhi.persistence.snapshot.time.interval.minutes
                if(document.getElementById("providerConfig::" + engineProvider.value + '::'+providerConfigNamesArray[i]).value==undefined||document.getElementById("providerConfig::" + engineProvider.value + '::'+providerConfigNamesArray[i]).value==""){
                    providerConfigValues = providerConfigValues + "-__-"+providerConfigNamesArray[i]+ "-_-"+ "false";
                }   else{
                    providerConfigValues = providerConfigValues + "-__-"+providerConfigNamesArray[i]+ "-_-"+ document.getElementById("providerConfig::" + engineProvider.value + '::'+providerConfigNamesArray[i]).value;
                }
            } else if(providerConfigNamesArray[i]=="siddhi.persistence.snapshot.time.interval.minutes"){
                if(document.getElementById("providerConfig::" + engineProvider.value + '::'+providerConfigNamesArray[i]).value==undefined||document.getElementById("providerConfig::" + engineProvider.value + '::'+providerConfigNamesArray[i]).value==""){
                    providerConfigValues = providerConfigValues + "-__-"+providerConfigNamesArray[i]+ "-_-"+ "0";
                }   else{
                    providerConfigValues = providerConfigValues + "-__-"+providerConfigNamesArray[i]+ "-_-"+ document.getElementById("providerConfig::" + engineProvider.value + '::'+providerConfigNamesArray[i]).value;
                }
            }  else{
                providerConfigValues = providerConfigValues + "-__-"+providerConfigNamesArray[i]+ "-_-"+ document.getElementById("providerConfig::" + engineProvider.value + '::'+providerConfigNamesArray[i]).value;
            }
        }
        providerConfigValues=providerConfigValues.substr(4);
    }
    if (bucketName.value == "") {
        CARBON.showErrorDialog("Bucket name is mandatory");
        return;
    }
    if (engineProvider.value == "") {
        CARBON.showErrorDialog("Engine provider is mandatory");
        return;
    }
    var reWhiteSpace = new RegExp("^[a-zA-Z0-9_]+$");
    // Check for white space
    if (!reWhiteSpace.test(bucketName.value)) {
        CARBON.showErrorDialog("Spaces are not allowed for the bucket Name");
        return;
    }

    var queriesTable = document.getElementById("queriesTable");
    if (queriesTable.rows.length == 0) {
        CARBON.showErrorDialog(" Add at least one query to proceed");
        return;
    }
    //make the ajax call (YUI)
    var sUrl = "cep_add_bucket_ajaxprocessor.jsp";

    var callback =
    {
        success:function (o) {  // success call back
            if (o.responseText !== undefined) {

                if (o.responseText.indexOf("Error") != -1) {
                    CARBON.showErrorDialog("" + o.responseText, function () {
                        // Nothing is doing if the adding query operation has got an error
//                                    location.href = 'cep_deployed_buckets.jsp';
                    });
                } else {
                    CARBON.showInfoDialog("" + o.responseText, function () {
                        location.href = 'cep_deployed_buckets.jsp';
                    }, function () {
                        location.href = 'cep_deployed_buckets.jsp';
                    });
                }
            }
        },
        failure:function (o) { //failure call back
            if (o.responseText !== undefined) {
                CARBON.showErrorDialog("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', sUrl, callback, "bucketName=" + bucketName.value + "&bucketDescription=" + bucketDescription.value + "&engineProvider=" + engineProvider.value +"&engineProviderConfig=" + providerConfigValues + "&data="); //This is where actually do the call

}

function finishEditBucketWizard() {
    var queriesTable = document.getElementById("queriesTable");
    if (queriesTable.rows.length == 0) {
        CARBON.showErrorDialog(" Add at least one query to proceed");
        return;
    }
    //make the ajax call (YUI)
    var sUrl = "cep_edit_bucket_ajaxprocessor.jsp";
    var handleSuccess = function (o) {  // success call back
        if (o.responseText !== undefined) {
            if (o.responseText.indexOf("Error") != -1) {
                CARBON.showErrorDialog("" + o.responseText, function () {
                    location.href = 'cep_deployed_buckets.jsp';
                }, function () {
                    location.href = 'cep_deployed_buckets.jsp';
                });
            } else {
                CARBON.showInfoDialog("" + o.responseText, function () {
                    location.href = 'cep_deployed_buckets.jsp';
                });
            }
        }
    };

    var handleFailure = function (o) { //failure call back
        if (o.responseText !== undefined) {
            alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
        }
    };

    var callback =
    {
        success:handleSuccess,
        failure:handleFailure
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', sUrl, callback, "data="); //This is where actually do the call
}


function addInputPropertyToSession(propName, propType, propValue1, propValue2,   format) {
    var callback =
    {
        success:function (o) {
            if (o.responseText !== undefined) {

            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_add_input_property.jsp", callback, "propName=" + propName +  "&propType=" + propType +"&propValue1=" + propValue1+"&propValue2=" + propValue2 + "&format=" + format);

}

function addOutputTupleDataPropertyToSession(propName, valueOf, type ,dataType) {
    var callback =
    {
        success:function (o) {
            if (o.responseText !== undefined) {

            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_add_output_mapping_property.jsp", callback, "propName=" + propName + "&valueOf=" + valueOf + "&type=" + type + "&dataType=" + dataType + "&format=tuple");

}

function addOutputMapDataPropertyToSession(propName,valueOf) {
    var callback =
    {
        success:function (o) {
            if (o.responseText !== undefined) {

            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_add_output_mapping_property.jsp", callback, "propName=" + propName + "&valueOf=" + valueOf + "&format=map");

}

function addNSprefixesToSession(prefix, nameSpace) {
    var callback =
    {
        success:function (o) {
            if (o.responseText !== undefined) {
            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_add_input_name_space_ajaxprocessor.jsp", callback, "prefix=" + prefix + "&nameSpace=" + nameSpace);

}

function addInputTopicToSession(topic, brokerName, mappingStream, tableIndex, eventClassName, mappingType, edit) {
    var callback =
    {
        success:function (o) {
            if(edit){
            location.href = 'cep_buckets.jsp?';
            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_add_input.jsp", callback, "topic=" + topic + "&brokerName=" + brokerName + "&mappingStream=" + mappingStream + "&tableIndex=" + tableIndex + "&queryEventType=" + eventClassName + "&mappingType=" + mappingType);

}

function addOutputMappingPropertyToSession(propName, propXMLFieldName, propXMLFieldType) {
    var callback =
    {
        success:function (o) {
            if (o.responseText !== undefined) {
            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };


    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_add_output_mapping_property.jsp", callback, "propName=" + propName + "&propXMLFieldName=" + propXMLFieldName + "&propXMLFieldType=" + propXMLFieldType + "&format=element");
}

function addQueryToSession(type, queryName, sourceText, outputTopic, brokerName, outputMapping, xmlMappingText, textMappingText, tableIndex, edit) {
    var callback =
    {
        success:function (o) {
            if (edit) {
                location.href = 'cep_buckets.jsp';
            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };


    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_add_query.jsp", callback, "type=" + type + "&queryName=" + queryName + "&sourceText=" + sourceText + "&outputTopic=" + outputTopic + "&brokerName=" + brokerName + "&xmlMappingText=" + xmlMappingText + "&textMappingText=" + textMappingText + "&tableIndex=" + tableIndex + "&outputMapping=" + outputMapping);
}


function removePropertyFromSession(property, format, type) {
    var callback =
    {
        success:function (o) {
            if (o.responseText !== undefined) {
            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_delete_property.jsp", callback, "property=" + property + "&type=" + type + "&format=" + format);

}

function goBack() {
    var callback =
    {
        success:function (o) {
            location.href = 'cep_buckets.jsp';
            if (o.responseText !== undefined) {
            }
        },
        failure:function (o) {
            if (o.responseText !== undefined) {
                alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
            }
        }
    };
    var request = YAHOO.util.Connect.asyncRequest('POST', "cep_clear_property_sessions_ajaxprocessor.jsp", callback, "");

}

/**
 * Utils
 */
function clearDataInTable(tableName) {
    deleteTableRows(tableName, true);
    document.getElementById(tableName).style.display = "none";
}

function deleteTableRows(tl, keepHeader) {
    if (typeof(tl) != "object") {
        tl = document.getElementById(tl);

    }
    //debugger;
    for (var i = tl.rows.length; tl.rows.length > 0; i--) {
        if (tl.rows.length > 1) {
            tl.deleteRow(tl.rows.length - 1);
        }
        if (tl.rows.length == 1) {
            if (!keepHeader) {
                tl.deleteRow(0);
            }
            return;
        }
    }

}

function setInputMapping() {
    var inputMappingElement = document.getElementById("inputMappingType");
    var selectedType = inputMappingElement[inputMappingElement.selectedIndex].value;
    populateElementDisplay(document.getElementsByName("inputXMLMapping"), "none");
    populateElementDisplay(document.getElementsByName("inputTupleMapping"), "none");
    populateElementDisplay(document.getElementsByName("inputMapMapping"), "none");
    if (selectedType == "xml") {
        populateElementDisplay(document.getElementsByName("inputXMLMapping"), "");
    } else if (selectedType == "tuple") {
        populateElementDisplay(document.getElementsByName("inputTupleMapping"), "");
    } else if (selectedType == "map") {
        populateElementDisplay(document.getElementsByName("inputMapMapping"), "");
    }
}


function setBackendRuntimeConfigurations() {
    var backendRuntimeConfigurationElement = document.getElementById("engineProviders");
    var selectedType = backendRuntimeConfigurationElement[backendRuntimeConfigurationElement.selectedIndex].value;
    populateElementDisplay(document.getElementsByName("SiddhiCEPRuntime"), "none");
    if (selectedType == "SiddhiCEPRuntime") {
        populateElementDisplay(document.getElementsByName("SiddhiCEPRuntime"), "");
    }
}


function setEventClass() {
    var inputEventClass = document.getElementById("eventClassName");
    var selectedType = inputEventClass[inputEventClass.selectedIndex].value;
    populateElementDisplay(document.getElementsByName("inputEventClass"), "none");
    if (selectedType == "Class") {
        populateElementDisplay(document.getElementsByName("inputEventClass"), "");
    }
}

function setOutputMapping() {
    var outputMappingElement = document.getElementById("outputMapping");
    var selectedType = outputMappingElement[outputMappingElement.selectedIndex].value;
    populateElementDisplay(document.getElementsByName("outputXMLMapping"), "none");
    populateElementDisplay(document.getElementsByName("outputTextMapping"), "none");
    populateElementDisplay(document.getElementsByName("outputElementMapping"), "none");
    populateElementDisplay(document.getElementsByName("outputTupleMapping"), "none");
    populateElementDisplay(document.getElementsByName("outputMapMapping"), "none");
    if (selectedType == "xml") {
        populateElementDisplay(document.getElementsByName("outputXMLMapping"), "");
    } else if (selectedType == "text") {
        populateElementDisplay(document.getElementsByName("outputTextMapping"), "");
    } else if (selectedType == "element") {
        populateElementDisplay(document.getElementsByName("outputElementMapping"), "");
    } else if (selectedType == "tuple") {
        populateElementDisplay(document.getElementsByName("outputTupleMapping"), "");
    } else if (selectedType == "map") {
        populateElementDisplay(document.getElementsByName("outputMapMapping"), "");
    }
}

function populateElementDisplay(elements, display) {
    for (var i = 0; i < elements.length; i++) {
        elements[i].style.display = display;
    }
}

