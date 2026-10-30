package bikerboys.blockmuffler.client.screen;

import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.areas.rules.*;
import bikerboys.blockmuffler.client.screen.widget.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.sounds.SoundSource;

import java.util.*;

public class AreaConfigScreen extends Screen {
    private record RuleView(SoundRule rule, int y) {
    }

    private static final int OUTER_PAD = 12;
    private static final int COLUMN_GAP = 12;
    private static final int MAX_COLUMN_WIDTH = 240;
    private static final int TITLE_Y = 8;
    private static final int TOP_BAR = 26;
    private static final int FOOTER_HEIGHT = 33;
    private static final int SEPARATOR_THICKNESS = 2;
    private static final int HEADER_HEIGHT = 12;
    private static final int WIDGET_HEIGHT = 20;
    private static final int WIDGET_GAP = 2;
    private static final int GRID_GAP = 4;
    private static final int SUGGESTION_COUNT = 5;
    private static final int SUGGESTION_HEIGHT = 16;
    private static final int RULE_LINE_HEIGHT = 14;
    private static final int RULE_SLIDER_HEIGHT = 18;
    private static final int RULE_ROW_HEIGHT = 36;
    private static final int REMOVE_WIDTH = 50;
    private static final int MUTE_WIDTH = 56;
    private static final int ADD_WIDTH = 40;
    private static final int PAGER_BUTTON_WIDTH = 20;
    private static final int DONE_WIDTH = 200;
    private static final int NAME_LABEL_WIDTH = 44;

    private static final int HEADER_COLOR = 0xFFA0A0A0;
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int SUCCESS_COLOR = 0xFF55FF55;

    private static final Component GENERAL_HEADER = Component.literal("General");
    private static final Component CATEGORIES_HEADER = Component.literal("Sound Categories");
    private static final Component COLOR_HEADER = Component.literal("Area Color");
    private static final Component NO_RULES = Component.literal("No sound rules yet");
    private static final Component INPUT_HINT = Component.literal("e.g. entity.zombie.* or *");
    private static final Component INCLUDE_TOOLTIP = Component.literal("If disabled it wont block entity sounds.");

    private static List<String> soundIds;

    private final Screen parent;
    private final Area area;
    private final AreaRule areaRule;

    private final List<AbstractWidget> drawList = new ArrayList<>();
    private final List<PanelButton> suggestionButtons = new ArrayList<>();
    private final List<String> suggestionIds = new ArrayList<>();
    private final List<RuleView> ruleViews = new ArrayList<>();

    private EditBox soundInput;
    private PanelButton includeButton;
    private PanelButton prevButton;
    private PanelButton nextButton;
    private EditBox nameInput;
    private int nameLabelY;

    private String inputText = "";
    private String statusText = "";
    private boolean statusError;
    private int page;
    private int pageCount = 1;
    private boolean needsRebuild;

    private int leftX, rightX, colWidth;
    private int bodyTop, bodyBottom;
    private int generalHeaderY, categoriesHeaderY, colorHeaderY, rightHeaderY;
    private int statusY, rulesTop, pagerY;

    public AreaConfigScreen(Screen parent, Area area) {
        super(Component.literal("Configure area"));
        this.parent = parent;
        this.area = area;
        this.areaRule = area.getAreaRule();
    }

    @Override
    protected void init() {
        super.init();

        drawList.clear();
        suggestionButtons.clear();
        suggestionIds.clear();
        ruleViews.clear();

        colWidth = Math.min(MAX_COLUMN_WIDTH, (width - OUTER_PAD * 2 - COLUMN_GAP) / 2);
        leftX = (width - (colWidth * 2 + COLUMN_GAP)) / 2;
        rightX = leftX + colWidth + COLUMN_GAP;

        bodyTop = TOP_BAR;
        bodyBottom = height - FOOTER_HEIGHT;
        int doneY = bodyBottom + SEPARATOR_THICKNESS + (FOOTER_HEIGHT - SEPARATOR_THICKNESS - WIDGET_HEIGHT) / 2;

        int rightY = bodyTop + 6;
        rightHeaderY = rightY;
        rightY += HEADER_HEIGHT;
        int inputY = rightY;
        rightY += WIDGET_HEIGHT + WIDGET_GAP;
        statusY = rightY;
        rightY += 11;
        rulesTop = rightY;
        pagerY = bodyBottom - WIDGET_HEIGHT - 4;
        int rulesBottom = pagerY - 4;
        int rowsPerPage = Math.max(1, (rulesBottom - rulesTop) / RULE_ROW_HEIGHT);

        for (int i = 0; i < SUGGESTION_COUNT; i++) {
            int index = i;
            PanelButton button = new PanelButton(rightX, inputY + WIDGET_HEIGHT + 2 + i * SUGGESTION_HEIGHT, colWidth, SUGGESTION_HEIGHT, Component.empty(), () -> pickSuggestion(index));
            button.visible = false;
            suggestionButtons.add(button);
            addWidget(button);
        }

        int leftY = bodyTop + 6;
        generalHeaderY = leftY;
        leftY += HEADER_HEIGHT;

        nameLabelY = leftY + (WIDGET_HEIGHT - font.lineHeight) / 2;

        nameInput = new EditBox(
                font,
                leftX + NAME_LABEL_WIDTH,
                leftY,
                colWidth - NAME_LABEL_WIDTH,
                WIDGET_HEIGHT,
                Component.literal("Area name")
        );

        nameInput.setMaxLength(48);
        nameInput.setHint(Component.literal("Enter area name"));
        nameInput.setValue(area.getName() == null ? "" : area.getName());

        nameInput.setResponder(area::setName);

        drawList.add(nameInput);

        leftY += WIDGET_HEIGHT + WIDGET_GAP;


        drawList.add(new ValueSlider(leftX, leftY, colWidth, WIDGET_HEIGHT, "Volume multiplier", areaRule.getVolMultiplier(), v -> areaRule.setVolMultiplier((float) v)));
        leftY += WIDGET_HEIGHT + WIDGET_GAP;

        includeButton = new PanelButton(leftX, leftY, colWidth, WIDGET_HEIGHT, includeLabel(), () -> {
            areaRule.setIncludeEntities(!areaRule.isIncludeEntities());
            includeButton.setMessage(includeLabel());
        });
        includeButton.setTooltip(Tooltip.create(INCLUDE_TOOLTIP));
        drawList.add(includeButton);
        leftY += WIDGET_HEIGHT + WIDGET_GAP + 6;

        categoriesHeaderY = leftY;
        leftY += HEADER_HEIGHT;

        int halfWidth = (colWidth - GRID_GAP) / 2;
        int gridIndex = 0;
        for (SoundSource source : SoundSource.values()) {
            if (source == SoundSource.MASTER) continue;

            int column = gridIndex % 2;
            int row = gridIndex / 2;
            int x = leftX + column * (halfWidth + GRID_GAP);
            int y = leftY + row * (WIDGET_HEIGHT + WIDGET_GAP);

            drawList.add(new ValueSlider(x, y, halfWidth, WIDGET_HEIGHT, capitalize(source.getName()), areaRule.getCategoryMultiplier(source), v -> areaRule.setCategoryMultiplier(source, (float) v)));
            gridIndex++;
        }

        int categoryRows = (gridIndex + 1) / 2;

        colorHeaderY = leftY + categoryRows * (WIDGET_HEIGHT + WIDGET_GAP) + 4;
        leftY = colorHeaderY + HEADER_HEIGHT;

        int color = area.getColor();
        String[] channelNames = {"Red", "Green", "Blue"};
        int[] shifts = {16, 8, 0};
        int channelWidth = (colWidth - GRID_GAP * 2) / 3;
        for (int i = 0; i < 3; i++) {
            int shift = shifts[i];
            int x = leftX + i * (channelWidth + GRID_GAP);
            int w = i == 2 ? leftX + colWidth - x : channelWidth;

            drawList.add(new ValueSlider(
                    x, leftY, w, WIDGET_HEIGHT,
                    channelNames[i],
                    ((color >> shift) & 0xFF) / 255.0,
                    255, "",
                    v -> setColorChannel(shift, v)
            ));
        }

        soundInput = new EditBox(font, rightX, inputY, colWidth - ADD_WIDTH - 2, WIDGET_HEIGHT, Component.literal("Sound id"));
        soundInput.setMaxLength(128);
        soundInput.setHint(INPUT_HINT);
        soundInput.setValue(inputText);
        soundInput.setResponder(s -> {
            inputText = s;
            statusText = "";
            refreshSuggestions();
        });
        drawList.add(soundInput);

        drawList.add(new PanelButton(rightX + colWidth - ADD_WIDTH, inputY, ADD_WIDTH, WIDGET_HEIGHT, Component.literal("Add"), this::addRule));

        List<SoundRule> rules = areaRule.getSoundRules();
        pageCount = Math.max(1, (rules.size() + rowsPerPage - 1) / rowsPerPage);
        page = Math.max(0, Math.min(page, pageCount - 1));

        int start = page * rowsPerPage;
        int end = Math.min(rules.size(), start + rowsPerPage);
        for (int i = start; i < end; i++) {
            SoundRule soundRule = rules.get(i);
            int y = rulesTop + (i - start) * RULE_ROW_HEIGHT;
            ruleViews.add(new RuleView(soundRule, y));

            drawList.add(new PanelButton(rightX + colWidth - REMOVE_WIDTH, y, REMOVE_WIDTH, RULE_LINE_HEIGHT, Component.literal("Remove"), () -> removeRule(soundRule)));

            int sliderY = y + RULE_LINE_HEIGHT + 2;
            drawList.add(new ValueSlider(rightX, sliderY, colWidth - MUTE_WIDTH - 2, RULE_SLIDER_HEIGHT, "Volume", soundRule.getVolumeMul(), v -> soundRule.setVolumeMul((float) v)));

            PanelButton[] holder = new PanelButton[1];
            holder[0] = new PanelButton(rightX + colWidth - MUTE_WIDTH, sliderY, MUTE_WIDTH, RULE_SLIDER_HEIGHT, muteLabel(soundRule), () -> {
                soundRule.setBlocked(!soundRule.blocked);
                holder[0].setMessage(muteLabel(soundRule));
            });
            drawList.add(holder[0]);
        }

        prevButton = new PanelButton(rightX, pagerY, PAGER_BUTTON_WIDTH, WIDGET_HEIGHT, Component.literal("<"), () -> changePage(-1));
        prevButton.active = page > 0;
        drawList.add(prevButton);

        nextButton = new PanelButton(rightX + colWidth - PAGER_BUTTON_WIDTH, pagerY, PAGER_BUTTON_WIDTH, WIDGET_HEIGHT, Component.literal(">"), () -> changePage(1));
        nextButton.active = page < pageCount - 1;
        drawList.add(nextButton);

        int doneWidth = Math.min(DONE_WIDTH, width - OUTER_PAD * 2);
        drawList.add(new PanelButton((width - doneWidth) / 2, doneY, doneWidth, WIDGET_HEIGHT, Component.literal("Done"), this::onClose));

        for (AbstractWidget widget : drawList) {
            addWidget(widget);
        }

        refreshSuggestions();
    }

    private Component includeLabel() {
        return Component.literal("Include entities: " + (areaRule.isIncludeEntities() ? "ON" : "OFF"));
    }

    private Component muteLabel(SoundRule soundRule) {
        return Component.literal("Mute: " + (soundRule.blocked ? "ON" : "OFF"));
    }

    private static String capitalize(String name) {
        if (name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    private static List<String> allSoundIds() {
        if (soundIds == null) {
            List<String> ids = new ArrayList<>();
            for (var key : BuiltInRegistries.SOUND_EVENT.keySet()) {
                ids.add(key.toString());
            }
            Collections.sort(ids);
            soundIds = ids;
        }
        return soundIds;
    }

    private void setColorChannel(int shift, double value) {
        int color = area.getColor();
        int channel = (int) Math.round(value * 255);

        int mask = ~(0xFF << shift);
        color = (color & mask) | (channel << shift);

        area.setColor(color);
    }

    private static String normalizeId(String raw) {
        String id = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\*+", "*");
        if (id.isEmpty()) return null;
        if (!id.contains(":") && !id.startsWith("*")) {
            id = "minecraft:" + id;
        }
        return isValidPattern(id) ? id : null;
    }

    private static boolean isValidPattern(String id) {
        if (!id.matches("[a-z0-9_.:*/-]+")) return false;
        int colon = id.indexOf(':');
        return colon == id.lastIndexOf(':') && colon != 0 && colon != id.length() - 1;
    }

    private void showWildcardMatches(String query) {
        String pattern = normalizeId(query);
        if (pattern == null) return;

        int count = 0;
        for (String id : allSoundIds()) {
            if (SoundRule.matches(pattern, id)) {
                count++;
            }
        }

        statusError = count == 0;
        statusText = count == 0 ? "Matches no known sounds" : "Matches " + count + (count == 1 ? " sound" : " sounds");
    }

    private void refreshSuggestions() {
        suggestionIds.clear();

        String query = inputText.trim().toLowerCase(Locale.ROOT);
        if (query.indexOf('*') >= 0) {
            showWildcardMatches(query);
        } else if (!query.isEmpty()) {
            for (String id : allSoundIds()) {
                if (id.contains(query) && areaRule.getSoundRule(id) == null) {
                    suggestionIds.add(id);
                    if (suggestionIds.size() >= SUGGESTION_COUNT) break;
                }
            }
        }

        if (suggestionIds.size() == 1 && suggestionIds.get(0).equals(query)) {
            suggestionIds.clear();
        }

        for (int i = 0; i < suggestionButtons.size(); i++) {
            PanelButton button = suggestionButtons.get(i);
            boolean show = i < suggestionIds.size();
            button.visible = show;
            if (show) {
                button.setMessage(fit(suggestionIds.get(i), colWidth - 10));
            }
        }
    }

    private void pickSuggestion(int index) {
        if (index < 0 || index >= suggestionIds.size()) return;
        soundInput.setValue(suggestionIds.get(index));
    }

    private void addRule() {
        String id = normalizeId(soundInput.getValue());
        if (id == null) {
            setStatus("Invalid sound id", true);
            return;
        }

        if (!areaRule.addSoundRule(new SoundRule(id))) {
            setStatus("A rule for that sound already exists", true);
            return;
        }

        inputText = "";
        page = Integer.MAX_VALUE;
        setStatus("Added " + id, false);
        needsRebuild = true;
    }

    private void removeRule(SoundRule soundRule) {
        areaRule.removeSoundRule(soundRule);
        needsRebuild = true;
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(page + delta, pageCount - 1));
        needsRebuild = true;
    }

    private void setStatus(String text, boolean error) {
        statusText = text;
        statusError = error;
    }

    private Component fit(String text, int maxWidth) {
        Component full = Component.literal(text);
        if (font.width(full) <= maxWidth) return full;

        String shortened = text;
        while (!shortened.isEmpty() && font.width(Component.literal(shortened + "...")) > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return Component.literal(shortened + "...");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (needsRebuild) {
            needsRebuild = false;
            clearWidgets();
            init();
        }

        super.extractRenderState(g, mouseX, mouseY, a);

        MenuStyle.centeredText(g, font, fit("Configure: " + area.getName(), width - OUTER_PAD * 2), width / 2, TITLE_Y, MenuStyle.TEXT_COLOR);

        int leftCenter = leftX + colWidth / 2;
        int rightCenter = rightX + colWidth / 2;
        MenuStyle.centeredText(g, font, GENERAL_HEADER, leftCenter, generalHeaderY, MenuStyle.TEXT_COLOR);
        g.text(
                font,
                Component.literal("Name"),
                leftX,
                nameLabelY,
                HEADER_COLOR
        );
        MenuStyle.centeredText(g, font, CATEGORIES_HEADER, leftCenter, categoriesHeaderY, MenuStyle.TEXT_COLOR);
        MenuStyle.centeredText(g, font, COLOR_HEADER, leftCenter, colorHeaderY, MenuStyle.TEXT_COLOR);
        MenuStyle.centeredText(g, font, Component.literal("Sound Rules (" + areaRule.getSoundRules().size() + ")"), rightCenter, rightHeaderY, MenuStyle.TEXT_COLOR);

        if (!statusText.isEmpty()) {
            g.text(font, fit(statusText, colWidth), rightX, statusY, statusError ? ERROR_COLOR : SUCCESS_COLOR);
        }

        if (ruleViews.isEmpty()) {
            g.text(font, NO_RULES, rightX, rulesTop + 4, MenuStyle.EMPTY_COLOR);
        }
        for (RuleView view : ruleViews) {
            g.text(font, fit(view.rule().soundId, colWidth - REMOVE_WIDTH - 6), rightX, view.y() + (RULE_LINE_HEIGHT - font.lineHeight) / 2 + 1, MenuStyle.TEXT_COLOR);
        }

        Component pageLabel = Component.literal("Page " + (page + 1) + "/" + pageCount);
        int pageLabelX = rightX + (colWidth - font.width(pageLabel)) / 2;
        g.text(font, pageLabel, pageLabelX, pagerY + (WIDGET_HEIGHT - font.lineHeight) / 2 + 1, HEADER_COLOR);

        for (AbstractWidget widget : drawList) {
            widget.extractRenderState(g, mouseX, mouseY, a);
        }

        for (PanelButton button : suggestionButtons) {
            button.extractRenderState(g, mouseX, mouseY, a);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        MenuStyle.body(g, 0, bodyTop, width, bodyBottom);
        MenuStyle.headerSeparator(g, 0, width, bodyTop - SEPARATOR_THICKNESS);
        MenuStyle.footerSeparator(g, 0, width, bodyBottom);
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    public void onClose() {
        String newName = nameInput.getValue().trim();

        area.setName(newName.isEmpty() ? "Unnamed" : newName);

        minecraft.setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}