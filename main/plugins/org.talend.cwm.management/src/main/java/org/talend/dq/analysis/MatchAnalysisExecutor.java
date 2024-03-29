// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.EList;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.utils.platform.PluginChecker;
import org.talend.core.ITDQRepositoryService;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DelimitedFileConnection;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.cwm.db.connection.DatabaseSQLExecutor;
import org.talend.cwm.db.connection.DelimitedFileSQLExecutor;
import org.talend.cwm.db.connection.ISQLExecutor;
import org.talend.cwm.db.connection.SQLExecutor;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.cwm.management.i18n.Messages;
import org.talend.cwm.relational.TdColumn;
import org.talend.dataquality.PluginConstant;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.ExecutionInformations;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.columnset.BlockKeyIndicator;
import org.talend.dataquality.indicators.columnset.RecordMatchingIndicator;
import org.talend.dataquality.indicators.columnset.impl.RecordMatchingIndicatorImpl;
import org.talend.dataquality.indicators.mapdb.DBMap;
import org.talend.dataquality.indicators.mapdb.StandardDBName;
import org.talend.dataquality.matchmerge.Record;
import org.talend.dataquality.record.linkage.grouping.MatchGroupResultConsumer;
import org.talend.dataquality.rules.MatchRuleDefinition;
import org.talend.dq.analysis.match.BlockAndMatchManager;
import org.talend.dq.analysis.memory.AnalysisThreadMemoryChangeNotifier;
import org.talend.dq.helper.AnalysisExecutorHelper;
import org.talend.dq.helper.ContextHelper;
import org.talend.dq.helper.StoreOnDiskUtils;
import org.talend.dq.indicators.Evaluator;
import org.talend.utils.sugars.ReturnCode;
import org.talend.utils.sugars.TypedReturnCode;
import org.talend.dataquality.record.linkage.iterator.ResultSetIterator;
import orgomg.cwm.foundation.softwaredeployment.DataManager;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * execute the match analysis
 *
 */
public class MatchAnalysisExecutor implements IAnalysisExecutor {

    private static Logger log = Logger.getLogger(MatchAnalysisExecutor.class);

    private long usedMemory;

    private volatile boolean isLowMemory = false;

    private long checkContinueCount = 0L;

    private boolean keepRunning = true;

    /*
     * (non-Javadoc)
     *
     * @see org.talend.dq.analysis.IAnalysisExecutor#execute(org.talend.dataquality.analysis.Analysis)
     */
    public ReturnCode execute(Analysis analysis) {
        assert analysis != null;
        // --- preconditions
        ReturnCode rc = AnalysisExecutorHelper.check(analysis);
        if (!rc.isOk()) {
            AnalysisExecutorHelper.setExecutionInfoInAnalysisResult(analysis, rc.isOk(), rc.getMessage());
            return rc;
        }

        // --- creation time
        final long startime = AnalysisExecutorHelper.setExecutionDateInAnalysisResult(analysis);

        EList<Indicator> indicators = analysis.getResults().getIndicators();
        RecordMatchingIndicator recordMatchingIndicator = null;
        BlockKeyIndicator blockKeyIndicator = null;
        for (Indicator ind : indicators) {
            if (ind instanceof RecordMatchingIndicator) {
                recordMatchingIndicator = (RecordMatchingIndicator) ind;
            } else if (ind instanceof BlockKeyIndicator) {
                blockKeyIndicator = (BlockKeyIndicator) ind;
            }
        }
        if (recordMatchingIndicator == null || blockKeyIndicator == null) {
            rc.setOk(Boolean.FALSE);
            rc.setMessage(Messages.getString("MatchAnalysisExecutor.noIndicators")); //$NON-NLS-1$
            AnalysisExecutorHelper.setExecutionInfoInAnalysisResult(analysis, rc.isOk(), rc.getMessage());
            return rc;
        }

        List<ModelElement> anlayzedElements = analysis.getContext().getAnalysedElements();
        if (anlayzedElements == null || anlayzedElements.size() == 0) {
            rc.setOk(Boolean.FALSE);
            rc.setMessage(Messages.getString("MatchAnalysisExecutor.EmptyAnalyzedElement")); //$NON-NLS-1$
            AnalysisExecutorHelper.setExecutionInfoInAnalysisResult(analysis, rc.isOk(), rc.getMessage());
            return rc;
        }

        // TDQ-9664 msjian: check the store on disk path.
        Boolean isStoreOnDisk = TaggedValueHelper.getValueBoolean(SQLExecutor.STORE_ON_DISK_KEY, analysis);
        Boolean isAllowDrillDown = false;
        if (isStoreOnDisk) {
            String tempDataPath = TaggedValueHelper.getValueString(SQLExecutor.TEMP_DATA_DIR, analysis);
            isAllowDrillDown =  TaggedValueHelper.getValueBoolean(SQLExecutor.ALLOW_DRILL_DOWN, analysis);
            File file = new File(tempDataPath);
            if (!file.exists() || !file.isDirectory()) {
                rc.setOk(Boolean.FALSE);
                rc.setMessage(Messages.getString("MatchAnalysisExecutor.InvalidPath", file.getPath())); //$NON-NLS-1$
                AnalysisExecutorHelper.setExecutionInfoInAnalysisResult(analysis, rc.isOk(), rc.getMessage());
                return rc;
            }
        }
        // TDQ-9664~

        Map<MetadataColumn, String> columnMap = getColumn2IndexMap(anlayzedElements);
        SQLExecutor sqlExecutor = getSQLExectutor(analysis, recordMatchingIndicator, columnMap);
        if (sqlExecutor == null) {
            rc.setOk(Boolean.FALSE);
            rc.setMessage(Messages.getString("MatchAnalysisExecutor.noSqlExecutor")); //$NON-NLS-1$
            AnalysisExecutorHelper.setExecutionInfoInAnalysisResult(analysis, rc.isOk(), rc.getMessage());
            return rc;
        }
        if (getMonitor() != null) {
            getMonitor().worked(20);
        }

        // Set schema for match key.
        TypedReturnCode<MatchGroupResultConsumer> returnCode = new TypedReturnCode<MatchGroupResultConsumer>();
        MetadataColumn[] completeColumnSchema = AnalysisRecordGroupingUtils.getCompleteColumnSchema(columnMap);
        String[] colSchemaString = new String[completeColumnSchema.length];
        int idx = 0;
        for (MetadataColumn metadataCol : completeColumnSchema) {
            colSchemaString[idx++] = metadataCol.getName();
        }
        recordMatchingIndicator.setMatchRowSchema(colSchemaString);
        recordMatchingIndicator.reset();

        MatchGroupResultConsumer matchResultConsumer =
                createMatchGroupResultConsumer(isAllowDrillDown, recordMatchingIndicator);
        DataManager connection = analysis.getContext().getConnection();
        // TDQ-19889 msjian: set Prompt Context Values to connection
        org.talend.core.model.metadata.builder.connection.Connection con = SwitchHelpers.CONNECTION_SWITCH
                .doSwitch(connection);
        Connection copyConnection = ContextHelper.getPromptContextValuedConnection(con);

        if (sqlExecutor.isStoreOnDisk()) {
            // need to execute the query
            try {
                sqlExecutor.executeQuery(copyConnection, analysis.getContext().getAnalysedElements());
            } catch (SQLException e) {
                log.error(e, e);
                rc.setOk(Boolean.FALSE);
                rc.setMessage(e.getMessage());
                AnalysisExecutorHelper.setExecutionInfoInAnalysisResult(analysis, rc.isOk(), rc.getMessage());
                return rc;
            }

            try {
                TypedReturnCode<Object> result = StoreOnDiskUtils.getDefault()
                        .executeWithStoreOnDisk(columnMap,
                                recordMatchingIndicator, blockKeyIndicator, sqlExecutor.getStoreOnDiskHandler(),
                                matchResultConsumer);
                if (isAllowDrillDown) {
                    handleDrillDown(anlayzedElements, recordMatchingIndicator, matchResultConsumer);
                }
                if (result != null) {
                    returnCode.setObject((MatchGroupResultConsumer) result.getObject());
                    returnCode.setOk(result.isOk());
                    returnCode.setMessage(result.getMessage());
                }
            } catch (Exception e) {
                log.error(e, e);
                returnCode.setMessage(e.getMessage());
                returnCode.setOk(false);
            }
        } else {
            // Added TDQ-9320 , use the result set iterator to replace the list of result in the memory.
            TypedReturnCode<java.sql.Connection> sqlConnection = null;
            try {
                sqlConnection =
                        ((AnalysisExecutor) sqlExecutor).getSqlConnection(analysis);
                Iterator<Record> resultSetIterator = sqlExecutor
                        .getResultSetIterator(copyConnection,
                        anlayzedElements, sqlConnection);
                BlockAndMatchManager bAndmManager =
                        new BlockAndMatchManager(resultSetIterator, matchResultConsumer, columnMap,
                                recordMatchingIndicator, blockKeyIndicator);
                bAndmManager.run();
            } catch (SQLException e) {
                log.error(e, e);
                rc.setOk(Boolean.FALSE);
                rc.setMessage(e.getMessage());
                AnalysisExecutorHelper.setExecutionInfoInAnalysisResult(analysis, rc.isOk(), rc.getMessage());
                return rc;
            } catch (BusinessException e) {
                log.error(e, e);
                rc.setOk(Boolean.FALSE);
                rc.setMessage(e.getMessage());
                AnalysisExecutorHelper.setExecutionInfoInAnalysisResult(analysis, rc.isOk(), rc.getMessage());
                return rc;
            } finally {
                if (!(analysis.getContext().getConnection() instanceof DelimitedFileConnection)) {
                    ((AnalysisExecutor) sqlExecutor).closeSqlConnection(analysis, sqlConnection.getObject());
                }
            }
        }

        if (!returnCode.isOk()) {
            rc.setOk(returnCode.isOk());
            rc.setMessage(returnCode.getMessage());
        }

        if (getMonitor() != null) {
            getMonitor().worked(20);
        }

        if (isLowMemory) {
            rc.setMessage(Messages.getString("Evaluator.OutOfMomory", usedMemory));//$NON-NLS-1$
        }

        // nodify the master page
        refreshTableWithMatchFullResult(analysis);

        // --- set metadata information of analysis
        AnalysisExecutorHelper.setExecutionInfoInAnalysisResult(analysis, rc.isOk(), rc.getMessage());

        // --- compute execution duration
        if (this.continueRun()) {
            long endtime = System.currentTimeMillis();
            final ExecutionInformations resultMetadata = analysis.getResults().getResultMetadata();
            resultMetadata.setExecutionDuration((int) (endtime - startime));
            resultMetadata.setOutThreshold(false);
        }
        if (getMonitor() != null) {
            getMonitor().worked(20);
        }
        return rc;
    }

    private void handleDrillDown(List<ModelElement> anlayzedElements, RecordMatchingIndicator recordMatchingIndicator,
            MatchGroupResultConsumer matchResultConsumer) {
        // TDQ-19618 support drill down
        List<Object[]> resultData =  new ArrayList<>();
        if (matchResultConsumer.getFullMatchResult() != null) {
            resultData = matchResultConsumer.getFullMatchResult();
        }
        List<Object[]> sortResultByGID = StoreOnDiskUtils.sortResultByGID(getColumnsForMatch(anlayzedElements, recordMatchingIndicator),
                resultData);
        List<String> matchRowSchemaList = Arrays.asList(recordMatchingIndicator.getMatchRowSchema());
        int groupSizeColumnIndex = matchRowSchemaList.indexOf(PluginConstant.GRP_SIZE);
        int masterColumnIndex = matchRowSchemaList.indexOf(PluginConstant.MASTER);
        int groupQualityColumnIndex = matchRowSchemaList.indexOf(PluginConstant.GRP_QUALITY);

        String mapDBName = StandardDBName.drillDown.name();
        DBMap<Object, Object[]> oneGroupDBMap = null;
        DBMap<Object, Object[]> matchGroupDBMap =
                (DBMap<Object, Object[]>) ((RecordMatchingIndicatorImpl) recordMatchingIndicator)
                        .getMapDB(mapDBName + "Matched");
        DBMap<Object, Object[]> suspectGroupDBMap =
                (DBMap<Object, Object[]>) ((RecordMatchingIndicatorImpl) recordMatchingIndicator)
                        .getMapDB(mapDBName + "Suspect");
        Long index = Long.valueOf(0l);
        Long matchIndex = Long.valueOf(0l);
        Long suspectIndex = Long.valueOf(0l);
        boolean isMatch = true;
        double confidenceThreshold = 0.85;
        if (recordMatchingIndicator.getBuiltInMatchRuleDefinition() != null) {
            confidenceThreshold =
                    ((MatchRuleDefinition) recordMatchingIndicator.getBuiltInMatchRuleDefinition())
                            .getMatchGroupQualityThreshold();
        }

        for (Object[] record : sortResultByGID) {
            int groupSize = StringUtils.isEmpty((String) record[groupSizeColumnIndex]) ? 0
                    : Integer.valueOf((String) record[groupSizeColumnIndex]);

            // get the group size of the master of one group
            if (Boolean.valueOf((String) record[masterColumnIndex])) {
                mapDBName = StandardDBName.drillDown.name() + groupSize;
                oneGroupDBMap = (DBMap<Object, Object[]>) ((RecordMatchingIndicatorImpl) recordMatchingIndicator)
                        .getMapDB(mapDBName);

                if (groupSize > 1) {
                    // Group quality score >= confidence threshold then it's a confident match group
                    double groupScore = Double.valueOf((String) record[groupQualityColumnIndex]);
                    if (groupScore >= confidenceThreshold) {
                        isMatch = true;
                    } else {
                        isMatch = false;
                    }
                }
            }

            // put this record into related dbmap;
            if (groupSize != 1) {
                if (isMatch) {
                    matchGroupDBMap.put(matchIndex++, record);
                } else {
                    suspectGroupDBMap.put(suspectIndex++, record);
                }
            }
            oneGroupDBMap.put(index++, record);
        }

    }
    
    private String[] getColumnsForMatch(List<ModelElement> anlayzedElements, RecordMatchingIndicator indicator) {
        String[] completeColumnNames = AnalysisRecordGroupingUtils.getCompleteColumnNames((ModelElement[]) anlayzedElements.toArray());

        List<Object[]> listCols = new ArrayList<Object[]>();
        listCols.add(completeColumnNames);
        indicator.setListRows(listCols);
        
        return completeColumnNames;
    }

    private MatchGroupResultConsumer createMatchGroupResultConsumer(boolean allowDrillDown,
            final RecordMatchingIndicator recordMatchingIndicator) {
        MatchGroupResultConsumer matchResultConsumer = new MatchGroupResultConsumer(allowDrillDown) {

            /*
             * (non-Javadoc)
             *
             * @see org.talend.dataquality.record.linkage.grouping. MatchGroupResultConsumer#handle(java.lang.Object)
             */
            @Override
            public void handle(Object row) {
                recordMatchingIndicator.handle(row);
                // TDQ-19618 support drill down
                if (this.isKeepDataInMemory) {
                    addOneRowOfResult(row);
                }
            }
        };
        return matchResultConsumer;
    }

    /**
     * refresh Table With Match Full Result .
     *
     * @param analysis
     *
     * @param matchResultConsumer
     */
    private void refreshTableWithMatchFullResult(Analysis analysis) {
        ITDQRepositoryService tdqRepService = AnalysisExecutorHelper.getTDQService();
        if (tdqRepService != null) {
            tdqRepService.refreshTableWithResult(analysis, null);
        }

    }

    /**
     * get Column2Index Map".
     *
     * @param anlayzedElements
     * @return
     */
    private Map<MetadataColumn, String> getColumn2IndexMap(List<ModelElement> anlayzedElements) {
        Map<MetadataColumn, String> columnMap = new HashMap<MetadataColumn, String>();
        int index = 0;
        for (ModelElement column : anlayzedElements) {
            columnMap.put((MetadataColumn) column, String.valueOf(index++));
        }
        return columnMap;
    }

    /**
     * getSQLExectutor .
     *
     * @param modelElement
     * @return
     */
    private SQLExecutor getSQLExectutor(Analysis analysis, RecordMatchingIndicator recordMatchingIndicator,
            Map<MetadataColumn, String> columnMap) {
        ModelElement modelElement = analysis.getContext().getAnalysedElements().get(0);
        SQLExecutor sqlExecutor = null;
        if (modelElement instanceof TdColumn) {
            sqlExecutor = new DatabaseSQLExecutor();
        } else if (modelElement instanceof MetadataColumn) {
            sqlExecutor = new DelimitedFileSQLExecutor();
        }
        sqlExecutor.SetAnalysis(analysis);
        // Tune on store on disk option when needed.
        Boolean isStoreOnDisk =
                PluginChecker.isTDQLoaded() ? TaggedValueHelper.getValueBoolean(SQLExecutor.STORE_ON_DISK_KEY,
                        analysis) : Boolean.FALSE;
        if (sqlExecutor != null && isStoreOnDisk) {
            sqlExecutor.setStoreOnDisk(Boolean.TRUE);
            sqlExecutor.initStoreOnDiskHandler(analysis, recordMatchingIndicator, columnMap);
        }
        return sqlExecutor;
    }

    // FIXME this method is the same as Evaluator.continueRun(). Factorize code.
    protected boolean continueRun() {
        // MOD scorreia 2013-09-10 avoid checking for each analyzed row. Check only every 1000 rows
        checkContinueCount++;
        if (checkContinueCount % Evaluator.CHECK_EVERY_N_COUNT != 0) {
            return keepRunning;
        }
        if (!Platform.isRunning()) { // Reporting engine is working as library
            return true;
        }

        if (getMonitor() != null && getMonitor().isCanceled()) {
            keepRunning = false;
        } else if (this.isLowMemory) {
            keepRunning = false;
        } else if (AnalysisThreadMemoryChangeNotifier.getInstance().isUsageThresholdExceeded()) {
            this.usedMemory = AnalysisThreadMemoryChangeNotifier.convertToMB(ManagementFactory.getMemoryMXBean()
                    .getHeapMemoryUsage()
                    .getUsed());
            this.isLowMemory = true;
            keepRunning = false;
        }
        return keepRunning;
    }

    private IProgressMonitor monitor;

    public IProgressMonitor getMonitor() {
        return monitor;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.talend.dq.analysis.IAnalysisExecutor#setMonitor(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void setMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }
}
