package de.fightpreset;

import de.fightpreset.mixin.HandledScreenAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Zeichnet Grün/Rot-Markierungen und Ghost-Items über die Slots des Inventars. */
public final class PresetOverlay {
	/** Richtiges Item im Slot: leicht transparentes Grün. */
	private static final int COLOR_CORRECT = 0x6632C832;
	/** Falsches Item im Slot: leicht transparentes Rot. */
	private static final int COLOR_WRONG = 0x70E63232;
	/** Fehlendes Item (Slot leer): dezenter oranger Schimmer hinter dem Ghost-Item. */
	private static final int COLOR_MISSING = 0x38FFB000;
	/** Deckschicht in Slot-Grau über dem Ghost-Item, damit es nur schwach zu sehen ist. */
	private static final int GHOST_COVER = 0xB48B8B8B;

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
	}
}
