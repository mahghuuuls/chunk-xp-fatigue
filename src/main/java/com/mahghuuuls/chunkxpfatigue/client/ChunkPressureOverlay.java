package com.mahghuuuls.chunkxpfatigue.client;

import com.mahghuuuls.chunkxpfatigue.network.OverlaySnapshot;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ChunkPressureOverlay {
    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        OverlaySnapshot snapshot = ClientOverlayState.get();
        Minecraft mc = Minecraft.getMinecraft();
        if (snapshot == null || mc.gameSettings.showDebugInfo) return;
        String text = snapshot.displayText();
        int x = (event.getResolution().getScaledWidth() - mc.fontRenderer.getStringWidth(text)) / 2;
        mc.fontRenderer.drawStringWithShadow(text, x, 4, 0xFFFFFF);
    }
}
