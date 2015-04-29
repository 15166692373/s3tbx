package org.jlinda.nest.dat;

import org.esa.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.visat.actions.AbstractVisatAction;

import java.io.File;

public class StitchAction extends AbstractVisatAction {
    
    @Override
    public void actionPerformed(CommandEvent event) {

        final GraphBuilderDialog dialog = new GraphBuilderDialog(new SnapApp.SnapContext(),
                "Stitch Unwrapped Tiles", "StitchOp", false);
        dialog.show();

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        final File graphFile =  new File(graphPath, "StitchTileGraph.xml");

        dialog.LoadGraph(graphFile);
    }
    
}
