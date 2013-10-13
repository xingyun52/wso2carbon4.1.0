
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "appfactory_svnauth_stub.h"
axiom_node_t *build_om_programatically(
    const axutil_env_t * env,
    const axis2_char_t * operation,
    const axis2_char_t * param1,
    const axis2_char_t * param2,
    const axis2_char_t * param3);
int hasAccess(char *username,char *password,char *appid,const axutil_env_t *env,char *client_home,char *address)
{
    axis2_stub_t *stub = NULL;
    axiom_node_t *node = NULL;
    int status =0;
   /* const axutil_env_t *env = NULL;*/
    
    axiom_node_t *ret_node = NULL;

    const axis2_char_t *operation = "hasAccess";
    const axis2_char_t *param1 = username;
    const axis2_char_t *param2 = password;
    const axis2_char_t *param3 = appid;

    /*env = axutil_env_create_all("math_blocking.log", AXIS2_LOG_LEVEL_CRITICAL);
    axutil_allocator_t *allocator = NULL;
allocator = axutil_allocator_init(NULL);
env = axutil_env_create(allocator);*/
    /*client_home = AXIS2_GETENV("AXIS2C_HOME");
    if (!client_home || !strcmp(client_home, ""))*/

    

    printf("Using endpoint : %s\n", address);
    printf("\nInvoking operation %s with params %s and %s\n", operation, param1,
           param2);

    node = build_om_programatically(env, operation, param1, param2,param3);
    stub =
        appfactory_svnauth_stub_create_with_endpoint_uri_and_client_home(env, address,
                                                                 client_home);

    /* create node and invoke math */
    if (stub)
    {
        ret_node = appfactory_svnauth_stub_call_hasAccess(stub, env, node,username,password);
    }

    if (ret_node)
    {
        if (axiom_node_get_node_type(ret_node, env) == AXIOM_ELEMENT)
        {
            axis2_char_t *result = NULL; 
            axiom_node_t *ret_node1 = NULL;   
            axiom_element_t *result_ele =
                (axiom_element_t *) axiom_node_get_data_element(ret_node, env);

            result = axiom_element_get_text(result_ele, env, ret_node);
            ret_node1 = axiom_node_get_first_element(ret_node, env);
             result_ele =
                (axiom_element_t *) axiom_node_get_data_element(ret_node1, env);
            result = axiom_element_get_text(result_ele, env, ret_node1);
if (axutil_strcmp(result, "true") == 0){
status=1;
}
        }
  /*      else
        {
            axiom_xml_writer_t *writer = NULL;
            axiom_output_t *om_output = NULL;
            axis2_char_t *buffer = NULL;
            writer =
                axiom_xml_writer_create_for_memory(env, NULL, AXIS2_TRUE, 0,
                                                   AXIS2_XML_PARSER_TYPE_BUFFER);
            om_output = axiom_output_create(env, writer);

            axiom_node_serialize(ret_node, env, om_output);
            buffer = (axis2_char_t *) axiom_xml_writer_get_xml(writer, env);
            printf("\nReceived invalid OM as result : %s\n", buffer);
            if (buffer)
            {
                AXIS2_FREE(env->allocator, buffer);
                buffer = NULL;
            }
            if (om_output)
            {
                axiom_output_free(om_output, env);
                om_output = NULL;
            }
            axiom_xml_writer_free(writer, env);
        }*/
    }
  /*  else
    {
        AXIS2_LOG_ERROR(env->log, AXIS2_LOG_SI,
                        "Stub invoke FAILED: Error code:" " %d :: %s",
                        env->error->error_number,
                        AXIS2_ERROR_GET_MESSAGE(env->error));
        printf("math stub invoke FAILED!\n");
    }*/

    if (stub)
    {
        axis2_stub_free(stub, env);
    }

    

    return status;
}

axiom_node_t *
build_om_programatically(
    const axutil_env_t * env,
    const axis2_char_t * operation,
    const axis2_char_t * username,
    const axis2_char_t * password,
    const axis2_char_t * appId)
{
    axiom_node_t *math_om_node = NULL;
    axiom_element_t *math_om_ele = NULL;
    axiom_node_t *text_om_node = NULL;
    axiom_element_t *text_om_ele = NULL;
    axiom_namespace_t *ns1 = NULL;

    axiom_xml_writer_t *xml_writer = NULL;
    axiom_output_t *om_output = NULL;
    axis2_char_t *buffer = NULL;

    ns1 =
        axiom_namespace_create(env, "http://service.mgt.repository.appfactory.carbon.wso2.org",
                               "ns1");

    math_om_ele =
        axiom_element_create(env, NULL, operation, ns1, &math_om_node);

    text_om_ele =
        axiom_element_create(env, math_om_node, "username", NULL, &text_om_node);
    axiom_element_set_text(text_om_ele, env, username, text_om_node);

    
    text_om_ele =
        axiom_element_create(env, math_om_node, "applicationId", NULL, &text_om_node);
    axiom_element_set_text(text_om_ele, env, appId, text_om_node);

    xml_writer =
        axiom_xml_writer_create_for_memory(env, NULL, AXIS2_FALSE, AXIS2_FALSE,
                                           AXIS2_XML_PARSER_TYPE_BUFFER);
    om_output = axiom_output_create(env, xml_writer);

    axiom_node_serialize(math_om_node, env, om_output);
    buffer = (axis2_char_t *) axiom_xml_writer_get_xml(xml_writer, env);
    AXIS2_LOG_DEBUG(env->log, AXIS2_LOG_SI, "\nSending OM node in XML : %s \n",
                    buffer);
    printf("\nSending OM node in XML : %s \n", buffer);
    if (om_output)
    {
        axiom_output_free(om_output, env);
        om_output = NULL;
    }

    return math_om_node;
}
