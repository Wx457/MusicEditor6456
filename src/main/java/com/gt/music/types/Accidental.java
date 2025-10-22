package com.gt.music.types;

public enum Accidental {
    NONE, SHARP, FLAT;

    public String toSymbol() {
        switch (this) {
            case SHARP: return "♯";
            case FLAT:  return "♭";
            default:    return "";
        }
    }

    public String toText() {
        switch (this) {
            case SHARP: return "Sharp";
            case FLAT:  return "Flat";
            default:    return "";
        }
    }
}
