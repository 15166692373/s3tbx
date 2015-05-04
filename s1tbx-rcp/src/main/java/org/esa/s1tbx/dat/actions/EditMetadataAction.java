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
package org.esa.s1tbx.dat.actions;

import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.framework.ui.command.ExecCommand;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.visat.VisatApp;

/**
 * This action to edit Metadata
 *
 * @author lveci
 * @version $Revision: 1.7 $ $Date: 2011-04-08 18:23:59 $
 */
public class EditMetadataAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {

        final Product product = SnapApp.getDefault().getSelectedProduct();
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        if (absRoot != null) {
            //createProductMetadataView(absRoot);
        } else {
            // no attributes found
            VisatApp.getApp().showErrorDialog("Edit Metadata", "No editable metadata found.");
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        final int n = SnapApp.getDefault().getProductManager().getProductCount();
        setEnabled(n > 0);
    }

    /**
     * Creates a new product metadata view and opens an internal frame for it.
     */
   /* public synchronized static ProductMetadataView createProductMetadataView(final MetadataElement element) {
        final ShowMetadataViewAction command = (ShowMetadataViewAction) VisatApp.getApp().getCommandManager().getCommand(
                ShowMetadataViewAction.ID);
        return command.openMetadataView(element);
    }*/
}
