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
package com.guardtime.ksi.integration;

import com.guardtime.ksi.KSI;
import com.guardtime.ksi.Signer;
import com.guardtime.ksi.SignerBuilder;
import com.guardtime.ksi.TestUtil;
import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.unisignature.verifier.VerificationErrorCode;
import com.guardtime.ksi.unisignature.verifier.VerificationResult;
import com.guardtime.ksi.unisignature.verifier.policies.KeyBasedVerificationPolicy;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.guardtime.ksi.CommonTestUtil.loadFile;
import static com.guardtime.ksi.Resources.INPUT_FILE;
import static com.guardtime.ksi.Resources.INPUT_FILE_REVERSED;

public class SignIntegrationTest extends AbstractCommonIntegrationTest {

    @Test
    public void testSigningWithSignerClient_Ok() throws Exception {
        Signer s = new SignerBuilder().setSigningService(ksi.getSigningService()).build();
        KSISignature sig = s.sign(loadFile(INPUT_FILE));
        VerificationResult result = ksi.verify(TestUtil.buildContext(sig, ksi, simpleHttpClient, getFileHash(INPUT_FILE)), new KeyBasedVerificationPolicy());
        Assert.assertTrue(result.isOk());
    }

    @Test(dataProvider = KSI_DATA_GROUP_NAME, groups = TEST_GROUP_INTEGRATION)
    public void testSignFile_Ok(KSI ksi) throws Exception {
        KSISignature sig = ksi.sign(loadFile(INPUT_FILE));
        VerificationResult result = ksi.verify(TestUtil.buildContext(sig, ksi, ksi.getExtendingService(), getFileHash(INPUT_FILE)), new KeyBasedVerificationPolicy());
        Assert.assertTrue(result.isOk());
    }

    @Test(dataProvider = KSI_DATA_GROUP_NAME, groups = TEST_GROUP_INTEGRATION)
    public void testSignHash_Ok(KSI ksi) throws Exception {
        KSISignature sig = ksi.sign(getFileHash(INPUT_FILE));
        VerificationResult result = ksi.verify(TestUtil.buildContext(sig, ksi, ksi.getExtendingService(), getFileHash(INPUT_FILE)), new KeyBasedVerificationPolicy());
        Assert.assertTrue(result.isOk());
    }

    @Test(dataProvider = KSI_DATA_GROUP_NAME, groups = TEST_GROUP_INTEGRATION)
    public void testSignHashWithLevel_Ok(KSI ksi) throws Exception {
        KSISignature sig = ksi.sign(getFileHash(INPUT_FILE), 3L);
        VerificationResult result = ksi.verify(TestUtil.buildContext(sig, ksi, ksi.getExtendingService(), getFileHash(INPUT_FILE), 3L), new KeyBasedVerificationPolicy());
        Assert.assertTrue(result.isOk());
    }

    @Test(dataProvider = KSI_DATA_GROUP_NAME, groups = TEST_GROUP_INTEGRATION)
    public void testSignFileAndUseInvalidHashForVerification_VerificationFailsWithErrorGen1(KSI ksi) throws Exception {
        KSISignature sig = ksi.sign(loadFile(INPUT_FILE));
        VerificationResult result = ksi.verify(TestUtil.buildContext(sig, ksi, ksi.getExtendingService(), getFileHash(INPUT_FILE_REVERSED)), new KeyBasedVerificationPolicy());
        Assert.assertFalse(result.isOk());
        Assert.assertEquals(result.getErrorCode(), VerificationErrorCode.GEN_01);
    }

}
