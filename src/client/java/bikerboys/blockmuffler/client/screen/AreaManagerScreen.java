package bikerboys.blockmuffler.client.screen;

import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.screen.widget.*;
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
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_GAP = 4;
    private static final int PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final double SCROLL_SPEED = 18.0;

    private static final int MIN_INFO_WIDTH = 140;
    private static final int PANEL_GAP = 4;

    private static final int CREATE_HEIGHT = 20;
    private static final int BOTTOM_MARGIN = 3;
    private static final int DEFAULT_SIZE = 1;
    private static final int[] AREA_COLORS = {colorFromFloat(0.5f, 0, 1, 0), colorFromFloat(0.5f, 0, 1, 1), ARGB.colorFromFloat(0.5f, 1, 1, 0), ARGB.colorFromFloat(0.5f, 1, 0, 0), ARGB.colorFromFloat(0.5f, 1, 0, 1), ARGB.colorFromFloat(0.5f, 1, 1, 1wwww)};

    private static final int PANEL_COLOR = 0xC0101010;
    private static final int SCROLL_TRACK_COLOR = 0x30FFFFFF;
    private static final int SCROLL_THUMB_COLOR = 0xFFA0A0A0;

    private final List<AreaWidget> allWidgets = new ArrayList<>();
    private final List<AreaWidget> shownWidgets = new ArrayList<>();

    private final AreaInfoPanel infoPanel = new AreaInfoPanel();

    private EditBox searchBar;
    private String searchText = "";

    private int listLeft, listTop, listRight, listBottom;

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
    };

    public AreaManagerScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        super.init();

        int margin = (int) (width * 0.06f);
        int searchY = (int) (height * 0.012f);
        int searchHeight = Math.max(20, (int) (height * 0.045f));
        int infoWidth = Math.max(MIN_INFO_WIDTH, (int) (width * 0.15f));
        int createY = height - BOTTOM_MARGIN - CREATE_HEIGHT;

        listLeft = margin;
        listRight = width - margin - infoWidth - PANEL_GAP;
        listTop = searchY + searchHeight + 4;
        listBottom = createY - 4;

        int infoLeft = listRight + PANEL_GAP;
        int infoRight = width - margin;

        infoPanel.setBounds(infoLeft, searchY, infoRight, listBottom);
        infoPanel.init(font, this::addWidget);

        addRenderableWidget(new PanelButton(infoLeft, createY, infoRight - infoLeft, CREATE_HEIGHT, Component.literal("Create new area"), this::createArea));

        searchBar = new EditBox(font, margin, searchY, listRight - listLeft, searchHeight, Component.literal("Area Searchbox"));
        searchBar.setValue(searchText);
        searchBar.setResponder(s -> {
            searchText = s;
            applyFilter();
            scrollTarget = 0;
            scrollCurrent = 0;
        });
        addRenderableWidget(searchBar);
        setInitialFocus(searchBar);

        buildWidgets();
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
        int rowWidth = listRight - listLeft - PADDING * 2 - (max > 0 ? SCROLLBAR_WIDTH + 2 : 0);
        int originY = listTop + PADDING - (int) Math.round(scrollCurrent);

        g.enableScissor(listLeft, listTop, listRight, listBottom);

        for (int i = 0; i < shownWidgets.size(); i++) {
            AreaWidget widget = shownWidgets.get(i);
            int y = originY + i * (ROW_HEIGHT + ROW_GAP);

            widget.setX(listLeft + PADDING);
            widget.setY(y);
            widget.setWidth(rowWidth);
            widget.setClip(listTop, listBottom);
            widget.setSelected(widget.getArea() == AreaManager.selectedArea);

            if (y + ROW_HEIGHT < listTop || y > listBottom) continue;
            widget.extractRenderState(g, mouseX, mouseY, a);
        }

        if (shownWidgets.isEmpty()) {
            Component empty = Component.literal(allWidgets.isEmpty() ? "No areas yet" : "No areas match your search");
            int textX = (listLeft + listRight - font.width(empty)) / 2;
            g.text(font, empty, textX, listTop + 12, 0xFF909090);
        }

        if (max > 0) {
            int viewport = listBottom - listTop;
            int thumbHeight = Math.max(16, viewport * viewport / (viewport + max));
            int trackX = listRight - SCROLLBAR_WIDTH - 2;
            int thumbY = listTop + (int) ((viewport - thumbHeight) * (scrollCurrent / max));

            g.fill(trackX, listTop, trackX + SCROLLBAR_WIDTH, listBottom, SCROLL_TRACK_COLOR);
            g.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLL_THUMB_COLOR);
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
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.fill(listLeft, listTop, listRight, listBottom, PANEL_COLOR);
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}