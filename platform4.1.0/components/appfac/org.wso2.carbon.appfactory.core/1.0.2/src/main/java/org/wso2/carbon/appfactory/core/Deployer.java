package org.wso2.carbon.appfactory.core;

import java.util.Map;

public interface Deployer {

    public void deployTaggedArtifact(Map<String,String[]> requestParameters) throws Exception;

//    public void getTagNamesOfPersistedArtifacts(StaplerRequest req, StaplerResponse rsp);

    public void deployLatestSuccessArtifact(Map<String,String[]> requestParameters) throws Exception;

    public void deployPromotedArtifact(Map<String,String[]> requestParameters) throws Exception;

    public void unDeployArtifact(Map<String,String[]> requestParameters) throws Exception;

}
