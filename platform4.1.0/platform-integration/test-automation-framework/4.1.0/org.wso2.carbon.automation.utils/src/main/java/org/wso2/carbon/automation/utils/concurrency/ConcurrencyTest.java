/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.carbon.automation.utils.concurrency;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.utils.axis2client.AxisServiceClient;
import org.wso2.carbon.automation.utils.concurrency.exception.ConcurrencyTestFailedError;
import org.wso2.carbon.automation.utils.concurrency.exception.ExceptionHandler;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

public class ConcurrencyTest {
    private static final Log log = LogFactory.getLog(ConcurrencyTest.class);

    private int concurrencyNumber;
    private int numberOfIterations;
    private Queue<OMElement> messageQueue;

    public ConcurrencyTest(int threadGroup, int loopCount) {
        concurrencyNumber = threadGroup;
        numberOfIterations = loopCount;
        messageQueue = new LinkedList<OMElement>();
    }

    public Queue<OMElement> getMessages() {
        return messageQueue;
    }

    public void clearQueue() {
        messageQueue.clear();
    }

    public void run(final String serviceEndPoint, final OMElement payload,
                    final String operation)
            throws ConcurrencyTestFailedError, InterruptedException {
        log.info("Starting Concurrency test with " + concurrencyNumber + " Threads and " + numberOfIterations
                 + " loop count");
        clearQueue();
        final ExceptionHandler handler = new ExceptionHandler();
        Thread[] clientThread = new Thread[concurrencyNumber];
        final AxisServiceClient serviceClient = new AxisServiceClient();
        for (int i = 0; i < concurrencyNumber; i++) {
            clientThread[i] = new Thread(new Runnable() {
                public void run() {
                    for (int j = 0; j < numberOfIterations; j++) {
                        try {
                            messageQueue.add(serviceClient.sendReceive(payload, serviceEndPoint, operation));
                        } catch (AxisFault axisFault) {
                            handler.setException(axisFault);
                        }
                    }
                }
            });
            clientThread[i].setUncaughtExceptionHandler(handler);

        }

        for (int i = 0; i < concurrencyNumber; i++) {
            clientThread[i].start();
        }

        for (int i = 0; i < concurrencyNumber; i++) {
            try {
                clientThread[i].join();
            } catch (InterruptedException e) {
                throw new InterruptedException("Exception Occurred while joining Thread");
            }
        }
        int aliveCount = 0;
        Calendar startTime = Calendar.getInstance();
        while (aliveCount < concurrencyNumber) {
            if ((Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()) < 120000) {
                break;
            }
            if (clientThread[aliveCount].isAlive()) {
                aliveCount = 0;
                continue;
            }
            aliveCount++;
        }
        if (!handler.isTestPass()) {
            throw new ConcurrencyTestFailedError(handler.getFailCount() + " service invocation/s failed out of "
                                                 + concurrencyNumber * numberOfIterations + " service invocations.\n"
                                                 + "Concurrency Test Failed for Thread Group=" + concurrencyNumber
                                                 + " and loop count=" + numberOfIterations, handler.getException());
        }

    }
}
