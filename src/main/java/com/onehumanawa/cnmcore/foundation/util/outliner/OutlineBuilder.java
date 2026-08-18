package com.onehumanawa.cnmcore.foundation.util.outliner;

import net.createmod.catnip.outliner.*;
import net.createmod.catnip.outliner.Outline.OutlineParams;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Fluent builder for Create-style world outlines.
 * <p>
 * Usage:
 * <pre>{@code
 * OutlineBuilder.highlight(pos, pos.offset(1,1,1))
 *     .color(0x00FF00)
 *     .lineWidth(0.04f)
 *     .show(60);
 * }</pre>
 */
@SuppressWarnings("unused")
public final class OutlineBuilder {

    private final Object slot;
    private final Outline outline;
    private final OutlineParams params;
    private boolean shown = false;

    private OutlineBuilder(Object slot, Outline outline) {
        this.slot = slot;
        this.outline = outline;
        this.params = outline.getParams();
        // sensible defaults
        this.params.lineWidth(0.04f);
    }

    // ============================
    // Factory methods
    // ============================

    /**
     * Creates an AABB highlight box from two corner positions.
     * Default color: red (0xFF0000).
     */
    public static OutlineBuilder highlight(Vec3 from, Vec3 to) {
        return highlight(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    public static OutlineBuilder highlight(double x1, double y1, double z1, double x2, double y2, double z2) {
        AABB box = new AABB(x1, y1, z1, x2, y2, z2);
        ChasingAABBOutline outline = new ChasingAABBOutline(box);
        return new OutlineBuilder(slotFor(outline), outline)
                .color(0xFF0000);
    }

    /**
     * Creates a 1-block highlight centered on a BlockPos.
     */
    public static OutlineBuilder highlight(BlockPos pos) {
        return highlight(Vec3.atLowerCornerOf(pos), Vec3.atLowerCornerOf(pos.offset(1, 1, 1)));
    }

    /**
     * Creates a line outline between two points.
     * Default color: green (0x00FF00).
     */
    public static OutlineBuilder line(Vec3 start, Vec3 end) {
        LineOutline outline = new LineOutline();
        outline.set(start, end);
        return new OutlineBuilder(slotFor(outline), outline)
                .color(0x00FF00);
    }

    /**
     * Creates a cluster outline for multiple block positions.
     * Default color: cyan (0x00AAFF).
     */
    public static OutlineBuilder cluster(Iterable<BlockPos> positions) {
        BlockClusterOutline outline = new BlockClusterOutline(positions);
        return new OutlineBuilder(slotFor(outline), outline)
                .color(0x00AAFF);
    }

    /**
     * Creates a 3D item render at a world position.
     */
    public static OutlineBuilder item(Vec3 pos, net.minecraft.world.item.ItemStack stack) {
        ItemOutline outline = new ItemOutline(pos, stack);
        return new OutlineBuilder(slotFor(outline), outline);
    }

    // ============================
    // Configuration (chained)
    // ============================

    public OutlineBuilder color(int color) {
        params.colored(color);
        return this;
    }

    public OutlineBuilder color(Color color) {
        params.colored(color);
        return this;
    }

    public OutlineBuilder lineWidth(float width) {
        params.lineWidth(width);
        return this;
    }

    public OutlineBuilder highlightFace(@Nullable Direction face) {
        params.highlightFace(face);
        return this;
    }

    public OutlineBuilder withTexture(net.createmod.catnip.render.BindableTexture texture) {
        params.withFaceTexture(texture);
        return this;
    }

    public OutlineBuilder disableCull() {
        params.disableCull();
        return this;
    }

    public OutlineBuilder disableLineNormals() {
        params.disableLineNormals();
        return this;
    }

    // ============================
    // Control
    // ============================

    /**
     * Shows the outline immediately. Uses a TTL of -1 (persistent until removed).
     */
    public OutlineBuilder show() {
        Outliner.getInstance().showOutline(slot, outline);
        this.shown = true;
        return this;
    }

    /**
     * Shows the outline with a time-to-live in ticks.
     * After {@code ttl} ticks, the outline will fade out and auto-remove.
     */
    public OutlineBuilder show(int ttl) {
        if (outline instanceof AABBOutline aabb) {
            Outliner.getInstance().showAABB(slot, aabb.getBounds(), ttl);
        } else {
            // fallback: show without TTL
            Outliner.getInstance().showOutline(slot, outline);
        }
        this.shown = true;
        return this;
    }

    /**
     * Moves the highlight box to a new AABB.
     * Only works for AABB-based outlines.
     */
    public OutlineBuilder moveTo(Vec3 from, Vec3 to) {
        return moveTo(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    public OutlineBuilder moveTo(double x1, double y1, double z1, double x2, double y2, double z2) {
        if (!shown) return this;
        AABB newBox = new AABB(x1, y1, z1, x2, y2, z2);

        if (outline instanceof ChasingAABBOutline chasing) {
            chasing.target(newBox);
        } else if (outline instanceof AABBOutline aabb) {
            aabb.setBounds(newBox);
        } else {
            return this; // unsupported outline type
        }
        Outliner.getInstance().keep(slot);
        return this;
    }

    public OutlineBuilder moveTo(BlockPos pos) {
        return moveTo(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        );
    }

    public OutlineBuilder moveToBlockCenter(BlockPos pos) {
        Vec3 c = Vec3.atCenterOf(pos);
        double half = 0.45;
        return moveTo(c.x - half, c.y - half, c.z - half,
                c.x + half, c.y + half, c.z + half);
    }

    /**
     * Refreshes the outline's lifetime, preventing it from expiring.
     */
    public OutlineBuilder keep() {
        if (shown) {
            Outliner.getInstance().keep(slot);
        }
        return this;
    }

    /**
     * Removes the outline from the world.
     */
    public void remove() {
        Outliner.getInstance().remove(slot);
        this.shown = false;
    }

    public boolean isShown() {
        return shown;
    }

    public Outline getOutline() {
        return outline;
    }

    // ============================
    // Internal
    // ============================

    private static String slotFor(Outline outline) {
        return "kubejava_outliner_" + System.identityHashCode(outline);
    }
}