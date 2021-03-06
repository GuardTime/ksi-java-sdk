/*
 * Copyright 2013-2018 Guardtime, Inc.
 *
 *  This file is part of the Guardtime client SDK.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES, CONDITIONS, OR OTHER LICENSES OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *  "Guardtime" and "KSI" are trademarks or registered trademarks of
 *  Guardtime, Inc., and no license to trademarks is granted; Guardtime
 *  reserves and retains all trademark rights.
 *
 */
package com.guardtime.ksi.integration;

import com.guardtime.ksi.Extender;
import com.guardtime.ksi.ExtenderBuilder;
import com.guardtime.ksi.KSI;
import com.guardtime.ksi.KSIBuilder;
import com.guardtime.ksi.PublicationsHandler;
import com.guardtime.ksi.PublicationsHandlerBuilder;
import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.hashing.DataHasher;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.pdu.AggregationRequest;
import com.guardtime.ksi.pdu.AggregationResponse;
import com.guardtime.ksi.pdu.AggregationResponseFuture;
import com.guardtime.ksi.pdu.ExtensionRequest;
import com.guardtime.ksi.pdu.ExtensionResponseFuture;
import com.guardtime.ksi.pdu.KSIRequestContext;
import com.guardtime.ksi.pdu.PduFactory;
import com.guardtime.ksi.pdu.PduVersion;
import com.guardtime.ksi.pdu.RequestContextFactory;
import com.guardtime.ksi.pdu.v2.PduV2Factory;
import com.guardtime.ksi.publication.PublicationData;
import com.guardtime.ksi.service.Future;
import com.guardtime.ksi.service.KSIExtendingService;
import com.guardtime.ksi.service.KSISigningService;
import com.guardtime.ksi.service.client.KSIExtenderClient;
import com.guardtime.ksi.service.client.KSIPublicationsFileClient;
import com.guardtime.ksi.service.client.KSIServiceCredentials;
import com.guardtime.ksi.service.client.KSISigningClient;
import com.guardtime.ksi.service.client.ServiceCredentials;
import com.guardtime.ksi.service.client.http.CredentialsAwareHttpSettings;
import com.guardtime.ksi.service.client.http.HTTPConnectionParameters;
import com.guardtime.ksi.service.client.http.HttpSettings;
import com.guardtime.ksi.service.client.http.apache.ApacheHttpExtenderClient;
import com.guardtime.ksi.service.client.http.apache.ApacheHttpPublicationsFileClient;
import com.guardtime.ksi.service.client.http.apache.ApacheHttpSigningClient;
import com.guardtime.ksi.service.ha.HAService;
import com.guardtime.ksi.service.http.simple.SimpleHttpExtenderClient;
import com.guardtime.ksi.service.http.simple.SimpleHttpPublicationsFileClient;
import com.guardtime.ksi.service.http.simple.SimpleHttpSigningClient;
import com.guardtime.ksi.service.tcp.TCPClient;
import com.guardtime.ksi.service.tcp.TCPClientSettings;
import com.guardtime.ksi.tlv.TLVElement;
import com.guardtime.ksi.tlv.TLVParserException;
import com.guardtime.ksi.trust.X509CertificateSubjectRdnSelector;
import com.guardtime.ksi.unisignature.AggregationHashChain;
import com.guardtime.ksi.unisignature.CalendarHashChain;
import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.unisignature.verifier.VerificationContextBuilder;
import com.guardtime.ksi.unisignature.verifier.VerificationResult;
import com.guardtime.ksi.unisignature.verifier.policies.InternalVerificationPolicy;
import com.guardtime.ksi.unisignature.verifier.policies.Policy;
import com.guardtime.ksi.util.Util;

import org.apache.commons.io.IOUtils;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static com.guardtime.ksi.CommonTestUtil.load;
import static com.guardtime.ksi.CommonTestUtil.loadFile;
import static com.guardtime.ksi.Resources.EXTENDED_RESPONSE_WITH_NO_CALENDAR;
import static com.guardtime.ksi.Resources.KSI_TRUSTSTORE;
import static com.guardtime.ksi.Resources.KSI_TRUSTSTORE_PASSWORD;
import static com.guardtime.ksi.Resources.PROPERTIES_INTEGRATION_TEST;
import static com.guardtime.ksi.TestUtil.calculateHash;
import static com.guardtime.ksi.tlv.GlobalTlvTypes.ELEMENT_TYPE_EXTENSION_RESPONSE_PDU_V2;
import static com.guardtime.ksi.tlv.GlobalTlvTypes.ELEMENT_TYPE_REQUEST_ID;

public abstract class AbstractCommonIntegrationTest {

    protected static final String KSI_DATA_GROUP_NAME = "ksiDataProvider";
    protected static final String TEST_GROUP_INTEGRATION = "integration";
    protected static final String DEFAULT_HASH_ALGORITHM = "DEFAULT";
    private static final int DEFAULT_TIMEOUT = 5000;
    protected static String javaKeyStorePath = null;
    private static List<Closeable> listOfCloseables = new LinkedList<>();

    protected KSI ksi;
    protected SimpleHttpSigningClient signerClient;
    protected SimpleHttpExtenderClient extenderClient;
    protected SimpleHttpExtenderClient invalidExtenderClient;
    protected SimpleHttpPublicationsFileClient publicationsFileClient;

    protected static CredentialsAwareHttpSettings signingSettings;
    protected static CredentialsAwareHttpSettings extenderSettings;
    protected static HttpSettings publicationsFileSettings;
    protected static Properties properties;

    @BeforeClass
    public void setUp() throws Exception {
        properties = loadProperties();
        javaKeyStorePath = loadJavaKeyStorePath();
        signingSettings = loadSignerSettings();
        extenderSettings = loadExtenderSettings();
        publicationsFileSettings = loadPublicationsFileSettings();
        signerClient = new SimpleHttpSigningClient(signingSettings);
        extenderClient = new SimpleHttpExtenderClient(extenderSettings);
        invalidExtenderClient = new SimpleHttpExtenderClient(signingSettings);
        publicationsFileClient = new SimpleHttpPublicationsFileClient(publicationsFileSettings);
        ksi = createKsi(extenderClient, signerClient, publicationsFileClient);
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (ksi != null) ksi.close();
        for (Closeable object : listOfCloseables) {
            object.close();
        }
    }

    public static DataHash getFileHash(String fileName, String name) throws Exception {
        return getFileHash(fileName, HashAlgorithm.getByName(name));
    }

    public static DataHash getFileHash(String fileName, HashAlgorithm algorithm) throws Exception {
        DataHasher dataHasher = new DataHasher(algorithm);
        dataHasher.addData(loadFile(fileName));
        return dataHasher.getHash();
    }

    public static DataHash getFileHash(String fileName) throws Exception {
        return getFileHash(fileName, DEFAULT_HASH_ALGORITHM);
    }

    protected static TCPClientSettings loadTCPSigningSettings() {
        Properties props = loadProperties();
        String signerIP = getProperty(props, "tcp.signerIP");
        int signerPort = Integer.parseInt(getProperty(props, "tcp.signerPort"));
        int tcpTransactionTimeoutSec = Integer.parseInt(getProperty(props, "tcp.transactionTimeoutSec"));
        ServiceCredentials serviceCredentials = new KSIServiceCredentials(
                getProperty(props, "tcp.signerLoginId", "tcp.loginId"),
                getProperty(props, "tcp.signerLoginKey", "tcp.loginKey"),
                HashAlgorithm.getByName(props.getProperty("tcp.signerHmacAlgorithm", DEFAULT_HASH_ALGORITHM)));
        return new TCPClientSettings(new InetSocketAddress(signerIP, signerPort), tcpTransactionTimeoutSec,
                serviceCredentials);
    }

    protected static TCPClientSettings loadTCPExtendingSettings(){
        Properties props = loadProperties();
        String extenderIp = getProperty(props, "tcp.extenderIP");
        int extenderPort = Integer.parseInt(getProperty(props, "tcp.extenderPort"));
        int tcpTransactionTimeoutSec = Integer.parseInt(getProperty(props, "tcp.transactionTimeoutSec"));
        ServiceCredentials serviceCredentials = new KSIServiceCredentials(
                getProperty(props, "tcp.extenderLoginId", "tcp.loginId"),
                getProperty(props, "tcp.extenderLoginKey", "tcp.loginKey"),
                HashAlgorithm.getByName(props.getProperty("tcp.extenderHmacAlgorithm", DEFAULT_HASH_ALGORITHM)));
        return new TCPClientSettings(new InetSocketAddress(extenderIp, extenderPort), tcpTransactionTimeoutSec,
                serviceCredentials);
    }

    public static HttpSettings loadPublicationsFileSettings() {
        if (publicationsFileSettings == null) {
            Properties props = loadProperties();
            HTTPConnectionParameters params = new HTTPConnectionParameters(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
            publicationsFileSettings = new HttpSettings(getProperty(props, "pubfileUrl"), params);
        }
        return publicationsFileSettings;
    }

    public static CredentialsAwareHttpSettings loadSignerSettings() {
        if (signingSettings == null) {
            signingSettings = loadSignerSettings(PduVersion.V2);
        }
        return signingSettings;
    }

    public static CredentialsAwareHttpSettings loadExtenderSettings() {
        if (extenderSettings == null) {
            extenderSettings = loadExtenderSettings(PduVersion.V2);
        }
        return extenderSettings;
    }

    public static CredentialsAwareHttpSettings loadSignerSettings(PduVersion pduVersion) {
        Properties props = loadProperties();
        ServiceCredentials credentials = new KSIServiceCredentials(
                getProperty(props, "signerLoginId", "loginId"),
                getProperty(props, "signerLoginKey", "loginKey"),
                HashAlgorithm.getByName(props.getProperty("signerHmacAlgorithm", DEFAULT_HASH_ALGORITHM)));
        return loadSettings(getProperty(props, "signerUrl", "gatewayUrl"), credentials, pduVersion);
    }

    public static CredentialsAwareHttpSettings loadExtenderSettings(PduVersion pduVersion) {
        Properties props = loadProperties();

        ServiceCredentials credentials = new KSIServiceCredentials(
                getProperty(props, "extenderLoginId", "loginId"),
                getProperty(props, "extenderLoginKey", "loginKey"),
                HashAlgorithm.getByName(props.getProperty("extenderHmacAlgorithm", DEFAULT_HASH_ALGORITHM)));
        return loadSettings(getProperty(props, "extenderUrl"), credentials,  pduVersion);
    }

    public static CredentialsAwareHttpSettings loadSettings(String url, ServiceCredentials credentials, PduVersion pduVersion) {
        HTTPConnectionParameters params = new HTTPConnectionParameters(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
        CredentialsAwareHttpSettings settings = new CredentialsAwareHttpSettings(url, credentials, params);
        settings.setPduVersion(pduVersion);
        return settings;
    }

    @DataProvider(name = KSI_DATA_GROUP_NAME, parallel = true)
    public static Object[][] transportProtocols() throws Exception {
        if (signingSettings == null) {
            signingSettings = loadSignerSettings();
        }

        if (extenderSettings == null){
            extenderSettings = loadExtenderSettings();
        }

        if (publicationsFileSettings == null) {
            publicationsFileSettings = loadPublicationsFileSettings();
        }

        SimpleHttpSigningClient simpleHttpSigningClient = new SimpleHttpSigningClient(signingSettings);
        ApacheHttpSigningClient apacheHttpSigningClient = new ApacheHttpSigningClient(signingSettings);

        SimpleHttpExtenderClient simpleHttpExtenderClient = new SimpleHttpExtenderClient(extenderSettings);
        ApacheHttpExtenderClient apacheHttpExtenderClient = new ApacheHttpExtenderClient(extenderSettings);

        SimpleHttpPublicationsFileClient simpleHttpPublicationsFileClient1 = new SimpleHttpPublicationsFileClient(publicationsFileSettings);
        ApacheHttpPublicationsFileClient apacheHttpPublicationsFileClient1 = new ApacheHttpPublicationsFileClient(publicationsFileSettings);
        SimpleHttpPublicationsFileClient simpleHttpPublicationsFileClient2 = new SimpleHttpPublicationsFileClient(publicationsFileSettings);
        ApacheHttpPublicationsFileClient apacheHttpPublicationsFileClient2 = new ApacheHttpPublicationsFileClient(publicationsFileSettings);

        KSISigningClient tcpClient = new TCPClient(loadTCPSigningSettings(), loadTCPExtendingSettings());

        PendingKSIService pendingKSIService = new PendingKSIService();

        List<KSISigningClient> signingClientsForHa = new ArrayList<KSISigningClient>();
        signingClientsForHa.add(simpleHttpSigningClient);
        signingClientsForHa.add(apacheHttpSigningClient);
        List<KSISigningService> signingServicesForHa = new ArrayList<KSISigningService>();
        signingServicesForHa.add(pendingKSIService);

        List<KSIExtenderClient> extenderClientsForHa = new ArrayList<KSIExtenderClient>();
        extenderClientsForHa.add(simpleHttpExtenderClient);
        extenderClientsForHa.add(apacheHttpExtenderClient);
        List<KSIExtendingService> extendingServicesForHa = new ArrayList<KSIExtendingService>();
        extendingServicesForHa.add(pendingKSIService);

        HAService haService = new HAService.Builder()
                .addSigningClients(signingClientsForHa)
                .addSigningServices(signingServicesForHa)
                .addExtenderClients(extenderClientsForHa)
                .addExtenderServices(extendingServicesForHa)
                .build();

        return new Object[][] {
                new Object[] {addToList(
                        createKsi(simpleHttpExtenderClient, simpleHttpSigningClient, simpleHttpPublicationsFileClient1),
                        listOfCloseables
                )},
                new Object[] {addToList(
                        createKsi(apacheHttpExtenderClient, apacheHttpSigningClient, apacheHttpPublicationsFileClient1),
                        listOfCloseables
                )},
                new Object[] {addToList(
                        createKsi((KSIExtenderClient) tcpClient, tcpClient, simpleHttpPublicationsFileClient2),
                        listOfCloseables
                )},
                new Object[] {addToList(
                        createKsi(haService, haService, apacheHttpPublicationsFileClient2),
                        listOfCloseables
                )}
        };
    }

    protected static Closeable addToList(Closeable object, List<Closeable> list) {
        list.add(object);
        return object;
    }

    private String loadJavaKeyStorePath() {
        Properties props = loadProperties();
        if (javaKeyStorePath == null && props.containsKey("javaKeyStorePath")) {
            javaKeyStorePath = getProperty(props, "javaKeyStorePath");
        }
        return javaKeyStorePath;
    }

    private static Properties loadProperties() {
        if (properties == null) {
            properties = new Properties();
            try {
                properties.load(load(PROPERTIES_INTEGRATION_TEST));
            } catch (IOException e) {
                throw new RuntimeException(PROPERTIES_INTEGRATION_TEST
                        + " file must be added to folder 'ksi-api/src/test/resources' for running the integration tests");
            }
        }
        return properties;
    }

    private static String getProperty(Properties props, String preferredKey, String alternativeKey) {
        String value;
        if (props.containsKey(preferredKey)) {
            value = getProperty(props, preferredKey);
        } else if (props.containsKey(alternativeKey)) {
            value = getProperty(props, alternativeKey);
        } else {
            throw new NullPointerException(preferredKey + " is missing in " + PROPERTIES_INTEGRATION_TEST);
        }
        return value;
    }

    private static String getProperty(Properties prop, String key) {
        return Objects.requireNonNull(prop.getProperty(key), key + " is missing in " + PROPERTIES_INTEGRATION_TEST);
    }

    protected static Object[] createKsiObject(KSIExtenderClient extenderClient, KSISigningClient signingClient,
                                              KSIPublicationsFileClient publicationsFileClient) throws Exception {
        return new Object[] {createKsi(extenderClient, signingClient, publicationsFileClient)};
    }

    protected static KSI createKsi(KSIExtenderClient extenderClient, KSISigningClient signingClient, KSIPublicationsFileClient
            publicationsFileClient) throws Exception {
        return initKsiBuilder(extenderClient, signingClient, publicationsFileClient).build();
    }

    protected static KSIBuilder initKsiBuilder(KSIExtenderClient extenderClient, KSISigningClient signingClient,
                                               KSIPublicationsFileClient publicationsFileClient) throws Exception {
        return new KSIBuilder().setKsiProtocolExtenderClient(extenderClient).
                setKsiProtocolPublicationsFileClient(publicationsFileClient).
                setKsiProtocolSignerClient(signingClient).
                setPublicationsFilePkiTrustStore(createKeyStore()).
                setPublicationsFileTrustedCertSelector(createCertSelector()).
                setDefaultVerificationPolicy(new InternalVerificationPolicy());
    }

    protected PublicationsHandler getPublicationsHandler(KSIPublicationsFileClient publicationsFileClient) throws Exception {
        return new PublicationsHandlerBuilder().setKsiProtocolPublicationsFileClient(publicationsFileClient)
                .setPublicationsFileCacheExpirationTime(10000L)
                .setPublicationsFilePkiTrustStore(createKeyStore())
                .setPublicationsFileCertificateConstraints(createCertSelector()).build();
    }

    protected Extender getExtender(KSIExtendingService extendingService, KSIPublicationsFileClient publicationsFileClient) throws Exception {
        return new ExtenderBuilder()
                .setExtendingService(extendingService)
                .setPublicationsHandler(getPublicationsHandler(publicationsFileClient)).build();
    }

    protected static KSI createKsi(KSIExtendingService extendingService, KSISigningService signingService, KSIPublicationsFileClient
            publicationsFileClient) throws Exception {
        return initKsiBuilder(extendingService, signingService, publicationsFileClient).build();
    }

    protected static KSIBuilder initKsiBuilder(KSIExtendingService extendingService, KSISigningService signingService,
                                               KSIPublicationsFileClient publicationsFileClient) throws Exception {
        return new KSIBuilder().setKsiProtocolExtendingService(extendingService).
                setKsiProtocolPublicationsFileClient(publicationsFileClient).
                setKsiProtocolSigningService(signingService).
                setPublicationsFilePkiTrustStore(createKeyStore()).
                setPublicationsFileTrustedCertSelector(createCertSelector());
    }

    protected static KeyStore createKeyStore() throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(KSI_TRUSTSTORE), KSI_TRUSTSTORE_PASSWORD.toCharArray());
        return trustStore;
    }

    protected static X509CertificateSubjectRdnSelector createCertSelector() throws KSIException {
        return new X509CertificateSubjectRdnSelector("E=publications@guardtime.com");
    }

    public VerificationResult verify(KSI ksi, KSIExtendingService extendingService, KSISignature signature, Policy policy) throws
            KSIException {
        VerificationContextBuilder builder = new VerificationContextBuilder();
        builder.setSignature(signature).setExtendingService(extendingService).setPublicationsFile(ksi.getPublicationsFile());
        return ksi.verify(builder.build(), policy);
    }

    public VerificationResult verify(KSI ksi, KSIExtendingService extendingService, KSISignature signature, Policy policy, boolean
            extendingAllowed) throws
            KSIException {
        VerificationContextBuilder builder = new VerificationContextBuilder();
        builder.setSignature(signature).setExtendingService(extendingService).setPublicationsFile(ksi.getPublicationsFile());
        builder.setExtendingAllowed(extendingAllowed);
        return ksi.verify(builder.build(), policy);
    }

    public VerificationResult verify(KSI ksi, KSIExtenderClient extenderClient, KSISignature signature, Policy policy, boolean
            extendingAllowed) throws KSIException {
        VerificationContextBuilder builder = new VerificationContextBuilder();
        builder.setSignature(signature).setExtenderClient(extenderClient).setPublicationsFile(ksi.getPublicationsFile());
        builder.setExtendingAllowed(extendingAllowed);
        return ksi.verify(builder.build(), policy);
    }

    public VerificationResult verify(KSI ksi, KSIExtenderClient extenderClient, KSISignature signature, Policy policy,
                                     PublicationData userPublication, boolean extendingAllowed) throws KSIException {
        VerificationContextBuilder builder = new VerificationContextBuilder();
        builder.setSignature(signature).setExtenderClient(extenderClient).setPublicationsFile(ksi.getPublicationsFile());
        builder.setUserPublication(userPublication);
        builder.setExtendingAllowed(extendingAllowed);
        return ksi.verify(builder.build(), policy);
    }

    protected static KSISigningService mockSigningService(final String responseFile, final ServiceCredentials credentials) throws Exception {
        KSISigningService mockedSigningService = Mockito.mock(KSISigningService.class);

        final Future<TLVElement> mockedFuture = Mockito.mock(Future.class);
        Mockito.when(mockedFuture.isFinished()).thenReturn(Boolean.TRUE);
        final TLVElement responseTLV = TLVElement.create(IOUtils.toByteArray(load(responseFile)));
        Mockito.when(mockedFuture.getResult()).thenReturn(responseTLV);

        Mockito.when(mockedSigningService.sign(Mockito.any(DataHash.class), Mockito.any
                (long.class))).then(new Answer<Future>() {
            public Future<AggregationResponse> answer(InvocationOnMock invocationOnMock) throws Throwable {
                DataHash dataHash = (DataHash) invocationOnMock.getArguments()[0];
                long level = (long) invocationOnMock.getArguments()[1];

                PduFactory factory = new PduV2Factory();
                KSIRequestContext context = RequestContextFactory.DEFAULT_FACTORY.createContext();
                AggregationRequest request = factory.createAggregationRequest(context, credentials, dataHash, level);
                ByteArrayInputStream bais = new ByteArrayInputStream(request.toByteArray());
                TLVElement requestElement = TLVElement.create(Util.toByteArray(bais));
                //Set header
                responseTLV.getFirstChildElement(0x1).setContent(requestElement.getFirstChildElement(0x1).getEncoded());
                //Set Request ID
                responseTLV.getFirstChildElement(0x2).getFirstChildElement(ELEMENT_TYPE_REQUEST_ID).setLongContent(
                        requestElement.getFirstChildElement(0x2).getFirstChildElement(ELEMENT_TYPE_REQUEST_ID).getDecodedLong()
                );
                //Set Input hash
                responseTLV.getFirstChildElement(0x2).getFirstChildElement(AggregationHashChain.ELEMENT_TYPE).getFirstChildElement(0x5).setDataHashContent(dataHash);
                //Update HMAC
                responseTLV.getFirstChildElement(0x1F).setDataHashContent(
                        calculateHash(
                                responseTLV,
                                responseTLV.getFirstChildElement(0x1F).getDecodedDataHash().getAlgorithm(),
                                credentials.getLoginKey()
                        )
                );
                return new AggregationResponseFuture(mockedFuture, context, credentials, factory);
            }
        });

        return mockedSigningService;
    }

    protected KSIExtendingService mockExtenderResponseCalendarHashCain(String responseCalendarChainFile) throws Exception {
        KSIExtendingService mockedExtenderService = Mockito.mock(KSIExtendingService.class);
        final Future<TLVElement> mockedFuture = Mockito.mock(Future.class);
        Mockito.when(mockedFuture.isFinished()).thenReturn(Boolean.TRUE);
        final TLVElement responseTLV = putCHCIntoExtenderResponsePdu(responseCalendarChainFile);
        Mockito.when(mockedFuture.getResult()).thenReturn(responseTLV);

        Mockito.when(mockedExtenderService.extend(Mockito.any(Date.class), Mockito.any
                (Date.class))).then(new Answer<Future>() {
            public Future answer(InvocationOnMock invocationOnMock) throws Throwable {
                ServiceCredentials credentials = loadExtenderSettings().getCredentials();
                KSIRequestContext requestContext = RequestContextFactory.DEFAULT_FACTORY.createContext();
                Date aggregationTime = (Date) invocationOnMock.getArguments()[0];
                Date publicationTime = (Date) invocationOnMock.getArguments()[1];
                PduFactory pduFactory = new PduV2Factory();
                ExtensionRequest requestMessage = pduFactory.createExtensionRequest(requestContext, credentials, aggregationTime,
                        publicationTime);
                ByteArrayInputStream requestStream = new ByteArrayInputStream(requestMessage.toByteArray());
                TLVElement requestElement = TLVElement.create(Util.toByteArray(requestStream));
                TLVElement responsePayload = responseTLV.getFirstChildElement(0x02);
                //Set header loginID
                responseTLV.getFirstChildElement(0x1).setContent(requestElement.getFirstChildElement(0x1).getEncoded());
                //Set request id
                responsePayload.getFirstChildElement(0x01).setLongContent(requestElement.getFirstChildElement(0x02).getFirstChildElement
                        (ELEMENT_TYPE_REQUEST_ID).getDecodedLong());
                //Update HMAC
                responseTLV.getFirstChildElement(0x1F).setDataHashContent(
                        calculateHash(
                                responseTLV,
                                responseTLV.getFirstChildElement(0x1F).getDecodedDataHash().getAlgorithm(),
                                credentials.getLoginKey()
                        ));
                return new ExtensionResponseFuture(mockedFuture, requestContext, credentials, pduFactory);
            }
        });
        return mockedExtenderService;
    }

    private static TLVElement putCHCIntoExtenderResponsePdu(String extenderResponse) throws IllegalArgumentException, IOException, TLVParserException {
        TLVElement rsp = TLVElement.create(IOUtils.toByteArray(load(extenderResponse)));
        if (rsp.getType() == ELEMENT_TYPE_EXTENSION_RESPONSE_PDU_V2) {
            return rsp;
        } else if (rsp.getType() == 0x2) {
            TLVElement responseTLV = TLVElement.create(IOUtils.toByteArray(load(EXTENDED_RESPONSE_WITH_NO_CALENDAR)));
            responseTLV.replace(responseTLV.getFirstChildElement(0x2), rsp);
            return responseTLV;
        } else if (rsp.getType() == CalendarHashChain.ELEMENT_TYPE) {
            TLVElement responseTLV = TLVElement.create(IOUtils.toByteArray(load(EXTENDED_RESPONSE_WITH_NO_CALENDAR)));
            responseTLV.getFirstChildElement(0x2).replace(responseTLV.getFirstChildElement(0x2).getFirstChildElement(CalendarHashChain.ELEMENT_TYPE), rsp);
            return responseTLV;
        }
        throw new IllegalArgumentException("Provided extender response is not supported.");
    }
}
