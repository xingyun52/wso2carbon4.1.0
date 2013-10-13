function doGovernanceActionAjax(actionName, stage, version, tagName, checkItems, comment){
	jagg.post("../blocks/lifecycle/add/ajax/add.jag", { 
		action:actionName,
		applicationKey:$("#applicationKey").attr('value'),
		stageName:stage,
		version:version,
		checkItems: JSON.stringify(checkItems),
		tagName: tagName,
		comment: comment
	},
	function (result) {
		if(result != undefined){
			jagg.message({content:"Successfully completed the operation",type:'info' });
			window.location.reload(false); 
		} 
	},
	function (jqXHR, textStatus, errorThrown) {
		jagg.message({content:"Error occurred while performing the governance operation",type:'info' });
	});
}