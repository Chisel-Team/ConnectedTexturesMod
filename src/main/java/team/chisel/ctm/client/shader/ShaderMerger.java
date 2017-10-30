package team.chisel.ctm.client.shader;

/**
 * A whole log of stuff to make all the shaders work together in peace and harmony
 *
 * After shader is bound, and has the attribute variable "shaderIndex", we have to use glGetAttribLocation to get the location.
 * We then must construct a custom VertexFormatElement with this location, and use that to store any data we want for that variable
 *
 * For Fragment shaders, we have to do some sort of "flat varying int shaderIndexVary"
 * Vertex shader sets shaderIndexVary varying, then fragment shader uses it
 *
 * We can generate dynamic if statements to check the int values, which represent ids of registered shaders in a master registry.
 *
 * Every shader is registered, and used to generate the if statement, and every block that uses a shader has the generic data of the id of the shader it uses
 */
public class ShaderMerger {

    private static final String ATTRIBUTE_DEFINE = "attribute int shaderIndex;";
    private static final String VARYING_DEFINE = "flat varying int shaderIndexVary;";




}
