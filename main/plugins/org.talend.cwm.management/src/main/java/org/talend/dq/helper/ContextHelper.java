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
package org.talend.dq.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.EList;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.context.ContextUtils;
import org.talend.core.model.context.JobContextManager;
import org.talend.core.model.context.link.ContextLinkService;
import org.talend.core.model.context.link.ItemContextLink;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.database.JavaSqlFactory;
import org.talend.core.model.process.IContext;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.ContextItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.utils.ContextParameterUtils;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.helpers.AnalysisHelper;
import org.talend.dataquality.helpers.ReportHelper;
import org.talend.dataquality.properties.TDQAnalysisItem;
import org.talend.dataquality.properties.TDQReportItem;
import org.talend.dataquality.reports.TdReport;
import org.talend.designer.core.model.utils.emf.talendfile.ContextParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.ContextType;
import org.talend.metadata.managment.utils.MetadataConnectionUtils;

/**
 * created by xqliu on Jul 24, 2013 Detailled comment
 *
 */
public final class ContextHelper {

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    private static final String CONTEXT_PREFFIX = "context."; //$NON-NLS-1$

    private static final ECodeLanguage LANGUAGE = LanguageManager.getCurrentLanguage();

    /**
     * if the str is context variable return true, else return false.
     *
     * @param str
     * @return
     */
    public static boolean isContextVar(String str) {
        return str == null ? false : str.startsWith(CONTEXT_PREFFIX);
    }

    /**
     * get the context value if pass a context name, otherwise return the str directly.
     *
     * @param contexts the context list
     * @param contextGroupName the name of the group which be used to get the context value
     * @param contextVarName the context variable's name
     * @return
     */
    public static String getContextValue(List<ContextType> contexts, String contextGroupName, String contextVarName) {
        String value = EMPTY_STRING;
        if (!StringUtils.isEmpty(contextVarName)) {
            boolean findContext = false;
            if (contextVarName.startsWith(CONTEXT_PREFFIX)) {
                String contextName = contextVarName.substring(CONTEXT_PREFFIX.length(), contextVarName.length());
                List<IContext> iContextLs = new ArrayList<IContext>();
                if (contexts != null) {
                    for (ContextType contxType : contexts) {
                        IContext context = ContextUtils.convert2IContext(contxType);
                        iContextLs.add(context);
                    }
                }
                for (ContextType ct : contexts) {
                    if (ct.getName().equals(contextGroupName)) {
                        for (Object obj : ct.getContextParameter()) {
                            ContextParameterType cpt = (ContextParameterType) obj;
                            if (cpt.getName().equals(contextName)) {
                                if ((cpt.isPromptNeeded() || ContextUtils.isPromptNeeded(iContextLs, cpt.getName()))
                                        && Platform.isRunning()) {
                                    value = JavaSqlFactory
                                            .getReportPromptConValueFromCache(contextGroupName,
                                                    cpt.getRepositoryContextId(), contextVarName);
                                } else {
                                    value = cpt.getRawValue();
                                }
                                findContext = true;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (!findContext) {
                value = contextVarName;
            }
        }
        return value;
    }

    /**
     * get the context's value from specified context group of the report.
     *
     * @param tdReport the Report
     * @param contextGroupName the name of the context group which be used to get the context value
     * @param contextVarName the context variable's name
     * @return
     */
    public static String getReportContextValue(TdReport tdReport, String contextGroupName, String contextVarName) {
        return getContextValue(tdReport.getContext(), contextGroupName, contextVarName);
    }

    /**
     * get the context's value from the last run context(or default context if last run context is empty) in the report.
     *
     * @param tdReport the Report
     * @param contextVarName the context variable's name
     * @return
     */
    public static String getReportContextValue(TdReport tdReport, String contextVarName) {
        return getReportContextValue(tdReport, ReportHelper.getContextGroupName(tdReport), contextVarName);
    }

    /**
     * get the context's value from specified context group of the analysis.
     *
     * @param analysis
     * @param contextGroupName the name of the context group which be used to get the context value
     * @param contextVarName the context variable's name
     * @return
     */
    public static String getAnalysisContextValue(Analysis analysis, String contextGroupName, String contextVarName) {
        return getContextValue(analysis.getContextType(), contextGroupName, contextVarName);
    }

    /**
     * get the context's value from the last run context(or default context if last run context is empty) in the
     * analysis.
     *
     * @param analysis
     * @param contextVarName the context variable's name
     * @return
     */
    public static String getAnalysisContextValue(Analysis analysis, String contextVarName) {
        return getAnalysisContextValue(analysis, AnalysisHelper.getContextGroupName(analysis), contextVarName);
    }

    /**
     * translate the context variable in the string to the value according to the specific context group name.
     *
     * @param context
     * @param contextGroupName
     * @param contextString the stirng contains context variable, example:
     * jdbc:mysql://context.TdqContext_Host:context.TdqContext_Port/context.TdqContext_DbName?characterEncoding=UTF8
     * @return
     */
    public static String getUrlWithoutContext(List<ContextType> context, String contextGroupName, String contextString) {
        // key is the context script code(start with context.), value is the context value
        Map<String, String> contextValues = new HashMap<String, String>();
        List<IContext> iContextLs = new ArrayList<IContext>();
        if (context != null) {
            for (ContextType contxType : context) {
                IContext ctx = ContextUtils.convert2IContext(contxType);
                iContextLs.add(ctx);
            }
        }
        for (ContextType contextType : context) {
            if (contextType.getName().equals(contextGroupName)) {
                for (Object obj : contextType.getContextParameter()) {
                    ContextParameterType cpt = (ContextParameterType) obj;
                    String contextVarName = ContextParameterUtils.getNewScriptCode(cpt.getName(), LANGUAGE);
                    String value = cpt.getRawValue();
                    if ((cpt.isPromptNeeded() || ContextUtils.isPromptNeeded(iContextLs, cpt.getName()))
                            && Platform.isRunning()) {
                        value = JavaSqlFactory
                                .getReportPromptConValueFromCache(contextGroupName, cpt.getRepositoryContextId(),
                                        contextVarName);
                    }
                    contextValues.put(contextVarName, value);
                }
                break;
            }
        }
        return getUrlWithoutContext(contextString, contextValues);
    }

    /**
     * translate the context variable in the string to the value according to the specific context group name.
     *
     * @param tdReport
     * @param contextGroupName
     * @param contextualizeUrl the stirng contains context variable, example:
     * jdbc:mysql://context.TdqContext_Host:context.TdqContext_Port/context.TdqContext_DbName?characterEncoding=UTF8
     * @return
     */
    public static String getUrlWithoutContext(TdReport tdReport, String contextGroupName, String contextualizeUrl) {
        return getUrlWithoutContext(tdReport.getContext(), contextGroupName, contextualizeUrl);
    }

    /**
     * translate the context variable in the string to the value according to the default context group name.
     *
     * @param tdReport
     * @param contextString the stirng contains context variable, example:
     * jdbc:mysql://context.TdqContext_Host:context.TdqContext_Port/context.TdqContext_DbName?characterEncoding=UTF8
     * @return
     */
    public static String getUrlWithoutContext(TdReport tdReport, String contextString) {
        return getUrlWithoutContext(tdReport, ReportHelper.getContextGroupName(tdReport), contextString);
    }

    /**
     * build a Map: the key is the context script code(start with context.), the value is the context value.
     *
     * @param contextType
     * @return
     */
    public static Map<String, String> buildContextValuesMap(ContextType contextType) {
        Map<String, String> contextValues = new HashMap<String, String>();
        EList<ContextParameterType> contextParameter = contextType.getContextParameter();
        for (ContextParameterType ctxPara : contextParameter) {
            // specially for Password type.
            contextValues.put(ContextParameterUtils.getNewScriptCode(ctxPara.getName(), LANGUAGE), ctxPara.getRawValue());
        }
        return contextValues;
    }

    /**
     * get the context script code according to the context variable name, for an example: the context variable name is
     * "server", then the context script code is "context.server".
     *
     * @param contextVarName
     * @return
     */
    public static String getContextVarScriptCode(String contextVarName) {
        return ContextParameterUtils.getNewScriptCode(contextVarName, LANGUAGE);
    }

    /**
     * if all the reports have the same output folder return it, else return null.
     *
     * @param reports
     * @return the context var or real string or null
     */
    public static String getOutputFolderFromReports(List<TdReport> reports) {
        String result = null;
        if (reports != null && !reports.isEmpty()) {
            if (reports.size() == 1) {
                TdReport report = reports.get(0);
                String ofName = ReportHelper.getOutputFolderNameAssinged(report);
                if (StringUtils.isNotBlank(ofName) && StringUtils.isNotBlank(ContextHelper.getReportContextValue(report, ofName))) {
                    result = ofName;
                }
            } else {
                boolean isContextMode = true;
                String contextVar = null;
                Set<String> temp = new HashSet<String>();
                for (TdReport report : reports) {
                    String ofNameAssinged = ReportHelper.getOutputFolderNameAssinged(report);
                    // if there exist the report which don't set the output folder, this mean use default output folder,
                    // so just return null
                    if (StringUtils.isBlank(ofNameAssinged)
                            || StringUtils.isBlank(ContextHelper.getReportContextValue(report, ofNameAssinged))) {
                        return null;
                    }
                    contextVar = ofNameAssinged;
                    if (isContextMode) {
                        isContextMode = ContextHelper.isContextVar(ofNameAssinged);
                    }
                    String ofName = ContextHelper.getReportContextValue(report, ofNameAssinged);
                    if (StringUtils.isNotBlank(ofName)) {
                        temp.add(ofName);
                    }
                }
                if (temp.size() == 1) {
                    if (isContextMode) {
                        result = contextVar;
                    } else {
                        result = temp.iterator().next();
                    }
                }
            }
        }
        return result;
    }

    /**
     * if the report use any context variable return true, else return false.
     *
     * @param tdReport the report using context or not
     * @return
     */
    public static boolean usingContext(TdReport tdReport) {
        String of = ReportHelper.getOutputFolderNameAssinged(tdReport);
        String logo = tdReport.getLogo();
        String host = ReportHelper.getHost(tdReport);
        String port = ReportHelper.getPort(tdReport);
        String warehouse = ReportHelper.getWarehouse(tdReport);
        String sid = ReportHelper.getDbName(tdReport);
        String schema = ReportHelper.getSchema(tdReport);
        String user = ReportHelper.getUser(tdReport);
        String password = ReportHelper.getPassword(tdReport);
        String paramter = ReportHelper.getParameter(tdReport);
        return isContextVar(of) || isContextVar(logo) || isContextVar(host) || isContextVar(port)
                || isContextVar(warehouse) || isContextVar(sid)
                || isContextVar(schema) || isContextVar(user) || isContextVar(password) || isContextVar(paramter);
    }

    /**
     * in ctxString, replace the context variable name with the value from contextValues .
     *
     * @param contextualizeUrl the string which contain several context variable names
     * @param contextValues the context values map, key=ContextScriptCode, value=ContextValue
     * @return the final string of url which is context mode
     */
    public static String getUrlWithoutContext(String contextualizeUrl, Map<String, String> contextValues) {
        String result = contextualizeUrl;
        for (String key : contextValues.keySet()) {
            // msjian: fix a wrong replacement bug when have two keys as follow(one is a part of another):
            // for example: context.mysql_context_Port and context.mysql_context_Port_dataMart.
            // jdbc:mysql://context.rep_Server:context.rep_Port/context.rep_Database?context.rep_AdditionalParams
            // host
            result = StringUtils.replace(result, "//" + key + ":", "//" + contextValues.get(key) + ":"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            result = StringUtils.replace(result, "@" + key + ":", "@" + contextValues.get(key) + ":"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            // port
            result = StringUtils.replace(result, ":" + key + "/", ":" + contextValues.get(key) + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            result = StringUtils.replace(result, ":" + key + ":", ":" + contextValues.get(key) + ":"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            // Database
            result = StringUtils.replace(result, "/" + key + "?", "/" + contextValues.get(key) + "?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

            // AdditionalParams
            result = StringUtils.replace(result, "?" + key, "?" + contextValues.get(key)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // use normal replace to handle the left. for example: when oracle sid url
        for (String key : contextValues.keySet()) {
            result = StringUtils.replace(result, key, contextValues.get(key));
        }

        return result;
    }

    /**
     * return default ContextType of the ContextItem, if ContextItem is null return null.
     *
     * @param contextItem
     * @return the default ContextType of the ContextItem
     */
    public static ContextType getDefaultContextType(ContextItem contextItem) {
        ContextType contextType = null;
        if (contextItem != null) {
            EList<ContextType> contexts = contextItem.getContext();
            for (ContextType ct : contexts) {
                if (ct.getName().equals(contextItem.getDefaultContext())) {
                    contextType = ct;
                    break;
                }
            }
        }
        return contextType;
    }

    /**
     * get DataFilter Without Context.
     *
     * @param analysis
     * @return
     */
    public static String getDataFilterWithoutContext(Analysis analysis) {
        return getDataFilterWithoutContext(analysis, 0);
    }

    /**
     * get DataFilter Without Context.
     *
     * @param analysis
     * @param index 0 for DataFilterA, 1 for DataFilterB
     * @return
     */
    public static String getDataFilterWithoutContext(Analysis analysis, int index) {
        String dataFilter = AnalysisHelper.getStringDataFilter(analysis, index);
        return ContextHelper.getAnalysisContextValue(analysis, dataFilter);
    }

    /**
     * compare the oldvalue with the : deleted/renamed context parameter: - if it is deleted, return the context
     * parameter's value - if it is renamed, return the new name
     *
     * @param eList context list
     * @param contextManager
     * @param old name of the parameter
     */
    public static String getChangedValue(EList<ContextType> contexts, JobContextManager contextManager,
            String oldValue) {
        String newParamName = checkRenamedContextParameter(contextManager, oldValue);
        String paramName = removeContextPreffix(oldValue);
        if (StringUtils.isNotEmpty(newParamName)) {
            return newParamName;
        }
        // if delete parameter
        Set<String> removedParameters = contextManager.getLostParameters();
        if (removedParameters.contains(paramName)) {
            return ContextHelper.getContextValue(contexts, contextManager.getDefaultContext().getName(),
                    ContextHelper.addContetPreffix(paramName));
        }
        return null;
    }

    public static String checkRenamedContextParameter(JobContextManager contextManager, String paramNameWithContext) {
        Map<String, String> nameMap = contextManager.getNameMap();

        return checkRenamedContextParameter(nameMap, paramNameWithContext);
    }

    public static String checkRenamedContextParameter(Map<String, String> nameMap, String paramNameWithContext) {
        String paramName = removeContextPreffix(paramNameWithContext);
        // if renamed parameter
        if (nameMap != null && nameMap.size() > 0) {
            for (Map.Entry<String, String> entry : nameMap.entrySet()) {
                if (paramName.equals(entry.getValue())) {
                    return addContetPreffix(entry.getKey());
                }
            }
        }
        return null;
    }

    /**
     * if the string contains the context preffix, remove it and return the parameter name only.
     *
     * @param strWithContext
     * @return
     */
    public static String removeContextPreffix(String strWithContext) {
        if (!ContextHelper.isContextVar(strWithContext)) {
            return strWithContext;
        }
        return strWithContext.substring(CONTEXT_PREFFIX.length(),
                strWithContext.length());
    }

    public static String addContetPreffix(String paramName) {
        return paramName == null ? null : CONTEXT_PREFFIX + paramName;
    }

    public static boolean isDQSupportContextItem(Item item) {
        return item instanceof TDQAnalysisItem || item instanceof TDQReportItem || item instanceof ConnectionItem;
    }
    public static EList<ContextType> getAllContextType(Item item) {
        // here is some related methods on DQ side
        // more can refer to ContextUtils
        if (item instanceof TDQAnalysisItem) {
            TDQAnalysisItem tdqAnalysisItem = (TDQAnalysisItem) item;
            if (tdqAnalysisItem.getAnalysis() != null) {
                return tdqAnalysisItem.getAnalysis().getContextType();
            }
        } else if (item instanceof TDQReportItem) {
            TDQReportItem tdqReportItem = (TDQReportItem) item;
            if (tdqReportItem.getReport() != null) {
                return ((TdReport) tdqReportItem.getReport()).getContext();
            }
        }
        return ContextUtils.getAllContextType(item);
    }

    public static Map<String, String> getContextParamterRenamedMap(Item item) {
        ItemContextLink itemContextLink = null;
        try {
            itemContextLink = ContextLinkService.getInstance().loadContextLinkFromJson(item);
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        }
        if (itemContextLink != null) {
            if (item instanceof TDQAnalysisItem || item instanceof TDQReportItem) {
                return compareContextParamName(item, itemContextLink);
            }
        }

        return ContextUtils.getContextParamterRenamedMap(item);
    }

    private static Map<String, String> compareContextParamName(Item item, ItemContextLink itemContextLink) {
        List<ContextType> contextTypeList = getAllContextType(item);
        return ContextUtils.compareContextParamName(contextTypeList, itemContextLink);
    }

    public static Connection getPromptContextValuedConnection(Connection connection) {
        Connection copyConnection = MetadataConnectionUtils.deepCopyAll(connection);
        JavaSqlFactory.setPromptContextValues(copyConnection);
        return copyConnection;
    }

}
