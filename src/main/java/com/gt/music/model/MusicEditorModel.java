package com.gt.music.model;

import com.gt.music.types.EditMode;
import com.gt.music.types.NoteDuration;
import com.gt.music.types.ToolType;

import java.util.ArrayList;

public class MusicEditorModel {
    //Use ArrayList to store numStaves of each page
    private ArrayList<Integer> stavesPerPage;
    //0-based index for the current page
    private int currentPage;

    private ToolType currentTool;
    private NoteDuration currentDuration;
    private EditMode currentMode;

    public MusicEditorModel() {
        //Initialize page list
        this.stavesPerPage = new ArrayList<>();
        this.stavesPerPage.add(4);//Default value
        this.currentPage = 0; //First page

        this.currentTool = ToolType.NOTE;
        this.currentDuration = NoteDuration.QUARTER;
    }


    //PAGE MANAGE
    public int getPageCount() {
        return stavesPerPage.size();
    }

    public int getCurrentPageNumber() {
        return currentPage + 1;
    }

    public void addNewPage() {
        stavesPerPage.add(4);
        currentPage = getPageCount() - 1;
    }

    public void deleteCurrentPage() {
        if (getPageCount() > 1) {
            stavesPerPage.remove(currentPage);

            if (currentPage >= getPageCount()) {
                currentPage = getPageCount() - 1;
            }
        }
    }

    public void nextPage() {
        if (currentPage <= getPageCount() - 1) {
            currentPage++;
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
        }
    }


    //STAVES MANAGE
    public int getStaves() {
        return stavesPerPage.get(currentPage);
    }

    public void addStaff() {
        stavesPerPage.set(currentPage, getStaves() + 1);
    }

    public void deleteStaff() {
        int numStave = stavesPerPage.get(currentPage);
        if (numStave > 1) {
            stavesPerPage.set(currentPage, numStave - 1);
        }
    }


    public ToolType getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(ToolType currentTool) {
        this.currentTool = currentTool;
    }

    public NoteDuration getCurrentDuration() {
        return currentDuration;
    }

    public void setCurrentDuration(NoteDuration currentDuration) {
        this.currentDuration = currentDuration;
    }

    public EditMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(EditMode currentMode) {
        this.currentMode = currentMode;
    }
}
