
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

#ifndef MATH_CLIENT_H
#define MATH_CLIENT_H

/**
 * @file 
 * @brief 
 */

#include "appfactory_svnauth_stub.h"
#include <stdio.h>
#include <axiom.h>
#include <axis2_util.h>
#include <axiom_soap.h>
#include <axis2_client.h>

#ifdef __cplusplus
extern "C"
{
#endif
/*axiom_node_t *build_om_programatically(
    const axutil_env_t * env,
    const axis2_char_t * operation,
    const axis2_char_t * param1,
    const axis2_char_t * param2,
    const axis2_char_t * param3);
*/
int hasAccess(char *username,char *password,char *appid,const axutil_env_t *env,char *client_home,char *address);

#ifdef __cplusplus
}
#endif
#endif                          /*  */
