package team.chisel.ctm.client.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;

import static org.objectweb.asm.Opcodes.*;

public class CTMTransformer implements IClassTransformer {

    private static final String BLOCK_CLASS = "net.minecraft.block.Block";
    private static final String EXTENDED_STATE_METHOD_NAME = "getExtendedState";
    private static final String CAN_RENDER_IN_LAYER_METHOD_NAME = "canRenderInLayer";
    private static final String CAN_RENDER_IN_LAYER_METHOD_DESC = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockRenderLayer;)Z";

    private static final String WRAPPER_CLASS_NAME = "team/chisel/ctm/client/state/ChiselExtendedState";
    private static final String WRAPPER_CLASS_CONSTRUCTOR_NAME = "<init>";
    private static final String WRAPPER_CLASS_CONSTRUCTOR_DESC = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)V";
    
    private static final String CHISEL_METHODS_CLASS_NAME = "team/chisel/ctm/client/asm/CTMCoreMethods";
    
    private static final String CHISEL_METHODS_LAYER_NAME = "canRenderInLayer";
    private static final String CHISEL_METHODS_LAYER_DESC = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockRenderLayer;)Ljava/lang/Boolean;";
    private static final String CHISEL_METHODS_DAMAGE_PRE_NAME = "preDamageModel";
    private static final String CHISEL_METHODS_DAMAGE_POST_NAME = "postDamageModel";
    private static final String CHISEL_METHODS_TRANFORM_PARENT_NAME = "transformParent";
    private static final String CHISEL_METHODS_TRANFORM_PARENT_DESC = "(Lnet/minecraftforge/client/model/IModel;)Lnet/minecraftforge/client/model/IModel;";

    private static final String FORGE_HOOKS_CLIENT_CLASS = "net.minecraftforge.client.ForgeHooksClient";
    private static final String DAMAGE_MODEL_METHOD_NAME = "getDamageModel";
    
    private static final String VANILLA_MODEL_WRAPPER_CLASS = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper";
    private static final String GET_TEXTURES_METHOD_NAME = "getTextures";

    private static final String TEXTURE_MAP_CLASS_NAME = "net.minecraft.client.renderer.texture.TextureMap";
    private static final String REGISTER_SPRITE_OBF_NAME = "func_174942_a";
    private static final String REGISTER_SPRITE_NAME = "registerSprite";
    private static final String CHISEL_METHODS_SPRITE_REGISTER_NAME = "onSpriteRegister";
    private static final String CHISEL_METHODS_SPRITE_REGISTER_DESC = "(Lnet/minecraft/client/renderer/texture/TextureMap;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals(BLOCK_CLASS)) {

            ClassNode classNode = preTransform(transformedName, EXTENDED_STATE_METHOD_NAME, basicClass);
            Iterator<MethodNode> methods = classNode.methods.iterator();

            while (methods.hasNext()) {
                MethodNode m = methods.next();
                if (m.name.equals(EXTENDED_STATE_METHOD_NAME)) {
                    for (int i = 0; i < m.instructions.size(); i++) {
                        AbstractInsnNode next = m.instructions.get(i);
                        // Find return statement
                        if (next instanceof InsnNode && ((InsnNode)next).getOpcode() == ARETURN) {
                            InsnList toAdd = new InsnList();
                            
                            // FIXME find a better way to do this, might not always be an ALOAD
                            // Grab lvt ID of the current object on the stack
                            AbstractInsnNode load = m.instructions.get(i - 1);
                            int var = ((VarInsnNode)load).var;
                            
                            // Wrap the object that was about to be returned in our own object
                            // This allows multiple hooks here to exist. If someone else adds a wrapper, we will wrap that wrapper :D
                            toAdd.add(new InsnNode(POP));
                            toAdd.add(new TypeInsnNode(NEW, WRAPPER_CLASS_NAME));
                            toAdd.add(new InsnNode(DUP));
                            // Put the old value back on, to be wrapped
                            toAdd.add(new VarInsnNode(ALOAD, var));
                            toAdd.add(new VarInsnNode(ALOAD, 2));
                            toAdd.add(new VarInsnNode(ALOAD, 3));
                            toAdd.add(new MethodInsnNode(INVOKESPECIAL, WRAPPER_CLASS_NAME, WRAPPER_CLASS_CONSTRUCTOR_NAME, WRAPPER_CLASS_CONSTRUCTOR_DESC, false));

                            m.instructions.insertBefore(next, toAdd);
                            break;
                        }
                    }
                } else if (m.name.equals(CAN_RENDER_IN_LAYER_METHOD_NAME) && m.desc.equals(CAN_RENDER_IN_LAYER_METHOD_DESC)) {
                    InsnList toAdd = new InsnList();
                    toAdd.add(new VarInsnNode(ALOAD, 1)); // Load state
                    toAdd.add(new VarInsnNode(ALOAD, 2)); // Load layer
                    // Invoke hook
                    toAdd.add(new MethodInsnNode(INVOKESTATIC, CHISEL_METHODS_CLASS_NAME, CHISEL_METHODS_LAYER_NAME, CHISEL_METHODS_LAYER_DESC, false));
                    toAdd.add(new InsnNode(DUP)); // Copy value on stack, avoids need for local var
                    toAdd.add(new JumpInsnNode(IFNULL, (LabelNode) m.instructions.getFirst())); // Check if return is null, if it is, jump to vanilla code
                    toAdd.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false)); // Otherwise evaluate the bool
                    toAdd.add(new InsnNode(IRETURN)); // And return it
                    AbstractInsnNode first = m.instructions.getFirst(); // First vanilla instruction
                    m.instructions.insertBefore(first, toAdd); // Put this before the first instruction (L1 label node)
                    m.instructions.insert(first, new InsnNode(POP)); // Pop the extra value that vanilla doesn't need
                }
            }

            return finishTransform(transformedName, classNode);

        } else if (transformedName.equals(FORGE_HOOKS_CLIENT_CLASS)) {

            ClassNode classNode = preTransform(transformedName, DAMAGE_MODEL_METHOD_NAME, basicClass);
            Iterator<MethodNode> methods = classNode.methods.iterator();

            while (methods.hasNext()) {
                MethodNode m = methods.next();
                if (m.name.equals(DAMAGE_MODEL_METHOD_NAME)) {
                    for (int i = 0; i < m.instructions.size(); i++) {
                        AbstractInsnNode next = m.instructions.get(i);
                        
                        String methodName = null;
                        if (next.getOpcode() == NEW) {
                            methodName = CHISEL_METHODS_DAMAGE_PRE_NAME;
                        } else if (next.getOpcode() == ARETURN) {
                            methodName = CHISEL_METHODS_DAMAGE_POST_NAME;
                        }
                        if (methodName != null) {
                            m.instructions.insertBefore(next, new MethodInsnNode(INVOKESTATIC, CHISEL_METHODS_CLASS_NAME, methodName, "()V", false));
                            i++;
                        }
                    }
                }
            }
            
            return finishTransform(transformedName, classNode);

        } else if (transformedName.equals(TEXTURE_MAP_CLASS_NAME)) {

            ClassNode classNode = preTransform(transformedName, REGISTER_SPRITE_NAME, basicClass);
            Iterator<MethodNode> methods = classNode.methods.iterator();

            while (methods.hasNext()) {
                MethodNode m = methods.next();
                if (m.name.equals(REGISTER_SPRITE_NAME) || m.name.equals(REGISTER_SPRITE_OBF_NAME)) {
                    for (int i = 0; i < m.instructions.size(); i++) {
                        AbstractInsnNode next = m.instructions.get(i);

                        if (next.getOpcode() == Opcodes.INVOKESTATIC) {
                            next = next.getNext();
                            if (next.getOpcode() == ASTORE && ((VarInsnNode) next).var == 2) {
                                InsnList toInsert = new InsnList();
                                toInsert.add(new VarInsnNode(ALOAD, 0));
                                toInsert.add(new VarInsnNode(ALOAD, 2));
                                toInsert.add(new MethodInsnNode(INVOKESTATIC, CHISEL_METHODS_CLASS_NAME, CHISEL_METHODS_SPRITE_REGISTER_NAME, CHISEL_METHODS_SPRITE_REGISTER_DESC, false));
                                m.instructions.insert(next, toInsert);
                                break;
                            }

                        }
                    }
                }
            }

            return finishTransform(transformedName, classNode);

        } else if (transformedName.equals(VANILLA_MODEL_WRAPPER_CLASS)) {

        	ClassNode classNode = preTransform(transformedName, GET_TEXTURES_METHOD_NAME, basicClass);
        	Iterator<MethodNode> methods = classNode.methods.iterator();

        	while (methods.hasNext()) {
        		MethodNode m = methods.next();
        		if (m.name.equals(GET_TEXTURES_METHOD_NAME)) {
        			for (int i = 0; i < m.instructions.size(); i++) {
        				AbstractInsnNode next = m.instructions.get(i);

        				if (next.getOpcode() == ASTORE && ((VarInsnNode)next).var == 1) {
        					InsnList toInsert = new InsnList();
        					toInsert.add(new VarInsnNode(ALOAD, 1));
        					toInsert.add(new MethodInsnNode(INVOKESTATIC, CHISEL_METHODS_CLASS_NAME, CHISEL_METHODS_TRANFORM_PARENT_NAME, CHISEL_METHODS_TRANFORM_PARENT_DESC, false));
        					toInsert.add(new VarInsnNode(ASTORE, 1));
        					m.instructions.insert(next, toInsert);
        					break;
        				}
        			}
        		}
        	}

        	return finishTransform(transformedName, classNode);
        }
        
        return basicClass;
    }

    private ClassNode preTransform(String transformedName, String methodName, byte[] basicClass) {
        System.out.println("Transforming Class [" + transformedName + "], Method [" + methodName + "]");

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, 0);

        return classNode;
    }

    private byte[] finishTransform(String transformedName, ClassNode classNode) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        System.out.println("Transforming " + transformedName + " Finished.");
        return cw.toByteArray();
    }
}
