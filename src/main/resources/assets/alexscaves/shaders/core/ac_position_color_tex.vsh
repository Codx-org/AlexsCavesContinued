#version 150

// A copy of vanilla's position_color_tex vertex shader, kept in this mod's namespace.
//
// 1.21 dropped the POSITION_COLOR_TEX vertex format in favour of POSITION_TEX_COLOR and deleted
// assets/minecraft/shaders/core/position_color_tex.vsh with it, so the two irradiated shaders
// could no longer name it. Attribute locations are bound by NAME, not by declaration order, so
// this one file serves both formats: on 1.20.x the JSON's attribute list puts Color at index 1,
// on 1.21+ the vertex format puts UV0 there, and either way each `in` below lands on the index
// the game bound for its name.

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;
}
