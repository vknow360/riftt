package com.sunny.riftt.ui;

import javax.swing.*;

import com.sunny.riftt.manager.SettingsManager;

import java.awt.*;

public class AddDownloadDialog extends JDialog {

    private JTextField urlField;
    private JTextField pathField;
    private JButton downloadButton;
    private JButton cancelButton;
    private boolean confirmed = false;

    public AddDownloadDialog(Frame owner) {
        super(owner, "Add Download", true);
        setSize(500, 200);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // URL Row
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        formPanel.add(new JLabel("URL:"), gbc);

        urlField = new JTextField();
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(urlField, gbc);
        urlField.setText("https://examplefile.com/file-download/325");

        // Path Row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.1;
        formPanel.add(new JLabel("Save Request:"), gbc);

        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        String defaultPath = SettingsManager.getInstance().getDefaultDownloadPath();
        pathField = new JTextField(defaultPath);
        JButton browseButton = new JButton("Browse...");

        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(pathPanel, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        downloadButton = new JButton("Download");
        cancelButton = new JButton("Cancel");

        buttonPanel.add(downloadButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Listeners
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        downloadButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getUrl() {
        return urlField.getText().trim();
    }

    public String getSaveDir() {
        return pathField.getText().trim();
    }
}
