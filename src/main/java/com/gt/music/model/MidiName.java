// main/java/com/gt/music/model/MidiName.java
package com.gt.music.model;

import com.gt.music.types.Accidental;

public final class MidiName {
    private MidiName(){}

    /**
     * @param base like "G4" / "C5"
     * @param acc  Accidental.NONE / SHARP / FLAT
     * @return e.g. "G#4" "Gb4" "G4"
     */
    public static String applyAccidental(String base, Accidental acc){
        if (base == null || base.length() < 2) return base;
        if (acc == null || acc == Accidental.NONE) return base;

        char letter = base.charAt(0);       // A..G
        String octave = base.substring(1);  // 3..6
        switch (acc){
            case SHARP: return letter + "#" + octave;
            case FLAT:  return letter + "b" + octave;
            default:    return base;
        }
    }
}
