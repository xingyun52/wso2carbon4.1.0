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

package org.wso2.carbon.automation.core.utils.endpointutils;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.environmentutils.ProductUrlGeneratorUtil;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkFactory;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkProperties;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

public class EsbEndpointSetter {
    private String userId;
    private boolean containsHttps = false;

    public OMElement setEndpointURL(DataHandler dh) throws IOException, XMLStreamException {

        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        OMElement endPointElem = builder.getDocumentElement();
        OMNode node;
        Iterator children = endPointElem.getChildElements();
        OMAttribute attribute = null;
        OMAttribute attribute2 = null;
        boolean changed = false;
        while (children.hasNext()) {
            node = (OMNode) children.next();
            if (((OMElementImpl) node).getLocalName().equals("end6point")) {
                this.replaceElement(node, attribute, attribute2);
            } else if (((OMElementImpl) node).getLocalName().equals("loadbalance")) {
                Iterator loadBalanceIterator = ((OMElementImpl) node).getChildElements();
                while (loadBalanceIterator.hasNext()) {
                    OMNode loadBalanceNode = (OMNode) loadBalanceIterator.next();
                    Iterator urlIterator = ((OMElementImpl) loadBalanceIterator).getChildElements();
                    while (urlIterator.hasNext()) {
                        OMNode urlNode = (OMNode) urlIterator.next();
                        String uri = ((OMElementImpl) urlNode).getAttribute(new QName("uri")).getAttributeValue();
                        attribute = ((OMElementImpl) urlNode).getAttribute(new QName("uri"));
                        ((OMElementImpl) urlNode).getAttribute(new QName("uri")).setAttributeValue(getUrl(uri));
                        attribute2 = ((OMElementImpl) urlNode).getAttribute(new QName("uri"));
                        System.out.println("LoadBalance");
                    }
                    endPointElem.removeAttribute(attribute);
                    endPointElem.addAttribute(attribute2);
                }
            } else if (((OMElementImpl) node).getLocalName().equals("failover")) {
                Iterator failOverIterator = ((OMElementImpl) node).getChildElements();
                while (failOverIterator.hasNext()) {
                    OMNode loadBalanceNode = (OMNode) failOverIterator.next();
                    Iterator urlIterator = ((OMElementImpl) loadBalanceNode).getChildElements();
                    while (urlIterator.hasNext()) {
                        OMNode urlNode = (OMNode) urlIterator.next();
                        String uri = ((OMElementImpl) urlNode).getAttribute(new QName("uri")).getAttributeValue();
                        attribute = ((OMElementImpl) urlNode).getAttribute(new QName("uri"));
                        ((OMElementImpl) urlNode).getAttribute(new QName("uri")).setAttributeValue(getUrl(uri));
                        attribute2 = ((OMElementImpl) urlNode).getAttribute(new QName("uri"));
                        System.out.println("failover");
                    }
                    endPointElem.removeAttribute(attribute);
                    endPointElem.addAttribute(attribute2);
                }
            } else {
                if (((OMElementImpl) node).getLocalName().equals("target")) {
                    Iterator nodIterator = ((OMElementImpl) node).getChildElements();
                    while (nodIterator.hasNext()) {
                        OMNode targetNode = (OMNode) nodIterator.next();
                        if (((OMElementImpl) targetNode).getLocalName().equals("endpoint")) {
                            this.replaceElement(targetNode, attribute, attribute2);
                            break;
                        } else {
                            travarsrchild(targetNode, attribute, attribute2);
                        }
                    }
                } else if (((OMElementImpl) node).getLocalName().equals("proxy")) {
                    Iterator nodIterator = ((OMElementImpl) node).getChildElements();
                    while (nodIterator.hasNext()) {
                        OMNode targetNode = (OMNode) nodIterator.next();
                        if (((OMElementImpl) targetNode).getLocalName().equals("endpoint")) {
                            this.replaceElement(targetNode, attribute, attribute2);
                            break;
                        } else {
                            travarsrchild(targetNode, attribute, attribute2);
                        }
                    }
                } else if (((OMElementImpl) node).getLocalName().equals("sequence")) {
                    Iterator nodIterator = ((OMElementImpl) node).getChildElements();
                    while (nodIterator.hasNext()) {
                        OMNode targetNode = (OMNode) nodIterator.next();
                        if (((OMElementImpl) targetNode).getLocalName().equals("endpoint")) {
                            this.replaceElement(targetNode, attribute, attribute2);
                            break;
                        } else {
                            travarsrchild(targetNode, attribute, attribute2);
                        }
                    }
                } else {
                    Iterator nodIterator = ((OMElementImpl) node).getChildElements();
                    while (nodIterator.hasNext()) {
                        OMNode targetNode = (OMNode) nodIterator.next();
                        if (((OMElementImpl) targetNode).getLocalName().equals("endpoint")) {
                            this.replaceElement(targetNode, attribute, attribute2);
                            break;
                        } else {
                            travarsrchild(targetNode, attribute, attribute2);
                        }
                    }

                }

                if (((OMElementImpl) node).getLocalName().equals("wsdl")) {
                    String urlValue = ((OMElementImpl) node).getAttribute(new QName("uri")).getAttributeValue();

                    attribute = ((OMElementImpl) node).getAttribute(new QName("uri"));
                    System.out.println(((OMElementImpl) node).getAttribute(new QName("uri")));

                    ((OMElementImpl) node).getAttribute(new QName("uri")).setAttributeValue(getUrl(urlValue));
                    attribute2 = ((OMElementImpl) node).getAttribute(new QName("uri"));
                    changed = true;
                } else if (((OMElementImpl) node).getLocalName().equals("address")) {
                    String urlValue = ((OMElementImpl) node).getAttribute(new QName("uri")).getAttributeValue();

                    attribute = ((OMElementImpl) node).getAttribute(new QName("uri"));
                    System.out.println(((OMElementImpl) node).getAttribute(new QName("uri")));

                    ((OMElementImpl) node).getAttribute(new QName("uri")).setAttributeValue(getUrl(urlValue));
                    attribute2 = ((OMElementImpl) node).getAttribute(new QName("uri"));
                    changed = true;
                }

            }
        }
        if (changed) {
            endPointElem.removeAttribute(attribute);
            endPointElem.addAttribute(attribute2);
        }
        return endPointElem;
    }


    private void travarsrchild(OMNode node, OMAttribute attribute, OMAttribute attribute2)
            throws XMLStreamException, IOException {
        Iterator nodIterator = ((OMElementImpl) node).getChildElements();
        while (nodIterator.hasNext()) {
            OMNode targetNode = (OMNode) nodIterator.next();
            if (((OMElementImpl) targetNode).getLocalName().equals("endpoint")) {
                this.replaceElement(targetNode, attribute, attribute2);
                break;
            } else {
                travarsrchild(targetNode, attribute, attribute2);
            }
        }
    }


    private void replaceElement(OMNode targetNode, OMAttribute attribute, OMAttribute attribute2)
            throws XMLStreamException, IOException {
        Iterator endpointNode = ((OMElementImpl) targetNode).getChildElements();
        OMNode endpoint = (OMNode) endpointNode.next();
        if (((OMElementImpl) endpoint) != null) {
            String uri = ((OMElementImpl) endpoint).getAttribute(new QName("uri")).getAttributeValue();
            attribute = ((OMElementImpl) endpoint).getAttribute(new QName("uri"));
            ((OMElementImpl) endpoint).getAttribute(new QName("uri")).setAttributeValue(getUrl(uri));
            attribute2 = ((OMElementImpl) endpoint).getAttribute(new QName("uri"));
        }
    }


    private String getUrl(String endpoint) throws IOException, XMLStreamException {
        String newEndPoint = endpoint;

        DataHandler dh = new DataHandler(new URL("file://" + ProductConstant.
                SYSTEM_TEST_RESOURCE_LOCATION + File.separator +
                                                 "artifacts" + File.separator +
                                                 "ESB" + File.separator + "endpointlookup.xml"));
        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        OMElement endPointElem = builder.getDocumentElement();
        Iterator productList = endPointElem.getChildElements();
        boolean endpointFound = false;
        while (productList.hasNext()) {
            OMNode productNode = (OMNode) productList.next();
            Iterator urlList = ((OMElementImpl) productNode).getChildrenWithLocalName("url");
            while (urlList.hasNext()) {
                OMNode urlNode = (OMNode) urlList.next();
                String endpointType = ((OMElementImpl) urlNode).getAttribute(new QName("type"))
                        .getAttributeValue();
                userId = ((OMElementImpl) urlNode).getAttribute(new QName("user")).getAttributeValue();
                String codedEndpoint = ((OMElementImpl) urlNode).getText();
                if (codedEndpoint.toLowerCase().contains("https")) {
                    containsHttps = true;
                }
                String product = ((OMElementImpl) productNode).getLocalName();
                if (endpointType.equals("")) {

                }
                if (codedEndpoint.contains(endpoint)) {

                    newEndPoint = productLookup(product, endpoint);
                    endpointFound = true;
                    break;
                }
                if (endpointFound) {
                    break;
                }
            }
        }
        ((OMElementImpl) endPointElem.getChildElements().next()).getChildElements();
        return newEndPoint;
    }

    private String productLookup(String product, String oldUrl) {
        String url = null;
        FrameworkProperties properties = null;
        if (product.equals("as")) {
            properties = FrameworkFactory.getFrameworkProperties(ProductConstant.APP_SERVER_NAME);
        } else if (product.equals("esb")) {
            properties = FrameworkFactory.getFrameworkProperties(ProductConstant.ESB_SERVER_NAME);
        } else if (product.equals("bps")) {
            properties = FrameworkFactory.getFrameworkProperties(ProductConstant.BPS_SERVER_NAME);
        } else if (product.equals("is")) {
            properties = FrameworkFactory.getFrameworkProperties(ProductConstant.IS_SERVER_NAME);
        } else if (product.equals("bam")) {
            properties = FrameworkFactory.getFrameworkProperties(ProductConstant.BAM_SERVER_NAME);
        } else if (product.equals("brs")) {
            properties = FrameworkFactory.getFrameworkProperties(ProductConstant.BRS_SERVER_NAME);
        } else if (product.equals("ds")) {
            properties = FrameworkFactory.getFrameworkProperties(ProductConstant.DSS_SERVER_NAME);
        } else if (product.equals("greg")) {
            properties = FrameworkFactory.getFrameworkProperties(ProductConstant.GREG_SERVER_NAME);
        } else if (product.equals("gs")) {
            properties = FrameworkFactory.getFrameworkProperties(ProductConstant.GS_SERVER_NAME);
        } else {

        }
        UserInfo info = UserListCsvReader.getUserInfo(Integer.parseInt(userId));
        ProductUrlGeneratorUtil productUrlGeneratorUtil = new ProductUrlGeneratorUtil();
        if (!containsHttps) {
            assert properties != null;
            if (properties.getEnvironmentSettings().is_runningOnStratos()) {
                url = productUrlGeneratorUtil.getHttpServiceURLOfStratos(properties.
                        getProductVariables().getHttpPort(), properties.getProductVariables().
                        getNhttpPort(), properties.getProductVariables().getHostName(), properties, info);
                String service = oldUrl.substring(oldUrl.indexOf("services/") + 9);
                url = url + File.separator + service;

            } else {
                url = productUrlGeneratorUtil.getHttpServiceURLOfProduct(properties.
                        getProductVariables().getHttpPort(), properties.getProductVariables().
                        getNhttpPort(), properties.getProductVariables().getHostName(), properties);
                String service = oldUrl.substring(oldUrl.indexOf("services/"));
                url = url + File.separator + service;
            }
        } else if (containsHttps) {
            assert properties != null;
            if (properties.getEnvironmentSettings().is_runningOnStratos()) {
                url = properties.getProductVariables().getBackendUrl();
                String service = oldUrl.substring(oldUrl.indexOf("services/") + 9);
                url = url + File.separator + service;
            } else {
                url = properties.getProductVariables().getBackendUrl();
                String service = oldUrl.substring(oldUrl.indexOf("services/"));
                url = url + File.separator + service;
            }
        }

        return url;
    }
}