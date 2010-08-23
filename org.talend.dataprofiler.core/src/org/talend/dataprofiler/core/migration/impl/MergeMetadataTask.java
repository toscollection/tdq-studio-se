// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.migration.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.talend.commons.bridge.ReponsitoryContextBridge;
import org.talend.commons.emf.FactoriesUtil;
import org.talend.dataprofiler.core.migration.AWorkspaceTask;
import org.talend.model.migration.TopMetadataMigrationFrom400to410usingGenericVM;
import org.talend.resource.ResourceManager;

/**
 * DOC xqliu class global comment. Detailled comment
 */
public class MergeMetadataTask extends AWorkspaceTask {

    private static Logger log = Logger.getLogger(MergeMetadataTask.class);

    private static final String MIGRATION_FILE_EXT = ".mig";

    private static final String MIGRATION_FOLDER_EXT = "_mig";

    private static final String TDQ_METADATA = "TDQ_Metadata";

    /**
     * replace strings for update ana file from 400 to 410.
     */
    private Map<String, String> replaceStringMapAna;

    public Map<String, String> getReplaceStringMapAna() {
        if (this.replaceStringMapAna == null) {
            this.replaceStringMapAna = initReplaceStringMapAna();
        }
        return this.replaceStringMapAna;
    }

    /**
     * DOC init the replace strings for update ana file from 400 to 410: six strings need to be replaced.
     * 
     * @return
     */
    private Map<String, String> initReplaceStringMapAna() {
        Map<String, String> result = new HashMap<String, String>();
        // TODO need to be refacted to get more readable codes
        // TdTable TdView TdColumn TdXMLElement use "xmlns:relational" , Catalog Schema use
        // "xmlns:orgomg.cwm.resource.relational", but there are not "xmlns:orgomg.cwm.resource.relational" in the .ana
        // file so we replate "xmlns:relational" with "xmlns:relational" and "xmlns:orgomg.cwm.resource.relational"
        result.put("xmlns:relational=\"http:///org/talend/cwm/resource.relational\"",
                "xmlns:relational=\"http://www.talend.org/cwm/resource/relational/2010\""
                        + " xmlns:orgomg.cwm.resource.relational=\"http:///orgomg/cwm/resource/relational.ecore\"");
        // ~~~
        result.put("xmlns:org.talend.cwm.xml=\"http:///org/talend/cwm/resource.xml\"",
                "xmlns:org.talend.cwm.xml=\"http://www.talend.org/cwm/resource/xml/2010\"");
        result.put("xmlns:softwaredeployment=\"http:///org.talend/cwm/foundation.softwaredeployment\"",
                "xmlns:TalendMetadata=\"http://www.talend.org/metadata/connection/2010\"");
        result.put("softwaredeployment:TdDataProvider", "TalendMetadata:DatabaseConnection");
        result.put("relational:TdCatalog", "orgomg.cwm.resource.relational:Catalog");
        result.put("relational:TdSchema", "orgomg.cwm.resource.relational:Schema");
        return result;
    }

    public void setReplaceStringMapAna(Map<String, String> replaceStringMapAna) {
        this.replaceStringMapAna = replaceStringMapAna;
    }

    /**
     * replace strings for update rule file from 400 to 410.
     */
    private Map<String, String> replaceStringMapRules;

    public Map<String, String> getReplaceStringMapRules() {
        if (this.replaceStringMapRules == null) {
            this.replaceStringMapRules = initReplaceStringMapRules();
        }
        return this.replaceStringMapRules;
    }

    /**
     * DOC init the replace strings for update rule file from 400 to 410: only one string need to be replaced.
     * 
     * @return
     */
    private Map<String, String> initReplaceStringMapRules() {
        Map<String, String> result = new HashMap<String, String>();
        result.put("xmlns:relational=\"http:///org/talend/cwm/resource.relational\"",
                "xmlns:relational=\"http://www.talend.org/cwm/resource/relational/2010\"");
        return result;
    }

    public void setReplaceStringMapRules(Map<String, String> replaceStringMapRules) {
        this.replaceStringMapRules = replaceStringMapRules;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.migration.IWorkspaceMigrationTask#getMigrationTaskType()
     */
    public MigrationTaskType getMigrationTaskType() {
        return MigrationTaskType.FILE;
    }

    /**
     * DOC Use replace method to migrate from 400 to 410.
     * 
     * @param result
     * @param migFolder
     * @param acceptFileExtentionNames
     * @param replaceStringMap
     * @return
     */
    private boolean migrateFolder(boolean result, File migFolder, final String[] acceptFileExtentionNames,
            Map<String, String> replaceStringMap) {

        ArrayList<File> fileList = new ArrayList<File>();
        getAllFilesFromFolder(migFolder, fileList, new FilenameFilter() {

            public boolean accept(File dir, String name) {
                for (String extName : acceptFileExtentionNames) {
                    if (name.endsWith(extName)) {
                        return true;
                    }
                }
                return false;
            }
        });
        log.info("-------------- Migrating " + fileList.size() + " files");

        int counter = 0;
        int errorCounter = 0;
        Throwable error = null;

        for (File sample : fileList) {
            log.info("-------------- Migrating (" + counter++ + ") : " + sample.getAbsolutePath());
            try {
                BufferedReader fileReader = new BufferedReader(new FileReader(sample));
                BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File(sample.getAbsolutePath()
                        + MIGRATION_FILE_EXT)));

                while (fileReader.ready()) {
                    String line = fileReader.readLine();
                    for (String key : replaceStringMap.keySet()) {
                        line = line.replaceAll(key, replaceStringMap.get(key));
                    }
                    fileWriter.append(line);
                    fileWriter.newLine();
                }

                fileWriter.flush();
                fileWriter.close();
                fileWriter = null;
                fileReader.close();
                fileReader = null;
                System.gc();
            } catch (Exception e) {
                error = e;
                errorCounter++;
                log.error("!!!!!!!!!!!  Error transforming (" + sample.getAbsolutePath() + ")\n" + e.getMessage(), e);
            }
            log.info("-------------- Migration done of " + counter + " files"
                    + (errorCounter != 0 ? (",  there are " + errorCounter + " files in error.") : "."));
        }

        if (error != null) {
            result = false;
        } else {
            // remove original files and rename new ones to old ones
            for (File sample : fileList) {
                boolean isDeleted = sample.delete();
                log.info(sample.getAbsolutePath() + (isDeleted ? " is deleted." : " failed to delete."));
                boolean isrenamed = new File(sample.getAbsolutePath() + MIGRATION_FILE_EXT).renameTo(sample); //$NON-NLS-1$
                log.info(sample.getAbsolutePath() + MIGRATION_FILE_EXT + (isrenamed ? " is renamed." : " failed to rename."));
            }
        }

        return result;
    }

    /**
     * DOC Use ATL transformation rules to migrate from 400 to 410.
     * 
     * @param result
     * @param rename
     * @param migFileMetadata
     * @param metadata400to410
     * @param final String[] acceptFileExtentionNames
     * @return
     */
    private boolean migrateFolder(boolean result, boolean rename, File migFileMetadata,
            TopMetadataMigrationFrom400to410usingGenericVM metadata400to410, final String[] acceptFileExtentionNames) {

        ArrayList<File> fileList = new ArrayList<File>();
        getAllFilesFromFolder(migFileMetadata, fileList, new FilenameFilter() {

            public boolean accept(File dir, String name) {
                for (String extName : acceptFileExtentionNames) {
                    if (name.endsWith(extName)) {
                        return true;
                    }
                }
                return false;
            }
        });
        log.info("-------------- Migrating " + fileList.size() + " files");

        int counter = 0;
        int errorCounter = 0;
        Throwable error = null;

        for (File sample : fileList) {
            log.info("-------------- Migrating (" + counter++ + ") : " + sample.getAbsolutePath());
            try {
                String inURI = sample.toURI().toString();
                String outURI = new File(sample.getAbsolutePath() + MIGRATION_FILE_EXT).toURI().toString();
                metadata400to410.migrate(inURI, outURI, new NullProgressMonitor());
            } catch (Exception e) {
                error = e;
                errorCounter++;
                log.error("!!!!!!!!!!!  Error transforming (" + sample.getAbsolutePath() + ")\n" + e.getMessage(), e);
            }
            log.info("-------------- Migration done of " + counter + " files"
                    + (errorCounter != 0 ? (",  there are " + errorCounter + " files in error.") : "."));
        }

        if (error != null) {
            result = false;
        } else {
            if (rename) {
                // remove original files and rename new ones to old ones
                for (File sample : fileList) {
                    boolean isDeleted = sample.delete();
                    log.info(sample.getAbsolutePath() + (isDeleted ? " is deleted." : " failed to delete."));
                    boolean isrenamed = new File(sample.getAbsolutePath() + MIGRATION_FILE_EXT).renameTo(sample); //$NON-NLS-1$
                    log.info(sample.getAbsolutePath() + MIGRATION_FILE_EXT + (isrenamed ? " is renamed." : " failed to rename."));
                }
            }
        }
        return result;
    }

    /**
     * DOC sgandon Comment method "getAllFilesFromFolder".
     * 
     * @param sampleFolder
     * @param arrayList
     * @param filenameFilter
     */
    private void getAllFilesFromFolder(File sampleFolder, ArrayList<File> fileList, FilenameFilter filenameFilter) {
        File[] folderFiles = sampleFolder.listFiles(filenameFilter);
        Collections.addAll(fileList, folderFiles);
        File[] allFolders = sampleFolder.listFiles(new FileFilter() {

            public boolean accept(File arg0) {
                return arg0.isDirectory();
            }
        });
        for (File folder : allFolders) {
            getAllFilesFromFolder(folder, fileList, filenameFilter);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.migration.IMigrationTask#getOrder()
     */
    public Date getOrder() {
        return createDate(2010, 6, 22);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.migration.AMigrationTask#doExecute()
     */
    @Override
    protected boolean doExecute() throws Exception {
        boolean result = true;
        boolean rename = true;
        File rawFileMetadata = new File(ReponsitoryContextBridge.getRootProject().getFolder(new Path(TDQ_METADATA))
                .getRawLocationURI());
        File migFileMetadata = new File(rawFileMetadata.getParentFile(), rawFileMetadata.getName() + MIGRATION_FOLDER_EXT);

        File rawFileDataProfiling = new File(ResourceManager.getDataProfilingFolder().getRawLocationURI());
        File migFileDataProfiling = new File(rawFileDataProfiling.getParentFile(), rawFileDataProfiling.getName()
                + MIGRATION_FOLDER_EXT);

        File rawFileLibraries = new File(ResourceManager.getLibrariesFolder().getRawLocationURI());
        File migFileLibraries = new File(rawFileLibraries.getParentFile(), rawFileLibraries.getName() + MIGRATION_FOLDER_EXT);

        try {
            result = rawFileMetadata.renameTo(migFileMetadata) && rawFileDataProfiling.renameTo(migFileDataProfiling)
                    && rawFileLibraries.renameTo(migFileLibraries);

            if (result) {
                // migrate the folder "TDQ_Metadata" for ".prv" files and
                // "TDQ_Libraries" for ".softwaresystem.softwaredeployment" files
                String[] metadataFileExtentionNames = { FactoriesUtil.ITEM_EXTENSION, ".softwaresystem.softwaredeployment" };
                File[] migrateFolderList = { migFileMetadata, migFileLibraries };
                TopMetadataMigrationFrom400to410usingGenericVM metadata400to410 = new TopMetadataMigrationFrom400to410usingGenericVM();
                for (File migrateFile : migrateFolderList) {
                    result = migrateFolder(result, rename, migrateFile, metadata400to410, metadataFileExtentionNames);
                }
                // migrate the folder "TDQ_Data Profiling" for ".ana" files and
                // "TDQ_Libraries" for ".rules" file
                String[] anaFileExtentionNames = { FactoriesUtil.ANA };
                String[] rulesFileExtentionNames = { FactoriesUtil.DQRULE };
                result = migrateFolder(result, migFileDataProfiling, anaFileExtentionNames, this.getReplaceStringMapAna())
                        && migrateFolder(result, migFileLibraries, rulesFileExtentionNames, this.getReplaceStringMapRules());
            }
        } catch (Exception e) {
            result = false;
            log.error(e, e);
        } finally {
            try {
                result = migFileMetadata.renameTo(rawFileMetadata) && migFileDataProfiling.renameTo(rawFileDataProfiling)
                        && migFileLibraries.renameTo(rawFileLibraries);
            } catch (Exception e) {
                log.error(e);
            }
        }
        return result;
    }
}
