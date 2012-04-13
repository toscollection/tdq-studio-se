// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.action.actions;

import static org.mockito.Mockito.*;
import static org.powermock.api.support.membermodification.MemberMatcher.*;
import static org.powermock.api.support.membermodification.MemberModifier.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dq.helper.DQDeleteHelper;
import org.talend.dq.helper.PropertyHelper;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.dq.nodes.RecycleBinRepNode;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryNode;

/**
 * DOC qiongli class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 55206 2011-02-15 17:32:14Z mhirt $
 * 
 */
// @RunWith(PowerMockRunner.class)
@PrepareForTest({ CorePlugin.class, DQDeleteHelper.class, IProxyRepositoryFactory.class, PropertyHelper.class,
        MessageDialog.class, ProxyRepositoryFactory.class, RepositoryNodeHelper.class, CoreRuntimePlugin.class })
public class DQEmptyRecycleBinActionTest {

    @Rule
    public PowerMockRule powerMockRule = new PowerMockRule();

    private DQEmptyRecycleBinAction dqEmptyAction_real;

    private DQEmptyRecycleBinAction dqEmptyAction_mock;

    private CorePlugin corePlugin;

    /**
     * DOC qiongli Comment method "setUp".
     * 
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        dqEmptyAction_real = new DQEmptyRecycleBinAction();
        dqEmptyAction_mock = PowerMockito.spy(dqEmptyAction_real);
        PowerMockito.doReturn(null).when(dqEmptyAction_mock).getActivePage();
        PowerMockito.doReturn(null).when(dqEmptyAction_mock, "getShell");

        corePlugin = mock(CorePlugin.class);
        PowerMockito.mockStatic(CorePlugin.class);
        when(CorePlugin.getDefault()).thenReturn(corePlugin);

        // mock/stub something for superclass AContextualAction.run.
        PowerMockito.mockStatic(CoreRuntimePlugin.class);
        CoreRuntimePlugin coreRuntPlugin = mock(CoreRuntimePlugin.class);
        when(CoreRuntimePlugin.getInstance()).thenReturn(coreRuntPlugin);
        IProxyRepositoryFactory proxFactory = mock(IProxyRepositoryFactory.class);
        when(coreRuntPlugin.getProxyRepositoryFactory()).thenReturn(proxFactory);

    }

    /**
     * DOC qiongli Comment method "tearDown".
     * 
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.talend.dataprofiler.core.ui.action.actions.DQEmptyRecycleBinAction#run()}.
     * 
     * there is not any dependcy for each node in recycle bin
     * 
     * @throws Exception
     */
    @Test
    public void testRun_1() throws Exception {
        PowerMockito.doNothing().when(dqEmptyAction_mock, "doRun");
        IRepositoryNode node1 = mock(IRepositoryNode.class);
        IRepositoryNode node2 = mock(IRepositoryNode.class);
        IRepositoryObject object1 = mock(IRepositoryObject.class);
        when(node1.getObject()).thenReturn(object1);
        when(node2.getObject()).thenReturn(object1);
        Property prop = mock(Property.class);
        when(object1.getProperty()).thenReturn(prop);

        List<IRepositoryNode> seleLs = new ArrayList<IRepositoryNode>();
        ISelection selecetion = new StructuredSelection(seleLs);
        PowerMockito.doReturn(selecetion).when(dqEmptyAction_mock).getSelection();
        seleLs.add(node2);
        seleLs.add(node1);

        stub(method(MessageDialog.class, "openQuestion")).toReturn(true);
        PowerMockito.mockStatic(ProxyRepositoryFactory.class);
        ProxyRepositoryFactory factory = mock(ProxyRepositoryFactory.class);
        when(ProxyRepositoryFactory.getInstance()).thenReturn(factory);
        stub(method(DQDeleteHelper.class, "deleteRelations"));

        RecycleBinRepNode recyBin = mock(RecycleBinRepNode.class);
        PowerMockito.mockStatic(RepositoryNodeHelper.class);
        when(RepositoryNodeHelper.getRecycleBinRepNode()).thenReturn(recyBin);
        when(RepositoryNodeHelper.findAllChildrenNodes(seleLs)).thenReturn(seleLs);
        when(recyBin.getChildren()).thenReturn(seleLs);

        dqEmptyAction_mock.run();

    }

    /**
     * Test method for {@link org.talend.dataprofiler.core.ui.action.actions.DQEmptyRecycleBinAction#run()}.
     * 
     * there is a dependce for a node in recycle bin,and this depence is not in recycle bin.
     * 
     * 
     * @throws Exception
     */
    @Test
    public void testRun_2() throws Exception {
        PowerMockito.doNothing().when(dqEmptyAction_mock, "doRun");
        IRepositoryNode node1 = mock(IRepositoryNode.class);
        List<IRepositoryNode> seleLs = new ArrayList<IRepositoryNode>();
        ISelection selecetion = new StructuredSelection(seleLs);
        PowerMockito.doReturn(selecetion).when(dqEmptyAction_mock).getSelection();
        seleLs.add(node1);

        PowerMockito.mockStatic(DQDeleteHelper.class);
        when(DQDeleteHelper.canEmptyRecyBin(seleLs)).thenReturn(false);

        RecycleBinRepNode recyBin = mock(RecycleBinRepNode.class);
        PowerMockito.mockStatic(RepositoryNodeHelper.class);
        when(RepositoryNodeHelper.getRecycleBinRepNode()).thenReturn(recyBin);
        when(RepositoryNodeHelper.findAllChildrenNodes(seleLs)).thenReturn(seleLs);
        when(recyBin.getChildren()).thenReturn(seleLs);

        dqEmptyAction_mock.run();

    }


    /**
     * Test method for {@link org.talend.dataprofiler.core.ui.action.actions.DQEmptyRecycleBinAction#getSelection()}.
     */
    @Test
    public void testGetSelection() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.talend.dataprofiler.core.ui.action.actions.DQEmptyRecycleBinAction#getRepositorySelection()}.
     */
    @Test
    public void testGetRepositorySelection() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.talend.dataprofiler.core.ui.action.actions.DQEmptyRecycleBinAction#doRun()}.
     */
    @Test
    public void testDoRun() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for {@link org.talend.dataprofiler.core.ui.action.actions.DQEmptyRecycleBinAction#DQEmptyRecycleBinAction()}.
     */
    @Test
    public void testDQEmptyRecycleBinAction() {
        // fail("Not yet implemented");
    }

}
