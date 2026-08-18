package com.onehumanawa.cnmcore.foundation.util.outliner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central manager for named outlines.
 * <p>
 * Useful for tracking outlines that need to be updated or removed later.
 * <p>
 * Usage:
 * <pre>{@code
 * OutlinerManager.get().highlight("target", pos)
 *     .color(0xFFAA00)
 *     .show();
 *
 * // later...
 * OutlinerManager.get().move("target", newPos);
 * OutlinerManager.get().clear("target");
 * }</pre>
 */
public final class OutlinerManager {

    private static final OutlinerManager INSTANCE = new OutlinerManager();
    private final Map<String, OutlineBuilder> active = new HashMap<>();

    private OutlinerManager() {}

    public static OutlinerManager get() {
        return INSTANCE;
    }

    // ============================
    // Create or retrieve
    // ============================

    public OutlineBuilder highlight(String id, BlockPos pos) {
        return getOrCreate(id)
                .moveTo(pos)
                .show();
    }

    public OutlineBuilder highlight(String id, BlockPos from, BlockPos to) {
        return getOrCreate(id)
                .moveTo(Vec3.atLowerCornerOf(from), Vec3.atLowerCornerOf(to))
                .show();
    }

    public OutlineBuilder highlight(String id, Vec3 from, Vec3 to) {
        return getOrCreate(id)
                .moveTo(from, to)
                .show();
    }

    public OutlineBuilder line(String id, Vec3 start, Vec3 end) {
        OutlineBuilder builder = active.computeIfAbsent(id, k -> OutlineBuilder.line(start, end));
        // If it's a line, we can't move it easily, so recreate.
        if (!builder.isShown()) {
            builder.show();
        }
        return builder;
    }

    // ============================
    // Control
    // ============================

    public void move(String id, BlockPos pos) {
        OutlineBuilder b = active.get(id);
        if (b != null) b.moveTo(pos);
    }

    public void move(String id, Vec3 from, Vec3 to) {
        OutlineBuilder b = active.get(id);
        if (b != null) b.moveTo(from, to);
    }

    public void keep(String id) {
        OutlineBuilder b = active.get(id);
        if (b != null) b.keep();
    }

    public void clear(String id) {
        OutlineBuilder removed = active.remove(id);
        if (removed != null) removed.remove();
    }

    public void clearAll() {
        for (String id : active.keySet()) {
            clear(id);
        }
    }

    public Optional<OutlineBuilder> get(String id) {
        return Optional.ofNullable(active.get(id));
    }

    public boolean has(String id) {
        return active.containsKey(id);
    }

    // ============================
    // Internal
    // ============================

    private OutlineBuilder getOrCreate(String id) {
        return active.computeIfAbsent(id, k -> {
            // default to 1-block highlight at origin; caller will move it
            return OutlineBuilder.highlight(BlockPos.ZERO)
                    .color(0xFF0000)
                    .show();
        });
    }
}