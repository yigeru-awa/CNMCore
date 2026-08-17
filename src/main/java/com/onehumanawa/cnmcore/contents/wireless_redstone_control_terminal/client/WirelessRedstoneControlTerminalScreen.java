package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.client;

import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.WirelessRedstoneControlTerminalBlockEntity;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.WirelessRedstoneControlTerminalMenu;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.Circuit;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.CircuitNode;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.NodeType;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network.TerminalEditPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Circuit editor screen: palette on the left, a scrollable node canvas in the middle,
 * config panel on the right and the two Redstone Link frequency pairs on top.
 *
 * Controls:
 * - click a palette button to add a node
 * - drag a node body to move it, middle-click a node body to delete it, right-click to deselect
 * - wiring: click an output port (right side), optionally click blank canvas to add waypoints
 *   (the wire must pass through them), then click the target input port to connect.
 *   Right-click cancels wiring.
 * - right-click an input port to disconnect its wire
 * - drag blank canvas to pan the viewport, use the scrollbars or the mouse wheel to scroll
 * - Delete key removes the selected node
 */
public class WirelessRedstoneControlTerminalScreen extends AbstractContainerScreen<WirelessRedstoneControlTerminalMenu> {
    public static final int X_SIZE = 286;
    public static final int Y_SIZE = 240;

    private static final int CANVAS_X = 56;
    private static final int CANVAS_Y = 32;
    private static final int CANVAS_WIDTH = 162;
    private static final int CANVAS_HEIGHT = 116;
    private static final int PALETTE_X = 8;
    private static final int PALETTE_Y = 32;
    private static final int PALETTE_COLUMNS = 2;
    private static final int PALETTE_ROWS = 9;
    private static final int PALETTE_BUTTON_WIDTH = 22;
    private static final int PALETTE_BUTTON_HEIGHT = 11;
    private static final int CONFIG_X = 222;
    private static final int CONFIG_Y = 32;
    private static final int CONFIG_WIDTH = 56;
    private static final int CONFIG_HEIGHT = 116;
    private static final int PORT_SPACING = 6;
    private static final int PORT_HIT_RADIUS = 5;
    private static final int SCROLLBAR_THICKNESS = 3;
    private static final int SCROLL_STEP = 8;

    /** Full container background, painted 1:1 (texture size == screen size). Regions:
     *  (6,6)-(280,28) top bar; (6,30)-(54,150) palette; (54,30)-(220,150) canvas;
     *  (220,30)-(278,150) config panel; (6,218)-(23,235) reusable 18x18 item slot background. */
    private static final ResourceLocation CONTAINER_TEXTURE = ResourceLocation.fromNamespaceAndPath("cnmcore", "textures/gui/wrt_container.png");
    /** Node box: 1 px border + fill, nine-sliced when drawn. */
    private static final ResourceLocation NODE_BOX_TEXTURE = ResourceLocation.fromNamespaceAndPath("cnmcore", "textures/gui/wrt_node.png");
    /** Active node frame overlay (border only). */
    private static final ResourceLocation NODE_FRAME_ON_TEXTURE = ResourceLocation.fromNamespaceAndPath("cnmcore", "textures/gui/wrt_node_on.png");
    /** Selected node / hovered button frame overlay (border only). */
    private static final ResourceLocation NODE_FRAME_SELECTED_TEXTURE = ResourceLocation.fromNamespaceAndPath("cnmcore", "textures/gui/wrt_node_selected.png");
    /** Port sprites, blit size 6x6. */
    private static final ResourceLocation PORT_ON_TEXTURE = ResourceLocation.fromNamespaceAndPath("cnmcore", "textures/gui/wrt_port_on.png");
    private static final ResourceLocation PORT_OFF_TEXTURE = ResourceLocation.fromNamespaceAndPath("cnmcore", "textures/gui/wrt_port_off.png");
    /** Slot background region inside CONTAINER_TEXTURE, drawn behind every container slot. */
    private static final int SLOT_BG_U = 6;
    private static final int SLOT_BG_V = 218;
    private static final int SLOT_BG_SIZE = 18;

    private static final int COLOR_TEXT = 0xFFC8CDD6;
    private static final int COLOR_TEXT_DIM = 0xFF8A919E;
    private static final int COLOR_WIRE_OFF = 0xFF7A6A55;
    private static final int COLOR_WIRE_ON = 0xFF59C959;
    private static final int COLOR_FLOW_DOT = 0xFFB6FFB6;
    private static final int COLOR_WAYPOINT = 0xFFFFC94D;
    private static final int COLOR_SCROLLBAR_TRACK = 0xFF2C2A24;
    private static final int COLOR_SCROLLBAR_THUMB = 0xFF9C8B74;
    private static final int COLOR_SCROLLBAR_THUMB_ACTIVE = 0xFFC8B69A;
    private static final int COLOR_ERROR = 0xFFFF5555;

    private int selectedId = -1;
    private int draggingId = -1;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean panning;
    private int panLastX;
    private int panLastY;
    /** 0 = none, 1 = vertical scrollbar, 2 = horizontal scrollbar. */
    private int scrollbarDrag;
    private int pendingWireSource = -1;
    private final List<Integer> pendingWaypoints = new ArrayList<>();
    private int scrollX;
    private int scrollY;
    private float animationTime;

    public WirelessRedstoneControlTerminalScreen(WirelessRedstoneControlTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = X_SIZE;
        this.imageHeight = Y_SIZE;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Nullable
    private WirelessRedstoneControlTerminalBlockEntity blockEntity() {
        return menu.blockEntity;
    }

    @Nullable
    private Circuit circuit() {
        return blockEntity() == null ? null : blockEntity().getCircuit();
    }

    // Viewport

    private static int maxScrollX() {
        return Math.max(0, Circuit.AREA_WIDTH - CANVAS_WIDTH);
    }

    private static int maxScrollY() {
        return Math.max(0, Circuit.AREA_HEIGHT - CANVAS_HEIGHT);
    }

    private void clampScroll() {
        scrollX = Mth.clamp(scrollX, 0, maxScrollX());
        scrollY = Mth.clamp(scrollY, 0, maxScrollY());
    }

    private int areaToScreenX(int areaX) {
        return leftPos + CANVAS_X + areaX - scrollX;
    }

    private int areaToScreenY(int areaY) {
        return topPos + CANVAS_Y + areaY - scrollY;
    }

    private int screenToAreaX(double screenX) {
        return Mth.clamp((int) screenX - leftPos - CANVAS_X + scrollX, 0, Circuit.AREA_WIDTH - 1);
    }

    private int screenToAreaY(double screenY) {
        return Mth.clamp((int) screenY - topPos - CANVAS_Y + scrollY, 0, Circuit.AREA_HEIGHT - 1);
    }

    private boolean inCanvas(double mouseX, double mouseY) {
        return inRect(mouseX, mouseY, leftPos + CANVAS_X, topPos + CANVAS_Y, CANVAS_WIDTH, CANVAS_HEIGHT);
    }

    // Rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        animationTime += partialTick;
        // Re-apply the in-progress drag position every frame so server snapshots
        // (arriving every few ticks) cannot snap the dragged node back
        if (draggingId >= 0) {
            Circuit circuit = circuit();
            CircuitNode node = circuit == null ? null : circuit.nodeById(draggingId);
            if (node != null) {
                node.x = Circuit.clampX((int) mouseX - leftPos - CANVAS_X + scrollX - dragOffsetX);
                node.y = Circuit.clampY((int) mouseY - topPos - CANVAS_Y + scrollY - dragOffsetY);
            } else {
                draggingId = -1;
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderCustomTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(CONTAINER_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, X_SIZE, Y_SIZE);
        // Slot backgrounds are part of the container texture; blit the template behind every slot
        for (Slot slot : this.menu.slots) {
            graphics.blit(CONTAINER_TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1,
                    SLOT_BG_U, SLOT_BG_V, SLOT_BG_SIZE, SLOT_BG_SIZE, X_SIZE, Y_SIZE);
        }

        Circuit circuit = circuit();
        if (circuit != null) {
            // Clip all canvas drawing to the canvas viewport
            graphics.enableScissor(leftPos + CANVAS_X, topPos + CANVAS_Y,
                    leftPos + CANVAS_X + CANVAS_WIDTH, topPos + CANVAS_Y + CANVAS_HEIGHT);
            drawWires(graphics, circuit);
            drawNodes(graphics, circuit);
            drawPendingWire(graphics, mouseX, mouseY);
            graphics.disableScissor();
        } else {
            graphics.drawString(font, Component.translatable("cnmcore.wrt.error"),
                    leftPos + CANVAS_X + 8, topPos + CANVAS_Y + 8, COLOR_ERROR, false);
        }

        drawPalette(graphics, mouseX, mouseY);
        drawConfigPanel(graphics, mouseX, mouseY);
        drawTopBar(graphics);
        drawScrollbars(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Drawn by drawTopBar / drawConfigPanel in absolute coordinates instead
    }

    private void drawTopBar(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("cnmcore.wrt.rx"), leftPos + 46, topPos + 12, COLOR_TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("cnmcore.wrt.tx"), leftPos + 102, topPos + 12, COLOR_TEXT_DIM, false);
        Circuit circuit = circuit();
        String count = (circuit == null ? 0 : circuit.getNodes().size()) + "/" + Circuit.MAX_NODES;
        graphics.drawString(font, Component.translatable("cnmcore.wrt.nodes"), leftPos + 150, topPos + 12, COLOR_TEXT_DIM, false);
        graphics.drawString(font, count, leftPos + 150 + font.width(Component.translatable("cnmcore.wrt.nodes")) + 4, topPos + 12, COLOR_TEXT, false);
    }

    // Nodes & wires

    private static int nodeHeight(CircuitNode node) {
        return Math.max(Circuit.NODE_MIN_HEIGHT, node.inputs.length * PORT_SPACING + 8);
    }

    private static int inputPortOffset(int port) {
        return 10 + port * PORT_SPACING;
    }

    private void drawNodes(GuiGraphics graphics, Circuit circuit) {
        for (CircuitNode node : circuit.getNodes()) {
            drawNode(graphics, circuit, node);
        }
    }

    private void drawNode(GuiGraphics graphics, Circuit circuit, CircuitNode node) {
        int x = areaToScreenX(node.x);
        int y = areaToScreenY(node.y);
        int width = Circuit.NODE_WIDTH;
        int height = nodeHeight(node);
        boolean on = node.value > 0;
        boolean selected = node.id == selectedId;

        blitBox(graphics, NODE_BOX_TEXTURE, x, y, width, height);
        if (selected) {
            graphics.blit(NODE_FRAME_SELECTED_TEXTURE, x, y, 0, 0, width, height, width, height);
        } else if (on) {
            graphics.blit(NODE_FRAME_ON_TEXTURE, x, y, 0, 0, width, height, width, height);
        }

        String symbol = node.type.getSymbol();
        graphics.drawString(font, symbol, x + (width - font.width(symbol)) / 2, y + 3, on ? COLOR_WIRE_ON : COLOR_TEXT, false);

        // Signal strength bar at the bottom of the node
        if (on) {
            int barWidth = Math.max(1, (width - 6) * node.value / 15);
            graphics.fill(x + 3, y + height - 4, x + 3 + barWidth, y + height - 2, COLOR_WIRE_ON);
        }

        // Input ports
        for (int port = 0; port < node.inputs.length; port++) {
            boolean portOn = node.inputValue(circuit, port) > 0;
            graphics.blit(portOn ? PORT_ON_TEXTURE : PORT_OFF_TEXTURE,
                    x - 3, y + inputPortOffset(port) - 3, 0, 0, 6, 6, 6, 6);
        }

        // Output port
        graphics.blit(on ? PORT_ON_TEXTURE : PORT_OFF_TEXTURE, x + width - 3, y + height / 2 - 3, 0, 0, 6, 6, 6, 6);
    }

    private void drawWires(GuiGraphics graphics, Circuit circuit) {
        for (CircuitNode node : circuit.getNodes()) {
            for (int port = 0; port < node.inputs.length; port++) {
                int sourceId = node.inputs[port];
                if (sourceId < 0) {
                    continue;
                }
                CircuitNode source = circuit.nodeById(sourceId);
                if (source == null) {
                    continue;
                }
                int[] waypoints = node.waypoints[port];
                int count = 2 + (waypoints == null ? 0 : waypoints.length);
                int[] xs = new int[count];
                int[] ys = new int[count];
                xs[0] = areaToScreenX(source.x + Circuit.NODE_WIDTH);
                ys[0] = areaToScreenY(source.y) + nodeHeight(source) / 2;
                int index = 1;
                if (waypoints != null) {
                    for (int packed : waypoints) {
                        xs[index] = areaToScreenX(packed >> 8);
                        ys[index] = areaToScreenY(packed & 0xFF);
                        graphics.fill(xs[index] - 1, ys[index] - 1, xs[index] + 2, ys[index] + 2, COLOR_WAYPOINT);
                        index++;
                    }
                }
                xs[index] = areaToScreenX(node.x);
                ys[index] = areaToScreenY(node.y) + inputPortOffset(port);
                drawWirePath(graphics, xs, ys, count, source.value > 0, source.id * 3 + port);
            }
        }
    }

    private void drawWirePath(GuiGraphics graphics, int[] xs, int[] ys, int count, boolean on, int seed) {
        int color = on ? COLOR_WIRE_ON : COLOR_WIRE_OFF;
        float total = 0;
        for (int i = 0; i < count - 1; i++) {
            int length = Math.abs(xs[i + 1] - xs[i]) + Math.abs(ys[i + 1] - ys[i]);
            total += length;
            segment(graphics, xs[i], ys[i], xs[i + 1], ys[i + 1], color);
        }

        // Signal flow animation: a bright dot travelling along the wire while active
        if (!on || total <= 0) {
            return;
        }
        float progress = (animationTime * 1.2f + seed * 7) % total;
        int px = xs[0];
        int py = ys[0];
        for (int i = 0; i < count - 1; i++) {
            float length = Math.abs(xs[i + 1] - xs[i]) + Math.abs(ys[i + 1] - ys[i]);
            if (progress <= length) {
                float t = length == 0 ? 0 : progress / length;
                px = (int) (xs[i] + (xs[i + 1] - xs[i]) * t);
                py = (int) (ys[i] + (ys[i + 1] - ys[i]) * t);
                break;
            }
            progress -= length;
            px = xs[i + 1];
            py = ys[i + 1];
        }
        graphics.fill(px - 1, py - 1, px + 2, py + 2, COLOR_FLOW_DOT);
    }

    private void segment(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(Math.min(x1, x2) - 1, Math.min(y1, y2) - 1, Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
    }

    private void drawPendingWire(GuiGraphics graphics, int mouseX, int mouseY) {
        Circuit circuit = circuit();
        if (pendingWireSource < 0 || circuit == null) {
            return;
        }
        CircuitNode source = circuit.nodeById(pendingWireSource);
        if (source == null) {
            cancelWiring();
            return;
        }
        int count = 2 + pendingWaypoints.size();
        int[] xs = new int[count];
        int[] ys = new int[count];
        xs[0] = areaToScreenX(source.x + Circuit.NODE_WIDTH);
        ys[0] = areaToScreenY(source.y) + nodeHeight(source) / 2;
        int index = 1;
        for (int packed : pendingWaypoints) {
            xs[index] = areaToScreenX(packed >> 8);
            ys[index] = areaToScreenY(packed & 0xFF);
            graphics.fill(xs[index] - 1, ys[index] - 1, xs[index] + 2, ys[index] + 2, COLOR_WAYPOINT);
            index++;
        }
        xs[index] = mouseX;
        ys[index] = mouseY;
        drawWirePath(graphics, xs, ys, count, source.value > 0, source.id);
    }

    // Scrollbars

    private static int vThumbHeight() {
        return Math.max(14, CANVAS_HEIGHT * CANVAS_HEIGHT / Circuit.AREA_HEIGHT);
    }

    private static int hThumbWidth() {
        return Math.max(14, (CANVAS_WIDTH - SCROLLBAR_THICKNESS - 2) * (CANVAS_WIDTH - SCROLLBAR_THICKNESS - 2) / Circuit.AREA_WIDTH);
    }

    private int[] verticalThumbRect() {
        int trackX = leftPos + CANVAS_X + CANVAS_WIDTH - SCROLLBAR_THICKNESS - 1;
        int trackY = topPos + CANVAS_Y;
        int thumbHeight = vThumbHeight();
        int range = CANVAS_HEIGHT - thumbHeight;
        int thumbY = trackY + (maxScrollY() == 0 ? 0 : scrollY * range / maxScrollY());
        return new int[]{trackX, thumbY, SCROLLBAR_THICKNESS, thumbHeight};
    }

    private int[] horizontalThumbRect() {
        int trackWidth = CANVAS_WIDTH - SCROLLBAR_THICKNESS - 2;
        int trackX = leftPos + CANVAS_X;
        int trackY = topPos + CANVAS_Y + CANVAS_HEIGHT - SCROLLBAR_THICKNESS - 1;
        int thumbWidth = hThumbWidth();
        int range = trackWidth - thumbWidth;
        int thumbX = trackX + (maxScrollX() == 0 ? 0 : scrollX * range / maxScrollX());
        return new int[]{thumbX, trackY, thumbWidth, SCROLLBAR_THICKNESS};
    }

    private void drawScrollbars(GuiGraphics graphics) {
        int vTrackX = leftPos + CANVAS_X + CANVAS_WIDTH - SCROLLBAR_THICKNESS - 1;
        graphics.fill(vTrackX, topPos + CANVAS_Y, vTrackX + SCROLLBAR_THICKNESS, topPos + CANVAS_Y + CANVAS_HEIGHT, COLOR_SCROLLBAR_TRACK);
        int hTrackY = topPos + CANVAS_Y + CANVAS_HEIGHT - SCROLLBAR_THICKNESS - 1;
        graphics.fill(leftPos + CANVAS_X, hTrackY, leftPos + CANVAS_X + CANVAS_WIDTH - SCROLLBAR_THICKNESS - 2, hTrackY + SCROLLBAR_THICKNESS, COLOR_SCROLLBAR_TRACK);

        int[] vThumb = verticalThumbRect();
        graphics.fill(vThumb[0], vThumb[1], vThumb[0] + vThumb[2], vThumb[1] + vThumb[3],
                scrollbarDrag == 1 ? COLOR_SCROLLBAR_THUMB_ACTIVE : COLOR_SCROLLBAR_THUMB);
        int[] hThumb = horizontalThumbRect();
        graphics.fill(hThumb[0], hThumb[1], hThumb[0] + hThumb[2], hThumb[1] + hThumb[3],
                scrollbarDrag == 2 ? COLOR_SCROLLBAR_THUMB_ACTIVE : COLOR_SCROLLBAR_THUMB);
    }

    // Palette

    private void drawPalette(GuiGraphics graphics, int mouseX, int mouseY) {
        NodeType[] types = NodeType.values();
        for (int i = 0; i < types.length; i++) {
            int column = i / PALETTE_ROWS;
            int row = i % PALETTE_ROWS;
            int x = leftPos + PALETTE_X + column * 23;
            int y = topPos + PALETTE_Y + row * 12;
            boolean hovered = inRect(mouseX, mouseY, x, y, PALETTE_BUTTON_WIDTH, PALETTE_BUTTON_HEIGHT);
            blitBox(graphics, NODE_BOX_TEXTURE, x, y, PALETTE_BUTTON_WIDTH, PALETTE_BUTTON_HEIGHT);
            if (hovered) {
                graphics.blit(NODE_FRAME_SELECTED_TEXTURE, x, y, 0, 0, PALETTE_BUTTON_WIDTH, PALETTE_BUTTON_HEIGHT,
                        PALETTE_BUTTON_WIDTH, PALETTE_BUTTON_HEIGHT);
            }
            int color = switch (types[i].getCategory()) {
                case GATE -> 0xFF6FA8DC;
                case SEQUENTIAL -> 0xFFB088D9;
                case IO -> 0xFFE0A860;
            };
            String symbol = types[i].getSymbol();
            graphics.drawString(font, symbol, x + (PALETTE_BUTTON_WIDTH - font.width(symbol)) / 2, y + 2, color, false);
        }
    }

    private int paletteButtonIndex(double mouseX, double mouseY) {
        NodeType[] types = NodeType.values();
        for (int i = 0; i < types.length; i++) {
            int column = i / PALETTE_ROWS;
            int row = i % PALETTE_ROWS;
            int x = leftPos + PALETTE_X + column * 23;
            int y = topPos + PALETTE_Y + row * 12;
            if (inRect(mouseX, mouseY, x, y, PALETTE_BUTTON_WIDTH, PALETTE_BUTTON_HEIGHT)) {
                return i;
            }
        }
        return -1;
    }

    // Config panel

    private void drawConfigPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        Circuit circuit = circuit();
        int x = leftPos + CONFIG_X;
        int y = topPos + CONFIG_Y;

        CircuitNode selected = circuit != null && selectedId >= 0 ? circuit.nodeById(selectedId) : null;
        if (selected != null) {
            String symbol = selected.type.getSymbol();
            graphics.drawString(font, symbol, x + (CONFIG_WIDTH - font.width(symbol)) / 2, y + 4, COLOR_TEXT, false);
            Component name = Component.translatable("cnmcore.wrt.node." + selected.type.name().toLowerCase());
            graphics.drawString(font, name, x + (CONFIG_WIDTH - font.width(name)) / 2, y + 15, COLOR_TEXT_DIM, false);

            if (selected.type.isConfigurable()) {
                graphics.drawString(font, configLabel(selected.type), x + 3, y + 32, COLOR_TEXT_DIM, false);
                button(graphics, x + 4, y + 44, 12, 12, "-", inRect(mouseX, mouseY, x + 4, y + 44, 12, 12));
                button(graphics, x + CONFIG_WIDTH - 16, y + 44, 12, 12, "+", inRect(mouseX, mouseY, x + CONFIG_WIDTH - 16, y + 44, 12, 12));
                Component value = configValue(selected);
                graphics.drawString(font, value, x + (CONFIG_WIDTH - font.width(value)) / 2, y + 46, COLOR_TEXT, false);
            } else {
                Component note = configNote(selected.type);
                if (note != null) {
                    graphics.drawString(font, note, x + (CONFIG_WIDTH - font.width(note)) / 2, y + 36, COLOR_TEXT_DIM, false);
                }
            }

            button(graphics, x + 4, y + 64, CONFIG_WIDTH - 8, 12, Component.translatable("cnmcore.wrt.delete").getString(),
                    inRect(mouseX, mouseY, x + 4, y + 64, CONFIG_WIDTH - 8, 12));
        } else {
            Component hint = Component.translatable("cnmcore.wrt.hint");
            graphics.drawString(font, hint, x + (CONFIG_WIDTH - font.width(hint)) / 2, y + 4, COLOR_TEXT_DIM, false);
        }

        button(graphics, x + 4, y + CONFIG_HEIGHT - 16, CONFIG_WIDTH - 8, 12, Component.translatable("cnmcore.wrt.clear").getString(),
                inRect(mouseX, mouseY, x + 4, y + CONFIG_HEIGHT - 16, CONFIG_WIDTH - 8, 12));
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, String text, boolean hovered) {
        blitBox(graphics, NODE_BOX_TEXTURE, x, y, width, height);
        if (hovered) {
            graphics.blit(NODE_FRAME_SELECTED_TEXTURE, x, y, 0, 0, width, height, width, height);
        }
        graphics.drawString(font, text, x + (width - font.width(text)) / 2, y + 2, COLOR_TEXT, false);
    }

    private Component configLabel(NodeType type) {
        String key = switch (type) {
            case AND, OR, XOR, NAND, NOR, XNOR -> "input_count";
            case PULSE -> "pulse_width";
            case CLOCK -> "period";
            case DELAY -> "delay";
            case COUNTER -> "threshold";
            case COMPARE -> "mode";
            case INPUT -> "side";
            default -> "value";
        };
        return Component.translatable("cnmcore.wrt.cfg." + key);
    }

    private Component configValue(CircuitNode node) {
        return switch (node.type) {
            case COMPARE -> Component.translatable("cnmcore.wrt.mode." + switch (node.config) {
                case 0 -> "gt";
                case 1 -> "lt";
                default -> "eq";
            });
            case INPUT -> Component.translatable("cnmcore.wrt.side." + node.config);
            default -> Component.literal(String.valueOf(node.config));
        };
    }

    @Nullable
    private Component configNote(NodeType type) {
        return switch (type) {
            case W_IN -> Component.translatable("cnmcore.wrt.note.rx");
            case W_OUT -> Component.translatable("cnmcore.wrt.note.tx");
            case OUTPUT -> Component.translatable("cnmcore.wrt.note.passthrough");
            case LATCH -> Component.translatable("cnmcore.wrt.note.latch");
            default -> null;
        };
    }

    // Hit testing

    private boolean inRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Nullable
    private CircuitNode nodeAt(double mouseX, double mouseY) {
        Circuit circuit = circuit();
        if (circuit == null) {
            return null;
        }
        List<CircuitNode> nodes = circuit.getNodes();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            CircuitNode node = nodes.get(i);
            if (inRect(mouseX, mouseY, areaToScreenX(node.x), areaToScreenY(node.y), Circuit.NODE_WIDTH, nodeHeight(node))) {
                return node;
            }
        }
        return null;
    }

    @Nullable
    private CircuitNode outputPortAt(double mouseX, double mouseY) {
        Circuit circuit = circuit();
        if (circuit == null) {
            return null;
        }
        for (CircuitNode node : circuit.getNodes()) {
            int x = areaToScreenX(node.x + Circuit.NODE_WIDTH);
            int y = areaToScreenY(node.y) + nodeHeight(node) / 2;
            if (mouseX >= x - PORT_HIT_RADIUS && mouseX <= x + PORT_HIT_RADIUS
                    && mouseY >= y - PORT_HIT_RADIUS && mouseY <= y + PORT_HIT_RADIUS) {
                return node;
            }
        }
        return null;
    }

    @Nullable
    private PortHit inputPortAt(double mouseX, double mouseY) {
        Circuit circuit = circuit();
        if (circuit == null) {
            return null;
        }
        for (CircuitNode node : circuit.getNodes()) {
            for (int port = 0; port < node.inputs.length; port++) {
                int x = areaToScreenX(node.x);
                int y = areaToScreenY(node.y) + inputPortOffset(port);
                if (mouseX >= x - PORT_HIT_RADIUS && mouseX <= x + PORT_HIT_RADIUS
                        && mouseY >= y - PORT_HIT_RADIUS && mouseY <= y + PORT_HIT_RADIUS) {
                    return new PortHit(node, port);
                }
            }
        }
        return null;
    }

    private record PortHit(CircuitNode node, int port) {
    }

    // Input handling

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Container slots take priority
        if (this.hoveredSlot != null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int paletteIndex = paletteButtonIndex(mouseX, mouseY);
        if (paletteIndex >= 0 && button == 0) {
            sendEdit(0, paletteIndex,
                    scrollX + (CANVAS_WIDTH - Circuit.NODE_WIDTH) / 2,
                    scrollY + (CANVAS_HEIGHT - Circuit.NODE_MIN_HEIGHT) / 2);
            return true;
        }

        if (handleConfigClick(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) {
            int[] vThumb = verticalThumbRect();
            if (inRect(mouseX, mouseY, vThumb[0] - 1, vThumb[1], vThumb[2] + 2, vThumb[3])) {
                scrollbarDrag = 1;
                return true;
            }
            int[] hThumb = horizontalThumbRect();
            if (inRect(mouseX, mouseY, hThumb[0], hThumb[1] - 1, hThumb[2], hThumb[3] + 2)) {
                scrollbarDrag = 2;
                return true;
            }
        }

        if (inCanvas(mouseX, mouseY)) {
            return handleCanvasClick(mouseX, mouseY, button);
        }
        if (button == 1 && pendingWireSource >= 0) {
            cancelWiring();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCanvasClick(double mouseX, double mouseY, int button) {
        // Input ports: disconnect (right) or finish a pending wire (left)
        PortHit input = inputPortAt(mouseX, mouseY);
        if (input != null) {
            if (button == 1) {
                if (input.node().inputs[input.port()] >= 0) {
                    sendEdit(4, input.node().id, input.port(), 0);
                }
                cancelWiring();
                return true;
            }
            if (button == 0 && pendingWireSource >= 0 && input.node().id != pendingWireSource) {
                sendEdit(3, input.node().id, input.port(), pendingWireSource);
                for (int packed : pendingWaypoints) {
                    sendEdit(7, input.node().id, input.port(), packed);
                }
                cancelWiring();
                return true;
            }
            return true;
        }

        // Output ports: start a wire
        CircuitNode output = outputPortAt(mouseX, mouseY);
        if (output != null && button == 0) {
            pendingWireSource = output.id;
            pendingWaypoints.clear();
            return true;
        }

        // Node bodies
        CircuitNode node = nodeAt(mouseX, mouseY);
        if (node != null) {
            if (button == 0) {
                selectedId = node.id;
                draggingId = node.id;
                dragOffsetX = (int) mouseX - areaToScreenX(node.x);
                dragOffsetY = (int) mouseY - areaToScreenY(node.y);
                return true;
            }
            if (button == 1) {
                if (pendingWireSource >= 0) {
                    cancelWiring();
                } else if (selectedId == node.id) {
                    selectedId = -1;
                }
                return true;
            }
            if (button == 2) {
                if (selectedId == node.id) {
                    selectedId = -1;
                }
                sendEdit(2, node.id, 0, 0);
                return true;
            }
        }

        // Blank canvas
        if (button == 0) {
            if (pendingWireSource >= 0) {
                if (pendingWaypoints.size() < Circuit.MAX_WAYPOINTS) {
                    pendingWaypoints.add((screenToAreaX(mouseX) << 8) | screenToAreaY(mouseY));
                }
                return true;
            }
            selectedId = -1;
            panning = true;
            panLastX = (int) mouseX;
            panLastY = (int) mouseY;
            return true;
        }
        if (button == 1 && pendingWireSource >= 0) {
            cancelWiring();
            return true;
        }
        return false;
    }

    private void cancelWiring() {
        pendingWireSource = -1;
        pendingWaypoints.clear();
    }

    private boolean handleConfigClick(double mouseX, double mouseY, int button) {
        Circuit circuit = circuit();
        CircuitNode selected = circuit != null && selectedId >= 0 ? circuit.nodeById(selectedId) : null;
        int x = leftPos + CONFIG_X;
        int y = topPos + CONFIG_Y;

        if (selected != null) {
            if (selected.type.isConfigurable()) {
                if (button == 0 && inRect(mouseX, mouseY, x + 4, y + 44, 12, 12)) {
                    sendEdit(5, selected.id, selected.config - 1, 0);
                    return true;
                }
                if (button == 0 && inRect(mouseX, mouseY, x + CONFIG_WIDTH - 16, y + 44, 12, 12)) {
                    sendEdit(5, selected.id, selected.config + 1, 0);
                    return true;
                }
            }
            if (button == 0 && inRect(mouseX, mouseY, x + 4, y + 64, CONFIG_WIDTH - 8, 12)) {
                selectedId = -1;
                sendEdit(2, selected.id, 0, 0);
                return true;
            }
        }

        if (button == 0 && inRect(mouseX, mouseY, x + 4, y + CONFIG_HEIGHT - 16, CONFIG_WIDTH - 8, 12)) {
            selectedId = -1;
            sendEdit(6, 0, 0, 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingId >= 0) {
            // Position is continuously re-applied in render() from the current mouse position
            return true;
        }
        if (panning) {
            scrollX -= (int) mouseX - panLastX;
            scrollY -= (int) mouseY - panLastY;
            clampScroll();
            panLastX = (int) mouseX;
            panLastY = (int) mouseY;
            return true;
        }
        if (scrollbarDrag == 1) {
            int thumbHeight = vThumbHeight();
            int range = CANVAS_HEIGHT - thumbHeight;
            if (range > 0) {
                scrollY = Mth.clamp(((int) mouseY - topPos - CANVAS_Y - thumbHeight / 2) * maxScrollY() / range, 0, maxScrollY());
            }
            return true;
        }
        if (scrollbarDrag == 2) {
            int trackWidth = CANVAS_WIDTH - SCROLLBAR_THICKNESS - 2;
            int thumbWidth = hThumbWidth();
            int range = trackWidth - thumbWidth;
            if (range > 0) {
                scrollX = Mth.clamp(((int) mouseX - leftPos - CANVAS_X - thumbWidth / 2) * maxScrollX() / range, 0, maxScrollX());
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingId >= 0 && button == 0) {
            Circuit circuit = circuit();
            CircuitNode node = circuit == null ? null : circuit.nodeById(draggingId);
            if (node != null) {
                sendEdit(1, node.id, node.x, node.y);
            }
            draggingId = -1;
            return true;
        }
        if (panning && button == 0) {
            panning = false;
            return true;
        }
        if (scrollbarDrag != 0 && button == 0) {
            scrollbarDrag = 0;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmountX, double scrollAmountY) {
        if (inCanvas(mouseX, mouseY)) {
            if (hasShiftDown()) {
                scrollX -= (int) (scrollAmountY * SCROLL_STEP);
            } else {
                scrollY -= (int) (scrollAmountY * SCROLL_STEP);
            }
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollAmountX, scrollAmountY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DELETE && selectedId >= 0) {
            sendEdit(2, selectedId, 0, 0);
            selectedId = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && pendingWireSource >= 0) {
            cancelWiring();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Tooltips

    private void renderCustomTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Circuit circuit = circuit();
        if (circuit == null || this.hoveredSlot != null) {
            return;
        }

        int paletteIndex = paletteButtonIndex(mouseX, mouseY);
        if (paletteIndex >= 0) {
            NodeType type = NodeType.byId(paletteIndex);
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("cnmcore.wrt.node." + type.name().toLowerCase()),
                    Component.translatable("cnmcore.wrt.node." + type.name().toLowerCase() + ".desc")),
                    mouseX, mouseY);
            return;
        }

        CircuitNode node = inCanvas(mouseX, mouseY) ? nodeAt(mouseX, mouseY) : null;
        if (node != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("cnmcore.wrt.node." + node.type.name().toLowerCase()));
            if (node.type == NodeType.W_IN || node.type == NodeType.W_OUT) {
                WirelessRedstoneControlTerminalBlockEntity be = blockEntity();
                if (be != null) {
                    boolean rx = node.type == NodeType.W_IN;
                    var first = be.frequencySlots.getStackInSlot(rx ? WirelessRedstoneControlTerminalBlockEntity.SLOT_RX_FIRST
                            : WirelessRedstoneControlTerminalBlockEntity.SLOT_TX_FIRST);
                    var second = be.frequencySlots.getStackInSlot(rx ? WirelessRedstoneControlTerminalBlockEntity.SLOT_RX_SECOND
                            : WirelessRedstoneControlTerminalBlockEntity.SLOT_TX_SECOND);
                    lines.add(Component.translatable("cnmcore.wrt.freq",
                            first.isEmpty() ? Component.translatable("cnmcore.wrt.freq.empty") : first.getHoverName(),
                            second.isEmpty() ? Component.translatable("cnmcore.wrt.freq.empty") : second.getHoverName()));
                }
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    // Drawing helpers

    /** Nine-slices the 48x48 box texture: the outer 1 px border stays crisp while the middle stretches. */
    private static void blitBox(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        int innerWidth = width - 2;
        int innerHeight = height - 2;
        int tex = 48;
        graphics.blit(texture, x, y, 0, 0, 1, 1, tex, tex);
        graphics.blit(texture, x + width - 1, y, tex - 1, 0, 1, 1, tex, tex);
        graphics.blit(texture, x, y + height - 1, 0, tex - 1, 1, 1, tex, tex);
        graphics.blit(texture, x + width - 1, y + height - 1, tex - 1, tex - 1, 1, 1, tex, tex);
        graphics.blit(texture, x + 1, y, 1, 0, innerWidth, 1, tex, tex);
        graphics.blit(texture, x + 1, y + height - 1, 1, tex - 1, innerWidth, 1, tex, tex);
        graphics.blit(texture, x, y + 1, 0, 1, 1, innerHeight, tex, tex);
        graphics.blit(texture, x + width - 1, y + 1, tex - 1, 1, 1, innerHeight, tex, tex);
        graphics.blit(texture, x + 1, y + 1, 1, 1, innerWidth, innerHeight, tex, tex);
    }

    // Editing

    private void sendEdit(int action, int a, int b, int c) {
        WirelessRedstoneControlTerminalBlockEntity be = blockEntity();
        if (be == null) {
            return;
        }
        PacketDistributor.sendToServer(new TerminalEditPayload(be.getBlockPos(), action, a, b, c));
    }
}
