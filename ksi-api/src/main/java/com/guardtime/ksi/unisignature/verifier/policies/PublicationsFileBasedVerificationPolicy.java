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

package com.guardtime.ksi.unisignature.verifier.policies;

import com.guardtime.ksi.unisignature.verifier.rules.CalendarHashChainAlgorithmDeprecatedExtenderResponseRule;
import com.guardtime.ksi.unisignature.verifier.rules.CalendarHashChainAlgorithmDeprecatedRule;
import com.guardtime.ksi.unisignature.verifier.rules.CompositeRule;
import com.guardtime.ksi.unisignature.verifier.rules.ExtendingPermittedVerificationRule;
import com.guardtime.ksi.unisignature.verifier.rules.NotRule;
import com.guardtime.ksi.unisignature.verifier.rules.PublicationsFileContainsPublicationRule;
import com.guardtime.ksi.unisignature.verifier.rules.PublicationsFileContainsSignaturePublicationRule;
import com.guardtime.ksi.unisignature.verifier.rules.PublicationsFileExtendedSignatureInputHashRule;
import com.guardtime.ksi.unisignature.verifier.rules.PublicationsFilePublicationHashMatchesExtenderResponseRule;
import com.guardtime.ksi.unisignature.verifier.rules.PublicationsFilePublicationTimeMatchesExtenderResponseRule;
import com.guardtime.ksi.unisignature.verifier.rules.Rule;
import com.guardtime.ksi.unisignature.verifier.rules.SignaturePublicationRecordExistenceRule;

/**
 *
 * KSI Signature verification policy. Can be used to verify signatures using publications file.
 */
public class PublicationsFileBasedVerificationPolicy extends InternalVerificationPolicy {

    private static final String TYPE_PUBLICATIONS_FILE_BASED_POLICY = "PUBLICATIONS_FILE_BASED_POLICY";

    public PublicationsFileBasedVerificationPolicy() {

        Rule useExtendingRule = new CompositeRule(false,
                new PublicationsFileContainsPublicationRule(),
                new ExtendingPermittedVerificationRule(),
                new CalendarHashChainAlgorithmDeprecatedExtenderResponseRule(),
                new PublicationsFilePublicationHashMatchesExtenderResponseRule(),
                new PublicationsFilePublicationTimeMatchesExtenderResponseRule(),
                new PublicationsFileExtendedSignatureInputHashRule()
        );

        Rule publicationsEqual = new CompositeRule(false,
                new SignaturePublicationRecordExistenceRule(),
                new PublicationsFileContainsSignaturePublicationRule(),
                new CalendarHashChainAlgorithmDeprecatedRule());

        Rule publicationTimesNotEqualDoExtending = new CompositeRule(false,
                new SignaturePublicationRecordExistenceRule(),
                new NotRule(new PublicationsFileContainsSignaturePublicationRule()),
                useExtendingRule);

        Rule signatureDoesNotContainPublicationDoExtending = new CompositeRule(false,
                new NotRule(new SignaturePublicationRecordExistenceRule()),
                useExtendingRule);

        addRule(new CompositeRule(true,
                publicationsEqual,
                publicationTimesNotEqualDoExtending,
                signatureDoesNotContainPublicationDoExtending));

    }

    public String getName() {
        return "Publications file based verification policy";
    }

    public String getType() {
        return TYPE_PUBLICATIONS_FILE_BASED_POLICY;
    }

}
