<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.hdfs.mgt.ui.HDFSAdminClient" %>
<%@ page import="org.wso2.carbon.hdfs.mgt.stub.fs.xsd.FolderInformation" %>
<%@ page import="org.wso2.carbon.hdfs.mgt.stub.fs.HDFSAdminHDFSServerManagementException" %>

<script type="text/javascript" src="js/hdfs_ui_util.js"></script>
<script type="text/javascript" src="js/_libs/jquery.js"></script>
<script type="text/javascript" src="js/jquery.jstree.js"></script>

<%!
    private String folderTree = "\"<li id='root'><a href='#'>/</a><ul>";
    private String folderRoot = "[{\"data\" :{ \"title\" : \"/\",\"icon\":\"images/root.png\"}, \"attr\" : {\"path\" : \"/\"}, \"state\" : \"closed\"}]";

%>



<script>

    $(function () {
        $("#createFolder").click(function () {
            $("#tree").jstree("create");
        });
        $("#renameFolder").click(function () {
            $("#tree").jstree("rename");
        });
        $("#delteFolder").click(function () {
            $("#tree").jstree("delete");
        });

        $("#tree").jstree({
            "json_data":{
                "data": <%=folderRoot%>,
                "ajax":{ "url":function (node) {
                    var nodePath = node.attr('path'); //id="A"
                    //var nodeIcon = node.attr('icon');
                    //var nodeType = node.attr('type');
                    //node.set_type(nodeType);
                    //alert(nodePath);
                    //alert(node.attr('id'));
                    if (typeof nodePath != "undefined") {
                        return "get_hdfs_folder_contents_ajaxprocessor.jsp?fs_object_path=" + nodePath + "/";

                    } else {

                    }
                }
                }
            },

            "types":{
                // I set both options to -2, as I do not need depth and children count checking
                // Those two checks may slow jstree a lot, so use only when needed
                "max_depth":-2,
                "max_children":-2,
                // I want only `drive` nodes to be root nodes
                // This will prevent moving or creating any other type as a root node
                "valid_children":[ "drive" ],
                "types":{
                    // The default type
                    "default":{
                        // I want this type to have no children (so only leaf nodes)
                        // In my case - those are files
                        "valid_children":"none",
                        // If we specify an icon for the default type it WILL OVERRIDE the theme icons
                        "icon":{
                            "image":"images/file.gif"
                        }
                    },
                    // The `folder` type
                    "folder":{
                        // can have files and other folders inside of it, but NOT `drive` nodes
                        "valid_children":[ "default", "folder" ],
                        "icon":{
                            "image":"images/folder.gif"
                        }
                    },
                    // The `drive` nodes
                    "drive":{
                        // can have files and folders inside, but NOT other `drive` nodes
                        "valid_children":[ "default", "folder" ],
                        "icon":{
                            "image":"images/root.png"
                        },
                        // those prevent the functions with the same name to be used on `drive` nodes
                        // internally the `before` event is used
                        "start_drag":false,
                        "move_node":false,
                        "delete_node":false,
                        "remove":false
                    }
                }
            },
            "plugins":[ "themes", "json_data", "ui", "crrm", "sort", "contextmenu", "dnd", "types" ]

        })
                .bind("rename.jstree", function (event, data) {
                    alert(data.inst);
//                                      alert('New path' + data.rslt.obj.attr("path"));
//                                  var nodePath = data.rslt.obj.parent.attr('path');
                    //ale);
//                                  alert(nodePath);
                    alert("Old path " + data.rslt.old_name);
                    alert("new path " + data.rslt.new_name);
                    //  $.get("rename_folder_ajaxprocessor.jsp", {"srcPath" : data.rslt.obj.attr('path'), "dstPath" : data.rslt.parent.attr('path') + '/' + data.rslt.new_name });
                })

                .bind("create.jstree",
                function (event, data) {
                    if (!confirm('Are you sure you want to create folder?')) {
                        $.jstree.rollback(data.rlbk);
                        return;
                    }
                    alert(data.rslt.name);
                    alert(data.rslt.parent.attr("path"));
                    if (data.rslt.parent.attr("path") != "/") {
                        $.get("create_folder_ajaxprocessor.jsp", {"folderPath":data.rslt.parent.attr("path") + '/' + data.rslt.name});
                    } else {
                        $.get("create_folder_ajaxprocessor.jsp", {"folderPath":data.rslt.parent.attr("path") + data.rslt.name});
                    }


                })
                .bind("delete_node.jstree",
                function (event, data) {
                    if (!confirm('Are you sure you want to delete?')) {
                        $.jstree.rollback(data.rlbk);
                        return false;
                    }
                    alert("going to delete a node " + data.rslt.obj.attr('path'));
                    $.get("delete_folder_ajaxprocessor.jsp", {"folderPath":data.rslt.obj.attr('path')});

                    //check the status code return and rollback the node.


                    return true;

                })
        ;

    });
</script>

<fmt:bundle basename="org.wso2.carbon.hdfs.mgt.ui.i18n.Resources">
    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.hdfs.mgt.ui.i18n.JSResources"
            request="<%=request%>" i18nObjectName="hdfsjsi18n"/>
    <carbon:breadcrumb
            label="hdfs.fs"
            resourceBundle="org.wso2.carbon.hdfs.mgt.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <div id="middle">
        <h2>Hadoop File System Explorer </h2>

        <div class="buttonRow">
            <input id="createFolder" class="button" type="button" value="Create Folder"/>
            <input id="renameFolder" class="button" type="button" value="Rename Folder"/>
            <input id="delteFolder" class="button" type="button" value="Delete Folder"/>
            <input id="upload" class="button" type="button" value="Upload"/>
        </div>
        <p/>
        <div id="tree">
        </div>
    </div>
</fmt:bundle>