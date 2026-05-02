package dev.tggamesyt.chalkboard.client;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThirdpersonModelRegisterer {

    private static final Map<Identifier, Identifier> CUSTOM_MODELS = new HashMap<>();

    public static void register(Identifier itemId, Identifier inHandModelId) {
        CUSTOM_MODELS.put(itemId, inHandModelId);
    }

    public static Identifier get(Item item) {
        return CUSTOM_MODELS.get(BuiltInRegistries.ITEM.getKey(item));
    }

    public static Map<Identifier, Identifier> getAll() {
        return Collections.unmodifiableMap(CUSTOM_MODELS);
    }
}