package com.gt.music.model.playback;

import com.gt.music.model.Note;
import com.gt.music.model.Rest;
import com.gt.music.model.Symbol;
import com.gt.music.model.DurationMs;
import com.gt.music.model.MidiName;
import com.gt.music.types.Accidental;
import com.gt.music.types.NoteDuration;
import com.gt.music.model.playback.PlayEvent.Type;

import java.util.*;
import java.util.stream.Collectors;

public final class TimelineBuilder {
    private TimelineBuilder(){}

    /**
     * @param symbols 已按“阅读顺序（逐行从左到右）”排好的一页全部符号
     * @param chordTolerancePx 认为同一列（和弦）的 x 容差，建议 8~12
     */
    public static List<PlayEvent> build(List<Symbol> symbols, int chordTolerancePx){
        // 1) 保险起见，再按 x 排一次
        symbols.sort(Comparator.comparingInt(Symbol::getX));

        long now = 0;
        List<PlayEvent> events = new ArrayList<>();

        for (int i = 0; i < symbols.size(); ){
            Symbol s = symbols.get(i);

            if (s instanceof Rest){
                NoteDuration d = ((Rest) s).getDuration();
                now += DurationMs.of(d); // 休止只推进时间
                i++;
                continue;
            }

            // 收集同一列的音（和弦）
            int x0 = s.getX();
            List<Note> chord = new ArrayList<>();
            int j = i;
            while (j < symbols.size()){
                Symbol t = symbols.get(j);
                if (t instanceof Note && Math.abs(t.getX() - x0) <= chordTolerancePx){
                    chord.add((Note) t);
                    j++;
                }else break;
            }

            // 当前列的 START：把每个音的“基音名+升降”转换成 MIDI 名
            List<String> chordNames = chord.stream()
                    .map(TimelineBuilder::toMidiName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!chordNames.isEmpty()){
                events.add(new PlayEvent(now, Type.START, chordNames));
            }

            // 为每个音生成独立的 STOP（允许列内时值不同）
            for (Note n : chord){
                String name = toMidiName(n);
                if (name == null) continue;
                long stopAt = now + DurationMs.of(n.getDuration());
                events.add(new PlayEvent(stopAt, Type.STOP, Collections.singletonList(name)));
            }

            // 推进到下一列：用“本列最短时值”
            int delta = chord.stream()
                    .map(n -> DurationMs.of(n.getDuration()))
                    .min(Integer::compareTo).orElse(0);
            now += delta;

            i = j;
        }

        // 同一时刻先 STOP 再 START，避免卡音
        events.sort(Comparator
                .comparingLong((PlayEvent e) -> e.atMs)
                .thenComparing(e -> e.type == Type.STOP ? 0 : 1));
        return events;
    }

    // —— 工具：Note -> "C#4" / "Gb4" / "G4"
    private static String toMidiName(Note n){
        String base = n.getPitch();
        if (base == null || base.isEmpty()) return null; // 没有可播放的基音名
        Accidental a = n.getAccidental();
        return MidiName.applyAccidental(base, a);
    }
}
