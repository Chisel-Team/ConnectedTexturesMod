package team.chisel.ctm.client.shader;

public enum EnumShaderType {
    VERTEX("vertex", ".vsh"),
    FRAGMENT("fragment", ".fsh");

    private final String name;

    private final String extension;

    EnumShaderType(String name, String extension){
        this.name = name;
        this.extension = extension;
    }

    public String getName(){
        return this.name;
    }

    public String getExtension(){
        return this.extension;
    }
}
