/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.dat.toolviews.Projects;

import org.esa.snap.ui.UIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.Enumeration;
import java.util.Observer;
import java.util.Vector;

/**
 * The tool window which displays the current project
 */
@TopComponent.Description(
        preferredID = "ProjectsToolView",
        iconBase = "org/esa/s1tbx/dat/icons/project-view.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "explorer",
        openAtStartup = false,
        position = 4
)
@ActionID(category = "Window", id = "org.esa.s1tbx.dat.toolviews.Projects.ProjectsToolView")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Toolbars/Projects", position = 10)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProjectsToolView_Name",
        preferredID = "ProjectsToolView"
)
@NbBundle.Messages({
        "CTL_ProjectsToolView_Name=View Projects",
        "CTL_ProjectsToolView_HelpId=projects"
})
public class ProjectsToolView extends TopComponent implements Observer {

    private ProjectTree projectTree;
    private DefaultMutableTreeNode rootNode;
    private final Project project = Project.instance();

    public ProjectsToolView() {
        setDisplayName("Projects");

        setLayout(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        add(createControl(), BorderLayout.CENTER);
    }

    public JComponent createControl() {
        Project.instance().addObserver(this);

        final JScrollPane prjScrollPane = new JScrollPane(createTree());
        prjScrollPane.setPreferredSize(new Dimension(320, 480));
        prjScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        prjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        prjScrollPane.setBorder(null);
        prjScrollPane.setViewportBorder(null);

        //final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        //splitPane.add(prjScrollPane);

        return prjScrollPane;
    }

    private ProjectTree createTree() {
        rootNode = new DefaultMutableTreeNode("");
        projectTree = new ProjectTree(false);//rootNode);
        projectTree.populateTree(rootNode);
        projectTree.setRootVisible(false);
        projectTree.setShowsRootHandles(true);
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) projectTree.getCellRenderer();
        renderer.setLeafIcon(UIUtils.loadImageIcon("/org/esa/s1tbx/resources/images/icons/RsBandAsSwath16.gif"));
        renderer.setClosedIcon(UIUtils.loadImageIcon("/org/esa/s1tbx/resources/images/icons/RsGroupClosed16.gif"));
        renderer.setOpenIcon(UIUtils.loadImageIcon("/org/esa/s1tbx/resources/images/icons/RsGroupOpen16.gif"));
        return projectTree;
    }

    private static void PopulateNode(Vector<ProjectSubFolder> subFolders, DefaultMutableTreeNode treeNode) {

        for (Enumeration e = subFolders.elements(); e.hasMoreElements(); ) {
            final ProjectSubFolder folder = (ProjectSubFolder) e.nextElement();

            final DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(folder);
            treeNode.add(folderNode);

            final Vector<ProjectFile> fileList = folder.getFileList();
            for (Enumeration file = fileList.elements(); file.hasMoreElements(); ) {
                final DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file.nextElement());
                folderNode.add(fileNode);
            }

            final Vector<ProjectSubFolder> moreFolders = folder.getSubFolders();
            if (!moreFolders.isEmpty())
                PopulateNode(moreFolders, folderNode);
        }
    }

    /**
     * Implements the functionality of Observer participant of Observer Design Pattern to define a one-to-many
     * dependency between a Subject object and any number of Observer objects so that when the
     * Subject object changes state, all its Observer objects are notified and updated automatically.
     * <p>
     * Defines an updating interface for objects that should be notified of changes in a subject.
     *
     * @param subject The Observerable subject
     * @param data    optional data
     */
    public void update(java.util.Observable subject, java.lang.Object data) {

        rootNode.removeAllChildren();

        final ProjectSubFolder projectFolders = project.getProjectSubFolders();
        if (projectFolders == null) {
            projectTree.setRootVisible(false);
            projectTree.populateTree(rootNode);
        } else {
            rootNode.setUserObject(project.getProjectSubFolders());
            projectTree.setRootVisible(true);

            final Vector<ProjectSubFolder> subFolders = project.getProjectSubFolders().getSubFolders();
            PopulateNode(subFolders, rootNode);
            projectTree.populateTree(rootNode);
        }
    }
}
