var esb_mediation_stats = ['ESB Mediation Statistics',[['ESB - Proxy','esb_proxy.jsp'],['ESB - Sequence','esb_sequence.jsp'],['ESB - Endpoint','esb_endpoint.jsp']]];
var as_service_stats = ['AS Service Statistics',[['Service Statistics','index.jsp']]];
var activity_monitoring = ['Activity Monitoring',[['Activity Monitoring','index.jsp']]];
var channel_monitoring = ['Mobile/Web Channel Monitoring',[['Channel Monitoring','index.jsp']]];
var jmx_stats = ['JMX Statistics',[['JMX Stats','index.jsp']]];

$(document).ready(function(){
			var dashboardurl = 'https://'+window.location.host+'/bamdashboards/';
			$.ajax({
				type: "GET",
				url: "../getDeployedToolboxes.jag",
				dataType: "text",
				success: function(text) {
					var deployedToolboxes = text;
					deployedToolboxes = deployedToolboxes.replace(/\n/g, '');
					deployedToolboxes = deployedToolboxes.split(',');
					for (var i=0; i<deployedToolboxes.length; i++){
						if(deployedToolboxes[i].toLowerCase() == "mediation_statistics_monitoring"){
							var navstring = '<li class="nav-header">'+esb_mediation_stats[0]+'</li>';
							for(var k=0;k<esb_mediation_stats[1].length;k++){
	  							navstring = navstring + '<li><a href="'+dashboardurl+'mediation_stats/'+esb_mediation_stats[1][k][1]+'">'+esb_mediation_stats[1][k][0]+'</a></li>';
							}
							$("#leftnav").append(navstring);
						}
						else if(deployedToolboxes[i].toLowerCase() == "service_statistics_monitoring"){
							var navstring = '<li class="nav-header">'+as_service_stats[0]+'</li>';
							for(var k=0;k<as_service_stats[1].length;k++){
	  							navstring = navstring + '<li><a href="'+dashboardurl+'service_stats/'+as_service_stats[1][k][1]+'">'+as_service_stats[1][k][0]+'</a></li>';
							}
							$("#leftnav").append(navstring);
						}
						else if(deployedToolboxes[i].toLowerCase() == "activity_monitoring"){
	                                                var navstring = '<li class="nav-header">'+activity_monitoring[0]+'</li>';
							for(var k=0;k<activity_monitoring[1].length;k++){
	  							navstring = navstring + '<li><a href="'+dashboardurl+'activity_monitoring/'+activity_monitoring[1][k][1]+'">'+activity_monitoring[1][k][0]+'</a></li>';
							}
							$("#leftnav").append(navstring);
	                                        }
						else if(deployedToolboxes[i].toLowerCase() == "mobile_web_channel_monitoring"){
	                                                var navstring = '<li class="nav-header">'+channel_monitoring[0]+'</li>';
							for(var k=0;k<channel_monitoring[1].length;k++){
	  							navstring = navstring + '<li><a href="'+dashboardurl+'channel-stats/'+channel_monitoring[1][k][1]+'">'+channel_monitoring[1][k][0]+'</a></li>';
							}
							$("#leftnav").append(navstring);
	                                        } else if(deployedToolboxes[i].toLowerCase() == "jmx_stats"){
                    				var navstring = '<li class="nav-header">'+jmx_stats[0]+'</li>';
                    					for(var k=0;k<jmx_stats[1].length;k++){
                        navstring = navstring + '<li><a href="'+dashboardurl+'jmx_monitoring/'+jmx_stats[1][k][1]+'">'+jmx_stats[1][k][0]+'</a></li>';
                    }
                    $("#leftnav").append(navstring);
                }
					}
				}
			});
		});
