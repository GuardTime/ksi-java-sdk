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
package com.guardtime.ksi.service.client.http;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.pdu.ExtenderConfiguration;
import com.guardtime.ksi.pdu.ExtensionRequest;
import com.guardtime.ksi.pdu.ExtensionResponseFuture;
import com.guardtime.ksi.pdu.KSIRequestContext;
import com.guardtime.ksi.pdu.PduFactory;
import com.guardtime.ksi.pdu.PduFactoryProvider;
import com.guardtime.ksi.pdu.PduVersion;
import com.guardtime.ksi.service.Future;
import com.guardtime.ksi.service.client.ConfigurationAwareClient;
import com.guardtime.ksi.service.client.ConfigurationAwareSigningClient;
import com.guardtime.ksi.service.client.KSIClientException;
import com.guardtime.ksi.service.client.KSIExtenderClient;
import com.guardtime.ksi.service.client.KSIPublicationsFileClient;
import com.guardtime.ksi.service.client.KSISigningClient;
import com.guardtime.ksi.service.client.ServiceCredentials;
import com.guardtime.ksi.tlv.TLVElement;
import com.guardtime.ksi.util.Util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * Common class for all KSI HTTP clients
 */
public abstract class AbstractHttpClient extends ConfigurationAwareSigningClient implements ConfigurationAwareClient, KSISigningClient, KSIExtenderClient, KSIPublicationsFileClient {

    public static final String HEADER_APPLICATION_KSI_REQUEST = "application/ksi-request";
    public static final String HEADER_NAME_CONTENT_TYPE = "Content-Type";

    protected AbstractHttpClientSettings settings;
    protected final PduFactory pduFactory;

    public AbstractHttpClient(AbstractHttpClientSettings settings) {
        super(PduFactoryProvider.get(exceptionIfNull(settings).getPduVersion()));
        this.pduFactory = PduFactoryProvider.get(settings.getPduVersion());
        this.settings = settings;
    }

    private static AbstractHttpClientSettings exceptionIfNull(AbstractHttpClientSettings settings) {
        Util.notNull(settings, "HttpClient.settings");
        return settings;
    }

    public ExtensionResponseFuture extend(KSIRequestContext requestContext, Date aggregationTime, Date publicationTime) throws KSIException {
        Util.notNull(requestContext, "requestContext");
        Util.notNull(aggregationTime, "aggregationTime");
        ServiceCredentials credentials = getServiceCredentials();
        ExtensionRequest requestMessage = pduFactory.createExtensionRequest(requestContext, credentials, aggregationTime, publicationTime);
        ByteArrayInputStream requestStream = new ByteArrayInputStream(requestMessage.toByteArray());
        HttpPostRequestFuture postRequestFuture = post(requestStream, settings.getExtendingUrl());
        return new ExtensionResponseFuture(postRequestFuture, requestContext, credentials, pduFactory);
    }

    public ExtenderConfiguration getExtenderConfiguration(KSIRequestContext requestContext) throws KSIException {
        Util.notNull(requestContext, "requestContext");
        ServiceCredentials credentials = getServiceCredentials();
        ExtensionRequest request = pduFactory.createExtensionConfigurationRequest(requestContext, credentials);
        Future<TLVElement> future = extend(new ByteArrayInputStream(request.toByteArray()));
        return pduFactory.readExtenderConfigurationResponse(requestContext, credentials, future.getResult());
    }

    protected abstract Future<TLVElement> extend(InputStream request) throws KSIClientException;

    protected Future<TLVElement> sign(InputStream inputStream) throws KSIClientException {
        return post(inputStream, settings.getSigningUrl());
    }

    protected abstract HttpPostRequestFuture post(InputStream inputStream, URL url) throws KSIClientException;

    public ServiceCredentials getServiceCredentials() {
        return settings.getCredentials();
    }

    public PduVersion getPduVersion() {
        return settings.getPduVersion();
    }

}
