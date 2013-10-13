Prerequisites
=============
1)VMware player should be installed in 64 bit machine with virtualization enabled. 
2)The host machine should have at least 4 GB memory and connected to Internet(this is only for email validation functionality)

Getting Started with VM
=======================

1)Unzip the downloaded file.
2)Start the VMware player.
3)Open the VM image through 'Open a Virtual Machine' and click on play.
4)Select "I moved it" from the pop-up.
5)Edit the image setting as follow.
Increase the memory to at least 2 GB(Recommended ~8GB)  and enable bridge network setting. 
6)Start the image(at boot up time all the servers will be started)
7)Use following credentials to login to the image.
Username: appfactory
Password: appfactory
8)You can get the IP of the VM by issuing the command 'ifconfig'
Add following entries to /etc/hosts of the host.

<IP_of_VMware> appfactorypreview.wso2.com
<IP_of_VMware> appserver.prod.appfactorypreview.wso2.com
<IP_of_VMware> appserver.dev.appfactorypreview.wso2.com
<IP_of_VMware> appserver.test.appfactorypreview.wso2.com
<IP_of_VMware> controller.appfactorypreview.wso2.com
<IP_of_VMware> jenkins.appfactorypreview.wso2.com
<IP_of_VMware> redmine.appfactorypreview.wso2.com
<IP_of_VMware> apimanager.appfactorypreview.wso2.com

9)Now you can access the servers using following links in host machine's browser. 
Access Appfactory https://appfactorypreview.wso2.com/appmgt/
Access Dev Cloud Application server https://appserver.dev.appfactorypreview.wso2.com/
Access Test Cloud Application server https://appserver.test.appfactorypreview.wso2.com/
Access Production Cloud Application server https://appserver.prod.appfactorypreview.wso2.com/
Access Jenkins Server http://jenkins.appfactorypreview.wso2.com:8080/
Access API Store https://apimanager.appfactorypreview.wso2.com:9449/store
