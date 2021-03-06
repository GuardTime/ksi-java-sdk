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

package com.guardtime.ksi.unisignature.verifier;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.service.KSIProtocolException;
import com.guardtime.ksi.service.client.KSIClientException;
import com.guardtime.ksi.unisignature.inmemory.InMemoryKsiSignatureComponentFactory;
import com.guardtime.ksi.unisignature.verifier.policies.ContextAwarePolicy;
import com.guardtime.ksi.unisignature.verifier.policies.Policy;
import com.guardtime.ksi.unisignature.verifier.policies.PolicyContext;
import com.guardtime.ksi.unisignature.verifier.rules.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * KSI signature verifier implementation.
 */
public final class KSISignatureVerifier implements SignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(KSISignatureVerifier.class);

    public KSIVerificationResult verify(VerificationContext context, Policy policy) throws KSIException {
        logger.info("Starting to verify signature {} using policy {}", context.getSignature(), policy.getName());
        KSIVerificationResult finalResult = new KSIVerificationResult();
        Policy runPolicy = policy;
        while (runPolicy != null) {
            PolicyVerificationResult result = verifySignature(context, runPolicy);
            finalResult.addPolicyResult(result);
            if (VerificationResultCode.NA.equals(result.getPolicyStatus())) {
                Policy fallbackPolicy = runPolicy.getFallbackPolicy();
                logger.info("Using a fallback policy {}", fallbackPolicy);
                if (fallbackPolicy instanceof ContextAwarePolicy) {
                    PolicyContext c = ((ContextAwarePolicy) fallbackPolicy).getPolicyContext();
                    context = new VerificationContextBuilder()
                            .setDocumentHash(context.getDocumentHash(), context.getInputHashLevel())
                            .setExtendingService(c.getExtendingService())
                            .setExtendingAllowed(c.isExtendingAllowed())
                            .setPublicationsFile(c.getPublicationsHandler() != null ? c.getPublicationsHandler().getPublicationsFile() : null)
                            .setSignature(context.getSignature())
                            .setUserPublication(c.getUserPublication())
                            .build();
                    context.setKsiSignatureComponentFactory(new InMemoryKsiSignatureComponentFactory());
                }
                runPolicy = fallbackPolicy;
            } else {
                runPolicy = null;
            }
        }
        return finalResult;
    }

    private KSIPolicyVerificationResult verifySignature(VerificationContext context, Policy policy) throws KSIException {
        KSIPolicyVerificationResult policyVerificationResult = new KSIPolicyVerificationResult(policy);
        List<Rule> rules = policy.getRules();
        for (final Rule rule : rules) {
            if (logger.isDebugEnabled()) {
                logger.debug("Starting to execute rule {}", rule);
            }
            RuleResult result = createRuleResult(context, rule);
            policyVerificationResult.addRuleResult(rule, result);
            policyVerificationResult.setPolicyStatus(result.getResultCode());

            if (!VerificationResultCode.OK.equals(result.getResultCode())) {
                break;
            }
        }
        return policyVerificationResult;
    }

    private RuleResult createRuleResult(VerificationContext context, final Rule rule) throws KSIException {
        RuleResult result;
        try {
            result = rule.verify(context);
            if (logger.isDebugEnabled()) {
                logger.debug("Rule '{}' result is {}", rule, result);
            }
        } catch (KSIProtocolException | KSIClientException e) {
            // accessing an external resource (extender or publication file) failed
            logger.warn("An error was returned while fetching a resource", e);
            result = new RuleResult() {
                @Override
                public VerificationResultCode getResultCode() {
                    return VerificationResultCode.NA;
                }

                @Override
                public VerificationErrorCode getErrorCode() {
                    return VerificationErrorCode.GEN_02;
                }

                @Override
                public String getRuleName() {
                    return rule.toString();
                }

                @Override
                public Exception getException() {
                    return e;
                }
            };
        }
        return result;
    }

    private class KSIPolicyVerificationResult implements PolicyVerificationResult {

        private final Policy policy;
        private VerificationResultCode policyStatus = VerificationResultCode.NA;
        private Map<Rule, RuleResult> ruleResults = new LinkedHashMap<>();
        private VerificationErrorCode errorCode;
        private Exception exception;

        public KSIPolicyVerificationResult(Policy policy) {
            this.policy = policy;
        }

        public void addRuleResult(Rule rule, RuleResult result) {
            ruleResults.put(rule, result);
            if (!VerificationResultCode.OK.equals(result.getResultCode())) {
                this.errorCode = result.getErrorCode();
                this.exception = result.getException();
            }
        }

        public VerificationResultCode getPolicyStatus() {
            return policyStatus;
        }

        public void setPolicyStatus(VerificationResultCode policyStatus) {
            this.policyStatus = policyStatus;
        }

        public Policy getPolicy() {
            return policy;
        }

        public VerificationErrorCode getErrorCode() {
            return errorCode;
        }

        public Exception getException() {
            return exception;
        }

        public Map<Rule, RuleResult> getRuleResults() {
            return ruleResults;
        }

        @Override
        public String toString() {
            return "policy='" + policy.getName() +
                    "', policyStatus=" + policyStatus + ", errorCode=" + errorCode +
                    ", ruleResults=[" + ruleResults.values() + "]";
        }
    }


    private class KSIVerificationResult implements VerificationResult {

        private List<PolicyVerificationResult> policyResults = new LinkedList<>();
        private VerificationErrorCode errorCode;

        public void addPolicyResult(PolicyVerificationResult result) {
            policyResults.add(result);
            if (!VerificationResultCode.OK.equals(result.getPolicyStatus())) {
                this.errorCode = result.getErrorCode();
            } else {
                this.errorCode = null;
            }
        }

        public VerificationErrorCode getErrorCode() {
            return errorCode;
        }

        public boolean isOk() {
            for (PolicyVerificationResult policyResult : policyResults) {
                if (VerificationResultCode.OK.equals(policyResult.getPolicyStatus())) {
                    return true;
                }
            }
            return false;
        }

        public List<PolicyVerificationResult> getPolicyVerificationResults() {
            return policyResults;
        }

        @Override
        public String toString() {
            return "Result=" + getPolicyVerificationResults() + ", errorCode=" + errorCode + ", policyResult=[" + policyResults + "]";
        }
    }

}
