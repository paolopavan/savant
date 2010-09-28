/*
 *    Copyright 2010 University of Toronto
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

/*
 * DataTab.java
 * Created on Feb 25, 2010
 */
package savant.snp;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.event.ChangeEvent;
import org.java.plugin.Plugin;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedEvent;
import savant.plugin.GUIPlugin;
import savant.plugin.PluginAdapter;
import savant.settings.ColourSettings;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.ChangeListener;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import savant.controller.BookmarkController;
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.controller.ViewTrackController;
import savant.controller.event.range.RangeChangedListener;
import savant.controller.event.viewtrack.ViewTrackListChangedListener;
import savant.model.BAMIntervalRecord;
import savant.model.FileFormat;
import savant.model.Genome;
//import savant.snp.Pileup.Nucleotide;
import savant.snp.Pileup.Nucleotide;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.swing.Savant;
import savant.view.swing.ViewTrack;

public class SNPFinderPlugin extends Plugin implements GUIPlugin, RangeChangedListener, ViewTrackListChangedListener {

    private final int MAX_RANGE_TO_SEARCH = 5000;
    private JTextArea info;
    private boolean isSNPFinderOn = true;
    private int sensitivity = 80;
    private int transparency = 50;
    private String sequence;

    private Map<ViewTrack, JPanel> viewTrackToCanvasMap;
    private Map<ViewTrack, List<Pileup>> viewTrackToPilesMap;
    private Map<ViewTrack, List<Pileup>> viewTrackToSNPsMap;

    //private Map<ViewTrack,JPanel> lastViewTrackToCanvasMapDrawn;

    /* == INITIALIZATION == */

    /* INITIALIZE THE SNP FINDER */
    public void init(JTabbedPane tabbedPane, PluginAdapter pluginAdapter) {
        JPanel tablePanel = createTabPanel(tabbedPane, "SNPs");
        pluginAdapter.getRangeController().addRangeChangedListener(this);
        pluginAdapter.getViewTrackController().addTracksChangedListener(this);
        snpsFound = new HashMap<ViewTrack,List<Pileup>>();
        setupGUI(tablePanel);
        addMessage("SNP finder initialized");
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

    /* INIT THE UI */
    private void setupGUI(JPanel panel) {

        JToolBar tb = new JToolBar();
        tb.setName("SNP Finder Toolbar");

        JLabel lab_on = new JLabel("On: ");
        JCheckBox cb_on = new JCheckBox();
        cb_on.setSelected(isSNPFinderOn);
        cb_on.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setIsOn(!isSNPFinderOn);
                addMessage("Turning SNP finder " + (isSNPFinderOn ? "on" : "off"));
            }
        });

        JLabel lab_sensitivity = new JLabel("Sensitivity: ");
        final JSlider sens_slider = new JSlider(0, 100);
        sens_slider.setValue(sensitivity);
        final JLabel lab_sensitivity_status = new JLabel("" + sens_slider.getValue());
        sens_slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                lab_sensitivity_status.setText("" + sens_slider.getValue());
                setSensitivity(sens_slider.getValue());
            }
        });
        sens_slider.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                addMessage("Changed sensitivity to " + sensitivity);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        JLabel lab_trans = new JLabel("Transparency: ");
        final JSlider trans_slider = new JSlider(0, 100);
        trans_slider.setValue(transparency);
        final JLabel lab_transparency_status = new JLabel("" + trans_slider.getValue());
        trans_slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                lab_transparency_status.setText("" + trans_slider.getValue());
                setTransparency(trans_slider.getValue());
            }
        });
        trans_slider.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                addMessage("Changed transparency to " + transparency);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });


        panel.setLayout(new BorderLayout());
        tb.add(lab_on);
        tb.add(cb_on);
        tb.add(new JToolBar.Separator());

        tb.add(lab_sensitivity);
        tb.add(sens_slider);
        tb.add(lab_sensitivity_status);

        tb.add(new JToolBar.Separator());

        tb.add(lab_trans);
        tb.add(trans_slider);
        tb.add(lab_transparency_status);

        panel.add(tb, BorderLayout.NORTH);

        info = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(info);
        panel.add(scrollPane, BorderLayout.CENTER);

    }

    /* ATTACH TO SAVANT UI */
    private JPanel createTabPanel(JTabbedPane jtp, String name) {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        pan.setBackground(ColourSettings.colorTabBackground);
        jtp.addTab(name, pan);
        return pan;
    }

    /* ADD INFO TO THE UI */
    private void addMessage(String msg) {
        info.setText(info.getText() + "[" + MiscUtils.now() + "]    " + msg + "\n");
        info.setCaretPosition(info.getText().length());
    }

    /* == HELPERS == */

    /* RETRIEVE CANVASES */
    private List<PileupPanel> getPileupPanels() {
        List<PileupPanel> panels = new ArrayList<PileupPanel>();
        for (JPanel p : this.viewTrackToCanvasMap.values()) {
            try {
                panels.add((PileupPanel) p.getComponent(0));
            } catch (ArrayIndexOutOfBoundsException e) {}
        }
        return panels;
    }

    /* == EVENTS == */

    /* RANGE CHANGE */
    @Override
    public void rangeChangeReceived(RangeChangedEvent event) {
        setSequence();
        updateTrackCanvasMap();
        runSNPFinder();
    }

    /* TRACK CHANGE */
    @Override
    public void viewTrackListChangeReceived(ViewTrackListChangedEvent event) {
        //updateTrackCanvasMap();
    }

    /* REFRESH LIST OF CANVASES */
    private void updateTrackCanvasMap() {




        if (viewTrackToCanvasMap == null) {
            viewTrackToCanvasMap = new HashMap<ViewTrack, JPanel>();
        }

        // TODO: should get rid of old JPanels here!
        // START
        for (JPanel p : viewTrackToCanvasMap.values()) {
            p.removeAll();
        }
        viewTrackToCanvasMap.clear();
        // END

        Map<ViewTrack, JPanel> newmap = new HashMap<ViewTrack, JPanel>();

        ViewTrackController vtc = ViewTrackController.getInstance();
        for (ViewTrack t : vtc.getTracks()) {

            if (t.getDataType() == FileFormat.INTERVAL_BAM) {

                try {
                    if (viewTrackToCanvasMap.containsKey(t)) {
                        newmap.put(t, viewTrackToCanvasMap.get(t));
                        viewTrackToCanvasMap.remove(t);
                    } else {
                        //System.out.println("putting " + t.getName() + " in BAM map");
                        newmap.put(t, t.getFrame().getLayerToDraw());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //System.out.println("done putting " + t.getName() + " in BAM map");
            }
        }

        viewTrackToCanvasMap = newmap;
    }

    /* SET THIS SNP FINDER ON/OFF */
    private void setIsOn(boolean isOn) {
        this.isSNPFinderOn = isOn;
        List<PileupPanel> panels = this.getPileupPanels();
        for (PileupPanel p : panels) {
            p.setIsOn(isSNPFinderOn);
        }
        this.repaintPileupPanels();
    }

    /* CHANGE THE SENSITIVITY OF THE FINDER */
    private void setSensitivity(int s) {
        sensitivity = s;
        this.doEverything();
    }

    /* CHANGE THE TRANSPARENCY OF THE RENDERER */
    private void setTransparency(int t) {
        transparency = t;
        List<PileupPanel> panels = this.getPileupPanels();
        for (PileupPanel p : panels) {
            p.setTransparency(this.transparency);
        }
        repaintPileupPanels();
    }

    /* == SNP FINDING == */

    /* RUN THE FINDER */
    private void runSNPFinder() {
        if (isSNPFinderOn) {

            if (RangeController.getInstance().getRange().getLength() > MAX_RANGE_TO_SEARCH) {
                addMessage("Won't look for SNPs above range of " + MAX_RANGE_TO_SEARCH + " basepairs");
                return;
            }

            //System.out.println("checking for snps");
            doEverything();
        }

        //System.out.println("done checking for snps");
    }

    /* DO EVERYTHING */
    private void doEverything() {
        if (sequence == null) { setSequence(); }
        updateTrackCanvasMap();
        createPileups();
        callSNPs();
        drawPiles(this.viewTrackToSNPsMap, this.viewTrackToCanvasMap);
    }

    /* PILE UP */
    private void createPileups() {

        if (this.viewTrackToPilesMap != null) { this.viewTrackToPilesMap.clear(); }
        this.viewTrackToPilesMap = new HashMap<ViewTrack,List<Pileup>>();

        for (ViewTrack t : viewTrackToCanvasMap.keySet()) {
            try {
                //List<Integer> snps =
                int startPosition = RangeController.getInstance().getRangeStart();
                List<Pileup> piles = makePileupsFromSAMRecords(t.getName(), t.getDataInRange(), sequence, startPosition);
                this.viewTrackToPilesMap.put(t, piles);
                //drawPiles(piles, viewTrackToCanvasMap.get(t));

                //addMessag(snps.)
            } catch (IOException ex) {
                addMessage("Error: " + ex.getMessage());
                break;
            }
            //addMessag(snps.)
        }
    }

    /* MAKE PILEUPS FOR SAMRECORDS */
    private List<Pileup> makePileupsFromSAMRecords(String viewTrackName, List<Object> samRecords, String sequence, long startPosition) throws IOException {

        //addMessage("Examining each position");

        List<Pileup> pileups = new ArrayList<Pileup>();

        // make the pileups
        int length = sequence.length();
        for (int i = 0; i < length; i++) {
            pileups.add(new Pileup(viewTrackName, startPosition + i, Pileup.getNucleotide(sequence.charAt(i))));
        }

        //System.out.println("Pileup start: " + startPosition);

        // go through the samrecords and edit the pileups
        for (Object o : samRecords) {
            BAMIntervalRecord bir = (BAMIntervalRecord) o;
            SAMRecord sr = bir.getSamRecord();
            updatePileupsFromSAMRecord(pileups, ReferenceController.getInstance().getGenome(), sr, startPosition);
        }

        //addMessage("Done examining each position");

        return pileups;
    }

    /* UPDATE PILEUP INFORMATION FROM SAM RECORD */
    private void updatePileupsFromSAMRecord(List<Pileup> pileups, Genome genome, SAMRecord samRecord, long startPosition) throws IOException {

        // the start and end of the alignment
        int alignmentStart = samRecord.getAlignmentStart();
        int alignmentEnd = samRecord.getAlignmentEnd();

        // the read sequence
        byte[] readBases = samRecord.getReadBases();
        boolean sequenceSaved = readBases.length > 0; // true iff read sequence is set

        // return if no bases (can't be used for SNP calling)
        if (!sequenceSaved) {
            return;
        }

        // the reference sequence
        //byte[] refSeq = genome.getSequence(new Range(alignmentStart, alignmentEnd)).getBytes();
        byte[] refSeq = genome.getSequence(ReferenceController.getInstance().getReferenceName(), new Range(alignmentStart, alignmentEnd)).getBytes();

        // get the cigar object for this alignment
        Cigar cigar = samRecord.getCigar();

        // set cursors for the reference and read
        int sequenceCursor = alignmentStart;
        int readCursor = alignmentStart;

        //System.out.println("Alignment start: " + alignmentStart);

        int pileupcursor = (int) (alignmentStart - startPosition);

        // cigar variables
        CigarOperator operator;
        int operatorLength;

        // consider each cigar element
        for (CigarElement cigarElement : cigar.getCigarElements()) {

            operatorLength = cigarElement.getLength();
            operator = cigarElement.getOperator();

            // delete
            if (operator == CigarOperator.D) {
                // [ DRAW ]
            } // insert
            else if (operator == CigarOperator.I) {
                // [ DRAW ]
            } // match **or mismatch**
            else if (operator == CigarOperator.M) {

                // some SAM files do not contain the read bases
                if (sequenceSaved) {
                    // determine if there's a mismatch
                    for (int i = 0; i < operatorLength; i++) {
                        int refIndex = sequenceCursor - alignmentStart + i;
                        int readIndex = readCursor - alignmentStart + i;

                        byte[] readBase = new byte[1];
                        readBase[0] = readBases[readIndex];

                        Nucleotide readN = Pileup.getNucleotide((new String(readBase)).charAt(0));

                        int j = i + (int) (alignmentStart - startPosition);
                        //for (int j = pileupcursor; j < operatorLength; j++) {
                        if (j >= 0 && j < pileups.size()) {
                            Pileup p = pileups.get(j);
                            p.pileOn(readN);
//                            /System.out.println("(P) " + readN + "\t@\t" + p.getPosition());
                        }
                        //}
                    }
                }
            } // skipped
            else if (operator == CigarOperator.N) {
                // draw nothing
            } // padding
            else if (operator == CigarOperator.P) {
                // draw nothing
            } // hard clip
            else if (operator == CigarOperator.H) {
                // draw nothing
            } // soft clip
            else if (operator == CigarOperator.S) {
                // draw nothing
            }


            if (operator.consumesReadBases()) {
                readCursor += operatorLength;
            }
            if (operator.consumesReferenceBases()) {
                sequenceCursor += operatorLength;
                pileupcursor += operatorLength;
            }
        }
    }


    private void addSNPBookmarks() {

    }

    /* CALL SNPS FOR ALL VIEWTRACKS*/
    private void callSNPs() {

        if (this.viewTrackToSNPsMap != null) { this.viewTrackToSNPsMap.clear(); }
        this.viewTrackToSNPsMap = new HashMap<ViewTrack,List<Pileup>>();

        for (ViewTrack t : viewTrackToCanvasMap.keySet()) {
            List<Pileup> piles = this.viewTrackToPilesMap.get(t);
            List<Pileup> snps = callSNPsFromPileups(piles, sequence);
            this.viewTrackToSNPsMap.put(t, snps);
            addFoundSNPs(t, snps);
        }
    }

    /* CALL SNP FOR PILES FOR CURRECT SEQUENCE */
    private List<Pileup> callSNPsFromPileups(List<Pileup> piles, String sequence) {

        //addMessage("Calling SNPs");

        List<Pileup> snps = new ArrayList<Pileup>();

        int length = sequence.length();
        Pileup.Nucleotide n;
        Pileup p;
        for (int i = 0; i < length; i++) {
            n = Pileup.getNucleotide(sequence.charAt(i));
            p = piles.get(i);

            double confidence = p.getSNPNucleotideConfidence()*100;

            /*
            System.out.println("Position: " + p.getPosition());
            System.out.println("\tAverage coverage: " + p.getCoverageProportion(n));
            System.out.println("\tAverage quality: " + p.getAverageQuality(n));
            System.out.println("\tConfidence: " + confidence);
            System.out.println("\tSensitivity: " + sensitivity);
             */

            if (confidence > 100-this.sensitivity) {
                //System.out.println("== Adding " + p.getPosition() + " as SNP");
                snps.add(p);
            }
        }

        addMessage(snps.size() + " SNPs found");

        //addMessage("Done calling SNPs");

        return snps;
    }

    /* == RENDERING == */

    /* REPAINT CANVASES (e.g. bc of change in transparency) */
    private void repaintPileupPanels() {

        List<PileupPanel> canvases = this.getPileupPanels();

        for (PileupPanel c : canvases) {
            c.repaint();
            c.revalidate();
        }
    }

    /* DRAW PILES ON PANELS FOR ALL VIEWTRACKS */
    private void drawPiles(Map<ViewTrack,List<Pileup>> viewTrackToPileMap, Map<ViewTrack,JPanel> viewTrackToCanvasMap) {

        //lastViewTrackToCanvasMapDrawn = viewTrackToCanvasMap;

        //System.out.println("Drawing annotations");

        for (ViewTrack t : viewTrackToPileMap.keySet()) {
            List<Pileup> pile = viewTrackToPileMap.get(t);
            drawPiles(pile, viewTrackToCanvasMap.get(t));
        }

        //System.out.println("Done drawing annotations");
    }

    /* DRAW PILES ON PANEL */
    private void drawPiles(List<Pileup> piles, JPanel p) {

        p.removeAll();
        PileupPanel pup = new PileupPanel(piles);
        pup.setTransparency(this.transparency);

        p.setLayout(new BorderLayout());
        p.add(pup, BorderLayout.CENTER);

        this.repaintPileupPanels();
    }

    /* == OTHER == */

    /* SET REFERENCE SEQUENCE */
    private void setSequence() {
        sequence = null;
        if (ReferenceController.getInstance().getGenome().isSequenceSet()) {
            try {
                sequence = ReferenceController.getInstance().getGenome().getSequence(ReferenceController.getInstance().getReferenceName(), RangeController.getInstance().getRange());
            } catch (IOException ex) {
                addMessage("Error: could not get sequence");
                return;
            }
        } else {
            addMessage("Error: no reference sequence loaded");
            return;
        }
    }

    HashMap<ViewTrack,List<Pileup>> snpsFound;

    private boolean foundSNPAlready(ViewTrack t, Pileup p) {
        if (snpsFound.containsKey(t) && snpsFound.get(t).contains(p)) {
            //System.out.println(p.getPosition() + " found already");
            return true;
        } else {
            //System.out.println(p.getPosition() + " is new");
            return false;
        }
    }

    private void addFoundSNPs(ViewTrack t, List<Pileup> snps) {
        for (Pileup snp : snps) {
            addFoundSNP(t,snp);
        }
    }

    private void addFoundSNP(ViewTrack t, Pileup snp) {
        if (!this.foundSNPAlready(t, snp)) {
            if (!snpsFound.containsKey(t)) {
                List<Pileup> snps = new ArrayList<Pileup>();
                snpsFound.put(t, snps);
            }

            BookmarkController.getInstance().addBookmark(new Bookmark(ReferenceController.getInstance().getReferenceName(), new Range((int) snp.getPosition(), (int) snp.getPosition()),
                    snp.getSNPNucleotide() + "/" + snp.getReferenceNucleotide()
                    + " SNP "
                    + (int) snp.getCoverage(snp.getSNPNucleotide()) + "/" + (int) snp.getCoverage(snp.getReferenceNucleotide())
                    + " = " + shortenPercentage(snp.getSNPNucleotideConfidence())
                    + " in " + t.getName()));
            snpsFound.get(t).add(snp);
        }
    }

    private String shortenPercentage(double p) {
        String s = ((int) Math.round(p*100)) + "";
        return s + "%";
    }

    @Override
    public void init(JPanel canvas, PluginAdapter pluginAdapter) {
        pluginAdapter.getRangeController().addRangeChangedListener(this);
        pluginAdapter.getViewTrackController().addTracksChangedListener(this);
        snpsFound = new HashMap<ViewTrack,List<Pileup>>();
        setupGUI(canvas);
        addMessage("SNP finder initialized");
    }

    @Override
    public String getTitle() {
        return "SNP Finder Plugin";
    }
}