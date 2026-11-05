#ifndef ENIGMA_DEMENTOR_CLOTH_INCLUDED
#define ENIGMA_DEMENTOR_CLOTH_INCLUDED
float4 _MainTex_TexelSize;
float2 ClothUV(float2 uv)
{
    float hem = pow(saturate(1.0-uv.y),2.2);
    float t = _Time.y * _WindSpeed;
    // Restrict deformation to the loose lower cloth, with no transform changes.
    uv.x += min(_Wind,0.012)*hem*(sin(uv.y*15+t*1.4)+0.35*sin(uv.y*29-t*0.9));
    return clamp(uv,_MainTex_TexelSize.xy,1.0-_MainTex_TexelSize.xy);
}
half ClothHeight(half4 sample)
{
    return dot(sample.rgb,half3(0.2126,0.7152,0.0722));
}
half3 ClothNormal(float2 uv,half4 center)
{
    float2 d=_MainTex_TexelSize.xy*2.0;
    half4 l=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv-float2(d.x,0));
    half4 r=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv+float2(d.x,0));
    half4 b=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv-float2(0,d.y));
    half4 t=SAMPLE_TEXTURE2D(_MainTex,sampler_MainTex,uv+float2(0,d.y));
    // Ignore transparent neighbours so the sprite outline does not become a hard bevel.
    half dx=(ClothHeight(r)-ClothHeight(l))*min(l.a,r.a);
    half dy=(ClothHeight(t)-ClothHeight(b))*min(t.a,b.a);
    return normalize(half3(-dx*_Relief*11,-dy*_Relief*11,1));
}
half ClothAlpha(half a,float2 uv)
{
    float hem=pow(saturate(1-uv.y),2.4);
    float wisp=0.65+0.35*sin(uv.x*37+uv.y*19+_Time.y*_WindSpeed);
    return a*_Opacity*_ClothTint.a*(1-_HemFade*hem*wisp);
}
half3 ClothColor(half3 rgb,float2 uv,half3 normalTS)
{
    float density=110;
    float filter=1-smoothstep(0.2,0.65,max(fwidth(uv.x),fwidth(uv.y))*density);
    half weave=sin(uv.x*density*6.283185)*sin(uv.y*density*6.283185);
    half fold=saturate(dot(normalTS,normalize(half3(-0.4,0.65,1))));
    return rgb*_ClothTint.rgb*_Exposure*(0.92+0.14*fold)*(1+weave*_Fabric*0.075*filter);
}
#endif
