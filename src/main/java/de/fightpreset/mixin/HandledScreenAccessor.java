package de.fightpreset.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gibt Zugriff auf die linke obere Ecke des Inventar-Fensters. */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
	@Accessor("x")
	int fightpreset$getX();

	@Accessor("y")
	int fightpreset$getY();
}
