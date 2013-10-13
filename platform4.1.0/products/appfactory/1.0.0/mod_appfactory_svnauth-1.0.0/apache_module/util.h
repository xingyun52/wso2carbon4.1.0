
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
/* From https://github.com/bpaquet/crowd-apache-connector*/
#include <apr.h>
#include <apr_tables.h>
#include <httpd.h>

#ifndef APR_ARRAY_IDX
#define APR_ARRAY_IDX(ary,i,type) (((type *)(ary)->elts)[i])
#endif

#ifndef APR_ARRAY_PUSH
#define APR_ARRAY_PUSH(ary,type) (*((type *)apr_array_push(ary)))
#endif

#ifndef APR_INT64_MAX
#ifdef INT64_MAX
#define APR_INT64_MAX   INT64_MAX
#else
#define APR_INT64_MAX   APR_INT64_C(0x7fffffffffffffff)
#endif
#endif

void *log_ralloc(const request_rec *r, void *alloc);

void *log_palloc(apr_pool_t *pool, void *alloc);
