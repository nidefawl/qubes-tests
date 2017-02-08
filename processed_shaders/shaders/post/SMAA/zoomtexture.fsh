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
#line 4 0
uniform sampler2D texColor;
uniform vec2 mousePixelPos;

in vec2 pass_texcoord;
out vec4 out_Color;
 
void main(void) {
	vec2 pixelPos = pass_texcoord.st*in_scene.viewport.xy;
	float scale = 8;
	int pixels = 64;
	vec2 rect = vec2(pixels*scale);
	vec2 pos = vec2(in_scene.viewport.x-1-rect.x, 0);
	vec4 minmax = vec4(pos.xy, pos.xy+rect.xy);
	if (pixelPos.x < minmax.x || pixelPos.y < minmax.y) {
		discard;
	}
	if (pixelPos.x >= minmax.z || pixelPos.y >= minmax.w) {
		discard;
	}
	pixelPos.xy -= minmax.xy+(minmax.zw-minmax.xy)*0.5;
	float scale2 = 1.0/8.0;
	ivec2 pixel = ivec2(pixelPos*scale2+mousePixelPos);
	// vec2 newTexcoord = vec2(pixel/in_scene.viewport.xy);
	vec4 tex = vec4(0);
	if (pixel.x >= 0 && pixel.y >= 0) {
		tex = texelFetch (texColor, pixel, 0);
	}
    out_Color = vec4(tex);
}
