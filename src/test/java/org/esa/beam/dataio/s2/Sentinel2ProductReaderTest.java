package org.esa.beam.dataio.s2;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public class Sentinel2ProductReaderTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, IllegalAccessException, InstantiationException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new Sentinel2ProductReader(new Sentinel2ProductReaderPlugIn()).readProductNodes("", null);
    }
}
