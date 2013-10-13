var redirectToDashboard = function(){ 
			$.ajax({
				type: "GET",
				url: "getDeployedToolboxes.jag",
				dataType: "text",
				success: function(text) {
					var deployedToolboxes = text;
					deployedToolboxes = deployedToolboxes.replace(/\n/g, '');
					deployedToolboxes = deployedToolboxes.split(',');
					for (var i=0; i<deployedToolboxes.length; i++){
						if(deployedToolboxes[i].toLowerCase() == "mediation_statistics_monitoring"){
							location.href = "mediation_stats/esb_proxy.jsp";
							break;
						}
						else if(deployedToolboxes[i].toLowerCase() == "service_statistics_monitoring"){
							location.href = "service_stats/index.jsp";
							break;
						}
						else if(deployedToolboxes[i].toLowerCase() == "activity_monitoring"){
	                                                location.href = "activity_monitoring/index.jsp";
							break;
	                                        }else if(deployedToolboxes[i].toLowerCase() == "mobile_web_channel_monitoring"){
	                                                location.href = "channel-stats/index.jsp";
							break;
	                                        }   else if(deployedToolboxes[i].toLowerCase() == "jmx_stats"){
                   					 location.href = "jmx_monitoring/index.jsp";
                    					break;
                				}
					}
				}
			});
};
		
