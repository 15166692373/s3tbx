/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.classification.rcp.wizards.TerrainFlattenedClassification;

import org.esa.s1tbx.dat.wizards.AbstractInstructPanel;
import org.esa.s1tbx.dat.wizards.WizardPanel;

/**
 * Instructions Panel
 */
public class TerrainFlattenedWizardInstructPanel extends AbstractInstructPanel {

    public final static String title = "Terrain Flattened Classification Wizard";

    public TerrainFlattenedWizardInstructPanel() {
        super(title);
        imgPosX = 100;
        imgPosY = 240;
    }

    public WizardPanel getNextPanel() {
        return new TerrainFlattenedWizardInputPanel();
    }

    protected String getDescription() {
        return "Welcome to the Terrain Flattened Classification Wizard.\n\n" +
                "With this wizard you will be able classify a fully polarimetric product.";
    }

    protected String getInstructions() {
        return "Step 1: Select a Quad Pol SLC product\n\n" +
                "Step 2: Create a T3 matrix, Terrain Flatten and Terrain Correct\n\n" +
                "Step 3: Create an unsupervised classification\n\n";
    }
}
