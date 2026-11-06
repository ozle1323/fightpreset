package de.fightpreset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.fightpreset.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * Zeichnet Grün/Rot-Markierungen und Ghost-Items über die Slots des Inventars
 * und links neben dem Inventar eine Liste der fehlenden Items.
 */
public final class PresetOverlay {
	/** Richtiges Item im Slot: leicht transparentes Grün. */
	private static final int COLOR_CORRECT = 0x6632C832;
	/** Falsches Item im Slot: leicht transparentes Rot. */
	private static final int COLOR_WRONG = 0x70E63232;
	/** Fehlendes Item (Slot leer): dezenter oranger Schimmer hinter dem Ghost-Item. */
	private static final int COLOR_MISSING = 0x38FFB000;
	/** Deckschicht in Slot-Grau über dem Ghost-Item, damit es nur schwach zu sehen ist. */
	private static final int GHOST_COVER = 0xB48B8B8B;

	private static final int MAX_LIST_ROWS = 14;
	private static final int ROW_H = 18;
	private static final int PANEL_PAD = 5;

	/** Eintrag der Fehlt-Liste. */
	private record Entry(String itemId, int amount, boolean inInventory) {
	}

	private PresetOverlay() {
	}

	public static void render(DrawContext context, HandledScreen<?> screen) {
		if (!PresetStore.overlayEnabled || screen instanceof CreativeInventoryScreen) {
			return;
		}
		Preset preset = PresetStore.active();
		if (preset == null) {
			return;
		}

		HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
		int originX = accessor.fightpreset$getX();
		int originY = accessor.fightpreset$getY();

		for (Slot slot : screen.getScreenHandler().slots) {
			// Nur Slots, die zum Spieler-Inventar gehören (auch in Kisten, Werkbank usw.)
			if (!(slot.inventory instanceof PlayerInventory)) {
				continue;
			}
			String expected = preset.slots.get(slot.getIndex());
			if (expected == null) {
				continue;
			}

			int x = originX + slot.x;
			int y = originY + slot.y;
			ItemStack actual = slot.getStack();

			if (actual.isEmpty()) {
				ItemStack ghost = PresetStore.stackOf(expected);
				if (!ghost.isEmpty()) {
					context.drawItem(ghost, x, y);
					context.fill(x, y, x + 16, y + 16, GHOST_COVER);
				}
				context.fill(x, y, x + 16, y + 16, COLOR_MISSING);
			} else if (expected.equals(Preset.itemId(actual))) {
				context.fill(x, y, x + 16, y + 16, COLOR_CORRECT);
			} else {
				context.fill(x, y, x + 16, y + 16, COLOR_WRONG);
			}
		}

		renderMissingList(context, preset, originX, originY);
	}

	/**
	 * Liste links neben dem Inventar: alle Items, die laut Preset noch nicht
	 * an ihrem Platz liegen. Rot = fehlt komplett, gelb = liegt im falschen Slot.
	 */
	private static void renderMissingList(DrawContext context, Preset preset, int originX, int originY) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
		PlayerInventory inventory = client.player.getInventory();
		TextRenderer font = client.textRenderer;

		// Pro Item-Typ: wie viele Slots erwarten es, wie viele sind korrekt belegt
		Map<String, int[]> needed = new LinkedHashMap<>(); // [erwartet, korrekt]
		for (Map.Entry<Integer, String> e : preset.slots.entrySet()) {
			int[] counts = needed.computeIfAbsent(e.getValue(), k -> new int[2]);
			counts[0]++;
			int slotIndex = e.getKey();
			if (slotIndex >= 0 && slotIndex < Preset.SLOT_COUNT) {
				ItemStack actual = inventory.getStack(slotIndex);
				if (!actual.isEmpty() && e.getValue().equals(Preset.itemId(actual))) {
					counts[1]++;
				}
			}
		}

		// Wie viele Stücke jedes Items liegen insgesamt im Inventar (Slots)
		Map<String, Integer> present = new LinkedHashMap<>();
		for (int i = 0; i < Preset.SLOT_COUNT; i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) {
				present.merge(Preset.itemId(stack), 1, Integer::sum);
			}
		}

		List<Entry> missing = new ArrayList<>();
		for (Map.Entry<String, int[]> e : needed.entrySet()) {
			int open = e.getValue()[0] - e.getValue()[1];
			if (open <= 0) {
				continue;
			}
			// Stücke, die zwar im Inventar sind, aber nicht auf einem korrekten Slot
			int elsewhere = present.getOrDefault(e.getKey(), 0) - e.getValue()[1];
			missing.add(new Entry(e.getKey(), open, elsewhere > 0));
		}

		Text title;
		if (missing.isEmpty()) {
			title = Text.literal("Alles komplett");
		} else {
			title = Text.literal("Fehlt (" + missing.size() + ")");
		}

		int shown = Math.min(missing.size(), MAX_LIST_ROWS);
		int hidden = missing.size() - shown;

		// Breite des Panels bestimmen
		int textWidth = font.getWidth(title);
		List<Text> labels = new ArrayList<>();
		for (int i = 0; i < shown; i++) {
			Entry entry = missing.get(i);
			Text name = PresetStore.stackOf(entry.itemId()).getName();
			String suffix = entry.amount() > 1 ? " x" + entry.amount() : "";
			Text label = Text.literal("").append(name).append(Text.literal(suffix));
			labels.add(label);
			textWidth = Math.max(textWidth, font.getWidth(label) + 20);
		}
		Text moreLabel = hidden > 0 ? Text.literal("... +" + hidden + " weitere") : null;
		if (moreLabel != null) {
			textWidth = Math.max(textWidth, font.getWidth(moreLabel));
		}

		int panelW = textWidth + PANEL_PAD * 2;
		int rows = shown + (moreLabel != null ? 1 : 0);
		int panelH = PANEL_PAD * 2 + 12 + rows * ROW_H;

		int x = Math.max(2, originX - panelW - 4);
		// unter den Buttons oben links beginnen
		int y = Math.max(originY, 72);

		context.fill(x, y, x + panelW, y + panelH, 0xC0101010);
		border(context, x, y, panelW, panelH, 0xFF606060);

		int titleColor = missing.isEmpty() ? 0xFF55FF55 : 0xFFFFFFFF;
		context.drawTextWithShadow(font, title, x + PANEL_PAD, y + PANEL_PAD, titleColor);

		int rowY = y + PANEL_PAD + 12;
		for (int i = 0; i < shown; i++) {
			Entry entry = missing.get(i);
			ItemStack icon = PresetStore.stackOf(entry.itemId());
			if (!icon.isEmpty()) {
				context.drawItem(icon, x + PANEL_PAD, rowY);
			}
			int color = entry.inInventory() ? 0xFFFFD84D : 0xFFFF6B6B;
			context.drawTextWithShadow(font, labels.get(i), x + PANEL_PAD + 20, rowY + 4, color);
			rowY += ROW_H;
		}
		if (moreLabel != null) {
			context.drawTextWithShadow(font, moreLabel, x + PANEL_PAD, rowY + 4, 0xFFAAAAAA);
		}
	}

	private static void border(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + 1, color);
		context.fill(x, y + h - 1, x + w, y + h, color);
		context.fill(x, y + 1, x + 1, y + h - 1, color);
		context.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}
}
