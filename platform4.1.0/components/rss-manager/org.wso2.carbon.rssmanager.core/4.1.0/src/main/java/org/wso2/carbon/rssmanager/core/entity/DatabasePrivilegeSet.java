/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.rssmanager.core.entity;

public class DatabasePrivilegeSet {

    private String selectPriv = "N";

    private String insertPriv = "N";

    private String updatePriv = "N";

    private String deletePriv = "N";

    private String createPriv = "N";

    private String dropPriv = "N";

    private String grantPriv = "N";

    private String referencesPriv = "N";

    private String indexPriv = "N";

    private String alterPriv = "N";

    private String createTmpTablePriv = "N";

    private String lockTablesPriv = "N";

    private String executePriv = "N";

    private String createViewPriv = "N";

    private String showViewPriv = "N";

    private String createRoutinePriv = "N";

    private String alterRoutinePriv = "N";

    private String triggerPriv = "N";

    private String eventPriv = "N";
    
    public DatabasePrivilegeSet() {}

    public String getSelectPriv() {
        return selectPriv;
    }

    public String getInsertPriv() {
        return insertPriv;
    }

    public String getReferencesPriv() {
        return referencesPriv;
    }

    public String getShowViewPriv() {
        return showViewPriv;
    }

    public String getDropPriv() {
        return dropPriv;
    }

    public String getTriggerPriv() {
        return triggerPriv;
    }

    public String getEventPriv() {
        return eventPriv;
    }

    public String getUpdatePriv() {
        return updatePriv;
    }

    public String getCreatePriv() {
        return createPriv;
    }

    public String getDeletePriv() {
        return deletePriv;
    }

    public String getIndexPriv() {
        return indexPriv;
    }

    public String getGrantPriv() {
        return grantPriv;
    }

    public String getAlterPriv() {
        return alterPriv;
    }

    public String getExecutePriv() {
        return executePriv;
    }

    public String getCreateTmpTablePriv() {
        return createTmpTablePriv;
    }

    public String getCreateViewPriv() {
        return createViewPriv;
    }

    public String getCreateRoutinePriv() {
        return createRoutinePriv;
    }

    
    public String getAlterRoutinePriv() {
        return alterRoutinePriv;
    }

    public String getLockTablesPriv() {
        return lockTablesPriv;
    }

    public void setSelectPriv(String selectPriv) {
        this.selectPriv = selectPriv;
    }

    public void setInsertPriv(String insertPriv) {
        this.insertPriv = insertPriv;
    }

    public void setUpdatePriv(String updatePriv) {
        this.updatePriv = updatePriv;
    }

    public void setDeletePriv(String deletePriv) {
        this.deletePriv = deletePriv;
    }

    public void setCreatePriv(String createPriv) {
        this.createPriv = createPriv;
    }

    public void setDropPriv(String dropPriv) {
        this.dropPriv = dropPriv;
    }

    public void setGrantPriv(String grantPriv) {
        this.grantPriv = grantPriv;
    }

    public void setReferencesPriv(String referencesPriv) {
        this.referencesPriv = referencesPriv;
    }

    public void setIndexPriv(String indexPriv) {
        this.indexPriv = indexPriv;
    }

    public void setAlterPriv(String alterPriv) {
        this.alterPriv = alterPriv;
    }

    public void setCreateTmpTablePriv(String createTmpTablePriv) {
        this.createTmpTablePriv = createTmpTablePriv;
    }

    public void setLockTablesPriv(String lockTablesPriv) {
        this.lockTablesPriv = lockTablesPriv;
    }

    public void setExecutePriv(String executePriv) {
        this.executePriv = executePriv;
    }

    public void setCreateViewPriv(String createViewPriv) {
        this.createViewPriv = createViewPriv;
    }

    public void setShowViewPriv(String showViewPriv) {
        this.showViewPriv = showViewPriv;
    }

    public void setCreateRoutinePriv(String createRoutinePriv) {
        this.createRoutinePriv = createRoutinePriv;
    }

    public void setAlterRoutinePriv(String alterRoutinePriv) {
        this.alterRoutinePriv = alterRoutinePriv;
    }

    public void setTriggerPriv(String triggerPriv) {
        this.triggerPriv = triggerPriv;
    }

    public void setEventPriv(String eventPriv) {
        this.eventPriv = eventPriv;
    }

}
