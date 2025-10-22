package com.gt.music.model;
import com.gt.music.types.Accidental;
import com.gt.music.types.NoteDuration;

public class Note extends Symbol {
    private String pitch;
    private Accidental accidental = Accidental.NONE;

    public Note(int x, int y, NoteDuration duration) {
        super(x, y, duration);
        this.pitch = "";
    }

    // pitch
    public String getPitch() {
        return pitch;
    }
    public void setPitch(String pitch) {
        this.pitch = pitch;
    }

    // Accidental
    public Accidental getAccidental() { return accidental; }
    public void setAccidental(Accidental a) {
        if (a == null) a = Accidental.NONE;
        this.accidental = a;
    }

    // show accidental in status bar
    public String getDisplayPitchWithAccidentalSymbol() {
        // eg "G4♯" / "G4♭" / "G4"
        return getPitch() + accidental.toSymbol();
    }
    public String getDisplayPitchWithAccidentalText() {
        // eg "G4 Sharp" / "G4 Flat" / "G4"
        String t = accidental.toText();
        return t.isEmpty() ? getPitch() : (getPitch() + " " + t);
    }

}