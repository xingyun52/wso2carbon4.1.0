<!DOCTYPE html>
<html lang="en">
<head>
<title>Business Activity Monitor Dashboard</title>

<script type="text/javascript" src="scripts/jquery-1.7.2.min.js"></script>

<link rel="stylesheet" type="text/css" href="bootstrap/css/bootstrap.min.css" />
<link rel="stylesheet" type="text/css" href="bootstrap/css/bootstrap-responsive.min.css" />
<link rel="stylesheet" type="text/css" href="css/bam-dashboard-common-styles.css" />

<link rel="stylesheet" type="text/css" href="css/jquery.timepicker.css" />
<link rel="stylesheet" type="text/css" href="css/bootstrap-datepicker.css" />
<script type="text/javascript" src="js/jquery.timepicker.js"></script>
<script type="text/javascript" src="js/bootstrap-datepicker.js"></script>
<!--[if lt IE 9]>
    <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->
    <style type="text/css">
        body {padding-top: 60px;
            padding-bottom: 40px;
        }
        .sidebar-nav {
            padding: 9px 0;
        }
    </style>

    <script type="text/javascript">
        Date.prototype.format = function (fmt) {
            var date = this;

            return fmt.replace(
                    /\{([^}:]+)(?::(\d+))?\}/g,
                    function (s, comp, pad) {
                        var fn = date["get" + comp];

                        if (fn) {
                            var v = (fn.call(date) +
                                     (/Month$/.test(comp) ? 1 : 0)).toString();

                            return pad && (pad = pad - v.length)
                                    ? new Array(pad + 1).join("0") + v
                                    : v;
                        } else {
                            return s;
                        }
                    });
        };
        $(document).ready(function() {
            $(".timepicker").timepicker();
            var currentTime = new Date();

            $(".datepicker").val(currentTime.format("{Month:2}/{Date:2}/{FullYear}"));

            $(".datepicker").datepicker();
        })
    </script>
</head>
<body>


<div class="navbar navbar-fixed-top">
  <div class="navbar-inner">
    <div class="container-fluid">
      <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </a>

      <a class="brand" href="#"><img src="images/bamlogo.png" alt=""><span style="color: #B4D8FF;margin: 0 10px;">Dashboard</span></a>
    </div>
  </div>
</div>

<div class="container-fluid">
  <div class="row-fluid">
    <div class="span2">
      <div class="well sidebar-nav">
        <ul id="leftnav" class="nav nav-list">
          <li class="nav-header">Tool Boxes</li>
          <!--<li class="active"><a href="#">Activity Monitoring</a></li>-->
        </ul>
      </div>
    </div>
    <div class="span10">
        <div class="well topFilteringPanel">
            <%--<span class="span3">Select Server :
                <select id="server-dd" name="basic-combo">
                    <option value="__default__"></option>
                </select>
            </span>
            <span class="span3">Select Service :
                <select id="service-dd" name="basic-combo">
                    <option value="__default__"></option>
                </select>
            </span>
            <span class="span3">Select Operation :
                <select id="operation-dd" name="basic-combo">
			        <option value="__default__"></option>
		        </select>
            </span>
            &nbsp;&nbsp;&nbsp;--%>
            <button id="clearSelectionBtn" class="btn btn-primary btn-small filter-btn">Clear</button>
            <br>
            <div id="time-selectors">
                <table>
                    <tr>
                        <td style="width: 225px">
                            <table>
                                <tr>
                                    <td>
                                        <b>From</b>
                                    </td>
                                    <td>
                                        <table>
                                            <tr>
                                                <td>
                                                    <span class="control-label" for="from-date">Date :</span>
                                                    <input type="text" id="from-date" class="input-small datepicker"></input>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td>
                                                    <span class="control-label" for="from-time">Time :</span>
                                                    <input type="text" id="from-time" class="input-small timepicker"></input>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </td>
                        <td>
                            <table>
                                <tr>
                                    <td>
                                        <b>To</b>
                                    </td>
                                    <td>
                                        <table>
                                            <tr>
                                                <td>
                                                    <span class="control-label" for="to-date">Date :</span>
                                                    <input type="text" id="to-date" class="input-small datepicker"></input>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td>
                                                    <span class="control-label" for="to-date">Time :</span>
                                                    <input type="text" id="to-time" class="input-small timepicker"></input>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </div>

        </div>

      <%--<div class="navbar timelySwitch" style="overflow: hidden;">
          <div id="timely-dd" class="btn-group timely-dd-btns">
            <button class="btn">All</button>
            <button class="btn">Month</button>
            <button class="btn">Day</button>
		    <button class="btn btn-primary">Now</button>
          </div>
      </div>
      <div class="clearfix"></div>--%>

      <%--<div class="hero-unit">
        <iframe id="dashboardWidget-1" class="single-column-gadget" src="gadgets/mult-line-dashboard-chart.jag?server=&service&opr&t=Now"></iframe>
      </div>--%>


        <div class="row-fluid">
            <div class="hero-unit">
                <iframe id="dashboardWidget-21" src="gadgets/dataTable.table.jag?t=Now" class="single-column-gadget"></iframe>
            </div>
        </div>





      <%--<div class="row-fluid">
        <div class="hero-unit">
          <iframe id="dashboardWidget-2" src="gadgets/simple.bar.chart.jag?t=Now" class="single-column-gadget"></iframe>
        </div>
      </div>
      <div class="row-fluid">
        <div class="span4">
          <iframe id="dashboardWidget-3" src="gadgets/gauge.jag?server=&service&opr&t=Now" class="gadget-small"></iframe>
        </div><!--/span-->
        <div class="span4">
          <iframe id="dashboardWidget-4" src="gadgets/simple.pie.chart.jag?server=&service&opr&t=Now" class="gadget-small gadget-larger-width"></iframe>
        </div><!--/span-->

      </div><!--/row-->--%>



    </div><!--/span-->
  </div><!--/row-->

  <hr>

  <footer>
    <p class="wso2copyRight_footer">&copy; <a href="http://wso2.org">WSO2</a> Inc</p>
    <span class="wso2Logo_footer">
        <a class="wso2" target="_blank" href="http://www.wso2.com"></a>
    </span>
  </footer>

</div><!--/.fluid-container-->

<!-- Le javascript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script type="text/javascript" src="bootstrap/js/bootstrap.js"></script>
<script type="text/javascript" src="scripts/bam.dashboard.main.js"></script>
<script type="text/javascript" src="../navigation.populator.js"></script>
</body>
</html>
