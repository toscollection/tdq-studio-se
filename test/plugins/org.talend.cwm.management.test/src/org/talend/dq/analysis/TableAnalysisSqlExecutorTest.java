// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dq.analysis;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.talend.core.model.metadata.builder.connection.ConnectionFactory;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.database.dburl.SupportDBUrlType;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.cwm.relational.RelationalFactory;
import org.talend.cwm.relational.TdColumn;
import org.talend.cwm.relational.TdExpression;
import org.talend.cwm.relational.TdTable;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.AnalysisContext;
import org.talend.dataquality.analysis.AnalysisFactory;
import org.talend.dataquality.analysis.AnalysisParameters;
import org.talend.dataquality.analysis.AnalysisResult;
import org.talend.dataquality.indicators.sql.IndicatorSqlFactory;
import org.talend.dataquality.indicators.sql.WhereRuleAideIndicator;
import org.talend.dataquality.rules.JoinElement;
import org.talend.dataquality.rules.RulesFactory;
import org.talend.dataquality.rules.WhereRule;
import orgomg.cwm.objectmodel.core.Expression;
import orgomg.cwm.resource.relational.Catalog;

/**
 * created by xqliu on 2014-8-20 Detailled comment
 * 
 */
public class TableAnalysisSqlExecutorTest {

    Analysis testAnalysis = null;

    // RowCountIndicator testRowCountIndicator = null;
    //
    // IndicatorDefinition testRowCountIndicatorDefinition = null;
    //
    // WhereRuleIndicator testWhereRuleIndicator = null;
    //
    // IndicatorDefinition testWhereRuleIndicatorDefinition = null;

    WhereRuleAideIndicator testWhereRuleAideIndicator = null;

    WhereRule testWhereRuleAideIndicatorDefinition = null;

    /**
     * DOC xqliu Comment method "setUpBeforeClass".
     * 
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // do nothing here
    }

    /**
     * DOC xqliu Comment method "tearDownAfterClass".
     * 
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // do nothing here
    }

    /**
     * DOC xqliu Comment method "setUp".
     * 
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // create TdTable
        TdTable testTdTable = RelationalFactory.eINSTANCE.createTdTable();
        testTdTable.setName("generateTable"); //$NON-NLS-1$

        // create connection
        DatabaseConnection testDatabaseConnection = ConnectionFactory.eINSTANCE.createDatabaseConnection();
        TaggedValueHelper.setTaggedValue(testDatabaseConnection, TaggedValueHelper.DB_PRODUCT_NAME,
                SupportDBUrlType.MYSQLDEFAULTURL.getDBKey());

        // create catalog
        Catalog testCatalog = orgomg.cwm.resource.relational.RelationalFactory.eINSTANCE.createCatalog();
        testCatalog.setName("TBI"); //$NON-NLS-1$
        testTdTable.setNamespace(testCatalog);
        testCatalog.setNamespace(testDatabaseConnection);
        testCatalog.getDataManager().add(testDatabaseConnection);

        // create analysis
        testAnalysis = AnalysisFactory.eINSTANCE.createAnalysis();

        // create Analaysis result
        AnalysisResult testAnalysisResult = AnalysisFactory.eINSTANCE.createAnalysisResult();
        testAnalysis.setResults(testAnalysisResult);

        // create analysis parameter
        AnalysisParameters testAnalysisParameters = AnalysisFactory.eINSTANCE.createAnalysisParameters();
        testAnalysis.setParameters(testAnalysisParameters);

        // create analysis Content
        AnalysisContext testAnalysisContext = AnalysisFactory.eINSTANCE.createAnalysisContext();
        testAnalysis.setContext(testAnalysisContext);
        testAnalysisContext.setConnection(testDatabaseConnection);

        // // create row count indicatorDefinition
        // testRowCountIndicatorDefinition = DefinitionFactory.eINSTANCE.createIndicatorDefinition();
        //        testRowCountIndicatorDefinition.setLabel("Row Count"); //$NON-NLS-1$
        //
        // // create row count indicator
        // testRowCountIndicator = IndicatorsFactory.eINSTANCE.createRowCountIndicator();
        // testAnalysisResult.getIndicators().add(testRowCountIndicator);
        // testRowCountIndicator.setAnalyzedElement(testTdTable);
        // testRowCountIndicator.setIndicatorDefinition(testRowCountIndicatorDefinition);
        //
        // // create WhereRule indicatorDefinition
        // testWhereRuleIndicatorDefinition = DefinitionFactory.eINSTANCE.createIndicatorDefinition();
        //        testWhereRuleIndicatorDefinition.setLabel("rule1"); //$NON-NLS-1$
        //
        // // create WhereRule indicator
        // testWhereRuleIndicator = IndicatorSqlFactory.eINSTANCE.createWhereRuleIndicator();
        // testAnalysisResult.getIndicators().add(testWhereRuleIndicator);
        // testWhereRuleIndicator.setAnalyzedElement(testTdTable);
        // testWhereRuleIndicator.setIndicatorDefinition(testWhereRuleIndicatorDefinition);

        // create WhereRuleAide indicatorDefinition
        testWhereRuleAideIndicatorDefinition = RulesFactory.eINSTANCE.createWhereRule();
        testWhereRuleAideIndicatorDefinition.setLabel("rule1"); //$NON-NLS-1$

        // create WhereRuleAide indicator
        testWhereRuleAideIndicator = IndicatorSqlFactory.eINSTANCE.createWhereRuleAideIndicator();
        testAnalysisResult.getIndicators().add(testWhereRuleAideIndicator);
        testWhereRuleAideIndicator.setAnalyzedElement(testTdTable);
        testWhereRuleAideIndicator.setIndicatorDefinition(testWhereRuleAideIndicatorDefinition);
    }

    /**
     * DOC xqliu Comment method "tearDown".
     * 
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.talend.dq.analysis.TableAnalysisSqlExecutor#createSqlStatement(org.talend.dataquality.analysis.Analysis)}
     * . case 1:the where rule have not join conditions
     */
    @Test
    public void testCreateSqlStatementCase1() {
        TdExpression expression = RelationalFactory.eINSTANCE.createTdExpression();
        expression.setBody("SELECT COUNT(*) FROM &lt;%=__TABLE_NAME__%> &lt;%=__JOIN_CLAUSE__%> &lt;%=__WHERE_CLAUSE__%>"); //$NON-NLS-1$
        expression.setLanguage("SQL"); //$NON-NLS-1$
        testWhereRuleAideIndicatorDefinition.getSqlGenericExpression().add(expression);

        TableAnalysisSqlExecutor tableAnalysisSqlExecutor = new TableAnalysisSqlExecutor();
        String actualSqlStatement = tableAnalysisSqlExecutor.createSqlStatement(testAnalysis);
        Assert.assertEquals(StringUtils.EMPTY, actualSqlStatement);
        EList<Expression> instantiatedExpressions = testWhereRuleAideIndicator.getInstantiatedExpressions();
        Assert.assertNotNull(instantiatedExpressions);
        Assert.assertEquals(0, instantiatedExpressions.size());
    }

    /**
     * Test method for
     * {@link org.talend.dq.analysis.TableAnalysisSqlExecutor#createSqlStatement(org.talend.dataquality.analysis.Analysis)}
     * . case 1:the where rule have join conditions
     */
    @Test
    public void testCreateSqlStatementCase2() {
        String sql = "select count(*) from table where 1=1"; //$NON-NLS-1$
        TdExpression expression = RelationalFactory.eINSTANCE.createTdExpression();
        expression.setBody(sql);
        expression.setLanguage("SQL"); //$NON-NLS-1$
        testWhereRuleAideIndicatorDefinition.getSqlGenericExpression().add(expression);

        JoinElement createJoinElement = RulesFactory.eINSTANCE.createJoinElement();
        createJoinElement.setColumnAliasA("colA"); //$NON-NLS-1$
        createJoinElement.setColumnAliasB("colB"); //$NON-NLS-1$
        createJoinElement.setTableAliasA("tabA"); //$NON-NLS-1$
        createJoinElement.setTableAliasB("tabB"); //$NON-NLS-1$
        TdColumn createTdColumnA = RelationalFactory.eINSTANCE.createTdColumn();
        createTdColumnA.setName("colA"); //$NON-NLS-1$
        TdColumn createTdColumnB = RelationalFactory.eINSTANCE.createTdColumn();
        createTdColumnB.setName("colB"); //$NON-NLS-1$
        createJoinElement.setColA(createTdColumnA);
        createJoinElement.setColB(createTdColumnB);
        testWhereRuleAideIndicatorDefinition.getJoins().add(createJoinElement);
        testWhereRuleAideIndicator.getJoinConditions().add(createJoinElement);

        TableAnalysisSqlExecutor tableAnalysisSqlExecutor = new TableAnalysisSqlExecutor();
        String actualSqlStatement = tableAnalysisSqlExecutor.createSqlStatement(testAnalysis);
        Assert.assertEquals(StringUtils.EMPTY, actualSqlStatement);
        EList<Expression> instantiatedExpressions = testWhereRuleAideIndicator.getInstantiatedExpressions();
        Assert.assertNotNull(instantiatedExpressions);
        Assert.assertEquals(1, instantiatedExpressions.size());
        Assert.assertEquals(sql, instantiatedExpressions.get(0).getBody());
    }
}
