/*
 * Copyright 2013-2019 Guardtime, Inc.
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

package com.guardtime.ksi.tree;

import com.guardtime.ksi.AbstractBlockSignatureTest;
import com.guardtime.ksi.blocksigner.IdentityMetadata;
import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.hashing.DataHasher;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.util.Base16;
import com.guardtime.ksi.util.Util;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.guardtime.ksi.AbstractBlockSignatureTest.DATA_HASH;
import static com.guardtime.ksi.AbstractBlockSignatureTest.DATA_HASH_2;
import static com.guardtime.ksi.AbstractBlockSignatureTest.DATA_HASH_3;

public class BlindingMaskLinkingHashTreeBuilderTest {

    public static final byte[] INITIALIZATION_VECTOR = new byte[32];
    private final DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);

    @Test
    public void testBlindingMasksCalculation() throws KSIException {
        BlindingMaskLinkingHashTreeBuilder treeBuilder = new BlindingMaskLinkingHashTreeBuilder(INITIALIZATION_VECTOR, DATA_HASH);
        treeBuilder.add(
            new ImprintNode(AbstractBlockSignatureTest.DATA_HASH_2),
            new ImprintNode(AbstractBlockSignatureTest.DATA_HASH_3)
        );

        ImprintNode root = treeBuilder.build();
        Assert.assertEquals(root.getLevel(), 2);

        DataHash firstSignatureMask = hasher.addData(DATA_HASH).addData(INITIALIZATION_VECTOR).getHash();

        // Test first blinding mask
        TreeNode blindingMask1 = root.getLeftChildNode().getLeftChildNode();
        Assert.assertEquals(new DataHash(blindingMask1.getValue()), firstSignatureMask);

        hasher.reset();

        // Test first node value
        DataHash nodeDataHash = hasher.addData(firstSignatureMask).addData(AbstractBlockSignatureTest.DATA_HASH_2).addData(Util.encodeUnsignedLong(1)).getHash();
        Assert.assertEquals(new DataHash(root.getLeftChildNode().getValue()), nodeDataHash);

        hasher.reset();

        // Test second blinding mask
        DataHash secondMask = hasher.addData(nodeDataHash).addData(INITIALIZATION_VECTOR).getHash();
        TreeNode blindingMask2 = root.getRightChildNode().getLeftChildNode();
        Assert.assertEquals(new DataHash(blindingMask2.getValue()), secondMask);

        hasher.reset();

        // Test second node value
        nodeDataHash = hasher.addData(secondMask).addData(AbstractBlockSignatureTest.DATA_HASH_3).addData(Util.encodeUnsignedLong(1)).getHash();
        Assert.assertEquals(new DataHash(root.getRightChildNode().getValue()), nodeDataHash);

        // Test tree builder output hash
        Assert.assertEquals(new DataHash(root.getRightChildNode().getValue()), treeBuilder.getLastNodeHash());
    }

    @Test
    public void testCalculateHeight() throws KSIException {
        BlindingMaskLinkingHashTreeBuilder builder = createTreeBuilder();
        builder.add(new ImprintNode(DATA_HASH_2));
        Assert.assertEquals(builder.calculateHeight(new ImprintNode(DATA_HASH_3)), 2);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = "Identity metadata is not supported by BlindingMaskLinkingHashTreeBuilder")
    public void testCalculateHeightThrowsExceptionWhenIdentityMetadataIsPresent() {
        BlindingMaskLinkingHashTreeBuilder builder = createTreeBuilder();
        builder.calculateHeight(new ImprintNode(DATA_HASH_2), new IdentityMetadata("1"));
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "HashAlgorithm can not be null")
    public void testCreateTreeBuilderUsingNullHashAlgorithm() {
        new BlindingMaskLinkingHashTreeBuilder(null, INITIALIZATION_VECTOR, AbstractBlockSignatureTest.DATA_HASH_2);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "HashAlgorithm can not be null")
    public void testCreateTreeBuilderUsingNullHashAlgorithm2() {
        new BlindingMaskLinkingHashTreeBuilder(null, INITIALIZATION_VECTOR, DATA_HASH_2, new HashTreeBuilder());
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Initialization vector can not be null")
    public void testCreateTreeBuilderUsingNullInitializationVector() {
        new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA2_256, null, AbstractBlockSignatureTest.DATA_HASH_2);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Initialization vector can not be null")
    public void testCreateTreeBuilderUsingNullInitializationVector2() {
        new BlindingMaskLinkingHashTreeBuilder(null, AbstractBlockSignatureTest.DATA_HASH_2);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Initialization vector can not be null")
    public void testCreateTreeBuilderUsingNullInitializationVector3() {
        new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA2_256, null, DATA_HASH_2, new HashTreeBuilder());
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Initialization vector can not be null")
    public void testCreateTreeBuilderUsingNullInitializationVector4() {
        new BlindingMaskLinkingHashTreeBuilder(null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Previous block hash can not be null")
    public void testCreateTreeBuilderUsingNullPreviousHash() {
        new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA2_256, INITIALIZATION_VECTOR, null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Previous block hash can not be null")
    public void testCreateTreeBuilderUsingNullPreviousHash2() {
        new BlindingMaskLinkingHashTreeBuilder(INITIALIZATION_VECTOR, null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Previous block hash can not be null")
    public void testCreateTreeBuilderUsingNullPreviousHash3() {
        new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA2_256, INITIALIZATION_VECTOR, null, new HashTreeBuilder());
    }

    @Test
    public void testUseTreeBuilderUsingInitializationVectorWithShortLength() throws KSIException {
        BlindingMaskLinkingHashTreeBuilder builder = new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA2_256, new byte[16], DATA_HASH_2);
        addAndBuild(builder, Base16.decode("01A0AA9C6859E051A6224EC3F2B65B1ACC7A6C2EA2EBFB83AD1E52BCADB3E1552B"));
    }

    @Test
    public void testUseTreeBuilderUsingNonDefaultAlgorithm() throws KSIException {
        BlindingMaskLinkingHashTreeBuilder builder = new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA2_384, new byte[HashAlgorithm.SHA2_384.getLength()], DATA_HASH_2);
        addAndBuild(builder, Base16.decode("04CC98666C62339353D788197312F520ED5E09D94B318ABBD6C07D68A3E65BC0C1A84AC9AA112049A0F36BE32ED4E60AC4]"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Hash algorithm SHA3_384 is not implemented")
    public void testUseTreeBuilderUsingNotImplementedAlgorithm() throws KSIException {
        BlindingMaskLinkingHashTreeBuilder builder = new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA3_384, new byte[HashAlgorithm.SHA3_512.getLength()], DATA_HASH_2);
        addAndBuild(builder, new byte[HashAlgorithm.SHA3_384.getLength()]);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Hash algorithm SHA1 is marked deprecated since.*2016.*")
    public void testUseTreeBuilderUsingNotTrustedAlgorithm() throws KSIException {
        BlindingMaskLinkingHashTreeBuilder builder = new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA1, new byte[HashAlgorithm.SHA3_512.getLength()], DATA_HASH_2);
        addAndBuild(builder, new byte[HashAlgorithm.SHA1.getLength()]);
    }

    @Test
    public void testUseTreeBuilderUsingInitializationVectorWithLongLength() throws KSIException {
        BlindingMaskLinkingHashTreeBuilder builder = new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA2_256, new byte[200], DATA_HASH_2);
        addAndBuild(builder, Base16.decode("018D80F90876630372C2537FEE55C1E7375B5133F3CD9CCEBA6C140FAA2F532B91"));
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Node can not be null")
    public void testAddNullNodeToHashBuilder() throws KSIException {
        BlindingMaskLinkingHashTreeBuilder builder = createTreeBuilder();
        builder.add((ImprintNode) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "ImprintNode with level greater than 0 is not supported by BlindingMaskLinkingHashTreeBuilder")
    public void testAddNodeWithLevelTwoToHashBuilder() throws KSIException {
        BlindingMaskLinkingHashTreeBuilder builder = createTreeBuilder();
        builder.add(new ImprintNode(DATA_HASH_2, 2));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = "Identity metadata is not supported by BlindingMaskLinkingHashTreeBuilder")
    public void testAddNodeWithMetadataToHashBuilder() {
        BlindingMaskLinkingHashTreeBuilder builder = createTreeBuilder();
        builder.add(new ImprintNode(DATA_HASH_2), new IdentityMetadata("ClientId"));
    }

    @Test
    public void testUseCustomTreeBuilder() throws KSIException {
        HashTreeBuilder treeBuilder = new HashTreeBuilder();
        BlindingMaskLinkingHashTreeBuilder blindingMaskLinkingHashTreeBuilder =
            new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA2_256, INITIALIZATION_VECTOR, DATA_HASH, treeBuilder);
        blindingMaskLinkingHashTreeBuilder.add(new ImprintNode(DATA_HASH_2));

        Assert.assertEquals(blindingMaskLinkingHashTreeBuilder.build(), treeBuilder.build());
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Tree builder can not be null")
    public void testCustomTreeBuilderIsNull() {
        new BlindingMaskLinkingHashTreeBuilder(HashAlgorithm.SHA2_256, INITIALIZATION_VECTOR, DATA_HASH, null);
    }

    private BlindingMaskLinkingHashTreeBuilder createTreeBuilder() {
        return new BlindingMaskLinkingHashTreeBuilder(INITIALIZATION_VECTOR);
    }

    private void addAndBuild(BlindingMaskLinkingHashTreeBuilder builder, byte[] expectedRootHash) throws KSIException {
        for (int i = 0; i < 4; i++) {
            builder.add(new ImprintNode(DATA_HASH_2));
        }
        ImprintNode root = builder.build();
        Assert.assertEquals(root.getLevel(), 3L, "Root level does not match with expected level.");
        Assert.assertEquals(root.getValue(), expectedRootHash, "Root hash does not match with expected hash.");
    }
}
