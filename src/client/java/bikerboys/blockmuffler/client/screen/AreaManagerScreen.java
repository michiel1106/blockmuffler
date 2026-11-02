package bikerboys.blockmuffler.client.screen;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.config.*;
import bikerboys.blockmuffler.client.screen.widget.*;
import eu.midnightdust.lib.config.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import static net.minecraft.util.ARGB.colorFromFloat;

import java.util.*;

public class AreaManagerScreen extends Screen {
    private static final int ROW_HEIGHT = 28;
    private static final int ROW_GAP = 4;
    private static final int PADDING = 4;
    private static final int LIST_INSET = 8;
    private static final int SCROLLBAR_WIDTH = MenuStyle.SCROLLBAR_WIDTH;
    private static final double SCROLL_SPEED = 18.0;

    private static final int MIN_INFO_WIDTH = 140;
    private static final int SEPARATOR_THICKNESS = 2;

    private static final int TITLE_Y = 8;
    private static final int SEARCH_Y = 22;
    private static final int SEARCH_HEIGHT = 20;
    private static final int SEARCH_MAX_WIDTH = 300;
    private static final int HEADER_HEIGHT = 48;
    private static final int FOOTER_HEIGHT = 33;
    private static final int BUTTON_HEIGHT = 20;
    private static final int DONE_WIDTH = 150;
    private static final int EDIT_BUTTON_WIDTH = 90;
    private static final int EDIT_BUTTON_GAP = 4;
    private static final int RENDER_BUTTON_WIDTH = 90;

    private static final int DEFAULT_SIZE = 1;
    private static final int[] AREA_COLORS = {colorFromFloat(0.5f, 0, 1, 0), colorFromFloat(0.5f, 0, 1, 1), ARGB.colorFromFloat(0.5f, 1, 1, 0), ARGB.colorFromFloat(0.5f, 1, 0, 0), ARGB.colorFromFloat(0.5f, 1, 0, 1), ARGB.colorFromFloat(0.5f, 1, 1, 1)};

    private final List<AreaWidget> allWidgets = new ArrayList<>();
    private final List<AreaWidget> shownWidgets = new ArrayList<>();

    private final AreaInfoPanel infoPanel = new AreaInfoPanel();

    private EditBox searchBar;
    private Button editModeButton;
    private Button renderButton;
    private String searchText = "";

    private int listLeft, listTop, listRight, listBottom, infoLeft;

    private double scrollTarget = 0;
    private double scrollCurrent = 0;
    private long lastFrameNanos = 0;

    private final Set<Area> placementOn = Collections.newSetFromMap(new IdentityHashMap<>());

    private Area pendingDelete;

    private final AreaWidget.Listener areaListener = new AreaWidget.Listener() {
        @Override
        public void onConfigure(Area area) {
            Minecraft.getInstance().setScreenAndShow(new AreaConfigScreen(AreaManagerScreen.this, area));
        }

        @Override
        public void onTogglePlacement(Area area) {
            area.toggleRenderPlacement();
        }

        @Override
        public void onDelete(Area area) {
            pendingDelete = area;
        }

        @Override
        public void onSelect(Area area) {
            AreaManager.selectedArea = area;
        }

        @Override
        public void onToggleEnabled(Area area) {
            area.toggleEnabled();
        }
    };

    public AreaManagerScreen() {
        super(Component.literal("Area Manager"));
    }

    @Override
    protected void init() {
        super.init();

        int infoWidth = Math.max(MIN_INFO_WIDTH, (int) (width * 0.15f));
        infoLeft = width - infoWidth;

        listLeft = 0;
        listRight = infoLeft - SEPARATOR_THICKNESS;
        listTop = HEADER_HEIGHT;
        listBottom = height - FOOTER_HEIGHT;

        int columnCenter = (listLeft + listRight) / 2;
        int buttonsWidth = EDIT_BUTTON_WIDTH + RENDER_BUTTON_WIDTH + EDIT_BUTTON_GAP * 2;
        int searchWidth = Math.min(listRight - listLeft - LIST_INSET * 2 - buttonsWidth, SEARCH_MAX_WIDTH);
        int searchX = columnCenter - (searchWidth + buttonsWidth) / 2;
        int editX = searchX + searchWidth + EDIT_BUTTON_GAP;
        int renderX = editX + EDIT_BUTTON_WIDTH + EDIT_BUTTON_GAP;
        int buttonY = listBottom + SEPARATOR_THICKNESS + (FOOTER_HEIGHT - SEPARATOR_THICKNESS - BUTTON_HEIGHT) / 2;
        int doneWidth = Math.min(DONE_WIDTH, listRight - listLeft - LIST_INSET * 2);

        infoPanel.setBounds(infoLeft, 0, width, listBottom);
        infoPanel.init(font, this::addWidget);

        addRenderableWidget(new PanelButton(infoLeft + LIST_INSET, buttonY, infoWidth - LIST_INSET * 2, BUTTON_HEIGHT, Component.literal("Create new area"), this::createArea));
        addRenderableWidget(new PanelButton(columnCenter - doneWidth / 2, buttonY, doneWidth, BUTTON_HEIGHT, Component.literal("Done"), this::onClose));

        searchBar = new EditBox(font, searchX, SEARCH_Y, searchWidth, SEARCH_HEIGHT, Component.literal("Area Searchbox"));
        searchBar.setHint(Component.literal("Search areas..."));
        searchBar.setValue(searchText);
        searchBar.setResponder(s -> {
            searchText = s;
            applyFilter();
            scrollTarget = 0;
            scrollCurrent = 0;
        });

        editModeButton = Button.builder(editModeLabel(), this::onPressToggleButton)
                .bounds(editX, SEARCH_Y, EDIT_BUTTON_WIDTH, SEARCH_HEIGHT)
                .build();

        renderButton = Button.builder(renderLabel(), this::onPressRenderButton)
                .bounds(renderX, SEARCH_Y, RENDER_BUTTON_WIDTH, SEARCH_HEIGHT)
                .build();

        addRenderableWidget(searchBar);
        addRenderableWidget(editModeButton);
        addRenderableWidget(renderButton);
        setInitialFocus(searchBar);

        buildWidgets();
    }


    private Component renderLabel() {
        return Component.literal("Rendering: ")
                .append(BlockMufflerConfig.rendering
                        ? Component.literal("ON").withColor(TextColor.GREEN)
                        : Component.literal("OFF").withColor(TextColor.RED));
    }

    private void onPressRenderButton(Button button) {
        BlockMufflerConfig.rendering = !BlockMufflerConfig.rendering;
        MidnightConfig.write(BlockMuffler.MOD_ID);
        button.setMessage(renderLabel());
    }

    private Component editModeLabel() {
        return Component.literal("Editmode: ")
                .append(BlockMufflerConfig.editMode
                        ? Component.literal("ON").withColor(TextColor.GREEN)
                        : Component.literal("OFF").withColor(TextColor.RED));
    }

    private void onPressToggleButton(Button button) {
        BlockMufflerConfig.editMode = !BlockMufflerConfig.editMode;
        MidnightConfig.write(BlockMuffler.MOD_ID);
        button.setMessage(editModeLabel());
    }

    private void createArea() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        BlockPos origin = player.blockPosition();
        BlockPos opposite = origin.offset(DEFAULT_SIZE - 1, DEFAULT_SIZE - 1, DEFAULT_SIZE - 1);
        Area area = new Area(origin, opposite, AREA_COLORS[AreaManager.areaList.size() % AREA_COLORS.length], nextAreaName());
        AreaManager.areaList.add(area);

        AreaManager.selectedArea = area;
        searchBar.setValue("");
        buildWidgets();
        scrollTarget = maxScroll();
    }

    private String nextAreaName() {
        Set<String> used = new HashSet<>();
        for (Area existing : AreaManager.areaList) {
            used.add(existing.getName());
        }

        int number = 1;
        while (used.contains("Area " + number)) {
            number++;
        }
        return "Area " + number;
    }

    private void buildWidgets() {
        for (AreaWidget widget : shownWidgets) {
            removeWidget(widget);
        }
        shownWidgets.clear();
        allWidgets.clear();

        for (Area area : new ArrayList<>(AreaManager.areaList)) {
            allWidgets.add(new AreaWidget(area, 0, 0, 0, ROW_HEIGHT, areaListener));
        }
        applyFilter();
    }

    private void applyFilter() {
        for (AreaWidget widget : shownWidgets) {
            removeWidget(widget);
        }
        shownWidgets.clear();

        String query = searchText.trim().toLowerCase(Locale.ROOT);
        for (AreaWidget widget : allWidgets) {
            if (query.isEmpty() || widget.getArea().getName().toLowerCase(Locale.ROOT).contains(query)) {
                shownWidgets.add(widget);
                addWidget(widget);
            }
        }
        clampScroll();
    }

    private void processPendingDelete() {
        if (pendingDelete == null) return;

        AreaManager.areaList.remove(pendingDelete);
        placementOn.remove(pendingDelete);
        if (AreaManager.selectedArea == pendingDelete) {
            AreaManager.selectedArea = null;
        }
        pendingDelete = null;

        buildWidgets();
    }

    private int contentHeight() {
        int rows = shownWidgets.size();
        if (rows == 0) return 0;
        return rows * ROW_HEIGHT + (rows - 1) * ROW_GAP + PADDING * 2;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (listBottom - listTop));
    }

    private void clampScroll() {
        double max = maxScroll();
        scrollTarget = Math.max(0, Math.min(scrollTarget, max));
        scrollCurrent = Math.max(0, Math.min(scrollCurrent, max));
    }

    private void updateSmoothScroll() {
        long now = System.nanoTime();
        double dtMs = lastFrameNanos == 0 ? 16.0 : Math.min(100.0, (now - lastFrameNanos) / 1_000_000.0);
        lastFrameNanos = now;

        double diff = scrollTarget - scrollCurrent;
        if (Math.abs(diff) < 0.5) {
            scrollCurrent = scrollTarget;
        } else {
            scrollCurrent += diff * (1.0 - Math.exp(-dtMs / 60.0));
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        boolean overList = x >= listLeft && x <= listRight && y >= listTop && y <= listBottom;
        if (overList && maxScroll() > 0) {
            scrollTarget = Math.max(0, Math.min(scrollTarget - scrollY * SCROLL_SPEED, maxScroll()));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    public void renderAreaWidgets(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int max = maxScroll();
        int rowWidth = listRight - listLeft - LIST_INSET * 2 - (max > 0 ? SCROLLBAR_WIDTH + 2 : 0);
        int originY = listTop + PADDING - (int) Math.round(scrollCurrent);

        g.enableScissor(listLeft, listTop, listRight, listBottom);

        for (int i = 0; i < shownWidgets.size(); i++) {
            AreaWidget widget = shownWidgets.get(i);
            int y = originY + i * (ROW_HEIGHT + ROW_GAP);

            widget.setX(listLeft + LIST_INSET);
            widget.setY(y);
            widget.setWidth(rowWidth);
            widget.setClip(listTop, listBottom);
            widget.setSelected(widget.getArea() == AreaManager.selectedArea);

            if (y + ROW_HEIGHT < listTop || y > listBottom) continue;
            widget.extractRenderState(g, mouseX, mouseY, a);
        }

        if (shownWidgets.isEmpty()) {
            Component empty = Component.literal(allWidgets.isEmpty() ? "No areas yet" : "No areas match your search");
            MenuStyle.centeredText(g, font, empty, (listLeft + listRight) / 2, listTop + 12, MenuStyle.EMPTY_COLOR);
        }

        if (max > 0) {
            MenuStyle.scrollbar(g, listRight - SCROLLBAR_WIDTH - 2, listTop, listBottom, listBottom - listTop + max, scrollCurrent, max);
        }

        g.disableScissor();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        processPendingDelete();
        super.extractRenderState(g, mouseX, mouseY, a);
        updateSmoothScroll();
        renderAreaWidgets(g, mouseX, mouseY, a);
        infoPanel.render(g, mouseX, mouseY, a, AreaManager.selectedArea);
        MenuStyle.centeredText(g, font, title, (listLeft + listRight) / 2, TITLE_Y, MenuStyle.TEXT_COLOR);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        MenuStyle.body(g, listLeft, listTop, listRight, listBottom);
        MenuStyle.body(g, infoLeft, 0, width, listBottom);
        MenuStyle.verticalSeparator(g, listRight, 0, listBottom);
        MenuStyle.headerSeparator(g, listLeft, listRight, listTop - SEPARATOR_THICKNESS);
        MenuStyle.footerSeparator(g, 0, width, listBottom);
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}