package com.gt.music.gestures;

public class ScratchOutDetector {

    public enum Result {
        SCRATCH_OUT, NOT_SCRATCH_OUT
    }

    // 本回合只做接口，下一回合实现老师要求的“方向反转+水平/垂直路程比>=4.0”的算法
    public Result classify(GestureStroke stroke) {
        if (stroke == null || stroke.isEmpty()) return Result.NOT_SCRATCH_OUT;
        // TODO: 下一步实现：方向反转计数 + 累积水平/垂直位移比
        return Result.NOT_SCRATCH_OUT;
    }
}
