/*
 * (C) Copyright IBM Corp. 2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.jdbc.util.type;

import static com.ibm.fhir.persistence.jdbc.JDBCConstants.AND;
import static com.ibm.fhir.persistence.jdbc.JDBCConstants.BIND_VAR;
import static com.ibm.fhir.persistence.jdbc.JDBCConstants.CODE;
import static com.ibm.fhir.persistence.jdbc.JDBCConstants.CODE_SYSTEM_ID;
import static com.ibm.fhir.persistence.jdbc.JDBCConstants.DOT;
import static com.ibm.fhir.persistence.jdbc.JDBCConstants.LEFT_PAREN;
import static com.ibm.fhir.persistence.jdbc.JDBCConstants.QUANTITY_VALUE;
import static com.ibm.fhir.persistence.jdbc.JDBCConstants.QUANTITY_VALUE_HIGH;
import static com.ibm.fhir.persistence.jdbc.JDBCConstants.QUANTITY_VALUE_LOW;
import static com.ibm.fhir.persistence.jdbc.JDBCConstants.RIGHT_PAREN;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.persistence.jdbc.JDBCConstants.JDBCOperator;
import com.ibm.fhir.persistence.jdbc.dao.api.ParameterDAO;
import com.ibm.fhir.persistence.jdbc.util.CodeSystemsCache;
import com.ibm.fhir.search.SearchConstants.Prefix;
import com.ibm.fhir.search.parameters.Parameter;
import com.ibm.fhir.search.parameters.ParameterValue;

/**
 * <a href="https://hl7.org/fhir/search.html#quantity>FHIR Specification: Search
 * - Quantity</a>
 * <br>
 * This utility encapsulates the logic specific to fhir-search related to
 * quantity.
 */
public class QuantityParmBehaviorUtil {

    public QuantityParmBehaviorUtil() {
        // No operation
    }

    public void executeBehavior(StringBuilder whereClauseSegment, Parameter queryParm, List<Object> bindVariables,
            String tableAlias, ParameterDAO parameterDao)
            throws Exception {
        // Start the Clause 
        // Query: AND ((
        whereClauseSegment.append(AND).append(LEFT_PAREN).append(LEFT_PAREN);

        // Process each parameter value in the query parameter
        boolean parmValueProcessed = false;
        Set<String> seen = new HashSet<>();
        for (ParameterValue value : queryParm.getValues()) {

            // Let's get the prefix. 
            Prefix prefix = value.getPrefix();
            if (prefix == null) {
                // Default to EQ
                prefix = Prefix.EQ;
            }

            // seen is used to optimize against a repeated value passed in. 
            // the hash must use the prefix and original values (reassembled). 
            String hash =
                    prefix.value() + value.getValueNumber() + '|' + value.getValueSystem() + '|' + value.getValueCode();
            if (!seen.contains(hash)) {
                seen.add(hash);

                // If multiple values are present, we need to OR them together.
                if (parmValueProcessed) {
                    // OR
                    whereClauseSegment.append(RIGHT_PAREN).append(JDBCOperator.OR.value()).append(LEFT_PAREN);
                } else {
                    // Signal to the downstream to treat any subsequent value as an OR condition 
                    parmValueProcessed = true;
                }

                addValue(whereClauseSegment, bindVariables, tableAlias, prefix, value.getValueNumber());
                addSystemIfPresent(parameterDao, whereClauseSegment, tableAlias, bindVariables,
                        value.getValueSystem());
                addCodeIfPresent(whereClauseSegment, tableAlias, bindVariables,
                        value.getValueCode());
            }
        }

        // End the Clause started above, and closes the parameter expression. 
        // Query: ))
        whereClauseSegment.append(RIGHT_PAREN).append(RIGHT_PAREN).append(RIGHT_PAREN);
    }

    /**
     * adds the system if present.
     * 
     * @param parameterDao
     * @param whereClauseSegment
     * @param tableAlias
     * @param bindVariables
     * @param system
     * @throws FHIRPersistenceException
     */
    public void addSystemIfPresent(ParameterDAO parameterDao, StringBuilder whereClauseSegment, String tableAlias,
            List<Object> bindVariables,
            String system) throws FHIRPersistenceException {
        /*
         * <code>GET
         * [base]/Observation?value-quantity=5.4|http://unitsofmeasure.org|mg</code>
         * system -> http://unitsofmeasure.org
         * <br>
         * In the above example, the system is unitsofmeasure.org
         * <br>
         * When a system is present, the following sql is returned:
         * <code>AND BASIC.CODE_SYSTEM_ID = ?</code>
         * -1 indicates the system is not found (rather than returning null)
         * 1 ... * - is used to indicate the key of the parameter in the parameters
         * table,
         * and to enable faster filtering.
         * <br>
         * This SQL is always an EXACT match unless a NOT modifier is used.
         * When :not is used, the semantics are treated as:
         * <code>value <> ? AND system = ? AND code = ?</code>
         */
        if (isPresent(system)) {
            Integer systemId = CodeSystemsCache.getCodeSystemId(system);
            if (systemId == null) {
                systemId = parameterDao.readCodeSystemId(system);
                if (systemId != null) {
                    // If found, we want to cache it. 
                    parameterDao.addCodeSystemsCacheCandidate(system, systemId);
                } else {
                    // This is an invalid number in the sequence. 
                    // All of our sequences start with 1 and NO CYCLE. 
                    systemId = -1;
                }
            }

            // We shouldn't be adding to the query if it's NULL at this point. 
            // What should we do? 
            whereClauseSegment.append(JDBCOperator.AND.value()).append(tableAlias).append(DOT)
                    .append(CODE_SYSTEM_ID)
                    .append(JDBCOperator.EQ.value()).append(BIND_VAR);
            bindVariables.add(systemId);
        }
    }

    /**
     * add code if present.
     * 
     * @param whereClauseSegment
     * @param tableAlias
     * @param bindVariables
     * @param code
     */
    public void addCodeIfPresent(StringBuilder whereClauseSegment, String tableAlias, List<Object> bindVariables,
            String code) {
        // Include code if present.
        if (isPresent(code)) {
            whereClauseSegment.append(JDBCOperator.AND.value()).append(tableAlias + DOT).append(CODE)
                    .append(JDBCOperator.EQ.value()).append(BIND_VAR);
            bindVariables.add(code);
        }
    }

    public boolean isPresent(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * the build common clause considers _VALUE_*** and _VALUE when querying the
     * data.
     * <br>
     * The data should not result in a duplication as the OR condition short
     * circuits double matches.
     * If one exists, great, we'll return it, else we'll peek at the other column.
     * <br>
     * 
     * @param whereClauseSegment
     * @param bindVariables
     * @param tableAlias
     * @param columnName
     * @param columnNameLowOrHigh
     * @param operator
     * @param value
     */
    public void buildCommonClause(StringBuilder whereClauseSegment, List<Object> bindVariables, String tableAlias,
            String columnName, String columnNameLowOrHigh, String operator, BigDecimal value, BigDecimal bound) {
        whereClauseSegment
                .append(LEFT_PAREN)
                .append(tableAlias).append(DOT).append(columnName).append(operator).append(BIND_VAR)
                .append(JDBCOperator.OR.value())
                .append(tableAlias).append(DOT).append(columnNameLowOrHigh).append(operator).append(BIND_VAR)
                .append(RIGHT_PAREN);

        bindVariables.add(value);
        bindVariables.add(bound);
    }
    
    public void buildEqualsRangeClause(StringBuilder whereClauseSegment, List<Object> bindVariables, String tableAlias,
           BigDecimal lowerBound, BigDecimal upperBound, BigDecimal value) {
        whereClauseSegment
            .append(LEFT_PAREN)
            .append(LEFT_PAREN)
                    .append(tableAlias).append(DOT).append(QUANTITY_VALUE).append(JDBCOperator.GT.value()).append(BIND_VAR)
                    .append(JDBCOperator.AND.value())
                    .append(tableAlias).append(DOT).append(QUANTITY_VALUE).append(JDBCOperator.LTE.value()).append(BIND_VAR)
                .append(RIGHT_PAREN)
                .append(JDBCOperator.OR.value())
                .append(LEFT_PAREN)
                    .append(tableAlias).append(DOT).append(QUANTITY_VALUE_LOW).append(JDBCOperator.LT.value()).append(BIND_VAR)
                    .append(JDBCOperator.AND.value())
                    .append(tableAlias).append(DOT).append(QUANTITY_VALUE_HIGH).append(JDBCOperator.GTE.value()).append(BIND_VAR)
                .append(RIGHT_PAREN).append(RIGHT_PAREN);

        bindVariables.add(lowerBound);
        bindVariables.add(upperBound);
        bindVariables.add(value);
        bindVariables.add(value);
    }
    
    public void buildApproxRangeClause(StringBuilder whereClauseSegment, List<Object> bindVariables, String tableAlias,
            BigDecimal lowerBound, BigDecimal upperBound, BigDecimal value) {
         whereClauseSegment
             .append(LEFT_PAREN)
             .append(LEFT_PAREN)
                     .append(tableAlias).append(DOT).append(QUANTITY_VALUE).append(JDBCOperator.GT.value()).append(BIND_VAR)
                     .append(JDBCOperator.AND.value())
                     .append(tableAlias).append(DOT).append(QUANTITY_VALUE).append(JDBCOperator.LTE.value()).append(BIND_VAR)
                 .append(RIGHT_PAREN)
                 .append(JDBCOperator.OR.value())
                 .append(LEFT_PAREN)
                     .append(tableAlias).append(DOT).append(QUANTITY_VALUE_LOW).append(JDBCOperator.LTE.value()).append(BIND_VAR)
                     .append(JDBCOperator.AND.value())
                     .append(tableAlias).append(DOT).append(QUANTITY_VALUE_HIGH).append(JDBCOperator.GTE.value()).append(BIND_VAR)
                 .append(RIGHT_PAREN).append(RIGHT_PAREN);

         bindVariables.add(lowerBound);
         bindVariables.add(upperBound);
         bindVariables.add(value);
         bindVariables.add(value);
     }
    
    public void buildNotEqualsRangeClause(StringBuilder whereClauseSegment, List<Object> bindVariables, String tableAlias,
            BigDecimal lowerBound, BigDecimal upperBound, BigDecimal value) {
         whereClauseSegment
             .append(LEFT_PAREN)
             .append(LEFT_PAREN)
                     .append(tableAlias).append(DOT).append(QUANTITY_VALUE).append(JDBCOperator.LTE.value()).append(BIND_VAR)
                     .append(JDBCOperator.OR.value())
                     .append(tableAlias).append(DOT).append(QUANTITY_VALUE).append(JDBCOperator.GT.value()).append(BIND_VAR)
                 .append(RIGHT_PAREN)
                 .append(JDBCOperator.OR.value())
                 .append(LEFT_PAREN)
                     .append(tableAlias).append(DOT).append(QUANTITY_VALUE_LOW).append(JDBCOperator.LTE.value()).append(BIND_VAR)
                     .append(JDBCOperator.OR.value())
                     .append(tableAlias).append(DOT).append(QUANTITY_VALUE_HIGH).append(JDBCOperator.GT.value()).append(BIND_VAR)
                 .append(RIGHT_PAREN).append(RIGHT_PAREN);

         bindVariables.add(lowerBound);
         bindVariables.add(upperBound);
         bindVariables.add(value);
         bindVariables.add(value);
     }

    /**
     * Append the condition and bind the variables according to the semantics of the
     * passed prefix
     * adds the value to the whereClause.
     * 
     * @param whereClauseSegment
     * @param bindVariables
     * @param tableAlias
     * @param prefix
     * @param value
     */
    public void addValue(StringBuilder whereClauseSegment, List<Object> bindVariables, String tableAlias,
            Prefix prefix, BigDecimal value) {

        BigDecimal lowerBound = NumberParmBehaviorUtil.generateLowerBound(value);
        BigDecimal upperBound = NumberParmBehaviorUtil.generateUpperBound(value);

        switch (prefix) {
        case EB:
            // EB - Ends Before
            // the range of the search value does not overlap with the range of the target value,
            // and the range above the search value contains the range of the target value
            buildCommonClause(whereClauseSegment, bindVariables, tableAlias, QUANTITY_VALUE, QUANTITY_VALUE_LOW,
                    JDBCOperator.LT.value(), value, lowerBound);
            break;
        case SA:
            // SA - Starts After
            // the range of the search value does not overlap with the range of the target value,
            // and the range below the search value contains the range of the target value
            buildCommonClause(whereClauseSegment, bindVariables, tableAlias, QUANTITY_VALUE, QUANTITY_VALUE_HIGH,
                    JDBCOperator.GT.value(), value, upperBound);
            break;
        case GE:
            // GE - Greater Than Equal
            // the range above the search value intersects (i.e. overlaps) with the range of the target value,
            // or the range of the search value fully contains the range of the target value
            buildCommonClause(whereClauseSegment, bindVariables, tableAlias, QUANTITY_VALUE, QUANTITY_VALUE_LOW,
                    JDBCOperator.GTE.value(), value, lowerBound);
            break;
        case GT:
            // GT - Greater Than
            // the range above the search value intersects (i.e. overlaps) with the range of the target value
            buildCommonClause(whereClauseSegment, bindVariables, tableAlias, QUANTITY_VALUE, QUANTITY_VALUE_LOW,
                    JDBCOperator.GT.value(), value, lowerBound);
            break;
        case LE:
            // LE - Less Than Equal
            // the range below the search value intersects (i.e. overlaps) with the range of the target value
            // or the range of the search value fully contains the range of the target value
            buildCommonClause(whereClauseSegment, bindVariables, tableAlias, QUANTITY_VALUE, QUANTITY_VALUE_HIGH,
                    JDBCOperator.LTE.value(), value, lowerBound);
            break;
        case LT:
            // LT - Less Than
            // the range below the search value intersects (i.e. overlaps) with the range of the target value
            buildCommonClause(whereClauseSegment, bindVariables, tableAlias, QUANTITY_VALUE, QUANTITY_VALUE_HIGH,
                    JDBCOperator.LT.value(), value, lowerBound);
            break;
        case AP:
            // AP - Approximate - Relative
            // -10% of the Lower Bound
            // +10% of the Upper Bound
            BigDecimal factor = value.multiply(NumberParmBehaviorUtil.FACTOR);
            buildApproxRangeClause(whereClauseSegment, bindVariables, tableAlias, lowerBound.subtract(factor), upperBound.add(factor), value);
            break;
        case NE:
            // NE:  Upper and Lower Bounds - Range Based Search
            // the range of the search value does not fully contain the range of the target value
            buildNotEqualsRangeClause(whereClauseSegment, bindVariables, tableAlias, lowerBound, upperBound, value);
            break;
        case EQ:
        default:
            // EQ:  Upper and Lower Bounds - Range Based Search
            // the range of the search value fully contains the range of the target value
            buildEqualsRangeClause(whereClauseSegment, bindVariables, tableAlias, lowerBound, upperBound, value);
            break;
        }
    }
}