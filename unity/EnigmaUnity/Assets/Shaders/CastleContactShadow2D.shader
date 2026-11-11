Shader "Enigma/Castle Contact Shadow 2D"
{
    Properties
    {
        [PerRendererData] _MainTex("Sprite", 2D) = "white" {}
        _ShadowColor("Contact shadow", Color) = (0.012,0.018,0.025,0.38)
        _Softness("Shadow softness (texels)", Range(1,12)) = 5
        [HideInInspector] _Color("Tint", Color) = (1,1,1,1)
        [HideInInspector] _RendererColor("Renderer color", Color) = (1,1,1,1)
    }
    SubShader
    {
        Tags { "Queue" = "Transparent" "RenderType" = "Transparent" "RenderPipeline" = "UniversalPipeline" }
        Blend SrcAlpha OneMinusSrcAlpha
        Cull Off
        ZWrite Off
        Pass
        {
            Tags { "LightMode" = "Universal2D" }
            HLSLPROGRAM
            #pragma vertex ShadowVertex
            #pragma fragment ShadowFragment
            #pragma multi_compile_instancing
            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/Core2D.hlsl"
            struct Attributes
            {
                COMMON_2D_INPUTS
                half4 color : COLOR;
            };
            struct Varyings
            {
                COMMON_2D_OUTPUTS
                half4 color : COLOR;
            };
            #include "Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/2DCommon.hlsl"
            CBUFFER_START(UnityPerMaterial)
                half4 _Color;
                half4 _ShadowColor;
                float _Softness;
            CBUFFER_END
            float4 _MainTex_TexelSize;

            // @author koldo. Sombra estatica preparada en escena; acompana al objeto sin scripts de seguimiento.
            Varyings ShadowVertex(Attributes input)
            {
                SetUpSpriteInstanceProperties();
                input.positionOS = UnityFlipSprite(input.positionOS, unity_SpriteProps.xy);
                Varyings output = CommonUnlitVertex(input);
                output.color = input.color * _Color * unity_SpriteColor;
                return output;
            }

            half4 ShadowFragment(Varyings input) : SV_Target
            {
                float2 offset = _MainTex_TexelSize.xy * _Softness;
                half alpha = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv).a * 0.4;
                alpha += SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv + float2(offset.x, 0)).a * 0.15;
                alpha += SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv - float2(offset.x, 0)).a * 0.15;
                alpha += SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv + float2(0, offset.y)).a * 0.15;
                alpha += SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, input.uv - float2(0, offset.y)).a * 0.15;
                return half4(_ShadowColor.rgb, alpha * _ShadowColor.a * input.color.a);
            }
            ENDHLSL
        }
    }
}
