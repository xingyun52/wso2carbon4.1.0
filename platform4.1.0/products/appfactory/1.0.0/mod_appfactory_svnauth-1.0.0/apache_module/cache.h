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
/*From https://github.com/bpaquet/crowd-apache-connector*/
#include <apr_hash.h>
#include <apr_time.h>
#include <httpd.h>
#include <http_log.h>

typedef struct cache_entry_struct cache_entry_t;

struct cache_entry_struct {
    char *key;
    void *value;
    apr_time_t expiry;
    cache_entry_t *younger;
    cache_entry_t *older;
};

typedef struct {
    const char *name;
    apr_thread_mutex_t *mutex;
    apr_hash_t *table;
    cache_entry_t *oldest;
    cache_entry_t *youngest;
    apr_time_t max_age;
    unsigned int max_entries;
    void *(*copy_data)(void *data, apr_pool_t *p);
    void (*free_data)(void *data);
} cache_t;

cache_t *cache_create(const char *name, apr_pool_t *pool, apr_time_t max_age, unsigned int max_entries,
    void *(*copy_data)(void *data, apr_pool_t *p), void (*free_data)(void *data));

void *cache_get(cache_t *cache, const char *key, const request_rec *r);

void cache_put(cache_t *cache, const char *key, void *value, const request_rec *r);
