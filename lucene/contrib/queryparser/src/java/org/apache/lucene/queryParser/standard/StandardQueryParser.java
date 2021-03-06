package org.apache.lucene.queryParser.standard;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.text.Collator;
import java.util.Locale;
import java.util.Map;
import java.util.TooManyListenersException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.QueryParserHelper;
import org.apache.lucene.queryParser.core.config.QueryConfigHandler;
import org.apache.lucene.queryParser.standard.builders.StandardQueryTreeBuilder;
import org.apache.lucene.queryParser.standard.config.AllowLeadingWildcardAttribute;
import org.apache.lucene.queryParser.standard.config.AnalyzerAttribute;
import org.apache.lucene.queryParser.standard.config.DateResolutionAttribute;
import org.apache.lucene.queryParser.standard.config.DefaultOperatorAttribute;
import org.apache.lucene.queryParser.standard.config.DefaultPhraseSlopAttribute;
import org.apache.lucene.queryParser.standard.config.FieldBoostMapAttribute;
import org.apache.lucene.queryParser.standard.config.FieldDateResolutionMapAttribute;
import org.apache.lucene.queryParser.standard.config.FuzzyAttribute;
import org.apache.lucene.queryParser.standard.config.FuzzyConfig;
import org.apache.lucene.queryParser.standard.config.LocaleAttribute;
import org.apache.lucene.queryParser.standard.config.LowercaseExpandedTermsAttribute;
import org.apache.lucene.queryParser.standard.config.MultiFieldAttribute;
import org.apache.lucene.queryParser.standard.config.MultiTermRewriteMethodAttribute;
import org.apache.lucene.queryParser.standard.config.NumericConfig;
import org.apache.lucene.queryParser.standard.config.PositionIncrementsAttribute;
import org.apache.lucene.queryParser.standard.config.RangeCollatorAttribute;
import org.apache.lucene.queryParser.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.queryParser.standard.config.StandardQueryConfigHandler.Operator;
import org.apache.lucene.queryParser.standard.config.StandardQueryConfigHandler.ConfigurationKeys;
import org.apache.lucene.queryParser.standard.nodes.RangeQueryNode;
import org.apache.lucene.queryParser.standard.parser.StandardSyntaxParser;
import org.apache.lucene.queryParser.standard.processors.StandardQueryNodeProcessorPipeline;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;

/**
 * <p>
 * This class is a helper that enables users to easily use the Lucene query
 * parser.
 * </p>
 * <p>
 * To construct a Query object from a query string, use the
 * {@link #parse(String, String)} method:
 * <ul>
 * StandardQueryParser queryParserHelper = new StandardQueryParser(); <br/>
 * Query query = queryParserHelper.parse("a AND b", "defaultField");
 * </ul>
 * <p>
 * To change any configuration before parsing the query string do, for example:
 * <p/>
 * <ul>
 * // the query config handler returned by {@link StandardQueryParser} is a
 * {@link StandardQueryConfigHandler} <br/>
 * queryParserHelper.getQueryConfigHandler().setAnalyzer(new
 * WhitespaceAnalyzer());
 * </ul>
 * <p>
 * The syntax for query strings is as follows (copied from the old QueryParser
 * javadoc):
 * <ul>
 * A Query is a series of clauses. A clause may be prefixed by:
 * <ul>
 * <li>a plus (<code>+</code>) or a minus (<code>-</code>) sign, indicating that
 * the clause is required or prohibited respectively; or
 * <li>a term followed by a colon, indicating the field to be searched. This
 * enables one to construct queries which search multiple fields.
 * </ul>
 * 
 * A clause may be either:
 * <ul>
 * <li>a term, indicating all the documents that contain this term; or
 * <li>a nested query, enclosed in parentheses. Note that this may be used with
 * a <code>+</code>/<code>-</code> prefix to require any of a set of terms.
 * </ul>
 * 
 * Thus, in BNF, the query grammar is:
 * 
 * <pre>
 *   Query  ::= ( Clause )*
 *   Clause ::= [&quot;+&quot;, &quot;-&quot;] [&lt;TERM&gt; &quot;:&quot;] ( &lt;TERM&gt; | &quot;(&quot; Query &quot;)&quot; )
 * </pre>
 * 
 * <p>
 * Examples of appropriately formatted queries can be found in the <a
 * href="../../../../../../queryparsersyntax.html">query syntax
 * documentation</a>.
 * </p>
 * </ul>
 * <p>
 * The text parser used by this helper is a {@link StandardSyntaxParser}.
 * <p/>
 * <p>
 * The query node processor used by this helper is a
 * {@link StandardQueryNodeProcessorPipeline}.
 * <p/>
 * <p>
 * The builder used by this helper is a {@link StandardQueryTreeBuilder}.
 * <p/>
 * 
 * @see StandardQueryParser
 * @see StandardQueryConfigHandler
 * @see StandardSyntaxParser
 * @see StandardQueryNodeProcessorPipeline
 * @see StandardQueryTreeBuilder
 */
public class StandardQueryParser extends QueryParserHelper {

  /**
   * Constructs a {@link StandardQueryParser} object.
   */
  public StandardQueryParser() {
    super(new StandardQueryConfigHandler(), new StandardSyntaxParser(),
        new StandardQueryNodeProcessorPipeline(null),
        new StandardQueryTreeBuilder());
  }

  /**
   * Constructs a {@link StandardQueryParser} object and sets an
   * {@link Analyzer} to it. The same as:
   * 
   * <ul>
   * StandardQueryParser qp = new StandardQueryParser();
   * qp.getQueryConfigHandler().setAnalyzer(analyzer);
   * </ul>
   * 
   * @param analyzer the analyzer to be used by this query parser helper
   */
  public StandardQueryParser(Analyzer analyzer) {
    this();

    this.setAnalyzer(analyzer);
  }

  @Override
  public String toString() {
    return "<StandardQueryParser config=\"" + this.getQueryConfigHandler()
        + "\"/>";
  }

  /**
   * Overrides {@link QueryParserHelper#parse(String, String)} so it casts the
   * return object to {@link Query}. For more reference about this method, check
   * {@link QueryParserHelper#parse(String, String)}.
   * 
   * @param query the query string
   * @param defaultField the default field used by the text parser
   * 
   * @return the object built from the query
   * 
   * @throws QueryNodeException if something wrong happens along the three
   *         phases
   */
  @Override
  public Query parse(String query, String defaultField)
      throws QueryNodeException {

    return (Query) super.parse(query, defaultField);

  }

  /**
   * Gets implicit operator setting, which will be either {@link Operator#AND}
   * or {@link Operator#OR}.
   */
  public StandardQueryConfigHandler.Operator getDefaultOperator() {
    return getQueryConfigHandler().get(ConfigurationKeys.DEFAULT_OPERATOR);
  }

  /**
   * Sets the collator used to determine index term inclusion in ranges for
   * RangeQuerys.
   * <p/>
   * <strong>WARNING:</strong> Setting the rangeCollator to a non-null collator
   * using this method will cause every single index Term in the Field
   * referenced by lowerTerm and/or upperTerm to be examined. Depending on the
   * number of index Terms in this Field, the operation could be very slow.
   * 
   * @param collator the collator to use when constructing
   *        {@link RangeQueryNode}s
   */
  public void setRangeCollator(Collator collator) {
    RangeCollatorAttribute attr = getQueryConfigHandler().getAttribute(
        RangeCollatorAttribute.class);
    attr.setDateResolution(collator);

    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.RANGE_COLLATOR, collator);
  }

  /**
   * @return the collator used to determine index term inclusion in ranges for
   *         RangeQuerys.
   */
  public Collator getRangeCollator() {
    return getQueryConfigHandler().get(ConfigurationKeys.RANGE_COLLATOR);
  }

  /**
   * Sets the boolean operator of the QueryParser. In default mode (
   * {@link Operator#OR}) terms without any modifiers are considered optional:
   * for example <code>capital of Hungary</code> is equal to
   * <code>capital OR of OR Hungary</code>.<br/>
   * In {@link Operator#AND} mode terms are considered to be in conjunction: the
   * above mentioned query is parsed as <code>capital AND of AND Hungary</code>
   * 
   * @deprecated
   */
  @Deprecated
  public void setDefaultOperator(DefaultOperatorAttribute.Operator operator) {
    DefaultOperatorAttribute attr = getQueryConfigHandler().getAttribute(DefaultOperatorAttribute.class);
    attr.setOperator(operator);
  }

  /**
   * Sets the boolean operator of the QueryParser. In default mode (
   * {@link Operator#OR}) terms without any modifiers are considered optional:
   * for example <code>capital of Hungary</code> is equal to
   * <code>capital OR of OR Hungary</code>.<br/>
   * In {@link Operator#AND} mode terms are considered to be in conjunction: the
   * above mentioned query is parsed as <code>capital AND of AND Hungary</code>
   */
  public void setDefaultOperator(
      org.apache.lucene.queryParser.standard.config.StandardQueryConfigHandler.Operator operator) {
    
    DefaultOperatorAttribute.Operator attrOperator;
    
    if (operator == org.apache.lucene.queryParser.standard.config.StandardQueryConfigHandler.Operator.AND) {
      attrOperator = DefaultOperatorAttribute.Operator.AND;
    } else {
      attrOperator = DefaultOperatorAttribute.Operator.OR;
    }
    
    setDefaultOperator(attrOperator);
    
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.DEFAULT_OPERATOR, operator);
  }

  /**
   * Set to <code>true</code> to allow leading wildcard characters.
   * <p>
   * When set, <code>*</code> or <code>?</code> are allowed as the first
   * character of a PrefixQuery and WildcardQuery. Note that this can produce
   * very slow queries on big indexes.
   * <p>
   * Default: false.
   */
  public void setLowercaseExpandedTerms(boolean lowercaseExpandedTerms) {
    LowercaseExpandedTermsAttribute attr = getQueryConfigHandler()
        .getAttribute(LowercaseExpandedTermsAttribute.class);
    attr.setLowercaseExpandedTerms(lowercaseExpandedTerms);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.LOWERCASE_EXPANDED_TERMS,
    // lowercaseExpandedTerms);
  }

  /**
   * @see #setLowercaseExpandedTerms(boolean)
   */
  public boolean getLowercaseExpandedTerms() {
    Boolean lowercaseExpandedTerms = getQueryConfigHandler().get(
        ConfigurationKeys.LOWERCASE_EXPANDED_TERMS);

    if (lowercaseExpandedTerms == null) {
      return true;

    } else {
      return lowercaseExpandedTerms;
    }

  }

  /**
   * Set to <code>true</code> to allow leading wildcard characters.
   * <p>
   * When set, <code>*</code> or <code>?</code> are allowed as the first
   * character of a PrefixQuery and WildcardQuery. Note that this can produce
   * very slow queries on big indexes.
   * <p>
   * Default: false.
   */
  public void setAllowLeadingWildcard(boolean allowLeadingWildcard) {
    AllowLeadingWildcardAttribute attr = getQueryConfigHandler().getAttribute(
        AllowLeadingWildcardAttribute.class);
    attr.setAllowLeadingWildcard(allowLeadingWildcard);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.ALLOW_LEADING_WILDCARD,
    // allowLeadingWildcard);
  }

  /**
   * Set to <code>true</code> to enable position increments in result query.
   * <p>
   * When set, result phrase and multi-phrase queries will be aware of position
   * increments. Useful when e.g. a StopFilter increases the position increment
   * of the token that follows an omitted token.
   * <p>
   * Default: false.
   */
  public void setEnablePositionIncrements(boolean enabled) {
    PositionIncrementsAttribute attr = getQueryConfigHandler().getAttribute(
        PositionIncrementsAttribute.class);
    attr.setPositionIncrementsEnabled(enabled);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.ENABLE_POSITION_INCREMENTS,
    // enabled);
  }

  /**
   * @see #setEnablePositionIncrements(boolean)
   */
  public boolean getEnablePositionIncrements() {
    Boolean enablePositionsIncrements = getQueryConfigHandler().get(
        ConfigurationKeys.ENABLE_POSITION_INCREMENTS);

    if (enablePositionsIncrements == null) {
      return false;

    } else {
      return enablePositionsIncrements;
    }

  }

  /**
   * By default, it uses
   * {@link MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT} when creating a
   * prefix, wildcard and range queries. This implementation is generally
   * preferable because it a) Runs faster b) Does not have the scarcity of terms
   * unduly influence score c) avoids any {@link TooManyListenersException}
   * exception. However, if your application really needs to use the
   * old-fashioned boolean queries expansion rewriting and the above points are
   * not relevant then use this change the rewrite method.
   */
  public void setMultiTermRewriteMethod(MultiTermQuery.RewriteMethod method) {
    MultiTermRewriteMethodAttribute attr = getQueryConfigHandler()
        .getAttribute(MultiTermRewriteMethodAttribute.class);
    attr.setMultiTermRewriteMethod(method);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.MULTI_TERM_REWRITE_METHOD,
    // method);
  }

  /**
   * @see #setMultiTermRewriteMethod(org.apache.lucene.search.MultiTermQuery.RewriteMethod)
   */
  public MultiTermQuery.RewriteMethod getMultiTermRewriteMethod() {
    return getQueryConfigHandler().get(
        ConfigurationKeys.MULTI_TERM_REWRITE_METHOD);
  }

  /**
   * Set the fields a query should be expanded to when the field is
   * <code>null</code>
   * 
   * @param fields the fields used to expand the query
   */
  public void setMultiFields(CharSequence[] fields) {

    if (fields == null) {
      fields = new CharSequence[0];
    }

    MultiFieldAttribute attr = getQueryConfigHandler().addAttribute(
        MultiFieldAttribute.class);
    attr.setFields(fields);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.MULTI_FIELDS, fields);

  }

  /**
   * Returns the fields used to expand the query when the field for a certain
   * query is <code>null</code>
   * 
   * @param fields the fields used to expand the query
   */
  public void getMultiFields(CharSequence[] fields) {
    getQueryConfigHandler().get(ConfigurationKeys.MULTI_FIELDS);
  }

  /**
   * Set the prefix length for fuzzy queries. Default is 0.
   * 
   * @param fuzzyPrefixLength The fuzzyPrefixLength to set.
   */
  public void setFuzzyPrefixLength(int fuzzyPrefixLength) {
    FuzzyAttribute attr = getQueryConfigHandler().addAttribute(
        FuzzyAttribute.class);
    attr.setPrefixLength(fuzzyPrefixLength);

    // uncomment code below when deprecated query parser attributes are removed
    /*
     * QueryConfigHandler config = getQueryConfigHandler(); FuzzyConfig
     * fuzzyConfig = config.get(ConfigurationKeys.FUZZY_CONFIG);
     * 
     * if (fuzzyConfig == null) { fuzzyConfig = new FuzzyConfig();
     * config.set(ConfigurationKeys.FUZZY_CONFIG, fuzzyConfig); }
     * 
     * fuzzyConfig.setPrefixLength(fuzzyPrefixLength);
     */
  }

  public void setNumericConfigMap(Map<String, NumericConfig> numericConfigMap) {
    getQueryConfigHandler().set(ConfigurationKeys.NUMERIC_CONFIG_MAP,
        numericConfigMap);
  }

  public Map<String, NumericConfig> getNumericConfigMap() {
    return getQueryConfigHandler().get(ConfigurationKeys.NUMERIC_CONFIG_MAP);
  }

  /**
   * Set locale used by date range parsing.
   */
  public void setLocale(Locale locale) {
    LocaleAttribute attr = getQueryConfigHandler().addAttribute(
        LocaleAttribute.class);
    attr.setLocale(locale);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.LOCALE, locale);
  }

  /**
   * Returns current locale, allowing access by subclasses.
   */
  public Locale getLocale() {
    return getQueryConfigHandler().get(ConfigurationKeys.LOCALE);
  }

  /**
   * Sets the default slop for phrases. If zero, then exact phrase matches are
   * required. Default value is zero.
   * 
   * @deprecated renamed to {@link #setPhraseSlop(int)}
   */
  @Deprecated
  public void setDefaultPhraseSlop(int defaultPhraseSlop) {
    setPhraseSlop(defaultPhraseSlop);
  }

  /**
   * Sets the default slop for phrases. If zero, then exact phrase matches are
   * required. Default value is zero.
   */
  public void setPhraseSlop(int defaultPhraseSlop) {
    DefaultPhraseSlopAttribute attr = getQueryConfigHandler().addAttribute(
        DefaultPhraseSlopAttribute.class);
    attr.setDefaultPhraseSlop(defaultPhraseSlop);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.PHRASE_SLOP,
    // defaultPhraseSlop);
  }

  public void setAnalyzer(Analyzer analyzer) {
    AnalyzerAttribute attr = getQueryConfigHandler().getAttribute(
        AnalyzerAttribute.class);
    attr.setAnalyzer(analyzer);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.ANALYZER, analyzer);
  }

  public Analyzer getAnalyzer() {
    return getQueryConfigHandler().get(ConfigurationKeys.ANALYZER);
  }

  /**
   * @see #setAllowLeadingWildcard(boolean)
   */
  public boolean getAllowLeadingWildcard() {
    Boolean allowLeadingWildcard = getQueryConfigHandler().get(
        ConfigurationKeys.ALLOW_LEADING_WILDCARD);

    if (allowLeadingWildcard == null) {
      return false;

    } else {
      return allowLeadingWildcard;
    }
  }

  /**
   * Get the minimal similarity for fuzzy queries.
   */
  public float getFuzzyMinSim() {
    FuzzyConfig fuzzyConfig = getQueryConfigHandler().get(
        ConfigurationKeys.FUZZY_CONFIG);

    if (fuzzyConfig == null) {
      return FuzzyQuery.defaultMinSimilarity;
    } else {
      return fuzzyConfig.getMinSimilarity();
    }
  }

  /**
   * Get the prefix length for fuzzy queries.
   * 
   * @return Returns the fuzzyPrefixLength.
   */
  public int getFuzzyPrefixLength() {
    FuzzyConfig fuzzyConfig = getQueryConfigHandler().get(
        ConfigurationKeys.FUZZY_CONFIG);

    if (fuzzyConfig == null) {
      return FuzzyQuery.defaultPrefixLength;
    } else {
      return fuzzyConfig.getPrefixLength();
    }
  }

  /**
   * Gets the default slop for phrases.
   */
  public int getPhraseSlop() {
    Integer phraseSlop = getQueryConfigHandler().get(
        ConfigurationKeys.PHRASE_SLOP);

    if (phraseSlop == null) {
      return 0;

    } else {
      return phraseSlop;
    }
  }

  /**
   * Set the minimum similarity for fuzzy queries. Default is defined on
   * {@link FuzzyQuery#defaultMinSimilarity}.
   */
  public void setFuzzyMinSim(float fuzzyMinSim) {
    FuzzyAttribute attr = getQueryConfigHandler().addAttribute(
        FuzzyAttribute.class);
    attr.setFuzzyMinSimilarity(fuzzyMinSim);
    // uncomment code below when deprecated query parser attributes are removed
    /*
     * QueryConfigHandler config = getQueryConfigHandler(); FuzzyConfig
     * fuzzyConfig = config.get(ConfigurationKeys.FUZZY_CONFIG);
     * 
     * if (fuzzyConfig == null) { fuzzyConfig = new FuzzyConfig();
     * config.set(ConfigurationKeys.FUZZY_CONFIG, fuzzyConfig); }
     * 
     * fuzzyConfig.setMinSimilarity(fuzzyMinSim);
     */
  }

  /**
   * Sets the boost used for each field.
   * 
   * @param boosts a collection that maps a field to its boost
   */
  public void setFieldsBoost(Map<String, Float> boosts) {
    FieldBoostMapAttribute attr = getQueryConfigHandler().addAttribute(
        FieldBoostMapAttribute.class);
    attr.setFieldBoostMap(boosts);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.FIELD_BOOST_MAP, boosts);
  }

  /**
   * Returns the field to boost map used to set boost for each field.
   * 
   * @return the field to boost map
   */
  public Map<String, Float> getFieldsBoost() {
    return getQueryConfigHandler().get(ConfigurationKeys.FIELD_BOOST_MAP);
  }

  /**
   * Sets the default {@link Resolution} used for certain field when no
   * {@link Resolution} is defined for this field.
   * 
   * @param dateResolution the default {@link Resolution}
   */
  public void setDateResolution(DateTools.Resolution dateResolution) {
    DateResolutionAttribute attr = getQueryConfigHandler().addAttribute(
        DateResolutionAttribute.class);
    attr.setDateResolution(dateResolution);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.DATE_RESOLUTION,
    // dateResolution);
  }

  /**
   * Returns the default {@link Resolution} used for certain field when no
   * {@link Resolution} is defined for this field.
   * 
   * @return the default {@link Resolution}
   */
  public DateTools.Resolution getDateResolution() {
    return getQueryConfigHandler().get(ConfigurationKeys.DATE_RESOLUTION);
  }

  /**
   * Sets the {@link Resolution} used for each field
   * 
   * @param dateRes a collection that maps a field to its {@link Resolution}
   * 
   * @deprecated this method was renamed to {@link #setDateResolutionMap(Map)}
   */
  @Deprecated
  public void setDateResolution(Map<CharSequence, DateTools.Resolution> dateRes) {
    setDateResolutionMap(dateRes);
  }

  /**
   * Returns the field to {@link Resolution} map used to normalize each date
   * field.
   * 
   * @return the field to {@link Resolution} map
   */
  public Map<CharSequence, DateTools.Resolution> getDateResolutionMap() {
    return getQueryConfigHandler().get(
        ConfigurationKeys.FIELD_DATE_RESOLUTION_MAP);
  }

  /**
   * Sets the {@link Resolution} used for each field
   * 
   * @param dateRes a collection that maps a field to its {@link Resolution}
   */
  public void setDateResolutionMap(
      Map<CharSequence, DateTools.Resolution> dateRes) {
    FieldDateResolutionMapAttribute attr = getQueryConfigHandler()
        .addAttribute(FieldDateResolutionMapAttribute.class);
    attr.setFieldDateResolutionMap(dateRes);
    // uncomment code below when deprecated query parser attributes are removed
    // getQueryConfigHandler().set(ConfigurationKeys.FIELD_DATE_RESOLUTION_MAP,
    // dateRes);
  }

}
