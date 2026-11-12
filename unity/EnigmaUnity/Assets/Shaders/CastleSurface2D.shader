Shader "Enigma/Castle Surface 2D"
{
    Properties
    {
        [Header(Surface Plane)]
        _FloorPlane("Horizontal floor treatment", Range(0,1)) = 0
        _WallFoot("Wall base shading", Range(0,1)) = 0
        _FloorTopY("Wall and floor junction (world Y)", Float) = -3.5
        _FloorDepth("Visible floor depth", Float) = 2.5
        _ContactShadow("Junction shadow", Range(0,1)) = 0.6
        _Tint("Surface tint", Color) = (1,1,1,1)
        _Exposure("Surface brightness", Range(0.5,1.8)) = 1
        _Relief("Stone and carved detail", Range(0,4)) = 0.6
        _Volume("Broad surface volume", Range(0,1)) = 0.65
        _DepthScale("Broad relief radius (texels)", Range(2,16)) = 8
        _Cavity("Local cavity shadows", Range(0,0.7)) = 0.3
        _Bevel("Sprite edge bevel", Range(0,1)) = 0.35
        _VolumeLight("Volume light direction", Vector) = (-0.45,0.65,0.8,0)
        _Crevice("Crevice depth", Range(0,0.5)) = 0.12
        _Variation("Surface weathering", Range(0,0.2)) = 0.04
        _Dampness("Damp stone sheen", Range(0,1)) = 0
        _Roughness("Surface roughness", Range(0.1,1)) = 0.8
        _Metallic("Metal accents", Range(0,1)) = 0
        _Emission("Lantern emission", Range(0,4)) = 0
        _FlameThreshold("Flame brightness threshold", Range(0.2,0.95)) = 0.6
        _Flicker("Flame flicker", Range(0,0.2)) = 0.055
        [HDR] _EmissionColor("Lantern glow", Color) = (1,0.62,0.24,1)
        [PerRendererData] _MainTex("Diffuse", 2D) = "white" {}
        _MaskTex("Mask", 2D) = "white" {}
        _NormalMap("Normal Map", 2D) = "bump" {}
        [MaterialToggle] _ZWrite("ZWrite", Float) = 0

        // Legacy properties. They're here so that materials using this shader can gracefully fallback to the legacy sprite shader.
        [HideInInspector] _Color("Tint", Color) = (1,1,1,1)
        [HideInInspector] _RendererColor("RendererColor", Color) = (1,1,1,1)
        [HideInInspector] _AlphaTex("External Alpha", 2D) = "white" {}
        [HideInInspector] _EnableExternalAlpha("Enable External Alpha", Float) = 0
    }

    SubShader
    {
        Tags {"Queue" = "Transparent" "RenderType" = "Transparent" "RenderPipeline" = "UniversalPipeline" }

        Blend SrcAlpha OneMinusSrcAlpha, One OneMinusSrcAlpha
        Cull Off
        ZWrite [_ZWrite]

        Pass
        {
            Tags { "LightMode" = "Universal2D" }

            HLSLPROGRAM
            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/Core2D.hlsl"

            #pragma vertex LitVertex
            #pragma fragment LitFragment

            #include_with_pragmas "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/ShapeLightShared.hlsl"

            // GPU Instancing
            #pragma multi_compile_instancing
            #pragma multi_compile _ DEBUG_DISPLAY
            #pragma multi_compile _ SKINNED_SPRITE

            struct Attributes
            {
                COMMON_2D_INPUTS
                half4 color        : COLOR;
                UNITY_SKINNED_VERTEX_INPUTS
            };

            struct Varyings
            {
                COMMON_2D_LIT_OUTPUTS
                half4 color        : COLOR;
                float2 worldXY : TEXCOORD4;
            };

            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/Lit2DCommon.hlsl"

            // NOTE: Do not ifdef the properties here as SRP batcher can not handle different layouts.
            CBUFFER_START(UnityPerMaterial)
                half4 _Color;
                half4 _Tint, _EmissionColor;
                float _Exposure, _Relief, _Crevice, _Variation;
                float _Dampness, _Roughness, _Metallic, _Emission;
                float _FlameThreshold, _Flicker;
                float _Volume, _DepthScale, _Cavity, _Bevel;
                half4 _VolumeLight;
                float _FloorPlane, _WallFoot, _FloorTopY, _FloorDepth, _ContactShadow;
            CBUFFER_END
            #include "CastleSurfaceCommon.hlsl"

            Varyings LitVertex(Attributes input)
            {
                UNITY_SKINNED_VERTEX_COMPUTE(input);
                SetUpSpriteInstanceProperties();
                input.positionOS = UnityFlipSprite(input.positionOS, unity_SpriteProps.xy);

                Varyings o = CommonLitVertex(input);
                o.color = input.color * _Color * unity_SpriteColor;

                o.worldXY=TransformObjectToWorld(input.positionOS).xy;
                return o;
            }

            half4 LitFragment(Varyings input) : SV_Target
            {
                float2 surfaceUV = CastleSurfaceUV(input.uv);
                half4 tex = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, surfaceUV);
                CastleSurface surface = CastleEvaluateSurface(surfaceUV, tex);
                half3 normalTS = surface.normalTS;
                half4 main=tex*input.color;
                main.rgb=CastleAlbedo(main.rgb, input.worldXY, surface);
                SurfaceData2D surfaceData;
                InputData2D inputData;
                half4 mask=SAMPLE_TEXTURE2D(_MaskTex,sampler_MaskTex,input.uv);
                InitializeSurfaceData(main.rgb,main.a,mask,normalTS,surfaceData);
                InitializeInputData(input.uv,input.lightingUV,inputData);
                half4 result=CombinedShapeLightShared(surfaceData,inputData);
                // Highlights follow existing scene illumination, never creating light in darkness.
                half brightness=dot(result.rgb,half3(.2126,.7152,.0722));
                half3 h=normalize(half3(-.35,.65,1.5));
                half spec=pow(saturate(dot(normalTS,h)),lerp(70,9,_Roughness));
                half wear=.6+.4*sin(input.worldXY.x*2.3+sin(input.worldXY.y*3.1));
                result.rgb+=spec*brightness*(_Dampness*wear*.24+_Metallic*.17)*lerp(half3(.75,.85,1),tex.rgb,_Metallic);
                result.rgb+=CastleEmission(tex,input.worldXY)*input.color.rgb;
                result.a=main.a;
                return result;
            }
            ENDHLSL
        }

        Pass
        {
            Tags { "LightMode" = "NormalsRendering"}

            HLSLPROGRAM
            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/Core2D.hlsl"

            #pragma vertex NormalsRenderingVertex
            #pragma fragment NormalsRenderingFragment

            // GPU Instancing
            #pragma multi_compile_instancing
            #pragma multi_compile _ SKINNED_SPRITE

            struct Attributes
            {
                COMMON_2D_NORMALS_INPUTS
                float4 color        : COLOR;
                UNITY_SKINNED_VERTEX_INPUTS
            };

            struct Varyings
            {
                COMMON_2D_NORMALS_OUTPUTS
                half4   color           : COLOR;
                float2 worldXY : TEXCOORD4;
            };

            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/Normals2DCommon.hlsl"

            // NOTE: Do not ifdef the properties here as SRP batcher can not handle different layouts.
            CBUFFER_START( UnityPerMaterial )
                half4 _Color;
                half4 _Tint, _EmissionColor;
                float _Exposure, _Relief, _Crevice, _Variation;
                float _Dampness, _Roughness, _Metallic, _Emission;
                float _FlameThreshold, _Flicker;
                float _Volume, _DepthScale, _Cavity, _Bevel;
                half4 _VolumeLight;
                float _FloorPlane, _WallFoot, _FloorTopY, _FloorDepth, _ContactShadow;
            CBUFFER_END
            #include "CastleSurfaceCommon.hlsl"

            Varyings NormalsRenderingVertex(Attributes input)
            {
                UNITY_SKINNED_VERTEX_COMPUTE(input);
                SetUpSpriteInstanceProperties();
                input.positionOS = UnityFlipSprite(input.positionOS, unity_SpriteProps.xy);

                Varyings o = CommonNormalsVertex(input);
                o.color = input.color * _Color * unity_SpriteColor;

                o.worldXY=TransformObjectToWorld(input.positionOS).xy;
                return o;
            }

            half4 NormalsRenderingFragment(Varyings input) : SV_Target
            {
                float2 surfaceUV = CastleSurfaceUV(input.uv);
                half4 main = input.color * SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, surfaceUV);
                return NormalsRenderingShared(main,CastleNormal(surfaceUV),input.tangentWS,input.bitangentWS,input.normalWS);
            }
            ENDHLSL
        }

        Pass
        {
            Tags { "LightMode" = "UniversalForward" "Queue"="Transparent" "RenderType"="Transparent"}

            HLSLPROGRAM
            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/Core2D.hlsl"

            #pragma vertex UnlitVertex
            #pragma fragment UnlitFragment

            struct Attributes
            {
                COMMON_2D_INPUTS
                half4 color : COLOR;
                UNITY_SKINNED_VERTEX_INPUTS
            };

            struct Varyings
            {
                COMMON_2D_OUTPUTS
                half4 color : COLOR;
                float2 worldXY : TEXCOORD4;
            };

            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/2DCommon.hlsl"
          
            // GPU Instancing
            #pragma multi_compile_instancing
            #pragma multi_compile _ DEBUG_DISPLAY SKINNED_SPRITE

            // NOTE: Do not ifdef the properties here as SRP batcher can not handle different layouts.
            CBUFFER_START(UnityPerMaterial)
                half4 _Color;
                half4 _Tint, _EmissionColor;
                float _Exposure, _Relief, _Crevice, _Variation;
                float _Dampness, _Roughness, _Metallic, _Emission;
                float _FlameThreshold, _Flicker;
                float _Volume, _DepthScale, _Cavity, _Bevel;
                half4 _VolumeLight;
                float _FloorPlane, _WallFoot, _FloorTopY, _FloorDepth, _ContactShadow;
            CBUFFER_END
            #include "CastleSurfaceCommon.hlsl"

            Varyings UnlitVertex(Attributes input)
            {
                UNITY_SKINNED_VERTEX_COMPUTE(input);
                SetUpSpriteInstanceProperties();
                input.positionOS = UnityFlipSprite(input.positionOS, unity_SpriteProps.xy);

                Varyings o = CommonUnlitVertex(input);
                o.color = input.color *_Color * unity_SpriteColor;
                o.worldXY=TransformObjectToWorld(input.positionOS).xy;
                return o;
            }

            half4 UnlitFragment(Varyings input) : SV_Target
            {
                float2 surfaceUV = CastleSurfaceUV(input.uv);
                half4 tex = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, surfaceUV);
                half4 main=tex*input.color;
                CastleSurface surface = CastleEvaluateSurface(surfaceUV, tex);
                main.rgb = CastleAlbedo(main.rgb, input.worldXY, surface);
                main.rgb+=CastleEmission(tex,input.worldXY)*input.color.rgb;
                return main;
            }
            ENDHLSL
        }
    }
}


