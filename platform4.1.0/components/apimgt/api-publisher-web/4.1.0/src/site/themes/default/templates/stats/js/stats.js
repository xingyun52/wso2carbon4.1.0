var t_on = {
            'apiChart':1,
            'subsChart':1,
            'serviceTimeChart':1,
            'tempLoadingSpace':1
            };
var currentLocation;

var loadErrorTable = function(){
    currentLocation =window.location.pathname;
    jagg.post("/site/blocks/stats/ajax/stats.jag", { action:"getAPIFaultyAnalyzeByTime", currentLocation:currentLocation},
              function (json) {
                  if (!json.error) {
                      var line1 = [];
                      var length = json.usage.length;
                      var allProviders = new Array();
                      for (var i = 0; i < json.usage.length; i++) {
                          //$('#apiFaultyByTimeTable').append($('<tr><td>' + json.usage[i].apiName + '</td><td>' + json.usage[i].version + '</td><td>' + json.usage[i].context + '</td><td>' + json.usage[i].requestTime + '</td></tr>'));
                          var selectedPublisher = $('#errorPublisher').val();
                          if(selectedPublisher == "all"){
                              line1.push([json.usage[i].requestTime,json.usage[i].apiName]);
                          }else if(selectedPublisher == json.usage[i].apiName.split(":")[1]){
                              line1.push([json.usage[i].requestTime,json.usage[i].apiName.split(":")[0]]);
                          }

                          var foundThisProvider = false;
                          for(var j=0;j<allProviders.length;j++){
                              if(allProviders[j]==json.usage[i].apiName.split(":")[1]){
                                foundThisProvider = true;
                              }
                          }
                          if(!foundThisProvider){
                            allProviders.push(json.usage[i].apiName.split(":")[1]);
                          }
                      }
                      $('#errorPublisher').empty();
                      $('#errorPublisher').append($('<option value="all">All</option>'))                      
                      for(var i=0;i<allProviders.length;i++){
                        $('#errorPublisher').append($('<option value="'+allProviders[i]+'">'+allProviders[i]+'</option>'))
                      }
                      if (length == 0) {

                          $('#apiFaultyByTimeChart').html('');
                          $('#apiFaultyByTimeChart').append($('<span class="label label-info">'+i18n.t('errorMsgs.checkBAMConnectivity')+'</span>'));

                      }else{
							  /*
						   ======================================================================
						   ======================================================================
						   ======================================================================
						   Creating the chart
						   */
						  var fiveMinutes = 5 * 60 * 1000;
                          var errorPublisher = $('#errorPublisher').val();
                          var errorTime = $('#errorTime').val();

						  $('#apiFaultyByTimeChart').html('');
						  /*line1 = [
							  [1353061610342,'a'],
							  [1353061618934,'b'],
							  [1353061428082,'c'],
							  [1353061436918,'a'],
							  [1353061442045,'b']
						  ];*/
						  var apiNames = new Array();
						  var minTime = line1[0][0];
						  var maxTime = line1[0][0];
						  for (var i = 0; i < line1.length; i++) {
							  var toFind = line1[i][1];
							  var found = false;
							  var foundAt = apiNames.length;

							  for (var j = 0; j < apiNames.length; j++) {
								  if (apiNames[j] == toFind) {
									  found = true;
									  foundAt = j;
								  }
							  }

							  if (!found) {
								  apiNames.push(toFind);
							  }
							  line1[i][1] = foundAt;

							  if (minTime > line1[i][0]) {
								  minTime = line1[i][0];
							  }
							  if (maxTime < line1[i][0]) {
								  maxTime = line1[i][0];
							  }

							  var date = new Date(line1[i][0]);
							  line1[i][0] = date.getFullYear() + "-" + date.getMonth() + "-" + date.getDay() + " " + date.getHours() + ":" + date.getMinutes() + ":" + date.getMilliseconds();
						  }
                          var now = new Date();
                          var nowTime = now.getTime();
                          var prevHour = nowTime - 1000*60*60;

                          if(errorTime == "last24"){
                              prevHour = nowTime - 1000*60*60*24;
                          }

                          if(prevHour < maxTime){
                            if(prevHour < maxTime && minTime < prevHour){
                                minTime =  prevHour;
                            }else if(prevHour < minTime && maxTime < now){
                                //Min max do not change
                            }

                          }else{
                              $('#apiFaultyByTimeChart').html('<div style="padding:30px"><span class="label label-info"> No data found withing the selected time</span></div>');
                              return;
                          }


						  minTime = minTime - fiveMinutes;
						  minTime = new Date(minTime);
						  minTime = minTime.getFullYear() + '-' + minTime.getMonth() + '-' + minTime.getDay() + ' ' + minTime.getHours() + ':' + minTime.getMinutes() + ":" + minTime.getMilliseconds();


						  maxTime = maxTime + fiveMinutes;
						  maxTime = new Date(maxTime);
						  maxTime = maxTime.getFullYear() + '-' + maxTime.getMonth() + '-' + maxTime.getDay() + ' ' + maxTime.getHours() + ':' + maxTime.getMinutes() + ":" + maxTime.getMilliseconds();
						  (function($) {
							  $.jqplot.LabelFormatter = function(format, val) {
								  var toReturn = apiNames[val];
								  if (toReturn == undefined) {
									  toReturn = "";
								  }
								  return toReturn;
							  };
						  })(jQuery);

						  var plot1 = $.jqplot('apiFaultyByTimeChart', [line1], {
							  title:'Time',
							  axesDefaults: {
								  labelRenderer: $.jqplot.CanvasAxisLabelRenderer
							  },
							  pointLabels: { show:true } ,
							  axes:{
								  xaxis:
								  {
									  renderer:$.jqplot.DateAxisRenderer,
                                      tickRenderer: $.jqplot.CanvasAxisTickRenderer ,
									  tickOptions:{
										  formatString: '%H:%M:%S.%#N',
                                          angle: 60,
                                          fontSize: '10pt'

									  },
									  min:minTime,
									  max:maxTime,
									  tickInterval:'5 minutes'
								  },
								  yaxis:
								  {
									  min:-1,
									  max:apiNames.length,
									  numberTicks:apiNames.length + 2,
									  pad:1,
									  tickOptions:{
										  formatString: '%d',
										  formatter: $.jqplot.LabelFormatter
									  }
								  }
							  },
                              seriesColors: [ "#ed3c3c", "#ffe03e", "#48ca48", "#49baff","#7d7dff", "#ff468b", "#de621d", "#cb68c9"],
							  seriesDefaults: {

								  /*pointLabels: {
									  show:true
								  }*/
							  },
							  series:[
								  {lineWidth:4, markerOptions:{style:'circle'},showLine:false}
							  ]
						  });

						  /*
						  ======================================================================
						  ======================================================================
						  ======================================================================
										Creating the chart
						   */
                      }



                  } else {
                      if (json.message == "AuthenticateError") {
                          jagg.showLogin();
                      } else {
                          jagg.message({content:json.message,type:"error"});
                      }
                  }
                  t_on['tempLoadingAPIFaultyByTime'] = 0;
              }, "json");
};

$(document).ready(function() {
    currentLocation=window.location.pathname;
    //Initiating the fake progress bar
    jagg.fillProgress('apiChart');jagg.fillProgress('subsChart');jagg.fillProgress('serviceTimeChart');jagg.fillProgress('tempLoadingSpace');

    jagg.post("/site/blocks/stats/ajax/stats.jag", { action:"getProviderAPIServiceTime",currentLocation:currentLocation },
             function (json) {
                 if (!json.error) {
                     var length = json.usage.length,s1 = [];
                     $('#serviceTimeChart').empty();
                     for (var i = 0; i < length; i++) {
                         var tmp = [parseFloat(json.usage[i].serviceTime),json.usage[i].apiName];
                         s1.push(tmp);
                     }

                     if (length > 0) {
                         var height = 200;
                         if (30 * length > 200) height = 30 * length;
                         $('#serviceTimeChart').height(height);
                         var plot1 = $.jqplot('serviceTimeChart', [s1], {
                             seriesDefaults: {
                                 renderer:$.jqplot.BarRenderer,
                                 pointLabels: { show: true, location: 'e', edgeTolerance: -15 },
                                 shadowAngle: 135,
                                 seriesColors: [ "#ed3c3c", "#ffe03e", "#48ca48", "#49baff","#7d7dff", "#ff468b", "#de621d", "#cb68c9"],
                                 rendererOptions: {
                                     barDirection: 'horizontal'
                                 }
                             },
                             axes: {
                                 yaxis: {
                                     renderer: $.jqplot.CategoryAxisRenderer
                                 },
                                 xaxis:{
                                     pad: 1.05,
                                     tickOptions: {formatString: '%dms'}
                                 }
                             }
                         });

                     } else {
                         $('#serviceTimeChart').css("fontSize", 14);
                         $('#serviceTimeChart').append($('<span class="label label-info">'+i18n.t('errorMsgs.checkBAMConnectivity')+'</span>'));
                     }


                 } else {
                     if (json.message == "AuthenticateError") {
                         jagg.showLogin();
                     } else {
                         jagg.message({content:json.message,type:"error"});
                     }
                 }
                 t_on['serviceTimeChart'] = 0;
             }, "json");

    jagg.post("/site/blocks/stats/ajax/stats.jag", { action:"getSubscriberCountByAPIs",currentLocation:currentLocation  },
              function (json) {
                  if (!json.error) {
                      var length = json.usage.length,data = [];
                      $('#subsChart').empty();
                      $('#subsTable').find("tr:gt(0)").remove();
                      for (var i = 0; i < length; i++) {
                          data[i] = parseFloat(json.usage[i].count);
                          data[i] = [json.usage[i].apiName, parseInt(json.usage[i].count)];
                          $('#subsTable').append($('<tr><td>' + json.usage[i].apiName + '</td><td>' + json.usage[i].count + '</td></tr>'));

                      }
                      if (length > 0) {
                          $('#subsTable').show();

                          var plot1 = jQuery.jqplot('subsChart', [data],
                                                    {
                                                        seriesDefaults:{
                                                            renderer:jQuery.jqplot.PieRenderer,
                                                            rendererOptions:{
                                                                showDataLabels:true
                                                            }
                                                        },
                                                        seriesColors: [ "#ed3c3c", "#ffe03e", "#48ca48", "#49baff","#7d7dff", "#ff468b", "#de621d", "#cb68c9"],
                                                        legend:{ show:true, location:'e' }
                                                    }
                                  );

                      } else {
                          $('#subsTable').hide();
                          $('#subsChart').css("fontSize", 14);
                          $('#subsChart').append($('<span class="label label-info">'+i18n.t('errorMsgs.checkBAMConnectivity')+'</span>'));
                      }


                  } else {
                      if (json.message == "AuthenticateError") {
                          jagg.showLogin();
                      } else {
                          jagg.message({content:json.message,type:"error"});
                      }
                  }
                  t_on['subsChart'] = 0;
              }, "json");


    jagg.post("/site/blocks/stats/ajax/stats.jag", { action:"getProviderAPIUsage",currentLocation:currentLocation  },
              function (json) {
                  if (!json.error) {
                      var length = json.usage.length,data = [];
                      $('#apiChart').empty();
                      $('#apiTable').find("tr:gt(0)").remove();
                      for (var i = 0; i < length; i++) {
                          data[i] = [json.usage[i].apiName, parseInt(json.usage[i].count)];
                          $('#apiTable').append($('<tr><td>' + json.usage[i].apiName + '</td><td>' + json.usage[i].count + '</td></tr>'));

                      }

                      if (length > 0) {
                          $('#apiTable').show();
                          var plot1 = jQuery.jqplot('apiChart', [data],
                                                    {
                                                        seriesDefaults:{
                                                            renderer:jQuery.jqplot.PieRenderer,
                                                            rendererOptions:{
                                                                showDataLabels:true
                                                            }
                                                        },
                                                        seriesColors: [ "#ed3c3c", "#ffe03e", "#48ca48", "#49baff","#7d7dff", "#ff468b", "#de621d", "#cb68c9"],
                                                        legend:{ show:true, location:'e' }
                                                    }
                                  );

                      } else {
                          $('#apiTable').hide();
                          $('#apiChart').css("fontSize", 14);
                          $('#apiChart').append($('<span class="label label-info">'+i18n.t('errorMsgs.checkBAMConnectivity')+'</span>'));
                      }


                  } else {
                      if (json.message == "AuthenticateError") {
                          jagg.showLogin();
                      } else {
                          jagg.message({content:json.message,type:"error"});
                      }
                  }
                  t_on['apiChart'] = 0;
              }, "json");


    jagg.post("/site/blocks/stats/ajax/stats.jag", { action:"getProviderAPIVersionUserLastAccess",currentLocation:currentLocation  },
              function (json) {
                  if (!json.error) {
                      $('#lastAccessTable').find("tr:gt(0)").remove();
                      var length = json.usage.length;
                      $('#lastAccessTable').show();
                      for (var i = 0; i < json.usage.length; i++) {
                          $('#lastAccessTable').append($('<tr><td>' + json.usage[i].api_name + '</td><td>' + json.usage[i].api_version + '</td><td>' + json.usage[i].user + '</td><td>' + jagg.getDate(json.usage[i].lastAccess) + '</td></tr>'));
                      }
                      if (length == 0) {
                          $('#lastAccessTable').hide();
                          $('#tempLoadingSpace').html('');
                          $('#tempLoadingSpace').append($('<span class="label label-info">'+i18n.t('errorMsgs.checkBAMConnectivity')+'</span>'));

                      }else{
                          $('#tempLoadingSpace').hide();
                      }

                  } else {
                      if (json.message == "AuthenticateError") {
                          jagg.showLogin();
                      } else {
                          jagg.message({content:json.message,type:"error"});
                      }
                  }
                  t_on['tempLoadingSpace'] = 0;
              }, "json");

    jagg.post("/site/blocks/stats/ajax/stats.jag", { action:"getAPIUsageByResourcePath", currentLocation:currentLocation},
              function (json) {
                  if (!json.error) {
                      $('#resourcePathUsageTable').find("tr:gt(0)").remove();
                      var length = json.usage.length;
                      $('#resourcePathUsageTable').show();
                      for (var i = 0; i < json.usage.length; i++) {
                          $('#resourcePathUsageTable').append($('<tr><td>' + json.usage[i].apiName + '</td><td>' + json.usage[i].version + '</td><td>' + json.usage[i].context + '</td><td>' + json.usage[i].resource + '</td><td>' + json.usage[i].count + '</td></tr>'));
                      }
                      if (length == 0) {
                          $('#resourcePathUsageTable').hide();
                          $('#tempLoadingSpaceResourcePath').html('');
                          $('#tempLoadingSpaceResourcePath').append($('<span class="label label-info">'+i18n.t('errorMsgs.checkBAMConnectivity')+'</span>'));

                      }else{
                          $('#tempLoadingSpaceResourcePath').hide();
                      }

                  } else {
                      if (json.message == "AuthenticateError") {
                          jagg.showLogin();
                      } else {
                          jagg.message({content:json.message,type:"error"});
                      }
                  }
                  t_on['tempLoadingSpaceResourcePath'] = 0;
              }, "json");

    jagg.post("/site/blocks/stats/ajax/stats.jag", { action:"getAPIResponseFaultCount", currentLocation:currentLocation},
                   function (json) {
                       if (!json.error) {
                           $('#apiFaultyTable').find("tr:gt(0)").remove();
                           var length = json.usage.length;
                           $('#apiFaultyTable').show();
                           for (var i = 0; i < json.usage.length; i++) {
                               $('#apiFaultyTable').append($('<tr><td>' + json.usage[i].apiName + '</td><td>' + json.usage[i].version + '</td><td>' + json.usage[i].count + '</td><td><span class="pull-right">' + json.usage[i].faultPercentage +'%</span></td></tr>'));
                           }
                           if (length == 0) {
                               $('#apiFaultyTable').hide();
                               $('#tempLoadingAPIFaulty').html('');
                               $('#tempLoadingAPIFaulty').append($('<span class="label label-info">'+i18n.t('errorMsgs.checkBAMConnectivity')+'</span>'));

                           }else{
                               $('#tempLoadingAPIFaulty').hide();
                           }

                       } else {
                           if (json.message == "AuthenticateError") {
                               jagg.showLogin();
                           } else {
                               jagg.message({content:json.message,type:"error"});
                           }
                       }
                       t_on['tempLoadingAPIFaulty'] = 0;
                   }, "json");

     //Initiating the fake progress bar
     jagg.fillProgress('apiChart');jagg.fillProgress('subsChart');jagg.fillProgress('serviceTimeChart');jagg.fillProgress('tempLoadingSpace');
     var currentLocation=window.location.pathname;

    jagg.post("/site/blocks/stats/ajax/stats.jag", { action:"getAPIResponseFaultCount",currentLocation:currentLocation },
                  function (json) {
                      if (!json.error) {
                          var length = json.usage.length,s1 = [];
                          $('#faultyCountChart').empty();
                          var ticks = 0;
                          for (var i = 0; i < length; i++) {
                              var tmp = [json.usage[i].apiName, parseFloat(json.usage[i].count)];
                              s1.push(tmp);
                              if(ticks<tmp[1]){
                                  ticks = tmp[1];
                              }
                          }
                          ticks++;
                          if (length > 0) {
                              /*var width = 300;
                              if (30 * length > 300) width = 35 * length;
                              $('#faultyCountChart').width(width);*/
                              var plot1 = $.jqplot('faultyCountChart', [s1], {
                                  seriesColors: [ "#ed3c3c"],
                                  seriesDefaults: {
                                      renderer:$.jqplot.BarRenderer,
                                      pointLabels: { show: true, location: 'e', edgeTolerance: -15 },
                                      shadowAngle: 135,
                                      varyBarColor:true,
                                      rendererOptions: {
                                          barDirection: 'vertical'
                                      }
                                  },
                                  axesDefaults: {
                                     tickRenderer: $.jqplot.CanvasAxisTickRenderer ,
                                     tickOptions: {
                                     angle: 60,
                                     fontSize: '10pt'
                                     }
                                  },

                                  axes: {
                                      yaxis: {
                                          min: 0,
                                          pad: 0,
                                          numberTicks:ticks,
                                          tickOptions: {
                                         angle: 0,
                                         fontSize: '10pt',
                                         formatString: '%d'
                                     }
                                      },
                                      xaxis:{
                                          renderer: $.jqplot.CategoryAxisRenderer
                                      }
                                  }
                              });

                          } else {
                              $('#faultyCountChart').css("fontSize", 14);
                              $('#faultyCountChart').append($('<span class="label label-info">'+i18n.t('errorMsgs.checkBAMConnectivity')+'</span>'));
                          }


                      } else {
                          if (json.message == "AuthenticateError") {
                              jagg.showLogin();
                          } else {
                              jagg.message({content:json.message,type:"error"});
                          }
                      }
                      t_on['faultyCountChart'] = 0;
                  }, "json");
    
    loadErrorTable();
    $('#errorPublisher').change(function(){
        $('#apiFaultyByTimeChart').html('<div class="progress progress-striped active" style="padding:30px"><div class="bar" style="width: 90%;"></div></div>');
        loadErrorTable();
    });

    $('#errorTime').change(function(){
        $('#apiFaultyByTimeChart').html('<div class="progress progress-striped active" style="padding:30px"><div class="bar" style="width: 90%;"></div></div>');
        loadErrorTable();
    });


});
