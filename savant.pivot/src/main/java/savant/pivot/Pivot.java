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
/*
 * DataTab.java
 * Created on Feb 25, 2010
 */

package savant.pivot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.*;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import savant.plugin.SavantPanelPlugin;
import savant.util.swing.FilterableTable;
import savant.view.swing.Savant;

public class Pivot extends SavantPanelPlugin {

    static JTextField a;
    static JPanel p;

    public void init(JPanel tablePanel) {
        
        tablePanel.setLayout(new BorderLayout());
        //f = new PathField(JFileChooser.OPEN_DIALOG);
        a = new JTextField("C:\\Users\\mfiume\\Desktop\\New Collection1.txt");
        p = new JPanel();
        p.setBackground(Color.red);
        p.setLayout(new BorderLayout());
        JButton b = new JButton("Open Excel File");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    //p.removeAll();
                    System.out.println("Getting Table");
                    p.add(getTable(), BorderLayout.CENTER);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(Savant.getInstance(), ex.getMessage());
                }
            }
        });
        tablePanel.add(p, BorderLayout.CENTER);
        tablePanel.add(a, BorderLayout.NORTH);
        tablePanel.add(b, BorderLayout.SOUTH);
    }

    /**
	 * creates an {@link HSSFWorkbook} the specified OS filename.
	 */
    private static JComponent getTable() throws IOException {

            BufferedReader reader = new BufferedReader(new FileReader(new File(a.getText())));
            Vector data = new Vector();
            Vector columnNames = new Vector();
            Vector classes = new Vector();

            String columnsLine = reader.readLine(); // skip first line
            String[] columnValues = columnsLine.split("\t");
            columnNames.addAll(Arrays.asList(columnValues));

            for (int i = 0; i < columnValues.length; i++) {
                classes.add(String.class);
            }

            do {
                String line = reader.readLine();
                if (line == null || line.length() == 0) {
                    break;
                }
                String[] values = line.split("\t");
                Vector lineData = new Vector();
                lineData.add(values[0]); // category  name
                lineData.add(values[1]); // product name
                lineData.add(values[2]); // category  name
                lineData.add(values[3]); // product name
                
                for (int i = 0; i < 1; i++) {
                    data.add(lineData);
                }
            }
            while (true);
            return new FilterableTable(data, columnNames, classes);

        /*
        HSSFWorkbook h = new HSSFWorkbook(new FileInputStream(a.getText()));
        HSSFSheet s = h.getSheetAt(0);
        Iterator it = s.rowIterator();

        System.out.println("Opened file: " + a.getText());

        int maxColumnCount = 0;
        Vector data = new Vector();

        while (it.hasNext()) {

            Vector row = new Vector();

            HSSFRow r = (HSSFRow) it.next();
            Iterator cit = r.cellIterator();

            while (cit.hasNext()) {
                HSSFCell c = (HSSFCell) cit.next();
                row.add(c.toString());
                System.out.print(c.toString() + "\t");
            }

            maxColumnCount = Math.max(maxColumnCount, row.size());

            System.out.println();
            data.add(row);
        }

        System.out.println("Columns: " + maxColumnCount);
        System.out.println("Rows: " + data.size());

        Vector columnNames = new Vector();
        Vector classes = new Vector();
        for (int i = 0; i < maxColumnCount; i++) {
            columnNames.add(i + 1 + "");
            classes.add(String.class);
        }

        return new FilterableTable(data, columnNames, classes);
         *
         */
    }

    protected void doStart() throws Exception {

    }

    protected void doStop() throws Exception {

    }

    public String getTitle() {
        return "Pivot";
    }
    
}
