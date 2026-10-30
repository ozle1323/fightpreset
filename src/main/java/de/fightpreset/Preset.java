package de.fightpreset;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Ein Inventar-Preset. Die Slots entsprechen den Indizes des Spieler-Inventars:
 * 0-8 Hotbar, 9-35 Inventar, 36-39 Rüstung (Schuhe bis Helm), 40 Zweithand.
 */
public class Preset {
	public static final int SLOT_COUNT = 41;

	public String id = UUID.randomUUID().toString();
	public String name = "Neues Preset";
	public String icon = "minecraft:iron_sword";
	public Map<Integer, String> slots = new TreeMap<>();

	public void captureFrom(PlayerInventory inventory) {
		slots.clear();
		for (int i = 0; i < SLOT_COUNT; i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) {
				slots.put(i, itemId(stack));
			}
		}
	}

	public static Preset fromInventory(PlayerInventory inventory, String name) {
		Preset preset = new Preset();
		preset.name = name;
		preset.captureFrom(inventory);
		// Standard-Symbol: erstes Item in der Hotbar, sonst irgendein Item
		for (int i = 0; i < SLOT_COUNT; i++) {
			String id = preset.slots.get(i);
			if (id != null) {
				preset.icon = id;
				break;
			}
		}
		return preset;
	}

	public static String itemId(ItemStack stack) {
		return Registries.ITEM.getId(stack.getItem()).toString();
	}
}
