// main/java/com/gt/music/model/DurationMs.java
package com.gt.music.model;

import com.gt.music.types.NoteDuration;

public final class DurationMs {
    private DurationMs(){}
    public static int of(NoteDuration d){
        switch (d){
            case WHOLE:      return 1600; // 1.6s
            case HALF:       return 800;
            case QUARTER:    return 400;
            case EIGHTH:     return 200;
            case SIXTEENTH:  return 100;
            default:         return 400; // 兜底
        }
    }
}
