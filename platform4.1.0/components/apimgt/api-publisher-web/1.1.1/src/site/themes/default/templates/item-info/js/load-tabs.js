var t_on = {
            'versionChart':1,
            'versionUserChart':1,
            'userVersionChart':1,
            'userChart':1
            };

var getLastAccessTime = function(name) {
    var lastAccessTime = null;
    var provider = $("#item-info #spanProvider").text();
    jagg.syncPost("/site/blocks/stats/ajax/stats.jag", { action:"getProviderAPIVersionUserLastAccess",provider:provider,mode:'browse' },
                  function (json) {
                      if (!json.error) {
                          var length = json.usage.length;
                          for (var i = 0; i < length; i++) {
                              if (json.usage[i].api_name == name) {
                                  lastAccessTime = json.usage[i].lastAccess + " (Accessed version: " + json.usage[i].api_version + ")";
                                  break;
                              }
                          }
                      } else {
                          if (json.message == "AuthenticateError") {
                              jagg.showLogin();
                          } else {
                              jagg.message({content:json.message,type:"error"});
                          }
                      }
                  });
    return lastAccessTime;
};

var getResponseTime = function(name) {
    var responseTime = null;
    var provider = $("#item-info #spanProvider").text();
    jagg.syncPost("/site/blocks/stats/ajax/stats.jag", { action:"getProviderAPIServiceTime",provider:provider,mode:'browse'},
                  function (json) {
                      if (!json.error) {
                          var length = json.usage.length;
                          for (var i = 0; i < length; i++) {
                              if (json.usage[i].apiName == name) {
                                  responseTime = json.usage[i].serviceTime + " ms";
                                  break;
                              }
                          }
                      } else {
                          if (json.message == "AuthenticateError") {
                              jagg.showLogin();
                          } else {
                              jagg.message({content:json.message,type:"error"});
                          }
                      }
                  });
    return responseTime;
};


$(document).ready(function() {
    if (($.cookie("selectedTab") != null)) {
        var tabLink = $.cookie("selectedTab");
        $('#' + tabLink + "Link").tab('show');
        //$.cookie("selectedTab", null);
        pushDataForTabs(tabLink);
    }

    $('a[data-toggle="tab"]').on('shown', function (e) {
        jagg.sessionAwareJS({callback:function(){
            var clickedTab = e.target.href.split('#')[1];
            ////////////// edit tab
            pushDataForTabs(clickedTab);
            $.cookie("selectedTab",clickedTab);
        }});

    });
    // Converting dates from timestamp to date string
    jagg.printDate();
});
var t_on = {
            'versionChart':1,
            'versionUserChart':1
            };
function pushDataForTabs(clickedTab){
     if (clickedTab == "versions") {

            jagg.fillProgress('versionChart');jagg.fillProgress('versionUserChart');
            var apiName = $("#infoAPIName").val();
            var version = $("#infoAPIVersion").val();
            var provider = $("#item-info #spanProvider").text();
            jagg.post("/site/blocks/usage/ajax/usage.jag", { action:"getProviderAPIVersionUsage", provider:provider,apiName:apiName },
                      function (json) {
                          if (!json.error) {
                              var length = json.usage.length,data = [];
                              $('#versionChart').empty();
                              $('#versionTable').find("tr:gt(0)").remove();
                              for (var i = 0; i < length; i++) {
                                  data[i] = [json.usage[i].version, parseInt(json.usage[i].count)];
                                  $('#versionTable').append($('<tr><td>' + json.usage[i].version + '</td><td>' + json.usage[i].count + '</td></tr>'));

                              }

                              if (length > 0) {
                                  $('#versionTable').show();
                                  var plot1 = jQuery.jqplot('versionChart', [data],
                                                            {
                                                                seriesDefaults:{
                                                                    // Make this a pie chart.
                                                                    renderer:jQuery.jqplot.PieRenderer,
                                                                    rendererOptions:{
                                                                        // Put data labels on the pie slices.
                                                                        // By default, labels show the percentage of the slice.
                                                                        showDataLabels:true
                                                                    }
                                                                },
                                                                seriesColors: [ "#ed3c3c", "#ffe03e", "#48ca48", "#49baff","#7d7dff", "#ff468b", "#de621d", "#cb68c9"],
                                                                legend:{ show:true, location:'e' }
                                                            }
                                          );
                              } else {
                                  $('#versionTable').hide();
                                  $('#versionChart').css("fontSize", 14);
                                  $('#versionChart').append($('<span class="label label-info">'+i18n.t("errorMsgs.noData")+'</span>'));
                              }

                          } else {
                              if (json.message == "AuthenticateError") {
                                  jagg.showLogin();
                              } else {
                                  jagg.message({content:json.message,type:"error"});
                              }
                          }
                          t_on['versionChart'] = 0;
                      }, "json");


            jagg.post("/site/blocks/usage/ajax/usage.jag", { action:"getSubscriberCountByAPIVersions", provider:provider,apiName:apiName },
                      function (json) {
                          if (!json.error) {
                              var length = json.usage.length,data = [];
                              $('#versionUserChart').empty();
                              $('#versionUserTable').find("tr:gt(0)").remove();
                              for (var i = 0; i < length; i++) {
                                  data[i] = [json.usage[i].apiVersion, parseInt(json.usage[i].count)];
                                  $('#versionUserTable').append($('<tr><td>' + json.usage[i].apiVersion + '</td><td>' + json.usage[i].count + '</td></tr>'));
                              }
                              if (length > 0) {
                                  $('#versionUserTable').show();
                                  var plot1 = jQuery.jqplot('versionUserChart', [data],
                                                            {
                                                                seriesDefaults:{
                                                                    // Make this a pie chart.
                                                                    renderer:jQuery.jqplot.PieRenderer,
                                                                    rendererOptions:{
                                                                        // Put data labels on the pie slices.
                                                                        // By default, labels show the percentage of the slice.
                                                                        showDataLabels:true
                                                                    }
                                                                },
                                                                seriesColors: [ "#ed3c3c", "#ffe03e", "#48ca48", "#49baff","#7d7dff", "#ff468b", "#de621d", "#cb68c9"],
                                                                legend:{ show:true, location:'e' }
                                                            }
                                          );
                              } else {
                                  $('#versionUserTable').hide();
                                  $('#versionUserChart').css("fontSize", 14);
                                  $('#versionUserChart').append($('<span class="label label-info">'+i18n.t('errorMsgs.noData')+'</span>'));
                              }

                          } else {
                              if (json.message == "AuthenticateError") {
                                  jagg.showLogin();
                              } else {
                                  jagg.message({content:json.message,type:"error"});
                              }
                          }
                          t_on['versionUserChart'] = 0;
                      }, "json");

        }

        if (clickedTab == "users") {
            jagg.fillProgress('userVersionChart');jagg.fillProgress('userChart');
            var name = $("#infoAPIName").val();
            var version = $("#infoAPIVersion").val();
            var provider = $("#item-info #spanProvider").text();
            jagg.post("/site/blocks/usage/ajax/usage.jag", { action:"getProviderAPIUserUsage", provider:provider,apiName:name },
                      function (json) {
                          if (!json.error) {
                              var length = json.usage.length,data = [];
                              $('#userChart').empty();
                              $('#userTable').find("tr:gt(0)").remove();
                              for (var i = 0; i < length; i++) {
                                  data[i] = [json.usage[i].user, parseInt(json.usage[i].count)];
                                  $('#userTable').append($('<tr><td>' + json.usage[i].user + '</td><td>' + json.usage[i].count + '</td></tr>'));

                              }

                              if (length > 0) {
                                  $('#userTable').show();
                                  var plot1 = jQuery.jqplot('userChart', [data],
                                                            {
                                                                seriesDefaults:{
                                                                    // Make this a pie chart.
                                                                    renderer:jQuery.jqplot.PieRenderer,
                                                                    rendererOptions:{
                                                                        // Put data labels on the pie slices.
                                                                        // By default, labels show the percentage of the slice.
                                                                        showDataLabels:true
                                                                    }
                                                                },
                                                                seriesColors: [ "#ed3c3c", "#ffe03e", "#48ca48", "#49baff","#7d7dff", "#ff468b", "#de621d", "#cb68c9"],
                                                                legend:{ show:true, location:'e' }
                                                            }
                                          );
                              } else {
                                  $('#userTable').hide();
                                  $('#userChart').css("fontSize", 14);
                                  $('#userChart').append($('<span class="label label-info">'+i18n.t('errorMsgs.noData')+'</span>'));
                              }

                          } else {
                              if (json.message == "AuthenticateError") {
                                  jagg.showLogin();
                              } else {
                                  jagg.message({content:json.message,type:"error"});
                              }
                          }
                      }, "json");

            jagg.post("/site/blocks/usage/ajax/usage.jag", { action:"getProviderAPIVersionUserUsage", provider:provider,apiName:name,version:version, server:"https://localhost:9444/" },
                      function (json) {
                          if (!json.error) {
                              var length = json.usage.length,data = [];
                              $('#userVersionChart').empty();
                              $('#userVersionTable').find("tr:gt(0)").remove();
                              for (var i = 0; i < length; i++) {
                                  data[i] = [json.usage[i].user, parseInt(json.usage[i].count)];
                                  $('#userVersionTable').append($('<tr><td>' + json.usage[i].user + '</td><td>' + json.usage[i].count + '</td></tr>'));

                              }

                              if (length > 0) {
                                  $('#userVersionTable').show();
                                  var plot1 = jQuery.jqplot('userVersionChart', [data],
                                                            {
                                                                seriesDefaults:{
                                                                    // Make this a pie chart.
                                                                    renderer:jQuery.jqplot.PieRenderer,
                                                                    rendererOptions:{
                                                                        // Put data labels on the pie slices.
                                                                        // By default, labels show the percentage of the slice.
                                                                        showDataLabels:true
                                                                    }
                                                                },
                                                                seriesColors:[ "#ed3c3c", "#ffe03e", "#48ca48", "#49baff","#7d7dff", "#ff468b", "#de621d", "#cb68c9"],
                                                                legend:{ show:true, location:'e' }
                                                            }
                                          );
                              } else {
                                  $('#userVersionTable').hide();
                                  $('#userVersionChart').css("fontSize", 14);
                                  $('#userVersionChart').append($('<span class="label label-info">'+i18n.t('errorMsgs.noData')+'</span>'));
                              }

                          } else {
                              if (json.message == "AuthenticateError") {
                                  jagg.showLogin();
                              } else {
                                  jagg.message({content:json.message,type:"error"});
                              }
                          }
                      }, "json");

            var responseTime = getResponseTime(name);
            var lastAccessTime = getLastAccessTime(name);

            if (responseTime != null && lastAccessTime != null) {
                $("#usageSummary").show();
                var doc = document;
                var tabBody = doc.getElementById("usageTable");

                var row1 = doc.createElement("tr");
                var cell1 = doc.createElement("td");
                cell1.setAttribute("class", "span4");
                cell1.innerHTML = i18n.t('titles.responseTimeGraph');
                var cell2 = doc.createElement("td");
                cell2.innerHTML = responseTime != null ? responseTime : i18n.t('errorMsgs.unavailableData');
                row1.appendChild(cell1);
                row1.appendChild(cell2);

                var row2 = doc.createElement("tr");
                var cell3 = doc.createElement("td");
                cell3.setAttribute("class", "span4");
                cell3.innerHTML = i18n.t('titles.lastAccessTimeGraph');
                var cell4 = doc.createElement("td");
                cell4.innerHTML = lastAccessTime != null ? lastAccessTime : i18n.t('errorMsgs.unavailableData');
                row2.appendChild(cell3);
                row2.appendChild(cell4);

                tabBody.appendChild(row1);
                tabBody.appendChild(row2);

            }

        }
}

Object.size = function(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) {
            size++;
        }
    }
    return size;
};



