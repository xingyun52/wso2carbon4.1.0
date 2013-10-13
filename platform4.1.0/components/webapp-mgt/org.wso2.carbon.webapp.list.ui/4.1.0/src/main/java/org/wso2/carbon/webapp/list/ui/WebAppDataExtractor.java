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

package org.wso2.carbon.webapp.list.ui;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.namespace.QName;

public class WebAppDataExtractor {

    private Map<String, String> jaxWSMap = new HashMap<String, String>();
    private Map<String, String> jaxRSMap = new HashMap<String, String>();

    public Map<String, String> getJaxWSMap() {
        return jaxWSMap;
    }

    public void setJaxWSMap(Map<String, String> jaxWSMap) {
        this.jaxWSMap = jaxWSMap;
    }

    public void getServletXML(InputStream inputStream) throws Exception{
        jaxWSMap.clear();
        jaxRSMap.clear();
        byte[] buffer = new byte[2048];
        ZipInputStream stream = new ZipInputStream(inputStream);

        try
        {

            ZipEntry entry;
            while((entry = stream.getNextEntry())!=null)
            {
                if(!entry.getName().equals("WEB-INF/cxf-servlet.xml")){
                    continue;
                }
                int len = 0;
                String output="";
                while ((len = stream.read(buffer)) > 0)
                {
                    output+=(new String(buffer));
                    buffer = new byte[2048];
                }

                output = stripNonValidXMLCharacters(output);
                OMElement element = AXIOMUtil.stringToOM(output);

                Iterator<OMElement> iterator = element.getChildrenWithName(new QName(
                        "http://cxf.apache.org/jaxws", "endpoint"));
                while (iterator.hasNext()) {
                    OMElement temp = iterator.next();
                    jaxWSMap.put(temp.getAttribute(new QName("id"))
                            .getAttributeValue(),
                            temp.getAttribute(new QName("address"))
                                    .getAttributeValue());

                }

                iterator = element.getChildrenWithName(new QName(
                        "http://cxf.apache.org/jaxrs", "server"));
                while (iterator.hasNext()) {
                    OMElement temp = iterator.next();
                    jaxRSMap.put(temp.getAttribute(new QName("id"))
                            .getAttributeValue(),
                            temp.getAttribute(new QName("address"))
                                    .getAttributeValue());

                }

            }
        }
        finally
        {
            stream.close();
        }
    }

    private static String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in)))
            return ""; // vacancy test.
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught
            // here; it should not happen.
            if ((current == 0x9) || (current == 0xA) || (current == 0xD)
                    || ((current >= 0x20) && (current <= 0xD7FF))
                    || ((current >= 0xE000) && (current <= 0xFFFD))
                    || ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    }

    public List getWSDLs(String serverURL){
        List<String> list=new ArrayList<String>();
        Iterator<String> iterator=jaxWSMap.keySet().iterator();
        while (iterator.hasNext())  {
            list.add(serverURL+"services"+jaxWSMap.get(iterator.next())+"?wsdl");
        }
        if(list.size()==0){
            return null;
        }
        return list;
    }

    public List getWADLs(String serverURL){
        List<String> list=new ArrayList<String>();
        Iterator<String> iterator=jaxRSMap.keySet().iterator();
        while (iterator.hasNext())  {
            list.add(serverURL+"services"+jaxRSMap.get(iterator.next())+"?_wadl");
        }
        if(list.size()==0){
            return null;
        }
        return list;
    }
}
