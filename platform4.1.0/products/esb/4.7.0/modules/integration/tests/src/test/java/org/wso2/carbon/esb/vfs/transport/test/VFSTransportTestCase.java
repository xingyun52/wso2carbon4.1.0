/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.esb.vfs.transport.test;

import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.esb.ESBIntegrationTest;

import java.io.File;

/**
 * This test class in skipped when user mode is tenant because of this release not support vfs transport for tenants
 */
public class VFSTransportTestCase extends ESBIntegrationTest {

    private ServerConfigurationManager serverConfigurationManager;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();

        serverConfigurationManager = new ServerConfigurationManager(esbServer.getBackEndUrl());
        serverConfigurationManager.applyConfiguration(new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "axis2.xml").getPath()));
        super.init();

        File outfolder = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator);
        File infolder = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator);
        File originalfolder = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "original" + File.separator);
        File failurelfolder = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "failure" + File.separator);
        outfolder.mkdirs();
        infolder.mkdirs();
        originalfolder.mkdirs();
        failurelfolder.mkdirs();
    }

    @AfterClass(alwaysRun = true)
    public void restoreServerConfiguration() throws Exception {
        try {
            super.cleanup();
        } finally {
            Thread.sleep(3000);
            serverConfigurationManager.restoreToLastConfiguration();
            serverConfigurationManager = null;
        }
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml")
    public void testVFSProxyFileURI_LinuxPath_ContentType_XML()
            throws Exception {

        addVFSProxy1();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);
        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");

        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("WSO2 Company"));
        bfile.delete();
        outfile.delete();
        removeProxy("VFSProxy1");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = - *\\.txt")
    public void testVFSProxyFileURI_LinuxPath_ContentType_Plain()
            throws Exception {

        addVFSProxy2();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);
        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt");

        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));
        bfile.delete();
        outfile.delete();
        removeProxy("VFSProxy2");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = *")
    public void testVFSProxyFileURI_LinuxPath_SelectAll_FileNamePattern()
            throws Exception {

        addVFSProxy3();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);
        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt");

        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));
        bfile.delete();
        outfile.delete();
        removeProxy("VFSProxy3");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = nothing")
    public void testVFSProxyFileURI_LinuxPath_No_FileNamePattern()
            throws Exception {

        addVFSProxy4();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);
        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt");

        Assert.assertTrue(!outfile.exists());
        bfile.delete();
        outfile.delete();
        removeProxy("VFSProxy4");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = - *\\.txt, transport.PollInterval=1")
    public void testVFSProxyPollInterval_1()
            throws Exception {

        addVFSProxy5();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);
        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt");

        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));
        bfile.delete();
        outfile.delete();
        removeProxy("VFSProxy5");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = - *\\.txt, transport.PollInterval=30")
    public void testVFSProxyPollInterval_30()
            throws Exception {

        addVFSProxy6();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(1000);
        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt");

        Assert.assertTrue(!outfile.exists());

        Thread.sleep(31000);
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));
        bfile.delete();
        outfile.delete();
        removeProxy("VFSProxy6");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = - *\\.txt, transport.PollInterval=1, transport.vfs.ActionAfterProcess=MOVE")
    public void testVFSProxyActionAfterProcess_Move()
            throws Exception {

        addVFSProxy7();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt");
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));

        File originalFile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "original" + File.separator + "test.txt");

        Assert.assertTrue(originalFile.exists());
        String vfsOriginal = FileUtils.readFileToString(originalFile);
        Assert.assertTrue(vfsOriginal.contains("andun@wso2.com"));

        bfile.delete();
        outfile.delete();
        originalFile.delete();
        removeProxy("VFSProxy7");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = - *\\.txt, transport.PollInterval=1, transport.vfs.ActionAfterProcess=DELETE")
    public void testVFSProxyActionAfterProcess_DELETE()
            throws Exception {

        addVFSProxy8();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt");
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));

        File originalFile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "original" + File.separator + "test.txt");

        Assert.assertTrue(!originalFile.exists());
        Assert.assertTrue(!bfile.exists());

        bfile.delete();
        outfile.delete();
        originalFile.delete();
        removeProxy("VFSProxy8");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = - *\\.txt, transport.PollInterval=1, transport.vfs.ReplyFileName = out.txt ")
    public void testVFSProxyReplyFileName_Normal()
            throws Exception {

        addVFSProxy9();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt");
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));

        bfile.delete();
        outfile.delete();
        removeProxy("VFSProxy9");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = - *\\.txt, transport.PollInterval=1, transport.vfs.ReplyFileName = out123@wso2_text.txt ")
    public void testVFSProxyReplyFileName_SpecialChars()
            throws Exception {

        addVFSProxy10();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out123@wso2_text.txt");
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));

        bfile.delete();
        outfile.delete();
        removeProxy("VFSProxy10");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = /home/someuser/somedir transport.vfs.ContentType = text/plain, transport.vfs.FileNamePattern = - *\\.txt, transport.PollInterval=1, transport.vfs.ReplyFileName = not specified ")
    public void testVFSProxyReplyFileName_NotSpecified()
            throws Exception {

        addVFSProxy11();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.txt").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.txt");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "response.xml");
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));

        bfile.delete();
        outfile.delete();
        removeProxy("VFSProxy11");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml transport.vfs.ActionAfterFailure=MOVE")
    public void testVFSProxyActionAfterFailure_MOVE()
            throws Exception {

        addVFSProxy12();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "fail.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "fail.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(!outfile.exists());

        File originalFile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "failure" + File.separator + "fail.xml");
        Assert.assertTrue(originalFile.exists());
        String vfsOut = FileUtils.readFileToString(originalFile);
        Assert.assertTrue(vfsOut.contains("andun@wso2.com"));

        originalFile.delete();
        bfile.delete();
        removeProxy("VFSProxy12");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml transport.vfs.ActionAfterFailure=DELETE")
    public void testVFSProxyActionAfterFailure_DELETE()
            throws Exception {

        addVFSProxy13();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "fail.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "fail.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(!outfile.exists());

        File originalFile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "failure" + File.separator + "fail.xml");

        Assert.assertTrue(!originalFile.exists());

        bfile.delete();
        removeProxy("VFSProxy13");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml transport.vfs.ActionAfterFailure=NotSpecified")
    public void testVFSProxyActionAfterFailure_NotSpecified()
            throws Exception {

        addVFSProxy14();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "fail.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "fail.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(!outfile.exists());

        File originalFile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "failure" + File.separator + "fail.xml");

        Assert.assertTrue(!originalFile.exists());

        bfile.delete();
        removeProxy("VFSProxy14");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Invalid, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml")
    public void testVFSProxyFileURI_Invalid()
            throws Exception {

        addVFSProxy15();

        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(!outfile.exists());

        removeProxy("VFSProxy15");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = Invalid, transport.vfs.FileNamePattern = - *\\.xml transport.vfs.FileURI = Invalid")
    public void testVFSProxyContentType_Invalid()
            throws Exception {

        addVFSProxy16();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("WSO2 Company"));

        outfile.delete();
        bfile.delete();
        removeProxy("VFSProxy16");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = Not Specified, transport.vfs.FileNamePattern = - *\\.xml transport.vfs.FileURI = Invalid")
    public void testVFSProxyContentType_NotSpecified()
            throws Exception {

        addVFSProxy17();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(!outfile.exists());

        bfile.delete();
        removeProxy("VFSProxy17");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml transport.PollInterval = Non Integer")
    public void testVFSProxyPollInterval_NonInteger()
            throws Exception {

        addVFSProxy18();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);
        //The poll interval will be set to 300s here,

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(!outfile.exists());

        bfile.delete();
        removeProxy("VFSProxy18");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml transport.vfs.ActionAfterProcess = Invalid")
    public void testVFSProxyActionAfterProcess_Invalid()
            throws Exception {

        addVFSProxy19();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("WSO2 Company"));

        File originalFile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "original" + File.separator + "test.xml");
        Assert.assertTrue(!originalFile.exists());
        Assert.assertTrue(!bfile.exists());

        outfile.delete();
        removeProxy("VFSProxy19");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml transport.vfs.ActionAfterFailure = Invalid")
    public void testVFSProxyActionAfterFailure_Invalid()
            throws Exception {

        addVFSProxy20();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "fail.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "fail.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(!outfile.exists());

        File originalFile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "failure" + File.separator + "fail.xml");
        Assert.assertTrue(!originalFile.exists());
        Assert.assertTrue(!bfile.exists());

        removeProxy("VFSProxy20");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml transport.vfs.MoveAfterProcess = Invalid")
    public void testVFSProxyMoveAfterProcess_Invalid()
            throws Exception {

        addVFSProxy21();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("WSO2 Company"));

        File originalFile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "invalid" + File.separator + "test.xml");
        Assert.assertTrue(!originalFile.exists());
        Assert.assertTrue(bfile.exists());

        outfile.delete();
        bfile.delete();
        removeProxy("VFSProxy21");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml transport.vfs.MoveAfterFailure = Invalid")
    public void testVFSProxyMoveAfterFailure_Invalid()
            throws Exception {

        addVFSProxy22();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "fail.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "fail.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(!outfile.exists());

        File originalFile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "invalid" + File.separator + "fail.xml");
        Assert.assertTrue(!originalFile.exists());
        Assert.assertTrue(bfile.exists());

        bfile.delete();
        removeProxy("VFSProxy22");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml, transport.vfs.ReplyFileURI  = Invalid")
    public void testVFSProxyReplyFileURI_Invalid()
            throws Exception {

        addVFSProxy23();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "invalid" + File.separator + "out.xml");
        Assert.assertTrue(outfile.exists());
        String vfsOut = FileUtils.readFileToString(outfile);
        Assert.assertTrue(vfsOut.contains("WSO2 Company"));

        bfile.delete();
        removeProxy("VFSProxy23");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    @Test(groups = {"wso2.esb"}, description = "Sending a file through VFS Transport : transport.vfs.FileURI = Linux Path, transport.vfs.ContentType = text/xml, transport.vfs.FileNamePattern = - *\\.xml, transport.vfs.ReplyFileName  = Invalid")
    public void testVFSProxyReplyFileName_Invalid()
            throws Exception {

        addVFSProxy24();

        File afile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator + "test.xml").getPath());
        File bfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "test.xml");

        FileUtils.copyFile(afile, bfile);
        Thread.sleep(2000);

        File outfile = new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml");
        Assert.assertTrue(!outfile.exists());

        bfile.delete();
        removeProxy("VFSProxy24");
    }

    private void addVFSProxy1()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy1\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy2()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy2\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*.txt</parameter>" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy3()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy3\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*.*</parameter>" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy4()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy4\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\"></parameter>" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy5()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy5\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*.txt</parameter>" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy6()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy6\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*.txt</parameter>" +
                                             "                <parameter name=\"transport.PollInterval\">30</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }


    private void addVFSProxy7()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy7\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*.txt</parameter>" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.ActionAfterProcess\">MOVE</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.MoveAfterProcess\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "original" + File.separator + "</parameter>" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy8()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy8\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*.txt</parameter>" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.ActionAfterProcess\">DELETE</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.txt\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy9()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy9\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*.txt</parameter>" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property name=\"transport.vfs.ReplyFileName\" value=\"out.txt\" scope=\"transport\"/>" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy10()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy10\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*.txt</parameter>" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property name=\"transport.vfs.ReplyFileName\" value=\"out123@wso2_text.txt\" scope=\"transport\"/>" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy11()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy11\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*.txt</parameter>" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <inSequence>\n" +
                                             "                           <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                           <log level=\"full\"/>\n" +
                                             "                           <send>\n" +
                                             "                               <endpoint name=\"FileEpr\">\n" +
                                             "                                   <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "\"/>\n" +
                                             "                               </endpoint>\n" +
                                             "                           </send>" +
                                             "                        </inSequence>" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy12()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy12\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.MoveAfterFailure\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "failure" + File.separator + "</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.ActionAfterFailure\">MOVE</parameter>" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy13()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy13\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.ActionAfterFailure\">DELETE</parameter>" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy14()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy14\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy15()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy15\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "invalid" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy16()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy16\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">invalid/invalid</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }


    private void addVFSProxy17()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy17\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy18()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy18\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1.1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy19()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy19\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.ActionAfterProcess\">MOVEDD</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.MoveAfterProcess\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "original" + File.separator + "</parameter>" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy20()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy20\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.MoveAfterFailure\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "failure" + File.separator + "</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.ActionAfterFailure\">MOVEDD</parameter>" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy21()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy21\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.ActionAfterProcess\">MOVE</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.MoveAfterProcess\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "invalid" + File.separator + "</parameter>" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy22()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy22\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.MoveAfterFailure\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "invalid" + File.separator + "</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.ActionAfterFailure\">MOVE</parameter>" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void addVFSProxy23()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy23\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "invalid" + File.separator + "out.xml\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }


    private void addVFSProxy24()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                             "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"VFSProxy24\" transports=\"vfs\">\n" +
                                             "                <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "in" + File.separator + "</parameter> <!--CHANGE-->\n" +
                                             "                <parameter name=\"transport.vfs.ContentType\">text/xml</parameter>\n" +
                                             "                <parameter name=\"transport.vfs.FileNamePattern\">.*\\.xml</parameter>\n" +
                                             "                <parameter name=\"transport.PollInterval\">1</parameter>\n" +
                                             "                <target>\n" +
                                             "                        <endpoint>\n" +
                                             "                                <address format=\"soap12\" uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                                             "                        </endpoint>\n" +
                                             "                        <outSequence>\n" +
                                             "                                <property name=\"transport.vfs.ReplyFileName\" value=\"out.xml\" scope=\"transport\"/>" +
                                             "                                <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                                <send>\n" +
                                             "                                        <endpoint>\n" +
                                             "                                                <address uri=\"vfs:ftpd://" + getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator).getPath() + "test" + File.separator + "out" + File.separator + "\"/> <!--CHANGE-->\n" +
                                             "                                        </endpoint>\n" +
                                             "                                </send>\n" +
                                             "                        </outSequence>\n" +
                                             "                </target>\n" +
                                             "        </proxy>"));
    }

    private void removeProxy(String proxyName) throws Exception {
        deleteProxyService(proxyName);
    }
}

