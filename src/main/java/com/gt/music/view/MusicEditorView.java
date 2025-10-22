package com.gt.music.view;

import com.gt.music.model.MusicEditorModel;
import com.gt.music.view.canvas.MusicView;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;
import java.util.ArrayList;


public class MusicEditorView extends JFrame {
    private JMenuItem newPageMenuItem;
    private JMenuItem deletePageMenuItem;
    private JMenuItem nextPageMenuItem;
    private JMenuItem prevPageMenuItem;

    private JButton newPageButton;
    private JButton deletePageButton;
    private JButton nextPageButton;
    private JButton prevPageButton;

    private JLabel pageLabel;

    private JMenuItem exitMenuItem;
    private JMenuItem newStaffMenuItem;
    private JMenuItem deleteStaffMenuItem;

    private JButton selectButton;
    private JButton penButton;
    private JButton playButton;
    private JButton stopButton;
    private JButton newStaffButton;
    private JButton deleteStaffButton;

    private JRadioButton noteRButton;
    private JRadioButton restRButton;
    private JRadioButton flatRButton;
    private JRadioButton sharpRButton;

    private JSlider durationSlider;

    private ArrayList<MusicView> pageViews;
    private JScrollPane contentScrollPane;
    private JLabel statusBar;

    private int displayedPageIndex = 0;


    public MusicEditorView(MusicEditorModel model) {
        //Create window and set title
        super("My Music Editor");
        //Window close behavior
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Window basic layout
        this.setLayout(new BorderLayout());


        //-----------------------------------------------------
        //1.MENU
        //Create menu
        JMenuBar menuBar = new JMenuBar();
        //menu-file
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        //menu-file -> exit
        exitMenuItem = new JMenuItem("Exit");
        fileMenu.add(exitMenuItem);
        //menu-edit
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        //menu-edit -> staff
        newStaffMenuItem = new JMenuItem("New Staff");
        deleteStaffMenuItem = new JMenuItem("Delete Staff");
        editMenu.add(newStaffMenuItem);
        editMenu.add(deleteStaffMenuItem);
        //menu-edit -> pages
        newPageMenuItem = new JMenuItem("New Page");
        deletePageMenuItem = new JMenuItem("Delete Page");
        editMenu.addSeparator();
        editMenu.add(newPageMenuItem);
        editMenu.add(deletePageMenuItem);
        //menu-view
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        nextPageMenuItem = new JMenuItem("Next Page");
        prevPageMenuItem = new JMenuItem("Previous Page");
        viewMenu.add(nextPageMenuItem);
        viewMenu.add(prevPageMenuItem);

        this.setJMenuBar(menuBar);

        //-----------------------------------------------------
        //2.PANEL
        //Create panel
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));

        //2.1 BUTTONS
        //toolPanel-select, pen
        JPanel selectPenPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        selectButton = new JButton("Select", loadIcon("/images/select.png", 24, 24));
        penButton = new JButton("Pen", loadIcon("/images/pen.png", 24, 24));
        selectPenPanel.add(selectButton);
        selectPenPanel.add(penButton);

        //toolPanel-New Staff, Delete Staff
        JPanel staffControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        newStaffButton = new JButton("New Staff", loadIcon("/images/addStaff.png", 24, 24));
        deleteStaffButton = new JButton("Delete Staff", loadIcon("/images/deleteStaff.png", 32, 32));
        staffControlPanel.add(newStaffButton);
        staffControlPanel.add(deleteStaffButton);

        //toolPanel-Play, Stop
        JPanel playStopPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        playButton = new JButton("Play", loadIcon("/images/play.png", 20, 20));
        stopButton = new JButton("Stop", loadIcon("/images/stop.png", 22, 22));
        playStopPanel.add(playButton);
        playStopPanel.add(stopButton);

        //Add sub panels to main panel
        toolPanel.add(selectPenPanel);
        toolPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        toolPanel.add(staffControlPanel);
        toolPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        toolPanel.add(playStopPanel);
        toolPanel.add(new JSeparator(JSeparator.HORIZONTAL));


        //Create a box for RadioButtons and Slider
        JPanel noteToolPanel = new JPanel(new BorderLayout(10, 0));
        //2.2 Radio Buttons
        //ButtonGroup- note, rest, flat, sharp
        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        noteRButton = new JRadioButton("Note");
        restRButton = new JRadioButton("Rest");
        flatRButton = new JRadioButton("Flat");
        sharpRButton = new JRadioButton("Sharp");
        //Set main.java.com.gt.music.model.Note default
        noteRButton.setSelected(true);
        //Create ButtonGroup[LOGIC CONSTRAINT], ensure ONLY ONE option can be selected
        ButtonGroup noteToolsGroup = new ButtonGroup();
        noteToolsGroup.add(noteRButton);
        noteToolsGroup.add(restRButton);
        noteToolsGroup.add(flatRButton);
        noteToolsGroup.add(sharpRButton);
        //Add to toolPanel
        radioPanel.add(noteRButton);
        radioPanel.add(restRButton);
        radioPanel.add(flatRButton);
        radioPanel.add(sharpRButton);

        //2.3 Slider
        //Create JSlider:5 Labels from 0-4
        durationSlider = new JSlider(0, 4);
        durationSlider.setOrientation(JSlider.VERTICAL);
        //Create labels: hash table
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("Sixteenth"));
        labelTable.put(1, new JLabel("Eighth"));
        labelTable.put(2, new JLabel("Quarter"));
        labelTable.put(3, new JLabel("Half"));
        labelTable.put(4, new JLabel("Whole"));
        //Set Slider -> labels and ticks
        durationSlider.setLabelTable(labelTable);
        durationSlider.setPaintLabels(true);
        durationSlider.setMajorTickSpacing(1);
        durationSlider.setPaintTicks(true);
        //Add Radio Buttons and Slider to BOX
        noteToolPanel.add(radioPanel, BorderLayout.WEST);
        noteToolPanel.add(durationSlider, BorderLayout.CENTER);
        //Add slider to toolPanel
        toolPanel.add(noteToolPanel);
        toolPanel.add(new JSeparator(JSeparator.HORIZONTAL));

        //2.4 Page Buttons
        JPanel pageControlPanel = new JPanel();
        newPageButton = new JButton("New Page");
        deletePageButton = new JButton("Delete Page");
        prevPageButton = new JButton("Previous");
        nextPageButton = new JButton("Next");
        pageControlPanel.add(prevPageButton);
        pageControlPanel.add(nextPageButton);
        pageControlPanel.add(newPageButton);
        pageControlPanel.add(deletePageButton);
        toolPanel.add(pageControlPanel);
        //Add page status at the bottom
        pageLabel = new JLabel("Page 1 of 1");
        toolPanel.add(pageLabel);

        this.add(toolPanel, BorderLayout.WEST);

        this.pageViews = new ArrayList<>();
        MusicView firstPage = new MusicView(model);
        firstPage.setStatusSink(text -> statusBar.setText(text));
        this.pageViews.add(firstPage);

        //Add label to scroll panel
        contentScrollPane = new JScrollPane(firstPage);
        //Set scroll bar always visible
        contentScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        contentScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        this.add(contentScrollPane, BorderLayout.CENTER);

        statusBar = new JLabel("Status: Ready");
        this.add(statusBar, BorderLayout.SOUTH);

        //-----------------------------------------------------
        //Set window size
        this.setSize(800, 600);
    }

    public JMenuItem getExitMenuItem() {
        return exitMenuItem;
    }

    public JMenuItem getNewStaffMenuItem() {
        return newStaffMenuItem;
    }

    public JMenuItem getDeleteStaffMenuItem() {
        return deleteStaffMenuItem;
    }

    public JButton getSelectButton() {
        return selectButton;
    }

    public JButton getPenButton() {
        return penButton;
    }

    public JButton getPlayButton() { return playButton; }

    public JButton getStopButton() {
        return stopButton;
    }

    public JButton getNewStaffButton() {
        return newStaffButton;
    }

    public JButton getDeleteStaffButton() {
        return deleteStaffButton;
    }

    public JRadioButton getNoteRButton() {
        return noteRButton;
    }

    public JRadioButton getRestRButton() {
        return restRButton;
    }

    public JRadioButton getFlatRButton() {
        return flatRButton;
    }

    public JRadioButton getSharpRButton() {
        return sharpRButton;
    }

    public JSlider getDurationSlider() {
        return durationSlider;
    }

    public JLabel getStatusBar() {
        return statusBar;
    }

    public JMenuItem getNewPageMenuItem() {
        return newPageMenuItem;
    }

    public JMenuItem getDeletePageMenuItem() {
        return deletePageMenuItem;
    }

    public JMenuItem getNextPageMenuItem() {
        return nextPageMenuItem;
    }

    public JMenuItem getPrevPageMenuItem() {
        return prevPageMenuItem;
    }

    public JButton getNewPageButton() {
        return newPageButton;
    }

    public JButton getDeletePageButton() {
        return deletePageButton;
    }

    public JButton getNextPageButton() {
        return nextPageButton;
    }

    public JButton getPrevPageButton() {
        return prevPageButton;
    }

    public JLabel getPageLabel() {
        return pageLabel;
    }


    //loadIcon method with scaling support
    private ImageIcon loadIcon(String path, int width, int height) {
        try {
            java.net.URL imageUrl = getClass().getResource(path);
            if (imageUrl != null) {
                ImageIcon icon = new ImageIcon(imageUrl);
                //Get the Image object inside the ImageIcon
                Image image = icon.getImage();
                //Create a scaled Image instance
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                //Create a new ImageIcon from the scaled Image
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.err.println("Couldn't find file: " + path);
        }
        return null;
    }


    public void addNewPageView(MusicEditorModel model) {
        MusicView newPage = new MusicView(model);
        newPage.setStatusSink(text -> statusBar.setText(text));
        this.pageViews.add(newPage);
    }

    public void deleteCurrentPageView(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < this.pageViews.size()) {
            this.pageViews.remove(pageIndex);
        }
    }

    public void displayPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < this.pageViews.size()) {
            MusicView viewToDisplay = this.pageViews.get(pageIndex);
            this.displayedPageIndex = pageIndex;
            // Switch display content
            this.contentScrollPane.setViewportView(viewToDisplay);
            this.revalidate();
            this.repaint();
        }
    }


    public com.gt.music.view.canvas.MusicView getMusicView() {
        if (pageViews == null || pageViews.isEmpty()) return null;
        if (displayedPageIndex < 0 || displayedPageIndex >= pageViews.size()) return null;
        return pageViews.get(displayedPageIndex);
    }


    /** 把当前页所有 Symbol 以“逐行从左到右”的顺序返回 */
    public java.util.List<com.gt.music.model.Symbol> getActivePageSymbolsInReadingOrder(){
        com.gt.music.view.canvas.MusicView mv = getMusicView();
        if (mv == null) return java.util.Collections.emptyList();
        return mv.exportSymbolsInReadingOrder();
    }

}
