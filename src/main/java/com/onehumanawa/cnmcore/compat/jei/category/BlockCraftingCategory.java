package com.onehumanawa.cnmcore.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.foundation.recipe.blockcrafting.BlockCraftingRecipe;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings({"unused", "removal"})
public class BlockCraftingCategory implements IRecipeCategory<BlockCraftingRecipe> {

    public static final RecipeType<BlockCraftingRecipe> TYPE = RecipeType.create(
            CNMCore.ID,
            "block_crafting",
            BlockCraftingRecipe.class
    );

    private static final int WIDTH = 177;
    private static final int HEIGHT = 120;

    // 3D preview area
    private static final int PREVIEW_X = 56;
    private static final int PREVIEW_Y = 6;
    private static final int PREVIEW_W = 80;
    private static final int PREVIEW_H = 88;
    private static final float SCALE_MIN = 8f;
    private static final float SCALE_MAX = 20f;

    private static final int COLOR_HOVER = 0xc9974c;
    private static final int COLOR_KEPT = 0xffe3b341;
    private static final int COLOR_CENTER = 0xffeaeff4;
    private static final int COLOR_SHADOW = 0x33181c1e;
    private static final int COLOR_HINT = 0xff9d9d9d;

    private static final IDrawable SLOT_DRAWABLE = new IDrawable() {
        @Override
        public int getWidth() {
            return 18;
        }

        @Override
        public int getHeight() {
            return 18;
        }

        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            AllGuiTextures.JEI_SLOT.render(graphics, xOffset - 1, yOffset - 1);
        }
    };

    private final IDrawable background;
    private final IDrawable icon;

    private Integer currentLayer = null;
    private BlockPos hoveredBlock = null;

    public BlockCraftingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Blocks.ANDESITE));
    }

    @Override
    public RecipeType<BlockCraftingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("cnmcore.recipe.block_crafting");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BlockCraftingRecipe recipe, IFocusGroup focuses) {
        var inputItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(recipe.itemInputId()));
        if (inputItem != null && inputItem != Items.AIR) {
            builder.addSlot(RecipeIngredientRole.INPUT, 12, 51)
                    .setBackground(SLOT_DRAWABLE, -1, -1)
                    .addItemStack(new ItemStack(inputItem))
                    .addRichTooltipCallback((view, tooltips) -> {
                        if (!recipe.consumeInput()) {
                            tooltips.add(Component.translatable("cnmcore.recipe.block_crafting.not_consumed")
                                    .withStyle(ChatFormatting.GOLD));
                        }
                    });
        }

        var resultMap = new HashMap<String, Integer>();
        for (String resultId : recipe.resultIds()) {
            resultMap.merge(resultId, 1, Integer::sum);
        }

        int idx = 0;
        for (var entry : resultMap.entrySet()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(entry.getKey()));
            if (item == null || item == Items.AIR) continue;
            int xOffset = idx % 2 == 0 ? 0 : 19;
            int yOffset = (idx / 2) * 19;
            builder.addSlot(RecipeIngredientRole.OUTPUT, 142 + xOffset, 34 + yOffset)
                    .setBackground(SLOT_DRAWABLE, -1, -1)
                    .addItemStack(new ItemStack(item, entry.getValue()));
            idx++;
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BlockCraftingRecipe recipe,
                                   IRecipeSlotsView slotsView, IFocusGroup focuses) {
        List<BlockCraftingRecipe.PatternEntry> pattern = recipe.pattern();
        int[] bounds = computeBounds(pattern);
        if (bounds[1] == bounds[4]) return; // single layer, nothing to cycle

        builder.addGuiEventListener(new IJeiGuiEventListener() {
            @Override
            public ScreenRectangle getArea() {
                return new ScreenRectangle(new ScreenPosition(PREVIEW_X, PREVIEW_Y), PREVIEW_W, PREVIEW_H);
            }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
                cycleLayer(bounds[1], bounds[4], scrollY >= 0 ? 1 : -1);
                return true;
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0 && currentLayer != null) {
                    currentLayer = null;
                    return true;
                }
                return false;
            }
        });
    }

    private void cycleLayer(int minY, int maxY, int dir) {
        if (currentLayer == null) {
            currentLayer = dir > 0 ? maxY : minY;
        } else {
            currentLayer += dir;
            if (currentLayer > maxY) currentLayer = minY;
            if (currentLayer < minY) currentLayer = maxY;
        }
    }

    @Override
    public void draw(BlockCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_ARROW.render(graphics, 38, 55);
        AllGuiTextures.JEI_ARROW.render(graphics, 118, 55);

        drawBlockStructure(recipe, graphics, (int) mouseX, (int) mouseY);
        drawLayerInfo(recipe, graphics);
    }

    private record PreviewBlock(int x, int y, int z, Block block, boolean kept, boolean center,
                                float screenX, float screenY, float depth) {}

    private void drawBlockStructure(BlockCraftingRecipe recipe, GuiGraphics graphics, int mouseX, int mouseY) {
        List<BlockCraftingRecipe.PatternEntry> pattern = recipe.pattern();
        if (pattern.isEmpty()) return;

        int[] bounds = computeBounds(pattern);
        int minX = bounds[0], minY = bounds[1], minZ = bounds[2];
        int maxX = bounds[3], maxY = bounds[4], maxZ = bounds[5];
        if (currentLayer != null) {
            currentLayer = Math.max(minY, Math.min(maxY, currentLayer));
        }

        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        int centerZ = (minZ + maxZ) / 2;

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        float maxDim = Math.max(Math.max(sizeX, sizeY), sizeZ);
        float scale = Math.max(SCALE_MIN, Math.min(SCALE_MAX, 55f / maxDim));
        float blockSize = 16f * scale / SCALE_MAX;

        float baseX = PREVIEW_X + PREVIEW_W / 2f - (sizeX - 1) * blockSize / 2;
        float baseY = PREVIEW_Y + PREVIEW_H / 2f - (sizeY - 1) * blockSize / 2;

        Matrix4f combined = new Matrix4f();
        combined.translate(baseX, baseY, 100);
        combined.rotate(Axis.XP.rotationDegrees(-15.5f));
        combined.rotate(Axis.YP.rotationDegrees(22.5f));
        combined.scale(scale);

        // Resolve blocks once per frame and project their centers to screen space
        List<PreviewBlock> blocks = new ArrayList<>();
        for (var entry : pattern) {
            var offset = entry.offset();
            int x = offset.getX() - centerX;
            int y = offset.getY() - centerY;
            int z = offset.getZ() - centerZ;

            if (currentLayer != null && offset.getY() != currentLayer) continue;

            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(entry.blockId()));
            if (block == null || block == Blocks.AIR) continue;

            boolean center = recipe.isCenter(entry);
            boolean kept = !(center ? recipe.consumeCenter() : recipe.consumePattern());

            Vector4f pos = new Vector4f(x + 0.5f, y + 0.5f, z + 0.5f, 1f);
            pos.mul(combined);
            blocks.add(new PreviewBlock(x, y, z, block, kept, center,
                    pos.x() / pos.w(), pos.y() / pos.w(), pos.z()));
        }

        // Painter's algorithm: draw far blocks first so near blocks overlap correctly
        blocks.sort(Comparator.comparingDouble(PreviewBlock::depth));

        PoseStack poseStack = graphics.pose();

        drawGroundShadow(graphics, combined, minX, minY, minZ, maxX, maxZ, centerX, centerY, centerZ, scale);

        poseStack.pushPose();
        poseStack.mulPose(combined);
        var lighting = AnimatedKinetics.DEFAULT_LIGHTING;
        for (PreviewBlock b : blocks) {
            poseStack.pushPose();
            poseStack.translate(b.x(), b.y() + 1, b.z());
            GuiGameElement.of(b.block().defaultBlockState())
                    .lighting(lighting)
                    .render(graphics);
            poseStack.popPose();
        }
        poseStack.popPose();

        // Hit test: nearest (last drawn) block wins
        hoveredBlock = null;
        float halfSize = 8f * scale / SCALE_MAX;
        for (int i = blocks.size() - 1; i >= 0; i--) {
            PreviewBlock b = blocks.get(i);
            if (Math.abs(mouseX - b.screenX()) < halfSize && Math.abs(mouseY - b.screenY()) < halfSize) {
                hoveredBlock = new BlockPos(b.x(), b.y(), b.z());
                break;
            }
        }

        if (hoveredBlock != null) {
            drawHoverOutline(graphics, hoveredBlock, combined);
        }

        drawBlockBadges(graphics, blocks);
    }

    private void drawGroundShadow(GuiGraphics graphics, Matrix4f combined,
                                  int minX, int minY, int minZ, int maxX, int maxZ,
                                  int centerX, int centerY, int centerZ, float scale) {
        float groundY = minY - centerY + 1.1f;
        float sx = 0, sy = 0;
        int corners = 0;
        for (int cx : new int[]{minX, maxX + 1}) {
            for (int cz : new int[]{minZ, maxZ + 1}) {
                Vector4f pos = new Vector4f(cx - centerX, groundY, cz - centerZ, 1f);
                pos.mul(combined);
                sx += pos.x() / pos.w();
                sy += pos.y() / pos.w();
                corners++;
            }
        }
        sx /= corners;
        sy /= corners;

        int footprint = Math.max(maxX - minX, maxZ - minZ) + 1;
        float radiusX = Math.max(10f, footprint * 7f * scale / SCALE_MAX);
        drawEllipse(graphics, sx, sy + 3f, radiusX, radiusX * 0.35f, COLOR_SHADOW);
    }

    private void drawEllipse(GuiGraphics graphics, float cx, float cy, float rx, float ry, int color) {
        int steps = 40;
        for (int i = 0; i < steps; i++) {
            float a0 = (float) (2 * Math.PI * i / steps);
            float a1 = (float) (2 * Math.PI * (i + 1) / steps);
            int x0 = Math.round(cx + rx * (float) Math.cos(a0));
            int x1 = Math.round(cx + rx * (float) Math.cos(a1)) + 1;
            int y = Math.round(cy + ry * (float) Math.sin(a0));
            graphics.fill(Math.min(x0, x1), y, Math.max(x0, x1), y + 1, color);
        }
    }

    private void drawBlockBadges(GuiGraphics graphics, List<PreviewBlock> blocks) {
        for (PreviewBlock b : blocks) {
            if (b.kept()) {
                drawDiamond(graphics, b.screenX(), b.screenY() - 9, 2.5f, COLOR_KEPT);
            } else if (b.center()) {
                drawDiamond(graphics, b.screenX(), b.screenY() - 9, 2.5f, COLOR_CENTER);
            }
        }
    }

    private void drawDiamond(GuiGraphics graphics, float cx, float cy, float r, int color) {
        for (int dy = 0; dy <= r; dy++) {
            float dx = r - dy;
            int x0 = Math.round(cx - dx);
            int x1 = Math.round(cx + dx) + 1;
            graphics.fill(x0, Math.round(cy - dy), x1, Math.round(cy - dy) + 1, color);
            graphics.fill(x0, Math.round(cy + dy), x1, Math.round(cy + dy) + 1, color);
        }
    }

    private void drawHoverOutline(GuiGraphics graphics, BlockPos pos, Matrix4f combined) {
        PoseStack poseStack = graphics.pose();
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();

        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

        poseStack.pushPose();
        poseStack.mulPose(combined);

        AABBOutline outline = new AABBOutline(box);
        outline.getParams()
                .colored(COLOR_HOVER)
                .lineWidth(1.5f / 16f);

        outline.render(poseStack, buffer, Vec3.ZERO, 0);

        poseStack.popPose();
        buffer.draw();
    }

    private void drawLayerInfo(BlockCraftingRecipe recipe, GuiGraphics graphics) {
        List<BlockCraftingRecipe.PatternEntry> pattern = recipe.pattern();
        if (pattern.isEmpty()) return;

        int[] bounds = computeBounds(pattern);
        int minY = bounds[1], maxY = bounds[4];
        if (minY == maxY) return;

        Font font = Minecraft.getInstance().font;
        Component text = currentLayer == null
                ? Component.translatable("cnmcore.recipe.block_crafting.layer_scroll")
                : Component.translatable("cnmcore.recipe.block_crafting.layer",
                        currentLayer - minY + 1, maxY - minY + 1);
        int textX = PREVIEW_X + (PREVIEW_W - font.width(text)) / 2;
        graphics.drawString(font, text, textX, PREVIEW_Y + PREVIEW_H + 4, COLOR_HINT, false);
    }

    @Override
    public List<Component> getTooltipStrings(BlockCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (hoveredBlock == null) return List.of();

        List<BlockCraftingRecipe.PatternEntry> pattern = recipe.pattern();
        int[] bounds = computeBounds(pattern);
        int centerX = (bounds[0] + bounds[3]) / 2;
        int centerY = (bounds[1] + bounds[4]) / 2;
        int centerZ = (bounds[2] + bounds[5]) / 2;

        for (var entry : pattern) {
            var offset = entry.offset();
            int x = offset.getX() - centerX;
            int y = offset.getY() - centerY;
            int z = offset.getZ() - centerZ;

            if (currentLayer != null && offset.getY() != currentLayer) continue;
            if (x != hoveredBlock.getX() || y != hoveredBlock.getY() || z != hoveredBlock.getZ()) continue;

            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(entry.blockId()));
            if (block == null || block == Blocks.AIR) continue;

            List<Component> tooltips = new ArrayList<>();
            tooltips.add(block.getName().copy().withStyle(ChatFormatting.WHITE));

            boolean center = recipe.isCenter(entry);
            boolean consumed = center ? recipe.consumeCenter() : recipe.consumePattern();

            tooltips.add(Component.translatable("cnmcore.recipe.block_crafting.consumed_status",
                            Component.translatable(consumed
                                    ? "cnmcore.recipe.block_crafting.consumed"
                                    : "cnmcore.recipe.block_crafting.not_consumed"))
                    .withStyle(consumed ? ChatFormatting.RED : ChatFormatting.GOLD));

            if (center) {
                tooltips.add(Component.translatable("cnmcore.recipe.block_crafting.center_marker")
                        .withStyle(ChatFormatting.AQUA));
            }

            return tooltips;
        }
        return List.of();
    }

    private static int[] computeBounds(List<BlockCraftingRecipe.PatternEntry> pattern) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (var entry : pattern) {
            var offset = entry.offset();
            minX = Math.min(minX, offset.getX());
            maxX = Math.max(maxX, offset.getX());
            minY = Math.min(minY, offset.getY());
            maxY = Math.max(maxY, offset.getY());
            minZ = Math.min(minZ, offset.getZ());
            maxZ = Math.max(maxZ, offset.getZ());
        }
        return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
    }
}
