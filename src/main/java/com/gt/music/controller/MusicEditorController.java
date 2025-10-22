package com.gt.music.controller;

import com.gt.music.model.MusicEditorModel;
import com.gt.music.types.EditMode;
import com.gt.music.types.NoteDuration;
import com.gt.music.types.ToolType;
import com.gt.music.view.MusicEditorView;
import com.gt.music.view.canvas.MusicView;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.gt.music.midi.MIDI_Player;
import com.gt.music.model.Symbol;
import com.gt.music.model.playback.PlayEvent;
import com.gt.music.model.playback.TimelineBuilder;
import com.gt.music.model.playback.PlaybackEngine;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MusicEditorController {
    private MusicEditorModel model;
    private MusicEditorView view;

    private final MIDI_Player midi = new MIDI_Player();
    private final AtomicReference<Thread> playThreadRef = new AtomicReference<>(null);
    private PlaybackEngine currentEngine;

    public MusicEditorController(MusicEditorModel model, MusicEditorView view) {
        this.model = model;
        this.view = view;
        addListeners();
        updateView();
    }

    private void addListeners() {
        //Add listener to simple buttons
        view.getSelectButton().addActionListener(e -> {
            // Switch mode: SELECT <--> DRAW
            if (model.getCurrentMode() == EditMode.DRAW) {
                model.setCurrentMode(EditMode.SELECT);
                view.getStatusBar().setText("Status: Select mode activated.");
            } else {
                model.setCurrentMode(EditMode.DRAW);
                view.getStatusBar().setText("Status: Draw mode activated.");
            }
        });
        view.getPenButton().addActionListener(e ->
                view.getStatusBar().setText("Status: Pen button clicked.")
        );

        // +++ Play/stop music +++
        view.getStopButton().setEnabled(false); // 初始 Stop 置灰

        view.getPlayButton().addActionListener(e -> {
            // 已在播则忽略
            if (playThreadRef.get() != null) return;

            // 1) 取当前页符号（按阅读顺序）
            List<Symbol> symbols = view.getActivePageSymbolsInReadingOrder();

            // 2) 构建时间线（x 容差 10px 可按需微调）
            List<PlayEvent> timeline = TimelineBuilder.build(symbols, 10);

            // 3) UI 状态 & 状态栏
            view.getStatusBar().setText("Status: Playing...");
            view.getPlayButton().setEnabled(false);
            view.getStopButton().setEnabled(true);

            // 4) 启动播放线程
            currentEngine = new PlaybackEngine(timeline, midi, () -> {
                // 播放自然结束时恢复 UI
                view.getStatusBar().setText("Status: Ready");
                view.getPlayButton().setEnabled(true);
                view.getStopButton().setEnabled(false);
                playThreadRef.set(null);
            });
            Thread t = new Thread(currentEngine, "music-playback");
            playThreadRef.set(t);
            t.start();
        });

        view.getStopButton().addActionListener(e -> {
            Thread t = playThreadRef.get();
            if (t != null) {
                view.getStatusBar().setText("Status: Stopped.");
                currentEngine.requestStop(); // 请求停止
                t.interrupt();               // 让线程尽快醒来
            }
        });


        //Add listener to radio buttons and update model
        view.getNoteRButton().addActionListener(e -> {
            view.getStatusBar().setText("Status: com.gt.music.model.Note tool selected.");
            model.setCurrentTool(ToolType.NOTE);
        });
        view.getRestRButton().addActionListener(e -> {
            view.getStatusBar().setText("Status: com.gt.music.model.Rest tool selected.");
            model.setCurrentTool(ToolType.REST);
        });
        view.getFlatRButton().addActionListener(e -> {
            view.getStatusBar().setText("Status: Flat tool selected.");
            model.setCurrentTool(ToolType.FLAT);
        });
        view.getSharpRButton().addActionListener(e -> {
            view.getStatusBar().setText("Status: Sharp tool selected.");
            model.setCurrentTool(ToolType.SHARP);
        });

        // Streamlined Tool Selection
        ToolSelectionHandler toolHandler = new ToolSelectionHandler();
        view.getNoteRButton().addMouseListener(toolHandler);
        view.getNoteRButton().addMouseMotionListener(toolHandler);
        view.getRestRButton().addMouseListener(toolHandler);
        view.getRestRButton().addMouseMotionListener(toolHandler);

        //Add listener for new & delete staff operations
        view.getNewStaffButton().addActionListener(e -> {
            model.addStaff();
            updateView();
        });
        view.getNewStaffMenuItem().addActionListener(e -> {
            model.addStaff();
            updateView();
        });
        view.getDeleteStaffButton().addActionListener(e -> {
            model.deleteStaff();
            updateView();
        });
        view.getDeleteStaffMenuItem().addActionListener(e -> {
            model.deleteStaff();
            updateView();
        });

        //Add listener for slider operations
        view.getDurationSlider().addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            //Ensure it fires only when the user releases the mouse
            if (!source.getValueIsAdjusting()) {
                int value = source.getValue();

                NoteDuration selectedDuration = NoteDuration.values()[value];
                String durationString = selectedDuration.toString();

                view.getStatusBar().setText("Status: Duration set to " + durationString);
                model.setCurrentDuration(selectedDuration);
            }
        });

        //Add listener for pages
        view.getNewPageButton().addActionListener(e -> {
            model.addNewPage();
            view.addNewPageView(model);
            updateView();
        });
        view.getNewPageMenuItem().addActionListener(e -> {
            model.addNewPage();
            view.addNewPageView(model);
            updateView();
        });

        view.getDeletePageButton().addActionListener(e -> {
            int pageToDeleteIndex = model.getCurrentPageNumber() - 1;
            model.deleteCurrentPage();
            view.deleteCurrentPageView(pageToDeleteIndex);
            updateView();
        });
        view.getDeletePageMenuItem().addActionListener(e -> {
            int pageToDeleteIndex = model.getCurrentPageNumber() - 1;
            model.deleteCurrentPage();
            view.deleteCurrentPageView(pageToDeleteIndex);
            updateView();
        });

        view.getNextPageButton().addActionListener(e -> {
            model.nextPage();
            updateView();
        });
        view.getNextPageMenuItem().addActionListener(e -> {
            model.nextPage();
            updateView();
        });

        view.getPrevPageButton().addActionListener(e -> {
            model.previousPage();
            updateView();
        });
        view.getPrevPageMenuItem().addActionListener(e -> {
            model.previousPage();
            updateView();
        });

        //Add listener for Menu-Exit
        view.getExitMenuItem().addActionListener(e -> {
            System.exit(0);
        });
    }


    private void updateView() {
        //Get current status of page information
        int currentPageNumber = model.getCurrentPageNumber();
        int totalPages = model.getPageCount();
        int currentPageIndex = currentPageNumber - 1;

        // Display the correct page
        view.displayPage(currentPageIndex);

        // Update the number of staves (on a new page)
        MusicView currentMusicView = view.getMusicView();
        if (currentMusicView != null) {
            currentMusicView.setNumStaves(model.getStaves());

            // Show pitch
            currentMusicView.setOnPitchCalculated(pitch -> {
                view.getStatusBar().setText("Pitch: " + pitch);
            });
        }

        //Update page label
        view.getPageLabel().setText("Page " + currentPageNumber + " of " + totalPages);

        //Update page buttons
        //Delete Staff
        boolean canDeleteStaff = model.getStaves() > 1;
        view.getDeleteStaffButton().setEnabled(canDeleteStaff);
        view.getDeleteStaffMenuItem().setEnabled(canDeleteStaff);

        //Delete Page
        boolean canDeletePage = totalPages > 1;
        view.getDeletePageButton().setEnabled(canDeletePage);
        view.getDeletePageMenuItem().setEnabled(canDeletePage);

        //Previous
        boolean canGoPrev = currentPageNumber > 1;
        view.getPrevPageButton().setEnabled(canGoPrev);
        view.getPrevPageMenuItem().setEnabled(canGoPrev);

        //Next
        boolean canGoNext = currentPageNumber < totalPages;
        view.getNextPageButton().setEnabled(canGoNext);
        view.getNextPageMenuItem().setEnabled(canGoNext);

        // 切页或变更视图时，确保 Play/Stop 的启用状态与是否正在播放保持一致
        boolean playing = playThreadRef.get() != null;
        view.getPlayButton().setEnabled(!playing);
        view.getStopButton().setEnabled(playing);
    }


    /**
     * Inner class to handle streamlined tool selection via mouse drag on radio buttons.
     */
    private class ToolSelectionHandler implements MouseListener, MouseMotionListener {
        private int initialSliderValue;
        private int lastSliderValue;

        @Override
        public void mousePressed(MouseEvent e) {
            // Make sure the event source is JRadioButton
            if (e.getComponent() instanceof JRadioButton) {
                JRadioButton button = (JRadioButton) e.getComponent();
                // Select button
                button.setSelected(true);

                // Initial value of the slider
                initialSliderValue = view.getDurationSlider().getValue();
                lastSliderValue = initialSliderValue;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            // Drag sensitivity: for every 10 pixels you drag, the slider moves one grid
            final int PIXELS_PER_NOTCH = 10;

            // Get slider and its value range
            JSlider slider = view.getDurationSlider();
            int min = slider.getMinimum();
            int max = slider.getMaximum();

            // Calculate how many squares to move
            int notchesToMove = e.getY() / PIXELS_PER_NOTCH;

            // Calculate the new slider value
            int newSliderValue = initialSliderValue - notchesToMove;

            // Ensure new value is within the valid range of the slider
            newSliderValue = Math.max(min, Math.min(max, newSliderValue));

            // Update the slider only if the value changes
            if (newSliderValue != lastSliderValue) {
                slider.setValue(newSliderValue);
                lastSliderValue = newSliderValue;
            }
        }

        // --- methods to be implemented ---
        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }
}
