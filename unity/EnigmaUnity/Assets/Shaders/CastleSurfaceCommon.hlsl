#ifndef ENIGMA_CASTLE_SURFACE_INCLUDED
#define ENIGMA_CASTLE_SURFACE_INCLUDED
float4 _MainTex_TexelSize;
half CastleHeight(half4 c){return dot(c.rgb,half3(.2126,.7152,.0722));}
half3 CastleNormal(float2 uv)
{
    float2 d=_MainTex_TexelSize.xy*1.5;
    half4 l=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv-float2(d.x,0));
    half4 r=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv+float2(d.x,0));
    half4 b=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv-float2(0,d.y));
    half4 t=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv+float2(0,d.y));
    half dx=(CastleHeight(r)-CastleHeight(l))*min(l.a,r.a);
    half dy=(CastleHeight(t)-CastleHeight(b))*min(t.a,b.a);
    return normalize(half3(-dx*_Relief*8,-dy*_Relief*8,1));
}
half3 CastleAlbedo(half3 color,float2 world,half3 normalTS)
{
    half lum=dot(color,half3(.2126,.7152,.0722));
    half crevice=(1-smoothstep(.07,.32,lum))*_Crevice;
    half weather=sin(world.x*.63+sin(world.y*.81))*sin(world.y*.47+world.x*.21);
    // Broad surface variation avoids noisy patterns and does not change the sprite silhouette.
    return color*_Tint.rgb*_Exposure*(1-crevice)*(1+weather*_Variation);
}
half3 CastleEmission(half4 tex,float2 world)
{
    // Select bright warm pixels: flame and glass glow while chains and stone stay solid.
    half warm=smoothstep(.025,.18,tex.r-tex.b)*smoothstep(.22,.52,tex.g);
    half bright=smoothstep(_FlameThreshold,min(_FlameThreshold+.22,.99),max(tex.r,tex.g));
    half flame=warm*bright*tex.a;
    float t=_Time.y;
    half pulse=1+_Flicker*(.55*sin(t*7.3+world.x*1.2)+.3*sin(t*11.7+world.y)+.15*sin(t*19.1));
    return _EmissionColor.rgb*flame*_Emission*pulse;
}
#endif
