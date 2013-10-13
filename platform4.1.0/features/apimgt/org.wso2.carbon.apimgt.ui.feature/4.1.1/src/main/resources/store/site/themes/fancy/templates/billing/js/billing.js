$(document).ready(function () {

    var yearVal = $("#year");
    var monthVal = $("#month");

    var selectedYearVal;
    if(yearVal.length!=0){
        selectedYearVal = year.options[year.selectedIndex].value;}else{
        selectedYearVal = "2012";
    }
    var selectedMonthVal;
    if(monthVal.length!=0){
        selectedMonthVal= month.options[month.selectedIndex].value;}else{
        selectedMonthVal = "11";
    }
    populateUsageData(selectedYearVal + '-' + selectedMonthVal);

    $("#generate").click(function () {
        var year = $("#year");
        var month = $("#month");
        var selectedYear = year.val();
        var selectedMonth = month.val();
        deleteTableRows("usageSummaryBody");
        deleteTableRows("billingBody");
        populateUsageData(selectedYear + "-" + selectedMonth);

    });

});

var populateUsageData = function(period) {
    jagg.post("/site/blocks/billing/ajax/billing.jag", {
        action:"getProviderAPIUsage",
        period:period
    }, function (result) {
        if (!result.error) {
            var usage = result.usage;
            var i;
            var tbody = document.getElementById("usageSummaryBody");
            var total=0.00;
            if(usage.length==0){
                $('#showMsg').show();
                $('#usageDiv').hide();
            }
            else{
                $('#usageDiv').show();
                $('#showMsg').hide();
                for (i = 0; i < usage.length; i++) {
                    var row = document.createElement("tr");
                    var cell1 = document.createElement("td");

                    cell1.innerHTML = usage[i].apiName;
                    var cell2 = document.createElement("td");
                    cell2.innerHTML = usage[i].version;
                    var cell3 = document.createElement("td");
                    var tmpSpan = document.createElement('span');
                    tmpSpan.className = "pull-right";
                    tmpSpan.innerHTML = usage[i].count;
                    cell3.appendChild(tmpSpan);
                    row.appendChild(cell1);
                    row.appendChild(cell2);
                    row.appendChild(cell3);
                    tbody.appendChild(row);

                    var tbillBody = document.getElementById("billingBody");
                    var rowBil = document.createElement("tr");
                    var cell12 = document.createElement("td");

                    cell12.innerHTML = usage[i].apiName;
                    var cellBil1 = document.createElement("td");
                    var tmpSpan1 = document.createElement('span');
                    tmpSpan1.className = "pull-right";
                    tmpSpan1.innerHTML = usage[i].count;
                    cellBil1.appendChild(tmpSpan1);
                    var cellBil2 = document.createElement("td");
                    if (usage[i].cost=='0.00') {
                        cellBil2.innerHTML = "FREE  [ 0-5 APIs]";
                    }
                    else{
                        cellBil2.innerHTML = "$"+usage[i].costPerAPI+" per API call";
                    }

                    var cellBil3 = document.createElement("td");
                    var tmpSpan2 = document.createElement('span');
                    tmpSpan2.className = "pull-right";
                    if (usage[i].cost=='0.00') {
                        tmpSpan2.innerHTML = "FREE";
                    }else{
                        tmpSpan2.innerHTML = usage[i].cost; }
                    cellBil3.appendChild(tmpSpan2);

                    rowBil.appendChild(cell12);
                    rowBil.appendChild(cellBil1);
                    rowBil.appendChild(cellBil2);
                    rowBil.appendChild(cellBil3);
                    tbillBody.appendChild(rowBil);

                    total+=parseFloat(total)+parseFloat(usage[i].cost);

                }
                //if(usage.length>=1 && usage[0].cost!='0.00') {
                var tbillBody1 = document.getElementById("billingBody");
                var rowBill = document.createElement("tr");
                var cellBill = document.createElement("td");
                cellBill.colSpan = "3";
                rowBill.appendChild(cellBill);

                var cellBill2 = document.createElement("td");
                cellBill2.style = "border-top:solid #ccc 3px;";
                cellBill2.innerHTML = '<strong>Total</strong>';
                var tmpSpan3 = document.createElement('span');
                tmpSpan3.className = "pull-right";
                tmpSpan3.innerHTML = total;
                cellBill2.appendChild(tmpSpan3);
                rowBill.appendChild(cellBill2);
                tbillBody1.appendChild(rowBill);
                //}
            } }else {
            jagg.message({content:result.message,type:"error"});


        }
    }, "json");
};

var deleteTableRows = function(tbodyId) {
    $("#"+tbodyId+"").empty();


}