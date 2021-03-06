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
package com.guardtime.ksi.hashing;

import com.guardtime.ksi.hashing.HashAlgorithm.Status;
import com.guardtime.ksi.util.Base16;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.guardtime.ksi.CommonTestUtil.loadFile;

public class DataHasherTest {

    private byte[] testData = null;

    @BeforeClass
    public void setUp() throws Exception {
        testData = new String("LongString1ReallyLongString2EvenLongerString3").getBytes("UTF-8");
    }

    @Test(dataProvider = "notImplementedAlgorithms", expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Hash algorithm .* is not implemented")
    public void testNotImplementedAlgorithms_throwsHashAlgorithmNotImplementedException(HashAlgorithm algorithm) {
        new DataHasher(algorithm);
    }

    @Test(dataProvider = "workingAlgorithms")
    public void testWorkingAlgorithms(HashAlgorithm algorithm) {
        DataHasher hasher = new DataHasher(algorithm, false);
        Assert.assertNotNull(hasher);
    }

    @Test(dataProvider = "deprecatedAlgorithms", expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Hash algorithm .* is marked deprecated since .*")
    public void testDeprecatedAlgorithms(HashAlgorithm algorithm) {
        new DataHasher(algorithm, true);
    }

    @Test
    public void testDefaultAlgorithmNameAlternative() {
        HashAlgorithm alg = HashAlgorithm.getByName("dEfaulT");
        Assert.assertEquals(HashAlgorithm.SHA2_256.getName(), alg.getName());
    }

    @Test
    public void testSha2AlgorithmNameAlternative() {
        HashAlgorithm alg = HashAlgorithm.getByName("ShA2");
        Assert.assertEquals(HashAlgorithm.SHA2_256.getName(), alg.getName());
    }

    @Test
    public void testSha1AlgorithmStateTag() {
        HashAlgorithm alg = HashAlgorithm.getByName("SHA1");
        Assert.assertEquals(alg.getStatus(), Status.NOT_TRUSTED);
        Assert.assertEquals(alg.getDeprecatedSince(), new Date(1467331200000L));
        Assert.assertNull(alg.getObsoleteSince());
        Assert.assertTrue(alg.isDeprecated(alg.getDeprecatedSince()));
        Assert.assertTrue(alg.isDeprecated(new Date()));
        Assert.assertFalse(alg.isObsolete(new Date()));
    }

    @Test
    public void testSha1AlgorithmBeforeObsolete() {
        HashAlgorithm alg = HashAlgorithm.SHA1;
        Assert.assertEquals(alg.getStatus(), Status.NOT_TRUSTED);
        Assert.assertFalse(alg.isDeprecated(new Date(1467234000000L)/* 30.06.2016 */));
        Assert.assertFalse(alg.isObsolete(new Date()));
    }

    @Test
    public void testSha2AlgorithmStateTag() {
        HashAlgorithm alg = HashAlgorithm.getByName("ShA2");
        Assert.assertEquals(alg.getStatus(), HashAlgorithm.Status.NORMAL);
    }

    @Test
    public void testSm3AlgorithmStateTag() {
        HashAlgorithm alg = HashAlgorithm.getByName("SM3");
        Assert.assertEquals(alg.getStatus(), HashAlgorithm.Status.NOT_IMPLEMENTED);
    }

    @Test
    public void testHashGenerationWithoutAddingData() {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        DataHash dataHash = hasher.getHash();
        Assert.assertEquals(HashAlgorithm.SHA2_256.getLength(), dataHash.getValue().length);
        Assert.assertEquals(Base16.encode(dataHash.getValue()), "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855");
    }

    @Test
    public void testHashGenerationWithData() {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData(testData);
        DataHash dataHash = hasher.getHash();
        Assert.assertEquals(HashAlgorithm.SHA2_256.getLength(), dataHash.getValue().length);
        Assert.assertEquals(Base16.encode(dataHash.getValue()), "CF00FC3A72A2F71C7DE2B718C0A4DFF38D83C0E1957EC219C3B266F8CC38B9EA");
    }

    @Test
    public void testHashGenerationAddDataTwice() {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData(testData);
        hasher.addData(testData);
        DataHash dataHash = hasher.getHash();
        Assert.assertEquals(HashAlgorithm.SHA2_256.getLength(), dataHash.getValue().length);
        Assert.assertEquals(Base16.encode(dataHash.getValue()), "8D592513810A47A329E63AE19EE5FE9AF979783271009D4857D5A61B2F07FE5D");
    }

    @Test
    public void testHashGenerationAddFile() throws Exception {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData(loadFile("test.txt"));
        DataHash dataHash = hasher.getHash();
        Assert.assertEquals(HashAlgorithm.SHA2_256.getLength(), dataHash.getValue().length);
        Assert.assertEquals(Base16.encode(dataHash.getValue()), "CF00FC3A72A2F71C7DE2B718C0A4DFF38D83C0E1957EC219C3B266F8CC38B9EA");
    }

    @Test
    public void testGetHashMultipleTimes() {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData(testData);
        DataHash dataHash = hasher.getHash();
        Assert.assertEquals(HashAlgorithm.SHA2_256.getLength(), dataHash.getValue().length);
        Assert.assertEquals(Base16.encode(dataHash.getValue()), "CF00FC3A72A2F71C7DE2B718C0A4DFF38D83C0E1957EC219C3B266F8CC38B9EA");
        DataHash dataHash2 = hasher.getHash();
        Assert.assertEquals(dataHash.getValue().length, dataHash2.getValue().length);
        Assert.assertEquals(dataHash.getValue(), dataHash2.getValue());
    }

    @Test
    public void testAddDataAfterReset() {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData(testData);
        DataHash hash = hasher.getHash();
        hasher.reset();
        hasher.addData(testData);
        Assert.assertEquals(hash, hasher.getHash());
    }

    @Test
    public void testAddDataHash() {
        DataHash input = new DataHash(Base16.decode("0111A700B0C8066C47ECBA05ED37BC14DCADB238552D86C659342D1D7E87B8772D"));
        DataHash output = new DataHash(Base16.decode("013B28D96EAFE042AB948D5D78C43AB08C7D55EB935DF9DD62BEEDD3D17BA070D0"));
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData(input);
        Assert.assertEquals(hasher.getHash(), output);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Output hash has already been calculated")
    public void testAddDataAfterGettingResult() {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData(testData);
        hasher.getHash();
        hasher.addData(testData);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "File can not be null")
    public void testAddDataInvalidFileException() {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData((File) null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Input stream can not be null")
    public void testAddDataInvalidInputStreamException() {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData((InputStream) null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Date can not be null")
    public void testAddInvalidData_ThrowsIllegalArgumentException() {
        DataHasher hasher = new DataHasher(HashAlgorithm.SHA2_256);
        hasher.addData((byte[]) null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Hash algorithm can not be null")
    public void testAlgorithmIsNullException() {
        new DataHasher(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Hash algorithm SHA3_256 is not implemented")
    public void testSha3AlgorithmIsNotImplemented_ThrowsHashAlgorithmNotImplementedException() {
        new DataHasher(HashAlgorithm.SHA3_256);
    }

    @DataProvider(name = "notImplementedAlgorithms")
    public Object[][] notImplementedHashAlgorithms() {
        return getHashAlgorithmsByStatus(Status.NOT_IMPLEMENTED);
    }

    @DataProvider(name = "workingAlgorithms")
    public Object[][] workingHashAlgorithms() {
        return getHashAlgorithmsByStatus(Status.NORMAL, Status.NOT_TRUSTED);
    }

    @DataProvider(name = "deprecatedAlgorithms")
    public Object[][] deprecatedAlgorithms() {
        List<Object[]> objectsList = new ArrayList<Object[]>();
        Date currentDate = new Date();
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (algorithm.isDeprecated(currentDate)) {
                objectsList.add(new Object[]{algorithm});
            }
        }
        Object[][] objects = new Object[objectsList.size()][];
        return objectsList.toArray(objects);
    }

    private Object[][] getHashAlgorithmsByStatus(Status... allowedStatuses) {
        List<Object[]> objectsList = new ArrayList<>();
        List<Status> statusList = Arrays.asList(allowedStatuses);
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (statusList.contains(algorithm.getStatus())) {
                objectsList.add(new Object[]{algorithm});
            }
        }
        Object[][] objects = new Object[objectsList.size()][];
        return objectsList.toArray(objects);
    }

}
