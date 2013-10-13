$(function () {
    triggerCollect();
    $("#from-date").focusout(function(){
        triggerCollect();
    });
    $("#from-time").focusout(function(){
        triggerCollect();
    });
    $("#to-date").focusout(function(){
        triggerCollect();
    });
    $("#to-time").focusout(function(){
        triggerCollect();
    });


    /*
    $("#server-dd").change(function(){
	    var selectedServer = $("#server-dd option:selected").text();
		$("#service-dd").find('option').remove();
		$("#operation-dd").find('option').remove();
		if(selectedServer==''){
			triggerCollect();
		}
		else{
			populateServicesCombo(selectedServer);		
		}
	});
    $("#service-dd").change(function(){
	    var selectedServer = $("#server-dd option:selected").text();
        	var selectedService = $("#service-dd option:selected").text();
		$("#operation-dd").find('option').remove();
		if(selectedService==''){
        		triggerCollect();
		}
		else{
			populateOperationsCombo(selectedServer,selectedService);		
		}
	});
	$("#operation-dd").change(function(){
		triggerCollect();	
	});
    //$("#service-dd").ufd({log:true});
    //$("#operation-dd").ufd({log:true});*/
    $("#clearSelectionBtn").click(function(){
        /*$("#server-dd option:first-child").attr("selected", "selected");
	    $("#service-dd").find('option').remove();
        $("#operation-dd").find('option').remove();
	    triggerCollect();
        $("#service-dd").find('option').remove();
        $("#operation-dd").find('option').remove();*/

        $("#from-date").val('');
        $("#from-time").val('');
        $("#to-date").val('');
        $("#to-time").val('');
	    triggerCollect();
    });
    /*$("#timely-dd button").click(function(){
        $("#timely-dd button").removeClass('btn-primary');
        $(this).addClass('btn-primary');
        triggerCollect();
    });*/

});
function triggerCollect(){
        var selectedServer = $("#server-dd").find('option:selected').text();
        var selectedService = $("#service-dd").find('option:selected').text();
        var selectedOperation = $("#operation-dd").find('option:selected').text();
        var timeGrouping = $("#timely-dd button.btn-primary").text();

        var fromDate = $("#from-date").val();
        var fromTime = $("#from-time").val();
        var toDate = $("#to-date").val();
        var toTime = $("#to-time").val();

        reloadIFrame({server:selectedServer,service:selectedService,operation:selectedOperation,
                         timeGroup:timeGrouping,fromDate:fromDate,fromTime:fromTime,toDate:toDate,toTime:toTime});
};
function reloadIFrame(param){
    var params = param || {};
    var server = param.server || "";
    var service = param.service || "";
    var operation = param.operation || "";
    var t = param.timeGroup || "";
    var fromDate = param.fromDate || "";
    var fromTime = param.fromTime || "";
    var toDate = param.toDate || "";
    var toTime = param.toTime || "";
    $("iframe").each(function(){
        //var id = $(this).attr('id');
        var currentUrl = $(this).attr('src');
        if(currentUrl.indexOf('?')){
            var absUrl = currentUrl.split('?');
            currentUrl = absUrl[0];
        }
        var newUrl = currentUrl + "?server=" + encodeURI(server) + "&service=" + encodeURI(service) +
                     "&opr=" + encodeURI(operation) + "&t=" + t + "&fromDate=" + encodeURI(fromDate) +
                     "&fromTime=" + encodeURI(fromTime) + "&toDate=" + encodeURI(toDate) + "&toTime=" +
                     encodeURI(toTime) + "";
        $(this).attr('src',newUrl);
    });
};
function populateCombo(id,data){
	
}
/*$(document).ready(function(){
	$.ajax({
       		url:'populate_combos_ajaxprocessor.jag',
		dataType:'json', 
		success:function(result){
			
			var options = "<option value='__default__'></option>";
			for(var i=0;i<result.length;i++){
				var data = result[i];
				for(var key in data){
					options = options + "<option>"+data[key]+"</option>"
				}
			}
            $("#server-dd").find('option').remove();
            $("#server-dd").append(options);
		    //$("#server-dd").ufd({log:true,addEmphasis: true});
  	    }
		
	});
    *//*$.getJSON("populate_combos_ajaxprocessor.jag?server=10.150.3.174:9443",
        function(data){
          alert(data);
        });*//*
});*/
function populateServicesCombo(server){
     $.ajax({
       		url:'populate_combos_ajaxprocessor.jag?server='+server+'',
		dataType:'json',
		success:function(result){

			var options = "<option value='__default__'></option>";
			for(var i=0;i<result.length;i++){
				var data = result[i];
				for(var key in data){
					options = options + "<option>"+data[key]+"</option>"
				}
			}
            
            $("#service-dd").append(options);
		    triggerCollect();//$("#service-dd").ufd({log:true,addEmphasis: true});
  	    }
	

	});
	
};
function populateOperationsCombo(server,service){

     $.ajax({
       		url:'populate_combos_ajaxprocessor.jag?server='+server+'&service='+service+'',
		    dataType:'json',
		success:function(result){

			var options = "<option value='__default__'></option>";
			for(var i=0;i<result.length;i++){
				var data = result[i];
				for(var key in data){
					if(data[key]!==null){
						options = options + "<option>"+data[key]+"</option>";
					}
				}
			}
            
            $("#operation-dd").append(options);
		triggerCollect();    //$("#operation-dd").ufd({log:true,addEmphasis: true});
  	    }

	 });
	
};
