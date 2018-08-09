package org.apache.lucene.index;

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

import java.io.Closeable;
import java.io.IOException;

/** Abstract class for enumerating terms.

 <p>Term enumerations are always ordered by Term.compareTo().  Each term in
 the enumeration is greater than all that precede it.  */

public abstract class TermEnum implements Closeable {
    /** Increments the enumeration to the next element.  True if one exists.*/
    public abstract boolean next() throws IOException;

    /** Returns the current Term in the enumeration.*/
    public abstract Term term();

    /** Returns the docFreq of the current Term in the enumeration.*/
    public abstract int docFreq();

    /** Closes the enumeration to further activity, freeing resources. */
    public abstract void close() throws IOException;
}
