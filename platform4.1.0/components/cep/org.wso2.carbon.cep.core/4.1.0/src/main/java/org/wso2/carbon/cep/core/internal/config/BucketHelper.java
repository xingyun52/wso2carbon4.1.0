/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.cep.core.internal.config;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.Bucket;
import org.wso2.carbon.cep.core.Query;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.internal.config.input.InputHelper;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.input.Input;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;

/**
 * this class is used to parse the top level bucket attributes
 */

public class BucketHelper {
    private static final Log log = LogFactory.getLog(BucketHelper.class);


    public static Bucket fromOM(OMElement bucketElement) throws CEPConfigurationException {

        Bucket bucket = new Bucket();

        String name =
                bucketElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_NAME));
        bucket.setName(name);

        OMElement descriptionElement =
                bucketElement.getFirstChildWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                              CEPConstants.CEP_CONF_ELE_DESCRIPTION));
        if (descriptionElement != null) {
            bucket.setDescription(descriptionElement.getText());
        }

        OMElement providerConfiguration =
                bucketElement.getFirstChildWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                              CEPConstants.CEP_CONF_ELE_PROVIDER_CONFIG));
        bucket.setEngineProvider(ProviderConfigurationHelper.engineProviderFromOM(providerConfiguration));

        if (providerConfiguration.getChildElements().hasNext()) {
            bucket.setProviderConfigurationProperties(ProviderConfigurationHelper.propertiesFromOM(providerConfiguration));
        }

        String owner = bucketElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ELE_CEP_BUCKET_OWNER));
        if (owner != null) {
            bucket.setOwner(owner);
        }

        OMElement inputOmElement = null;
        for (Iterator iter = bucketElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                         CEPConstants.CEP_CONF_ELE_INPUT)); iter.hasNext(); ) {
            inputOmElement = (OMElement) iter.next();
            bucket.addInput(InputHelper.fromOM(inputOmElement));
        }


        OMElement queryOmElement = null;
        int queryIndex = 0;
        for (Iterator iterator = bucketElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                             CEPConstants.CEP_CONF_ELE_QUERY)); iterator.hasNext(); ) {
            queryOmElement = (OMElement) iterator.next();
            Query query = QueryHelper.fromOM(queryOmElement);
            query.setQueryIndex(queryIndex);
            bucket.addQuery(query);
            queryIndex++;
        }


        return bucket;

    }


    public static OMElement bucketToOM(Bucket bucket) {
        String bucketName = bucket.getName();
        String bucketDescription = bucket.getDescription();
        List<Input> inputList = bucket.getInputs();
        List<Query> queryList = bucket.getQueries();
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement bucketItem = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_BUCKET,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        bucketItem.addAttribute(CEPConstants.CEP_CONF_ATTR_NAME, bucketName,
                                null);
        OMElement description = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_DESCRIPTION,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));

        description.setText(bucketDescription);

        bucketItem.addChild(description);
        bucketItem.addChild(ProviderConfigurationHelper.providerConfigurationToOM(bucket.getEngineProvider(), bucket.getProviderConfigurationProperties()));
        for (Input input : inputList) {
            OMElement inputChild = InputHelper.inputToOM(input);
            bucketItem.addChild(inputChild);
        }
        for (Query query : queryList) {
            OMElement queryChild = QueryHelper.queryToOM(query);
            bucketItem.addChild(queryChild);
        }
        return bucketItem;
    }


    public static String getBucketName(OMElement bucket) {
        return bucket.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_NAME));
    }
}
