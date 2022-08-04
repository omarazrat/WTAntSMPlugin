/*
 * Web application tester- Utility to test web applications via Selenium 
 * Copyright (C) 2021-Nestor Arias
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package antsm.com.tests.swing;

import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.toList;
import java.util.stream.IntStream;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.ListModel;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import antsm.com.tests.plugins.AntSMUtilites;
import antsm.com.tests.utils.ConfluenceHelper;
import antsm.com.tests.utils.JIRAReportHelper;
import antsm.com.tests.utils.PoiHelper;
import static antsm.com.tests.utils.PoiHelper.buildDatabaseWB;
import antsm.com.tests.utils.PythonReportHelper;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 *
 * @author nesto
 */
public class ControlPanel extends javax.swing.JFrame {

    private Properties sysProps;
    private Logger log = Logger.getLogger("WebAppTester");
    final String REGEXP_TEAMS_NAME = "^team\\.(.*)\\.capacity";
    private transient boolean procRunning = false;

    /**
     * Creates new form ControlPanel
     */
    public ControlPanel() {
        AntSMUtilites.init();
        sysProps = AntSMUtilites.getConfigFile();
        PoiHelper.init();
        PythonReportHelper.init();
        JIRAReportHelper.init();
        ConfluenceHelper.init();
        initComponents();
    }

    private ListModel<String> getSprintsModel() {
        final List<String> sprints = IntStream.range(1, 27)
                .boxed().map(i -> "" + i).collect(toList());
        DefaultListModel<String> resp = new DefaultListModel<>();
        resp.addAll(sprints);
        return resp;
    }

    private ListModel<String> getTeamNamesModel() {
        List<String> names = getTeamNames();

        DefaultListModel<String> resp = new DefaultListModel<>();
        resp.addAll(names);
        return resp;
    }

    private List<String> getTeamNames() {
        Pattern pattern = Pattern.compile(REGEXP_TEAMS_NAME);
        final List<String> names = sysProps.entrySet()
                .stream().map(e -> {
                    Matcher matcher = pattern.matcher(e.getKey().toString());
                    if (matcher.matches()) {
                        return matcher.group(1);
                    }
                    return null;
                }).filter(e -> e != null)
                .sorted().
                collect(toList());
        return names;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jTabbedPane1 = new javax.swing.JTabbedPane();
        group1 = new javax.swing.JPanel();
        G1TeamL = new javax.swing.JLabel();
        G1SprintL = new javax.swing.JLabel();
        G1TeamSP = new javax.swing.JScrollPane();
        G1Team = new javax.swing.JList<>();
        G1TeamCBox = new javax.swing.JCheckBox();
        G1SprintSP = new javax.swing.JScrollPane();
        G1Sprint = new javax.swing.JList<>();
        G1SprintCBox = new javax.swing.JCheckBox();
        G1DatabaseBtn = new javax.swing.JButton();
        group2 = new javax.swing.JPanel();
        G2TeamL = new javax.swing.JLabel();
        G2TeamSP = new javax.swing.JScrollPane();
        G2Team = new javax.swing.JList<>();
        G2TeamCBox = new javax.swing.JCheckBox();
        G2BBQCBtn = new javax.swing.JButton();
        G2QuarterL = new javax.swing.JLabel();
        G2QuarterSP = new javax.swing.JScrollPane();
        G2Quarter = new javax.swing.JList<>();
        G2QuarterCBox = new javax.swing.JCheckBox();
        progressBar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("antsm"); // NOI18N
        jTabbedPane1.setName(bundle.getString("group1.title")); // NOI18N

        group1.setLayout(new java.awt.GridBagLayout());

        G1TeamL.setText(bundle.getString("labels.team.name")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        group1.add(G1TeamL, gridBagConstraints);

        G1SprintL.setText(bundle.getString("labels.sprint")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        group1.add(G1SprintL, gridBagConstraints);

        G1Team.setModel(getTeamNamesModel()
        );
        G1Team.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                G1TeamValueChanged(evt);
            }
        });
        G1TeamSP.setViewportView(G1Team);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        group1.add(G1TeamSP, gridBagConstraints);

        G1TeamCBox.setText(bundle.getString("labels.all")); // NOI18N
        G1TeamCBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                AllTeamsStateChanged(evt);
            }
        });
        G1TeamCBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                G1TeamCBoxStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        group1.add(G1TeamCBox, gridBagConstraints);

        G1Sprint.setModel(getSprintsModel());
        G1Sprint.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                G1SprintValueChanged(evt);
            }
        });
        G1SprintSP.setViewportView(G1Sprint);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        group1.add(G1SprintSP, gridBagConstraints);

        G1SprintCBox.setText(bundle.getString("labels.all")); // NOI18N
        G1SprintCBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                AllSprintsStateChanged(evt);
            }
        });
        G1SprintCBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                G1SprintCBoxStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        group1.add(G1SprintCBox, gridBagConstraints);

        G1DatabaseBtn.setText(bundle.getString("button.data_base.label")); // NOI18N
        G1DatabaseBtn.setEnabled(false);
        G1DatabaseBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                G1DatabaseBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        group1.add(G1DatabaseBtn, gridBagConstraints);

        jTabbedPane1.addTab(bundle.getString("group1.title"), group1); // NOI18N

        group2.setLayout(new java.awt.GridBagLayout());

        G2TeamL.setText(bundle.getString("labels.team.name")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        group2.add(G2TeamL, gridBagConstraints);

        G2Team.setModel(getTeamNamesModel()
        );
        G2Team.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                G2TeamValueChanged(evt);
            }
        });
        G2TeamSP.setViewportView(G2Team);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        group2.add(G2TeamSP, gridBagConstraints);

        G2TeamCBox.setText(bundle.getString("labels.all")); // NOI18N
        G2TeamCBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                G2TeamCBoxAllTeamsStateChanged(evt);
            }
        });
        G2TeamCBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                G2TeamCBoxStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        group2.add(G2TeamCBox, gridBagConstraints);

        G2BBQCBtn.setText(bundle.getString("button.qc.label")); // NOI18N
        G2BBQCBtn.setEnabled(false);
        G2BBQCBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                G2BBQCBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        group2.add(G2BBQCBtn, gridBagConstraints);

        G2QuarterL.setText(bundle.getString("labels.quarter")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        group2.add(G2QuarterL, gridBagConstraints);
        G2QuarterL.getAccessibleContext().setAccessibleName(bundle.getString("labels.quarter")); // NOI18N

        G2Quarter.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "1", "2", "3", "4" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        G2Quarter.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                G2QuarterValueChanged(evt);
            }
        });
        G2QuarterSP.setViewportView(G2Quarter);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        group2.add(G2QuarterSP, gridBagConstraints);

        G2QuarterCBox.setText(bundle.getString("labels.all")); // NOI18N
        G2QuarterCBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                G2QuarterCBoxAllSprintsStateChanged(evt);
            }
        });
        G2QuarterCBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                G2QuarterCBoxStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        group2.add(G2QuarterCBox, gridBagConstraints);

        jTabbedPane1.addTab(bundle.getString("group2.title"), group2); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 10;
        gridBagConstraints.ipadx = 392;
        gridBagConstraints.ipady = 197;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 27, 6);
        getContentPane().add(jTabbedPane1, gridBagConstraints);
        jTabbedPane1.getAccessibleContext().setAccessibleName("Group1");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipady = 15;
        getContentPane().add(progressBar, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void AllTeamsStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_AllTeamsStateChanged
        final boolean enabled = evt.getStateChange() == ItemEvent.DESELECTED;
        G1Team.setEnabled(enabled);
        getContentPane().repaint();
        updateG1BtnEnabledStatus();
    }//GEN-LAST:event_AllTeamsStateChanged

    private void AllSprintsStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_AllSprintsStateChanged
        final boolean enabled = evt.getStateChange() == ItemEvent.DESELECTED;
        G1Sprint.setEnabled(enabled);
        getContentPane().repaint();
        updateG1BtnEnabledStatus();
    }//GEN-LAST:event_AllSprintsStateChanged
    private void G1DatabaseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_G1DatabaseBtnActionPerformed
//        log.info("Finding out stats for Data base ...");
        if (procRunning) {
            String title = G1DatabaseBtn.getActionCommand();
            promptRunningProc(title);
            return;
        }
        procRunning = true;
        G1DatabaseBtn.setEnabled(false);
        getContentPane().repaint();
        List<String> teamNames = G1TeamCBox.isSelected()
                ? getTeamNames()
                : G1Team.getSelectedValuesList();
        List<Integer> sprints = G1SprintCBox.isSelected()
                ? IntStream.range(1, 27).boxed().collect(toList())
                : new LinkedList<Integer>(G1Sprint.getSelectedValuesList().stream().map(Integer::parseInt).collect(toList()));
        SwingWorker<File, Integer> worker = new SwingWorker<File, Integer>() {
            @Override
            protected File doInBackground() throws Exception {
                String filePath = "";
                try {
                    login();
                    File f = prepareOutputFile();
                    filePath = f.getAbsolutePath();
                    buildDatabaseWB(filePath, teamNames, sprints, this);
                    return f;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, null, ex);
                    return null;
                } finally {
                    logout();
                    log.info("end of operation");
                    JOptionPane.showMessageDialog(null, "Report saved to " + filePath);
                    G1DatabaseBtn.setEnabled(true);
                    procRunning = false;
                    resetProgressBar();
                }
            }
        };
        worker.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("progress")) {
                    progressBar.setValue((Integer) evt.getNewValue());
                    getContentPane().repaint();
                }
            }
        });
        worker.execute();
    }//GEN-LAST:event_G1DatabaseBtnActionPerformed

    private void promptRunningProc(String title) throws HeadlessException {
        ResourceBundle bundle = java.util.ResourceBundle.getBundle("antsm");
        String msg = bundle.getString("button.runningprocess");
        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    private void G1TeamValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_G1TeamValueChanged
        updateG1BtnEnabledStatus();
    }//GEN-LAST:event_G1TeamValueChanged

    private void G1SprintValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_G1SprintValueChanged
        updateG1BtnEnabledStatus();
    }//GEN-LAST:event_G1SprintValueChanged

    private void G1TeamCBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_G1TeamCBoxStateChanged
        updateG1BtnEnabledStatus();
    }//GEN-LAST:event_G1TeamCBoxStateChanged

    private void G1SprintCBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_G1SprintCBoxStateChanged
        updateG1BtnEnabledStatus();
    }//GEN-LAST:event_G1SprintCBoxStateChanged

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened

    }//GEN-LAST:event_formWindowOpened

    private void G2TeamValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_G2TeamValueChanged
        updateG2BtnEnabledStatus();
    }//GEN-LAST:event_G2TeamValueChanged

    private void G2TeamCBoxAllTeamsStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_G2TeamCBoxAllTeamsStateChanged
        updateG2BtnEnabledStatus();
    }//GEN-LAST:event_G2TeamCBoxAllTeamsStateChanged

    private void G2TeamCBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_G2TeamCBoxStateChanged
        updateG2BtnEnabledStatus();
    }//GEN-LAST:event_G2TeamCBoxStateChanged

    private void G2BBQCBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_G2BBQCBtnActionPerformed
        if (procRunning) {
            String title = G2BBQCBtn.getActionCommand();
            promptRunningProc(title);
            return;
        }
        procRunning = true;
        G2BBQCBtn.setEnabled(false);
        getContentPane().repaint();
        SwingWorker<File, Integer> worker = new SwingWorker<File, Integer>() {
            @Override
            protected File doInBackground() throws Exception {
                List<String> teamNames = G2TeamCBox.isSelected()
                        ? getTeamNames()
                        : G2Team.getSelectedValuesList();
                List<Integer> quarters = G2QuarterCBox.isSelected()
                        ? IntStream.range(1, 5).boxed().collect(toList())
                        : new LinkedList<Integer>(G2Quarter.getSelectedValuesList().stream().map(Integer::parseInt).collect(toList()));

//               log.info("teams : "+teamNames.stream().collect(Collectors.joining(",")));
//               log.info("quarters: "+quarters.stream()
//                       .map(Object::toString)
//                       .collect(Collectors.joining(",")));
                String filePath = "";
                try {
                    login();
                    File f = prepareOutputFile();
                    filePath = f.getAbsolutePath();
                    PoiHelper.QCbuildWB(filePath, teamNames, quarters, this);
                    return f;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, null, ex);
                    return null;
                } finally {
                    logout();
                    log.info("end of operation");
                    JOptionPane.showMessageDialog(null, "Report saved to " + filePath);
                    G2BBQCBtn.setEnabled(true);
                    resetProgressBar();
                    procRunning = false;
                }
            }
        };
        worker.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("progress")) {
                    progressBar.setValue((int) evt.getNewValue());
                    getContentPane().repaint();
                }
            }
        });
        worker.execute();
    }//GEN-LAST:event_G2BBQCBtnActionPerformed

    private File prepareOutputFile() {
        final File outputFolder = new File("lib/antsm/output/");
        if (!outputFolder.exists()) {
            outputFolder.mkdir();
        }
        String slash = System.getProperty("file.separator");
        long suffix = System.currentTimeMillis();
        String filePath = outputFolder.getAbsoluteFile() + slash + "output_" + suffix + ".xlsx";
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
        return f;
    }

    private void G2QuarterValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_G2QuarterValueChanged
        updateG2BtnEnabledStatus();
    }//GEN-LAST:event_G2QuarterValueChanged

    private void G2QuarterCBoxAllSprintsStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_G2QuarterCBoxAllSprintsStateChanged
        updateG2BtnEnabledStatus();
    }//GEN-LAST:event_G2QuarterCBoxAllSprintsStateChanged

    private void G2QuarterCBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_G2QuarterCBoxStateChanged
        updateG2BtnEnabledStatus();
    }//GEN-LAST:event_G2QuarterCBoxStateChanged

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        log.info("closing window");
        PoiHelper.destroy();
        PythonReportHelper.destroy();
        JIRAReportHelper.destroy();
        ConfluenceHelper.destroy();
        AntSMUtilites.destroy();
        dispose();
    }//GEN-LAST:event_formWindowClosing

    private void login() throws InvalidParamException, IOException, InvalidVarNameException {
        //-login to Confluence
        ConfluenceHelper.login();
        //Login to JIRA
        JIRAReportHelper.login();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton G1DatabaseBtn;
    private javax.swing.JList<String> G1Sprint;
    private javax.swing.JCheckBox G1SprintCBox;
    private javax.swing.JLabel G1SprintL;
    private javax.swing.JScrollPane G1SprintSP;
    private javax.swing.JList<String> G1Team;
    private javax.swing.JCheckBox G1TeamCBox;
    private javax.swing.JLabel G1TeamL;
    private javax.swing.JScrollPane G1TeamSP;
    private javax.swing.JButton G2BBQCBtn;
    private javax.swing.JList<String> G2Quarter;
    private javax.swing.JCheckBox G2QuarterCBox;
    private javax.swing.JLabel G2QuarterL;
    private javax.swing.JScrollPane G2QuarterSP;
    private javax.swing.JList<String> G2Team;
    private javax.swing.JCheckBox G2TeamCBox;
    private javax.swing.JLabel G2TeamL;
    private javax.swing.JScrollPane G2TeamSP;
    private javax.swing.JPanel group1;
    private javax.swing.JPanel group2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables

    private void logout() {
        try {
            ConfluenceHelper.logout();
            JIRAReportHelper.logout();
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    private void updateG1BtnEnabledStatus() {
        boolean enabled
                = (G1TeamCBox.isSelected() || G1Team.getSelectedIndex() > -1)
                && (G1SprintCBox.isSelected() || G1Sprint.getSelectedIndex() > -1);
        for (JButton btn : new JButton[]{G1DatabaseBtn}) {
            btn.setEnabled(enabled);
        }
        getContentPane().repaint();
    }

    private void updateG2BtnEnabledStatus() {
        boolean enabled
                = (G2TeamCBox.isSelected() || G2Team.getSelectedIndex() > -1)
                && (G2QuarterCBox.isSelected() || G2Quarter.getSelectedIndex() > -1);
        for (JButton btn : new JButton[]{G2BBQCBtn}) {
            btn.setEnabled(enabled);
        }
        getContentPane().repaint();
    }

    private void resetProgressBar() {
        progressBar.setValue(0);
        getContentPane().repaint();
    }

}
