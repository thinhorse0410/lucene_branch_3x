package org.apache.lucene.search;

/**
 * Copyright 2004 The Apache Software Foundation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

/**
 * The Scorer for DisjunctionMaxQuery.  The union of all documents generated by the the subquery scorers
 * is generated in document number order.  The score for each document is the maximum of the scores computed
 * by the subquery scorers that generate that document, plus tieBreakerMultiplier times the sum of the scores
 * for the other subqueries that generate the document.
 */
class DisjunctionMaxScorer extends Scorer {

    /* The scorers for subqueries that have remaining docs, kept as a min heap by number of next doc. */
    private final Scorer[] subScorers;
    private int numScorers;
    /* Multiplier applied to non-maximum-scoring subqueries for a document as they are summed into the result. */
    private final float tieBreakerMultiplier;
    private int doc = -1;

    /* Used when scoring currently matching doc. */
    private float scoreSum;
    private float scoreMax;

    /**
     * Creates a new instance of DisjunctionMaxScorer
     *
     * @param weight
     *          The Weight to be used.
     * @param tieBreakerMultiplier
     *          Multiplier applied to non-maximum-scoring subqueries for a
     *          document as they are summed into the result.
     * @param similarity
     *          -- not used since our definition involves neither coord nor terms
     *          directly
     * @param subScorers
     *          The sub scorers this Scorer should iterate on
     * @param numScorers
     *          The actual number of scorers to iterate on. Note that the array's
     *          length may be larger than the actual number of scorers.
     */
    public DisjunctionMaxScorer(Weight weight, float tieBreakerMultiplier,
                                Similarity similarity, Scorer[] subScorers, int numScorers) throws IOException {
        super(similarity, weight);
        this.tieBreakerMultiplier = tieBreakerMultiplier;
        // The passed subScorers array includes only scorers which have documents
        // (DisjunctionMaxQuery takes care of that), and their nextDoc() was already
        // called.
        this.subScorers = subScorers;
        this.numScorers = numScorers;

        heapify();
    }

    @Override
    public int nextDoc() throws IOException {
        if (numScorers == 0) return doc = NO_MORE_DOCS;
        while (subScorers[0].docID() == doc) {
            if (subScorers[0].nextDoc() != NO_MORE_DOCS) {
                heapAdjust(0);
            } else {
                heapRemoveRoot();
                if (numScorers == 0) {
                    return doc = NO_MORE_DOCS;
                }
            }
        }

        return doc = subScorers[0].docID();
    }

    @Override
    public int docID() {
        return doc;
    }

    /** Determine the current document score.  Initially invalid, until {@link #nextDoc()} is called the first time.
     * @return the score of the current generated document
     */
    @Override
    public float score() throws IOException {
        int doc = subScorers[0].docID();
        scoreSum = scoreMax = subScorers[0].score();
        int size = numScorers;
        scoreAll(1, size, doc);
        scoreAll(2, size, doc);
        return scoreMax + (scoreSum - scoreMax) * tieBreakerMultiplier;
    }

    // Recursively iterate all subScorers that generated last doc computing sum and max
    private void scoreAll(int root, int size, int doc) throws IOException {
        if (root < size && subScorers[root].docID() == doc) {
            float sub = subScorers[root].score();
            scoreSum += sub;
            scoreMax = Math.max(scoreMax, sub);
            scoreAll((root << 1) + 1, size, doc);
            scoreAll((root << 1) + 2, size, doc);
        }
    }

    @Override
    public int advance(int target) throws IOException {
        if (numScorers == 0) return doc = NO_MORE_DOCS;
        while (subScorers[0].docID() < target) {
            if (subScorers[0].advance(target) != NO_MORE_DOCS) {
                heapAdjust(0);
            } else {
                heapRemoveRoot();
                if (numScorers == 0) {
                    return doc = NO_MORE_DOCS;
                }
            }
        }
        return doc = subScorers[0].docID();
    }

    // Organize subScorers into a min heap with scorers generating the earliest document on top.
    private void heapify() {
        for (int i = (numScorers >> 1) - 1; i >= 0; i--) {
            heapAdjust(i);
        }
    }

    /* The subtree of subScorers at root is a min heap except possibly for its root element.
     * Bubble the root down as required to make the subtree a heap.
     */
    private void heapAdjust(int root) {
        Scorer scorer = subScorers[root];
        int doc = scorer.docID();
        int i = root;
        while (i <= (numScorers >> 1) - 1) {
            int lchild = (i << 1) + 1;
            Scorer lscorer = subScorers[lchild];
            int ldoc = lscorer.docID();
            int rdoc = Integer.MAX_VALUE, rchild = (i << 1) + 2;
            Scorer rscorer = null;
            if (rchild < numScorers) {
                rscorer = subScorers[rchild];
                rdoc = rscorer.docID();
            }
            if (ldoc < doc) {
                if (rdoc < ldoc) {
                    subScorers[i] = rscorer;
                    subScorers[rchild] = scorer;
                    i = rchild;
                } else {
                    subScorers[i] = lscorer;
                    subScorers[lchild] = scorer;
                    i = lchild;
                }
            } else if (rdoc < doc) {
                subScorers[i] = rscorer;
                subScorers[rchild] = scorer;
                i = rchild;
            } else {
                return;
            }
        }
    }

    // Remove the root Scorer from subScorers and re-establish it as a heap
    private void heapRemoveRoot() {
        if (numScorers == 1) {
            subScorers[0] = null;
            numScorers = 0;
        } else {
            subScorers[0] = subScorers[numScorers - 1];
            subScorers[numScorers - 1] = null;
            --numScorers;
            heapAdjust(0);
        }
    }

}
