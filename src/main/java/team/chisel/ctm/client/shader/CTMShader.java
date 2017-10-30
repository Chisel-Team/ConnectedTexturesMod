package team.chisel.ctm.client.shader;

import java.util.List;

/**
 * Represents a shader
 */
public class CTMShader {

    private final String shaderSource;

    private final EnumShaderType type;

    private List<String> uniforms;

    private String[] shaderLines;

    public CTMShader(String source, EnumShaderType type){
        this.shaderSource = source;
        this.type = type;
        this.shaderLines = shaderSource.split("\n");
    }

    private void fillUniforms(){
        for (int i = 0 ; i < shaderLines.length ; i++ ){
            if (shaderLines[i].startsWith("uniform")){
                String[] split = shaderLines[i].split(" ");
                if (split[2].endsWith(";")){
                    this.uniforms.add(split[2].substring(0, split[2].length() - 1));
                }
                else {
                    this.uniforms.add(split[2]);
                }
                shaderLines[i] = null;
            }
        }
    }
}
