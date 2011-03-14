/*
 *    Copyright 2009-2010 University of Toronto
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
package savant.view.dialog.tree;

import java.awt.event.MouseEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.jidesoft.grid.TreeTable;
import com.jidesoft.swing.TableSearchable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import savant.net.DownloadController;

/**
 *
 * @author mfiume
 */
public class PluginRepositoryBrowser extends JDialog {
    private static final Log LOG = LogFactory.getLog(PluginRepositoryBrowser.class);
    private static final TableCellRenderer FILE_RENDERER = new FileRowCellRenderer();

    private Frame p;
    private String saveToDirectory;
    private TreeTable table;

    public PluginRepositoryBrowser(Frame parent, boolean modal, String title, File xmlfile, String destDir) throws JDOMException, IOException {
        this(parent, modal, title, "Download", getDownloadTreeRows(xmlfile), destDir);
    }

    public PluginRepositoryBrowser(Frame parent, boolean modal, String title, String buttonText, File xmlfile, String destDir) throws JDOMException, IOException {
        this(parent, modal, title, buttonText, getDownloadTreeRows(xmlfile), destDir);
    }

    public PluginRepositoryBrowser(
            Frame parent,
            boolean modal,
            String title,
            String buttonText,
            List<TreeBrowserEntry> roots,
            String dir) {

        super(parent, title, modal);

        saveToDirectory = dir;
        p = parent;

        this.setResizable(true);
        this.setLayout(new BorderLayout());
        this.add(getCenterPanel(roots), BorderLayout.CENTER);

        JToolBar bottombar = new JToolBar();
        bottombar.setFloatable(false);
        bottombar.setAlignmentX(RIGHT_ALIGNMENT);
        bottombar.add(Box.createHorizontalGlue());
        JButton downbutt = new JButton(buttonText);
        downbutt.putClientProperty( "JButton.buttonType", "default" );
        downbutt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelectedItem(false);
            }
        });
        bottombar.add(downbutt);
        this.add(bottombar, BorderLayout.SOUTH);

        this.setPreferredSize(new Dimension(800, 500));
        this.pack();

        setLocationRelativeTo(parent);
    }

    private void downloadSelectedItem(boolean ignoreBranchSelected) {
        TreeBrowserEntry r = (TreeBrowserEntry) table.getRowAt(table.getSelectedRow());
        if (r != null && r.isLeaf()) {
            DownloadController.getInstance().enqueueDownload(r.getURL(), new File(saveToDirectory), null);
        } else {
            if (!ignoreBranchSelected) {
                JOptionPane.showMessageDialog(p, "Please select a file");
            }
        }
    }

    private static TreeBrowserEntry parseDocumentTreeRow(Element root) {
        if (root.getName().equals("branch")) {
            List<TreeBrowserEntry> children = new ArrayList<TreeBrowserEntry>();
            for (Object o : root.getChildren()) {
                Element c = (Element) o;
                children.add(parseDocumentTreeRow(c));
            }
            return new TreeBrowserEntry(root.getAttributeValue("name"), children);
        } else if (root.getName().equals("leaf")) {
            try {
                return new TreeBrowserEntry(root.getAttributeValue("name"), root.getChildText("type"), root.getChildText("description"), new URL(root.getChildText("url")), root.getChildText("size"));
            } catch (MalformedURLException ex) {
                LOG.error(ex);
            }
        }
        return null;
    }

    public static class FileRowCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof TreeBrowserEntry) {
                TreeBrowserEntry fileRow = (TreeBrowserEntry) value;
                JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                        fileRow.getName(),
                        isSelected, hasFocus, row, column);
                try {
                    label.setIcon(fileRow.getIcon());
                } catch (Exception e) {
                    //System.out.println(fileRow.getFile().getAbsolutePath());
                }
                label.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
                return label;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private static List<TreeBrowserEntry> getDownloadTreeRows(File f) throws JDOMException, IOException {
        List<TreeBrowserEntry> roots = new ArrayList<TreeBrowserEntry>();
        Document d = new SAXBuilder().build(f);
        Element root = d.getRootElement();
        TreeBrowserEntry treeroot = parseDocumentTreeRow(root);
        roots.add(treeroot);
        return roots;
    }

    public final Component getCenterPanel(List<TreeBrowserEntry> roots) {
        table = new TreeTable(new TreeBrowserModel(roots));
        table.setSortable(true);
        table.setRespectRenderPreferredHeight(true);

        // configure the TreeTable
        table.setExpandAllAllowed(true);
        table.setShowTreeLines(false);
        table.setSortingEnabled(false);
        table.setRowHeight(18);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //table.expandAll();
        table.expandFirstLevel();
        table.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    downloadSelectedItem(true);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        // do not select row when expanding a row.
        table.setSelectRowWhenToggling(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        //table.getColumnModel().getColumn(1).setPreferredWidth(300);
        //table.getColumnModel().getColumn(2).setPreferredWidth(50);
        //table.getColumnModel().getColumn(3).setPreferredWidth(100);
        //table.getColumnModel().getColumn(4).setPreferredWidth(50);

        table.getColumnModel().getColumn(0).setCellRenderer(FILE_RENDERER);

        // add searchable feature
        TableSearchable searchable = new TableSearchable(table) {

            @Override
            protected String convertElementToString(Object item) {
                if (item instanceof TreeBrowserEntry) {
                    return ((TreeBrowserEntry) item).getType();
                }
                return super.convertElementToString(item);
            }
        };
        searchable.setMainIndex(0); // only search for name column

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(800, 500));
        return panel;
    }
}
