package software.hacker_E303.pigeon_core.client.item;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import software.hacker_E303.pigeon_core.PigeonCore;
import software.hacker_E303.pigeon_core.item.EItem;
import software.hacker_E303.pigeon_core.util.PigeUtils;
import software.hacker_E303.pigeon_core.util.locator.Location;
import software.hacker_E303.pigeon_core.util.locator.Path;

/**
 * Resolves baked {@link BakedModel}s for {@link EItem} instances and spawn eggs,
 * sourcing textures from the framework's item atlas. Falls back to the base
 * model when textures are missing or the atlas has not yet stitched.
 */
public final class EItemModelHandler {

    private static final Map<ResourceLocation, BakedModel> CACHE = new HashMap<>();
    private static final Map<String, BakedModel> CUSTOM_CACHE = new HashMap<>();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ModelState IDENTITY_STATE = new ModelState() {};
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    // Minimal stand-in with a resolvable "layer0" texture variable, just so
    // ItemModelGenerator agrees to process it (see generateFlatModel). The actual
    // texture reference is never used — the real sprite is injected via the
    // Function<Material, TextureAtlasSprite> passed to generateBlockModel.
    private static final BlockModel FLAT_TEMPLATE =
        BlockModel.fromString("{\"textures\":{\"layer0\":\"minecraft:item/stick\"}}");

    private EItemModelHandler() {}

    /**
     * Resolves the appropriate {@link BakedModel} for an item stack.
     *
     * @param stack the item stack
     * @param base  the fallback baked model
     * @return the resolved model, or {@code base} if no override applies
     */
    public static BakedModel resolve(ItemStack stack, BakedModel base) {
        if (base == null) return base;

        if (stack.getItem() instanceof EItem eItem) {
            return resolveEItem(stack, base, eItem);
        } else if (stack.getItem() instanceof SpawnEggItem) {
            String pigeid = PigeUtils.getEggPigeid(stack);
            if (pigeid != null && PigeonCore.hasRegistered(pigeid)) {
                return resolveEgg(stack, base);
            }
        }

        return base;
    }

    /**
     * Resolves the baked model for an {@link EItem}, sourcing its texture from the
     * item atlas and — if the stack has a custom {@code Model} selected — its
     * shape from {@link PigeonItemModelRegistry}. The resolved texture is used to
     * re-skin every face of that shape (a custom model is a shape only; it carries
     * no textures of its own, see {@link software.hacker_E303.pigeon_core.item.common.IItemModel}).
     *
     * @param stack   the item stack
     * @param base    the fallback baked model
     * @param eItem   the {@link EItem} being resolved
     * @return the resolved model, or {@code base} if the atlas is unavailable
     */
    private static BakedModel resolveEItem(ItemStack stack, BakedModel base, EItem eItem) {
        ResourceLocation texture = eItem.getTextureLocation(stack);
        TextureAtlas atlas = getItemsAtlas();
        if (atlas == null) {
            // Atlas not stitched yet (e.g. before first resource reload). Fail
            // silently to the base model — this is transient, not an error.
            return base;
        }

        TextureAtlasSprite sprite = atlas.getSprite(texture);

        // A sprite that did not actually exist resolves to the atlas's baked
        // "missing" sprite. That is the signal: surface it instead of hiding it.
        if (isMissing(sprite, atlas)) {
            PigeonCore.LOGGER.warn("[EItemModel] missing texture '{}', showing none", texture);
            sprite = getNoneSprite(atlas, eItem.modid());
        }
        if (sprite == null) return base;

        String modelName = eItem.getModel(stack);
        if (!"none".equals(modelName)) {
            ResourceLocation modelId = eItem.getModelLocation(stack);
            BlockModel unbaked = PigeonItemModelRegistry.get(modelId);
            if (unbaked != null) return buildCustomModel(unbaked, modelId, sprite);
            PigeonCore.LOGGER.warn("[EItemModel] missing model '{}', using default shape", modelId);
        }

        return buildModel(base, sprite);
    }

    /**
     * Resolves the baked model for a spawn egg, sourcing its texture from the
     * item atlas by the pige-id encoded in the egg's {@link ItemStack}.
     *
     * @param stack the spawn egg item stack
     * @param base  the fallback baked model
     * @return the resolved model, or {@code base} if the egg or atlas is unavailable
     */
    private static BakedModel resolveEgg(ItemStack stack, BakedModel base) {
        ResourceLocation atlasLoc = PigeUtils.getEggSpriteId(stack);
        if (atlasLoc == null) return base;

        TextureAtlas atlas = getItemsAtlas();
        if (atlas == null) return base;

        TextureAtlasSprite sprite = atlas.getSprite(atlasLoc);
        if (isMissing(sprite, atlas)) {
            PigeonCore.LOGGER.warn("[EItemModel] missing texture '{}', showing none", atlasLoc);
            return buildModel(base, getNoneSprite(atlas, atlasLoc.getNamespace()));
        }

        return buildModel(base, sprite);
    }

    /**
     * Builds (or retrieves from cache) the default icon for the given sprite —
     * vanilla's own {@code item/generated} shape (front/back plate plus the
     * pixel-accurate "tape" side quads derived from the sprite's actual alpha
     * channel), re-baked here against the framework's atlas.
     *
     * <p>This deliberately does NOT use {@link net.minecraft.client.resources.model.BuiltInModel}:
     * that class always returns an empty quad list and reports
     * {@code isCustomRenderer() == true}, which tells {@code ItemRenderer} to skip
     * normal quad rendering entirely and hand off to a (here, unregistered)
     * {@code BlockEntityWithoutLevelRenderer} — i.e. nothing gets drawn. It also
     * does NOT hand-roll a bare flat plate: that skips the tape quads vanilla
     * generates from the sprite, which is what gives normal items their subtle
     * pseudo-3D edge instead of looking like a flat, slightly larger sheet.
     * {@link #getRenderTypes} binds the framework's own item atlas texture (the
     * quads' sprite lives there, not in vanilla's {@code blocks.png} atlas).</p>
     *
     * @param base   the base baked model to copy transforms from
     * @param sprite the sprite to use for rendering
     * @return the built model, or {@code base} if the sprite is null
     */
    @SuppressWarnings("deprecation")
    private static BakedModel buildModel(BakedModel base, TextureAtlasSprite sprite) {
        if (sprite == null) return base;
        return CACHE.computeIfAbsent(sprite.contents().name(), key -> {
            BlockModel generated = ITEM_MODEL_GENERATOR.generateBlockModel(material -> sprite, FLAT_TEMPLATE);
            Quads quads = bakeElements(generated.getElements(), sprite, key);
            return new SpriteBakedModel(base.getTransforms(), base.getOverrides(), sprite,
                base.useAmbientOcclusion(), false, quads.culled(), quads.unculled());
        });
    }

    /**
     * Builds (or retrieves from cache) a baked model for a custom shape
     * ({@code models/items/<name>.json}, loaded by {@link PigeonItemModelRegistry}),
     * with every face re-skinned with the given sprite regardless of whatever
     * texture variable the JSON declares.
     *
     * @param unbaked the parsed (unbaked) shape
     * @param modelId the shape's model id, used only for error reporting
     * @param sprite  the sprite to skin every face with
     * @return the built model
     */
    @SuppressWarnings("deprecation")
    private static BakedModel buildCustomModel(BlockModel unbaked, ResourceLocation modelId, TextureAtlasSprite sprite) {
        String cacheKey = modelId + "|" + sprite.contents().name();
        return CUSTOM_CACHE.computeIfAbsent(cacheKey, key -> {
            Quads quads = bakeElements(unbaked.getElements(), sprite, modelId);
            return new SpriteBakedModel(unbaked.getTransforms(), ItemOverrides.EMPTY, sprite,
                unbaked.hasAmbientOcclusion(), unbaked.getGuiLight().lightLikeBlock(),
                quads.culled(), quads.unculled());
        });
    }

    /**
     * Bakes every face of every element with {@code sprite}, ignoring whatever
     * texture variable each face declares, and buckets the resulting quads the
     * way {@code SimpleBakedModel} does: unculled faces (no {@code cullface}) are
     * only ever returned for the direction-agnostic query, culled faces only for
     * their own direction.
     */
    private static Quads bakeElements(List<BlockElement> elements, TextureAtlasSprite sprite, ResourceLocation id) {
        Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        List<BakedQuad> unculled = new ArrayList<>();

        for (BlockElement element : elements) {
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                BlockElementFace face = entry.getValue();
                BakedQuad quad = FACE_BAKERY.bakeQuad(element.from, element.to, face, sprite, entry.getKey(),
                    IDENTITY_STATE, element.rotation, element.shade, id);
                if (face.cullForDirection == null) unculled.add(quad);
                else culled.computeIfAbsent(face.cullForDirection, d -> new ArrayList<>()).add(quad);
            }
        }

        return new Quads(culled, unculled);
    }

    private record Quads(Map<Direction, List<BakedQuad>> culled, List<BakedQuad> unculled) {}

    private static final ResourceLocation MISSING_NAME = new ResourceLocation("minecraft", "missingno");

    /**
     * Tests whether the given sprite is the atlas's built-in "missing" sprite.
     *
     * @param sprite the sprite to test
     * @param atlas  the texture atlas (unused but kept for API consistency)
     * @return {@code true} if the sprite is null or represents a missing texture
     */
    private static boolean isMissing(TextureAtlasSprite sprite, TextureAtlas atlas) {
        if (sprite == null) return true;
        // The atlas returns its own baked "missing" sprite (name minecraft:missingno,
        // see MissingTextureAtlasSprite.getLocation()) for any unknown name. Compare
        // on the sprite's contents name rather than identity/equals — that is the
        // stable, deterministic signal and does not depend on whether vanilla
        // overrides TextureAtlasSprite.equals.
        return MISSING_NAME.equals(sprite.contents().name());
    }

    /**
     * Resolves the {@code <modid>:misc/none} sprite used as a fallback when a
     * texture is missing.
     *
     * @param atlas the texture atlas to query
     * @param modid the mod id for namespacing the sprite
     * @return the "none" sprite from the atlas
     */
    private static TextureAtlasSprite getNoneSprite(TextureAtlas atlas, String modid) {
        ResourceLocation none = Location.create(Path.create("misc/", ""), "none")
            .from(modid);
        return atlas.getSprite(none);
    }

    /**
     * Resolves the framework's item atlas ({@code pigeon_core:items}), stitched
     * and published by {@link PigeonItemAtlas#getAtlas()}. Returns null only
     * before the first resource reload has stitched it — callers then fall back
     * to the base model.
     */
    private static TextureAtlas getItemsAtlas() {
        return PigeonItemAtlas.getAtlas();
    }

    /**
     * A {@link BakedModel} bound to a single sprite from the framework's own
     * item atlas — either the flat 2-quad default icon, or a full shape loaded
     * from {@link PigeonItemModelRegistry} and re-skinned with that sprite.
     */
    private static final class SpriteBakedModel implements BakedModel {

        private final ItemTransforms transforms;
        private final ItemOverrides overrides;
        private final TextureAtlasSprite sprite;
        private final boolean ambientOcclusion;
        private final boolean gui3d;
        private final Map<Direction, List<BakedQuad>> culledQuads;
        private final List<BakedQuad> unculledQuads;

        SpriteBakedModel(ItemTransforms transforms, ItemOverrides overrides, TextureAtlasSprite sprite,
                boolean ambientOcclusion, boolean gui3d,
                Map<Direction, List<BakedQuad>> culledQuads, List<BakedQuad> unculledQuads) {
            this.transforms = transforms;
            this.overrides = overrides;
            this.sprite = sprite;
            this.ambientOcclusion = ambientOcclusion;
            this.gui3d = gui3d;
            this.culledQuads = culledQuads;
            this.unculledQuads = unculledQuads;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
            // Mirrors vanilla's SimpleBakedModel bucketing: unculled faces
            // (cullForDirection == null) only come back for the direction-agnostic
            // query, culled faces only for their own direction.
            return side == null ? unculledQuads : culledQuads.getOrDefault(side, List.of());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ambientOcclusion;
        }

        @Override
        public boolean isGui3d() {
            return gui3d;
        }

        @Override
        public boolean usesBlockLight() {
            return false;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return sprite;
        }

        @Override
        public ItemTransforms getTransforms() {
            return transforms;
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
            // The quads' sprite lives in PigeonItemAtlas.ATLAS_LOCATION, not
            // vanilla's blocks/items atlas — bind the matching texture.
            return List.of(RenderType.entityCutout(PigeonItemAtlas.ATLAS_LOCATION));
        }
    }
}
