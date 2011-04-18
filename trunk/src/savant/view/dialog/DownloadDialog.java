/*
 * DownloadDialog.java
 *
 * Created on Sep 7, 2010, 4:48:54 PM
 *
 *
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

package savant.view.dialog;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import savant.api.util.DialogUtils;

/**
 *
 * @author mfiume
 */
public class DownloadDialog extends JDialog {

    private Thread t;
    private boolean complete = false;


    public DownloadDialog(JFrame parent, boolean modal, Thread t) {

        initComponents();
        this.t = t;
        setLocationRelativeTo(parent);

        this.setAlwaysOnTop(true);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                askToDispose();
            }
        });
        
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        progress = new javax.swing.JProgressBar();
        b_cancel = new javax.swing.JButton();
        l_currentfilename = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        l_destination = new javax.swing.JLabel();
        l_amount = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 13));
        jLabel1.setText("Downloading: ");

        b_cancel.setText("Cancel");
        b_cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_cancelActionPerformed(evt);
            }
        });

        l_currentfilename.setText("filename");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 13));
        jLabel2.setText("To:");

        l_destination.setText("destination");

        l_amount.setText("Starting download...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1)
                                .addComponent(jLabel2))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(l_currentfilename, javax.swing.GroupLayout.PREFERRED_SIZE, 329, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(l_destination, javax.swing.GroupLayout.PREFERRED_SIZE, 329, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addComponent(b_cancel, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(l_amount, javax.swing.GroupLayout.PREFERRED_SIZE, 407, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(l_currentfilename))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(l_destination))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
                .addComponent(l_amount)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addComponent(b_cancel)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void b_cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_cancelActionPerformed
        askToDispose();
    }//GEN-LAST:event_b_cancelActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton b_cancel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel l_amount;
    private javax.swing.JLabel l_currentfilename;
    private javax.swing.JLabel l_destination;
    private javax.swing.JProgressBar progress;
    // End of variables declaration//GEN-END:variables

    public void setProgress(int i) {
        this.progress.setValue(i);
    }

    public JProgressBar getProgressBar() {
        return this.progress;
    }

    public void setSource(String arg) {
        this.setTitle("Downloading " + arg);
        this.l_currentfilename.setText(arg);
    }

    public void setDestination(String arg) {
        this.l_destination.setText(arg);
    }

    public void setAmountDownloaded(String string) {
        this.l_amount.setText(string);
    }

    private void askToDispose() {
        if (!complete) {
            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel?", "Cancel download", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                t.interrupt();
                this.dispose();
            }
        } else {
            this.dispose();
        }
    }

    public void setComplete() {
        this.l_amount.setText("Download Complete");
        this.progress.setValue(100);
        this.b_cancel.setText("Close");
        this.complete = true;
        this.dispose();
        DialogUtils.displayMessage("Download complete", "Download finished successfully.");
    }

}
