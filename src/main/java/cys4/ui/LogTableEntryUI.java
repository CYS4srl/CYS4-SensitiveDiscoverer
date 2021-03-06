/*
Copyright (C) 2021 CYS4 Srl
See the file 'LICENSE' for copying permission
*/
package cys4.ui;

import burp.ITextEditor;
import cys4.model.LogEntity;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

// JTable for Viewing Logs
public class LogTableEntryUI extends JTable {

    // get the reference of the array of entries
    private List<LogEntity> logEntries;
    private ITextEditor originalRequestViewer;
    private ITextEditor originalResponseViewer;


    public LogTableEntryUI(LogTableEntriesUI tableModelEntries, List<LogEntity> logEntries, ITextEditor originalRequestViewer, ITextEditor originalResponseViewer) {
        super(tableModelEntries);
        this.getColumnModel().getColumn(0).setMinWidth(80);
        this.getColumnModel().getColumn(0).setMaxWidth(80);
        this.getColumnModel().getColumn(0).setPreferredWidth(80);

        this.logEntries = logEntries;
        this.originalRequestViewer = originalRequestViewer;
        this.originalResponseViewer = originalResponseViewer;
    }

    @Override
    public void changeSelection(int row, int col, boolean toggle, boolean extend) {
        super.changeSelection(row, col, toggle, extend);
        // show the log entry for the selected row; convertRowIndexToModel is used because otherwise the
        // selected row is wrong in case the column is sorted somehow
        int realRow = this.convertRowIndexToModel(row);
        LogEntity logEntry = logEntries.get(realRow);

        byte[] originalRequest = logEntry.getRequestResponse().getRequest();
        byte[] originalResponse = logEntry.getRequestResponse().getResponse();
        updateRequestViewers(originalRequest, originalResponse, logEntry.getMatch());
    }

    public void updateRequestViewers(byte[] request, byte[] response, String search) {
        SwingUtilities.invokeLater(() -> {
            originalRequestViewer.setText(Objects.requireNonNullElseGet(request, () -> new byte[0]));

            if (response != null) {
                originalResponseViewer.setText(response);
                originalResponseViewer.setSearchExpression(search);
            } else {
                originalResponseViewer.setText(new byte[0]);
            }
        });
    }
}