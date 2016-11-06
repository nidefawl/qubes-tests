#version 150 core


uniform sampler2D tex0;

in vec4 pass_Color;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
void main(void) {
	vec4 tex = texture(tex0, pass_texcoord.st, 0);
    out_Color = tex*pass_Color;
}