#ifndef ENIGMA_DEMENTOR_CLOTH_INCLUDED
#define ENIGMA_DEMENTOR_CLOTH_INCLUDED

float4 _MainTex_TexelSize;

// @author koldo. Ruido continuo para corrientes y transparencias sin pulsos uniformes.
float ClothNoise(float2 coordinates)
{
    float2 cell = floor(coordinates);
    float2 fraction = frac(coordinates);
    float2 blend = fraction * fraction * (3.0 - 2.0 * fraction);
    float4 corners = float4(
        dot(cell, float2(127.1, 311.7)),
        dot(cell + float2(1, 0), float2(127.1, 311.7)),
        dot(cell + float2(0, 1), float2(127.1, 311.7)),
        dot(cell + 1.0, float2(127.1, 311.7)));
    float4 values = frac(sin(corners) * 43758.5453);
    return lerp(lerp(values.x, values.y, blend.x), lerp(values.z, values.w, blend.x), blend.y);
}

float2 ClothUV(float2 uv)
{
    float hem = pow(saturate(1.0 - uv.y), 2.2);
    float time = _Time.y * _WindSpeed;
    float current = ClothNoise(float2(uv.y * 5.0, time * 0.35)) * 2.0 - 1.0;
    float flutter = sin(uv.y * 24.0 - time * 1.2 + current * 2.0);
    // La capucha permanece estable; las corrientes afectan al tejido suelto.
    uv.x += min(_Wind, 0.012) * hem * (current + flutter * 0.3);
    return clamp(uv, _MainTex_TexelSize.xy, 1.0 - _MainTex_TexelSize.xy);
}

half ClothHeight(half4 sample)
{
    return dot(sample.rgb, half3(0.2126, 0.7152, 0.0722));
}

half3 ClothNormal(float2 uv, half4 center)
{
    float2 offset = _MainTex_TexelSize.xy * 2.0;
    half4 left = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv - float2(offset.x, 0));
    half4 right = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv + float2(offset.x, 0));
    half4 bottom = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv - float2(0, offset.y));
    half4 top = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv + float2(0, offset.y));
    // Las zonas transparentes no deben producir un bisel artificial.
    half dx = (ClothHeight(right) - ClothHeight(left)) * min(left.a, right.a);
    half dy = (ClothHeight(top) - ClothHeight(bottom)) * min(top.a, bottom.a);
    return normalize(half3(-dx * _Relief * 11.0, -dy * _Relief * 11.0, 1));
}

half ClothAlpha(half alpha, float2 uv)
{
    float hem = pow(saturate(1.0 - uv.y), 1.6);
    float wisps = ClothNoise(float2(uv.x * 19.0, uv.y * 9.0 - _Time.y * _SpectralSpeed));
    float erosion = smoothstep(0.25, 0.8, wisps);
    // @author koldo. La erosion se concentra en bordes suaves y faldones, preservando el torso.
    float detail = ClothNoise(float2(uv.x * 43.0, uv.y * 17.0 - _Time.y * _SpectralSpeed * 1.4));
    float ragged = lerp(wisps, detail, 0.35);
    float edgeMask = (1.0 - smoothstep(0.45, 0.95, alpha)) * smoothstep(0.0, 0.08, alpha);
    float wear = edgeMask * smoothstep(0.2, 0.75, ragged) * _TatterStrength;
    return alpha * _Opacity * _ClothTint.a
        * (1.0 - _HemFade * hem * erosion) * (1.0 - wear * hem);
}

// @author koldo. Luz espectral limitada al interior del contorno, sin halo rectangular.
half3 ClothSpectralEdge(float2 uv, half alpha, half3 normalTS)
{
    float2 offset = _MainTex_TexelSize.xy * 2.0;
    half horizontal = min(
        SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv - float2(offset.x, 0)).a,
        SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv + float2(offset.x, 0)).a);
    half vertical = min(
        SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv - float2(0, offset.y)).a,
        SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv + float2(0, offset.y)).a);
    half edge = saturate(alpha - min(horizontal, vertical)) * alpha;
    float flow = ClothNoise(float2(uv.x * 7.0, uv.y * 6.0 - _Time.y * _SpectralSpeed));
    half foldRim = pow(saturate(1.0 - normalTS.z), 1.5);
    half energy = lerp(1.0, 0.45 + flow * 1.1, _SpectralStrength);
    // Reflejo interrumpido por sombra: evita el contorno azul de aspecto neon.
    half brokenEdge = smoothstep(0.35, 0.8, flow);
    return _EdgeTint.rgb * _EdgeStrength
        * (edge * brokenEdge * 0.35 + foldRim * 0.25) * energy;
}

half3 ClothColor(half3 rgb, float2 uv, half3 normalTS)
{
    float density = 110.0;
    float filter = 1.0 - smoothstep(0.2, 0.65, max(fwidth(uv.x), fwidth(uv.y)) * density);
    half weave = sin(uv.x * density * 6.283185) * sin(uv.y * density * 6.283185);
    half fold = saturate(dot(normalTS, normalize(half3(-0.4, 0.65, 1))));
    // @author koldo. Curva de carbon conserva los pliegues claros y profundiza el interior de la capucha.
    half luminance = ClothHeight(half4(rgb, 1));
    half3 charcoal = lerp(luminance.xxx, rgb, 0.35);
    charcoal = lerp(charcoal, charcoal * charcoal * 1.35, _ShadowDepth);
    return charcoal * _ClothTint.rgb * _Exposure * (0.88 + 0.18 * fold)
        * (1.0 + weave * _Fabric * 0.045 * filter);
}

#endif
