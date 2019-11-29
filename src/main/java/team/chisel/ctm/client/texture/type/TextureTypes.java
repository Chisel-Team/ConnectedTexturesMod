package team.chisel.ctm.client.texture.type;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TextureTypes {
    private static final Map<String, ITextureType> REGISTRY = Maps.newHashMap();
    private static final Logger LOGGER = LogManager.getLogger();

    private TextureTypes() {
    }

    /**
     * Gets the normal (default) texture type from the registry
     *
     * @return The normal texture type
     * @throws IllegalStateException If the type has not been registered yet
     */
    public static ITextureType normal() {
        return getType("normal").orElseThrow(() -> new IllegalStateException("Normal texture type not registered"));
    }

    /**
     * Gets the {@link ITextureType} registered under the given {@code alias}
     *
     * @param alias An alias of the texture type
     * @return An optional of the texture type, which will be empty if none is registered
     */
    public static Optional<ITextureType> getType(final String alias) {
        return Optional.ofNullable(REGISTRY.get(lowerCase(alias)));
    }

    /**
     * Gets all registered aliases for the given {@code type}
     *
     * @param type The texture type
     * @return The texture type's aliases
     */
    public static Collection<String> getAliases(final ITextureType type) {
        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (final Map.Entry<String, ITextureType> entry : REGISTRY.entrySet()) {
            if (type.equals(entry.getValue())) {
                builder.add(entry.getKey());
            }
        }
        return builder.build();
    }

    /**
     * Determines whether the given {@code alias} is registered to any {@link ITextureType}
     *
     * @param alias A texture type alias
     * @return True if the given {@code alias} is registered
     */
    public static boolean isPresent(final String alias) {
        return REGISTRY.containsKey(lowerCase(alias));
    }

    /**
     * Registers the given {@code type} under the given {@code alias}
     *
     * @param type The texture type to be registered
     * @param alias An alias of the texture type
     * @throws IllegalArgumentException If the given type or an existing
     *         type are already registered any of the given aliases
     */
    public static void register(final ITextureType type, String alias) {
        Preconditions.checkArgument(type != null, "Cannot register a null type");
        alias = lowerCase(alias);
        if (REGISTRY.containsKey(alias)) {
            if (type.equals(REGISTRY.get(alias))) {
                // TODO This should be a failure condition now that the annotation parsing isn't duplicating registration
                LOGGER.warn("Duplicate registration of a type under alias {}." +
                        " Duplicate registration will be a failure condition in future versions of CTM", alias);
                return;
            }
            throw new IllegalArgumentException("An existing type is already registered under alias " + alias);
        }
        LOGGER.debug("Registering alias {} for type {}", alias, type);
        REGISTRY.put(alias, type);
    }

    /**
     * Registers the given {@code type} under the given {@code aliases}
     *
     * @param type The texture type to be registered
     * @param aliases The aliases of the texture type
     * @throws IllegalArgumentException If the given type or an existing
     *         type are already registered any of the given aliases
     */
    public static void register(final ITextureType type, final String... aliases) {
        for (final String alias : aliases) {
            register(type, alias);
        }
    }

    /**
     * Registers any fields and classes annotated with {@link TextureType}
     * that have been discovered and stored in the given {@link ASMDataTable}
     *
     * @param table The ASM data table for this runtime
     */
    public static void registerAll(final ASMDataTable table) {
        for (final ASMData data : table.getAll(TextureType.class.getName())) {
            register(getTypeInstance(data), getTypeAlias(data));
        }
    }

    private static String getTypeAlias(final ASMData data) {
        final String alias = (String) data.getAnnotationInfo().get("value");
        return alias.isEmpty() ? inferAliasFromType(data) : alias;
    }

    private static ITextureType getTypeInstance(final ASMData data) {
        final String className = data.getClassName();
        final String objectName = data.getObjectName();
        final boolean isClass = className.equals(objectName);
        try {
            final Class<?> target = Class.forName(className);
            if (isClass) {
                return (ITextureType) target.getConstructor().newInstance();
            }
            return (ITextureType) target.getField(objectName).get(null);
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(isClass ? "Constructing texture type" : "Getting texture type", e);
        }
    }

    // TODO Consider formatting pascal and lower camel to lower underscore to follow naming standards of game registries
    // TODO Consider stripping outer class qualifiers ($ recursive), possibly map to underscores if implementing above
    private static String inferAliasFromType(final ASMData data) {
        final String className = data.getClassName();
        final String objectName = data.getObjectName();
        if (className.equals(objectName)) {
            return className.substring(className.lastIndexOf('.') + 1);
        }
        return objectName;
    }

    private static String lowerCase(final String rawAlias) {
        return rawAlias.toLowerCase(Locale.ROOT);
    }
}
