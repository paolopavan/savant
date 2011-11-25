/*
 *    Copyright 2011 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package savant.sql;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.DefaultComboBoxModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Dialog which lets user specify the mapping between database columns and our record
 * types.
 *
 * @author tarkvara
 */
public class MappingDialog extends javax.swing.JDialog {
    private static final Log LOG = LogFactory.getLog(MappingDialog.class);
    public static final DefaultComboBoxModel FORMAT_COMBO_MODEL = new DefaultComboBoxModel(new FormatDef[] { new FormatDef("BED", MappingFormat.INTERVAL_RICH), new FormatDef("Generic Interval", MappingFormat.INTERVAL_GENERIC), new FormatDef("Generic Continuous", MappingFormat.CONTINUOUS_VALUE_COLUMN), new FormatDef("WIG", MappingFormat.CONTINUOUS_WIG) });
    private SQLDataSourcePlugin plugin;
    private Table table;
    private MappingPanel mappingPanel;

    public MappingDialog(Window parent, SQLDataSourcePlugin plugin, Table table) throws SQLException {
        super(parent, ModalityType.APPLICATION_MODAL);
        initComponents();
        this.plugin = plugin;
        this.table = table;
        mappingPanel = new MappingPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        add(mappingPanel, gbc);

        formatCombo.setModel(FORMAT_COMBO_MODEL);
        formatCombo.setSelectedIndex(0);
        formatComboActionPerformed(null);   // So that the correct components are displayed.
        populateDatabaseCombo();
        pack();
        setLocationRelativeTo(parent);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        javax.swing.JPanel navigationPanel = new javax.swing.JPanel();
        javax.swing.JLabel formatLabel = new javax.swing.JLabel();
        formatCombo = new javax.swing.JComboBox();
        javax.swing.JLabel databaseLabel = new javax.swing.JLabel();
        databaseCombo = new javax.swing.JComboBox();
        javax.swing.JLabel tableLabel = new javax.swing.JLabel();
        tableCombo = new javax.swing.JComboBox();
        javax.swing.JButton okButton = new javax.swing.JButton();
        javax.swing.JButton cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        navigationPanel.setLayout(new java.awt.GridBagLayout());

        formatLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        formatLabel.setText("Format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        navigationPanel.add(formatLabel, gridBagConstraints);

        formatCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                formatComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        navigationPanel.add(formatCombo, gridBagConstraints);

        databaseLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        databaseLabel.setText("Database:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        navigationPanel.add(databaseLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        navigationPanel.add(databaseCombo, gridBagConstraints);

        tableLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        tableLabel.setText("Table:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        navigationPanel.add(tableLabel, gridBagConstraints);

        tableCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tableComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.ipadx = 23;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        navigationPanel.add(tableCombo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        getContentPane().add(navigationPanel, gridBagConstraints);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        getContentPane().add(okButton, gridBagConstraints);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        getContentPane().add(cancelButton, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void formatComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_formatComboActionPerformed
        mappingPanel.setFormat(((FormatDef)formatCombo.getSelectedItem()).format);
}//GEN-LAST:event_formatComboActionPerformed

    private void tableComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tableComboActionPerformed
        final Table t = (Table) tableCombo.getSelectedItem();
        if (t != null) {
            new SQLWorker<Column[]>("Fetching database columns...", "Unable to fetch database columns.") {
                @Override
                public Column[] doInBackground() throws SQLException {
                    // The first call to getColumns() can require a lengthy query.
                    return t.getColumns();
                }

                @Override
                public void done(Column[] columns) {
                    if (columns != null) {
                        Arrays.sort(columns, new Comparator<Column>() {

                            @Override
                            public int compare(Column t, Column t1) {
                                return t.toString().compareTo(t1.toString());
                            }
                        });
                        mappingPanel.populate(columns, ColumnMapping.getSavedMapping(plugin, columns, false), false);
                    }
                }
            }.execute();
        }
}//GEN-LAST:event_tableComboActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        ColumnMapping mapping = mappingPanel.getMapping();
        mapping.save(plugin);
        table = (Table)tableCombo.getSelectedItem();
        setVisible(false);
}//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        table = null;
        setVisible(false);
}//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox databaseCombo;
    private javax.swing.JComboBox formatCombo;
    private javax.swing.JComboBox tableCombo;
    // End of variables declaration//GEN-END:variables

    /**
     * Populate the database combo with all databases on the server, excluding
     * system databases which contain no tables.
     */
    private void populateDatabaseCombo() {
        new SQLWorker<List<Database>>("Fetching database list...", "Unable to get list of databases.") {
            @Override
            public List<Database> doInBackground() throws SQLException {
                 return plugin.getDatabases();
            }

            @Override
            public void done(List<Database> databases) {
                if (databases != null) {
                    for (Database db : databases) {
                        databaseCombo.addItem(db);
                    }
                    
                    databaseCombo.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            populateTableCombo();
                        }
                        
                    });
                    // Setting the selected index will populate the table combo.
                    if (table != null) {
                        databaseCombo.setSelectedItem(table.database);
                    } else {
                        databaseCombo.setSelectedIndex(0);
                    }
                }
            }
        }.execute();
    }

    /**
     * Get all the tables from the currently-selected database, and put their names into the
     * Table combo.
     */
    private void populateTableCombo() {
        new SQLWorker<List<Table>>("Fetching table list...", "Unable to get list of tables.") {
            @Override
            public List<Table> doInBackground() throws SQLException{
                return ((Database) databaseCombo.getSelectedItem()).getTables();
            }

            @Override
            public void done(List<Table> tables) {
                tableCombo.removeAllItems();
                if (tables != null) {
                    Collections.sort(tables, new Comparator<Table>() {

                        @Override
                        public int compare(Table t, Table t1) {
                            return t.toString().compareTo(t1.toString());
                        }
                    });
                    for (Table t : tables) {
                        tableCombo.addItem(t);
                    }
                    if (table != null) {
                        tableCombo.setSelectedItem(table);
                    }
                }
            }
        }.execute();
    }

    public MappedTable getMapping() {
        if (table != null) {
            return new MappedTable(table, mappingPanel.getMapping());
        }
        return null;
    }

    public static class FormatDef {
        String name;
        public MappingFormat format;

        FormatDef(String name, MappingFormat format) {
            this.name = name;
            this.format = format;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
