#version 150 core
#line 2 0

uniform usampler2D tex0;

in vec2 pass_texcoord;
 
out vec4 out_Color;

void main(void) {
  uvec4 blockdata = texture(tex0, pass_texcoord);
  vec3 blockF = clamp(vec3(blockdata.rgb)/10.0, vec3(0), vec3(1));
  out_Color = vec4(blockF, 1);
}
