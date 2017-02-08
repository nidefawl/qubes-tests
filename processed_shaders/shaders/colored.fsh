#version 150 core
#line 2 0


in vec4 pass_Color;

out vec4 out_Color;
 
void main(void) {
    out_Color = pass_Color;
}
