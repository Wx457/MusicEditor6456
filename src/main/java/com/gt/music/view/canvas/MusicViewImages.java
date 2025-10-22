package com.gt.music.view.canvas;

import com.gt.music.model.Note;
import com.gt.music.model.Rest;
import com.gt.music.model.Symbol;
import com.gt.music.types.Accidental;
import com.gt.music.types.NoteDuration;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

final class MusicViewImages {
    private MusicViewImages() {
    }

    /**
     * `com.gt.music.view.canvas.MusicView` 提供图片的接口——我们只问它要 Key 对应的 Image
     */
    interface Bank {
        Image get(Key key);
    }

    /**
     * 我们定义的一套“键”，不新建文件，放在本类里即可
     */
    enum Key {
        NOTE_WHOLE, NOTE_HALF, NOTE_QUARTER, NOTE_EIGHTH, NOTE_SIXTEENTH,
        REST_WHOLE, REST_HALF, REST_QUARTER, REST_EIGHTH, REST_SIXTEENTH,
        CLEF_TREBLE, TIME_COMMON, ACC_SHARP, ACC_FLAT
    }

    // 选择器：根据 com.gt.music.model.Symbol/Duration 选择一个图片 Key，然后让 Bank 提供真正的 Image
    static Image forSymbol(Symbol s, Bank bank) {
        NoteDuration d = s.getDuration();
        if (s instanceof Note) {
            switch (d) {
                case WHOLE:
                    return bank.get(Key.NOTE_WHOLE);
                case HALF:
                    return bank.get(Key.NOTE_HALF);
                case QUARTER:
                    return bank.get(Key.NOTE_QUARTER);
                case EIGHTH:
                    return bank.get(Key.NOTE_EIGHTH);
                case SIXTEENTH:
                    return bank.get(Key.NOTE_SIXTEENTH);
                default:
                    return null;
            }
        } else if (s instanceof Rest) {
            switch (d) {
                case WHOLE:
                    return bank.get(Key.REST_WHOLE);
                case HALF:
                    return bank.get(Key.REST_HALF);
                case QUARTER:
                    return bank.get(Key.REST_QUARTER);
                case EIGHTH:
                    return bank.get(Key.REST_EIGHTH);
                case SIXTEENTH:
                    return bank.get(Key.REST_SIXTEENTH);
                default:
                    return null;
            }
        }
        return null;
    }
    // 供 MusicView 使用：根据 Accidental 类型从 bank 获取图片
    static Image forAccidental(Accidental a, Bank bank) {
        if (a == null || a == Accidental.NONE) {
            return null;
        }

        switch (a) {
            case SHARP:
                return bank.get(Key.ACC_SHARP);
            case FLAT:
                return bank.get(Key.ACC_FLAT);
            default:
                return null;
        }
    }

    // 统一持有所有需要的图片，并按 Key 取图
    static final class SpriteBank implements Bank {
        private final Image noteWhole, noteHalf, noteQuarter, noteEighth, noteSixteenth;
        private final Image restWhole, restHalf, restQuarter, restEighth, restSixteenth;
        private final Image clefTreble, timeCommon;
        private final Image accSharp, accFlat;

        private SpriteBank(
                Image noteWhole, Image noteHalf, Image noteQuarter, Image noteEighth, Image noteSixteenth,
                Image restWhole, Image restHalf, Image restQuarter, Image restEighth, Image restSixteenth,
                Image clefTreble, Image timeCommon, Image accSharp, Image accFlat
        ) {
            this.noteWhole = noteWhole;
            this.noteHalf = noteHalf;
            this.noteQuarter = noteQuarter;
            this.noteEighth = noteEighth;
            this.noteSixteenth = noteSixteenth;
            this.restWhole = restWhole;
            this.restHalf = restHalf;
            this.restQuarter = restQuarter;
            this.restEighth = restEighth;
            this.restSixteenth = restSixteenth;
            this.clefTreble = clefTreble;
            this.timeCommon = timeCommon;
            this.accSharp = accSharp;
            this.accFlat = accFlat;
        }

        /**
         * 用你“已经加载好的 Image”来构造 Bank —— 不改变你当前任何加载/缩放代码
         */
        static SpriteBank fromExisting(
                Image noteWhole, Image noteHalf, Image noteQuarter, Image noteEighth, Image noteSixteenth,
                Image restWhole, Image restHalf, Image restQuarter, Image restEighth, Image restSixteenth,
                Image clefTreble, Image timeCommon, Image accSharp, Image accFlat
        ) {
            return new SpriteBank(
                    noteWhole, noteHalf, noteQuarter, noteEighth, noteSixteenth,
                    restWhole, restHalf, restQuarter, restEighth, restSixteenth,
                    clefTreble, timeCommon, accSharp, accFlat
            );
        }

        @Override
        public Image get(Key key) {
            switch (key) {
                case NOTE_WHOLE:
                    return noteWhole;
                case NOTE_HALF:
                    return noteHalf;
                case NOTE_QUARTER:
                    return noteQuarter;
                case NOTE_EIGHTH:
                    return noteEighth;
                case NOTE_SIXTEENTH:
                    return noteSixteenth;
                case REST_WHOLE:
                    return restWhole;
                case REST_HALF:
                    return restHalf;
                case REST_QUARTER:
                    return restQuarter;
                case REST_EIGHTH:
                    return restEighth;
                case REST_SIXTEENTH:
                    return restSixteenth;
                case CLEF_TREBLE:
                    return clefTreble;
                case TIME_COMMON:
                    return timeCommon;
                case ACC_SHARP: return accSharp;
                case ACC_FLAT: return accFlat;
                default:
                    return null;
            }
        }
    }


    // 按你的默认路径加载 10 张图片并缩放，然后组装成 SpriteBank 返回。
    static SpriteBank loadDefaultFromResources(double scaleFactor) {
        try {
            Image noteWhole = loadScaled("/images/IMGmaterials/wholeNote.png", scaleFactor);
            Image noteHalf = loadScaled("/images/IMGmaterials/halfNote.png", scaleFactor);
            Image noteQuarter = loadScaled("/images/IMGmaterials/quarterNote.png", scaleFactor);
            Image noteEighth = loadScaled("/images/IMGmaterials/eighthNote.png", scaleFactor);
            Image noteSixteenth = loadScaled("/images/IMGmaterials/sixteenthNote.png", scaleFactor);

            Image restWhole = loadScaled("/images/IMGmaterials/wholeRest.png", scaleFactor);
            Image restHalf = loadScaled("/images/IMGmaterials/halfRest.png", scaleFactor);
            Image restQuarter = loadScaled("/images/IMGmaterials/quarterRest.png", scaleFactor);
            Image restEighth = loadScaled("/images/IMGmaterials/eighthRest.png", scaleFactor);
            Image restSixteenth = loadScaled("/images/IMGmaterials/sixteenthRest.png", scaleFactor);

            Image clefTreble = loadScaled("/images/IMGmaterials/trebleClef.png", 1.2);
            Image timeCommon = loadScaled("/images/IMGmaterials/commonTime.png", 0.8);

            Image accSharp = loadScaled("/images/IMGmaterials/sharp.png", 1.2);
            Image accFlat  = loadScaled("/images/IMGmaterials/flat.png",  1.2);

            return SpriteBank.fromExisting(
                    noteWhole, noteHalf, noteQuarter, noteEighth, noteSixteenth,
                    restWhole, restHalf, restQuarter, restEighth, restSixteenth,
                    clefTreble, timeCommon, accSharp, accFlat
            );
        } catch (Exception e) {
            System.err.println("Unable to load note/rest images: " + e);
            return SpriteBank.fromExisting(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null
            );
        }
    }

    // 私有小工具：读图并缩放（抛异常给上面的 try 统一处理）
    private static Image loadScaled(String path, double factor) throws java.io.IOException {
        java.awt.Image raw = javax.imageio.ImageIO.read(MusicViewImages.class.getResource(path));
        return scaled(raw, factor);
    }

    static Image Load(String path) {
        try {
            return ImageIO.read(MusicViewImages.class.getResource(path));
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("Missing image: " + path + " (" + ex + ")");
            return null;
        }
    }

    /**
     * Scale img with factor
     * @param srcImg original img
     * @param factor scale factor (e.g., 1.2 , 0.8)
     * @return scaled img
     */
    static Image scaled(Image srcImg, double factor) {
        if (srcImg == null) return null;
        int newWidth = (int) (srcImg.getWidth(null) * factor);
        int newHeight = (int) (srcImg.getHeight(null) * factor);
        if (newWidth < 1) newWidth = 1;
        if (newHeight < 1) newHeight = 1;
        return srcImg.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }
}
