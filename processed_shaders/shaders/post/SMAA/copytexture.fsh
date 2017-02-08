#version 150 core
#line 2 0

#line 1 1
#define SMAA_RT_METRICS float4(1.0 / in_scene.viewport.x, 1.0 / in_scene.viewport.y, in_scene.viewport.x, in_scene.viewport.y)
#define SMAA_GLSL_3 1 
#define SMAA_PRESET_MEDIUM
#define SMAA_PREDICATION 1
#define SMAA_REPROJECTION 0
#define SMAA_DEPTH_THRESHOLD (0.01 * SMAA_THRESHOLD)
#line 4 0

uniform sampler2D texColor;
#if SMAA_PREDICATION
uniform sampler2D texMaterial;
#endif
#if SMAA_REPROJECTION
uniform sampler2D velocityTex;
#endif

in vec2 pass_texcoord;
 
out vec4 out_Color;

#if SMAA_PREDICATION
out vec4 out_FinalMaterial;
#endif

#if SMAA_REPROJECTION
out vec4 out_Velocity;
#endif
 
void main(void) {
	vec4 tex = texture(texColor, pass_texcoord.st, 0);
    out_Color = tex;
#if SMAA_PREDICATION
	vec4 tex2 = texture(texMaterial, pass_texcoord.st, 0);
	out_FinalMaterial = tex2;
#endif
#if SMAA_REPROJECTION
	vec4 tex3 = texture(velocityTex, pass_texcoord.st, 0);
	out_Velocity = tex3;
#endif

}
