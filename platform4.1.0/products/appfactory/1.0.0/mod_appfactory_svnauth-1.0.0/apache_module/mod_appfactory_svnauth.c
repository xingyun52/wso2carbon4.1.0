
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
#include <apr_md5.h>
#include <apr_base64.h>
#include "cache.h"
#include "mod_auth.h"
#include <httpd.h>
#include <http_config.h>
#include <http_log.h>
#include <http_protocol.h>
#include <ap_config.h>
#include <apr_strings.h>
#include <axutil_error_default.h>
#include <axutil_log_default.h>
#include <axutil_thread_pool.h>
#include <axiom_xml_reader.h>
#include <axutil_version.h>
#include <apr_rmm.h>
#include <apr_shm.h>
#include <axis2_http_transport.h>
#include <appfactory_svnauth_client.h>
#if APR_HAVE_UNISTD_H
#include <unistd.h>
#endif
axutil_log_t *axutil_logger = NULL;
cache_t *auth_cache=NULL;

/* Configuration structure populated by apache2.conf */
typedef struct axis2_config_rec
{
    char *axutil_log_file;
    char *axis2_repo_path;
    axutil_log_levels_t log_level;
    int max_log_file_size;
	int axis2_global_pool_size;
    char *appfactory_svnl_auth_epr;
    int cache;
    char *cache_max_age;
    char *cache_max_entry;

}
axis2_config_rec_t;


/******************************Function Headers********************************/
static void *appfactory_create_config(
    apr_pool_t * p,
    server_rec * s);

static const char *axis2_set_repo_path(
    cmd_parms * cmd,
    void *dummy,
    const char *arg);

static const char *axis2_set_log_file(
    cmd_parms * cmd,
    void *dummy,
    const char *arg);

static const char *
axis2_set_max_log_file_size(
    cmd_parms * cmd,
    void *dummy,
    const char *arg);
static const char *axis2_set_log_level(
    cmd_parms * cmd,
    void *dummy,
    const char *arg);
static const char *axis2_set_appfactory_svnl_auth_epr(
    cmd_parms * cmd,
    void *dummy,
    const char *arg);
static const char *set_credential_cache(
        cmd_parms * cmd,
        void *dummy,
        int on);
static const char *set_cache_max_age(
    cmd_parms * cmd,
    void *dummy,
    const char *arg);
static const char * set_cache_max_entry(
     cmd_parms * cmd,
     void *dummy,
     const char *arg);

static int axis2_handler(
    request_rec * req);

/* Shutdown Axis2 */

void *AXIS2_CALL appfactory_svnauth_module_malloc(
    axutil_allocator_t * allocator,
    size_t size);

void *AXIS2_CALL appfactory_svnauth_module_realloc(
    axutil_allocator_t * allocator,
    void *ptr,
    size_t size);

void AXIS2_CALL appfactory_svnauth_module_free(
    axutil_allocator_t * allocator,
    void *ptr);

static void appfactory_svnauth_module_init(
    apr_pool_t * p,
    server_rec * svr_rec);

static void appfactory_register_hooks(
    apr_pool_t * p);

/***************************End of Function Headers****************************/

static const command_rec appfactory_cmds[] = {
    AP_INIT_TAKE1("Axis2RepoPath", axis2_set_repo_path, NULL, RSRC_CONF,
                  "Axis2/C repository path"),
    AP_INIT_TAKE1("Axis2LogFile", axis2_set_log_file, NULL, RSRC_CONF,
                  "Axis2/C log file name"),
    AP_INIT_TAKE1("Axis2LogLevel", axis2_set_log_level, NULL, RSRC_CONF,
                  "Axis2/C log level"),
    AP_INIT_TAKE1("Axis2MaxLogFileSize", axis2_set_max_log_file_size, NULL, RSRC_CONF,
                  "Axis2/C maximum log file size"),
    AP_INIT_TAKE1("AppfactorySVNAuthEPR", axis2_set_appfactory_svnl_auth_epr, NULL,
                  RSRC_CONF,
                  "Axis2/C service URL prifix"),
   AP_INIT_FLAG("CredentialCache", set_credential_cache, NULL,
                                    RSRC_CONF,
                                    "Enable credential caching"),
    AP_INIT_TAKE1("CacheMaxAge", set_cache_max_age, NULL,
                                                      RSRC_CONF,
                                                      "Max duration of cache"),
    AP_INIT_TAKE1("CacheMaxEntries", set_cache_max_entry, NULL,
                                                       RSRC_CONF,
                                                       "Max number of entries"),
    {NULL}
};

/* Dispatch list for API hooks */
module AP_MODULE_DECLARE_DATA appfactory_svnauth_module = {
    STANDARD20_MODULE_STUFF,
    NULL,                       /* create per-dir    config structures */
    NULL,                       /* merge  per-dir    config structures */
    appfactory_create_config,           /* create per-server config structures */
    NULL,                       /* merge  per-server config structures */
    appfactory_cmds,                 /* table of config file commands       */
    appfactory_register_hooks        /* register hooks                      */
};

static void *
appfactory_create_config(
    apr_pool_t * p,
    server_rec * s)
{
    axis2_config_rec_t *conf = apr_palloc(p, sizeof(*conf));
    conf->axutil_log_file = NULL;
    conf->axis2_repo_path = NULL;
    conf->log_level = AXIS2_LOG_LEVEL_DEBUG;
    conf->axis2_global_pool_size = 0;
	conf->max_log_file_size = 1;
    
    return conf;
}

static const char *
axis2_set_repo_path(
    cmd_parms * cmd,
    void *dummy,
    const char *arg)
{
    axis2_config_rec_t *conf = NULL;
    conf =
        (axis2_config_rec_t *) ap_get_module_config(cmd->server->module_config,
                                                    &appfactory_svnauth_module);
    conf->axis2_repo_path = apr_pstrdup(cmd->pool, arg);
    return NULL;
}

static const char *
axis2_set_log_file(
    cmd_parms * cmd,
    void *dummy,
    const char *arg)
{
    axis2_config_rec_t *conf = NULL;
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL)
    {
        return err;
    }

    conf =
        (axis2_config_rec_t *) ap_get_module_config(cmd->server->module_config,
                                                    &appfactory_svnauth_module);
    conf->axutil_log_file = apr_pstrdup(cmd->pool, arg);
    return NULL;
}

static const char *
axis2_set_max_log_file_size(
    cmd_parms * cmd,
    void *dummy,
    const char *arg)
{
    axis2_config_rec_t *conf = NULL;
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL)
    {
        return err;
    }

    conf =
        (axis2_config_rec_t *) ap_get_module_config(cmd->server->module_config,
                                                    &appfactory_svnauth_module);
    conf->max_log_file_size = 1024 * 1024 * atoi(arg);
    return NULL;
}

static const char *
axis2_set_global_pool_size(
    cmd_parms * cmd,
    void *dummy,
    const char *arg)
{
    axis2_config_rec_t *conf = NULL;
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL)
    {
        return err;
    }

    conf =
        (axis2_config_rec_t *) ap_get_module_config(cmd->server->module_config,
                                                    &appfactory_svnauth_module);
    conf->axis2_global_pool_size = 1024 * 1024 * atoi(arg);
    return NULL;
}

static const char *
axis2_set_log_level(
    cmd_parms * cmd,
    void *dummy,
    const char *arg)
{
    char *str;
    axutil_log_levels_t level = AXIS2_LOG_LEVEL_DEBUG;
    axis2_config_rec_t *conf = NULL;
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);
    if (err != NULL)
    {
        return err;
    }

    conf =
        (axis2_config_rec_t *) ap_get_module_config(cmd->server->module_config,
                                                    &appfactory_svnauth_module);

    str = ap_getword_conf(cmd->pool, &arg);
    if (str)
    {
        if (!strcasecmp(str, "crit"))
        {
            level = AXIS2_LOG_LEVEL_CRITICAL;
        }
        else if (!strcasecmp(str, "error"))
        {
            level = AXIS2_LOG_LEVEL_ERROR;
        }
        else if (!strcasecmp(str, "warn"))
        {
            level = AXIS2_LOG_LEVEL_WARNING;
        }
        else if (!strcasecmp(str, "info"))
        {
            level = AXIS2_LOG_LEVEL_INFO;
        }
        else if (!strcasecmp(str, "debug"))
        {
            level = AXIS2_LOG_LEVEL_DEBUG;
        }
        else if (!strcasecmp(str, "user"))
        {
            level = AXIS2_LOG_LEVEL_USER;
        }
        else if (!strcasecmp(str, "trace"))
        {
            level = AXIS2_LOG_LEVEL_TRACE;
        }
    }
    conf->log_level = level;
    return NULL;
}

static const char *
axis2_set_svc_url_prefix(
    cmd_parms * cmd,
    void *dummy,
    const char *arg)
{
    AXIS2_IMPORT extern axis2_char_t *axis2_request_url_prefix;
    const char *err = ap_check_cmd_context(cmd, GLOBAL_ONLY);

    axis2_request_url_prefix = AXIS2_REQUEST_URL_PREFIX;
    if (!err)
    {
        axis2_char_t *prefix = apr_pstrdup(cmd->pool, arg);
        if (prefix)
            axis2_request_url_prefix = prefix;
    }

    return NULL;
}
static const char *axis2_set_appfactory_svnl_auth_epr(
    cmd_parms * cmd,
    void *dummy,
    const char *arg){
 axis2_config_rec_t *conf = NULL;
   conf =
        (axis2_config_rec_t *) ap_get_module_config(cmd->server->module_config,
                                                    &appfactory_svnauth_module);
 
 conf->appfactory_svnl_auth_epr = ap_getword_conf(cmd->pool, &arg);

return NULL;

}

static const char *set_credential_cache(
    cmd_parms * cmd,
    void *dummy,
    int on){
 axis2_config_rec_t *conf = NULL;
   conf =
        (axis2_config_rec_t *) ap_get_module_config(cmd->server->module_config,
                                                    &appfactory_svnauth_module);

 conf->cache = on;

return NULL;
}


static const char *set_cache_max_age(
    cmd_parms * cmd,
    void *dummy,
    const char *arg){
 axis2_config_rec_t *conf = NULL;
   conf =
        (axis2_config_rec_t *) ap_get_module_config(cmd->server->module_config,
                                                    &appfactory_svnauth_module);

 conf->cache_max_age = atoi(arg)*60*1000*1000;

return NULL;
 }

static const char * set_cache_max_entry(
    cmd_parms * cmd,
    void *dummy,
    const char *arg){
 axis2_config_rec_t *conf = NULL;
   conf =
        (axis2_config_rec_t *) ap_get_module_config(cmd->server->module_config,
                                                    &appfactory_svnauth_module);

 conf->cache_max_entry = atoi(arg);

return NULL;

  }

void *AXIS2_CALL
appfactory_svnauth_module_malloc(
    axutil_allocator_t * allocator,
    size_t size)
{
	return apr_palloc((apr_pool_t *) (allocator->current_pool), size);
}

void *AXIS2_CALL
appfactory_svnauth_module_realloc(
    axutil_allocator_t * allocator,
    void *ptr,
    size_t size)
{
	return NULL;
}

void AXIS2_CALL
appfactory_svnauth_module_free(
    axutil_allocator_t * allocator,
    void *ptr)
{
 return NULL;

}
static void *copy_string(void *data, apr_pool_t *p){
    return  apr_pstrdup(p, data);
}
appfactory_svnauth_module_init(
    apr_pool_t * p,
    server_rec * svr_rec)
{
    apr_pool_t *pool;
    apr_status_t status;
    axutil_allocator_t *allocator = NULL;
    axutil_error_t *error = NULL;
    axis2_config_rec_t *conf = (axis2_config_rec_t*)ap_get_module_config(
                svr_rec->module_config, &appfactory_svnauth_module);



    /* We need to init xml readers before we go into threaded env
     */
    axiom_xml_reader_init();

    /* create an allocator that uses APR memory pools and lasts the
     * lifetime of the httpd server child process
     */
    status = apr_pool_create(&pool, p);
    if (status)
    {
        ap_log_error(APLOG_MARK, APLOG_EMERG, status, svr_rec,
                     "[Axis2] Error allocating mod_axis2 memory pool");
        exit(APEXIT_CHILDFATAL);
    }
    allocator = (axutil_allocator_t*) apr_palloc(pool,
                                                sizeof(axutil_allocator_t));
    if (! allocator)
    {
        ap_log_error(APLOG_MARK, APLOG_EMERG, APR_ENOMEM, svr_rec,
                     "[Axis2] Error allocating mod_axis2 allocator");
        exit(APEXIT_CHILDFATAL);
    }
    allocator->malloc_fn = appfactory_svnauth_module_malloc;
    allocator->realloc = appfactory_svnauth_module_realloc;
    allocator->free_fn = appfactory_svnauth_module_free;
    allocator->local_pool = (void*) pool;
    allocator->current_pool = (void*) pool;
    allocator->global_pool = (void*) pool;

    if (! allocator)
    {
        ap_log_error(APLOG_MARK, APLOG_EMERG, APR_EGENERAL, svr_rec,
                         "[Axis2] Error initializing mod_axis2 allocator");
        exit(APEXIT_CHILDFATAL);
    }
    
    axutil_logger = axutil_log_create(allocator, NULL, conf->axutil_log_file);
    if (! axutil_logger)
    {
        ap_log_error(APLOG_MARK, APLOG_EMERG, APR_EGENERAL, svr_rec,
                     "[Axis2] Error creating mod_axis2 log structure");
        exit(APEXIT_CHILDFATAL);
    }
    if (axutil_logger)
    {

        axutil_logger->level = conf->log_level;
        AXIS2_LOG_INFO(axutil_logger, "Apache Axis2/C version in use : %s", 
            axis2_version_string());
        AXIS2_LOG_INFO(axutil_logger, "Starting log with log level %d",
            conf->log_level);
    }
//expire time in micro seconds
if(conf->cache==1){
 auth_cache = cache_create("auth", pool, conf->cache_max_age, conf->cache_max_entry, copy_string, free);
 }
}
static authn_status authn_appfactory_check_password(request_rec *req, const char *user, const char *password)
{
    int status=AUTH_DENIED;
    char *uri=req->uri;
    char *key;
    char digest[128];
    char bsalt[64];
    char salt[128];
    axutil_env_t *thread_env = NULL;
    axutil_allocator_t *allocator = NULL;
    axutil_error_t *error = NULL;
    apr_allocator_t *local_allocator = NULL;
    apr_pool_t *local_pool = NULL;
        axis2_config_rec_t  *conf=  (axis2_config_rec_t *)ap_get_module_config(req->server->module_config, &appfactory_svnauth_module);
        apr_allocator_create(&local_allocator);
        apr_pool_create_ex(&local_pool, NULL, NULL, local_allocator);
        allocator = (axutil_allocator_t *) apr_palloc(local_pool,
                                                  sizeof(axutil_allocator_t));
        allocator->malloc_fn = appfactory_svnauth_module_malloc;
        allocator->realloc = appfactory_svnauth_module_realloc;
        allocator->free_fn = appfactory_svnauth_module_free;
        allocator->local_pool = (void *)local_pool;
        allocator->current_pool = (void *)local_pool;
        error = axutil_error_create(allocator);
        thread_env = axutil_env_create_with_error_log(allocator,
                                                            error,axutil_logger);
        thread_env->allocator = allocator;
        ap_getword(req->pool, &uri, '/');
        ap_getword(req->pool, &uri, '/');
        char *appId=ap_getword(req->pool, &uri, '/');


        key=apr_psprintf(req->pool, "%s::%s", user,appId);
        if(auth_cache!=NULL){
            char *cached_password=cache_get(auth_cache,key,req);
            if(cached_password!=NULL && apr_password_validate(password,cached_password)==APR_SUCCESS){
                ap_log_rerror(APLOG_MARK, APLOG_DEBUG, 0, req, "cache hit....");
                status=AUTH_GRANTED;
                }
        }
        if(status==AUTH_DENIED){
            if(hasAccess(
	                        user,
	                        password,
	                        appId,thread_env,
	                        conf->axis2_repo_path,
	                        conf->appfactory_svnl_auth_epr)==1){
                ap_log_rerror(APLOG_MARK, APLOG_DEBUG, 0, req, "Requested authentication externally for %s to %s",user,appId);
                status=AUTH_GRANTED;
                    if (auth_cache != NULL && key != NULL) {
                        if (password != NULL) {
                            ap_log_rerror(APLOG_MARK, APLOG_DEBUG, 0, req, "cache miss....");
                            apr_generate_random_bytes(bsalt, sizeof(bsalt));
                            apr_base64_encode(salt, bsalt, sizeof(bsalt));
                            apr_md5_encode(password, salt, digest, sizeof(digest));
                            cache_put(auth_cache, key, strdup(digest), req);
                            }
                     }
            }
        }
        return status;
}
static const authn_provider authn_appfactory_provider =
{
    &authn_appfactory_check_password,    /* Callback for HTTP Basic authentication */
    NULL                            /* Callback for HTTP Digest authentication */
};
static void
appfactory_register_hooks(
    apr_pool_t * p)
{
    ap_hook_child_init(appfactory_svnauth_module_init, NULL, NULL, APR_HOOK_MIDDLE);
   ap_register_provider(
        p,
        AUTHN_PROVIDER_GROUP,
        "appfactory",
        "0",                    /* Version of callback interface, not the version of the implementation. */
        &authn_appfactory_provider
    );
}


