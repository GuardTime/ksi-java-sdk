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

import com.guardtime.ksi.util.Base16;
import com.guardtime.ksi.util.Util;

import java.util.Arrays;

/**
 * Representation of hash values as hash computation results. Includes name of the algorithm used and computed hash
 * value.
 */
public class DataHash {

    private byte[] imprint;
    private HashAlgorithm algorithm;
    private byte[] value;

    /**
     * Constructor which initializes the DataHash.
     *
     * @param algorithm
     *         {@link HashAlgorithm} used to compute this hash.
     * @param value
     *         hash value computed for the input data.
     *
     * @throws NullPointerException when one of input parameters is null.
     * @throws IllegalArgumentException
     *         when hash size does not match algorithm size.
     */
    public DataHash(HashAlgorithm algorithm, byte[] value) {
        Util.notNull(algorithm, "Hash algorithm");
        Util.notNull(value, "Hash value");
        if (value.length != algorithm.getLength()) {
            throw new IllegalArgumentException(
                    "Hash size(" + value.length + ") does not match "
                            + algorithm.getName() + " size(" + algorithm.getLength() + ")"
            );
        }
        this.algorithm = algorithm;
        this.value = value;
        this.imprint = Util.join(new byte[]{(byte) algorithm.getId()}, value);
    }

    /**
     * Constructor which initializes the DataHash.
     *
     * @param hashImprint
     *         Hash imprint.
     *
     * @throws NullPointerException when input parameter is null.
     * @throws IllegalArgumentException
     *         when hash imprint is not in correct format.
     */
    public DataHash(byte[] hashImprint) {
        Util.notNull(hashImprint, "Hash imprint");
        if (hashImprint.length < 1) {
            throw new IllegalArgumentException("Hash imprint too short");
        }

        this.algorithm = HashAlgorithm.getById(hashImprint[0]);

        if (this.algorithm.getLength() + 1 != hashImprint.length) {
            throw new IllegalArgumentException(
                    "Hash size(" + (hashImprint.length - 1) + ") does not match "
                            + algorithm.getName() + " size(" + algorithm.getLength() + ")"
            );
        }

        this.value = Util.copyOf(hashImprint, 1, hashImprint.length - 1);
        this.imprint = hashImprint;
    }

    /**
     * Checks if the input byte array can be converted to the {@link DataHash} object.
     *
     * @param imprint byte array to be checked.
     *
     * @return True, if the input byte array can be converted to the {@link DataHash} object.
     *
     * @throws NullPointerException when input is null
     */
    public static boolean isDataHash(byte[] imprint) {
        Util.notNull(imprint, "Hash imprint");
        if (imprint.length < 1) {
            return false;
        }
        if (!HashAlgorithm.isHashAlgorithmId(imprint[0])) {
            return false;
        }
        HashAlgorithm algorithm = HashAlgorithm.getById(imprint[0]);
        return algorithm.getLength() + 1 == imprint.length;
    }

    /**
     * Gets the HashAlgorithm used to compute this DataHash.
     *
     * @return HashAlgorithm used.
     */

    public final HashAlgorithm getAlgorithm() {
        return algorithm;
    }


    /**
     * Gets data imprint.
     * <p>
     * Imprint is created by concatenating hash algorithm ID with hash value.</p>
     *
     * @return Imprint bytes.
     */
    public final byte[] getImprint() {
        return imprint;
    }


    /**
     * Gets the computed hash value for DataHash.
     *
     * @return Computed hash value.
     */
    public final byte[] getValue() {
        return value;
    }


    /**
     * Gets the hash code of current object.
     *
     * @return Hash code of current object.
     */
    @Override
    public final int hashCode() {
        return Arrays.hashCode(this.imprint) + Arrays.hashCode(this.value) + this.algorithm.hashCode();
    }


    /**
     * Checks if object is equal to current DataHash.
     *
     * @param object object to be checked.
     *
     * @return True, if the object and the DataHash are equal.
     */
    @Override
    public final boolean equals(Object object) {
        if (object instanceof DataHash) {
            DataHash b = (DataHash) object;

            /*
                No need to check if algorithm is null because constructor will do it for us
             */
            return Arrays.equals(b.imprint, this.imprint)
                    && Arrays.equals(b.value, this.value)
                    && b.algorithm.equals(this.algorithm);
        }

        return false;
    }


    /**
     * Get DataHash as a string including the algorithm name and computed hash value.
     *
     * @return String representing algorithm name and value.
     */
    @Override
    public final String toString() {
        return algorithm.getName() + ":[" + Base16.encode(value) + "]";
    }
}
