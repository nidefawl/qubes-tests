#version 150 core
#line 2 0

#line 1 1

layout(std140) uniform uboMatrix3D
{
    mat4 mvp;
    mat4 mv;
    mat4 view;
    mat4 vp;
    mat4 p;
    mat4 normal;
    mat4 mv_inv;
    mat4 proj_inv;
    mat4 mvp_inv;
} in_matrix_3D;

layout(std140) uniform uboMatrix2D
{
    mat4 mvp;
    mat4 p3DOrtho;
    mat4 mv3DOrtho;
} in_matrix_2D;

layout(std140) uniform uboMatrixShadow
{
    mat4 shadow_split_mvp[4];
    vec4 shadow_split_depth;
} in_matrix_shadow;

layout(std140) uniform uboSceneData
{
    vec4 cameraPosition;
    vec4 framePos;
    vec4 viewport;
    vec4 pxoffset;
    vec4 prevCameraPosition;
    vec4 sceneSettings;
} in_scene;

#define FRAME_TIME in_scene.framePos.w
#define CAMERA_POS in_scene.cameraPosition.xyz
#define PREV_CAMERA_POS in_scene.prevCameraPosition.xyz
#define RENDER_OFFSET in_scene.framePos.xyz
#define PX_OFFSET in_scene.pxoffset
#line 1 2
// Simplex 2D noise
// Unknown author
vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }

float snoise(vec2 v){
  const vec4 C = vec4(0.211324865405187, 0.366025403784439,
           -0.577350269189626, 0.024390243902439);
  vec2 i  = floor(v + dot(v, C.yy) );
  vec2 x0 = v -   i + dot(i, C.xx);
  vec2 i1;
  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);
  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
  + i.x + vec3(0.0, i1.x, 1.0 ));
  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
    dot(x12.zw,x12.zw)), 0.0);
  m = m*m ;
  m = m*m ;
  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}
vec3 pal( in float t, in vec3 a, in vec3 b, in vec3 c, in vec3 d )
{
    return a + b*cos( 6.28318*(c*t+d) );
}

void colorizeLeaves(inout vec3 color_adj, in vec3 position) {


    float idOffset = 0;

      float sampleDist = 4.4;
      vec2 p0 = position.xz *0.02;
      // float fSin = sin(FRAME_TIME*0.0003)*0.5+0.5;
      // p0 += vec2(fSin*110.3);
      vec2 p1 = p0 + vec2(1, 0)*sampleDist;
      vec2 p2 = p0 + vec2(0, 1)*sampleDist;
      float s0 = snoise(p0);
      float s1 = snoise(p1);
      float s2 = snoise(p2);
      color_adj*=pal((s0+s1+s2)/3.0, 
              vec3(0.4+(idOffset/5.0)*0.5,0.88,0.08)*(0.27+(idOffset/10.0)),
              vec3(0.1-clamp(1-idOffset/4.0,0,1)*0.07),
              vec3(0.15),
              vec3(0.15)  )*1.2;
}
#line 1 3

#define BLOCK_ID_u32(blockinfo32) ((blockinfo32 >> 16u) & 0xFFFu)
#define BLOCK_RENDERPASS_u32(blockinfo32) ((blockinfo32 >> 28u) & 0xFu)
#define BLOCK_TEX_SLOT_u32(blockinfo32) float(blockinfo32&0xFFFu)
#define BLOCK_NORMAL_SLOT_u32(blockinfo32) float((blockinfo32&0xF000u)>>12u)

#define BLOCK_ID(blockinfo) (blockinfo.y&0xFFFu)
#define BLOCK_RENDERPASS(blockinfo) float((blockinfo.y&0xF000u)>>12u)
#define BLOCK_TEX_SLOT(blockinfo) float(blockinfo.x&0xFFFu)
#define BLOCK_NORMAL_SLOT(blockinfo) float((blockinfo.x&0xF000u)>>12u)
#define BLOCK_FACEDIR(blockinfo) (blockinfo.w&0x7u)
#define BLOCK_VERTDIR(blockinfo) ((blockinfo.w >> 3u) & 0x3Fu)
#define BLOCK_AO_IDX_0(blockinfo) (in_blockinfo.z)&AO_MASK
#define BLOCK_AO_IDX_1(blockinfo) (in_blockinfo.z>>2u)&AO_MASK
#define BLOCK_AO_IDX_2(blockinfo) (in_blockinfo.z>>4u)&AO_MASK
#define BLOCK_AO_IDX_3(blockinfo) (in_blockinfo.z>>6u)&AO_MASK
#define IS_SKY(blockid) float(blockid==0u)
#define IS_WATER(blockid) float(blockid==10u)

#define IS_LIGHT(blockid) float(blockid==2222u)

#define IS_ILLUM(renderpass) float(renderpass==4)
#define IS_BACKFACE(renderpass) float(renderpass==3)

#define ENCODE_RENDERPASS(renderpass) ((uint(renderpass)&0xFu)<<12u)
#line 1 4
const float A = 0.15;
const float B = 0.50;
const float C = 0.10;
const float D = 0.20;
const float E = 0.02;
const float F = 0.30;
const float W = 11.2;

#define SRGB_TEXTURES

void srgb(inout float v)
{
    v = clamp(v, 0.0, 1.0);
    float K0 = 0.03928;
    float a = 0.055;
    float phi = 12.92;
    float gamma = 2.4;
    v = v <= K0 / phi ? v * phi : (1.0 + a) * pow(v, 1.0 / gamma) - a;
}
void linear(inout float v)
{
    v = clamp(v, 0.0, 1.0);
    float K0 = 0.03928;
    float a = 0.055;
    float phi = 12.92;
    float gamma = 2.4;
    v = v <= K0 ? v / phi : pow((v + a) / (1.0 + a), gamma);
}
void linearizeInput(inout vec3 srgb)
{
    #ifndef SRGB_TEXTURES
    linear(srgb.x);
    linear(srgb.y);
    linear(srgb.z);
    #endif
}

void srgbToLin(inout vec3 srgb) {
    linear(srgb.x);
    linear(srgb.y);
    linear(srgb.z);
}
void linToSrgb(inout vec3 linear) {
    srgb(linear.x);
    srgb(linear.y);
    srgb(linear.z);
}

vec3 Uncharted2Tonemap(vec3 x)
{
    // http://www.gdcvault.com/play/1012459/Uncharted_2__HDR_Lighting
    // http://filmicgames.com/archives/75 - the coefficients are from here
    return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F; // E/F = Toe Angle
}

#define MAX_COLOR_RANGE 2.0//TONEMAP   
vec3 Uncharted2Tonemap2(vec3 x) {
float A2 = .8;    //brightness multiplier
float B2 = 0.37;   //black level (lower means darker and more constrasted, higher make the image whiter and less constrasted)
float C2 = 0.1;    //constrast level 
  float D2 = 0.2;    
  float E2 = 0.02;
  float F2 = 0.3;
  float W2 = MAX_COLOR_RANGE;
  return ((x*(A2*x+C2*B2)+D2*E2)/(x*(A2*x+B2)+D2*F2))-E2/F2;
}


vec3 ToneMap( in vec3 texColor, float exposure)
{

  vec3 curr = Uncharted2Tonemap2(exposure*texColor);
  
  vec3 whiteScale = 1.0f/Uncharted2Tonemap2(vec3(MAX_COLOR_RANGE));
 vec3 color = curr*whiteScale;
   // vec3 curr = Uncharted2Tonemap(exposure*texColor);

   // vec3 whiteScale = 1.0f/Uncharted2Tonemap(vec3(W));
   // vec3 color = curr*whiteScale;

   // vec3 retColor = pow(color,vec3(1/2.2));
   linToSrgb(color);
   return color;
}
#line 7 0


uniform sampler2DArray blockTextures;
uniform sampler2D noisetex;


in vec4 color;
in vec3 normal;
in vec4 texcoord;
in vec2 texPos;
flat in uvec4 blockinfo;
in float Idiff;

out vec4 out_Color;
 
void main(void) {
	vec4 tex=texture(blockTextures, vec3(texcoord.st, BLOCK_TEX_SLOT(blockinfo)), -100);
	if (tex.a<1.0)
		discard;
	vec3 color_adj = tex.rgb;
	vec3 color_adj2 = color.rgb;
	// srgbToLin(color_adj.rgb);
	srgbToLin(color_adj2.rgb);
	color_adj *= color_adj2.rgb;
	color_adj *= Idiff * 1.8;
	vec3 toneMapped = ToneMap(color_adj, 2.0f);
	out_Color = vec4(toneMapped, tex.a);
}
