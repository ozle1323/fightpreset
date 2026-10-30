package de.fightpreset;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Menü zum Verwalten der Presets: Inventar einlesen, benennen, Symbol wählen,
 * löschen und das Overlay an- und ausschalten.
 */
public class PresetMenuScreen extends Screen {
	private static final int PANEL_W = 380;
	private static final int PANEL_H = 240;
	private static final int LIST_ROWS = 5;
	private static final int ICON_COLS = 11;

	private static final String[] ICONS = {
			"minecraft:netherite_sword", "minecraft:diamond_sword", "minecraft:iron_sword",
			"minecraft:netherite_axe", "minecraft:diamond_axe", "minecraft:mace", "minecraft:trident",
			"minecraft:bow", "minecraft:crossbow", "minecraft:shield", "minecraft:totem_of_undying",
			"minecraft:golden_apple", "minecraft:enchanted_golden_apple", "minecraft:ender_pearl",
			"minecraft:wind_charge", "minecraft:water_bucket", "minecraft:cobweb", "minecraft:tnt",
			"minecraft:end_crystal", "minecraft:respawn_anchor", "minecraft:netherite_chestplate",
			"minecraft:elytra"
	};

	private final Screen parent;
	private int left;
	private int top;
	private int listOffset = 0;

	public PresetMenuScreen(Screen parent) {
		super(Text.literal("Fight Presets"));
		this.parent = parent;
	}

	public static String shorten(String text, int max) {
		return text.length() <= max ? text : text.substring(0, max - 1) + ".";
	}

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = (height - PANEL_H) / 2;

		List<Preset> presets = PresetStore.presets;
		listOffset = Math.max(0, Math.min(listOffset, presets.size() - LIST_ROWS));

		// ---- Linke Spalte: Preset-Liste ----
		for (int row = 0; row < LIST_ROWS; row++) {
			int index = listOffset + row;
			if (index >= presets.size()) {
				break;
			}
			Preset preset = presets.get(index);
			addDrawableChild(ButtonWidget.builder(Text.literal(shorten(preset.name, 14)), button -> {
				PresetStore.setActive(preset.id);
				clearAndInit();
			}).dimensions(left + 30, top + 22 + row * 22, 98, 20).build());
		}

		addDrawableChild(ButtonWidget.builder(Text.literal("Hoch"), button -> {
			listOffset = Math.max(0, listOffset - 1);
			clearAndInit();
		}).dimensions(left + 8, top + 134, 58, 16).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Runter"), button -> {
			listOffset++;
			clearAndInit();
		}).dimensions(left + 70, top + 134, 58, 16).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Neu aus Inventar"), button -> {
			if (client == null || client.player == null) {
				return;
			}
			Preset created = Preset.fromInventory(client.player.getInventory(),
					"Preset " + (PresetStore.presets.size() + 1));
			PresetStore.add(created);
			listOffset = Math.max(0, PresetStore.presets.size() - LIST_ROWS);
			clearAndInit();
		}).dimensions(left + 8, top + 156, 120, 20).build());

		Preset selected = PresetStore.active();

		addDrawableChild(ButtonWidget.builder(Text.literal("Löschen"), button -> {
			Preset current = PresetStore.active();
			if (current != null) {
				PresetStore.remove(current);
				clearAndInit();
			}
		}).dimensions(left + 8, top + 180, 120, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), button -> close())
				.dimensions(left + 8, top + 214, 120, 20).build());

		// ---- Rechte Spalte ----
		addDrawableChild(ButtonWidget.builder(Text.literal("Overlay: " + (PresetStore.overlayEnabled ? "AN" : "AUS")),
				button -> {
					PresetStore.overlayEnabled = !PresetStore.overlayEnabled;
					PresetStore.save();
					button.setMessage(Text.literal("Overlay: " + (PresetStore.overlayEnabled ? "AN" : "AUS")));
				}).dimensions(left + 258, top + 216, 114, 20).build());

		if (selected == null) {
			return;
		}

		TextFieldWidget nameField = new TextFieldWidget(textRenderer, left + 140, top + 32, 232, 18,
				Text.literal("Name"));
		nameField.setMaxLength(32);
		nameField.setText(selected.name);
		nameField.setChangedListener(text -> selected.name = text);
		addDrawableChild(nameField);

		for (int i = 0; i < ICONS.length; i++) {
			String iconId = ICONS[i];
			int x = left + 140 + (i % ICON_COLS) * 21;
			int y = top + 66 + (i / ICON_COLS) * 21;
			addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
				selected.icon = iconId;
				PresetStore.save();
			}).dimensions(x, y, 20, 20).build());
		}

		addDrawableChild(ButtonWidget.builder(Text.literal("Inventar übernehmen"), button -> {
			if (client != null && client.player != null) {
				selected.captureFrom(client.player.getInventory());
				PresetStore.save();
			}
		}).dimensions(left + 140, top + 216, 114, 20).build());
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		context.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE0101010);
		border(context, left, top, PANEL_W, PANEL_H, 0xFF606060);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawTextWithShadow(textRenderer, Text.literal("Fight Presets"), left + 8, top + 8, 0xFFFFFFFF);

		// Liste: Symbole und Markierung des aktiven Presets
		List<Preset> presets = PresetStore.presets;
		Preset selected = PresetStore.active();
		for (int row = 0; row < LIST_ROWS; row++) {
			int index = listOffset + row;
			if (index >= presets.size()) {
				break;
			}
			Preset preset = presets.get(index);
			int y = top + 22 + row * 22;
			context.fill(left + 8, y, left + 28, y + 20, 0xFF373737);
			ItemStack icon = PresetStore.stackOf(preset.icon);
			if (!icon.isEmpty()) {
				context.drawItem(icon, left + 10, y + 2);
			}
			if (preset == selected) {
				border(context, left + 8, y, 120, 20, 0xFF55FF55);
			}
		}

		if (selected == null) {
			context.drawTextWithShadow(textRenderer, Text.literal("Noch kein Preset vorhanden."),
					left + 140, top + 40, 0xFFFFFFFF);
			context.drawTextWithShadow(textRenderer, Text.literal("Lege dein Inventar so, wie du es"),
					left + 140, top + 56, 0xFFAAAAAA);
			context.drawTextWithShadow(textRenderer, Text.literal("willst, und klicke links auf"),
					left + 140, top + 68, 0xFFAAAAAA);
			context.drawTextWithShadow(textRenderer, Text.literal("\"Neu aus Inventar\"."),
					left + 140, top + 80, 0xFFAAAAAA);
			return;
		}

		context.drawTextWithShadow(textRenderer, Text.literal("Name"), left + 140, top + 22, 0xFFAAAAAA);
		context.drawTextWithShadow(textRenderer, Text.literal("Symbol"), left + 140, top + 56, 0xFFAAAAAA);

		// Symbol-Auswahl
		for (int i = 0; i < ICONS.length; i++) {
			int x = left + 140 + (i % ICON_COLS) * 21;
			int y = top + 66 + (i / ICON_COLS) * 21;
			ItemStack icon = PresetStore.stackOf(ICONS[i]);
			if (!icon.isEmpty()) {
				context.drawItem(icon, x + 2, y + 2);
			}
			if (ICONS[i].equals(selected.icon)) {
				border(context, x, y, 20, 20, 0xFF55FF55);
			}
		}

		drawPreview(context, selected, left + 175, top + 114);
	}

	/** Kleine Vorschau des Presets im Layout des Spieler-Inventars. */
	private void drawPreview(DrawContext context, Preset preset, int originX, int originY) {
		// Rüstung (Helm bis Schuhe = Inventar-Index 39..36) und Zweithand (40)
		for (int i = 0; i < 4; i++) {
			drawSlot(context, preset, 39 - i, originX + i * 18, originY);
		}
		drawSlot(context, preset, 40, originX + 144, originY);

		// Inventar: 3 Reihen (Index 9-35)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				drawSlot(context, preset, 9 + row * 9 + col, originX + col * 18, originY + 22 + row * 18);
			}
		}
		// Hotbar (Index 0-8)
		for (int col = 0; col < 9; col++) {
			drawSlot(context, preset, col, originX + col * 18, originY + 80);
		}
	}

	private void drawSlot(DrawContext context, Preset preset, int index, int x, int y) {
		context.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
		border(context, x, y, 18, 18, 0xFF373737);
		String id = preset.slots.get(index);
		if (id != null) {
			ItemStack stack = PresetStore.stackOf(id);
			if (!stack.isEmpty()) {
				context.drawItem(stack, x + 1, y + 1);
			}
		}
	}

	/** Zeichnet einen 1 Pixel breiten Rahmen (eigene Methode, da DrawContext.drawBorder in 1.21.11 entfällt). */
	private static void border(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + 1, color);
		context.fill(x, y + h - 1, x + w, y + h, color);
		context.fill(x, y + 1, x + 1, y + h - 1, color);
		context.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	@Override
	public void removed() {
		PresetStore.save();
	}

	@Override
	public void close() {
		MinecraftClient.getInstance().setScreen(parent);
	}
}
