package com.gt.music.model.playback;

import java.util.List;

public class PlayEvent implements Comparable<PlayEvent> {
    public enum Type { START, STOP }

    public final long atMs;
    public final Type type;
    public final List<String> noteNames;

    public PlayEvent(long atMs, Type type, List<String> noteNames){
        this.atMs = atMs; this.type = type; this.noteNames = noteNames;
    }

    @Override public int compareTo(PlayEvent o){
        return Long.compare(this.atMs, o.atMs);
    }
}
