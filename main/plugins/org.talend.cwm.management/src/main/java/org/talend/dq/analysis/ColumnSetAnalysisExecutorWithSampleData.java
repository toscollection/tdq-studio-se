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

import org.talend.dataquality.analysis.Analysis;
import org.talend.dq.indicators.ColumnSetIndicatorEvaluator;
import org.talend.dq.indicators.ColumnSetIndicatorEvaluatorWithSampleData;

/**
 * DOC zshen class global comment. Detailled comment
 */
public class ColumnSetAnalysisExecutorWithSampleData extends ColumnSetAnalysisExecutor {

    /**
     * DOC zshen ColumnSetAnalysisExecutorWithSampleData constructor comment.
     *
     * @param isDelimitedFile
     */
    public ColumnSetAnalysisExecutorWithSampleData(boolean isDelimitedFile) {
        super(isDelimitedFile);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.talend.dq.analysis.ColumnSetAnalysisExecutor#createIndicatorEvaluator(org.talend.dataquality.analysis.Analysis)
     */
    @Override
    protected ColumnSetIndicatorEvaluator createIndicatorEvaluator(Analysis analysis) {
        return new ColumnSetIndicatorEvaluatorWithSampleData(analysis);
    }

}
