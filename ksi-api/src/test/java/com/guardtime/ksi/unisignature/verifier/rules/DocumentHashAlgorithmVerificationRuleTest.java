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

package com.guardtime.ksi.unisignature.verifier.rules;

import com.guardtime.ksi.TestUtil;
import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.unisignature.verifier.RuleResult;
import com.guardtime.ksi.unisignature.verifier.VerificationErrorCode;
import com.guardtime.ksi.unisignature.verifier.VerificationResultCode;

import org.testng.Assert;
import org.testng.annotations.Test;

import static com.guardtime.ksi.Resources.RFC3161_SIGNATURE;
import static com.guardtime.ksi.Resources.SIGNATURE_2017_03_14;

public class DocumentHashAlgorithmVerificationRuleTest extends AbstractRuleTest {

    private Rule rule = new DocumentHashAlgorithmVerificationRule();

    @Test
    public void testSignatureVerificationWithoutDocumentHashReturnsOkStatus_Ok() throws Exception {
        RuleResult result = rule.verify(build(TestUtil.loadSignature(SIGNATURE_2017_03_14)));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResultCode(), VerificationResultCode.OK);
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void testSignatureVerificationWithValidDocumentHashReturnsOkStatus_Ok() throws Exception {
        KSISignature signature = TestUtil.loadSignature(SIGNATURE_2017_03_14);
        Assert.assertEquals(rule.verify(build(signature, signature.getInputHash())).getResultCode(), VerificationResultCode.OK);
    }

    @Test
    public void testSignatureVerificationWithInvalidDocumentHashAlgorithm() throws Exception {
        RuleResult result = rule.verify(build(TestUtil.loadSignature(SIGNATURE_2017_03_14), new DataHash(HashAlgorithm.SHA2_512, new byte[64])));
        Assert.assertEquals(result.getResultCode(), VerificationResultCode.FAIL);
        Assert.assertEquals(result.getErrorCode(), VerificationErrorCode.GEN_04);
    }

    @Test
    public void testSignatureVerificationWithInvalidRfc3161OutputHashReturnsFailStatus_Ok() throws Exception {
        RuleResult result = rule.verify(build(TestUtil.loadSignature(RFC3161_SIGNATURE), new DataHash(HashAlgorithm.SHA2_512, new byte[64])));
        Assert.assertEquals(result.getResultCode(), VerificationResultCode.FAIL);
        Assert.assertEquals(result.getErrorCode(), VerificationErrorCode.GEN_04);
    }

}