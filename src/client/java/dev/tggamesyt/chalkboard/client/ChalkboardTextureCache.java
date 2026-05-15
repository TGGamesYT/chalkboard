package dev.tggamesyt.chalkboard.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.tggamesyt.chalkboard.ChalkboardMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChalkboardTextureCache {
    private static final int STALE_TICKS = 200;

    private static final Map<Long, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static volatile int currentTick;

    private ChalkboardTextureCache() {}

    public static Identifier getOrUpload(BlockPos pos, int[] pixels, int hash) {
        long key = pos.asLong();
        Entry e = ENTRIES.computeIfAbsent(key, Entry::new);
        e.lastSeen = currentTick;
        if (e.hash != hash) {
            e.upload(pixels);
            e.hash = hash;
        }
        return e.id;
    }

    public static void tick() {
        currentTick++;
        Iterator<Map.Entry<Long, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next().getValue();
            if (currentTick - e.lastSeen > STALE_TICKS) {
                e.release();
                it.remove();
            }
        }
    }

    private static final class Entry {
        final Identifier id;
        final DynamicTexture texture;
        int hash = -1;
        int lastSeen;

        Entry(long key) {
            String path = "dyn/" + Long.toUnsignedString(key);
            this.id = Identifier.fromNamespaceAndPath(ChalkboardMod.ID, path);
            String dbgName = ChalkboardMod.ID + ":" + path;
            this.texture = new DynamicTexture(() -> dbgName, 16, 16, true);
            Minecraft.getInstance().getTextureManager().register(this.id, this.texture);
        }

        void upload(int[] argbPixels) {
            NativeImage img = texture.getPixels();
            if (img == null) return;
            for (int i = 0; i < argbPixels.length && i < 256; i++) {
                int argb = argbPixels[i];
                int abgr;
                if (argb == 0) {
                    abgr = 0;
                } else {
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    abgr = (0xFF << 24) | (b << 16) | (g << 8) | r;
                }
                img.setPixelABGR(i & 15, i >> 4, abgr);
            }
            texture.upload();
        }

        void release() {
            TextureManager tm = Minecraft.getInstance().getTextureManager();
            tm.release(id);
            texture.close();
        }
    }
}
