#version 150 core
#line 2 0



uniform sampler2DArray itemTextures;


in vec4 color;
in vec4 texcoord;
flat in uint idx;
 
out vec4 out_Color;
 
void main(void) {
	vec4 tex = textureLod(itemTextures, vec3(texcoord.st, idx), 0);
	// vec4 tex = texture(itemTextures, vec3(texcoord.st, idx));
    if (tex.a < 1.0)
    	discard;
    out_Color = tex*color;
}
