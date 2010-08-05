/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.tools;

import com.jidesoft.swing.JideTabbedPane;
import savant.controller.ThreadController;
import com.jidesoft.docking.DefaultDockingManager;
import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import com.jidesoft.docking.DockingManager.TabbedPaneCustomizer;
import com.jidesoft.pane.CollapsiblePane;
import com.jidesoft.pane.CollapsiblePanes;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import savant.controller.BookmarkController;
import savant.controller.RangeController;
import savant.controller.InformativeThread;
import savant.controller.ViewTrackController;
import savant.controller.event.bookmark.BookmarksChangedEvent;
import savant.controller.event.bookmark.BookmarksChangedListener;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;
import savant.controller.event.thread.ThreadActivityChangedEvent;
import savant.controller.event.thread.ThreadActivityChangedListener;
import savant.controller.event.viewtrack.ViewTrackListChangedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedListener;
import savant.plugin.PluginAdapter;
import savant.plugin.ToolInformation;
import savant.plugin.ToolPlugin;
import savant.util.MiscUtils;
import savant.view.dialog.DataFormatForm;
import savant.view.icon.SavantIconFactory;
import savant.settings.BrowserSettings;
import savant.settings.ColourSettings;
import savant.view.swing.DockableFrameFactory;
import savant.view.swing.Savant;
import savant.view.tools.ToolRunInformation.TerminationStatus;

/**
 *
 * @author mfiume
 */
public class ToolsModule implements BookmarksChangedListener, RangeChangedListener, ViewTrackListChangedListener, ThreadActivityChangedListener {

    private Map<String, List<ToolPlugin>> organizeToolsByCategory(List<ToolPlugin> tools) {

        Map<String, List<ToolPlugin>> map = new HashMap<String,List<ToolPlugin>>();

        for (ToolPlugin p : tools) {
            String category = p.getToolInformation().getCategory();
            List<ToolPlugin> list = null;
            if (map.containsKey(category)) {
                list = map.get(category);
            } else {
                list = new ArrayList<ToolPlugin>();
            }
            list.add(p);
            map.put(category, list);
        }
        return map;
    }

    private void addBogusTools() {
        ToolPlugin tp = new ToolPlugin() {

            @Override
            public void init(PluginAdapter pluginAdapter) {
            }

            @Override
            public ToolInformation getToolInformation() {
                return new ToolInformation(
                        "VARiD",
                        "Genetic Variation Discovery",
                        "VARiD is a Hidden Markov Model for SNP and indel identification with AB-SOLiD color-space as well as regular letter-space reads. VARiD combines both types of data in a single framework which allows for accurate predictions. VARiD was developed at the University of Toronto Computational Biology Lab.",
                        "1.0",
                        "Adrian Dalca",
                        "http://compbio.cs.utoronto.ca/varid/");
            }

            @Override
            public JComponent getCanvas() {
                JPanel p = new JPanel();
                return p;
            }

            @Override
            public void runTool() throws InterruptedException {

                ToolRunInformation runInfo = this.getRunInformation();

                System.out.println("Running VARID");
                int maxCount = 1000000;

                for (int i = 1; i <= maxCount; i++) {

                    if (i % 10000 == 0) {
                        getOutputStream().println(i);
                    }

                    runInfo.setProgress(i*100/maxCount);
                    runInfo.setStatus("on " + i + " of " + maxCount);

                    //System.out.println(i);
                    terminateIfInterruped();
                }
            }
        };

        ToolPlugin tp2 = new ToolPlugin() {

            @Override
            public void init(PluginAdapter pluginAdapter) {

            }

            @Override
            public ToolInformation getToolInformation() {
                return new ToolInformation(
                        "Savant Formatter",
                        STANDARD_CATEGORIES.Convert.name(),
                        "Some other tool",
                        "1.0",
                        "Marc Fiume",
                        "http://www.cs.toronto.edu/~mfiume");
            }

            @Override
            public JComponent getCanvas() {
                return null;
                //JPanel p = new JPanel();
                //return p;
            }

            @Override
            public void runTool() {
                System.out.println("Running some other tool");
                DataFormatForm dff = new DataFormatForm(Savant.getInstance(), false);
                dff.setVisible(true);
            }
        };

        this.addTool(tp);
        this.addTool(tp2);
    }

    private void runTools(List<ToolPlugin> tools) {
        for (ToolPlugin t : tools) {
            ThreadController.getInstance().runInNewThread(t, t.getToolInformation().getName());
        }
        //updateThreadsList();
    }

    @Override
    public void bookmarksChangeReceived(BookmarksChangedEvent event) {
        runTools(toolsSubscribedToBookmarksChangeEvent);
    }


    @Override
    public void rangeChangeReceived(RangeChangedEvent event) {
        runTools(toolsSubscribedToRangeChangeEvent);
    }

    @Override
    public void viewTrackListChangeReceived(ViewTrackListChangedEvent event) {
        runTools(toolsSubscribedToTrackListChangeEvent);
    }

    @Override
    public void threadActivityChangedReceived(ThreadActivityChangedEvent event) {
        System.out.println("Tools module was notified of thread status change to " + event.getActivity());
        updateThreadsList();
    }

    public enum STANDARD_CATEGORIES { Analyze, Convert, Format };

    Set<String> categories;

    JPanel toolsListCanvas;
    static JPanel toolCanvas;
    static JPanel eventSubscriptionCanvas;
    static JPanel outputCanvas;
    static JPanel threadsCanvas;
    static JToolBar runToolbar;

    List<ToolPlugin> tools;
    static ToolPlugin currentTool;

    // subscriptions
    static List<ToolPlugin> toolsSubscribedToBookmarksChangeEvent;
    static List<ToolPlugin> toolsSubscribedToRangeChangeEvent;
    static List<ToolPlugin> toolsSubscribedToTrackListChangeEvent;

    private static void subscribeToList(ToolPlugin tool, List<ToolPlugin> tools) {
        if (!tools.contains(tool)) {
            tools.add(tool);
            updateEventSubscriptions();
        }
    }

    private static void unsubscribeFromList(ToolPlugin tool, List<ToolPlugin> tools) {
        if (tools.contains(tool)) {
            tools.remove(tool);
            updateEventSubscriptions();
        }
    }

    public ToolsModule(JPanel canvas) {

        tools = new ArrayList<ToolPlugin>();
        toolsSubscribedToBookmarksChangeEvent = new ArrayList<ToolPlugin>();
        toolsSubscribedToRangeChangeEvent = new ArrayList<ToolPlugin>();
        toolsSubscribedToTrackListChangeEvent = new ArrayList<ToolPlugin>();

        subscribeToEvents();
        initDocking(canvas);

        addBogusTools();

        updateToolsList();
        updateEventSubscriptions();
        updateThreadsList();

        //updateOutput();
    }

    private void subscribeToEvents() {
        BookmarkController.getInstance().addFavoritesChangedListener(this);
        RangeController.getInstance().addRangeChangedListener(this);
        ViewTrackController.getInstance().addTracksChangedListener(this);
        ThreadController.getInstance().addThreadActivityListener(this);
    }

    public void addTool(ToolPlugin plugin) {
        tools.add(plugin);
    }

    public void removeTool(ToolPlugin plugin) {
        tools.remove(plugin);
    }

    private void setFrameVisibility(String frameKey, boolean isVisible, DefaultDockingManager m) {
        DockableFrame f = m.getFrame(frameKey);
        if (isVisible) { m.showFrame(frameKey); }
        else { m.hideFrame(frameKey); }
    }

    private void initDocking(JPanel canvas) {

        DefaultDockingManager toolsDockingManager = new DefaultDockingManager(Savant.getInstance(),canvas);
        canvas.setBackground(ColourSettings.colorSplitter);

        toolsDockingManager.setInitSplitPriority(DockingManager.SPLIT_EAST_SOUTH_WEST_NORTH);
        toolsDockingManager.loadLayoutData();

        Savant.addDockingManagerToGroup(toolsDockingManager);

        toolsDockingManager.setTabbedPaneCustomizer(new TabbedPaneCustomizer() {

            @Override
            public void customize(JideTabbedPane arg0) {
                arg0.setBoldActiveTab(true);
                arg0.setTabShape(JideTabbedPane.SHAPE_FLAT);
            }
            
        });


        toolCanvas = new JPanel();
        toolCanvas.setBackground(ColourSettings.colorToolsBackground);
        //toolCanvas.setPreferredSize(new Dimension(2000, 2000));
        JComponent workspace = toolsDockingManager.getWorkspace();
        workspace.setLayout(new BorderLayout());
        JPanel c = new JPanel();
        c.setBackground(Color.white);
        
        workspace.add(c, BorderLayout.CENTER);
        pad(toolCanvas,c,10);

        String tlname = "Toolbox";
        DockableFrame toolsListBar = DockableFrameFactory.createFrame(tlname,DockContext.STATE_FRAMEDOCKED,DockContext.DOCK_SIDE_WEST);
        toolsListBar.setAvailableButtons(DockableFrame.BUTTON_HIDE_AUTOHIDE);
        toolsListBar.setFloatable(false);

        toolsListCanvas = new JPanel();

        Container tlworkspace = toolsListBar.getContentPane();
        tlworkspace.setBackground(ColourSettings.colorToolsParameterMarginsBackground);
        pad(toolsListCanvas,tlworkspace,0);

        toolsDockingManager.addFrame(toolsListBar);
        setFrameVisibility(tlname, true, toolsDockingManager);

        String esname = "Event Subscriptions";

        DockableFrame eventSubscriptionBar = DockableFrameFactory.createFrame(esname,DockContext.STATE_FRAMEDOCKED,DockContext.DOCK_SIDE_EAST);
        eventSubscriptionBar.setAvailableButtons(DockableFrame.BUTTON_HIDE_AUTOHIDE);
        eventSubscriptionBar.setFloatable(false);

        eventSubscriptionCanvas = new JPanel();
        Container esworkspace = eventSubscriptionBar.getContentPane();
        pad(eventSubscriptionCanvas,esworkspace,0);

        toolsDockingManager.addFrame(eventSubscriptionBar);
        setFrameVisibility(esname, true, toolsDockingManager);

        String thname = "History";

        DockableFrame threadsBar = DockableFrameFactory.createFrame(thname,DockContext.STATE_FRAMEDOCKED,DockContext.DOCK_SIDE_EAST);
        threadsBar.setAvailableButtons(DockableFrame.BUTTON_HIDE_AUTOHIDE);
        threadsBar.setFloatable(false);

        threadsCanvas = new JPanel();
        Container threadsworkspace = threadsBar.getContentPane();
        pad(threadsCanvas,threadsworkspace,0);

        toolsDockingManager.addFrame(threadsBar);
        setFrameVisibility(thname, true, toolsDockingManager);

        String outputname = "Output";

        DockableFrame outputSubscriptionBar = DockableFrameFactory.createFrame(outputname,DockContext.STATE_AUTOHIDE,DockContext.DOCK_SIDE_SOUTH);
        outputSubscriptionBar.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE);
        outputSubscriptionBar.setFloatable(false);

        outputCanvas = new JPanel();
        Container outputworkspace = outputSubscriptionBar.getContentPane();
        pad(outputCanvas,outputworkspace,10);

        toolsDockingManager.addFrame(outputSubscriptionBar);
        setFrameVisibility(outputname, true, toolsDockingManager);
        
        try {
            toolsListBar.setActive(false);
            eventSubscriptionBar.setActive(false);
            threadsBar.setActive(false);
            outputSubscriptionBar.setActive(false);
        } catch (PropertyVetoException ex) {
        }
    }

    private static void pad(JComponent internal, Container external, int padAmount) {
        external.removeAll();
        SpringLayout layout = new SpringLayout();
        external.setLayout(layout);
        external.add(internal);
        layout.putConstraint(SpringLayout.WEST, internal, padAmount, SpringLayout.WEST, external);
        layout.putConstraint(SpringLayout.EAST, external, padAmount, SpringLayout.EAST, internal);
        layout.putConstraint(SpringLayout.NORTH, internal, padAmount, SpringLayout.NORTH, external);
        layout.putConstraint(SpringLayout.SOUTH, external, padAmount, SpringLayout.SOUTH, internal);
    }

    static JTabbedPane outputTabs;

    private static void removeOutputTabForThread(String name) {
        for (int i = 0; i < outputTabs.getTabCount(); i++) {
            if (outputTabs.getTitleAt(i).equals(name)) {
                outputTabs.remove(i);
            }
        }
    }

    private static void removeOutputTabForThread(Component c) {
        outputTabs.remove(c);
    }

    private static void addOutputTabForThread(InformativeThread t) {
        if (outputTabs == null) {
            outputTabs = new JTabbedPane();
            outputCanvas.setLayout(new BorderLayout());
            outputCanvas.add(outputTabs, BorderLayout.CENTER);
        }

        ToolRunInformation information = t.getRunInformation();
            if (information != null) {
                JComponent toolOutput = information.getOutputCanvas();

                toolOutput.add(getOutputToolBar(t), BorderLayout.NORTH);
                toolOutput.add(getStatusToolbar(t), BorderLayout.SOUTH);


                outputTabs.addTab(t.getName(), toolOutput);
            }

        /*
        JPanel p = new JPanel();
        JLabel l = new JLabel(t.getName());
        p.add(l);
        
        final Component c = outputTabs.getComponentAt(outputTabs.getTabCount()-1);
        JComponent button_close = createHyperlinkButton("x", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeOutputTabForThread(c);
                }
            });
        p.add(button_close);

        outputTabs.setTabComponentAt(outputTabs.getTabCount()-1, p);

        p.setBackground(Color.white);
         * 
         */

        outputTabs.setSelectedIndex(outputTabs.getTabCount()-1);

        outputCanvas.revalidate();
    }

    /*
    private static void updateOutput() {
        
        outputCanvas.removeAll();

        outputCanvas.setLayout(new BorderLayout());

        JTabbedPane jtp = new JTabbedPane();
        ThreadController tm = ThreadController.getInstance();
        for (InformativeThread t : tm.getThreads()) {
            ToolRunInformation information = t.getRunInformation();
            if (information != null) {
                JComponent toolOutput = information.getOutputCanvas();

                toolOutput.add(getOutputToolBar(t), BorderLayout.NORTH);

                jtp.addTab(t.getName(), toolOutput);
            }
        }

        outputCanvas.add(jtp, BorderLayout.CENTER);

        outputCanvas.revalidate();

    }
     * 
     */

    public static JToolBar getStatusToolbar(final InformativeThread t) {
        JToolBar statusToolbar = new JToolBar();
        statusToolbar.setFloatable(false);

        statusToolbar.add(t.getRunInformation().getProgressBar());

        return statusToolbar;
    }

    public static JToolBar getOutputToolBar(final InformativeThread t) {
        JToolBar outputToolBar = new JToolBar();
        outputToolBar.setFloatable(false);

        JButton button_cpy = new JButton();
        button_cpy.setToolTipText("Copy");
        button_cpy.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.COPY));
        button_cpy.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection( t.getRunInformation().getOutput() );
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents( stringSelection, null );
                JOptionPane.showMessageDialog(null, "Output copied to clipboard", "Copy Output", JOptionPane.INFORMATION_MESSAGE);
            }

        });
        outputToolBar.add(button_cpy);

        JButton button_save = new JButton();
        button_save.setToolTipText("Save");
        button_save.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SAVE));
        button_save.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                saveTextToFile(t.getRunInformation().getOutput());
            }

        });
        outputToolBar.add(button_save);

        outputToolBar.add(new JSeparator(SwingConstants.VERTICAL));

        JButton button_close = new JButton();
        button_close.setToolTipText("Close");
        button_close.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.CLOSE));
        button_close.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeOutputTabForThread(t.getName());
                t.getRunInformation().getOutputStream().close();
            }

        });
        outputToolBar.add(button_close);

        return outputToolBar;
    }

    public static void saveTextToFile(String text) {
        JFrame jf = new JFrame();
        String selectedFileName;
        if (Savant.mac) {
            FileDialog fd = new FileDialog(jf, "Export Data", FileDialog.SAVE);
            fd.setVisible(true);
            jf.setAlwaysOnTop(true);
            // get the path (null if none selected)
            selectedFileName = fd.getFile();
            if (selectedFileName != null) {
                selectedFileName = fd.getDirectory() + selectedFileName;
            }
        }
        else {
            JFileChooser fd = new JFileChooser();
            fd.setDialogTitle("Export Data");
            fd.setDialogType(JFileChooser.SAVE_DIALOG);
            int result = fd.showOpenDialog(jf);
            if (result == JFileChooser.CANCEL_OPTION || result == JFileChooser.ERROR_OPTION ) return;
            selectedFileName = fd.getSelectedFile().getPath();
        }

        if (selectedFileName != null) {
            try {

                BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFileName));
                bw.write(text);
                bw.close();

            } catch (IOException ex) {
                String message = "Save unsuccessful";
                String title = "Uh oh...";
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void updateEventSubscriptions() {

        CollapsiblePanes _container = new CollapsiblePanes();

        _container.add(createPanel("Bookmarks Change", toolsSubscribedToBookmarksChangeEvent,true));
        _container.add(createPanel("Range Change", toolsSubscribedToRangeChangeEvent,true));
        _container.add(createPanel("Track List Change", toolsSubscribedToTrackListChangeEvent,true));

        _container.setBackground(ColourSettings.colorToolsListBackground);
        _container.setGap(0);
        //_container.setBorder(new LineBorder(Color.lightGray, 1));

        _container.addExpansion();

        eventSubscriptionCanvas.removeAll();
        eventSubscriptionCanvas.setLayout(new BorderLayout());
        JScrollPane jsp = new JScrollPane(_container);
        jsp.setBorder(new LineBorder(eventSubscriptionCanvas.getBackground(),0));
        eventSubscriptionCanvas.add(jsp, BorderLayout.CENTER);
        eventSubscriptionCanvas.revalidate();
    }

    private void updateToolsList() {

        CollapsiblePanes _container = new CollapsiblePanes();

        Map<String, List<ToolPlugin>> category2ToolMap = organizeToolsByCategory(tools);

        for (String category : category2ToolMap.keySet()) {
            CollapsiblePane pane = createPanel(category, category2ToolMap.get(category),true);
            _container.add(pane);
        }

        _container.setBackground(ColourSettings.colorToolsListBackground);
        _container.setGap(0);
        //_container.setBorder(new LineBorder(Color.lightGray, 1));

        _container.addExpansion();
        
        toolsListCanvas.removeAll();
        toolsListCanvas.setLayout(new BorderLayout());
        JScrollPane jsp = new JScrollPane(_container);
        jsp.setBorder(new LineBorder(toolsListCanvas.getBackground(),0));
        toolsListCanvas.add(jsp, BorderLayout.CENTER);
        
        toolsListCanvas.revalidate();
    }

    private static void updateThreadsList() {

        CollapsiblePanes _container = new CollapsiblePanes();

        List<InformativeThread> threads = ThreadController.getInstance().getThreads();

        for (int i = threads.size()-1; i >= 0; i--) {
            InformativeThread t = threads.get(i);
            CollapsiblePane pane = createPanel(t);
            wash(pane,Color.white);
            _container.add(pane);
        }

        _container.setBackground(ColourSettings.colorToolsListBackground);
        _container.setGap(0);
        //_container.setBorder(new LineBorder(Color.lightGray, 1));

        _container.addExpansion();

        threadsCanvas.removeAll();
        threadsCanvas.setLayout(new BorderLayout());
        JScrollPane jsp = new JScrollPane(_container);
        jsp.setBorder(new LineBorder(threadsCanvas.getBackground(),0));
        threadsCanvas.add(jsp, BorderLayout.CENTER);

        JToolBar tb = new JToolBar();
        tb.setBorder(new LineBorder(Color.lightGray, 1));
        tb.setFloatable(false);
        JButton button_refresh = new JButton();
        button_refresh.setToolTipText("Refresh status (last done " + MiscUtils.now() + ")");
        button_refresh.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.REFRESH));
        button_refresh.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateThreadsList();
                }
            });

        tb.add(button_refresh);
        threadsCanvas.add(tb ,BorderLayout.SOUTH);

        threadsCanvas.revalidate();
    }

    private static CollapsiblePane createPanel(final InformativeThread t) {

        String title = t.getName();
        ToolRunInformation runInformation = t.getRunInformation();

        CollapsiblePane pane = new CollapsiblePane(title);
        pane.setName(title);
        try {
            pane.setCollapsed(false);
        } catch (PropertyVetoException ex) {}

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        if (runInformation.getStartTime() != null) {
            panel.add(new JLabel("Start time: " + runInformation.getStartTime().toLocaleString()));
        }

        switch(runInformation.getTerminationStatus()) {
            case INTERRUPT:
                JLabel l = new JLabel("Status: cancelled");
                panel.add(new JLabel("Cancel time: " + runInformation.getEndTime().toLocaleString()));
                l.setForeground(Color.red);
                panel.add(l);
                break;
            case COMPLETE:
                try {
                    pane.setCollapsed(true);
                } catch (PropertyVetoException ex) {}
                if (runInformation.getEndTime() != null) {
                    panel.add(new JLabel("End time: " + runInformation.getEndTime().toLocaleString()));
                }
                panel.add(new JLabel("Status: complete"));
                break;
            default:
                pane.setTitle(pane.getTitle() + " (running...)");
                panel.add(new JLabel("Status: running"));
                panel.add(createHyperlinkButton("Cancel", new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadController.getInstance().killThread(t.getName());
                        updateThreadsList();
                    }
                }));
                break;
        }

        pane.setStyle(CollapsiblePane.PLAIN_STYLE);
        pane.setContentPane(JideSwingUtilities.createTopPanel(panel));
        return pane;
    }

    private static CollapsiblePane createPanel(String title, JComponent c, boolean isExpanded) {
        CollapsiblePane pane = new CollapsiblePane(title);
        pane.setName(title);
        try {
            pane.setCollapsed(!isExpanded);
        } catch (PropertyVetoException ex) {
        }


        pane.setStyle(CollapsiblePane.PLAIN_STYLE);
        pane.setContentPane(JideSwingUtilities.createTopPanel(c));
        return pane;
    }

    private static CollapsiblePane createPanel(String title, List<ToolPlugin> tools, boolean isExpanded) {

        title += " (" + tools.size() + ")";
        CollapsiblePane pane = new CollapsiblePane(title);
        pane.setName(title);

        try {
            pane.setCollapsed(!isExpanded);
        } catch (PropertyVetoException ex) {
        }

        JPanel labelPanel = new JPanel();
        labelPanel.setOpaque(false);
        labelPanel.setLayout(new GridLayout(tools.size(), 1, 1, 0));

        // TODO: does this (making p final) work?
        
        for (final ToolPlugin p : tools) {
            JComponent button = createHyperlinkButton(p.getToolInformation().getName(), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentTool == null || !currentTool.getToolInformation().getName().equals(p.getToolInformation().getName())) {
                        setCurrentTool(p);
                        showCurrentTool();
                    }
                }
            });
            button.setToolTipText(p.getToolInformation().getDescription());
            labelPanel.add(button);
        }

        pane.setStyle(CollapsiblePane.PLAIN_STYLE);
        pane.setContentPane(JideSwingUtilities.createTopPanel(labelPanel));
        return pane;
    }
    
    private static void setCurrentTool(ToolPlugin p) {
        currentTool = p;
    }

    private static void showCurrentTool() {
        showTool(currentTool);
    }

    private static void showTool(ToolPlugin p) {

        CollapsiblePanes _container = new CollapsiblePanes();

        _container.add(createPanel("Information", getToolInformation(p), true));
        JPanel pan = getToolCanvas(p);
        if (pan != null) { _container.add(createPanel("Settings",pan, true)); }
        _container.add(createPanel("Event Subscriptions", getEventSubscriptionCanvas(p), true));

        _container.setBackground(ColourSettings.colorToolsListBackground);
        _container.setGap(0);
        _container.setBorder(new LineBorder(Color.lightGray, 1));

        _container.addExpansion();

        toolCanvas.removeAll();
        toolCanvas.repaint();
        toolCanvas.setLayout(new BorderLayout());
        //toolCanvas.setLayout(new BoxLayout(toolCanvas, BoxLayout.PAGE_AXIS));
        toolCanvas.setBackground(ColourSettings.colorToolsListBackground);

        JLabel title = new JLabel(" " + p.getToolInformation().getName());
        title.setFont(new Font("Arial", Font.BOLD, 20));

        toolCanvas.add(title, BorderLayout.NORTH);
        toolCanvas.add(_container, BorderLayout.CENTER);

        JToolBar runtoolbar = new JToolBar();
        runtoolbar.setFloatable(false);
        runtoolbar.setBorder(new LineBorder(Color.lightGray, 1));
        JButton button_run = new JButton();
        button_run.setToolTipText("Run tool now");
        button_run.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.RUN));
        button_run.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTool != null) {
                    runToolInNewThread(currentTool);
                }
            }
        });

        runtoolbar.add(button_run);
        toolCanvas.add(runtoolbar, BorderLayout.SOUTH);
    }

    private static void runToolInNewThread(ToolPlugin tool) {
        InformativeThread t = ThreadController.getInstance().runInNewThread(tool, tool.getToolInformation().getName());
        updateThreadsList();
        addOutputTabForThread(t);
    }

    private static JPanel getEventSubscriptionCanvas(final ToolPlugin p) {
        JPanel panel = new JPanel();

        panel.add(new JLabel("Run this tool when: "));
        panel.add(makeEventSubscriptionCheckBox(p, "Bookmarks change", toolsSubscribedToBookmarksChangeEvent));
        panel.add(makeEventSubscriptionCheckBox(p, "Range changes", toolsSubscribedToRangeChangeEvent));
        panel.add(makeEventSubscriptionCheckBox(p, "Track list changes", toolsSubscribedToTrackListChangeEvent));

        wash(panel,Color.white);

        return panel;
    }

    private static void wash(JComponent c, Color col) {
        if (c == null) { return; }
        c.setBackground(col);
        for (Component c2 : c.getComponents()) {
            if (c2 instanceof JComponent) {
                wash((JComponent) c2, col);
            }
        }
    }

    private static JCheckBox makeEventSubscriptionCheckBox(final ToolPlugin p, String cbText, final List<ToolPlugin> subscribedTools) {

        final JCheckBox cb = new JCheckBox();
        cb.setText(cbText);
        cb.setSelected(subscribedTools.contains(p));
        cb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cb.isSelected()) {
                    subscribeToList(p, subscribedTools);
                } else {
                    unsubscribeFromList(p, subscribedTools);
                }
            }
        });

        return cb;
    }

    private static JPanel getToolInformation(final ToolPlugin p) {

        JPanel toolInformationCanvas = new JPanel();
        
        toolInformationCanvas.setLayout(new GridLayout(1, 1, 1, 0));
        //toolInformationCanvas.setBorder(new LineBorder(Color.lightGray, 1));

        JEditorPane info = new JEditorPane();
        info.setContentType("text/html");
        info.setMargin(new Insets(10,10,10,10));
        info.setText("<html>"
                //+ "<body bgcolor=\"#E0DFE3\">"
                + "<body>"
                + "<div style=\"margin:5px;\">"
                + "<font face=\"Arial\" size=\"3\">"
                + "<b>Version:</b> " + p.getToolInformation().getVersion() + "<br/>"
                + "<b>Author:</b> " + p.getToolInformation().getAuthor() + "<br/>"
                + "<b>Description:</b> " + p.getToolInformation().getDescription() + "<br/>"
                + "<b>Site:</b> <u>" + p.getToolInformation().getLink() + "</u>"
                + "</font>"
                + "</div>"
                + "</body>"
                + "</html>");
        info.setBackground(toolInformationCanvas.getBackground());
        info.setBorder(new LineBorder(toolInformationCanvas.getBackground(), 0, true));

        info.setEditable(false);

        toolInformationCanvas.add(info);

        wash(toolInformationCanvas, Color.white);

        toolInformationCanvas.revalidate();

        return toolInformationCanvas;
    }

    private static JPanel getToolCanvas(ToolPlugin p) {

        JPanel toolParameterCanvas = new JPanel();

        //toolParameterCanvas.removeAll();
        toolParameterCanvas.setLayout(new BorderLayout());

        JComponent pan = p.getCanvas();

        wash(pan, Color.white);

        if (pan == null) { return null; }

        toolParameterCanvas.add(pan,BorderLayout.CENTER);
        toolParameterCanvas.revalidate();

        return toolParameterCanvas;
    }

    static JComponent createHyperlinkButton(String name, ActionListener l) {
        final JideButton button = new JideButton(name);
        button.setButtonStyle(JideButton.HYPERLINK_STYLE);

        button.setOpaque(false);
        button.setHorizontalAlignment(SwingConstants.LEADING);

        button.setRequestFocusEnabled(true);
        button.setFocusable(true);

        button.addActionListener(l);

        //button.setCursor(Cursor.getPredefinedCursor(Cursor.));
        return button;
    }
}