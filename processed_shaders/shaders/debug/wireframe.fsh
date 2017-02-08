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




uniform float thickness;
uniform float maxDistance;
in vec4 color;
noperspective in vec3 vposition;
 in vec3 triangle;
noperspective in vec3 normal;

out vec4 out_Color;

float edgeFactor(){
    vec3 d = fwidth(triangle);
    vec3 a3 = smoothstep(vec3(0.0), d*1.5, triangle);
    return min(min(a3.x, a3.y), a3.z);
}

void main() {
    float ftime = FRAME_TIME*0.05;
    // float min_dist = min(min(triangle.x, triangle.y), triangle.z);
    // float edge = 1.0-smoothstep(fwidth(min_dist), 2 * fwidth(min_dist), min_dist);
    // out_Color = vec4(1,0,1, color.a*edge);

    // float edge = 1.-edgeFactor();
    // out_Color = vec4(triangle, color.a*edge);

    float dist = length(vposition);
    // if (dist > 200)
    //     discard;
    float fdistscale = 1.0f-clamp((dist - maxDistance) / 15.0f, 0.0f, 1.0f);
    vec3 d = fwidth(triangle)*fdistscale;
    vec3 tdist = smoothstep(vec3(0.0), d*2.0f, triangle);
    float mixF = min(min(tdist.x, tdist.y), tdist.z);
    if (mixF > thickness)
        discard;
#ifdef ALTERNATE 
    float fMod = floor(mod(ftime, 3));
    vec3 acolor = vec3(0);
    if (abs(normal.x)+abs(normal.z) < 0.1) {
        if (fMod != 0)
            discard;
        acolor+=vec3(1, 0, 0);
    }
    else if (abs(normal.y)+abs(normal.z) < 0.1) {
        if (fMod != 1)
            discard;
        acolor+=vec3(0, 1, 0);
    }
    else if (abs(normal.x)+abs(normal.y) < 0.1) {
        if (fMod != 2)
            discard;
        acolor+=vec3(0, 0, 1);
    }
#else

    // float fMod = floor(mod(ftime, 3));
    vec3 acolor = vec3(0);
    // if (abs(normal.x)+abs(normal.z) < 0.1) {
    //     // if (fMod != 0)
    //     //     discard;
    //     acolor+=vec3(1, 0, 0);
    // }
    // else if (abs(normal.y)+abs(normal.z) < 0.1) {
    //     // if (fMod != 1)
    //     //     discard;
    //     acolor+=vec3(0, 1, 0);
    // }
    // else if (abs(normal.x)+abs(normal.y) < 0.1) {
    //     // if (fMod != 2)
    //     //     discard;
    //     acolor+=vec3(0, 0, 1);
    // }
        acolor+=color.rgb;
#endif
    out_Color = vec4(vec3(acolor), color.a*fdistscale);
}
