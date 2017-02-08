#version 150 core
#line 2 0

in Data{
    vec3 color;
} gdata;

out vec4 out_Color;

void main(){
	//There is some weird color interpolation going on
	// between geometry shader and fragment shader
	// color values between 0,1 are reinterpolated and land outside 0,1

	// i have no clue whats going on, added this bit of code to clamp it back
	vec4 colorSample = clamp(vec4(gdata.color, 1), vec4(0), vec4(1));
    out_Color = colorSample;
}
