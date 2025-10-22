package com.gt.music.model.playback;

import com.gt.music.midi.MIDI_Player;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaybackEngine implements Runnable {
    private final List<PlayEvent> timeline;
    private final MIDI_Player midi;
    private final Runnable onFinishUi;
    private volatile boolean stopRequested = false;

    public PlaybackEngine(List<PlayEvent> timeline, MIDI_Player midi, Runnable onFinishUi){
        this.timeline = timeline;
        this.midi = midi;
        this.onFinishUi = onFinishUi;
    }

    public void requestStop(){ stopRequested = true; }

    @Override public void run() {
        long start = System.currentTimeMillis();
        int idx = 0;
        Set<String> sounding = new HashSet<>();

        try{
            while (!stopRequested && idx < timeline.size()){
                long now = System.currentTimeMillis() - start;
                PlayEvent e = timeline.get(idx);

                if (now < e.atMs){
                    long sleep = Math.min(10, e.atMs - now); // 小步睡，Stop 响应更快
                    Thread.sleep(sleep);
                    continue;
                }

                if (e.type == PlayEvent.Type.START){
                    for (String name : e.noteNames){
                        midi.playMidiSound(name);
                        sounding.add(name);
                    }
                }else{ // STOP
                    for (String name : e.noteNames){
                        midi.stopMidiSound(name);
                        sounding.remove(name);
                    }
                }
                idx++;
            }
        } catch (InterruptedException ignored) {
        } finally {
            // 确保全部停掉
            for (String n : sounding){
                try { midi.stopMidiSound(n); } catch (Exception ignored){}
            }
            if (onFinishUi != null){
                SwingUtilities.invokeLater(onFinishUi);
            }
        }
    }
}
