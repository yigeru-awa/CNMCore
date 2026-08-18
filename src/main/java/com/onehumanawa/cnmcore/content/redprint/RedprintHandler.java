package com.onehumanawa.cnmcore.content.redprint;

import com.onehumanawa.cnmcore.AllPackets;
import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.utility.RaycastHelper;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side selection handler for the Redprint tool.
 * Manages the two selection corners and the outline rendering.
 */
public class RedprintHandler {

    private static final RedprintHandler INSTANCE = new RedprintHandler();

    private BlockPos firstPos;
    private BlockPos secondPos;
    private BlockPos selectedPos;
    private Direction selectedFace;

    private final Object outlineSlot = new Object();
    private int range = 10;

    private RedprintHandler() {}

    public static RedprintHandler getInstance() {
        return INSTANCE;
    }

    public boolean isSelecting() {
        return firstPos != null || secondPos != null;
    }

    /**
     * Called each tick to update the selection outline.
     */
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (level == null || player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof RedprintItem)) {
            Outliner.getInstance().remove(outlineSlot);
            return;
        }

        updateSelectedPos(player);

        Direction highlightFace = null;
        if (firstPos != null && secondPos != null) {
            AABB bb = getCurrentSelectionBox();
            if (bb != null) {
                Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
                boolean inside = bb.contains(cameraPos);
                RaycastHelper.PredicateTraceResult result =
                        RaycastHelper.rayTraceUntil(player, 70, pos -> inside ^ bb.contains(VecHelper.getCenterOf(pos)));
                highlightFace = result.missed() ? null
                        : inside ? result.getFacing().getOpposite() : result.getFacing();
            }
        }

        AABB selectionBox = getCurrentSelectionBox();
        if (selectionBox != null) {
            Outliner.getInstance().showAABB(outlineSlot, selectionBox)
                    .colored(0xFF6464)
                    .lineWidth(1 / 16f)
                    .highlightFace(highlightFace);
        } else {
            Outliner.getInstance().remove(outlineSlot);
        }
    }

    /**
     * Updates selectedPos and selectedFace based on current player look.
     */
    private void updateSelectedPos(Player player) {
        Minecraft mc = Minecraft.getInstance();

        if (AllKeys.ACTIVATE_TOOL.isPressed()) {
            float pt = AnimationTickHolder.getPartialTicks();
            Vec3 targetVec = player.getEyePosition(pt).add(player.getLookAngle().scale(range));
            selectedPos = BlockPos.containing(targetVec);
            selectedFace = null;
            return;
        }

        BlockHitResult trace = RaycastHelper.rayTraceRange(player.level(), player, 75);
        if (trace != null && trace.getType() == HitResult.Type.BLOCK) {
            BlockPos hit = trace.getBlockPos();
            Direction face = trace.getDirection();
            selectedFace = face;
            // Get the block adjacent to the clicked face
            selectedPos = hit.relative(face);
        } else {
            selectedPos = null;
            selectedFace = null;
        }
    }

    /**
     * Handles right-click interactions with the Redprint tool.
     * Performs a fresh raycast to ensure selectedPos is current.
     */
    public void onItemUse(Player player) {
        // Fresh raycast on use, not relying on cached value
        updateSelectedPos(player);

        // Shift + right-click: cancel selection
        if (player.isShiftKeyDown()) {
            cancelSelection(player);
            return;
        }

        // If no target, abort
        if (selectedPos == null) {
            player.displayClientMessage(Component.translatable("cnmcore.redprint.no_target"), true);
            return;
        }

        // If both corners are selected, execute the removal
        if (firstPos != null && secondPos != null) {
            executeRemoval(player);
            return;
        }

        // Set first or second corner
        if (firstPos == null) {
            firstPos = selectedPos;
            player.displayClientMessage(
                    Component.translatable("cnmcore.redprint.first_pos", firstPos.getX(), firstPos.getY(), firstPos.getZ()),
                    true
            );
        } else {
            secondPos = selectedPos;
            player.displayClientMessage(
                    Component.translatable("cnmcore.redprint.second_pos", secondPos.getX(), secondPos.getY(), secondPos.getZ()),
                    true
            );
        }
    }

    /**
     * Executes the block removal on the server.
     */
    private void executeRemoval(Player player) {
        BlockPos from = new BlockPos(
                Math.min(firstPos.getX(), secondPos.getX()),
                Math.min(firstPos.getY(), secondPos.getY()),
                Math.min(firstPos.getZ(), secondPos.getZ())
        );
        BlockPos to = new BlockPos(
                Math.max(firstPos.getX(), secondPos.getX()),
                Math.max(firstPos.getY(), secondPos.getY()),
                Math.max(firstPos.getZ(), secondPos.getZ())
        );

        AllPackets.sendToServer(new RedprintPacket(from, to, player.getMainHandItem()));
        player.displayClientMessage(Component.translatable("cnmcore.redprint.executing"), true);

        // Clear selection after sending the packet
        firstPos = null;
        secondPos = null;
        selectedPos = null;
        Outliner.getInstance().remove(outlineSlot);
    }

    /**
     * Cancels the current selection.
     */
    private void cancelSelection(Player player) {
        firstPos = null;
        secondPos = null;
        selectedPos = null;
        Outliner.getInstance().remove(outlineSlot);
        player.displayClientMessage(Component.translatable("cnmcore.redprint.cancelled"), true);
    }

    /**
     * Returns the current selection box.
     * Uses AABB.minmax() to correctly merge two block AABBs.
     */
    private AABB getCurrentSelectionBox() {
        if (secondPos == null) {
            if (firstPos == null) {
                return selectedPos == null ? null : new AABB(selectedPos);
            }
            if (selectedPos == null) {
                return new AABB(firstPos);
            }
            // Merge firstPos and selectedPos AABBs
            return new AABB(firstPos).minmax(new AABB(selectedPos));
        }
        if (firstPos == null) return null;

        // Merge firstPos and secondPos AABBs
        return new AABB(firstPos).minmax(new AABB(secondPos));
    }

    /**
     * Handles mouse scroll to adjust range.
     */
    public boolean mouseScrolled(double delta) {
        if (!AllKeys.ctrlDown()) return false;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;

        if (level == null || player == null) return false;
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof RedprintItem)) return false;

        range = (int) Math.max(1, Math.min(100, range + delta));
        player.displayClientMessage(Component.translatable("cnmcore.redprint.range", range), true);
        return true;
    }

    /**
     * Handles mouse input for the tool.
     */
    public boolean onMouseInput(int button, boolean pressed) {
        if (!pressed || button != 0) return false;
        return true;
    }
}