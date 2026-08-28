package software.hacker_E303.pigeon_core.client.item;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.hacker_E303.pigeon_core.PigeonCore;

/**
 * Holds the unbaked {@link BlockModel}s for every {@code models/items/*.json}
 * shipped by a registered modid, keyed by the bare model id
 * ({@code <modid>:items/<name>}, matching {@link software.hacker_E303.pigeon_core.item.EItem#getModelLocation}).
 *
 * <p>These never go through the vanilla {@code ModelBakery}/{@code ModelManager}
 * pipeline — {@link EItemModelHandler} bakes them itself, on demand, against
 * whichever texture atlas sprite the stack currently has selected (see
 * {@link software.hacker_E303.pigeon_core.item.common.IItemModel}: a custom
 * model is a shape, its faces are re-skinned with the item's own dynamic
 * texture rather than any texture declared in the JSON).</p>
 *
 * <p>Reloaded by {@link PigeonItemAtlas} on every stitch, so it stays in step
 * with the item atlas's own reload lifecycle.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PigeonItemModelRegistry {

    private static volatile Map<ResourceLocation, BlockModel> MODELS = Map.of();

    private PigeonItemModelRegistry() {}

    /**
     * @param id the bare model id ({@code <modid>:items/<name>})
     * @return the unbaked model, or {@code null} if none is registered under that id
     */
    public static BlockModel get(ResourceLocation id) {
        return MODELS.get(id);
    }

    /**
     * Rebuilds the registry by scanning {@code models/items/*.json} for every
     * registered modid.
     *
     * @param resourceManager the resource manager to scan
     */
    static void reload(ResourceManager resourceManager) {
        Map<ResourceLocation, BlockModel> models = new HashMap<>();

        for (String modid : PigeonCore.getRegisteredModids()) {
            Map<ResourceLocation, Resource> resources = resourceManager.listResources("models/items",
                name -> name.getNamespace().equals(modid) && name.getPath().endsWith(".json"));

            for (Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation modelId = toModelId(entry.getKey());
                try (InputStreamReader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                    models.put(modelId, BlockModel.fromStream(reader));
                } catch (IOException | RuntimeException e) {
                    PigeonCore.LOGGER.warn("[PigeonItemModel] failed to parse {}", entry.getKey(), e);
                }
            }
        }

        MODELS = models;
        PigeonCore.LOGGER.info("[PigeonItemModel] loaded {} item model(s)", models.size());
    }

    /**
     * Converts a resource id like {@code modid:models/items/foo.json} into the
     * bare model id {@code modid:items/foo}.
     */
    private static ResourceLocation toModelId(ResourceLocation resource) {
        String strip = "models/items/";
        String path = resource.getPath();
        if (path.startsWith(strip)) path = path.substring(strip.length());
        if (path.endsWith(".json")) path = path.substring(0, path.length() - 5);
        return new ResourceLocation(resource.getNamespace(), "items/" + path);
    }
}
