/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.handler.dataimport;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.solr.handler.dataimport.DataImportHandlerException.SEVERE;
import static org.apache.solr.handler.dataimport.DataImportHandlerException.wrapAndThrow;

/**
 * <p>
 * An implementation of {@link EntityProcessor} which fetches values from a
 * separate Solr implementation using the SolrJ client library. Yield a row per
 * Solr document.
 * </p>
 * <p>
 * Limitations: 
 * All configuration is evaluated at the beginning;
 * Only one query is walked;
 * </p>
 */
public class SolrEntityProcessor extends EntityProcessorBase {
  
  private static final Logger LOG = LoggerFactory.getLogger(SolrEntityProcessor.class);
  
  public static final String SOLR_SERVER = "url";
  public static final String QUERY = "query";
  public static final String TIMEOUT = "timeout";

  public static final int TIMEOUT_SECS = 5 * 60; // 5 minutes
  public static final int ROWS_DEFAULT = 50;
  
  private SolrServer solrServer = null;
  private String queryString;
  private int rows = ROWS_DEFAULT;
  private String[] filterQueries;
  private String[] fields;
  private String queryType;
  private int timeout = TIMEOUT_SECS;
  
  private boolean initDone = false;

  /**
   * Factory method that returns a {@link HttpClient} instance used for interfacing with a source Solr service.
   * One can override this method to return a differently configured {@link HttpClient} instance.
   * For example configure https and http authentication.
   *
   * @return a {@link HttpClient} instance used for interfacing with a source Solr service
   */
  protected HttpClient getHttpClient() {
    return new HttpClient(new MultiThreadedHttpConnectionManager());
  }

  @Override
  protected void firstInit(Context context) {
    super.firstInit(context);
    
    try {
      String serverPath = context.getResolvedEntityAttribute(SOLR_SERVER);
      if (serverPath == null) {
        throw new DataImportHandlerException(DataImportHandlerException.SEVERE,
            "SolrEntityProcessor: parameter 'url' is required");
      }

      HttpClient client = getHttpClient();
      URL url = new URL(serverPath);
      // (wt="javabin|xml") default is javabin
      if ("xml".equals(context.getResolvedEntityAttribute(CommonParams.WT))) {
        solrServer = new CommonsHttpSolrServer(url, client, new XMLResponseParser(), false);
        LOG.info("using XMLResponseParser");
      } else {
        solrServer = new CommonsHttpSolrServer(url, client);
        LOG.info("using BinaryResponseParser");
      }
    } catch (MalformedURLException e) {
      throw new DataImportHandlerException(DataImportHandlerException.SEVERE, e);
    }

    this.queryString = context.getResolvedEntityAttribute(QUERY);
    if (this.queryString == null) {
      throw new DataImportHandlerException(
          DataImportHandlerException.SEVERE,
          "SolrEntityProcessor: parameter 'query' is required"
      );
    }
    
    String rowsP = context.getResolvedEntityAttribute(CommonParams.ROWS);
    if (rowsP != null) {
      rows = Integer.parseInt(rowsP);
    }
    
    String fqAsString = context.getResolvedEntityAttribute(CommonParams.FQ);
    if (fqAsString != null) {
      this.filterQueries = fqAsString.split(",");
    }
    
    String fieldsAsString = context.getResolvedEntityAttribute(CommonParams.FL);
    if (fieldsAsString != null) {
      this.fields = fieldsAsString.split(",");
    }
    this.queryType = context.getResolvedEntityAttribute(CommonParams.QT);
    String timeoutAsString = context.getResolvedEntityAttribute(TIMEOUT);
    if (timeoutAsString != null) {
      this.timeout = Integer.parseInt(timeoutAsString);
    }
  }
  
  @Override
  public Map<String,Object> nextRow() {
    buildIterator();
    return getNext();
  }
  
  /**
   * The following method changes the rowIterator mutable field. It requires
   * external synchronization. In fact when used in a multi-threaded setup the nextRow() method is called from a
   * synchronized block {@link ThreadedEntityProcessorWrapper#nextRow()}, so this
   * is taken care of.
   */
  private void buildIterator() {
    if (rowIterator == null) {
      // We could use an AtomicBoolean but there's no need since this method
      // would require anyway external synchronization
      if (!initDone) {
        initDone = true;
        SolrDocumentList solrDocumentList = doQuery(0);
        if (solrDocumentList != null) {
          rowIterator = new SolrDocumentListIterator(solrDocumentList);
        }
      }
      return;
    }
    
    SolrDocumentListIterator documentListIterator = (SolrDocumentListIterator) rowIterator;
    if (!documentListIterator.hasNext() && documentListIterator.hasMoreRows()) {
      SolrDocumentList solrDocumentList = doQuery(documentListIterator
          .getStart() + documentListIterator.getSize());
      if (solrDocumentList != null) {
        rowIterator = new SolrDocumentListIterator(solrDocumentList);
      }
    }
    
  }
  
  protected SolrDocumentList doQuery(int start) {
    SolrQuery solrQuery = new SolrQuery(queryString);
    solrQuery.setRows(rows);
    solrQuery.setStart(start);
    if (fields != null) {
      for (String field : fields) {
        solrQuery.addField(field);
      }
    }
    solrQuery.setQueryType(queryType);
    solrQuery.setFilterQueries(filterQueries);
    solrQuery.setTimeAllowed(timeout * 1000);
    
    QueryResponse response = null;
    try {
      response = solrServer.query(solrQuery);
    } catch (SolrServerException e) {
      if (ABORT.equals(onError)) {
        wrapAndThrow(SEVERE, e);
      } else if (SKIP.equals(onError)) {
        wrapAndThrow(DataImportHandlerException.SKIP_ROW, e);
      }
    }
    
    return response == null ? null : response.getResults();
  }
  
  private static class SolrDocumentListIterator implements Iterator<Map<String,Object>> {
    
    private final int start;
    private final int size;
    private final long numFound;
    private final Iterator<SolrDocument> solrDocumentIterator;
    
    public SolrDocumentListIterator(SolrDocumentList solrDocumentList) {
      this.solrDocumentIterator = solrDocumentList.iterator();
      this.numFound = solrDocumentList.getNumFound();
      // SolrQuery has the start field of type int while SolrDocumentList of
      // type long. We are always querying with an int so we can't receive a
      // long as output. That's the reason why the following cast seems safe
      this.start = (int) solrDocumentList.getStart();
      this.size = solrDocumentList.size();
    }
    
    public boolean hasNext() {
      return solrDocumentIterator.hasNext();
    }
    
    public Map<String,Object> next() {
      SolrDocument solrDocument = solrDocumentIterator.next();
      
      HashMap<String,Object> map = new HashMap<String,Object>();
      Collection<String> fields = solrDocument.getFieldNames();
      for (String field : fields) {
        Object fieldValue = solrDocument.getFieldValue(field);
        map.put(field, fieldValue);
      }
      return map;
    }
    
    public int getStart() {
      return start;
    }
    
    public int getSize() {
      return size;
    }
    
    public boolean hasMoreRows() {
      return numFound > start + size;
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  
}
