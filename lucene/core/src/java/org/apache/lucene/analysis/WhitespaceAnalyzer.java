package org.apache.lucene.analysis;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * An Analyzer that uses {@link WhitespaceTokenizer}.
 * <p>
 * <a name="version">You must specify the required {@link Version} compatibility
 * when creating {@link CharTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link WhitespaceTokenizer} uses an int based API to normalize and
 * detect token codepoints. See {@link CharTokenizer#isTokenChar(int)} and
 * {@link CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 * <p>
 **/
public final class WhitespaceAnalyzer extends ReusableAnalyzerBase {

    private final Version matchVersion;

    /**
     * Creates a new {@link WhitespaceAnalyzer}
     * @param matchVersion Lucene version to match See {@link <a href="#version">above</a>}
     */
    public WhitespaceAnalyzer(Version matchVersion) {
        this.matchVersion = matchVersion;
    }

    /**
     * Creates a new {@link WhitespaceAnalyzer}
     * @deprecated use {@link #WhitespaceAnalyzer(Version)} instead
     */
    @Deprecated
    public WhitespaceAnalyzer() {
        this(Version.LUCENE_30);
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName,
                                                     final Reader reader) {
        return new TokenStreamComponents(new WhitespaceTokenizer(matchVersion, reader));
    }
}
