package org.wso2.carbon.appfactory.jenkins.deploy;

import javax.activation.DataHandler;

import org.kohsuke.stapler.StaplerRequest;

public interface JenkinsDeployer {

    public void deployTaggedArtifact(StaplerRequest req) throws Exception;

//    public void getTagNamesOfPersistedArtifacts(StaplerRequest req, StaplerResponse rsp);

    public void deployLatestSuccessArtifact(StaplerRequest req) throws Exception;

    public void deployPromotedArtifact(StaplerRequest req) throws Exception;
    
    

}
