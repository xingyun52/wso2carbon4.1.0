/*
 * Copyright 2011-2012 WSO2, Inc. (http://wso2.com)
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


package org.wso2.appserver.sample.genericjavabean.servlet;


import org.apache.naming.SelectorContext;
import org.wso2.appserver.sample.genericjavabean.bean.MyBean;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;


public class MyServlet extends HttpServlet {
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        Context initCtx = null;
        try {

            initCtx = new InitialContext();
            SelectorContext selectorContext = new SelectorContext(
                    (Hashtable<String, Object>) initCtx.getEnvironment(), false);
            Context initialContext = (Context) selectorContext.lookup("java:comp/env");


            MyBean bean = null;

            bean = (MyBean) initialContext.lookup("bean/MyBeanFactory");

            PrintWriter writer = null;

            writer = response.getWriter();

            writer.println("foo = " + bean.getFoo() + ", bar = " +
                           bean.getBar());
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
