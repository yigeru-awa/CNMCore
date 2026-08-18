package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.client;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Accepts ingredients dragged from the JEI list onto the selected wireless node's
 * two frequency slots. The item is only copied as a ghost frequency reference.
 */
public class TerminalGhostIngredientHandler implements IGhostIngredientHandler<WirelessRedstoneControlTerminalScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(WirelessRedstoneControlTerminalScreen screen,
            ITypedIngredient<I> ingredient, boolean doStart) {
        if (!(ingredient.getIngredient() instanceof ItemStack)) {
            return List.of();
        }
        List<Target<I>> targets = new ArrayList<>();
        for (int slot = 0; slot < 2; slot++) {
            Rect2i area = screen.frequencySlotRect(slot);
            if (area == null) {
                return List.of();
            }
            final int targetSlot = slot;
            targets.add(new Target<I>() {
                @Override
                public Rect2i getArea() {
                    return area;
                }

                @Override
                public void accept(I target) {
                    if (target instanceof ItemStack stack) {
                        screen.jeiSetFrequency(stack, targetSlot);
                    }
                }
            });
        }
        return targets;
    }

    @Override
    public <I> boolean quickMove(WirelessRedstoneControlTerminalScreen screen, ITypedIngredient<I> ingredient) {
        // Shift-click an ingredient in JEI while the frequency slots are visible; fills the first slot
        if (screen.frequencySlotRect(0) != null && ingredient.getIngredient() instanceof ItemStack stack) {
            screen.jeiSetFrequency(stack, 0);
            return true;
        }
        return false;
    }

    @Override
    public void onComplete() {
    }
}
