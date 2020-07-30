package team.chisel.ctm.api.texture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to register an {@link ITextureType}.
 * <p>
 * If applied to a class, the class must have a no-arg constructor.
 * <p>
 * Can also be applied to fields.
 * <p>
 * <strong>Note: This annotation is {@link Repeatable}, so a single texture type can be assigned multiple aliases.</strong>
 * For an example of this, see {@link team.chisel.ctm.client.texture.type.TextureTypePillar}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
@Repeatable(TextureTypeList.class)
public @interface TextureType {

    /**
     * The name used in JSON files to select this texture type. For example, connected textures would be "ctm"
     * <p>
     * This value can be left out to use the name of the class/field being annotated.
     * 
     * @return The name of the texture type.
     */
    String value() default "";
}
