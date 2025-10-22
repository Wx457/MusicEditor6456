package com.gt.music.view.canvas;

import com.gt.music.model.Note;
import com.gt.music.model.Symbol;
import java.util.List;
import java.awt.*;

final class MusicViewSnapper {

    // —— 新增：给 Snapper 一个“取 Note 宽度”的回调接口 ——
    @FunctionalInterface
    interface NoteWidthProvider {
        int getWidth(Note n); // 返回像素宽度（来自图片）
    }

    private Note snappedTo = null;
    private Integer snapAnchorX = null;
    private final int unsnapDx;

    public MusicViewSnapper(int unsnapDxPixels) {
        this.unsnapDx = Math.max(4, unsnapDxPixels);
    }

    public void onDragStart(Note dragging) {
        snappedTo = null;
        snapAnchorX = null;
    }

    // —— 改动点：多一个 widthProvider 参数；并用“中心点±半宽”来判断水平相交 ——
    public void onDragMove(Note dragging,
                           List<Symbol> staffItems,
                           int mouseX,
                           int aHalfW,
                           NoteWidthProvider widthProvider) {
        if (dragging == null) return;

        if (isSnapped()) {
            if (Math.abs(mouseX - snapAnchorX) > unsnapDx) {
                snappedTo = null;
                snapAnchorX = null;
            }
            return;
        }

        // A 的水平范围：以鼠标为中心（你在外面已做居中对齐）
        int aLeft = mouseX - aHalfW;
        int aRight = mouseX + aHalfW;

        for (Symbol s : staffItems) {
            if (!(s instanceof Note)) continue;
            if (s == dragging) continue;
            Note b = (Note) s;

            int bW = Math.max(1, widthProvider.getWidth(b));
            // 你的坐标是“左上角”，因此 B 的范围 = [b.getX(), b.getX() + bW]
            int bLeft = b.getX();
            int bRight = b.getX() + bW;

            boolean horizontalOverlap = (aRight >= bLeft) && (aLeft <= bRight);
            if (horizontalOverlap) {
                snappedTo = b;
                // 题意是“同一水平位置”，通常锁中心 X 更自然（和你 bboxForSymbol 的中心一致）
                snapAnchorX = b.getX();
                break;
            }
        }
    }

    public void onDragEnd() { }

    public Integer snappedXOrNull() { return snapAnchorX; }

    public boolean isSnapped() { return snappedTo != null && snapAnchorX != null; }

    // —— Utils ——
    static int nearestStaffTopY(int absoluteY, int topPadding, int staffHeight, int staffSpacing, int numStaves) { /* 原样 */
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
        return bestTop;
    }

    static int snapY(int rawY, int staffTopY, double halfLineSpacing, int tolerancePx) { /* 原样 */
        double idx = Math.rint((rawY - staffTopY) / halfLineSpacing);
        int snapped = (int) Math.round(staffTopY + idx * halfLineSpacing);
        return (Math.abs(rawY - snapped) <= tolerancePx) ? snapped : rawY;
    }

    static int snapX(int rawX, int leftPadding, int gridStepPx, int tolerancePx) { /* 原样 */
        if (gridStepPx <= 0) return rawX;
        int local = rawX - leftPadding;
        int k = (int) Math.round((double) local / gridStepPx);
        int snapped = leftPadding + k * gridStepPx;
        return (Math.abs(rawX - snapped) <= tolerancePx) ? snapped : rawX;
    }

    static Point snapPoint(Point raw, int leftPadding, int staffTopY, double halfLineSpacing, int yTolerancePx, int gridStepPx, int xTolerancePx) { /* 原样 */
        int y = snapY(raw.y, staffTopY, halfLineSpacing, yTolerancePx);
        int x = (gridStepPx > 0) ? snapX(raw.x, leftPadding, gridStepPx, xTolerancePx) : raw.x;
        return new Point(x, y);
    }

    static Rectangle bboxForSymbol(Symbol s, int noteW, int noteH) { /* 原样 */
        return new Rectangle(
                s.getX() - noteW / 2,
                s.getY() - noteH / 2,
                noteW,
                noteH
        );
    }

    static Note hitTestNote(int mx, int my, java.util.List<Note> notes, MusicViewImages.Bank bank) { /* 原样 */
        for (Note n : notes) {
            Image img = MusicViewImages.forSymbol(n, bank);
            if (img == null) continue;
            int w = img.getWidth(null);
            int h = img.getHeight(null);
            Rectangle r = new Rectangle(
                    n.getX() - w / 2,
                    n.getY() - h / 2,
                    w, h);
            if (r.contains(mx, my)) return n;
        }
        return null;
    }
}
