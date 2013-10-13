/*
 * Copyright 2006,2007 WSO2, Inc. http://www.wso2.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.mashup.javascript.hostobjects.email;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.mashup.javascript.hostobjects.email.internal.EmailServiceComponent;
import org.wso2.carbon.mashup.javascript.hostobjects.file.FileHostObject;
import org.wso2.carbon.mashup.javascript.hostobjects.hostobjectservice.service.HostObjectService;
import org.wso2.carbon.mashup.utils.MashupConstants;
import org.wso2.javascript.xmlimpl.XML;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * <p/>
 * The Email host object allows users to send out email from their mashups.  It helps notify users
 * of certain events and acts as a bridge between mashups and users.
 * <p/>
 * Notes:
 * <p/>
 * The constructor of the Emal object can be called with or without user credentials. If its called with credentials
 * they are used to authenticate the user. If the function is called without credentials the details
 * are taken from the server.xml found under conf directory where the mashup server is located.
 * So if you wish to keep the credentials in server.xml please update it with the needed usernames
 * and passwords. The section that corresponds to this is as follows.
 *
 * <p>
 * <!--Used to configure your default email account that will be used to send emails from mashups using the Email host Object-->
 *   <EmailConfig>
 *       <host>smtp.gmail.com</host>
 *       <port>25</port>
 *       <username>username@gmail.com</username>
 *       <password>password</password>
 *   </EmailConfig>
 * <p/>
 * </p>
 * <p/>
 * <pre>
 * eg:
 * <p/>
 *     function sendEmail(){
 *          var email = new Email("host", "port", "username", "password");
 *          var file = new File("temp.txt");
 *          email.from = "keith@wso2.com";
 *          email.to = "keith@wso2.com"; // alternatively message.to can be a array of strings. Same goes for cc and bcc
 *          email.cc = "keith@wso2.com";
 *          email.bcc = "keith@wso2.com";
 *          email.subject = "WSO2 Mashup server 1.0 Released";
 *          email.addAttachement(file, "temp.txt"); // Optionally can add attachements, it has a variable number of arguments. each argument can be a File hostObject or a string representing a file.
 *          email.text = "WSO2 Mashup server 1.0 was Released on 28th January 2008";
 *          email.send();
 *     }
 * <p/>
 * </pre>
 * </p>
 */
public class EmailHostObject extends ScriptableObject {

    private Properties properties;
    private MimeMessage message;
    private String text;
    private String html;
    Multipart multipart;

    /**
     * Return the name of the class.
     * <p/>
     * This is typically the same name as the constructor.
     * Classes extending ScriptableObject must implement this abstract
     * method.
     */
    public String getClassName() {
        return "Email";
    }

    /**
     * <p>
     * The Email Object has three different constructors. Choose one depending on your configuration
     * and your needs.
     *
     * 1. The first constructor takes no parameters and uses configuration information specified in the
     * server.xml. Using a configuration such as this is useful if you want to use a default email
     * account to send out mail from your mashups. It also reduces the hassle of having to key in
     * the configuration details each time you need a new email object.
     *
     * var email = new Email();
     *
     * 2. The second constructor, unlike the first, requires the user to provide the configuration
     * details each time he creates a new email object.  The benefit is that no server configuration
     * is needed and you can use diffent accounts when ever you need. The configuration details
     * should be given as follows:
     *
     * var email = new Email("smtp.gmail.com", "25", "username@gmail.com", "password"); // host, port, username, password
     *
     * 3. The third is a slight variant of the second. It does not require a port to be specified:
     *
     * var email = new Email("smtp.gmail.com", "username@gmail.com", "password"); // host, username, password
     * </p>
     */
    public static Scriptable jsConstructor(Context cx, Object[] args, Function ctorObj,
                                           boolean inNewExpr) throws CarbonException {

        EmailHostObject emailHostObject = new EmailHostObject();
        Properties props = new Properties();
        emailHostObject.properties = props;
        emailHostObject.multipart = new MimeMultipart();

        String host, username, password;
        String port = null;
        ServerConfiguration serverConfig = ServerConfiguration.getInstance();

        int length = args.length;
        if (length == 3) {
            // We assume that the three parameters are host, username and password
            host = (String) args[0];
            username = (String) args[1];
            password = (String) args[2];
            port = serverConfig.getFirstProperty(MashupConstants.PORT);
        } else if (length == 4) {
            //We assume that the parameters are host, port, username and password
            host = (String) args[0];
            port = (String) args[1];
            username = (String) args[2];
            password = (String) args[3];
        } else {
            throw new CarbonException("Incorrect number of arguments. Please specify host, username, " +
                    "password or host, port, username, password within the constructor of Email hostobject.");
        }

        if (host == null) {
            throw new CarbonException("Invalid host name. Please recheck the given details " +
                    "within the constructor of Email hostobject.");
        }
        emailHostObject.setProperty("mail.smtp.host", host);

        if (port != null) {
            emailHostObject.setProperty("mail.smtp.port", port);
        }

        SMTPAuthenticator smtpAuthenticator = null;
        if (username != null){
            smtpAuthenticator = new SMTPAuthenticator(username, password);
            emailHostObject.setProperty("mail.smtp.auth", "true");
        }
        Session session = Session.getInstance(props, smtpAuthenticator);
        emailHostObject.message = new MimeMessage(session);

        emailHostObject.setProperty("mail.smtp.starttls.enable", "true");

        return emailHostObject;
    }

    private void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * <p>The from address to appear in the email</p>
     * <pre>
     * email.from = "keith@wso2.com";
     * </pre>
     */
    public void jsSet_from(String from) throws CarbonException {
        try {
            message.setFrom(new InternetAddress(from));
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }
    }

    public String jsGet_from() throws MessagingException {
        String from = null;
        Address[] addresses = message.getFrom();
        if (addresses != null && addresses.length > 0) {
            from = addresses[0].toString();
        }
        return from;
    }

    public String[] jsGet_to() throws CarbonException {
        Address[] addresses;
        try {
            addresses = message.getRecipients(Message.RecipientType.TO);
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }
        String[] to = new String[addresses.length];
        for (int i = 0; i < to.length; i++) {
            to[i] = addresses[i].toString();
        }
        return to;
    }

    /**
     * <p>The to address that the mail is sent to</p>
     * <pre>
     * email.to = "keith@wso2.com";
     *
     * OR
     *
     * var to = new Array();
     * to[0] = "jonathan@wso2.com";
     * to[1] =  "keith@wso2.com";
     * email.to = to;
     * </pre>
     */
    public void jsSet_to(Object toObject) throws CarbonException {
        addRecipients(Message.RecipientType.TO, toObject);
    }

    public String[] jsGet_cc() throws CarbonException {
        Address[] addresses;
        try {
            addresses = message.getRecipients(Message.RecipientType.CC);
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }
        String[] cc = new String[addresses.length];
        for (int i = 0; i < cc.length; i++) {
            cc[i] = addresses[i].toString();
        }
        return cc;
    }

    /**
     * <p>The cc address that the mail is sent to</p>
     * <pre>
     * email.cc = "keith@wso2.com";
     *
     * OR
     *
     * var cc = new Array();
     * cc[0] = "jonathan@wso2.com";
     * cc[1] =  "keith@wso2.com";
     * email.cc = cc;
     * </pre>
     */
    public void jsSet_cc(Object ccObject) throws CarbonException {
        addRecipients(Message.RecipientType.CC, ccObject);
    }

    public String[] jsGet_bcc() throws CarbonException {
        Address[] addresses;
        try {
            addresses = message.getRecipients(Message.RecipientType.BCC);
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }
        String[] bcc = new String[addresses.length];
        for (int i = 0; i < bcc.length; i++) {
            bcc[i] = addresses[i].toString();
        }
        return bcc;
    }

    /**
     * <p>The bcc address that the mail is sent to</p>
     * <pre>
     * email.bcc = "keith@wso2.com";
     *
     * OR
     *
     * var bcc = new Array();
     * bcc[0] = "jonathan@wso2.com";
     * bcc[1] =  "keith@wso2.com";
     * email.bcc = bcc;
     * </pre>
     */
    public void jsSet_bcc(Object bccObject) throws CarbonException {
        addRecipients(Message.RecipientType.BCC, bccObject);

    }

    /**
     * <p>The subject of the mail been sent</p>
     * <pre>
     * email.subject = "WSO2 Mashup server 1.0 Released";
     * </pre>
     */
    public void jsSet_subject(String subject) throws CarbonException {
        try {
            message.setSubject(subject);
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }
    }

    public String jsGet_subject() throws CarbonException {
        try {
            return message.getSubject();
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }
    }

    /**
     * <p>The body text of the mail been sent</p>
     * <pre>
     * email.text = "WSO2 Mashup server 1.0 was Released on 28th January 2008";
     * </pre>
     */
    public void jsSet_text(String text) throws CarbonException {
        this.text = text;
        BodyPart messageBodyPart = new MimeBodyPart();
        try {
            messageBodyPart.setText(text);
            multipart.addBodyPart(messageBodyPart);
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }
    }

    public String jsGet_text() {
        return text;
    }

    /**
     * <p>The body of the email to be sent. This function can be used to send HTML mail.</p>
     * <pre>
     * email.html = "<h1>WSO2 Mashup server 1.0 was Released on 28th January 2008</h1>";  // Setthing the HTML content as a String
     *                                                   OR
     * email.html = <h1>WSO2 Mashup server 1.0 was Released on 28th January 2008</h1>;     // Setting the HTML content as an XML object
     * </pre>
     */
    public void jsSet_html(Object html) throws CarbonException {
        if (html instanceof String) {
            this.html = (String) html;
        } else if (html instanceof XML) {
            OMNode node = ((XML) html).getAxiomFromXML();
                if (node instanceof OMElement) {
                    OMElement htmlElement = (OMElement) node;
                    this.html = htmlElement.toString();
                } else {
                    throw new CarbonException("Invalid input argument. The html function accepts " +
                            "either a String or an XML element.");
                }
        } else {
            throw new CarbonException("Invalid input argument. The html function accepts " +
                            "either a String or an XML element.");
        }
        BodyPart messageBodyPart = new MimeBodyPart();
        DataHandler dataHandler = null;
        try {
            dataHandler = new DataHandler(
                    new ByteArrayDataSource(this.html, "text/html"));
            messageBodyPart.setDataHandler(dataHandler);
            multipart.addBodyPart(messageBodyPart);
        } catch (IOException e) {
            throw new CarbonException(e);
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }

    }

    public String jsGet_html() {
        return html;
    }

    /**
     * <p>Send the mail out</p>
     * <pre>
     * email.send()
     * </pre>
     */
    public void jsFunction_send() throws CarbonException {
        try {
            Date currentDate=new Date();
            message.setSentDate(currentDate);
            message.setContent(multipart);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }
    }
    public static void jsFunction_addAttachement(Context cx, Scriptable thisObj, Object[] arguments,
                                                Function funObj) throws CarbonException {
            EmailHostObject.jsFunction_addAttachment(cx, thisObj, arguments,funObj);
    }

    /**
     * <p>Add attachments to the mail been sent. This function  has a variable number of arguments,
     * each argument can be a File hostObject or a string representing a file.</p>
     * <pre>
     * var file = new File("temp.txt"); // A file exists at temp.txt
     * email.addAttachement(file, "temp.txt");
     * </pre>
     */
    public static void jsFunction_addAttachment(Context cx, Scriptable thisObj, Object[] arguments,
                                                Function funObj) throws CarbonException {
        if (!isJavaScriptFileObjectAvailable()) {
            throw new CarbonException("Cannot find the FileHostObject class. " +
                    "Make sure that its present in the classpath");
        }
        EmailHostObject emailHostObject = (EmailHostObject) thisObj;
        for (int i = 0; i < arguments.length; i++) {
            FileHostObject fileHostObject;
            Object object = arguments[i];
            if (object instanceof FileHostObject) {
                fileHostObject = (FileHostObject) object;
            } else if (object instanceof String){
                fileHostObject = (FileHostObject) cx.newObject(emailHostObject, "File", new Object[]{object});
            } else {
                throw new CarbonException("Invalid parameter. The attachment should be a " +
                    "FileHostObject or a string representing the path of a file");
            }
            BodyPart messageBodyPart = new MimeBodyPart();
            File file = fileHostObject.getFile();
            DataSource source = new FileDataSource(file);
            try {
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(file.getName());
                emailHostObject.multipart.addBodyPart(messageBodyPart);
            } catch (MessagingException e) {
                throw new CarbonException(e);
            }
        }
    }

    private static class SMTPAuthenticator extends javax.mail.Authenticator {

        private String username, password;

        private SMTPAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }

    private void addRecipients(Message.RecipientType recipientType, Object recipientObject)
            throws CarbonException {
        try {
            if (recipientObject instanceof String[]) {
                String[] to = (String[]) recipientObject;
                InternetAddress[] recipientAddresses = new InternetAddress[to.length];
                for (int i = 0; i < to.length; i++) {
                    recipientAddresses[i] = new InternetAddress(to[i]);
                }
                message.addRecipients(recipientType, recipientAddresses);
            } else if (recipientObject instanceof NativeArray) {
                NativeArray nativeArray = (NativeArray) recipientObject;
                Object[] objects = nativeArray.getIds();
                for (int i = 0; i < objects.length; i++) {
                    Object object = objects[i];
                    Object o;
                    if (object instanceof String) {
                        String property = (String) object;
                        o = nativeArray.get(property, nativeArray);
                    } else {
                        Integer property = (Integer) object;
                        o = nativeArray.get(property.intValue(), nativeArray);
                    }
                    message.addRecipient(recipientType, new InternetAddress((String) o));
                }
            } else if (recipientObject instanceof String) {
                message.addRecipient(recipientType, new InternetAddress((String) recipientObject));
            } else {
                throw new CarbonException(
                        "The argument to this function should be an array of email addresses or a " +
                                "single email address");
            }
        } catch (MessagingException e) {
            throw new CarbonException(e);
        }
    }

    private static boolean isJavaScriptFileObjectAvailable() {
        HostObjectService hostObjectDetails = EmailServiceComponent.getHostObjectService();
        if (hostObjectDetails != null) {
            if (hostObjectDetails.getHostObjectClasses().contains(MashupConstants.FILE_HOST_OBJECT_NAME)) {
                return true;
            }
        }
        return false;
    }
}
