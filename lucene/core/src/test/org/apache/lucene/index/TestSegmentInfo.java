package org.apache.lucene.index;

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;

import java.io.IOException;
import java.util.Iterator;

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

public class TestSegmentInfo extends LuceneTestCase {

    public void testSizeInBytesCache() throws Exception {
        Directory dir = newDirectory();
        IndexWriterConfig conf = newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random)).setMergePolicy(newLogMergePolicy());
        IndexWriter writer = new IndexWriter(dir, conf);
        Document doc = new Document();
        doc.add(new Field("a", "value", Store.YES, Index.ANALYZED));
        writer.addDocument(doc);
        writer.close();

        SegmentInfos sis = new SegmentInfos();
        sis.read(dir);
        SegmentInfo si = sis.info(0);
        long sizeInBytesNoStore = si.sizeInBytes(false);
        long sizeInBytesWithStore = si.sizeInBytes(true);
        assertTrue("sizeInBytesNoStore=" + sizeInBytesNoStore + " sizeInBytesWithStore=" + sizeInBytesWithStore, sizeInBytesWithStore > sizeInBytesNoStore);
        dir.close();
    }

    // LUCENE-2584: calling files() by multiple threads could lead to ConcurrentModificationException
    public void testFilesConcurrency() throws Exception {
        Directory dir = newDirectory();
        // Create many files
        IndexWriterConfig conf = newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random));
        IndexWriter writer = new IndexWriter(dir, conf);
        Document doc = new Document();
        doc.add(new Field("a", "b", Store.YES, Index.ANALYZED, TermVector.YES));
        writer.addDocument(doc);
        writer.close();

        SegmentInfos sis = new SegmentInfos();
        sis.read(dir);
        final SegmentInfo si = sis.info(0);
        Thread[] threads = new Thread[_TestUtil.nextInt(random, 2, 5)];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        // Verify that files() does not throw an exception and that the
                        // iteration afterwards succeeds.
                        Iterator<String> iter = si.files().iterator();
                        while (iter.hasNext()) iter.next();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        dir.close();
    }

}
