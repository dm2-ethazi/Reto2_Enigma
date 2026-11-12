#ifndef ENIGMA_CASTLE_SURFACE_INCLUDED
#define ENIGMA_CASTLE_SURFACE_INCLUDED

float4 _MainTex_TexelSize;

// @author koldo. Datos compartidos para que relieve, cavidades e iluminacion sean coherentes.
struct CastleSurface
{
    half3 normalTS;
    half cavity;
};

// @author koldo. Escorzo de las losas: las filas se comprimen hacia la pared.
float2 CastleSurfaceUV(float2 uv)
{
    float projectedY = uv.y / lerp(2.6, 1.0, saturate(uv.y));
    uv.y = lerp(uv.y, projectedY, _FloorPlane);
    return uv;
}

half CastleHeight(half4 color)
{
    return dot(color.rgb, half3(0.2126, 0.7152, 0.0722));
}

CastleSurface CastleEvaluateSurface(float2 uv, half4 center)
{
    float2 fineOffset = _MainTex_TexelSize.xy * 1.5;
    float2 broadOffset = _MainTex_TexelSize.xy * _DepthScale;
    half4 left = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv - float2(fineOffset.x, 0));
    half4 right = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv + float2(fineOffset.x, 0));
    half4 bottom = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv - float2(0, fineOffset.y));
    half4 top = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv + float2(0, fineOffset.y));
    half4 broadLeft = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv - float2(broadOffset.x, 0));
    half4 broadRight = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv + float2(broadOffset.x, 0));
    half4 broadBottom = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv - float2(0, broadOffset.y));
    half4 broadTop = SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv + float2(0, broadOffset.y));

    half2 fineSlope = half2(
        (CastleHeight(right) - CastleHeight(left)) * min(left.a, right.a),
        (CastleHeight(top) - CastleHeight(bottom)) * min(top.a, bottom.a));
    half2 broadSlope = half2(
        (CastleHeight(broadRight) - CastleHeight(broadLeft)) * min(broadLeft.a, broadRight.a),
        (CastleHeight(broadTop) - CastleHeight(broadBottom)) * min(broadTop.a, broadBottom.a));
    // El alfa solo aporta bisel dentro de los sprites recortados: no expande la silueta.
    half2 bevelSlope = half2(right.a - left.a, top.a - bottom.a) * center.a;
    half2 slope = fineSlope * _Relief * 5.0 + broadSlope * _Volume * 3.5 + bevelSlope * _Bevel * 2.0;
    CastleSurface surface;
    slope.y *= lerp(1.0, 0.45, _FloorPlane);
    surface.normalTS = normalize(half3(-slope, 1));

    half neighbourHeight = (CastleHeight(broadLeft) + CastleHeight(broadRight)
        + CastleHeight(broadBottom) + CastleHeight(broadTop)) * 0.25;
    half opaqueNeighbours = min(min(broadLeft.a, broadRight.a), min(broadBottom.a, broadTop.a));
    // Solo se oscurecen depresiones respecto a sus vecinas, no toda la textura oscura.
    surface.cavity = saturate((neighbourHeight - CastleHeight(center) - 0.015) * 5.0)
        * opaqueNeighbours * _Cavity;
    return surface;
}

half3 CastleNormal(float2 uv)
{
    return CastleEvaluateSurface(uv, SAMPLE_TEXTURE2D(_MainTex, sampler_MainTex, uv)).normalTS;
}

half3 CastleAlbedo(half3 color, float2 world, CastleSurface surface)
{
    half luminance = dot(color, half3(0.2126, 0.7152, 0.0722));
    half crevice = (1.0 - smoothstep(0.07, 0.32, luminance)) * _Crevice;
    half weather = sin(world.x * 0.63 + sin(world.y * 0.81)) * sin(world.y * 0.47 + world.x * 0.21);
    half3 keyDirection = normalize(_VolumeLight.xyz);
    half flatLight = max(keyDirection.z, 0.25);
    half shapedLight = saturate(dot(surface.normalTS, keyDirection));
    // Relieve de luz suave normalizado: una superficie plana conserva su brillo original.
    half volumeLight = lerp(1.0, clamp((0.3 + shapedLight) / (0.3 + flatLight), 0.45, 1.35), _Volume);
    float floorDistance = saturate((_FloorTopY - world.y) / max(_FloorDepth, 0.01));
    float floorContact = exp2(-floorDistance * 16.0) * _ContactShadow;
    float wallDistance = max(world.y - _FloorTopY, 0.0);
    float wallContact = exp2(-wallDistance * 3.5) * _ContactShadow * 0.4;
    half planeShade = lerp(1.0, (1.0 - floorContact) * lerp(0.72, 1.05, floorDistance), _FloorPlane);
    planeShade *= 1.0 - wallContact * _WallFoot;
    // Suelo de pizarra mas neutro: la luz calida de las lamparas sigue iluminandolo.
    half3 floorStone = lerp(luminance.xxx, color, 0.45);
    half3 planeColor = lerp(color, floorStone, _FloorPlane);
    return planeColor * _Tint.rgb * _Exposure * (1.0 - crevice)
        * (1.0 - surface.cavity) * volumeLight * (1.0 + weather * _Variation) * planeShade;
}

half3 CastleEmission(half4 texel, float2 world)
{
    // @author koldo. Solo el vidrio y las llamas calidas emiten luz, conservando cadenas y piedra.
    half warm = smoothstep(0.025, 0.18, texel.r - texel.b) * smoothstep(0.22, 0.52, texel.g);
    half bright = smoothstep(_FlameThreshold, min(_FlameThreshold + 0.22, 0.99), max(texel.r, texel.g));
    half flame = warm * bright * texel.a;
    float time = _Time.y;
    half pulse = 1.0 + _Flicker * (0.55 * sin(time * 7.3 + world.x * 1.2)
        + 0.3 * sin(time * 11.7 + world.y) + 0.15 * sin(time * 19.1));
    return _EmissionColor.rgb * flame * _Emission * pulse;
}

#endif
