<?php
$ADMIN_USER_NAME='admin';
$ADMIN_PASSWORD='admin';
$PARENT_REPO_PATH='/home/*/repository/';
if (!isset($_SERVER['PHP_AUTH_USER'])) {
	header('WWW-Authenticate: Basic realm="My Realm"');
	header('HTTP/1.0 401 Unauthorized');
	echo '401 Authorization Required';
	exit;
} else {
	if((strcmp($_SERVER['PHP_AUTH_USER'],$ADMIN_USER_NAME)==0) && (strcmp($_SERVER['PHP_AUTH_PW'],$ADMIN_PASSWORD)==0)){
    		$name = $_REQUEST['name'];
		$cmd = 'svnadmin create ' . escapeshellarg($PARENT_REPO_PATH . $name) . ' --fs-type fsfs';
		$output1=exec($cmd, $output, $exitValue);
		if ($exitValue != 0) {
  			Print $output1;
 			header('HTTP/1.1 500 Internal Server Error');
			return;
		}else{
			header('HTTP/1.1 201 Created');
			return;
			}
	}else{
		header('WWW-Authenticate: Basic realm="My Realm"');
		header('HTTP/1.0 401 Unauthorized');
    		echo '401 Authorization Required';
    		exit;

	}
} 

?> 
