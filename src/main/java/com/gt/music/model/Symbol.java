package com.gt.music.model;
import com.gt.music.types.NoteDuration;

public abstract class Symbol {
    protected int x;
    protected int y;
    protected NoteDuration duration;

    public Symbol(int x, int y, NoteDuration duration) {
        this.x = x;
        this.y = y;
        this.duration = duration;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public NoteDuration getDuration() {
        return duration;
    }
}