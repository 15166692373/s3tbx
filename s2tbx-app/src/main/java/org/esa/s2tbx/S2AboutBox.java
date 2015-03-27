package org.esa.s2tbx;

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This class pertains to the "about" dialog box for the application.
 */
public class S2AboutBox extends ModalDialog {

    public S2AboutBox() {
        this(new JButton[]{
//                new JButton(),
                new JButton(),
        });
    }

    private S2AboutBox(JButton[] others) {
        super(VisatApp.getApp().getApplicationWindow(), String.format("About %s", VisatApp.getApp().getAppName()),
                ModalDialog.ID_OK, others, "");    /*I18N*/

        JButton systemButton = others[0];
        systemButton.setText("System Info...");  /*I18N*/
        systemButton.addActionListener(e -> showSystemDialog());

        final ApplicationDescriptor applicationDescriptor = VisatApp.getApp().getApplicationDescriptor();
        String aboutImagePath = applicationDescriptor.getAboutImagePath();
        Icon imageIcon = null;
        if (aboutImagePath != null) {
            aboutImagePath = aboutImagePath.trim();
            URL resource = getClass().getResource(aboutImagePath);
            if (resource != null) {
                imageIcon = new ImageIcon(resource);
            } else {
                BeamLogManager.getSystemLogger().severe("Missing icon resource: " + aboutImagePath);
            }
        }

        JLabel imageLabel = new JLabel(imageIcon);
        JPanel dialogContent = new JPanel(new BorderLayout());
        String versionText = getVersionHtml();
        JLabel versionLabel = new JLabel(versionText);

        JPanel labelPane = new JPanel(new BorderLayout());
        labelPane.add(BorderLayout.NORTH, versionLabel);

        dialogContent.setLayout(new BorderLayout(4, 4));
        dialogContent.add(BorderLayout.WEST, imageLabel);
        dialogContent.add(BorderLayout.EAST, labelPane);

        setContent(dialogContent);
    }

    @Override
    protected void onOther() {
        // override default behaviour by doing nothing
    }

    private void showSystemDialog() {
        final ModalDialog modalDialog = new ModalDialog(getJDialog(), "System Info", ID_OK, null);
        final Object[][] sysInfo = getSystemInfo();
        final JTable sysTable = new JTable(sysInfo, new String[]{"Property", "Value"}); /*I18N*/
        final JScrollPane systemScroll = new JScrollPane(sysTable);
        systemScroll.setPreferredSize(new Dimension(400, 400));
        modalDialog.setContent(systemScroll);
        modalDialog.show();
    }

    private static String getVersionHtml() {
        // todo - load text from resource
        final String pattern = "<html>" +
                "<b>{0} Version {1} ({2})</b>" +
                "<br><b>{3}</b>" +
                "<br>" +
                "<br>This software is based on SNAP, the Sentinels Application Platform." +
                "<br>SNAP is an evolution of the BEAM development platform and NEST." +
                "<br>(c) Copyright 2002-2014 by the originators." +
                "<br>SNAP is developed under contract to ESA (ESRIN)." +
                "<br>" +
                "<br>SNAP is free software; you can redistribute it and/or modify it" +
                "<br>under the terms of the GNU General Public License as published by the" +
                "<br>Free Software Foundation. This program is distributed in the hope it will be" +
                "<br>useful, but WITHOUT ANY WARRANTY; without even the implied warranty" +
                "<br>of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE." +
                "<br>See the GNU General Public License for more details." +
                "</html>";
        return MessageFormat.format(pattern,
                S2tbxApp.getApp().getAppName(),
                S2tbxApp.getApp().getAppVersion(),
                S2tbxApp.getApp().getAppBuildInfo(),
                S2tbxApp.getApp().getAppCopyright());
    }

    private static Object[][] getSystemInfo() {

        java.util.List<Object[]> data = new ArrayList<>();

        Properties sysProps = null;
        try {
            sysProps = System.getProperties();
        } catch (RuntimeException e) {
            //ignore
        }
        if (sysProps != null) {
            String[] names = new String[sysProps.size()];
            Enumeration<?> e = sysProps.propertyNames();
            for (int i = 0; i < names.length; i++) {
                names[i] = (String) e.nextElement();
            }
            Arrays.sort(names);
            for (String name : names) {
                String value = sysProps.getProperty(name);
                data.add(new Object[]{name, value});
            }
        }

        Object[][] dataArray = new Object[data.size()][2];
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = data.get(i);
        }
        return dataArray;
    }
}
