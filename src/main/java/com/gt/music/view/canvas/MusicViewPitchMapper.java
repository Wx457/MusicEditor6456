package com.gt.music.view.canvas;

import com.gt.music.types.NoteDuration;

import java.awt.*;

final class MusicViewPitchMapper {
    private MusicViewPitchMapper() {
    }

    /**
     * 等价于你原来的 getPositionPointForNote(duration)
     */
    static Point getPositionPointForNote(NoteDuration duration, double scaleFactor) {
        Point base;
        switch (duration) {
            case WHOLE:
                base = new Point(10, 6);
                break;
            case HALF:
                base = new Point(15, 34);
                break;
            case QUARTER:
                base = new Point(7, 35);
                break;
            case EIGHTH:
                base = new Point(15, 36);
                break;
            case SIXTEENTH:
                base = new Point(6, 35);
                break;
            default:
                base = new Point(0, 0);
                break;
        }
        int scaledX = (int) (base.x * scaleFactor);
        int scaledY = (int) (base.y * scaleFactor);
        return new Point(scaledX, scaledY);
    }

    /**
     * 等价于 calculatePitch(int absoluteY)
     */
    static String calculatePitch(
            int absoluteY,
            int numStaves,
            int topPadding,
            int staffHeight,
            int staffSpacing,
            double halfLineSpacing,
            String[] pitchNames) {

        // 1) 选最近的 staff（以 staff 中线距离最小为准）
        int bestTop = topPadding;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < numStaves; i++) {
            int top = topPadding + i * (staffHeight + staffSpacing);
            int center = top + staffHeight / 2;
            int dist = Math.abs(absoluteY - center);
            if (dist < bestDist) {
                bestDist = dist;
                bestTop = top;
            }
        }

        // 2) 相对该 staff 顶部按“半线距”计算 step（允许落在 staff 之外）
        int step = (int) Math.round((absoluteY - bestTop) / halfLineSpacing);

        // 3) 以 F5 为锚点：step=0 时应该落在 "F5" 的下标处
        int baseIndex = -1;
        for (int i = 0; i < pitchNames.length; i++) {
            if ("F5".equals(pitchNames[i])) { baseIndex = i; break; }
        }
        if (baseIndex < 0) {
            // 若没找到 F5（异常配置），退化为旧行为避免崩溃
            baseIndex = 0;
        }

        int idx = baseIndex + step; // 正常在 F5 上下沿半线距滚动
        if (idx < 0 || idx >= pitchNames.length) return "Outside";
        return pitchNames[idx];
    }


    /**
     *
     */
    static int getSnapYForPitch(
            String pitch,
            int staffTopY,
            double halfLineSpacing,
            String[] pitchNames) {

        for (int i = 0; i < pitchNames.length; i++) {
            if (pitchNames[i].equals(pitch)) {
                double standardSnapY = staffTopY + (i * halfLineSpacing);
                return (int) Math.round(standardSnapY);
            }
        }
        return -1;
    }
}
