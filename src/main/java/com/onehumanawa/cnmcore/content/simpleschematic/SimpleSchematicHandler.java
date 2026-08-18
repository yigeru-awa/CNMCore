package com.onehumanawa.cnmcore.content.simpleschematic;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cnmcore.AllDataComponents;
import com.onehumanawa.cnmcore.AllItems;
import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.content.simpleschematic.tools.SimpleToolType;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicProcessor;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.content.schematics.client.SchematicHotbarSlotOverlay;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.simibubi.create.content.schematics.client.ToolSelectionScreen;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SimpleSchematicHandler extends SchematicHandler {

    public static final SimpleSchematicHandler SIMPLE_SCHEMATIC_HANDLER = new SimpleSchematicHandler();

    private SchematicTransformation transformation;
    private AABB bounds;
    private boolean deployed;
    private boolean active;
    private SimpleToolType currentTool;

    private ItemStack activeSchematicItem;
    private AABBOutline outline;

    private final SchematicRenderer[] renderers = new SchematicRenderer[3];
    private final SchematicHotbarSlotOverlay overlay;
    private ToolSelectionScreen selectionScreen;

    public SimpleSchematicHandler() {
        overlay = new SchematicHotbarSlotOverlay();
        currentTool = SimpleToolType.DEPLOY;
        selectionScreen = new SimpleToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
        transformation = new SchematicTransformation();
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        // Defensive: gameMode may be null on certain client states
        if (player == null || mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            if (active) {
                activeSchematicItem = null;
                setInactive();
            }
            return;
        }

        if (activeSchematicItem != null && transformation != null)
            transformation.tick();

        ItemStack stackBefore = activeSchematicItem;
        ItemStack stack = findBlueprintInHand(player);
        if (stack == null) {
            if (activeSchematicItem != null && itemLost(player)) {
                activeSchematicItem = null;
            }
            setInactive();
            return;
        }

        // Player holds a brand-new schematic item: reset all deploy state
        if (stackBefore == null || !ItemStack.matches(stack, stackBefore)) {
            setInactive();
            active = true;
            deployed = false;

            Level level = mc.level;
            if (level == null)
                return;
            StructureTemplate template = SimpleSchematicItem.loadSchematic(
                    level.holderLookup(Registries.BLOCK), stack);
            Vec3i size = template.getSize();
            bounds = new AABB(0, 0, 0, size.getX(), size.getY(), size.getZ());

            outline = new AABBOutline(bounds);
            outline.getParams().colored(0x32CD32).lineWidth(1 / 16f);

            StructurePlaceSettings settings = new StructurePlaceSettings();
            settings.addProcessor(SchematicProcessor.INSTANCE);
            transformation = new SchematicTransformation();
            transformation.init(BlockPos.ZERO, settings, bounds);

            selectionScreen = new SimpleToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
        }
        // Player switched back to the previous schematic item
        else if (!active) {
            setInactive();
            active = true;

            setupRenderer();
            if (deployed) {
                ToolType toolBefore = currentTool.getToolType();
                selectionScreen = new SimpleToolSelectionScreen(SimpleToolType.getTools(player.isCreative()), this::equip);
                if (toolBefore != null) {
                    selectionScreen.setSelectedElement(toolBefore);
                    equip(toolBefore);
                }
            } else {
                selectionScreen = new SimpleToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
            }
        }

        if (!active)
            return;

        selectionScreen.update();
        currentTool.getTool().updateSelection();
    }

    private void setupRenderer() {
        Level clientWorld = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (clientWorld == null || player == null)
            return;
        if (activeSchematicItem == null)
            return;

        StructureTemplate schematic =
                SimpleSchematicItem.loadSchematic(clientWorld.holderLookup(Registries.BLOCK), activeSchematicItem);
        Vec3i size = schematic.getSize();
        if (size.equals(Vec3i.ZERO))
            return;

        SchematicLevel w = new SchematicLevel(clientWorld);
        SchematicLevel wMirroredFB = new SchematicLevel(clientWorld);
        SchematicLevel wMirroredLR = new SchematicLevel(clientWorld);
        StructurePlaceSettings placementSettings = new StructurePlaceSettings();

        try {
            schematic.placeInWorld(w, BlockPos.ZERO, BlockPos.ZERO, placementSettings, w.getRandom(), Block.UPDATE_CLIENTS);
            for (BlockEntity blockEntity : w.getBlockEntities())
                blockEntity.setLevel(w);
            fixControllerBlockEntities(w);
        } catch (Exception e) {
            player.displayClientMessage(CreateLang.translate("schematic.error").component(), false);
            CNMCore.LOGGER.error("Failed to load Schematic for Previewing", e);
            return;
        }

        placementSettings.setMirror(Mirror.FRONT_BACK);
        BlockPos posFB = BlockPos.ZERO.east(size.getX() - 1);
        schematic.placeInWorld(wMirroredFB, posFB, posFB, placementSettings, wMirroredFB.getRandom(), Block.UPDATE_CLIENTS);
        StructureTransform transformFB = new StructureTransform(placementSettings.getRotationPivot(), Direction.Axis.Y,
                Rotation.NONE, placementSettings.getMirror());
        for (BlockEntity be : wMirroredFB.getRenderedBlockEntities())
            transformFB.apply(be);
        fixControllerBlockEntities(wMirroredFB);

        placementSettings.setMirror(Mirror.LEFT_RIGHT);
        BlockPos posLR = BlockPos.ZERO.south(size.getZ() - 1);
        schematic.placeInWorld(wMirroredLR, posLR, posLR, placementSettings, wMirroredLR.getRandom(), Block.UPDATE_CLIENTS);
        StructureTransform transformLR = new StructureTransform(placementSettings.getRotationPivot(), Direction.Axis.Y,
                Rotation.NONE, placementSettings.getMirror());
        for (BlockEntity be : wMirroredLR.getRenderedBlockEntities())
            transformLR.apply(be);
        fixControllerBlockEntities(wMirroredLR);

        renderers[0] = new SchematicRenderer(w);
        renderers[1] = new SchematicRenderer(wMirroredFB);
        renderers[2] = new SchematicRenderer(wMirroredLR);
    }

    // Mirrors Create's own SchematicHandler#fixControllerBlockEntities
    private void fixControllerBlockEntities(SchematicLevel level) {
        for (BlockEntity blockEntity : level.getBlockEntities()) {
            if (!(blockEntity instanceof IMultiBlockEntityContainer multiBlockEntity))
                continue;
            BlockPos lastKnown = multiBlockEntity.getLastKnownPos();
            BlockPos current = blockEntity.getBlockPos();
            if (lastKnown == null)
                continue;
            if (multiBlockEntity.isController())
                continue;
            if (!lastKnown.equals(current)) {
                BlockPos newControllerPos = multiBlockEntity.getController()
                        .offset(current.subtract(lastKnown));
                if (multiBlockEntity instanceof SmartBlockEntity sbe)
                    sbe.markVirtual();
                multiBlockEntity.setController(newControllerPos);
            }
        }
    }

    @Override
    public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera) {
        boolean present = activeSchematicItem != null;
        if (!active && !present)
            return;
        if (transformation == null)
            return;

        if (active && currentTool != null) {
            ms.pushPose();
            currentTool.getTool().renderTool(ms, buffer, camera);
            ms.popPose();
        }

        ms.pushPose();
        transformation.applyTransformations(ms, camera);

        if (deployed) {
            float pt = AnimationTickHolder.getPartialTicks();
            boolean lr = transformation.getScaleLR().getValue(pt) < 0;
            boolean fb = transformation.getScaleFB().getValue(pt) < 0;
            if (lr && !fb && renderers[2] != null) {
                renderers[2].render(ms, buffer);
            } else if (fb && !lr && renderers[1] != null) {
                renderers[1].render(ms, buffer);
            } else if (renderers[0] != null) {
                renderers[0].render(ms, buffer);
            }
        }

        if (active && currentTool != null)
            currentTool.getTool().renderOnSchematic(ms, buffer);

        ms.popPose();
    }

    @Override
    public void updateRenderers() {
        for (SchematicRenderer renderer : renderers) {
            if (renderer != null)
                renderer.update();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc.options.hideGui || !active || player == null)
            return;

        // No hotbar slot is tracked, so re-verify the held item to avoid brief
        // mis-renders right after switching items
        if (activeSchematicItem != null && ItemStack.matches(player.getMainHandItem(), activeSchematicItem))
            this.overlay.renderOn(guiGraphics, player.getInventory().selected);

        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        if (currentTool != null)
            currentTool.getTool()
                    .renderOverlay(mc.gui, guiGraphics, partialTicks, guiGraphics.guiWidth(), guiGraphics.guiHeight());
        if (selectionScreen != null)
            selectionScreen.renderPassive(guiGraphics, partialTicks);
    }

    @Override
    public boolean onMouseInput(int button, boolean pressed) {
        if (!active || !pressed || button != 1)
            return false;
        if (currentTool == null)
            return false;

        return currentTool.getTool().handleRightClick();
    }

    @Override
    public void onKeyInput(int key, boolean pressed) {
        if (!active)
            return;
        if (!AllKeys.TOOL_MENU.doesModifierAndCodeMatch(key))
            return;

        if (pressed && !selectionScreen.focused)
            selectionScreen.focused = true;
        if (!pressed && selectionScreen.focused) {
            selectionScreen.focused = false;
            selectionScreen.onClose();
        }
    }

    @Override
    public boolean mouseScrolled(double delta) {
        if (!active)
            return false;

        if (selectionScreen.focused) {
            selectionScreen.cycle((int) Math.signum(delta));
            return true;
        }
        if (AllKeys.ctrlDown() && currentTool != null)
            return currentTool.getTool().handleMouseWheel(delta);
        return false;
    }

    private ItemStack findBlueprintInHand(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty())
            return null;
        if (!AllItems.SIMPLE_SCHEMATIC.isIn(stack))
            return null;
        if (!stack.has(AllDataComponents.SCHEMATIC_FILE))
            return null;

        activeSchematicItem = stack;
        return stack;
    }

    private boolean itemLost(Player player) {
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !ItemStack.matches(stack, activeSchematicItem))
                continue;
            return false;
        }
        return true;
    }

    @Override
    public void equip(ToolType tool) {
        this.currentTool = SimpleToolType.of(tool);
        currentTool.getTool().init();
    }

    @Override
    public void deploy() {
        if (!deployed) {
            Minecraft mc = Minecraft.getInstance();
            boolean creative = mc.player != null && mc.player.isCreative();
            List<ToolType> tools = SimpleToolType.getTools(creative);
            selectionScreen = new SimpleToolSelectionScreen(tools, this::equip);
        }
        deployed = true;
        setupRenderer();
    }

    @Override
    public void printInstantly() {
        if (activeSchematicItem == null || transformation == null)
            return;

        StructurePlaceSettings settings = transformation.toSettings();
        CatnipServices.NETWORK.sendToServer(new SimpleSchematicPlacePacket(
                activeSchematicItem.copy(), transformation.getAnchor(), settings.getRotation(), settings.getMirror()));
        // Reset the deploy position state
        activeSchematicItem = null;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public AABB getBounds() {
        return bounds;
    }

    @Override
    public SchematicTransformation getTransformation() {
        return transformation;
    }

    @Override
    public boolean isDeployed() {
        return deployed;
    }

    @Override
    public ItemStack getActiveSchematicItem() {
        return activeSchematicItem;
    }

    @Override
    public AABBOutline getOutline() {
        return outline;
    }

    public void setInactive() {
        active = false;
    }
}
