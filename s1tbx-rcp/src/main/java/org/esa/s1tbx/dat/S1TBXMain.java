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
package org.esa.s1tbx.dat;

import com.jidesoft.utils.Lm;
import org.esa.snap.framework.ui.application.ApplicationDescriptor;
import org.esa.snap.visat.VisatApp;
import org.esa.snap.visat.VisatMain;

public class S1TBXMain extends VisatMain {

    @Override
    protected void verifyJideLicense() {
        Lm.verifyLicense("Array", "NEST", "XwckhJCkWG5BO0MOaVM6hjD1jvupU1p");
    }

    @Override
    protected VisatApp createApplication(ApplicationDescriptor applicationDescriptor) {
        return new S1TBXApp(applicationDescriptor);
    }
}
