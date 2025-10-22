package com.gt.music.app;

import com.gt.music.controller.MusicEditorController;
import com.gt.music.model.MusicEditorModel;
import com.gt.music.view.MusicEditorView;

import javax.swing.*;

public class MusicEditorApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            //1.Create Model
            MusicEditorModel model = new MusicEditorModel();
            //2.Create View
            MusicEditorView view = new MusicEditorView(model);
            //3.Create Controller
            new MusicEditorController(model, view);
            //4.Show window
            view.setVisible(true);
        });
    }
}
