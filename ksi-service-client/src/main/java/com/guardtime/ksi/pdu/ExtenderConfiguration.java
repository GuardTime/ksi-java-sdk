/*
 * Copyright 2013-2016 Guardtime, Inc.
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

package com.guardtime.ksi.pdu;

import java.util.Date;
import java.util.List;

/**
 * Interface for extender configuration.
 */
public interface ExtenderConfiguration {
    /**
     * Returns the maximum number of requests the client is allowed to send within one second
     */
    Long getMaximumRequests();

    /**
     * Returns a list of parent server URI-s
     */
    List<String> getParents();

    /**
     * Returns the aggregation time of the newest calendar record the extender has.
     */
    Date getCalendarFirstTime();

    /**
     * Return the aggregation time of the oldest calendar record the extender has
     */
    Date getCalendarLastTime();

    /**
     * Returned list is empty if this configuration belongs to a client that connects directly to a single extender. Otherwise it
     * contains ExtenderConfigurations of all the subclients.
     */
    List<SubclientConfiguration<ExtenderConfiguration>> getSubConfigurations();
}
