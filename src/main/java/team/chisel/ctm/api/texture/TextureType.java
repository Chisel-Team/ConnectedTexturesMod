package team.chisel.ctm.api.texture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import team.chisel.ctm.client.texture.type.TextureTypeCTMV;

/**
 * Annotation to register an {@link ITextureType}.
 * <p>
 * If applied to a class, the class must have a no-arg constructor.
 * <p>
 * Can also be applied to fields.
 * <p>
 * <strong>Note: This annotation is {@link Repeatable}, so a single texture type can be assigned multiple aliases.</strong>
 * For an example of this, see {@link TextureTypeCTMV}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
@Repeatable(TextureTypeList.class)
public @interface TextureType {

    /**
     * An alias of the annotated texture type, used to reference it from model or texture metadata
     * <p>
     * If the value is left blank (default) the alias will be inferred from the annotated field or class name
     * 
     * @return The alias for this texture type
     */
    String value() default "";
}
