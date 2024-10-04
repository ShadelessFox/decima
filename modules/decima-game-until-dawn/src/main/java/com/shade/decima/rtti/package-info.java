@GenerateBindings(
    namespace = "UntilDawn",
    source = "data/until_dawn_rtti.json",
    callbacks = {
        @Callback(type = "AnimatedTexture", handler = AnimatedTextureCallback.class),
        @Callback(type = "CoreScript", handler = CoreScriptCallback.class),
        @Callback(type = "ExternalSourceCacheResource", handler = ExternalSourceCacheResourceCallback.class),
        @Callback(type = "HavokClothResource", handler = HavokClothResourceCallback.class),
        @Callback(type = "IndexArrayResource", handler = IndexArrayResourceCallback.class),
        @Callback(type = "MorphemeAnimationResource", handler = MorphemeAnimationResourceCallback.class),
        @Callback(type = "NavMesh", handler = NavMeshCallback.class),
        @Callback(type = "PhysicsCollisionResource", handler = PhysicsCollisionResourceCallback.class),
        @Callback(type = "ScaleformGFxMovieResource", handler = ScaleformGFxMovieResourceCallback.class),
        @Callback(type = "ShaderResource", handler = ShaderResourceCallback.class),
        @Callback(type = "Texture", handler = TextureCallback.class),
        @Callback(type = "VertexArrayResource", handler = VertexArrayResourceCallback.class),
        @Callback(type = "WWiseSoundBankResource", handler = WWiseSoundBankResourceCallback.class),
    }
)
package com.shade.decima.rtti;

import com.shade.decima.rtti.callbacks.*;
import com.shade.decima.rtti.generator.GenerateBindings;
import com.shade.decima.rtti.generator.GenerateBindings.Callback;