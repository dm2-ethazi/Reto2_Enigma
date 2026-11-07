Shader "Enigma/Dementor Cloth 2D"
{
    Properties
    {
        _Exposure("Cloth brightness", Range(0.5,2)) = 1.35
        _ShadowLift("Shadow detail", Range(0,1)) = 0.42
        _ClothTint("Cloth tint", Color) = (0.76,0.8,0.87,1)
        _Opacity("Opacity", Range(0,1)) = 1
        _Fabric("Fabric detail", Range(0,1)) = 0.88
        _Relief("Fold relief", Range(0,2)) = 0.55
        _Sheen("Cloth sheen", Range(0,1)) = 0.16
        _EdgeTint("Cold edge tint", Color) = (0.25,0.40,0.52,1)
        _EdgeStrength("Edge light", Range(0,1)) = 0.10
        _Wind("Cloth drift", Range(0,0.012)) = 0.003
        _WindSpeed("Wind speed", Range(0,3)) = 1.65
        _HemFade("Tattered hem transparency", Range(0,1)) = 0.20
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
            };

            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/Lit2DCommon.hlsl"

            // NOTE: Do not ifdef the properties here as SRP batcher can not handle different layouts.
            CBUFFER_START(UnityPerMaterial)
                half4 _Color;
                half4 _ClothTint;
                half4 _EdgeTint;
                float _Opacity, _Fabric, _Relief, _Sheen;
                float _EdgeStrength, _Wind, _WindSpeed, _HemFade;
                float _Exposure, _ShadowLift;
            CBUFFER_END
            #include "DementorClothCommon.hlsl"


            Varyings LitVertex(Attributes input)
            {
                UNITY_SKINNED_VERTEX_COMPUTE(input);
                SetUpSpriteInstanceProperties();
                input.positionOS = UnityFlipSprite(input.positionOS, unity_SpriteProps.xy);

                Varyings o = CommonLitVertex(input);
                o.color = input.color * _Color * unity_SpriteColor;

                return o;
            }

            half4 LitFragment(Varyings input) : SV_Target
            {
                float2 uv=ClothUV(input.uv);
                half4 tex=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv);
                half3 normalTS=ClothNormal(uv,tex);
                half4 main=tex*input.color;
                main.rgb=ClothColor(main.rgb,uv,normalTS);
                main.a=ClothAlpha(main.a,uv);
                half4 mask=SAMPLE_TEXTURE2D(_MaskTex,sampler_MaskTex,uv);
                SurfaceData2D surfaceData;
                InputData2D inputData;
                InitializeSurfaceData(main.rgb,main.a,mask,normalTS,surfaceData);
                InitializeInputData(uv,input.lightingUV,inputData);
                half4 result=CombinedShapeLightShared(surfaceData,inputData);
                // Gentle ambient bounce retains the painted folds in the darkest areas.
                result.rgb=max(result.rgb,main.rgb*_ShadowLift);
                half3 key=normalize(half3(-0.45,0.6,1));
                half sheen=pow(saturate(dot(normalTS,normalize(key+half3(0,0,1)))),12);
                result.rgb+=main.rgb*sheen*_Sheen*0.22;
                float2 d=_MainTex_TexelSize.xy*2;
                half nearby=min(
                    SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv+float2(d.x,0)).a,
                    SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv+float2(0,d.y)).a);
                half edge=saturate(tex.a-nearby)*tex.a;
                half foldRim=pow(saturate(1-normalTS.z),1.5);
                result.rgb+=_EdgeTint.rgb*_EdgeStrength*(edge*0.75+foldRim*0.25);
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
            };

            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/Normals2DCommon.hlsl"

            // NOTE: Do not ifdef the properties here as SRP batcher can not handle different layouts.
            CBUFFER_START( UnityPerMaterial )
                half4 _Color;
                half4 _ClothTint;
                half4 _EdgeTint;
                float _Opacity, _Fabric, _Relief, _Sheen;
                float _EdgeStrength, _Wind, _WindSpeed, _HemFade;
                float _Exposure, _ShadowLift;
            CBUFFER_END
            #include "DementorClothCommon.hlsl"

            Varyings NormalsRenderingVertex(Attributes input)
            {
                UNITY_SKINNED_VERTEX_COMPUTE(input);
                SetUpSpriteInstanceProperties();
                input.positionOS = UnityFlipSprite(input.positionOS, unity_SpriteProps.xy);

                Varyings o = CommonNormalsVertex(input);
                o.color = input.color * _Color * unity_SpriteColor;

                return o;
            }

            half4 NormalsRenderingFragment(Varyings input) : SV_Target
            {
                float2 uv=ClothUV(input.uv);
                half4 tex=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv);
                half4 main=tex*input.color;
                main.a=ClothAlpha(main.a,uv);
                return NormalsRenderingShared(main,ClothNormal(uv,tex),input.tangentWS,input.bitangentWS,input.normalWS);
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
            };

            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/2DCommon.hlsl"
          
            // GPU Instancing
            #pragma multi_compile_instancing
            #pragma multi_compile _ DEBUG_DISPLAY SKINNED_SPRITE

            // NOTE: Do not ifdef the properties here as SRP batcher can not handle different layouts.
            CBUFFER_START(UnityPerMaterial)
                half4 _Color;
                half4 _ClothTint;
                half4 _EdgeTint;
                float _Opacity, _Fabric, _Relief, _Sheen;
                float _EdgeStrength, _Wind, _WindSpeed, _HemFade;
                float _Exposure, _ShadowLift;
            CBUFFER_END
            #include "DementorClothCommon.hlsl"

            Varyings UnlitVertex(Attributes input)
            {
                UNITY_SKINNED_VERTEX_COMPUTE(input);
                SetUpSpriteInstanceProperties();
                input.positionOS = UnityFlipSprite(input.positionOS, unity_SpriteProps.xy);

                Varyings o = CommonUnlitVertex(input);
                o.color = input.color *_Color * unity_SpriteColor;
                return o;
            }

            half4 UnlitFragment(Varyings input) : SV_Target
            {
                float2 uv=ClothUV(input.uv);
                half4 tex=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv);
                half4 main=tex*input.color;
                main.rgb=ClothColor(main.rgb,uv,ClothNormal(uv,tex));
                main.a=ClothAlpha(main.a,uv);
                return main;
            }
            ENDHLSL
        }
    }
}


