Shader "Enigma/Dementor Mist 2D"
{
    Properties
    {
        _Density("Densidad", Range(0, 2)) = 0.8
        _Softness("Suavidad del borde", Range(1, 5)) = 2.5
        _FlowSpeed("Movimiento de la niebla", Range(0, 1)) = 0.18
    }

    SubShader
    {
        Tags
        {
            "Queue" = "Transparent"
            "RenderType" = "Transparent"
            "RenderPipeline" = "UniversalPipeline"
        }

        Blend SrcAlpha OneMinusSrcAlpha
        Cull Off
        ZWrite Off

        HLSLINCLUDE
        #include "Packages/com.unity.render-pipelines.universal/ShaderLibrary/Core.hlsl"

        CBUFFER_START(UnityPerMaterial)
            float _Density;
            float _Softness;
            float _FlowSpeed;
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

        // @author koldo. Niebla procedural sin texturas externas ni bordes de quad visibles.
        float AuraHash(float2 coordinates)
        {
            return frac(sin(dot(coordinates, float2(127.1, 311.7))) * 43758.5453);
        }

        float AuraNoise(float2 coordinates)
        {
            float2 cell = floor(coordinates);
            float2 local = frac(coordinates);
            float2 blend = local * local * (3.0 - 2.0 * local);

            return lerp(
                lerp(AuraHash(cell), AuraHash(cell + float2(1, 0)), blend.x),
                lerp(AuraHash(cell + float2(0, 1)), AuraHash(cell + float2(1, 1)), blend.x),
                blend.y);
        }

        // @author koldo. Tres escalas de detalle evitan manchas circulares uniformes.
        float AuraCloud(float2 coordinates)
        {
            return AuraNoise(coordinates) * 0.57
                + AuraNoise(coordinates * 2.07 + 13.7) * 0.29
                + AuraNoise(coordinates * 4.13 + 31.2) * 0.14;
        }

        Varyings MistVertex(Attributes input)
        {
            Varyings output;
            output.positionCS = TransformObjectToHClip(input.positionOS.xyz);
            output.uv = input.uv;
            output.color = input.color;
            return output;
        }

        half4 MistFragment(Varyings input) : SV_Target
        {
            float2 centered = input.uv * 2.0 - 1.0;
            float2 drift = float2(_Time.y * _FlowSpeed, -_Time.y * _FlowSpeed * 0.7);
            float2 distortion = float2(
                AuraNoise(input.uv * 3.0 + drift),
                AuraNoise(input.uv * 3.0 - drift + 17.3)) - 0.5;
            float cloud = AuraCloud(input.uv * 4.5 + distortion * 1.4 + drift);
            float radius = length(centered + distortion * 0.3);
            float edge = pow(saturate(1.0 - radius * radius), _Softness);
            float wisps = smoothstep(0.22, 0.75, cloud);
            half alpha = saturate(input.color.a * _Density * edge * wisps);
            half3 tint = input.color.rgb * lerp(0.7, 1.2, cloud);
            return half4(tint, alpha);
        }
        ENDHLSL

        Pass
        {
            Name "Mist2D"
            Tags { "LightMode" = "Universal2D" }
            HLSLPROGRAM
            #pragma vertex MistVertex
            #pragma fragment MistFragment
            ENDHLSL
        }

        Pass
        {
            Name "MistForward"
            Tags { "LightMode" = "UniversalForward" }
            HLSLPROGRAM
            #pragma vertex MistVertex
            #pragma fragment MistFragment
            ENDHLSL
        }
    }
}
