/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.plugin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.commons.lang3.StringUtils;

import savant.api.adapter.BAMDataSourceAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.data.DataFormat;
import savant.api.event.LocationChangedEvent;
import savant.api.event.TrackEvent;
import savant.api.util.Listener;
import savant.api.util.TrackUtils;
import savant.controller.LocationController;
import savant.util.NetworkUtils;


/**
 * The panel on which the tool's user interface is presented.
 *
 * @author tarkvara
 */
class ToolSettingsPanel extends JPanel implements Scrollable {
    private final Tool tool;
    private JLabel commandLine;
    
    /** Listens for carriage returns on text fields to activate the Execute button. */
    private ActionListener executeListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            tool.execute();
        }
    };

    ToolSettingsPanel(Tool t) {
        tool = t;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridy = 0;
        try {
            tool.parseDescriptor();
            commandLine = new JLabel("", SwingConstants.CENTER);
            commandLine.setFont(new Font("Serif", Font.PLAIN, 14));
            commandLine.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(commandLine, gbc);

            JButton executeButton = new JButton("Execute");
            executeButton.addActionListener(executeListener);
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.NONE;
            add(executeButton, gbc);

            for (ToolArgument a: tool.arguments) {
                addArgumentToPanel(a, ++gbc.gridy);
            }
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            add(new JPanel(), gbc);

            tool.displayCommandLine(commandLine);
        } catch (Exception x) {
            Tool.LOG.info(String.format("Unable to load %s.", tool.getDescriptor().getFile()), x);
            add(new JLabel(String.format("<html>Unable to load <i>%s</i><br>%s</html>", tool.getDescriptor().getFile(), x)), gbc);
        }
    }

    private void addArgumentToPanel(ToolArgument arg, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridy = row;
        JComponent widget = null;
        if (arg.type == ToolArgument.Type.BOOL) {
            gbc.gridx = 2;
            gbc.anchor = GridBagConstraints.WEST;
            addWidget(arg, new BoolCheck(arg), gbc);
        } else {
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel nameLabel = new JLabel(arg.name + ":");
            add(nameLabel, gbc);
        
            gbc.gridx = 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1.0;
            JTextField field = null;
            switch (arg.type) {
                case INT:
                    field = new JFormattedTextField();
                    ((JFormattedTextField)field).setValue(Integer.valueOf(arg.value != null ? arg.value : "0"));
                    field.setColumns(5);
                    addField(arg, field, gbc);
                    break;
                case FLOAT:
                    field = new JFormattedTextField();
                    ((JFormattedTextField)field).setValue(Double.valueOf(arg.value != null ? arg.value : "0.0"));
                    field.setColumns(10);
                    addField(arg, field, gbc);
                    break;
                case OUTPUT_FILE:
                    gbc.gridwidth = 1;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    field = new JTextField(arg.value);
                    addField(arg, field, gbc);

                    gbc.gridx = 3;
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    gbc.weightx = 0.0;
                    JCheckBox loadCheck = new JCheckBox("Load upon Completion", true);
                    loadCheck.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            tool.loadUponCompletion = ((JCheckBox)ae.getSource()).isSelected();
                        }
                    });
                    add(loadCheck, gbc);
                    break;
                case RANGE:
                    field = new JTextField();
                    field.setColumns(25);
                    LocationController.getInstance().addListener(new RangeUpdater(field));
                    addField(arg, field, gbc);
                    break;
                case LIST:
                    addWidget(arg, new StringCombo(arg), gbc);
                    break;
                case MULTI:
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    addWidget(arg, new MultiCheckGrid(arg), gbc);
                    break;
                case BAM_INPUT_FILE:
                    TrackUtils.addTrackListener((TrackCombo)addWidget(arg, new TrackCombo(arg, DataFormat.ALIGNMENT), gbc));
                    break;
                case FASTA_INPUT_FILE:
                    TrackUtils.addTrackListener((TrackCombo)addWidget(arg, new TrackCombo(arg, DataFormat.SEQUENCE), gbc));
                    break;
            }
        }
    }

    private void addField(ToolArgument arg, JTextField field, GridBagConstraints gbc) {
        field.addActionListener(executeListener);
        field.getDocument().addDocumentListener(new EditListener(arg));
        field.setMinimumSize(field.getPreferredSize());
        addWidget(arg, field, gbc);
    }

    private JComponent addWidget(ToolArgument arg, JComponent widget, GridBagConstraints gbc) {
        JCheckBox enablerCheck = null;
        if (!arg.required) {
            enablerCheck = new JCheckBox();
            GridBagConstraints gbc2 = new GridBagConstraints();
            gbc2.insets = new Insets(5, 5, 5, 5);
            gbc2.gridy = gbc.gridy;
            gbc2.gridx = 0;
            add(enablerCheck, gbc2);
        }
        add(widget, gbc);
        if (enablerCheck != null) {
            enablerCheck.addActionListener(new EnablerCheckListener(arg, widget));
        }

        return widget;
    }

    /**
     * Scrollable implementation so that we get resized when containing JScrollPane resizes.
     */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    /**
     * Scrollable implementation so that we get resized when containing JScrollPane resizes.
     */
    @Override
    public int getScrollableUnitIncrement(Rectangle rctngl, int i, int i1) {
        return 1;
    }

    /**
     * Scrollable implementation so that we get resized when containing JScrollPane resizes.
     */
    @Override
    public int getScrollableBlockIncrement(Rectangle rctngl, int i, int i1) {
        return 10;
    }

    /**
     * Scrollable implementation so that we get resized when containing JScrollPane resizes.
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Scrollable implementation so that we get resized when containing JScrollPane resizes.
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private class EnablerCheckListener implements ActionListener {
        private final ToolArgument argument;
        private final JComponent[] widgets;

        EnablerCheckListener(ToolArgument arg, JComponent... widgets) {
            argument = arg;
            this.widgets = widgets;
            for (JComponent w: this.widgets) {
                w.setEnabled(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            boolean enabled = ((JCheckBox)ae.getSource()).isSelected();
            for (JComponent w: widgets) {
                w.setEnabled(enabled);
                if (enabled && w instanceof JTextField) {
                    ((JTextField)w).selectAll();
                }
            }
            argument.enabled = enabled;
            tool.displayCommandLine(commandLine);
        }
    }

    /**
     * Check-box which controls the value of a boolean parameter.
     */
    private class BoolCheck extends JCheckBox {
        private final ToolArgument argument;

        private BoolCheck(ToolArgument arg) {
            super(arg.name);
            argument = arg;
            setSelected(Boolean.getBoolean(arg.value));
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    argument.value = String.valueOf(isSelected());
                }
            });
        }
    }

    /**
     * Combo-box which lets user select tracks of a particular type.
     */
    private class TrackCombo extends JComboBox implements Listener<TrackEvent> {
        private final ToolArgument argument;
        private final DataFormat format;
        private String selection;

        TrackCombo(ToolArgument arg, DataFormat df) {
            argument = arg;
            format = df;
            handleEvent((TrackEvent)null);  // Load any existing tracks (probably none)
        }

        /**
         * When Savant's track list is updated, update the combo to reflect it.
         * @param event track notification event
         */
        @Override
        public final void handleEvent(TrackEvent event) {
            List<String> tracks = new ArrayList<String>();
            for (TrackAdapter t: TrackUtils.getTracks(format)) {
                tracks.add(NetworkUtils.getNeatPathFromURI(t.getDataSource().getURI()));
            }
            setModel(new TrackComboModel(tracks.toArray(new String[0])));
            if (tracks.size() > 0) {
                if (selection != null) {
                    setSelectedItem(selection);
                }
                if (getSelectedIndex() < 0 ) {
                    setSelectedIndex(0);
                }
            }
        }

        private class TrackComboModel implements ComboBoxModel {
            private final String[] tracks;
            
            private TrackComboModel(String[] t) {
                tracks = t;
            }

            @Override
            public void setSelectedItem(Object o) {
                selection = (String)o;
                argument.value = selection;
                if (argument.type == ToolArgument.Type.BAM_INPUT_FILE) {
                    tool.useHomoRefs = true;
                    String firstRef = ((BAMDataSourceAdapter)TrackUtils.getTrackDataSource(selection)).getHeader().getSequence(0).getSequenceName();
                    if (firstRef.startsWith("chr")) {
                        tool.useHomoRefs = false;
                    }
                }
                tool.displayCommandLine(commandLine);
            }

            @Override
            public Object getSelectedItem() {
                return selection;
            }

            @Override
            public int getSize() {
                return tracks.length;
            }

            @Override
            public Object getElementAt(int i) {
                return tracks[i];
            }

            @Override
            public void addListDataListener(ListDataListener ll) {
            }

            @Override
            public void removeListDataListener(ListDataListener ll) {
            }
        }
    }
    
    /**
     * Panel with a grid of check-boxes which lets user select multiple arguments.
     */
    private class MultiCheckGrid extends JPanel {
        private final ToolArgument argument;

        /** Whenever a check-box is clicked, update the argument. */
        private final ActionListener checkListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                List<String> checks = new ArrayList<String>();
                for (Component c: getComponents()) {
                    JCheckBox cb = (JCheckBox)c;
                    if (cb.isSelected()) {
                        checks.add(cb.getText());
                    }
                }
                argument.value = StringUtils.join(checks, ',');
                tool.displayCommandLine(commandLine);
            }
        };
        
        int widestCheck;

        private MultiCheckGrid(ToolArgument arg) {
            super();
            argument = arg;
            setLayout(new GridLayout(0, 1, 5, 5));
        
            widestCheck = 1;
            Font f = getFont();
            Font smallFont = f.deriveFont(f.getSize() - 2.0f);
            for (String t: arg.choices) {
                JCheckBox check = new JCheckBox(t);
                check.setFont(smallFont);
                check.addActionListener(checkListener);
                add(check);
                widestCheck = Math.max(widestCheck, check.getPreferredSize().width);
            }
            
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int oldCols = ((GridLayout)getLayout()).getColumns();
                    int newCols = Math.min(getComponentCount(), getWidth() / widestCheck);
                    if (newCols != oldCols) {
                        Tool.LOG.info("New width for " + argument.name + " was " + getWidth() + ", setting columns to " + newCols);
                        ((GridLayout)getLayout()).setColumns(newCols);
                        revalidate();
                    }
                }
            });
        }
        
        @Override
        public void setEnabled(boolean flag) {
            for (Component c: getComponents()) {
                c.setEnabled(flag);
            }
        }
    }

    /**
     * Combo-box which lets user select between a number of string arguments.
     */
    private class StringCombo extends JComboBox {
        private final ToolArgument argument;

        private StringCombo(ToolArgument arg) {
            super(arg.choices);
            argument = arg;
            super.setSelectedItem(arg.value);
        }
        
        @Override
        public void setSelectedItem(Object o) {
            super.setSelectedItem(o);
            argument.value = (String)o;
            tool.displayCommandLine(commandLine);
        }
    }
    
    private class RangeUpdater implements Listener<LocationChangedEvent> {
        private final JTextField field;

        RangeUpdater(JTextField f) {
            field = f;
        }

        @Override
        public void handleEvent(LocationChangedEvent event) {
            field.setText(String.format("%s:%d-%d", event.getReference(), event.getRange().getFrom(), event.getRange().getTo()));
        }
    }
    
    private class EditListener implements DocumentListener {
        ToolArgument argument;

        private EditListener(ToolArgument arg) {
            argument = arg;
        }

        @Override
        public void insertUpdate(DocumentEvent de) {
            try {
                Document d = de.getDocument();
                argument.value = d.getText(0, d.getLength());
                tool.displayCommandLine(commandLine);
            } catch (BadLocationException ignored) {
            }
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            try {
                Document d = de.getDocument();
                argument.value = d.getText(0, d.getLength());
                tool.displayCommandLine(commandLine);
            } catch (BadLocationException ignored) {
            }
        }

        @Override
        public void changedUpdate(DocumentEvent de) {
            try {
                Document d = de.getDocument();
                argument.value = d.getText(0, d.getLength());
                tool.displayCommandLine(commandLine);
            } catch (BadLocationException ignored) {
            }
        }
    }
}
