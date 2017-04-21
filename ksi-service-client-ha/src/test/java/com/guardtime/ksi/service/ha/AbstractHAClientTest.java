/*
 * Copyright 2013-2017 Guardtime, Inc.
 *
 * This file is part of the Guardtime client SDK.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES, CONDITIONS, OR OTHER LICENSES OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * "Guardtime" and "KSI" are trademarks or registered trademarks of
 * Guardtime, Inc., and no license to trademarks is granted; Guardtime
 * reserves and retains all trademark rights.
 */
package com.guardtime.ksi.service.ha;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.pdu.KSIRequestContext;
import com.guardtime.ksi.service.client.KSISigningClient;
import com.guardtime.ksi.service.ha.tasks.ServiceCallingTask;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;

public class AbstractHAClientTest {

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Invalid input parameter. It " +
            "is not " +
            "possible to have more clients in one selection than there are available clients")
    public void testActiveClientsPerRequestLargerThanClientsList() throws Exception {
        new DummyHAClient(singletonList(mock(KSISigningClient.class)), 2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Can not initialize " +
            "DummyHAClient with less than one subclient per selection")
    public void testInititalizingWithNoClientsPerRequest() throws Exception {
        new DummyHAClient(singletonList(mock(KSISigningClient.class)), 0);
    }

    @Test
    public void testIfOneServiceCallInSelectionSucceeds() throws Exception {
        for (int i = 0; i < 100; i++) {
            DummyHAClient haClient = new DummyHAClient(Collections.singletonList(mock(KSISigningClient.class)), null);
            List<ServiceCallingTask<Integer>> tasks = new ArrayList<ServiceCallingTask<Integer>>();
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 1")));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 2")));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 3")));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 4")));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 5")));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 6")));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 7")));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 8")));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 9")));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 10")));
            tasks.add(new DumbTask(777));
            tasks.add(new DummyFailingTask(new RuntimeException("Test failed. Task 11")));
            Integer result = haClient.callAnyService(tasks, 0L).getResult();
            Assert.assertEquals(new Integer(777), result);
        }
    }

    @Test
    public void testAllButOneServiceCallInSelectionIsSlow() throws Exception {
        for (int i = 0; i < 100; i++) {
            DummyHAClient haClient = new DummyHAClient(Collections.singletonList(mock(KSISigningClient.class)), null);
            List<ServiceCallingTask<Integer>> tasks = new ArrayList<ServiceCallingTask<Integer>>();
            tasks.add(new DummySlowTask(1));
            tasks.add(new DummySlowTask(2));
            tasks.add(new DummySlowTask(3));
            tasks.add(new DummySlowTask(4));
            tasks.add(new DummySlowTask(5));
            tasks.add(new DummySlowTask(6));
            tasks.add(new DummySlowTask(7));
            tasks.add(new DummySlowTask(8));
            tasks.add(new DummySlowTask(9));
            tasks.add(new DummySlowTask(10));
            tasks.add(new DumbTask(777));
            tasks.add(new DummySlowTask(11));
            Integer result = haClient.callAnyService(tasks, 0L).getResult();
            Assert.assertEquals(new Integer(777), result);
        }
    }

    @Test(expectedExceptions = HASubclientsFailedException.class)
    public void testAllServiceCallsFail() throws Exception {
        DummyHAClient haClient = new DummyHAClient(Collections.singletonList(mock(KSISigningClient.class)), null);
        List<ServiceCallingTask<Integer>> tasks = new ArrayList<ServiceCallingTask<Integer>>();
        List<String> messages = Arrays.asList("Test failed. Task 1",
                "Test failed. Task 2",
                "Test failed. Task 3",
                "Test failed. Task 4",
                "Test failed. Task 5",
                "Test failed. Task 6",
                "Test failed. Task 7",
                "Test failed. Task 8",
                "Test failed. Task 9",
                "Test failed. Task 10");
        for (String message : messages) {
            tasks.add(new DummyFailingTask(new RuntimeException(message)));
        }
        try {
            haClient.callAnyService(tasks, 0L).getResult();
        } catch (HASubclientsFailedException e) {
            Map<String, Exception> subexceptions = e.getExceptions();
            Assert.assertEquals(messages.size(), subexceptions.size(), "HASubclientsFailedException did not contain all expected subexceptions.");
            for (Exception exception : subexceptions.values()) {
                Assert.assertTrue(messages.contains(exception.getMessage()), "HASubclientsFailedException did not contain all expected subexceptions.");
            }
            throw e;
        }
    }

    private static class DummyHAClient extends AbstractHAClient<DummyClient, Integer, Object> {
        public DummyHAClient(List subclients, Integer clientsForRequest) throws KSIException {
            super(subclients, clientsForRequest);
        }

        protected boolean configurationsEqual(Object c1, Object c2) {
            return c1.equals(c2);
        }

        protected String configurationsToString(List<Object> configurations) {
            return configurations.toString();
        }

        protected Object aggregateConfigurations(List<Object> configurations) {
            return configurations.get(0);
        }
    }

    private static class DumbTask extends ServiceCallingTask<Integer> {

        protected final int x;

        DumbTask(int x) {
            super(ServiceCallingTask.createClientKey("test"), new KSIRequestContext(0L, 0L, 0L));
            this.x = x;
        }


        protected Integer completeTask() throws KSIException {
            return x;
        }
    }

    private static class DummyFailingTask extends DumbTask {

        private RuntimeException e;

        DummyFailingTask(RuntimeException e) {
            super(0);
            this.e = e;
        }

        public Integer completeTask() {
            throw e;
        }
    }

    private static class DummySlowTask extends DumbTask {

        DummySlowTask(int x) {
            super(x);
        }

        public Integer completeTask() {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return x;
        }
    }

    private static class DummyClient implements Closeable {
        public void close() throws IOException {
        }
    }
}
