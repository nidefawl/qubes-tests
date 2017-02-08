#version 150 core
#line 2 0


uniform sampler2D tex0;

in vec4 pass_Color;
in vec2 pass_texcoord;
flat in vec3 clipInfo;
flat in float nearPlaneZ;
flat in float farPlaneZ;
 
out vec4 out_Color;
 

float reconstructCSZ(float depthBufferValue) {
    return clipInfo[0] / (depthBufferValue * clipInfo[1] + clipInfo[2]);
}

float Linear01Depth(float depth) {
	float camSpaceZ = reconstructCSZ(depth);
	float clipSpaceZ= (camSpaceZ-nearPlaneZ) / (farPlaneZ-nearPlaneZ);
	return clipSpaceZ;
}

void main(void) {
  float z = texture2D(tex0, pass_texcoord).x;
  float d = pow(1-Linear01Depth(z), 16);
  out_Color = vec4(d, d, d, 1);
}
