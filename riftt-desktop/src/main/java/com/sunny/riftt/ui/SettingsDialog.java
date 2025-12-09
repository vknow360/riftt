package com.sunny.riftt.ui;

import com.sunny.riftt.manager.SettingsManager;

import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends JDialog {

    private final SettingsManager settingsManager;
    private JSpinner concurrentSpinner;
    private JSpinner threadsSpinner;
    private JTextField pathField;
    private JSpinner timeoutSpinner;

    public SettingsDialog(Frame owner) {
        super(owner, "Settings", true);
        settingsManager = SettingsManager.getInstance();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setSize(450, 250);
        setLocationRelativeTo(getOwner());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 1. Concurrent Downloads
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Max Concurrent Downloads:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        concurrentSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getMaxConcurrentDownloads(), 1, 10, 1));
        formPanel.add(concurrentSpinner, gbc);

        // 2. Threads per Download
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Threads per Download:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        threadsSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getThreadsPerDownload(), 1, 32, 1));
        formPanel.add(threadsSpinner, gbc);

        // 3. Default Path
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Default Output Folder:"), gbc);

        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathField = new JTextField(settingsManager.getDefaultDownloadPath());
        pathField.setEditable(false);
        JButton browseButton = new JButton("...");
        browseButton.addActionListener(e -> choosePath());

        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(pathPanel, gbc);

        // 4. Timeout
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Unused Connection Timeout (ms):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getConnectionTimeout(), 1000, 60000, 1000));
        formPanel.add(timeoutSpinner, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> saveSettings());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void choosePath() {
        JFileChooser chooser = new JFileChooser(pathField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void saveSettings() {
        settingsManager.setMaxConcurrentDownloads((int) concurrentSpinner.getValue());
        settingsManager.setThreadsPerDownload((int) threadsSpinner.getValue());
        settingsManager.setDefaultDownloadPath(pathField.getText());
        settingsManager.setConnectionTimeout((int) timeoutSpinner.getValue());

        JOptionPane.showMessageDialog(this, "Settings saved. Restart required for some changes to take effect.");
        dispose();
    }
}
