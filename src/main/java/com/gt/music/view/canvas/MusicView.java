package com.gt.music.view.canvas;

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
import java.util.ArrayList;
import java.awt.Point;
import java.util.function.Consumer;

public class MusicView extends JComponent {

    private static final int STAFF_WIDTH = 1000;
    private static final int STAFF_HEIGHT = 60;
    private static final int TOP_PADDING = 60;
    private static final int LEFT_PADDING = 60;
    private static final int STAFF_SPACING = 100;
    private static final double SCALE_FACTOR = 1.5;

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
        Graphics2D g2 = (Graphics2D) g; // 统一用 g2，不再 create()/dispose()

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

        // --- Drawing items (notes/rests) ---
        for (Symbol symbol : symbols) {

            if (symbol instanceof Note) {
                Note n = (Note) symbol;

                // 先画上/下加线
                drawLedgerLines(g2, n); // 你刚加的函数

                // 再画音符图像
                Image noteImg = getImageForSymbol(n);
                if (noteImg != null) {
                    g2.drawImage(noteImg, n.getX(), n.getY(), null);
                }

                // 最后画升降号（若有）
                Accidental a = n.getAccidental();
                if (a != Accidental.NONE) {
                    Image accImg = MusicViewImages.forAccidental(a, sprites);
                    if (accImg != null) {
                        int aw = accImg.getWidth(null);
                        int ax = n.getX() - aw - 4;     // 贴在音头左侧
                        int ay = n.getY() + 35;         // 简单垂直对齐（后续可用 PitchMapper 微调）
                        g2.drawImage(accImg, ax, ay, null);
                    }
                }

            } else if (symbol instanceof Rest) {
                Image restImg = getImageForSymbol(symbol);
                if (restImg != null) {
                    g2.drawImage(restImg, symbol.getX(), symbol.getY(), null);
                }
            }

            // 选中框（蓝色）
            Image img = getImageForSymbol(symbol);
            if (symbol == selectedSymbol && img != null) {
                g2.setColor(Color.BLUE);
                g2.drawRect(symbol.getX(), symbol.getY(), img.getWidth(null), img.getHeight(null));
                g2.setColor(Color.BLACK);
            }
        }

        // --- NOTES全局包围盒高亮（拖拽升/降号时显示） ---
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

        // --- 拖拽中的 Accidental 预览（置顶绘制） ---
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
                    int px = accidentalX - aw / 2; // 鼠标中心对齐
                    int py = accidentalY - ah / 2;
                    g2.drawImage(accImg, px, py, null);
                }
            }
        }

        // --- “accidental 的选中高亮” ---
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
            MusicView.this.requestFocusInWindow();
            EditMode currentMode = model.getCurrentMode();

            if (currentMode == EditMode.DRAW) {
                ToolType currentTool = model.getCurrentTool();
                NoteDuration currentDuration = model.getCurrentDuration();
                Symbol newSymbol = null;
                if (currentTool == ToolType.NOTE) {
                    newSymbol = new Note(e.getX(), e.getY(), currentDuration);
                } else if (currentTool == ToolType.REST) {
                    newSymbol = new Rest(e.getX(), e.getY(), currentDuration);
                } else if (currentTool == ToolType.SHARP || currentTool == ToolType.FLAT) {
                    startAccidentalDrag(e.getX(), e.getY());
                    return;
                }

                if (newSymbol != null) {
                    Image img = getImageForSymbol(newSymbol);
                    if (img != null) {
                        newSymbol.setX(e.getX() - img.getWidth(null) / 2);
                        newSymbol.setY(e.getY() - img.getHeight(null) / 2);
                    }
                    symbols.add(newSymbol);
                    activeSymbol = newSymbol;
                }

            } else if (currentMode == EditMode.SELECT) {
                // Traverse from back to front
                boolean hit = false;
                for (int i = symbols.size() - 1; i >= 0; i--) {
                    Symbol symbol = symbols.get(i);
                    Image img = getImageForSymbol(symbol);

                    if (img != null) {
                        // Create a rectangle representing the symbol's boundaries
                        Rectangle bounds = new Rectangle(symbol.getX(), symbol.getY(), img.getWidth(null), img.getHeight(null));
                        // Check if the mouse click is within this rectangle
                        if (bounds.contains(e.getPoint())) {
                            selectedSymbol = symbol;
                            activeSymbol = symbol;
                            hit = true;

                            // 如果命中的是 Note，则立即更新状态栏并 return，避免被后续逻辑覆盖
                            if (symbol instanceof Note) {
                                snapper.onDragStart((Note) activeSymbol);
                                Note n = (Note) symbol;
                                updatePitchStatus(n);
                                repaint();
                                return; // 防止后面的“点空白/accidental命中”等覆盖状态栏
                            }
                            break;
                        }
                    }
                }
                // If no symbol is selected, all selections are cleared.
                if (!hit) {
                    selectedSymbol = null;
                }

                //For accidental
                Note hitAcc = findAccidentalAt(e.getX(), e.getY());
                if (hitAcc != null) {
                    selectedAccidentalNote = hitAcc;
                    selectedSymbol = null;
                    updatePitchStatus(hitAcc);
                    repaint();
                    return;
                } else {
                    // 点空白：取消 accidental 的选择
                    selectedAccidentalNote = null;
                    if (!hit) {
                        updateStatus("Ready");
                    }
                }
            }
            MusicView.this.repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (draggingAccidental) {
                updateAccidentalDrag(e.getX(), e.getY());
                return;
            }

            if (activeSymbol != null) {
                Image img = getImageForSymbol(activeSymbol);

                if (activeSymbol instanceof Note) {
                    Note dragging = (Note) activeSymbol;

                    int halfW = (img != null) ? img.getWidth(null) / 2 : 8;
                    int halfH = (img != null) ? img.getHeight(null) / 2 : 8;

                    // 同一 staff 的符号（如果你暂时没有这个函数，就先传整页的 symbols，下一步再收紧）
                    java.util.List<Symbol> sameStaff = getSymbolsOnSameStaff(dragging);

                    // 计算“当前拖拽时所属 staff”的 top（用中心Y来判断）
                    int aCenterYNow = (img != null) ? (dragging.getY() + img.getHeight(null) / 2) : dragging.getY();
                    int staffTopNow = MusicViewSnapper.nearestStaffTopY(
                            aCenterYNow, TOP_PADDING, STAFF_HEIGHT, STAFF_SPACING, numStaves);

                    // 记录上一次 staffTop（给 MusicView 加个字段保存即可）
                    if (_lastDragStaffTop == null) {
                        _lastDragStaffTop = staffTopNow;
                    } else if (!_lastDragStaffTop.equals(staffTopNow)) {
                        // 跨 staff：重置吸附状态，避免“跨行粘住”
                        snapper.onDragStart(dragging);
                        _lastDragStaffTop = staffTopNow;
                    }

                    // 推进吸附状态机（把“如何取宽度”交给图片系统）
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
                    int newX = (snappedX != null) ? snappedX : (e.getX() - halfW); // 锁X或随鼠标
                    int newY = (img != null) ? (e.getY() - halfH) : e.getY();      // Y 一律自由

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
                    // 非 Note（Rest 等）保持原始自由拖拽
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
            if (draggingAccidental) {
                boolean attached = tryAttachAccidental(e.getX(), e.getY());
                if (!attached) updateStatus("Canceled: no note hit.");
                endAccidentalDrag();
                repaint();
                return;
            }

            if (activeSymbol != null) {
                // Drag main.java.com.gt.music.model.Note
                if (activeSymbol instanceof Note) {
                    Note activeNote = (Note) activeSymbol;

                    // 1) 用音头中心 Y 计算音高（覆盖 G3..D6）
                    int headY = noteheadCenterY(activeNote);
                    String pitch = MusicViewPitchMapper.calculatePitch(
                            headY, numStaves, TOP_PADDING, STAFF_HEIGHT, STAFF_SPACING,
                            HALF_LINE_SPACING, PITCH_NAMES);
                    activeNote.setPitch(pitch);
                    if (onPitchCalculatedCallback != null) onPitchCalculatedCallback.accept(pitch);

                    // 2) 纵向吸附可继续沿用你原来的 snapY（如果你希望贴线/贴间）
                    int staffTopY = findStaffTopY(headY); // 你已有；若返回 -1 就不 snap
                    if (staffTopY != -1) {
                        int snappedHeadY = MusicViewSnapper.snapY(
                                headY, staffTopY, HALF_LINE_SPACING, SNAP_Y_TOLERANCE_PX);
                        // 把“音头中心”还原成符号左上角 Y：
                        Image img = getImageForSymbol(activeNote);
                        int noteH = (img != null) ? img.getHeight(null) : 0;
                        int lineSpacing = (int) Math.round(2 * HALF_LINE_SPACING);
                        activeNote.setY(snappedHeadY - (noteH - lineSpacing / 2));
                        MusicView.this.repaint();
                    }

                    // 最后再更新一次状态栏（包含可能的吸附修正）
                    updatePitchStatus(activeNote);
                    repaint();

                // Drag main.java.com.gt.music.model.Rest
                } else if (activeSymbol instanceof Rest) {
                    int centerX = activeSymbol.getX() + getImageForSymbol(activeSymbol).getWidth(null) / 2;
                    int centerY = activeSymbol.getY() + getImageForSymbol(activeSymbol).getHeight(null) / 2;

                    int staffTopY = findStaffTopY(centerY);
                    if (staffTopY != -1) {
                        int midY = staffTopY + (STAFF_HEIGHT / 2); // 中线
                        activeSymbol.setY(midY - getImageForSymbol(activeSymbol).getHeight(null) / 2);
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

}
