Shader "Enigma/Dementor Shadow Trail 2D"
{
    Properties
    {
        _MainTex("Dementor silhouette", 2D) = "white" {}
        _Softness("Soft silhouette edge (texels)", Range(0,5)) = 2
    }
    SubShader
    {
        Tags { "Queue" = "Transparent" "RenderType" = "Transparent" "RenderPipeline" = "UniversalPipeline" }
        Blend SrcAlpha OneMinusSrcAlpha
        Cull Off
        ZWrite Off

        HLSLINCLUDE
        #include "Packages/com.unity.render-pipelines.universal/ShaderLibrary/Core.hlsl"
        TEXTURE2D(_MainTex);
        SAMPLER(sampler_MainTex);
        CBUFFER_START(UnityPerMaterial)
            float4 _MainTex_TexelSize;
            float _Softness;
        CBUFFER_END
        struct Attributes
        {
            float4 positionOS : POSITION;
            float2 uv : TEXCOORD0;
            half4 color : COLOR;
        };
        struct Varyings
        {
            float4 positionCS : SV_POSITION;
            float2 uv : TEXCOORD0;
            half4 color : COLOR;
        };
        Varyings ShadowVertex(Attributes input)
        {
            Varyings output;
            output.positionCS = TransformObjectToHClip(input.positionOS.xyz);
            output.uv = input.uv;
            output.color = input.color;
            return output;
        }
        // @author koldo. Silueta de humo sin colores de la ropa ni brillo propio.
        half4 ShadowFragment(Varyings input) : SV_Target
        {
            float2 offset = _MainTex_TexelSize.xy * _Softness;
            half alpha = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv).a * 0.4;
            alpha += SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv + float2(offset.x, 0)).a * 0.15;
            alpha += SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv - float2(offset.x, 0)).a * 0.15;
            alpha += SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv + float2(0, offset.y)).a * 0.15;
            alpha += SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv - float2(0, offset.y)).a * 0.15;
            return half4(input.color.rgb, alpha * input.color.a);
        }
        ENDHLSL
        Pass
        {
            Name "Shadow2D"
            Tags { "LightMode" = "Universal2D" }
            HLSLPROGRAM
            #pragma vertex ShadowVertex
            #pragma fragment ShadowFragment
            ENDHLSL
        }
        Pass
        {
            Name "ShadowForward"
            Tags { "LightMode" = "UniversalForward" }
            HLSLPROGRAM
            #pragma vertex ShadowVertex
            #pragma fragment ShadowFragment
            ENDHLSL
        }
    }
}
