package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.hash.CRC32C;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Field;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.util.NotNull;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ValueHandlerRegistration(id = "nameHash", name = "Name Hash", value = {
    @Selector(field = @Field(type = "ShaderTextureBinding", field = "BindingNameHash")),
    @Selector(field = @Field(type = "ShaderTextureBinding", field = "BindingSwizzleNameHash")),
    @Selector(field = @Field(type = "ShaderTextureBinding", field = "SamplerNameHash")),
    @Selector(field = @Field(type = "ShaderVariableBinding", field = "BindingNameHash")),
    @Selector(field = @Field(type = "ShaderVariableBinding", field = "VariableIDHash"))
})
public class NameHashValueHandler extends NumberValueHandler {
    public static final NameHashValueHandler INSTANCE = new NameHashValueHandler();

    private static final Map<Integer, String> LOOKUP;

    static {
        ArrayList<String> knownNames = new ArrayList<String>();

        // TextureBindings: BindingNameHash
        for (String n : Arrays.asList("BLACK", "WHITE", "RED", "GREEN", "BLUE", "CYAN", "MAGENTA", "YELLOW", "SKIN")) {
            for (String t : Arrays.asList("Color", "Normal", "NormalColor")) {
                knownNames.add("inSampler_%s_TextureSet%s".formatted(n, t));
            }
        }
        for (String n : Arrays.asList("Base_Set", "Base_Texture_Set", "Base_TextureSet", "Color_TextureSet",
            "Decal_Set", "Eyes_Teeth", "Fur_TextureSet", "FurDetail_Textureset",
            "HeadCap_TextureSet", "Hair_LUT", "Hair_Paint_Set",
            "Main_TextureSet", "Shared_Set", "textureset",
            "WrinkleMap_01_Set", "WrinkleMap_02_Set"
        )) {
            for (String t : Arrays.asList("Alpha",
                "AO", "AORoughnessReflectance",
                "Color", "ColorAlpha", "ColorReflectance", "ColorRoughness",
                "Height",
                "Incandescence",
                "Mask", "Mask_Alpha", "MaskReflectance", "MaskRoughness",
                "Misc_01", "Misc_01AO", "Misc_01Reflectance", "Misc_01Roughness",
                "Normal", "NormalReflectance", "NormalRoughness",
                "Reflectance", "ReflectanceAO",
                "Roughness", "RoughnessReflectanceAO",
                "Translucency_Amount", "Translucency_Diffusion"
            )) {
                knownNames.add("inSampler_%s%s".formatted(n, t));
            }
        }
        for (int i = 0; i < 20; i++) {
            knownNames.add("inSampler%d".formatted(i));
            knownNames.add("Layer_Wet_Character_inSampler%d".formatted(i));
            knownNames.add("Layer_Wet_Robot_inSampler%d".formatted(i));
        }
        knownNames.addAll(Arrays.asList(
            "AmbientBRDFTexture",
            "VolumeLightVolumeTexture",
            "DepthColorizeTexture",
            "ShadowmapTexture",
            "SunShadowmapTextureA",
            "SunShadowmapTextureB",
            "SunShadowmapTextureLongDistance",
            "SunCompartmentTextureC0",
            "SunCompartmentTextureC1",
            "NormalTexture",
            "ReflectanceTexture",
            "AttributesTexture",
            "DepthTexture",
            "DiffuseLightTexture",
            "SceneCubemapTexture",
            "LocalCubemapTexture0",
            "LocalCubemapTexture1",
            "IV0Terrain",
            "IV0Height",
            "IV0SkyVis",
            "IV00Aleph",
            "IV00Beth",
            "IV00Axis",
            "IV01Aleph",
            "IV01Beth",
            "IV01Axis",
            "IV02Aleph",
            "IV02Beth",
            "IV02Axis",
            "ForceFieldsTexture0",
            "ForceFieldsTexture1",
            "ForceFieldsTexture2",
            "BlendMatrices",
            "inSampler_LightSource",
            "inSampler_LightSourceDepth",
            "inSampler_MuscleMaskMask_Alpha",
            "inSampler_FakeSpecRamp",
            "inSampler_Fake_Spec_Ramp",
            "inSampler_Fake_Specular_Ramp",
            "inSampler_Base_TextureSet",
            "inSampler_Color_TextureSet",
            "inSampler_NoiseBreakup_TextureSetColor",
            "inSampler_TextureSet4AO",
            "inSampler_TextureSet4Normal",
            "inSampler_TextureSet6Mask",
            "inSampler_TextureSet27Height",
            "inSampler_TextureSet27Normal",
            "inSampler_TextureSet8Color",
            "inSampler_TextureSet8Normal",
            "inSampler_TextureSet8Height",
            "inSampler_Cracks_Set1Height",
            "inSampler_Cracks_Set1Normal",
            "inSampler_Lumps_Set1NormalColorHeight",
            "RampTexture89313432",
            "RampTexture833145267",
            "RampTexture906088057",
            "RampTexture1110498300",
            "WorldData_Topo_Roads_Topo_Water_Topo_Objects_Ecotope_Effect"
        ));

        // TextureBindings: BindingSwizzleNameHash
        for (String s : knownNames.toArray(new String[knownNames.size()])) {
            knownNames.add(s + "Swizzle");
        }

        // TextureBindings: SamplerNameHash
        for (int i = 0; i < 200; i++) {
            knownNames.add("GlobalSamplers_%d".formatted(i));
        }
        knownNames.addAll(Arrays.asList(
            "ShadowmapSampler",
            "SunShadowmapSampler",
            "SunLongDistanceSampler"
        ));

        // VariableBindings: BindingNameHash
        for (int i = 0; i < 100; i++) {
            knownNames.add("inVariable%d".formatted(i));
            knownNames.add("Layer_Wet_Character_inVariable%d".formatted(i));
            knownNames.add("Layer_Wet_Robot_inVariable%d".formatted(i));
        }
        knownNames.addAll(Arrays.asList(
            // Resource Bindings
            "Scratch_PerFrame",
            "Scratch_PerPass",
            "Scratch_PerView",
            "Scratch_PerBatch",
            "Scratch_PerInstance",
            "ShaderInstance_PerInstance",

            // Buffer Definitions
            // cbuffer Scratch_PerFrame
            // struct.GlobalConstants {
            "ColorizeValue",
            "AlbedoBiasScaleName",
            "ZoneReflectionIntensity",
            "PerTileVolumeTransformScale",
            "PerTileVolumeTransformOffsetName",
            "Temperature",
            "Precipitation",
            "Wetness",
            "CurTime",
            "DepthDirection",
            "MaterialRefColorFilter",
            "ShaderDebugMask",
            "PlayerSpeed",
            "WorldMinHeight",
            "WorldMaxHeight",
            "PlayerPosition",
            // } GlobalConstants

            // cbuffer Scratch_PerView
            // struct.ViewConstants {
            "View",
            "Proj",
            "ViewProj",
            "InvView",
            "OldViewProj",
            "DepthReconstructMatrix",
            "HalfResDepthReconstructMatrix",
            "QuarterResDepthReconstructMatrix",
            "Viewport",
            "WPOSScaleOffset",
            "MVScaleBias",
            "PixelSize",
            "ViewPos",
            "HPOSReconstructScaleOffset",
            "FloatingOrigin",
            // struct.GlobalRenderVariablesSRT {
            "GlobalRenderVariable0",
            "GlobalRenderVariable1",
            "GlobalRenderVariable2",
            "GlobalRenderVariable3",
            "GlobalRenderVariable4",
            "GlobalRenderVariable5",
            "GlobalRenderVariable6",
            "GlobalRenderVariable7",
            "GlobalRenderVariable8",
            "GlobalRenderVariable9",
            "GlobalRenderVariable10",
            "GlobalRenderVariable11",
            "GlobalRenderVariable12",
            "GlobalRenderVariable13",
            "GlobalRenderVariable14",
            "GlobalRenderVariable15",
            // } GlobalRenderVariables
            "ForceFieldsRegionOffset0",
            "ForceFieldsRegionOffset1",
            "ForceFieldsRegionOffset2",
            "ForceFieldsRegionScale0",
            "ForceFieldsRegionScale1",
            "ForceFieldsRegionScale2",
            "LodDistanceMul",
            "VolumeLightDepthRange",
            "PackedLightIndices",
            "VantageRenderScaler",
            // } ViewConstants

            // cbuffer Scratch_PerInstance
            // struct.RasterizerVariables {
            "ModelViewProj",
            "ModelView",
            "OldModelViewProj",
            "Model",
            "InstanceCustomData",
            // } RasterizerVariables
            // struct.RasterizerVariablesExtended {
            "InvModel",
            "CameraFacingMatrix",
            "InvModelView",
            "OldModel",
            "InvModelViewProj",
            // } RasterizerVariablesExtended
            // struct.SkinnedMeshInstanceData {
            "BlendMatrixOffset",
            "PrevBlendMatrixOffset",
            "Padding0",
            "Padding1",
            // } SkinnedMeshInstanceData
            // struct.ShadowMapConstants {
            "Transform",
            "ShadowFacingMatrix",
            "Bias",
            // } ShadowMapConstants

            // cbuffer Scratch_PerPass
            // struct.MetaLight {
            "Position",
            "LightDirection",
            "LightDirectionUp",
            "LightAreaParams",
            "DiffuseColor",
            "AttenuationDot",
            "AttenuationSmooth",
            "LightVPLPosition",
            "CameraToProjectiveTexture",
            "CameraToLightMatrix",
            "LightToCameraMatrix",
            "TanHalfConeAngle",
            "LightRange",
            "DepthMin",
            "DepthMax",
            "DiffuseAndSpecularMultiplier",
            "Padding",
            // } LightConstants[8]
            // struct.LightingMaterial {
            "SpecularColor",
            "Auxillary",
            // } MaterialConstants[6]
            // struct.ShadowMapSampleConstants {
            "CameraToShadowmap",
            "LightToShadowmap",
            "ShadowMapZScaleBias",
            "ShadowIntensity",
            // } ShadowMapSampleConstants[8]
            // struct.SunShadowSampleConstants {
            "CascadeInfo",
            "mCameraToLongDistanceShadowMatrix",
            "ExternalFadeoutOriginA",
            "ExternalFadeoutHalfExtentA",
            "ExternalFadeoutTransitionScaleA",
            "ExternalFadeoutOriginB",
            "ExternalFadeoutHalfExtentB",
            "ExternalFadeoutTransitionScaleB",
            "ExternalFadeoutOriginLD",
            "ExternalFadeoutHalfExtentLD",
            "ExternalFadeoutTransitionScaleLD",
            "ExternalFadeoutOriginC0",
            "ExternalFadeoutHalfExtentC0",
            "ExternalFadeoutTransitionScaleC0",
            "ExternalFadeoutOriginC1",
            "ExternalFadeoutHalfExtentC1",
            "ExternalFadeoutTransitionScaleC1",
            // } SunShadowConstants
            // struct.AtmosphericScattingCB {
            "SunlightDirection",
            "SkyColor",
            "MieScatteringPhases",
            "SunLightAbsorptionCoefficient",
            "SkyFadeOffSunAngleMin",
            "SkyFadeOffSunAngleMax",
            "SunIntensityAngleFadeMin",
            "SunIntensityAngleFadeMax",
            "SkyZenithIntensity",
            "SkyHorizonIntensity",
            "SkyGradientPower",
            "SkyBrightness",
            "MieIntensityGradientPower",
            "MieColorAbsorptionZenith",
            "MieColorAbsorptionHorizon",
            "MieColorGradientPower",
            "MieBaseIntensity",
            "MieLightShaftIntensity",
            "HazeStartDistance",
            "HazeEndDistance",
            "HazeDensityCurvature",
            "SunShapeIntensity",
            "SunShapeSize",
            "SunColorAbsorptionZenith",
            "SunColorAbsorptionHorizon",
            "SunColorAbsorptionGradientPower",
            "SunsetStartAngle",
            "SunsetAngleFadeRange",
            "NewAtmosphereEnabled",
            // } AtmosphereConstants
            // struct.WaterInteractionSampleParams_Constant {
            "AABB",
            // } WaterInteractionSampleParams_Constant
            // struct.SnowInteractionSampleParams_Constant {
            "SnowDeformationAABB",
            "SnowDeformationMaxDepth",
            // } SnowInteractionSampleParams_Constant

            // cbuffer Scratch_PerBatch
            // struct struct.CubemapZone2SRTData_Constant {
            "LocalCubemapParams",
            // } CubeMapZoneData_Constant
            // struct struct.ForwardPassIndirectConstants {
            "LayeredIrradianceParam",
            "LayeredIrradianceStrength",
            "NormalStepScale",
            "CommonTerrainParams",
            "SkyColorAverage",
            "SkyColorPX",
            "SkyColorPY",
            "IV0Region_Origin",
            "IV0Region_BasisU",
            "IV0Region_BasisV",
            "IV0Region_BasisW",
            "IV0Local_Min",
            "IV0Local_Max",
            "IV0Local_BorderFade",
            "IV0HeightUnpackScale",
            "IV0TerrainBase",
            "IV0TerrainDeltaConfidence",
            "IV00ColorScale",
            "IV01ColorScale",
            "IV02ColorScale",
            "MinSkyvisValue"
            // } ForwardPassIndirectConstants
        ));

        LOOKUP = knownNames.stream().collect(Collectors.toMap(
            name -> CRC32C.calculate(name.getBytes(StandardCharsets.UTF_8)),
            Function.identity()
        ));
    }

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final String name = getText(type, value);
            if (LOOKUP.containsKey(((Number) value).intValue())) {
                component.append("\"%s\"".formatted(name), CommonTextAttributes.STRING_TEXT_ATTRIBUTES);
            } else {
                component.append(name, CommonTextAttributes.NUMBER_ATTRIBUTES);
            }
        };
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        final int hash = ((Number) value).intValue();
        final String name = LOOKUP.get(hash);

        if (name != null) {
            return name;
        } else {
            return IOUtils.toHexDigits(hash, ByteOrder.BIG_ENDIAN);
        }
    }
}
