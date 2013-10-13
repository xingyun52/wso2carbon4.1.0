package org.wso2.carbon.governance.registry.extensions.interfaces;

import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;

import java.util.Map;

/**
 * This is the interface that is used to write custom validations for lifecycles.
 * Validations are code segments that runs before a transition happens
 */

public interface CustomValidations {

    /**
     * This method is called when the validations object is initialised.
     * All the validations objects are initialised only once.
     *
     * @param parameterMap Static parameter map given by the user. These are the parameters that have been given in the
     *                     lifecycle configuration as the parameters of the custom validator.
     *
     *                     Eg:- <validation forEvent="Promote" class="org.wso2.carbon.governance.registry.extensions.validators.AttributeValueValidator">
                                    <parameter name="pattern" value="^\\d+[.]\\d+[.]\\d$"/>
                                    <parameter name="atLeastOne" value="true"/>
                                    <parameter name="attribute" value="overview_version"/>
                                </validation>

                           The parameters defined here will be passed to the custom validator using this method.
     */
    void init(Map parameterMap);

    /**
     * This method will be called when the invoke() method of the default lifecycle implementation is called.
     * Custom validations logic should reside in this method since the default lifecycle implementation will determine
     * the validation output by looking at the output of this method.
     *
     * @param context The request context that was generated from the registry core for the invoke() call.
     *                The request context contains the resource, resource path and other variables generated during the initial call.
     * @return Returns whether the validation was passed of failed.
     * */
    boolean validate(RequestContext context);
}
