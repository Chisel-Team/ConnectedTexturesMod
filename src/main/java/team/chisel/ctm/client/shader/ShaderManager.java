package team.chisel.ctm.client.shader;

import java.util.List;

/**
 * Manages all the shaders
 */
public class ShaderManager {

    public static final ShaderManager INSTANCE = new ShaderManager();

    private ShaderManager(){}

    private List<ShaderEntry> vertShaders;

    private List<ShaderEntry> fragShaders;

    /**
     * Represents a shader entry
     */
    public static class ShaderEntry {

        /**
         * The raw shader text.
         */
        private String shader;

        /**
         * The numbers of valid target vertex attributes to apply to
         */
        private int[] targets;

        public ShaderEntry(String shader, int... targets){
            this.shader = shader;
            this.targets = targets;
        }
    }

    public String compile(){
        for (ShaderEntry vertShader : vertShaders){
            
        }
    }
}
