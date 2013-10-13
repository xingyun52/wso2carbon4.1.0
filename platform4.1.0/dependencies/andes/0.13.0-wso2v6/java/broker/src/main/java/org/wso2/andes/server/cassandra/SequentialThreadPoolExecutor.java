/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.andes.server.cassandra;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

public class SequentialThreadPoolExecutor {

    List<ExecutorService> executorServiceList ;
    int size = -1;

    public SequentialThreadPoolExecutor(int size,String poolName) {
        this.size = size;
        executorServiceList =  new ArrayList<ExecutorService>(size);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(poolName+"-%d").build();
        for (int i =0 ; i < size ; i++) {
          executorServiceList.add(Executors.newFixedThreadPool(1,namedThreadFactory));
        }

    }

    public void submit(Runnable runnable, long subscriptionId){

       int executorId = (int) (subscriptionId % size);
       executorServiceList.get(executorId).submit(runnable);
    }

    public int getSize(){
        int workqueueSize = 0;
        for (ExecutorService executor : executorServiceList) {
            workqueueSize = workqueueSize + ((ThreadPoolExecutor) executor).getQueue().size();
        }
        return workqueueSize;
    }
}
