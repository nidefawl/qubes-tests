#version 150 core
#line 2 0

uniform sampler2D texColor;
in vec2 pass_texcoord;
 
out vec4 out_Color;
 
void main(void) {
	vec4 tex = texture(texColor, pass_texcoord.st);
    out_Color = vec4(tex.a);

}
