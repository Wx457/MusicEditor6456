package com.gt.music.view.canvas;

import com.gt.music.gestures.GestureStroke;
import com.gt.music.gestures.ScratchOutDetector;
import com.gt.music.model.MusicEditorModel;
import com.gt.music.model.Note;
import com.gt.music.model.Rest;
import com.gt.music.model.Symbol;
import com.gt.music.types.Accidental;
import com.gt.music.types.EditMode;
import com.gt.music.types.NoteDuration;
import com.gt.music.types.ToolType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import dollar.DollarRecognizer;
import dollar.Result;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.awt.Point;
import java.util.function.Consumer;


public class MusicView extends JComponent {

    private static final int STAFF_WIDTH = 1000;
    private static final int STAFF_HEIGHT = 60;
    private static final int TOP_PADDING = 60;
    private static final int LEFT_PADDING = 60;
    private static final int STAFF_SPACING = 100;
    private static final double SCALE_FACTOR = 1.5;
    private static final double SCRATCH_DIR_RATIO = 4.0;  // 横纵位移比例阈值
    private static final int SCRATCH_MIN_REVERSALS = 2;   // 至少两次水平反转

    private static final String[] PITCH_NAMES = {
            "D6","C6","B5","A5","G5",
            "F5","E5","D5","C5","B4","A4","G4","F4","E4",
            "D4","C4","B3","A3","G3"
    };
    private static final double HALF_LINE_SPACING = (double) STAFF_HEIGHT / 8.0;
    // yTolerance 吸附常量
    private static final int SNAP_Y_TOLERANCE_PX = (int) Math.round(HALF_LINE_SPACING);

    private int numStaves = 4;
    private MusicEditorModel model;
    private ArrayList<Symbol> symbols;
    private Symbol activeSymbol = null;
    private Symbol selectedSymbol = null;
    private Consumer<String> onPitchCalculatedCallback;

    // 拖拽升降号期间的 UI 状态
    private boolean draggingAccidental = false;
    private boolean showAllNoteBBoxes = false;
    private int accidentalX, accidentalY;
    private Note selectedAccidentalNote = null;  // 仅表示“选中了某个音符的accidental”
    private Consumer<String> statusSink = null;

    private final MusicViewSnapper snapper = new MusicViewSnapper(12);
    // 记录拖拽期间所属的 staff，跨 staff 时用于重置吸附
    private Integer _lastDragStaffTop = null;

    private List<Point2D> currentStroke = new ArrayList<>();
    private boolean isDrawingStroke = false;
    private final DollarRecognizer recognizer = new DollarRecognizer();

    // imports: com.gt.music.gestures.*
    private GestureStroke gesCurStroke = null;
    private final ScratchOutDetector scratchOutDetector = new ScratchOutDetector();

    /**
     * View.Component.main.java.com.gt.music.view.canvas.MusicView Constructor
     */
    public MusicView(MusicEditorModel model) {
        this.model = model;
        this.symbols = new ArrayList<>();

        updatePreferredSize();

        MouseHandler handler = new MouseHandler();
        this.addMouseListener(handler);
        this.addMouseMotionListener(handler);
        this.addKeyListener(handler);
        this.setFocusable(true);
        this.sprites = MusicViewImages.loadDefaultFromResources(SCALE_FACTOR);
    }

    /**
     * paintComponent function: override JComponent
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g; // 统一用 g2

        // 抗锯齿应在绘制前开启（至少在画笔迹前；建议全程开启）
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // --- Drawing staff background ---
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // --- Drawing staff ---
        g2.setColor(Color.BLACK);
        for (int i = 0; i < numStaves; i++) {
            int staffY = TOP_PADDING + i * (STAFF_HEIGHT + STAFF_SPACING);

            int lineSpacing = STAFF_HEIGHT / 4;
            for (int j = 0; j < 5; j++) {
                int lineY = staffY + j * lineSpacing;
                g2.drawLine(LEFT_PADDING, lineY, LEFT_PADDING + STAFF_WIDTH, lineY);
            }

            // Left/Right borders
            g2.drawLine(LEFT_PADDING, staffY, LEFT_PADDING, staffY + STAFF_HEIGHT);
            g2.drawLine(LEFT_PADDING + STAFF_WIDTH, staffY, LEFT_PADDING + STAFF_WIDTH, staffY + STAFF_HEIGHT);

            // End line for last staff
            if (i == numStaves - 1) {
                g2.drawLine(LEFT_PADDING + STAFF_WIDTH - 6, staffY, LEFT_PADDING + STAFF_WIDTH - 6, staffY + STAFF_HEIGHT);
                g2.fillRect(LEFT_PADDING + STAFF_WIDTH - 3, staffY, 3, STAFF_HEIGHT);
            } else {
                g2.drawLine(LEFT_PADDING + STAFF_WIDTH, staffY, LEFT_PADDING + STAFF_WIDTH, staffY + STAFF_HEIGHT);
            }

            // Clef & time
            Image treble = sprites.get(MusicViewImages.Key.CLEF_TREBLE);
            if (treble != null) {
                g2.drawImage(treble, LEFT_PADDING + 10, staffY - 22, this);
            }
            Image common = sprites.get(MusicViewImages.Key.TIME_COMMON);
            if (common != null) {
                g2.drawImage(common, LEFT_PADDING + 80, staffY + 9, this);
            }
        }

        // --- Drawing items (notes/rests) ---  （原样保留）
        for (Symbol symbol : symbols) {
            if (symbol instanceof Note) {
                Note n = (Note) symbol;
                drawLedgerLines(g2, n);

                Image noteImg = getImageForSymbol(n);
                if (noteImg != null) g2.drawImage(noteImg, n.getX(), n.getY(), null);

                Accidental a = n.getAccidental();
                if (a != Accidental.NONE) {
                    Image accImg = MusicViewImages.forAccidental(a, sprites);
                    if (accImg != null) {
                        int aw = accImg.getWidth(null);
                        int ax = n.getX() - aw - 4;
                        int ay = n.getY() + 35;
                        g2.drawImage(accImg, ax, ay, null);
                    }
                }
            } else if (symbol instanceof Rest) {
                Image restImg = getImageForSymbol(symbol);
                if (restImg != null) g2.drawImage(restImg, symbol.getX(), symbol.getY(), null);
            }

            Image img = getImageForSymbol(symbol);
            if (symbol == selectedSymbol && img != null) {
                g2.setColor(Color.BLUE);
                g2.drawRect(symbol.getX(), symbol.getY(), img.getWidth(null), img.getHeight(null));
                g2.setColor(Color.BLACK);
            }
        }

        // --- NOTES全局包围盒高亮（原样） ---
        if (showAllNoteBBoxes) {
            g2.setColor(new Color(0, 120, 215, 140));
            for (Symbol s : symbols) {
                if (s instanceof Note) {
                    Image noteImg = MusicViewImages.forSymbol(s, sprites);
                    if (noteImg == null) continue;
                    int w = noteImg.getWidth(null);
                    int h = noteImg.getHeight(null);
                    g2.drawRect(s.getX(), s.getY(), w, h);
                }
            }
            g2.setColor(Color.BLACK);
        }

        // --- 拖拽中的 Accidental 预览（原样） ---
        if (draggingAccidental) {
            ToolType tool = model.getCurrentTool();
            Accidental a =
                    (tool == ToolType.SHARP) ? Accidental.SHARP :
                            (tool == ToolType.FLAT)  ? Accidental.FLAT  :
                                    Accidental.NONE;

            if (a != Accidental.NONE) {
                Image accImg = MusicViewImages.forAccidental(a, sprites);
                if (accImg != null) {
                    int aw = accImg.getWidth(null);
                    int ah = accImg.getHeight(null);
                    int px = accidentalX - aw / 2;
                    int py = accidentalY - ah / 2;
                    g2.drawImage(accImg, px, py, null);
                }
            }
        }

        // --- accidental 的选中高亮（原样） ---
        if (selectedAccidentalNote != null) {
            Accidental a = selectedAccidentalNote.getAccidental();
            if (a != Accidental.NONE) {
                Image accImg = MusicViewImages.forAccidental(a, sprites);
                if (accImg != null) {
                    int aw = accImg.getWidth(null), ah = accImg.getHeight(null);
                    int ax = selectedAccidentalNote.getX() - aw - 4;
                    int ay = selectedAccidentalNote.getY() + 35;

                    Color old = g2.getColor();
                    g2.setColor(Color.MAGENTA);
                    g2.drawRect(ax, ay, aw, ah);
                    g2.setColor(old);
                }
            }
        }

        // --- 置顶绘制：当前笔迹（Stroke Preview） ---
        if (currentStroke != null && currentStroke.size() >= 2) {
            Color oldColor = g2.getColor();
            Stroke oldStroke = g2.getStroke();

            g2.setColor(Color.RED);
            // 圆角更像“墨迹”
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 1; i < currentStroke.size(); i++) {
                Point2D p1 = currentStroke.get(i - 1);
                Point2D p2 = currentStroke.get(i);
                g2.drawLine((int) p1.getX(), (int) p1.getY(),
                        (int) p2.getX(), (int) p2.getY());
            }

            // 恢复现场
            g2.setStroke(oldStroke);
            g2.setColor(oldColor);
        }
    }

    /**
     * 启发式检测并执行“划除（scratch-out）”。
     * 满足阈值即视为划除；会删除与笔迹包围盒相交的符号：
     * - Rest：直接删除
     * - Note：若只命中 accidental 区域则去掉 accidental；否则删除整颗音符
     *
     * @return true 表示判定为 scratch-out（无论是否删到对象）；
     *         false 表示不是 scratch-out（应交由 $1 识别器处理）
     */
    private boolean tryScratchOut(List<Point2D> stroke) {
        if (stroke == null || stroke.size() < 2) return false;

        // 1) 统计：水平反转次数、累计横/纵位移
        int reversals = 0;
        double totalDx = 0.0, totalDy = 0.0;

        // 当前方向：-1 (向左), 0 (未知), +1 (向右)
        int lastDir = 0;

        for (int i = 1; i < stroke.size(); i++) {
            Point2D p0 = stroke.get(i - 1);
            Point2D p1 = stroke.get(i);
            double dx = p1.getX() - p0.getX();
            double dy = p1.getY() - p0.getY();

            totalDx += Math.abs(dx);
            totalDy += Math.abs(dy);

            int dir = 0;
            if (dx > 0) dir = +1;
            else if (dx < 0) dir = -1;

            if (dir != 0 && lastDir != 0 && dir != lastDir) {
                reversals++;
            }
            if (dir != 0) lastDir = dir;
        }

        // 2) 计算横/纵比例（避免除零）
        double ratio = (totalDy == 0.0) ? Double.POSITIVE_INFINITY : (totalDx / totalDy);

        // 3) 是否满足 scratch-out 阈值
        boolean looksLikeScratch = (reversals >= SCRATCH_MIN_REVERSALS) && (ratio >= SCRATCH_DIR_RATIO);
        if (!looksLikeScratch) return false;

        // 4) 计算笔迹包围盒（整条 stroke 的 min/max）
        Rectangle scratchBox = strokeBoundingBox(stroke);

        // 5) 执行删除逻辑
        //    - 与 scratchBox 相交的 Rest 直接删除
        //    - 与 scratchBox 相交的 Note：若只命中 accidental 区域则去掉 accidental；否则删除 Note
        List<Symbol> toRemove = new ArrayList<>();
        int accidentalClearedCount = 0;

        for (Symbol s : new ArrayList<>(symbols)) {
            Rectangle symBox = getSymbolBounds(s);
            if (symBox == null) continue;

            if (!symBox.intersects(scratchBox)) {
                continue; // 不相交，跳过
            }

            if (s instanceof Rest) {
                toRemove.add(s);
            } else if (s instanceof Note) {
                Note n = (Note) s;

                // 计算 note 图像包围盒（音头/符干）
                Rectangle noteBox = getSymbolBounds(n);
                boolean noteHit = noteBox != null && noteBox.intersects(scratchBox);

                // 计算 accidental 区域包围盒（若有）
                Rectangle accBox = getAccidentalBoundsIfAny(n);

                boolean accHit = (accBox != null) && accBox.intersects(scratchBox);

                if (accHit && !noteHit) {
                    // 只划到了升降号 → 清除 accidental
                    if (n.getAccidental() != Accidental.NONE) {
                        n.setAccidental(Accidental.NONE);
                        accidentalClearedCount++;
                    }
                } else {
                    // 划到音头/或同时划到音头与升降号 → 删除整颗 Note
                    toRemove.add(n);
                }
            }
        }

        // 应用删除
        if (!toRemove.isEmpty()) {
            symbols.removeAll(toRemove);
        }

        // 状态栏提示
        if (!toRemove.isEmpty() && accidentalClearedCount > 0) {
            updateStatus("Scratch-out: removed " + toRemove.size() + " symbols, cleared " + accidentalClearedCount + " accidental(s).");
        } else if (!toRemove.isEmpty()) {
            updateStatus("Scratch-out: removed " + toRemove.size() + " symbols.");
        } else if (accidentalClearedCount > 0) {
            updateStatus("Scratch-out: cleared " + accidentalClearedCount + " accidental(s).");
        } else {
            updateStatus("Scratch-out: no symbols intersected.");
        }

        // 6) 无论是否删到对象，只要满足阈值就视为 scratch-out（不再交给 $1）
        return true;
    }

    /** 非划除时，把 stroke 丢给 $1 识别器，并据返回结果新增 Note/Rest/Accidental 或提示失败 */
    private void handleStrokeForRecognition(List<Point2D> stroke) {
        if (stroke == null || stroke.isEmpty()) return;

        // 1️调用 $1 Recognizer
        Result result = recognizer.recognize(new ArrayList<>(stroke));
        String name = result.getName();     // 模板名称
        double score = result.getScore();   // 匹配得分

        // 调试输出：打印识别结果
        System.out.println("Recognized name: " + name + " (score=" + score + ")");

        // 如果未识别到
        if (name == null || name.equalsIgnoreCase("No match")) {
            updateStatus("Unrecognized gesture: no match found.");
            return;
        }

        // 获取起始点坐标（用于放置符号）
        Point2D start = stroke.get(0);
        int x = (int) start.getX();
        int y = (int) start.getY();

        // 根据名称决定类型
        Symbol newSymbol = null;
        if (name.contains("note") || name.contains("circle")) {
            NoteDuration dur = mapDurationFromName(name);
            newSymbol = new Note(x, y, dur);
            symbols.add(newSymbol);
            updateStatus("Recognized: " + name + " → Note added at (" + x + ", " + y + ")");
        } else if (name.contains("rest") || name.contains("rectangle") || name.contains("right curly brace")) {
            NoteDuration dur = mapDurationFromName(name);
            newSymbol = new Rest(x, y, dur);
            symbols.add(newSymbol);
            updateStatus("Recognized: " + name + " → Rest added at (" + x + ", " + y + ")");
        } else if (name.contains("star") || name.contains("flat")) {
            Symbol target = findNoteAtPoint(x, y);
            if (target instanceof Note) {
                Note note = (Note) target;
                if (name.contains("star")) note.setAccidental(Accidental.SHARP);
                else note.setAccidental(Accidental.FLAT);
                updateStatus("Recognized: " + name + " → applied to note " + note.getPitch());
            } else {
                updateStatus("Recognized " + name + " but not over a note → ignored.");
            }
        } else {
            updateStatus("Recognized: " + name + " (no matching action)");
        }

        repaint();
    }

    /** 计算笔迹包围盒（基于所有点的 min/max） */
    private Rectangle strokeBoundingBox(List<Point2D> stroke) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Point2D p : stroke) {
            double x = p.getX(), y = p.getY();
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
        }
        int x = (int) Math.floor(minX);
        int y = (int) Math.floor(minY);
        int w = (int) Math.ceil(maxX - minX);
        int h = (int) Math.ceil(maxY - minY);
        // 避免 0 宽/高导致 intersects 边界不稳定
        if (w <= 0) w = 1;
        if (h <= 0) h = 1;
        return new Rectangle(x, y, w, h);
    }

    /** 返回符号的包围盒（基于已加载的贴图尺寸与符号 x/y） */
    private Rectangle getSymbolBounds(Symbol s) {
        Image img = getImageForSymbol(s);
        if (img == null) return null;
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w <= 0 || h <= 0) return null;
        return new Rectangle(s.getX(), s.getY(), w, h);
    }

    /**
     * 计算 Note 的 accidental（若存在）的包围盒；坐标与 paintComponent 中绘制一致：
     *   ax = noteX - accWidth - 4;
     *   ay = noteY + 35;
     */
    private Rectangle getAccidentalBoundsIfAny(Note n) {
        Accidental a = n.getAccidental();
        if (a == Accidental.NONE) return null;

        Image accImg = MusicViewImages.forAccidental(a, sprites);
        if (accImg == null) return null;

        int aw = accImg.getWidth(null);
        int ah = accImg.getHeight(null);
        if (aw <= 0 || ah <= 0) return null;

        int ax = n.getX() - aw - 4;
        int ay = n.getY() + 35;
        return new Rectangle(ax, ay, aw, ah);
    }











    // 把现有字段映射到 Key
    private MusicViewImages.Bank sprites;


    private Image getImageForSymbol(Symbol symbol) {
        return MusicViewImages.forSymbol(symbol, sprites);
    }


    public void setNumStaves(int count) {
        this.numStaves = count;

        updatePreferredSize();
        this.repaint();
    }

    private void updatePreferredSize() {
        int newWidth = LEFT_PADDING * 2 + STAFF_WIDTH;
        // Total height
        int newHeight = (TOP_PADDING * 2) + (numStaves * STAFF_HEIGHT) + ((numStaves - 1) * STAFF_SPACING);

        // New preferred size
        setPreferredSize(new Dimension(newWidth, newHeight));

        revalidate();
    }


    /**
     * Private inner class: handling all mouse events
     */
    private class MouseHandler implements MouseListener, MouseMotionListener, KeyListener {

        @Override
        public void mousePressed(MouseEvent e) {
            // 保证能收到拖拽与键盘事件
            MusicView.this.requestFocusInWindow();

            // ========== 1) PEN：开始记录笔迹（Ink Recognition） ==========
            if (isPenMode()) {
                if (currentStroke != null) currentStroke.clear();
                currentStroke.add(e.getPoint());
                isDrawingStroke = true;

                // 开始自由墨迹时清理选中态，避免跨模式残留影响
                activeSymbol = null;
                selectedSymbol = null;
                selectedAccidentalNote = null;

                repaint();
                return;
            }

            // ========== 2) DRAW：普通绘制（NOTE / REST / SHARP / FLAT） ==========
            if (isDrawMode()) {
                ToolType tool = model.getCurrentTool();
                NoteDuration dur = model.getCurrentDuration();

                // 升降号：进入拖拽态，提前返回
                if (tool == ToolType.SHARP || tool == ToolType.FLAT) {
                    startAccidentalDrag(e.getX(), e.getY());
                    return;
                }

                // Note / Rest：创建并以鼠标为中心落点
                Symbol newSymbol = null;
                if (tool == ToolType.NOTE) {
                    newSymbol = new Note(e.getX(), e.getY(), dur);
                } else if (tool == ToolType.REST) {
                    newSymbol = new Rest(e.getX(), e.getY(), dur);
                }

                if (newSymbol != null) {
                    Image img = getImageForSymbol(newSymbol);
                    if (img != null) {
                        newSymbol.setX(e.getX() - img.getWidth(null)  / 2);
                        newSymbol.setY(e.getY() - img.getHeight(null) / 2);
                    }
                    symbols.add(newSymbol);
                    activeSymbol = newSymbol;
                    repaint();
                }
                return;
            }

            // ========== 3) SELECT：命中检测（从后往前） ==========
            if (isSelectMode()) {
                boolean hit = false;

                for (int i = symbols.size() - 1; i >= 0; i--) {
                    Symbol s = symbols.get(i);
                    Image img = getImageForSymbol(s);
                    if (img == null) continue;

                    Rectangle bounds = new Rectangle(s.getX(), s.getY(), img.getWidth(null), img.getHeight(null));
                    if (!bounds.contains(e.getPoint())) continue;

                    // 命中符号
                    selectedSymbol = s;
                    activeSymbol   = s;
                    hit = true;

                    if (s instanceof Note) {
                        // 进入拖拽准备态 & 状态栏更新
                        snapper.onDragStart((Note) s);
                        updatePitchStatus((Note) s);
                        repaint();
                        return; // 命中 Note 时直接返回，避免后续逻辑覆盖状态栏
                    }
                    break; // 命中了非 Note，跳出循环到后续逻辑
                }

                // 若没命中任何符号，清空选中态
                if (!hit) {
                    selectedSymbol = null;
                }

                // 额外：检查是否命中“已存在的 accidental 区域”
                Note hitAcc = findAccidentalAt(e.getX(), e.getY());
                if (hitAcc != null) {
                    selectedAccidentalNote = hitAcc;
                    selectedSymbol = null; // accidental 高亮优先，避免与符号选中冲突
                    updatePitchStatus(hitAcc);
                    repaint();
                    return;
                } else {
                    // 点空白：取消 accidental 的选择
                    selectedAccidentalNote = null;
                    if (!hit) updateStatus("Ready");
                }

                repaint();
                return;
            }

            // （如果还有其它模式，走到这里再按需处理；当前三态已覆盖，留作安全网）
            repaint();
        }


        @Override
        public void mouseDragged(MouseEvent e) {
            // 1) PEN：记录自由墨迹的轨迹点
            if (isPenMode()) {
                // 可选：过滤过密点，减少噪声
                if (currentStroke != null) {
                    Point2D p = e.getPoint();
                    if (currentStroke.isEmpty()) {
                        currentStroke.add(p);
                    } else {
                        Point2D last = currentStroke.get(currentStroke.size() - 1);
                        double dx = p.getX() - last.getX();
                        double dy = p.getY() - last.getY();
                        // 仅当移动足够远再记点，避免每像素都记
                        if ((dx*dx + dy*dy) >= 4.0) { // 距离阈值≈2px
                            currentStroke.add(p);
                        }
                    }
                }
                repaint();
                return;
            }

            // 2) Accidental 拖拽（你原有的逻辑）
            if (draggingAccidental) {
                updateAccidentalDrag(e.getX(), e.getY());
                return;
            }

            // 3) 选中符号拖拽（你原有的大段逻辑，原样保留）
            if (activeSymbol != null) {
                Image img = getImageForSymbol(activeSymbol);

                if (activeSymbol instanceof Note) {
                    Note dragging = (Note) activeSymbol;

                    int halfW = (img != null) ? img.getWidth(null) / 2 : 8;
                    int halfH = (img != null) ? img.getHeight(null) / 2 : 8;

                    java.util.List<Symbol> sameStaff = getSymbolsOnSameStaff(dragging);

                    int aCenterYNow = (img != null) ? (dragging.getY() + img.getHeight(null) / 2) : dragging.getY();
                    int staffTopNow = MusicViewSnapper.nearestStaffTopY(
                            aCenterYNow, TOP_PADDING, STAFF_HEIGHT, STAFF_SPACING, numStaves);

                    if (_lastDragStaffTop == null) {
                        _lastDragStaffTop = staffTopNow;
                    } else if (!_lastDragStaffTop.equals(staffTopNow)) {
                        snapper.onDragStart(dragging);
                        _lastDragStaffTop = staffTopNow;
                    }

                    snapper.onDragMove(
                            dragging,
                            sameStaff,
                            e.getX(),
                            halfW,
                            (Note n) -> {
                                Image im = getImageForSymbol(n);
                                return (im != null) ? im.getWidth(null) : (halfW * 2);
                            }
                    );

                    Integer snappedX = snapper.snappedXOrNull();
                    int newX = (snappedX != null) ? snappedX : (e.getX() - halfW);
                    int newY = (img != null) ? (e.getY() - halfH) : e.getY();

                    dragging.setX(newX);
                    dragging.setY(newY);

                    int headY = noteheadCenterY(dragging);
                    String pitch = MusicViewPitchMapper.calculatePitch(
                            headY,
                            numStaves,
                            TOP_PADDING, STAFF_HEIGHT, STAFF_SPACING,
                            HALF_LINE_SPACING,
                            PITCH_NAMES
                    );
                    dragging.setPitch(pitch);
                    updatePitchStatus(dragging);

                } else {
                    if (img != null) {
                        activeSymbol.setX(e.getX() - img.getWidth(null) / 2);
                        activeSymbol.setY(e.getY() - img.getHeight(null) / 2);
                    } else {
                        activeSymbol.setX(e.getX());
                        activeSymbol.setY(e.getY());
                    }
                }

                MusicView.this.repaint();
            }
        }


        @Override
        public void mouseReleased(MouseEvent e) {
            // --- 0) 识别模式：先处理笔迹（优先 scratch-out，再交给 $1） ---
            if (isPenMode() && currentStroke != null && !currentStroke.isEmpty()) {
                isDrawingStroke = false; // 如果你有这个标记的话
                boolean scratched = tryScratchOut(currentStroke); // ✅ 先做启发式“划除”判断并执行删除
                if (!scratched) {
                    handleStrokeForRecognition(currentStroke);     // ✅ 非划除则走 $1 Recognizer（下一步实现）
                }
                currentStroke.clear(); // 轨迹必须消失（Rubric 要求）
                repaint();
                return;
            }

            // --- 1) 拖拽升降号的结束 ---
            if (draggingAccidental) {
                boolean attached = tryAttachAccidental(e.getX(), e.getY());
                if (!attached) updateStatus("Canceled: no note hit.");
                endAccidentalDrag();
                repaint();
                return;
            }

            // --- 2) 结束符号拖拽（Note / Rest）---
            if (activeSymbol != null) {
                // Drag Note
                if (activeSymbol instanceof Note) {
                    Note activeNote = (Note) activeSymbol;

                    // 1) 用音头中心 Y 计算音高（覆盖 G3..D6）
                    int headY = noteheadCenterY(activeNote);
                    String pitch = MusicViewPitchMapper.calculatePitch(
                            headY, numStaves, TOP_PADDING, STAFF_HEIGHT, STAFF_SPACING,
                            HALF_LINE_SPACING, PITCH_NAMES);
                    activeNote.setPitch(pitch);
                    if (onPitchCalculatedCallback != null) onPitchCalculatedCallback.accept(pitch);

                    // 2) 纵向吸附（贴线/贴间）
                    int staffTopY = findStaffTopY(headY); // 若返回 -1 就不 snap
                    if (staffTopY != -1) {
                        int snappedHeadY = MusicViewSnapper.snapY(
                                headY, staffTopY, HALF_LINE_SPACING, SNAP_Y_TOLERANCE_PX);

                        Image img = getImageForSymbol(activeNote);
                        int noteH = (img != null) ? img.getHeight(null) : 0;
                        int lineSpacing = (int) Math.round(2 * HALF_LINE_SPACING);
                        activeNote.setY(snappedHeadY - (noteH - lineSpacing / 2));
                        MusicView.this.repaint();
                    }

                    // 最后更新一次状态栏
                    updatePitchStatus(activeNote);
                    repaint();

                    // Drag Rest
                } else if (activeSymbol instanceof Rest) {
                    Image img = getImageForSymbol(activeSymbol);
                    int imgW = (img != null) ? img.getWidth(null) : 0;
                    int imgH = (img != null) ? img.getHeight(null) : 0;

                    int centerX = activeSymbol.getX() + imgW / 2;
                    int centerY = activeSymbol.getY() + imgH / 2;

                    int staffTopY = findStaffTopY(centerY);
                    if (staffTopY != -1) {
                        int midY = staffTopY + (STAFF_HEIGHT / 2); // 中线
                        activeSymbol.setY(midY - imgH / 2);
                        MusicView.this.repaint();
                    }
                }
            }

            activeSymbol = null;
        }


        private int findStaffTopY(int absoluteY) {
            return MusicViewSnapper.nearestStaffTopY(
                    absoluteY, TOP_PADDING, STAFF_HEIGHT, STAFF_SPACING, numStaves
            );
        }

        // 判断当前模式：选择SELECT/自由绘制PEN/普通绘制DRAW
        private boolean isPenMode()  { return model.getCurrentMode() == EditMode.PEN; }
        private boolean isDrawMode() { return model.getCurrentMode() == EditMode.DRAW; }
        private boolean isSelectMode() { return model.getCurrentMode() == EditMode.SELECT; }




        // --- Other methods ---
        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }


        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {

            if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                if (selectedAccidentalNote != null) {
                    Note note = selectedAccidentalNote;

                    // 清除状态
                    note.setAccidental(Accidental.NONE);

                    updatePitchStatus(note);

                    // 保持选中该音符
                    selectedAccidentalNote = note;

                    repaint();
                    return;
                }

                if (selectedSymbol != null) {
                    symbols.remove(selectedSymbol);
                    selectedSymbol = null;
                    MusicView.this.repaint();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }


    public void startAccidentalDrag(int x, int y) {
        draggingAccidental = true;
        showAllNoteBBoxes = true;
        accidentalX = x;
        accidentalY = y;
        repaint();
    }

    public void updateAccidentalDrag(int x, int y) {
        accidentalX = x;
        accidentalY = y;
        repaint();
    }

    public boolean tryAttachAccidental(int x, int y) {
        // 1) 收集当前可检测的 notes
        java.util.List<Note> notes = new java.util.ArrayList<>();
        for (Symbol s : symbols) if (s instanceof Note) notes.add((Note) s);

        // 2) 用 bank + forSymbol 逐个计算 bbox 后做命中
        Note target = null;
        for (Note n : notes) {
            Image img = MusicViewImages.forSymbol(n, sprites);
            if (img == null) continue;

            int w = img.getWidth(null), h = img.getHeight(null);
            if (w <= 0 || h <= 0) {
                // 兜底，避免惰性图像导致宽高为 -1
                javax.swing.ImageIcon icon = new javax.swing.ImageIcon(img);
                w = icon.getIconWidth(); h = icon.getIconHeight();
                if (w <= 0 || h <= 0) continue;
            }

            java.awt.Rectangle r = new java.awt.Rectangle(n.getX(), n.getY(), w, h);
            if (r.contains(x, y)) { target = n; break; }

        }
        if (target == null) return false;

        // 3) 写入 accidental（唯一性自然替换）
        ToolType tool = model.getCurrentTool();
        String oldPitch = target.getPitch();
        if (tool == ToolType.SHARP) {
            target.setAccidental(Accidental.SHARP);
        } else if (tool == ToolType.FLAT) {
            target.setAccidental(Accidental.FLAT);
        } else {
            return false;
        }

        updatePitchStatus(target);
        return true;
    }

    public void endAccidentalDrag() {
        draggingAccidental = false;
        showAllNoteBBoxes = false;
        repaint();
    }

    private Note findAccidentalAt(int mx, int my) {
        for (Symbol s : symbols) {
            if (!(s instanceof Note)) continue;
            Note n = (Note) s;
            Accidental a = n.getAccidental();
            if (a == Accidental.NONE) continue;

            Image accImg = MusicViewImages.forAccidental(a, sprites);
            if (accImg == null) continue;

            int aw = accImg.getWidth(null), ah = accImg.getHeight(null);

            // 与你当前绘制accidental一致的坐标（保持完全同一套算法！）
            int ax = n.getX() - aw - 4;
            // 垂直对齐：如果你已经改成按音头对齐，就用那套；否则用顶部对齐
            int ay = n.getY() + 35;  // 或：int ay = headCenterY(n) - ah/2;

            java.awt.Rectangle r = new java.awt.Rectangle(ax, ay, aw, ah);
            if (r.contains(mx, my)) return n;
        }
        return null;
    }

    private static String applyAccidentalToPitch(String pitch, Accidental a) {
        if (pitch == null || pitch.isEmpty()) return pitch;

        // 拆：字母 [A-G] + 可选 #/b + 其余（通常是八度如 "4"）
        // 简化假设：pitch 形如  A4 / C#5 / Db3
        char L = pitch.charAt(0);
        if ("ABCDEFG".indexOf(L) < 0) return pitch; // 非标准，直接返回

        int i = 1;
        // 跳过现有 accidental
        if (i < pitch.length() && (pitch.charAt(i) == '#' || pitch.charAt(i) == 'b')) {
            i++;
        }
        String rest = pitch.substring(i); // 比如 "4"

        if (a == Accidental.NONE) {
            return "" + L + rest; // 去掉 #/b
        } else if (a == Accidental.SHARP) {
            return "" + L + "#" + rest;
        } else { // FLAT
            return "" + L + "b" + rest;
        }
    }


    private Point getPositionPointForNote(NoteDuration duration) {
        return MusicViewPitchMapper.getPositionPointForNote(duration, SCALE_FACTOR);
    }

    private String calculatePitch(int absoluteY) {
        return MusicViewPitchMapper.calculatePitch(
                absoluteY,
                numStaves,
                TOP_PADDING,
                STAFF_HEIGHT,
                STAFF_SPACING,
                HALF_LINE_SPACING,
                PITCH_NAMES
        );
    }

    public MusicEditorModel getModel() {
        return this.model;
    }

    public void setOnPitchCalculated(Consumer<String> callback) {
        this.onPitchCalculatedCallback = callback;
    }


    public void setStatusSink(Consumer<String> sink) {
        this.statusSink = sink;
    }
    private void updateStatus(String msg) {
        if (statusSink != null) statusSink.accept("Status: " + msg);
    }
    private void updatePitchStatus(Note note) {
        if (note == null || statusSink == null) return;
        // 组合“G4♯ / G4♭ / G4”
        statusSink.accept("Status: Pitch: " + note.getDisplayPitchWithAccidentalSymbol());
    }


    // 只返回与 dragging 同一 staff 的 Note（Rest 自动被排除）
    private java.util.List<Symbol> getSymbolsOnSameStaff(Note dragging) {
        java.util.List<Symbol> out = new java.util.ArrayList<>();
        Image imgA = getImageForSymbol(dragging);
        int headH = (int) Math.round(2 * HALF_LINE_SPACING);
        int headCenterY = (imgA != null) ? (dragging.getY() + imgA.getHeight(null) - headH / 2)
                : (dragging.getY());
        int staffTop = staffTopForNotehead(headCenterY);
        int staffBottom = staffTop + STAFF_HEIGHT;

        for (Symbol s : symbols) {
            if (!(s instanceof Note)) continue;
            Image im = getImageForSymbol(s);
            int hH = (int) Math.round(2 * HALF_LINE_SPACING);
            int cY = (im != null) ? (s.getY() + im.getHeight(null) - hH / 2) : s.getY(); // 用“音头中心”
            if (cY >= staffTop - STAFF_SPACING / 2 && cY <= staffBottom + STAFF_SPACING / 2) {
                out.add(s);
            }
        }
        return out;
    }



    /**
     * 为超出五线谱上下边界的 Note 画加线（最多两条）。
     * 约定：当前 Note 的 (x,y) 是图片的“左上角”，我们据此取图宽高，计算中心与横向线段宽度。
     */
    private void drawLedgerLines(Graphics2D g2, Note n) {
        Image img = getImageForSymbol(n);
        if (img == null) return;

        // 以“线距”为单位计算：lineSpacing=相邻两条五线的垂直距离
        final int lineSpacing = (int) Math.round(2 * HALF_LINE_SPACING);  // = STAFF_HEIGHT/4
        // 先算音头中心（已修正为 head 在底部）
        final int HEAD_BOTTOM_PADDING = 0;
        final int headH = (int) Math.round(2 * HALF_LINE_SPACING); // ~= lineSpacing
        final int posY  = n.getY() + img.getHeight(null) - HEAD_BOTTOM_PADDING - headH / 2;

        // 用“半间距缓冲”的归属法，而不是最近中心
        final int staffTopY    = staffTopForNotehead(posY);
        final int staffBottomY = staffTopY + STAFF_HEIGHT;


        final int noteLeft = n.getX();
        final int noteTop = n.getY();
        final int noteW = img.getWidth(null);
        final int noteH = img.getHeight(null);

        // 画短横线长度：略宽于音头（视觉好看一点）
        final int halfW = noteW / 2;
        final int pad = Math.max(4, halfW / 2);
        final int x1 = noteLeft - pad;
        final int x2 = noteLeft + noteW + pad;


        int count = 0;            // 需要几条加线（0~2）
        boolean above = false;    // true=在上方; false=在下方

        if (posY < staffTopY) {
            int diff = staffTopY - posY;
            count = Math.min(2, diff / lineSpacing); // floor(diff/lineSpacing)
            above = true;
        } else if (posY > staffBottomY) {
            int diff = posY - staffBottomY;
            count = Math.min(2, diff / lineSpacing); // floor(diff/lineSpacing)
            above = false;
        }

        if (count <= 0) return;

        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(2f)); // 线条稍粗一点，和五线一致或略粗
        // 画 1 或 2 条
        if (above) {
            int y1 = staffTopY - lineSpacing;      // 第一条：贴近 staff 顶部的那条
            g2.drawLine(x1, y1, x2, y1);
            if (count >= 2) {
                int y2 = staffTopY - 2 * lineSpacing;
                g2.drawLine(x1, y2, x2, y2);
            }
        } else {
            int y1 = staffBottomY + lineSpacing;   // 第一条：贴近 staff 底部的那条
            g2.drawLine(x1, y1, x2, y1);
            if (count >= 2) {
                int y2 = staffBottomY + 2 * lineSpacing;
                g2.drawLine(x1, y2, x2, y2);
            }
        }
        g2.setStroke(old);
    }


    /**
     * 用“音头中心Y + 半间距缓冲”来决定属于哪条 staff。
     * 只有当音头中心跨过两条 staff 的中线（空白区的一半）才切换 staff。
     */
    private int staffTopForNotehead(int headCenterY) {
        for (int i = 0; i < numStaves; i++) {
            int top = TOP_PADDING + i * (STAFF_HEIGHT + STAFF_SPACING);
            int bottom = top + STAFF_HEIGHT;
            int bandTop    = top - STAFF_SPACING / 2;    // 向上扩一半空白
            int bandBottom = bottom + STAFF_SPACING / 2; // 向下扩一半空白
            if (headCenterY >= bandTop && headCenterY <= bandBottom) {
                return top;
            }
        }
        // 超出所有带子：钳到最近的端头
        int lastTop = TOP_PADDING + (numStaves - 1) * (STAFF_HEIGHT + STAFF_SPACING);
        if (headCenterY < TOP_PADDING - STAFF_SPACING / 2) return TOP_PADDING;
        return lastTop;
    }


    /** 当前精灵：音头在图片底部，音头中心=noteTop+noteH-lineSpacing/2 */
    private int noteheadCenterY(Note n) {
        Image img = getImageForSymbol(n);
        int noteH = (img != null) ? img.getHeight(null) : 0;
        int lineSpacing = (int) Math.round(2 * HALF_LINE_SPACING);
        return n.getY() + noteH - lineSpacing / 2;
    }


    // +++ 在 MusicView 里新增 +++
    public java.util.List<com.gt.music.model.Symbol> exportSymbolsInReadingOrder(){
        java.util.List<com.gt.music.model.Symbol> out = new java.util.ArrayList<>(this.symbols /* ← 替换为你的容器名 */);
        out.sort(java.util.Comparator.comparingInt(com.gt.music.model.Symbol::getX));
        return out;
    }

    /** 将识别出的名称映射到 NoteDuration */
    private NoteDuration mapDurationFromName(String name) {
        name = name.toLowerCase();
        if (name.contains("circle") || name.contains("rectangle")) return NoteDuration.WHOLE;
        if (name.contains("half")) return NoteDuration.HALF;
        if (name.contains("quarter") || name.contains("right curly brace")) return NoteDuration.QUARTER;
        if (name.contains("eighth")) return NoteDuration.EIGHTH;
        if (name.contains("sixteenth")) return NoteDuration.SIXTEENTH;
        return NoteDuration.QUARTER; // 默认值
    }

    /** 查找点击点落在哪个 Note 的包围盒内（用于附加升降号） */
    private Symbol findNoteAtPoint(int x, int y) {
        for (Symbol s : symbols) {
            if (s instanceof Note) {
                Rectangle box = getSymbolBounds(s);
                if (box != null && box.contains(x, y)) {
                    return s;
                }
            }
        }
        return null;
    }

}
