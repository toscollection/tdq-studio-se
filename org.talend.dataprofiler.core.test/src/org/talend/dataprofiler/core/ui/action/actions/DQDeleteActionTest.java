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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.navigator.CommonViewer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.ItemState;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.repository.i18n.Messages;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.dialog.message.DeleteModelElementConfirmDialog;
import org.talend.dataprofiler.core.ui.views.DQRespositoryView;
import org.talend.dq.helper.EObjectHelper;
import org.talend.dq.helper.PropertyHelper;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.dq.nodes.RecycleBinRepNode;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;

/**
 * DOC qionli class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 55206 2011-02-15 17:32:14Z mhirt $
 * 
 */
// @RunWith(PowerMockRunner.class)
@PrepareForTest({ CorePlugin.class, RepositoryNodeHelper.class, CoreRuntimePlugin.class, IProxyRepositoryFactory.class,
        PropertyHelper.class, DeleteModelElementConfirmDialog.class, EObjectHelper.class,
        org.talend.core.repository.i18n.Messages.class, ResourceBundle.class, DefaultMessagesImpl.class })
public class DQDeleteActionTest {

    @Rule
    public PowerMockRule powerMockRule = new PowerMockRule();

    private DQDeleteAction dqDeleteAction_real;

    private DQDeleteAction deleteAction_mock;

    private CorePlugin corePlugin;

    /**
     * DOC qiongli Comment method "setUp".
     * 
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        corePlugin = mock(CorePlugin.class);
        PowerMockito.mockStatic(CorePlugin.class);
        when(CorePlugin.getDefault()).thenReturn(corePlugin);
        ResourceBundle rb2 = mock(ResourceBundle.class);
        stub(method(ResourceBundle.class, "getBundle", String.class)).toReturn(rb2);
        PowerMockito.mockStatic(Messages.class);
        when(Messages.getString(anyString())).thenReturn("aa");
        PowerMockito.mock(DefaultMessagesImpl.class);
        when(DefaultMessagesImpl.getString(anyString())).thenReturn("bb").thenReturn("cc").thenReturn("dd").thenReturn("ee");
        dqDeleteAction_real = new DQDeleteAction();
        deleteAction_mock = PowerMockito.spy(dqDeleteAction_real);
        PowerMockito.doReturn(null).when(deleteAction_mock).getActivePage();
        PowerMockito.doNothing().when(deleteAction_mock, "doRun");

        // mock/stub something for superclass AContextualAction.run.
        PowerMockito.mockStatic(CoreRuntimePlugin.class);
        CoreRuntimePlugin coreRuntPlugin = mock(CoreRuntimePlugin.class);
        when(CoreRuntimePlugin.getInstance()).thenReturn(coreRuntPlugin);
        IProxyRepositoryFactory proxFactory = mock(IProxyRepositoryFactory.class);
        when(coreRuntPlugin.getProxyRepositoryFactory()).thenReturn(proxFactory);
    }

    /**
     * DOC Administrator Comment method "tearDown".
     * 
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method run for logical delete.
     * 
     * @throws Exception
     */
    @Test
    public void testRun_1() throws Exception {
        dqDeleteAction_real.setCurrentNode(null);
        DQRespositoryView dqView = mock(DQRespositoryView.class);
        when(corePlugin.getRepositoryView()).thenReturn(dqView);
        CommonViewer commonView = mock(CommonViewer.class);
        List<RepositoryNode> seleLs = new ArrayList<RepositoryNode>();
        RepositoryNode node1 = mock(RepositoryNode.class);
        IRepositoryObject object = mock(IRepositoryObject.class);
        Property prop = mock(Property.class);
        Item item = mock(Item.class);
        ItemState state = mock(ItemState.class);
        when(prop.getItem()).thenReturn(item);
        when(node1.getObject()).thenReturn(object);
        when(object.getProperty()).thenReturn(prop);
        when(item.getState()).thenReturn(state);
        when(state.isDeleted()).thenReturn(false);
        seleLs.add(node1);

        ISelection selecetion = new StructuredSelection(seleLs);
        when(dqView.getCommonViewer()).thenReturn(commonView);
        when(commonView.getSelection()).thenReturn(selecetion);
        assertNotNull(dqDeleteAction_real.getSelection());

        stub(method(RepositoryNodeHelper.class, "isStateDeleted")).toReturn(false);
        RecycleBinRepNode recyBinNode = mock(RecycleBinRepNode.class);
        stub(method(RepositoryNodeHelper.class, "getRecycleBinRepNode")).toReturn(recyBinNode);

        deleteAction_mock.run();

    }

    /**
     * dqDeleteAction_real.currentNode==null means logical delete.
     */
    @Test
    public void testGetSelection_1() {
        dqDeleteAction_real.setCurrentNode(null);
        PowerMockito.mockStatic(CorePlugin.class);
        when(CorePlugin.getDefault()).thenReturn(corePlugin);
        DQRespositoryView dqView = mock(DQRespositoryView.class);
        when(corePlugin.getRepositoryView()).thenReturn(dqView);
        CommonViewer commonView = mock(CommonViewer.class);
        List<RepositoryNode> seleLs = new ArrayList<RepositoryNode>();
        seleLs.add(mock(RepositoryNode.class));
        seleLs.add(mock(RepositoryNode.class));
        ISelection selecetion = new StructuredSelection(seleLs);
        when(dqView.getCommonViewer()).thenReturn(commonView);
        when(commonView.getSelection()).thenReturn(selecetion);
        assertNotNull(dqDeleteAction_real.getSelection());

    }

    /**
     * 
     * dqDeleteAction_real.currentNode!=null means physical delete.
     */
    @Test
    public void testGetSelection_2() {
        // dqDeleteAction_real.currentNode==null means logical delete.
        RepositoryNode repNode = mock(RepositoryNode.class);
        dqDeleteAction_real.setCurrentNode(repNode);
        assertNotNull(dqDeleteAction_real.getSelection());

    }

    /**
     * Test method for
     * {@link org.talend.dataprofiler.core.ui.action.actions.dqDeleteAction_real#getRepositorySelection()}.
     */
    @Test
    public void testGetRepositorySelection() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.talend.dataprofiler.core.ui.action.actions.dqDeleteAction_real#init(org.eclipse.jface.viewers.TreeViewer, org.eclipse.jface.viewers.IStructuredSelection)}
     * .
     */
    @Test
    public void testInit() {
        // fail("Not yet implemented");
    }

}
