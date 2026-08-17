package com.onehumanawa.cnmcore.foundation.data.lang;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLangProvider extends LanguageProvider {
    private final String locale;

    public ModLangProvider(PackOutput output, String locale) {
        super(output, CNMCore.ID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        if (locale.equals("en_us")) {
            generateEnUs();
        } else if (locale.equals("zh_cn")) {
            generateZhCn();
        }
    }

    private void generateEnUs() {
        generateLang("item.cnmcore.logistic_mechanism", "Logistic Mechanism");
        generateLang("item.cnmcore.fluid_mechanism", "Fluid Mechanism");

        generateLang("itemGroup.cnmcore.main", "CNM Core");

        generateLang("block.cnmcore.wireless_redstone_control_terminal", "Wireless Redstone Control Terminal");
        generateLang("cnmcore.wrt.rx", "Receive");
        generateLang("cnmcore.wrt.tx", "Transmit");
        generateLang("cnmcore.wrt.nodes", "Nodes:");
        generateLang("cnmcore.wrt.error", "Terminal block entity not found!");
        generateLang("cnmcore.wrt.hint", "Select a node");
        generateLang("cnmcore.wrt.clear", "Clear");
        generateLang("cnmcore.wrt.delete", "Delete");
        generateLang("cnmcore.wrt.tab.new", "New program");
        generateLang("cnmcore.wrt.freq", "Frequency: %s");
        generateLang("cnmcore.wrt.freq.empty", "Empty");
        generateLang("cnmcore.wrt.note.freq", "Click with a held item to copy it, right-click to clear, or drag an item in from JEI");
        generateLang("cnmcore.wrt.cfg.input_count", "Inputs");
        generateLang("cnmcore.wrt.cfg.pulse_width", "Width (tick)");
        generateLang("cnmcore.wrt.cfg.period", "Period (tick)");
        generateLang("cnmcore.wrt.cfg.delay", "Delay (tick)");
        generateLang("cnmcore.wrt.cfg.threshold", "Threshold");
        generateLang("cnmcore.wrt.cfg.mode", "Mode");
        generateLang("cnmcore.wrt.cfg.side", "Side");
        generateLang("cnmcore.wrt.cfg.value", "Strength");
        generateLang("cnmcore.wrt.mode.gt", "A > B");
        generateLang("cnmcore.wrt.mode.lt", "A < B");
        generateLang("cnmcore.wrt.mode.eq", "A = B");
        generateLang("cnmcore.wrt.side.0", "All sides");
        generateLang("cnmcore.wrt.side.1", "Down");
        generateLang("cnmcore.wrt.side.2", "Up");
        generateLang("cnmcore.wrt.side.3", "North");
        generateLang("cnmcore.wrt.side.4", "South");
        generateLang("cnmcore.wrt.side.5", "West");
        generateLang("cnmcore.wrt.side.6", "East");
        generateLang("cnmcore.wrt.note.passthrough", "Passes input through");
        generateLang("cnmcore.wrt.note.latch", "IN1: SET, IN2: RESET");

        generateLang("cnmcore.wrt.node.and", "AND Gate");
        generateLang("cnmcore.wrt.node.and.desc", "Output ON when all inputs are ON");
        generateLang("cnmcore.wrt.node.or", "OR Gate");
        generateLang("cnmcore.wrt.node.or.desc", "Output ON when any input is ON");
        generateLang("cnmcore.wrt.node.not", "NOT Gate");
        generateLang("cnmcore.wrt.node.not.desc", "Inverts the input");
        generateLang("cnmcore.wrt.node.xor", "XOR Gate");
        generateLang("cnmcore.wrt.node.xor.desc", "Output ON when an odd number of inputs are ON");
        generateLang("cnmcore.wrt.node.nand", "NAND Gate");
        generateLang("cnmcore.wrt.node.nand.desc", "Inverted AND");
        generateLang("cnmcore.wrt.node.nor", "NOR Gate");
        generateLang("cnmcore.wrt.node.nor.desc", "Inverted OR");
        generateLang("cnmcore.wrt.node.xnor", "XNOR Gate");
        generateLang("cnmcore.wrt.node.xnor.desc", "Inverted XOR");
        generateLang("cnmcore.wrt.node.pulse", "Pulse Generator");
        generateLang("cnmcore.wrt.node.pulse.desc", "Emits a short pulse on a rising edge");
        generateLang("cnmcore.wrt.node.latch", "SR Latch");
        generateLang("cnmcore.wrt.node.latch.desc", "SET latches the output ON, RESET clears it");
        generateLang("cnmcore.wrt.node.clock", "Clock Generator");
        generateLang("cnmcore.wrt.node.clock.desc", "Emits a continuous square wave");
        generateLang("cnmcore.wrt.node.delay", "Delay");
        generateLang("cnmcore.wrt.node.delay.desc", "Delays the input signal by n ticks");
        generateLang("cnmcore.wrt.node.counter", "Counter");
        generateLang("cnmcore.wrt.node.counter.desc", "Counts rising edges, outputs at the threshold");
        generateLang("cnmcore.wrt.node.compare", "Comparator");
        generateLang("cnmcore.wrt.node.compare.desc", "Compares two signal strengths");
        generateLang("cnmcore.wrt.node.input", "Redstone Input");
        generateLang("cnmcore.wrt.node.input.desc", "Reads the redstone signal feeding the block");
        generateLang("cnmcore.wrt.node.w_in", "Wireless Input");
        generateLang("cnmcore.wrt.node.w_in.desc", "Listens to its own wireless frequency");
        generateLang("cnmcore.wrt.node.output", "Redstone Output");
        generateLang("cnmcore.wrt.node.output.desc", "Emits a redstone signal to adjacent blocks");
        generateLang("cnmcore.wrt.node.w_out", "Wireless Output");
        generateLang("cnmcore.wrt.node.w_out.desc", "Transmits on its own wireless frequency");
        generateLang("cnmcore.wrt.node.const", "Constant");
        generateLang("cnmcore.wrt.node.const.desc", "Emits a fixed signal strength");
    }

    private void generateZhCn() {
        generateLang("item.cnmcore.logistic_mechanism", "物流构件");
        generateLang("item.cnmcore.fluid_mechanism", "流体构件");

        generateLang("itemGroup.cnmcore.main", "联结机构 | 核心");

        generateLang("block.cnmcore.wireless_redstone_control_terminal", "无线红石总控终端");
        generateLang("cnmcore.wrt.rx", "接收");
        generateLang("cnmcore.wrt.tx", "发射");
        generateLang("cnmcore.wrt.nodes", "节点:");
        generateLang("cnmcore.wrt.error", "未找到终端方块实体！");
        generateLang("cnmcore.wrt.hint", "选择一个节点");
        generateLang("cnmcore.wrt.clear", "清空");
        generateLang("cnmcore.wrt.delete", "删除");
        generateLang("cnmcore.wrt.tab.new", "新建程序");
        generateLang("cnmcore.wrt.freq", "频段: %s");
        generateLang("cnmcore.wrt.freq.empty", "空");
        generateLang("cnmcore.wrt.note.freq", "手持物品左键点击复制，右键清空，或直接从 JEI 拖入");
        generateLang("cnmcore.wrt.cfg.input_count", "输入数");
        generateLang("cnmcore.wrt.cfg.pulse_width", "脉宽 (tick)");
        generateLang("cnmcore.wrt.cfg.period", "周期 (tick)");
        generateLang("cnmcore.wrt.cfg.delay", "延迟 (tick)");
        generateLang("cnmcore.wrt.cfg.threshold", "阈值");
        generateLang("cnmcore.wrt.cfg.mode", "模式");
        generateLang("cnmcore.wrt.cfg.side", "输入面");
        generateLang("cnmcore.wrt.cfg.value", "强度");
        generateLang("cnmcore.wrt.mode.gt", "A > B");
        generateLang("cnmcore.wrt.mode.lt", "A < B");
        generateLang("cnmcore.wrt.mode.eq", "A = B");
        generateLang("cnmcore.wrt.side.0", "所有面");
        generateLang("cnmcore.wrt.side.1", "下");
        generateLang("cnmcore.wrt.side.2", "上");
        generateLang("cnmcore.wrt.side.3", "北");
        generateLang("cnmcore.wrt.side.4", "南");
        generateLang("cnmcore.wrt.side.5", "西");
        generateLang("cnmcore.wrt.side.6", "东");
        generateLang("cnmcore.wrt.note.passthrough", "直通输入信号");
        generateLang("cnmcore.wrt.note.latch", "输入1: 置位, 输入2: 复位");

        generateLang("cnmcore.wrt.node.and", "与门");
        generateLang("cnmcore.wrt.node.and.desc", "所有输入为 ON 时输出 ON");
        generateLang("cnmcore.wrt.node.or", "或门");
        generateLang("cnmcore.wrt.node.or.desc", "任一输入为 ON 时输出 ON");
        generateLang("cnmcore.wrt.node.not", "非门");
        generateLang("cnmcore.wrt.node.not.desc", "输入取反输出");
        generateLang("cnmcore.wrt.node.xor", "异或门");
        generateLang("cnmcore.wrt.node.xor.desc", "输入为 ON 的数量为奇数时输出 ON");
        generateLang("cnmcore.wrt.node.nand", "与非门");
        generateLang("cnmcore.wrt.node.nand.desc", "与门的反相");
        generateLang("cnmcore.wrt.node.nor", "或非门");
        generateLang("cnmcore.wrt.node.nor.desc", "或门的反相");
        generateLang("cnmcore.wrt.node.xnor", "同或门");
        generateLang("cnmcore.wrt.node.xnor.desc", "异或门的反相");
        generateLang("cnmcore.wrt.node.pulse", "脉冲发生器");
        generateLang("cnmcore.wrt.node.pulse.desc", "输入上升沿时输出一个短脉冲");
        generateLang("cnmcore.wrt.node.latch", "SR 锁存器");
        generateLang("cnmcore.wrt.node.latch.desc", "置位输入锁存输出 ON，复位输入清除");
        generateLang("cnmcore.wrt.node.clock", "时钟发生器");
        generateLang("cnmcore.wrt.node.clock.desc", "持续输出方波信号");
        generateLang("cnmcore.wrt.node.delay", "延迟器");
        generateLang("cnmcore.wrt.node.delay.desc", "输入信号延迟 n tick 后输出");
        generateLang("cnmcore.wrt.node.counter", "计数器");
        generateLang("cnmcore.wrt.node.counter.desc", "计数脉冲次数，达到阈值时输出");
        generateLang("cnmcore.wrt.node.compare", "比较器");
        generateLang("cnmcore.wrt.node.compare.desc", "比较两个输入信号强度");
        generateLang("cnmcore.wrt.node.input", "红石输入");
        generateLang("cnmcore.wrt.node.input.desc", "读取输入方块的红石信号强度");
        generateLang("cnmcore.wrt.node.w_in", "无线输入");
        generateLang("cnmcore.wrt.node.w_in.desc", "监听自己专属频段的无线红石信号");
        generateLang("cnmcore.wrt.node.output", "红石输出");
        generateLang("cnmcore.wrt.node.output.desc", "输出红石信号到相邻方块");
        generateLang("cnmcore.wrt.node.w_out", "无线输出");
        generateLang("cnmcore.wrt.node.w_out.desc", "通过自己专属的频段发送无线红石信号");
        generateLang("cnmcore.wrt.node.const", "常量");
        generateLang("cnmcore.wrt.node.const.desc", "输出固定的信号强度");
    }

    private void generateLang(String key, String value) {
        add(key, value);
    }
}