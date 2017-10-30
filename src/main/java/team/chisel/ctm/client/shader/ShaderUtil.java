package team.chisel.ctm.client.shader;

import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.ByteBuffer;

public class ShaderUtil {

    public static int createProgram(String frag, String vert) throws ShaderCompileException {
        //Create a new empty program
        int program = OpenGlHelper.glCreateProgram();
        if (!nullOrEmpty(vert)){
            int vertShader = createShader(vert, GL20.GL_VERTEX_SHADER);
            OpenGlHelper.glAttachShader(program, vertShader);
        }
        if (!nullOrEmpty(frag)){
            int fragShader = createShader(frag, GL20.GL_FRAGMENT_SHADER);
            OpenGlHelper.glAttachShader(program, fragShader);
        }
        OpenGlHelper.glLinkProgram(program);
        return program;
    }

    private static int createShader(String text, int type) throws ShaderCompileException {
        int shader = OpenGlHelper.glCreateShader(type);
        OpenGlHelper.glShaderSource(type, ByteBuffer.wrap(text.getBytes()));
        OpenGlHelper.glCompileShader(shader);
        checkError(shader);
        return shader;
    }

    private static boolean nullOrEmpty(String string){
        return string == null || string.isEmpty();
    }

    private static void checkError(int shader) throws ShaderCompileException {
        int status = OpenGlHelper.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
        if (status == GL11.GL_FALSE){
            int logLen = OpenGlHelper.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH);
            String log = OpenGlHelper.glGetShaderInfoLog(shader, logLen);
            throw new ShaderCompileException(log);
        }
    }
}
