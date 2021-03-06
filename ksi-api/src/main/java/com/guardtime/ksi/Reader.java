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

package com.guardtime.ksi;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.unisignature.KSISignature;

import java.io.File;
import java.io.InputStream;

/**
 * Parsing of KSI signatures. An instance of this class can be obtained using {@link SignatureReader} class.
 */
public interface Reader {

    /**
     * Creates {@link KSISignature} from input stream.
     *
     * @param input
     *         the {@link InputStream} to create KSI signature from, must not be null.
     * @return KSI signature ({@link KSISignature}).
     * @throws KSIException
     *         when error occurs (e.g. file contains invalid TLV structures).
     */
    KSISignature read(InputStream input) throws KSIException;

    /**
     * Converts byte array to {@link KSISignature}.
     *
     * @param bytes
     *         bytes to create KSI signature from, must not be null.
     * @return KSI signature ({@link KSISignature}).
     * @throws KSIException
     *         when error occurs (e.g. file contains invalid TLV structures).
     */
    KSISignature read(byte[] bytes) throws KSIException;

    /**
     * Creates {@link KSISignature} from file.
     *
     * @param file
     *         file to create the KSI signature from.
     * @return KSI signature ({@link KSISignature}).
     * @throws KSIException
     *         when error occurs (e.g. file contains invalid TLV structures).
     */
    KSISignature read(File file) throws KSIException;

}
