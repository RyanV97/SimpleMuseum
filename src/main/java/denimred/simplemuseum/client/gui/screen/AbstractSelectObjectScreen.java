package denimred.simplemuseum.client.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.Collection;

import javax.annotation.Nullable;

public abstract class AbstractSelectObjectScreen<T> extends Screen {
    protected final Minecraft mc = Minecraft.getInstance();
    protected final Screen parent;
    @Nullable protected ListWidget.Entry selected;
    protected ListWidget list;
    protected TextFieldWidget search;
    protected String lastSearchText = "";

    protected AbstractSelectObjectScreen(Screen parent, ITextComponent title) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        mc.keyboardListener.enableRepeatEvents(true);

        final int margin = 10;
        final int third = width / 3;
        final int bottom = height - margin;
        final int top = (margin * 2) + font.FONT_HEIGHT;
        list = new ListWidget(top, bottom, third * 2);

        final int remainingWidth = third - (margin * 3);
        final int remainingX = width - margin - remainingWidth;

        search =
                new TextFieldWidget(
                        font,
                        remainingX,
                        top + font.FONT_HEIGHT + 2,
                        remainingWidth,
                        20,
                        new StringTextComponent("Search"));

        this.addButton(
                new Button(
                        remainingX,
                        bottom - 20 - (20 + (margin / 2)),
                        remainingWidth,
                        20,
                        DialogTexts.GUI_DONE,
                        b -> this.saveAndClose()));
        this.addButton(
                new Button(
                        remainingX,
                        bottom - 20,
                        remainingWidth,
                        20,
                        DialogTexts.GUI_CANCEL,
                        b -> this.closeScreen()));

        search.setResponder(
                s -> {
                    if (!s.equals(lastSearchText)) {
                        lastSearchText = s;
                        list.refreshList();
                        selected = null;
                        list.setSelected(null);
                    }
                });
        children.add(search);

        list.setLeftPos(margin);
        list.func_244605_b(false);
        list.func_244606_c(false);
        list.getEventListeners().stream()
                .filter(this::isSelected)
                .findFirst()
                .ifPresent(
                        entry -> {
                            selected = entry;
                            list.setSelected(entry);
                        });
        children.add(list);
    }

    @Override
    public void tick() {
        search.tick();
        list.setSelected(selected);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        drawCenteredString(
                matrixStack,
                font,
                title.copyRaw().mergeStyle(TextFormatting.UNDERLINE),
                width / 2,
                10,
                0xFFFFFF);
        final double scale = mc.getMainWindow().getGuiScaleFactor();
        RenderSystem.enableScissor(
                (int) (list.getLeft() * scale),
                (int) ((height - list.getBottom()) * scale),
                (int) (list.getWidth() * scale),
                (int) (list.getHeight() * scale));
        list.render(matrixStack, mouseX, mouseY, partialTicks);
        RenderSystem.disableScissor();
        drawString(
                matrixStack,
                font,
                search.getMessage(),
                search.x,
                search.y - font.FONT_HEIGHT - 2,
                0xA0A0A0);
        search.render(matrixStack, mouseX, mouseY, partialTicks);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    protected void saveAndClose() {
        this.onSave();
        this.closeScreen();
    }

    @Override
    public void closeScreen() {
        mc.displayGuiScreen(parent);
    }

    @Override
    public void onClose() {
        mc.keyboardListener.enableRepeatEvents(false);
    }

    public void setSelected(ListWidget.Entry entry) {
        selected = (entry == selected ? null : entry);
    }

    protected abstract void onSave();

    protected abstract boolean isSelected(ListWidget.Entry entry);

    protected abstract Collection<T> getEntries();

    protected boolean matchesSearch(T value) {
        if (!lastSearchText.isEmpty()) {
            return value.toString().contains(lastSearchText);
        }
        return true;
    }

    protected class ListWidget extends ExtendedList<ListWidget.Entry> {
        protected final int listWidth;

        public ListWidget(int top, int bottom, int width) {
            super(
                    mc,
                    width,
                    bottom - top,
                    top,
                    bottom,
                    AbstractSelectObjectScreen.this.font.FONT_HEIGHT + 6);
            this.listWidth = width;
            this.refreshList();
        }

        @Override
        protected int getScrollbarPosition() {
            return listWidth + getLeft() - 6;
        }

        @Override
        public int getRowWidth() {
            return listWidth;
        }

        public void refreshList() {
            this.clearEntries();
            for (T value : AbstractSelectObjectScreen.this.getEntries()) {
                if (AbstractSelectObjectScreen.this.matchesSearch(value)) {
                    this.addEntry(new Entry(value));
                }
            }
        }

        @Override
        protected void renderBackground(MatrixStack matrixStack) {
            AbstractSelectObjectScreen.this.fillGradient(
                    matrixStack, x0, y0, x1, y1, 0xc0101010, 0xd0101010);
        }

        protected class Entry extends ExtendedList.AbstractListEntry<Entry> {
            protected final T value;

            public Entry(T value) {
                this.value = value;
            }

            @Override
            public void render(
                    MatrixStack matrixStack,
                    int id,
                    int top,
                    int left,
                    int width,
                    int height,
                    int mouseX,
                    int mouseY,
                    boolean hovered,
                    float partialTicks) {
                final ITextComponent name = new StringTextComponent(value.toString());
                final FontRenderer font = AbstractSelectObjectScreen.this.font;
                font.func_238422_b_(
                        matrixStack,
                        LanguageMap.getInstance()
                                .func_241870_a(
                                        ITextProperties.func_240655_a_(
                                                font.func_238417_a_(name, listWidth))),
                        left + 3,
                        top + 3,
                        0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(
                    double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
                AbstractSelectObjectScreen.this.setSelected(this);
                ListWidget.this.setSelected(this);
                return false;
            }
        }
    }
}