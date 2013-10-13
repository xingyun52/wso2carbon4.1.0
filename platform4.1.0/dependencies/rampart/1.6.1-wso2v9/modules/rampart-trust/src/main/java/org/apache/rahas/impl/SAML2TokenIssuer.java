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

package org.apache.rahas.impl;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasConstants;
import org.apache.rahas.RahasData;
import org.apache.rahas.Token;
import org.apache.rahas.TokenIssuer;
import org.apache.rahas.TrustException;
import org.apache.rahas.TrustUtil;
import org.apache.rahas.impl.util.SAMLAttributeCallback;
import org.apache.rahas.impl.util.SAMLCallbackHandler;
import org.apache.rahas.impl.util.SignKeyHolder;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.util.Base64;
import org.apache.ws.security.util.Loader;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.utils.EncryptionConstants;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.SAMLException;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.KeyInfoConfirmationDataType;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml2.core.impl.ConditionsBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.X509Data;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SAML2TokenIssuer implements TokenIssuer {

    private Assertion SAMLAssertion;

    private String configParamName;

    private OMElement configElement;

    private String configFile;

    protected List<Signature> signatureList = new ArrayList<Signature>();

    private boolean isSymmetricKeyBasedHoK = false;

    private static Log log = LogFactory.getLog(SAML2TokenIssuer.class);

    static {
            try {
                DefaultBootstrap.bootstrap();
            } catch (ConfigurationException e) {
                log.error("SAML2TokenIssuerBootstrapError", e);
                throw new RuntimeException(e);
            }
        }
    
    public SOAPEnvelope issue(RahasData data) throws TrustException {
        MessageContext inMsgCtx = data.getInMessageContext();

        try {
            SAMLTokenIssuerConfig config = null;
            if (this.configElement != null) {
                config = new SAMLTokenIssuerConfig(configElement
                        .getFirstChildWithName(SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG));
            }

            // Look for the file
            if (config == null && this.configFile != null) {
                config = new SAMLTokenIssuerConfig(this.configFile);
                //config = new SAMLTokenIssuerConfig("/home/thilina/Desktop/saml-issuer-config.xml");
            }

            // Look for the param
            if (config == null && this.configParamName != null) {
                Parameter param = inMsgCtx.getParameter(this.configParamName);
                if (param != null && param.getParameterElement() != null) {
                    config = new SAMLTokenIssuerConfig(param
                            .getParameterElement().getFirstChildWithName(
                            SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG));
                } else {
                    throw new TrustException("expectedParameterMissing",
                            new String[]{this.configParamName});
                }
            }

            if (config == null) {
                throw new TrustException("configurationIsNull");
            }

            //initialize and set token persister and config in configuration context.
            if (TokenIssuerUtil.isPersisterConfigured(config)) {
                TokenIssuerUtil.manageTokenPersistenceSettings(config, inMsgCtx);
            }

            SOAPEnvelope env = TrustUtil.createSOAPEnvelope(inMsgCtx
                    .getEnvelope().getNamespace().getNamespaceURI());

            Crypto crypto;
            if (config.cryptoElement != null) { // crypto props
                // defined as
                // elements
                crypto = CryptoFactory.getInstance(TrustUtil
                        .toProperties(config.cryptoElement), inMsgCtx
                        .getAxisService().getClassLoader());
            } else { // crypto props defined in a properties file
                crypto = CryptoFactory.getInstance(config.cryptoPropertiesFile,
                        inMsgCtx.getAxisService().getClassLoader());
            }


            // Get the document
            Document doc = ((Element) env).getOwnerDocument();

            // Get the key size and create a new byte array of that size
            int keySize = data.getKeysize();
            String keyType = data.getKeyType();

            keySize = (keySize == -1) ? config.keySize : keySize;

            //Build the assertion
            AssertionBuilder assertionBuilder = new AssertionBuilder();
            Assertion assertion = assertionBuilder.buildObject();
            assertion.setVersion(SAMLVersion.VERSION_20);

            // Set an UUID as the ID of an assertion
            assertion.setID(UUIDGenerator.getUUID());

            //Set the issuer
            IssuerBuilder issuerBuilder = new IssuerBuilder();
            Issuer issuer = issuerBuilder.buildObject();
            issuer.setValue(config.issuerName);
            assertion.setIssuer(issuer);

            // Set the issued time.
            assertion.setIssueInstant(new DateTime());

            // Validity period
            DateTime creationDate = new DateTime();
            DateTime expirationDate = new DateTime(creationDate.getMillis() + config.ttl);

            // These variables are used to build the trust assertion
            Date creationTime = creationDate.toDate();
            Date expirationTime = expirationDate.toDate();

            Conditions conditions = new ConditionsBuilder().buildObject();
            conditions.setNotBefore(creationDate);
            conditions.setNotOnOrAfter(expirationDate);
            assertion.setConditions(conditions);

            // Create the subject
            Subject subject;

            if (!data.getKeyType().endsWith(RahasConstants.KEY_TYPE_BEARER)) {
                subject = createSubjectWithHolderOfKeySC(config, doc, crypto, creationDate, expirationDate, data);
            }
            else{
                subject = createSubjectWithBearerSC(data);
            }

            // Set the subject
            assertion.setSubject(subject);

            // If a SymmetricKey is used build an attr stmt, if a public key is build an authn stmt. 
            if (isSymmetricKeyBasedHoK) {
                AttributeStatement attrStmt = createAttributeStatement(data, config);
                assertion.getAttributeStatements().add(attrStmt);
            } else {
                AuthnStatement authStmt = createAuthnStatement(data);
                assertion.getAuthnStatements().add(authStmt);
                if (data.getClaimDialect() != null && data.getClaimElem() != null) {
                    assertion.getAttributeStatements().add(createAttributeStatement(data, config));
                }
            }

            // Create a SignKeyHolder to hold the crypto objects that are used to sign the assertion
            SignKeyHolder signKeyHolder = createSignKeyHolder(config, crypto);

            // Sign the assertion
            assertion = setSignature(assertion, signKeyHolder);


            OMElement rstrElem;
            int wstVersion = data.getVersion();
            if (RahasConstants.VERSION_05_02 == wstVersion) {
                rstrElem = TrustUtil.createRequestSecurityTokenResponseElement(
                        wstVersion, env.getBody());
            } else {
                OMElement rstrcElem = TrustUtil
                        .createRequestSecurityTokenResponseCollectionElement(
                                wstVersion, env.getBody());
                rstrElem = TrustUtil.createRequestSecurityTokenResponseElement(
                        wstVersion, rstrcElem);
            }

            TrustUtil.createTokenTypeElement(wstVersion, rstrElem).setText(
                    RahasConstants.TOK_TYPE_SAML_20);

            if (keyType.endsWith(RahasConstants.KEY_TYPE_SYMM_KEY)) {
                TrustUtil.createKeySizeElement(wstVersion, rstrElem, keySize);
            }

            if (config.addRequestedAttachedRef) {
                TrustUtil.createRequestedAttachedRef(wstVersion, rstrElem, "#"
                        + assertion.getID(), RahasConstants.TOK_TYPE_SAML_20);
            }

            if (config.addRequestedUnattachedRef) {
                TrustUtil.createRequestedUnattachedRef(wstVersion, rstrElem,
                        assertion.getID(), RahasConstants.TOK_TYPE_SAML_20);
            }

            if (data.getAppliesToAddress() != null) {
                TrustUtil.createAppliesToElement(rstrElem, data
                        .getAppliesToAddress(), data.getAddressingNs());
            }

            // Use GMT time in milliseconds
            DateFormat zulu = new XmlSchemaDateFormat();

            // Add the Lifetime element
            TrustUtil.createLifetimeElement(wstVersion, rstrElem, zulu
                    .format(creationTime), zulu.format(expirationTime));

            // Create the RequestedSecurityToken element and add the SAML token
            // to it
            OMElement reqSecTokenElem = TrustUtil
                    .createRequestedSecurityTokenElement(wstVersion, rstrElem);
            Token assertionToken;

            Node tempNode = assertion.getDOM();

            //Serializing and re-generating the AXIOM element using the DOM Element created using xerces
            Element element = assertion.getDOM();

            ByteArrayOutputStream byteArrayOutputStrm = new ByteArrayOutputStream();

            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();

            DOMImplementationLS impl =
                    (DOMImplementationLS) registry.getDOMImplementation("LS");

            LSSerializer writer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            output.setByteStream(byteArrayOutputStrm);
            writer.write(element, output);
            String elementString = byteArrayOutputStrm.toString();

            OMElement assertionElement = AXIOMUtil.stringToOM(elementString);

            reqSecTokenElem.addChild((OMNode) ((Element) rstrElem)
                    .getOwnerDocument().importNode(tempNode, true));

            // Store the token
            assertionToken = new Token(assertion.getID(),
                    (OMElement) assertionElement, creationTime,
                    expirationTime);

            // At this point we definitely have the secret
            // Otherwise it should fail with an exception earlier
            assertionToken.setSecret(data.getEphmeralKey());
            //SAML tokens are enabled for persistence only if token store is not disabled.
            if (!config.isTokenStoreDisabled()){
                assertionToken.setPersistenceEnabled(true);
                TrustUtil.getTokenStore(inMsgCtx).add(assertionToken);
            }

            if (keyType.endsWith(RahasConstants.KEY_TYPE_SYMM_KEY)
                && config.keyComputation != SAMLTokenIssuerConfig.KeyComputation.KEY_COMP_USE_REQ_ENT) {

                // Add the RequestedProofToken
                TokenIssuerUtil.handleRequestedProofToken(data, wstVersion,
                                                          config, rstrElem, assertionToken, doc);
            }

            return env;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * This method is used to create the subject of an assertion
     * @param config
     * @param doc
     * @param crypto
     * @param creationTime
     * @param expirationTime
     * @param data
     * @return Subject
     * @throws Exception
     */
    private Subject createSubjectWithHolderOfKeySC(SAMLTokenIssuerConfig config,
                                                   Document doc, Crypto crypto,
                                                   DateTime creationTime,
                                                   DateTime expirationTime, RahasData data) throws Exception {


        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
        SAMLObjectBuilder<Subject> subjectBuilder =
                (SAMLObjectBuilder<Subject>) builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME);
        Subject subject = subjectBuilder.buildObject();
        Element keyInfoElem = null;

        // If it is a Symmetric Key
        if (data.getKeyType().endsWith(RahasConstants.KEY_TYPE_SYMM_KEY)) {

            isSymmetricKeyBasedHoK = true;
            Element encryptedKeyElem;
            X509Certificate serviceCert = null;
            try {
                if (data.getPrincipal() != null) {
                    //get subject's name from Rahas data
                    String subjectNameID = data.getPrincipal().getName();
                    //Create NameID and attach it to the subject
                    NameID nameID = new NameIDBuilder().buildObject();
                    nameID.setValue(subjectNameID);
                    nameID.setFormat(NameIdentifier.EMAIL);
                    subject.setNameID(nameID);
                }
                // Get ApliesTo to figure out which service to issue the token
                // for
                serviceCert = config.getServiceCert(crypto, data.getAppliesToAddress());

                // Create the encrypted key
                WSSecEncryptedKey encrKeyBuilder = new WSSecEncryptedKey();

                // Use thumbprint id
                encrKeyBuilder
                        .setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);

                // SEt the encryption cert
                encrKeyBuilder.setUseThisCert(serviceCert);

                // set keysize
                int keysize = data.getKeysize();
                keysize = (keysize != -1) ? keysize : config.keySize;
                encrKeyBuilder.setKeySize(keysize);

                encrKeyBuilder.setEphemeralKey(TokenIssuerUtil.getSharedSecret(
                        data, config.keyComputation, keysize));

                // Set key encryption algo
                encrKeyBuilder
                        .setKeyEncAlgo(EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSA15);

                // Build
                encrKeyBuilder.prepare(doc, crypto);

                // Extract the base64 encoded secret value
                byte[] tempKey = new byte[keysize / 8];
                System.arraycopy(encrKeyBuilder.getEphemeralKey(), 0, tempKey,
                        0, keysize / 8);

                data.setEphmeralKey(tempKey);

                // Extract the Encryptedkey DOM element
                encryptedKeyElem = encrKeyBuilder.getEncryptedKeyElement();
            } catch (WSSecurityException e) {
                throw new TrustException(
                        "errorInBuildingTheEncryptedKeyForPrincipal",
                        new String[]{serviceCert.getSubjectDN().getName()},
                        e);
            }

            keyInfoElem = doc.createElementNS(WSConstants.SIG_NS,
                    "ds:KeyInfo");
            ((OMElement) encryptedKeyElem).declareNamespace(WSConstants.SIG_NS,
                    WSConstants.SIG_PREFIX);
            ((OMElement) encryptedKeyElem).declareNamespace(WSConstants.ENC_NS,
                    WSConstants.ENC_PREFIX);

            keyInfoElem.appendChild(encryptedKeyElem);

        }

        // If it is a public Key
        else if(data.getKeyType().endsWith(RahasConstants.KEY_TYPE_PUBLIC_KEY)){
            try {
                String subjectNameId = data.getPrincipal().getName();

                //Create NameID and attach it to the subject
                NameIDBuilder nb = new NameIDBuilder();
                NameID nameID = nb.buildObject();
                nameID.setValue(subjectNameId);
                nameID.setFormat(NameIdentifier.EMAIL);
                subject.setNameID(nameID);


                // Create the ds:KeyValue element with the ds:X509Data
                X509Certificate clientCert = data.getClientCert();

                if (clientCert == null) {
                    X509Certificate[] certs = crypto.getCertificates(
                            data.getPrincipal().getName());
                    clientCert = certs[0];
                }

                byte[] clientCertBytes = clientCert.getEncoded();

                String base64Cert = Base64.encode(clientCertBytes);

                Text base64CertText = doc.createTextNode(base64Cert);

                //-----------------------------------------

                Element x509CertElem = doc.createElementNS(WSConstants.SIG_NS,
                        "ds:X509Certificate");
                x509CertElem.appendChild(base64CertText);
                Element x509DataElem = doc.createElementNS(WSConstants.SIG_NS,
                        "ds:X509Data");
                x509DataElem.appendChild(x509CertElem);


                if (x509DataElem != null) {
                    keyInfoElem = doc.createElementNS(WSConstants.SIG_NS, "ds:KeyInfo");
                    ((OMElement) x509DataElem).declareNamespace(
                            WSConstants.SIG_NS, WSConstants.SIG_PREFIX);
                    keyInfoElem.appendChild(x509DataElem);
                }

            } catch (Exception e) {
                throw new TrustException("samlAssertionCreationError", e);
            }
        }

        // Unmarshall the keyInfo DOM element into an XMLObject
        String keyInfoElementString = keyInfoElem.toString();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(keyInfoElementString.trim().getBytes()));
        Element element = document.getDocumentElement();


        // Get appropriate unmarshaller
        UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);

        // Unmarshall using the document root element, an keyInfo element in this case
        XMLObject keyInfoElement = null;
        try {
            keyInfoElement = unmarshaller.unmarshall(element);
        } catch (UnmarshallingException e) {
            throw new TrustException("Error unmarshalling KeyInfo Element", e);
        }


        //Build the Subject Confirmation
        SAMLObjectBuilder<SubjectConfirmation> subjectConfirmationBuilder =
                (SAMLObjectBuilder<SubjectConfirmation>) builderFactory.getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        SubjectConfirmation subjectConfirmation = subjectConfirmationBuilder.buildObject();

        //Set the subject Confirmation method
        subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:holder-of-key");

        SAMLObjectBuilder<KeyInfoConfirmationDataType> keyInfoSubjectConfirmationDataBuilder =
                (SAMLObjectBuilder<KeyInfoConfirmationDataType>) builderFactory.getBuilder(KeyInfoConfirmationDataType.TYPE_NAME);

        //Build the subject confirmation data element
        KeyInfoConfirmationDataType scData = keyInfoSubjectConfirmationDataBuilder.
                buildObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME, KeyInfoConfirmationDataType.TYPE_NAME);

        //Set the keyInfo element
        scData.getKeyInfos().add(keyInfoElement);

        // Set the validity period
        scData.setNotBefore(creationTime);
        scData.setNotOnOrAfter(expirationTime);

        //Set the subject confirmation data
        subjectConfirmation.setSubjectConfirmationData(scData);

        //set the subject confirmation
        subject.getSubjectConfirmations().add(subjectConfirmation);

        log.debug("SAML2.0 subject is constructed successfully.");
        return subject;
    }

    /**
     * This method creates a subject element with the bearer subject confirmation method
     * @param data RahasData element
     * @return  SAML 2.0 Subject element with Bearer subject confirmation
     */
    private Subject createSubjectWithBearerSC(RahasData data){
        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
        SAMLObjectBuilder<Subject> subjectBuilder =
                (SAMLObjectBuilder<Subject>) builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME);
        Subject subject = subjectBuilder.buildObject();

        //Create NameID and attach it to the subject
        NameID nameID = new NameIDBuilder().buildObject();
        nameID.setValue(data.getPrincipal().getName());
        nameID.setFormat(NameIdentifier.EMAIL);
        subject.setNameID(nameID);

        //Build the Subject Confirmation
        SAMLObjectBuilder<SubjectConfirmation> subjectConfirmationBuilder =
                (SAMLObjectBuilder<SubjectConfirmation>) builderFactory.getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        SubjectConfirmation subjectConfirmation = subjectConfirmationBuilder.buildObject();

        //Set the subject Confirmation method
        subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");

        subject.getSubjectConfirmations().add(subjectConfirmation);
        return subject;
    }


    /**
     * This method is used to sign the assertion
     * @param assertion
     * @param cred
     * @return Assertion
     * @throws Exception
     */
    public Assertion setSignature(Assertion assertion, SignKeyHolder cred) throws Exception {

        // Build the signature object and set the credentials.
        Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
        signature.setSigningCredential(cred);
        signature.setSignatureAlgorithm(cred.getSignatureAlgorithm());
        signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        //Build the KeyInfo element and set the certificate
        try {
            KeyInfo keyInfo = (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
            X509Data data = (X509Data) buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
            org.opensaml.xml.signature.X509Certificate cert = (org.opensaml.xml.signature.X509Certificate) buildXMLObject(org.opensaml.xml.signature.X509Certificate.DEFAULT_ELEMENT_NAME);
            String value = org.apache.xml.security.utils.Base64.encode(cred.getEntityCertificate().getEncoded());
            cert.setValue(value);
            data.getX509Certificates().add(cert);
            keyInfo.getX509Datas().add(data);
            signature.setKeyInfo(keyInfo);
            assertion.setSignature(signature);
            signatureList.add(signature);

            //Marshall and Sign
            MarshallerFactory marshallerFactory = org.opensaml.xml.Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(assertion);
            marshaller.marshall(assertion);
            org.apache.xml.security.Init.init();
            Signer.signObjects(signatureList);
        } catch (CertificateEncodingException e) {
            throw new TrustException("Error in setting the signature", e);
        } catch (SignatureException e) {
            throw new TrustException("errorMarshellingOrSigning", e);
        } catch (MarshallingException e) {
            throw new TrustException("errorMarshellingOrSigning", e);
        }

        log.debug("SAML2.0 assertion is marshalled and signed..");

        return assertion;
    }


    /**
     * This method is used to build the assertion elements
     * @param objectQName
     * @return
     * @throws Exception
     */
    protected static XMLObject buildXMLObject(QName objectQName) throws Exception {
        XMLObjectBuilder builder = org.opensaml.xml.Configuration.getBuilderFactory().getBuilder(objectQName);
        if (builder == null) {
            throw new TrustException("Unable to retrieve builder for object QName "
                    + objectQName);
        }
        return builder.buildObject(objectQName.getNamespaceURI(), objectQName.getLocalPart(),
                objectQName.getPrefix());
    }

    /**
     * This method is used to create SignKeyHolder instances that contains the credentials required for signing the
     * assertion
     * @param config
     * @param crypto
     * @return
     * @throws TrustException
     */
    private SignKeyHolder createSignKeyHolder(SAMLTokenIssuerConfig config, Crypto crypto) throws TrustException {

        SignKeyHolder signKeyHolder = new SignKeyHolder();

        try {
            X509Certificate[] issuerCerts = crypto
                    .getCertificates(config.issuerKeyAlias);

            String sigAlgo = XMLSignature.ALGO_ID_SIGNATURE_RSA;
            String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();
            if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
                sigAlgo = XMLSignature.ALGO_ID_SIGNATURE_DSA;
            }
            java.security.Key issuerPK = crypto.getPrivateKey(
                    config.issuerKeyAlias, config.issuerKeyPassword);

            signKeyHolder.setIssuerCerts(issuerCerts);
            signKeyHolder.setIssuerPK((PrivateKey) issuerPK);
            signKeyHolder.setSignatureAlgorithm(sigAlgo);

        } catch (Exception e) {
            throw new TrustException("Error creating issuer signature");
        }

        log.debug("SignKeyHolder object is created with the credentials..");

        return signKeyHolder;
    }

    /**
     * Creates the Attribute Statement
     * @param data
     * @param config
     * @return
     * @throws SAMLException
     */
    private AttributeStatement createAttributeStatement(RahasData data, SAMLTokenIssuerConfig config) throws SAMLException, TrustException {

        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
        SAMLObjectBuilder<AttributeStatement> attrStmtBuilder =
                (SAMLObjectBuilder<AttributeStatement>) builderFactory.getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);

        SAMLObjectBuilder<Attribute> attrBuilder =
                    (SAMLObjectBuilder<Attribute>) builderFactory.getBuilder(Attribute.DEFAULT_ELEMENT_NAME);

        AttributeStatement attrstmt = attrStmtBuilder.buildObject();

        Attribute[] attributes = null;

        //Call the attribute callback handlers to get any attributes if exists
        if (config.getCallbackHandler() != null) {
            SAMLAttributeCallback cb = new SAMLAttributeCallback(data);
            SAMLCallbackHandler handler = config.getCallbackHandler();
            handler.handle(cb);
            attributes = cb.getSAML2Attributes();
        }
        else if (config.getCallbackHandlerName() != null
                && config.getCallbackHandlerName().trim().length() > 0) {
            SAMLAttributeCallback cb = new SAMLAttributeCallback(data);
            SAMLCallbackHandler handler = null;
            MessageContext msgContext = data.getInMessageContext();
            ClassLoader classLoader = msgContext.getAxisService().getClassLoader();
            Class cbClass = null;
            try {
                cbClass = Loader.loadClass(classLoader, config.getCallbackHandlerName());
            } catch (ClassNotFoundException e) {
                throw new TrustException("cannotLoadPWCBClass", new String[]{config
                        .getCallbackHandlerName()}, e);
            }
            try {
                handler = (SAMLCallbackHandler) cbClass.newInstance();
            } catch (java.lang.Exception e) {
                throw new TrustException("cannotCreatePWCBInstance", new String[]{config
                        .getCallbackHandlerName()}, e);
            }
            handler.handle(cb);
            attributes = cb.getSAML2Attributes();
            // else add the attribute with a default value
        } 

        //else add the attribute with a default value
        else {
            Attribute attribute = attrBuilder.buildObject();
            attribute.setName("Name");
            attribute.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");

            XSStringBuilder attributeValueBuilder = (XSStringBuilder) builderFactory
                    .getBuilder(XSString.TYPE_NAME);

            XSString stringValue = attributeValueBuilder.buildObject(
                    AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            stringValue.setValue("Colombo/Rahas");
            attribute.getAttributeValues().add(stringValue);
            attributes = new Attribute[1];
            attributes[0] = attribute;
        }
        //add attributes to the attribute statement
        attrstmt.getAttributes().addAll(Arrays.asList(attributes));

        log.debug("SAML2.0 attribute statement is constructed successfully.");

        return attrstmt;
    }

    /**
     * build the authentication statement
     * @param data
     * @return
     */
    private AuthnStatement createAuthnStatement(RahasData data) {
        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
        MessageContext inMsgCtx = data.getInMessageContext();

        SAMLObjectBuilder<AuthnStatement> authStmtBuilder =
                (SAMLObjectBuilder<AuthnStatement>) builderFactory.getBuilder(AuthnStatement.DEFAULT_ELEMENT_NAME);

        //build the auth stmt
        AuthnStatement authStmt = authStmtBuilder.buildObject();

        // set the authn instance
        authStmt.setAuthnInstant(new DateTime());

        SAMLObjectBuilder<AuthnContext> authCtxBuilder =
                (SAMLObjectBuilder<AuthnContext>) builderFactory.getBuilder(AuthnContext.DEFAULT_ELEMENT_NAME);
        AuthnContext authContext = authCtxBuilder.buildObject();

        SAMLObjectBuilder<AuthnContextClassRef> authCtxClassRefBuilder =
                (SAMLObjectBuilder<AuthnContextClassRef>) builderFactory.getBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        AuthnContextClassRef authCtxClassRef = authCtxClassRefBuilder.buildObject();
        
        //if username/password based authn
        if (inMsgCtx.getProperty(RahasConstants.USERNAME) != null) {
            authCtxClassRef.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);
        }
        //if X.509 cert based authn
        else if (inMsgCtx.getProperty(RahasConstants.X509_CERT) != null) {
            authCtxClassRef.setAuthnContextClassRef(AuthnContext.X509_AUTHN_CTX);
        }

        authContext.setAuthnContextClassRef(authCtxClassRef);
        authStmt.setAuthnContext(authContext);

        log.debug("SAML2.0 authentication statement is constructed successfully.");

        return authStmt;
    }


    public String getResponseAction(RahasData data) throws TrustException {
        return null;
    }

    public void setConfigurationFile(String configFile) {
        this.configFile = configFile;
    }

    public void setConfigurationElement(OMElement configElement) {
        this.configElement = configElement;
    }

    public void setConfigurationParamName(String configParamName) {
        this.configParamName = configParamName;
    }

}
