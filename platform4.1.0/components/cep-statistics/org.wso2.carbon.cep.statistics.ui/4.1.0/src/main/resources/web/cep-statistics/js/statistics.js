/*
 statistics.js contains scripts pertaining to handle @server_short_name@ statistics data
 */

// CEP
var graphCepRequest;
var graphCepResponse;
var graphMainRequest;
var graphMainResponse;
var graphTopicRequest= {};
var graphTopicResponse= {};


function initStats(cepXScale) {
    if (cepXScale != null) {
        initCepGraphs(cepXScale);
    }
}

function initSubStats(mainXScale,topicXScale,topicSimpleNames) {
    if (mainXScale != null) {
        initMainGraphs(mainXScale);
    }
    if (topicXScale != null) {
        initTopicGraphs(topicXScale,topicSimpleNames);
    }
}

function isNumeric(sText){
    var validChars = "0123456789.";
    var isNumber = true;
    var character;
    for (var i = 0; i < sText.length && isNumber == true; i++) {
        character = sText.charAt(i);
        if (validChars.indexOf(character) == -1) {
            isNumber = false;
        }
    }
    return isNumber;
}

function initCepGraphs(cepXScale) {
    if (cepXScale < 1 || !isNumeric(cepXScale)) {
        return;
    }
    graphCepRequest = new carbonGraph(cepXScale);
    graphCepResponse = new carbonGraph(cepXScale);
}

function initMainGraphs(mainXScale) {
    if (mainXScale < 1 || !isNumeric(mainXScale)) {
        return;
    }
    graphMainRequest = new carbonGraph(mainXScale);
    graphMainResponse = new carbonGraph(mainXScale);
}

function initTopicGraphs(topicXScale,topicSimpleNames) {
    if (topicXScale < 1 || !isNumeric(topicXScale)) {
        return;
    }
    for(var i=0;i<topicSimpleNames.length;i++){
        graphTopicRequest[topicSimpleNames[i]] = new carbonGraph(topicXScale);
        graphTopicResponse[topicSimpleNames[i]] = new carbonGraph(topicXScale);
    }

}




