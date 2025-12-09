package com.sunny.riftt.ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ProgressBarRenderer extends JProgressBar implements TableCellRenderer {

    public ProgressBarRenderer() {
        super(0, 100);
        setStringPainted(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        double progress = (value instanceof Double) ? (Double) value : 0;
        setValue((int) progress);
        setString(String.format("%.1f%%", progress));
        return this;
    }
}
