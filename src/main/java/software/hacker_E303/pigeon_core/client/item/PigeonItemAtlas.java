package software.hacker_E303.pigeon_core.client.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.hacker_E303.pigeon_core.PigeonCore;

/**
 * The framework's own item atlas ({@code pigeon_core:items}). In 1.20.1 the
 * vanilla {@code minecraft:items} atlas is populated from item-model Materials
 * and is not stitchable through the public sprite pipeline, so the framework
 * stitches its own atlas and resolves {@code EItem} textures from it.
 *
 * <p>Sprites for every framework modid ({@code textures/items/*.png} and the
 * shared {@code textures/misc/none.png}) are contributed directly here at stitch
 * time — no atlas-info JSON and no mixin required, so it works for external mods
 * that use the framework. Registered as a {@link PreparableReloadListener} so it
 * re-stitches on every resource reload (pack change, F3+T), exactly like a
 * normal atlas.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PigeonItemAtlas implements PreparableReloadListener {

    public static final ResourceLocation ATLAS_LOCATION = new ResourceLocation("pigeon_core", "items");
    public static final ResourceLocation ATLAS_INFO_LOCATION = new ResourceLocation("pigeon_core", "atlases/items");

    // Floor for the stitched canvas — see the comment in stitchNow() for why this
    // must not be left to shrink to fit the (possibly tiny) sprite count. 1024
    // keeps FaceBakery's built-in UV shrink (4px / this) at ~0.4%, in line with
    // vanilla's own (large) atlases — 256 left a still-faint residual crop.
    private static final int MIN_ATLAS_SIZE = 1024;

    private static volatile TextureAtlas INSTANCE = null;

    private final TextureAtlas atlas = new TextureAtlas(ATLAS_LOCATION);

    public static TextureAtlas getAtlas() {
        return INSTANCE;
    }

    public static void init() {
        PigeonItemAtlas instance = new PigeonItemAtlas();
        INSTANCE = instance.atlas;
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().register(ATLAS_LOCATION, instance.atlas);
        ((net.minecraft.server.packs.resources.ReloadableResourceManager) mc.getResourceManager())
            .registerReloadListener(instance);

        // Stitch immediately using the current ResourceManager so the atlas is
        // ready even if the first global resource reload has already been
        // scheduled (avoids a one-frame-empty race). Future reloads (F3+T, pack
        // changes) re-stitch via the reload listener above.
        instance.stitchNow(mc.getResourceManager(),
            runnable -> mc.execute(runnable));

        PigeonCore.LOGGER.info("[PigeonAtlas] registered framework item atlas {}", ATLAS_LOCATION);
    }

    /** Builds and uploads the atlas from the given resource manager. */
    private void stitchNow(ResourceManager resourceManager, Executor executor) {
        // Kept in step with the atlas: item model JSONs (models/items/*.json) are
        // re-scanned on the same reload so EItemModelHandler always sees a
        // consistent pair of sprites + shapes.
        PigeonItemModelRegistry.reload(resourceManager);

        // Deliberately NOT Minecraft.getInstance().options.mipmapLevels(): this
        // atlas is tiny (a handful of tightly packed 16x16 icons), nowhere near
        // large enough to hold the border padding mipmapping needs per sprite
        // (up to 2^mipLevel px/side) without adjacent sprites bleeding into each
        // other. 2D pixel-art icons gain nothing from mipmapping anyway (unlike
        // world-distance block textures).
        int mipLevel = 0;

        // NOT SpriteLoader.create(atlas): that derives the stitcher's minimum
        // canvas size from the ATLAS'S OWN CURRENT width/height, which is 0 before
        // the first stitch — so with only a couple of 16x16 icons, Stitcher packs
        // a tiny (e.g. 32x32) canvas. FaceBakery.bakeQuad unconditionally insets
        // every face's UV by TextureAtlasSprite#uvShrinkRatio() = 4px / atlasSize
        // (an anti-bleed epsilon that's negligible on a normal, hundreds-of-pixels
        // atlas). On a 32px atlas that's a 12.5% inward crop, applied uniformly to
        // every face — barely noticeable on the thin edge "tape" quads, but very
        // visible on the front/back plate, which showed as an oversized, cropped
        // texture. Flooring the canvas at MIN_ATLAS_SIZE keeps that shrink
        // negligible, same as any normal-sized atlas.
        SpriteLoader loader = new SpriteLoader(ATLAS_LOCATION, atlas.maxSupportedTextureSize(),
            MIN_ATLAS_SIZE, MIN_ATLAS_SIZE);
        List<Supplier<SpriteContents>> suppliers = new ArrayList<>();

        // Any sprites contributed by an atlas-info JSON (pigeon_core:atlases/items),
        // if a mod ships one. Harmless if absent.
        try {
            suppliers.addAll(net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader
                .load(resourceManager, ATLAS_INFO_LOCATION).list(resourceManager));
        } catch (Throwable t) {
            PigeonCore.LOGGER.debug("[PigeonAtlas] no atlas-info for {}", ATLAS_INFO_LOCATION);
        }

        // Framework item sprites for every registered modid.
        int count = 0;
        for (String modid : PigeonCore.getRegisteredModids()) {
            count += addItemsFolder(resourceManager, suppliers, modid);
            if (addNonePlaceholder(resourceManager, suppliers, modid)) count++;
        }
        PigeonCore.LOGGER.info("[PigeonAtlas] stitching {} item sprite(s) into {}", count, ATLAS_LOCATION);

        SpriteLoader.runSpriteSuppliers(suppliers, executor)
            .thenApply(contents -> loader.stitch(contents, mipLevel, executor))
            .thenAccept(atlas::upload)
            .exceptionally(t -> {
                PigeonCore.LOGGER.error("[PigeonAtlas] failed to stitch item atlas", t);
                return null;
            });
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
            ProfilerFiller profilerPrep, ProfilerFiller profilerApply, Executor executorPrep, Executor executorApply) {
        stitchNow(resourceManager, executorPrep);
        return barrier.wait(null);
    }

    private static int addItemsFolder(ResourceManager rm, List<Supplier<SpriteContents>> suppliers, String modid) {
        Map<ResourceLocation, Resource> resources = rm.listResources("textures/items",
            name -> name.getNamespace().equals(modid) && name.getPath().endsWith(".png"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation spriteId = toSpriteId(entry.getKey(), "textures/items/", "items/");
            Resource resource = entry.getValue();
            suppliers.add(() -> SpriteLoader.loadSprite(spriteId, resource));
        }
        return resources.size();
    }

    private static boolean addNonePlaceholder(ResourceManager rm, List<Supplier<SpriteContents>> suppliers, String modid) {
        ResourceLocation res = new ResourceLocation(modid, "textures/misc/none.png");
        List<Resource> stack = rm.getResourceStack(res);
        if (stack.isEmpty()) return false;
        ResourceLocation spriteId = new ResourceLocation(modid, "misc/none");
        Resource resource = stack.get(stack.size() - 1);
        suppliers.add(() -> SpriteLoader.loadSprite(spriteId, resource));
        return true;
    }

    /**
     * Converts a resource id like {@code modid:textures/items/foo.png} into the
     * atlas sprite id {@code modid:items/foo} (the form atlas.getSprite expects).
     */
    private static ResourceLocation toSpriteId(ResourceLocation resource, String strip, String prefix) {
        String path = resource.getPath();
        if (path.startsWith(strip)) path = path.substring(strip.length());
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
        return new ResourceLocation(resource.getNamespace(), prefix + path);
    }
}
