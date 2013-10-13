package org.wso2.carbon.mashup.utils;

import org.apache.axis2.description.AxisService;
import org.jaggeryjs.scriptengine.exceptions.ScriptException;
import org.jaggeryjs.scriptengine.security.RhinoSecurityDomain;

import java.security.CodeSource;
import java.security.cert.Certificate;

public class MashupSecurityDomain implements RhinoSecurityDomain {

    //private String scriptPath;
    private CodeSource codeSource;
    private AxisService service;

    public MashupSecurityDomain(AxisService service) {
        //this.scriptPath = scriptPath;
        this.service = service;
    }

    @Override
    public CodeSource getCodeSource() throws ScriptException {
        if(codeSource != null) {
            return codeSource;
        }
        codeSource = new CodeSource(service.getFileName(), (Certificate[]) null);
        return codeSource;
    }

    public AxisService getService() {
        return service;
    }
}
