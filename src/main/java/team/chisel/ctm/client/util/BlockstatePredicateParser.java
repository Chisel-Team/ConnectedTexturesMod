package team.chisel.ctm.client.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import lombok.Delegate;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.val;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class BlockstatePredicateParser {
    
    @RequiredArgsConstructor
    enum ComparisonType {
        EQUAL("=", i -> i == 0),
        NOT_EQUAL("!=", i -> i != 0),
        GREATER_THAN(">", i -> i > 0),
        LESS_THAN("<", i -> i < 0),
        GREATER_THAN_EQ(">=", i -> i >= 0),
        LESS_THAN_EQ("<=", i -> i <= 0),
        ;
        
        private final String key;
        private final IntPredicate compareFunc;
        
        static class Deserializer implements JsonDeserializer<ComparisonType> {

            @Override
            public ComparisonType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                    Optional<ComparisonType> type = Arrays.stream(ComparisonType.values()).filter(t -> t.key.equals(json.getAsString())).findFirst();
                    if (type.isPresent()) {
                        return type.get();
                    }
                    throw new JsonParseException(json + " is not a valid comparison type!");
                }
                throw new JsonSyntaxException("ComparisonType must be a String");
            }
        }
    }
    
    @RequiredArgsConstructor
    enum Composition {
        AND(Predicate::and),
        OR(Predicate::or);
        
        private final BiFunction<Predicate<IBlockState>, Predicate<IBlockState>, Predicate<IBlockState>> composer;
    }
    
    @Value
    class PropertyPredicate<T extends Comparable<T>> implements Predicate<IBlockState> {
        private Block block;
        private IProperty<T> prop;
        private T value;
        private ComparisonType type;
        
        @Override
        public boolean test(IBlockState t) {
            return t.getBlock() == block && type.compareFunc.test(t.getValue(prop).compareTo(value));
        }
    }
    
    @Value
    static class MultiPropertyPredicate<T extends Comparable<T>> implements Predicate<IBlockState> {
        private Block block;
        private IProperty<T> prop;
        private Set<T> validValues;
        
        @Override
        public boolean test(IBlockState t) {
            return t.getBlock() == block && validValues.contains(t.getValue(prop));
        }
    }
    
    @Value
    class BlockPredicate implements Predicate<IBlockState> {
        private Block block;
        
        @Override
        public boolean test(IBlockState t) {
            return t.getBlock() == block;
        }
    }
    
    @RequiredArgsConstructor
    @ToString
    class PredicateComposition implements Predicate<IBlockState> {
        private final Composition type;
        private final List<Predicate<IBlockState>> composed;
        
        @Override
        public boolean test(IBlockState t) {
            if (type == Composition.AND) {
                for (Predicate<IBlockState> p : composed) {
                    if (!p.test(t)) {
                        return false;
                    }
                }
                return true;
            } else {
                for (Predicate<IBlockState> p : composed) {
                    if (p.test(t)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
    
    class PredicateDeserializer implements JsonDeserializer<Predicate<IBlockState>> {
        
        final Predicate<IBlockState> EMPTY = p -> false;
        
        // Unlikely that this will be threaded, but I think foamfix tries, so let's be safe
        // A global cache for the default predicate for use in creating deferring predicates
        ThreadLocal<Predicate<IBlockState>> defaultPredicate = new ThreadLocal<>();

        @Override
        public Predicate<IBlockState> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(JsonUtils.getString(obj, "block")));
                if (block == Blocks.AIR) {
                    return EMPTY;
                }
                Composition composition = null;
                if (obj.has("defer")) {
                    if (defaultPredicate.get() == null) {
                        throw new JsonParseException("Cannot defer when no default is set!");
                    }
                    try {
                        composition = Composition.valueOf(JsonUtils.getString(obj, "defer").toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        throw new JsonSyntaxException(JsonUtils.getString(obj, "defer") + " is not a valid defer type.");
                    }
                }
                if (!obj.has("predicate")) {
                    return compose(composition, new BlockPredicate(block));
                }
                JsonElement propsEle = obj.get("predicate");
                if (propsEle.isJsonObject()) {
                    return compose(composition, parsePredicate(block, propsEle.getAsJsonObject(), context));
                } else if (propsEle.isJsonArray()) {
                    List<Predicate<IBlockState>> predicates = new ArrayList<>();
                    for (JsonElement ele : propsEle.getAsJsonArray()) {
                        if (ele.isJsonObject()) {
                            predicates.add(parsePredicate(block, ele.getAsJsonObject(), context));
                        } else {
                            throw new JsonSyntaxException("Predicate entry must be a JSON Object. Found: " + ele);
                        }
                    }
                    return compose(composition, new PredicateComposition(Composition.AND, predicates));
                }
            } else if (json.isJsonArray()) {
                List<Predicate<IBlockState>> predicates = new ArrayList<>();
                for (JsonElement ele : json.getAsJsonArray()) {
                    Predicate<IBlockState> p = context.deserialize(ele, PREDICATE_TYPE);
                    if (p != EMPTY) {
                        predicates.add(p);
                    }
                }
                return predicates.size() == 0 ? EMPTY : predicates.size() == 1 ? predicates.get(0) : new PredicateComposition(Composition.OR, predicates);
            }
            throw new JsonSyntaxException("Predicate deserialization expects an object or an array. Found: " + json);
        }
        
        private Predicate<IBlockState> compose(@Nullable Composition composition, @Nonnull Predicate<IBlockState> child) {
            if (composition == null) {
                return child;
            }
            return composition.composer.apply(defaultPredicate.get(), child);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Predicate<IBlockState> parsePredicate(@Nonnull Block block, JsonObject obj, JsonDeserializationContext context) {
            ComparisonType compareFunc = JsonUtils.deserializeClass(obj, "compare_func", ComparisonType.EQUAL, context, ComparisonType.class);
            obj.remove("compare_func");
            
            val entryset = obj.entrySet();
            if (entryset.size() > 1 || entryset.size() == 0) {
                throw new JsonSyntaxException("Predicate entry must define exactly one property->value pair. Found: " + entryset.size());
            }
            
            String key = entryset.iterator().next().getKey();
            
            Optional<IProperty<?>> prop = block.getBlockState().getProperties().stream().filter(p -> p.getName().equals(key)).findFirst();
            if (!prop.isPresent()) {
                throw new JsonParseException(key + " is not a valid property for blockstate " + block.getDefaultState());
            }
            JsonElement valueEle = obj.get(key);
            if (valueEle.isJsonArray()) {
                return new MultiPropertyPredicate(block, prop.get(), StreamSupport.stream(valueEle.getAsJsonArray().spliterator(), false).map(e -> this.parseValue(prop.get(), e)).collect(Collectors.toSet()));
            } else {
                return new PropertyPredicate(block, prop.get(), parseValue(prop.get(), valueEle), compareFunc);
            }
        }
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Comparable parseValue(IProperty prop, JsonElement ele) {
            String valstr = JsonUtils.getString(ele, prop.getName());
            Optional<Comparable> value = (Optional<Comparable>) prop.getAllowedValues().stream().filter(v -> prop.getName((Comparable) v).equalsIgnoreCase(valstr)).findFirst();
            if (!value.isPresent()) {
                throw new JsonParseException(valstr + " is not a valid value for property " + prop);
            }
            return value.get();
        }
    }

    @RequiredArgsConstructor
    class PredicateMap implements BiPredicate<EnumFacing, IBlockState> {
                
        private final EnumMap<EnumFacing, Predicate<IBlockState>> predicates = new EnumMap<>(EnumFacing.class);
        
        @Override
        public boolean test(EnumFacing dir, IBlockState state) {
            return predicates.get(dir).test(state);
        }
    }

    class MapDeserializer implements JsonDeserializer<PredicateMap> {

        @Override
        public PredicateMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("default")) {
                    predicateDeserializer.defaultPredicate.set(context.deserialize(obj.get("default"), PREDICATE_TYPE));
                    obj.remove("default");
                }
                PredicateMap ret = new PredicateMap();
                ret.predicates.putAll(context.deserialize(obj, MAP_TYPE));
                for (EnumFacing dir : EnumFacing.VALUES) {
                    ret.predicates.putIfAbsent(dir, Optional.ofNullable(predicateDeserializer.defaultPredicate.get()).orElse(predicateDeserializer.EMPTY));
                }
                predicateDeserializer.defaultPredicate.set(null);
                return ret;
            } else if (json.isJsonArray()) {
                Predicate<IBlockState> predicate = context.deserialize(json, PREDICATE_TYPE);
                PredicateMap ret = new PredicateMap();
                for (EnumFacing dir : EnumFacing.VALUES) {
                    ret.predicates.put(dir, predicate);
                }
                return ret;
            }
            throw new JsonSyntaxException("connectTo must be an object or an array. Found: " + json);
        }
    }
    
    static final Type MAP_TYPE = new TypeToken<EnumMap<EnumFacing, Predicate<IBlockState>>>(){}.getType();
    static final Type PREDICATE_TYPE = new TypeToken<Predicate<IBlockState>>() {}.getType();
    
    final PredicateDeserializer predicateDeserializer = new PredicateDeserializer();
    
    private final Gson GSON = new GsonBuilder()
                                     .registerTypeAdapter(PREDICATE_TYPE, predicateDeserializer)
                                     .registerTypeAdapter(ComparisonType.class, new ComparisonType.Deserializer())
                                     .registerTypeAdapter(MAP_TYPE, (InstanceCreator<?>) type -> new EnumMap<>(EnumFacing.class))
                                     .registerTypeAdapter(PredicateMap.class, new MapDeserializer())
                                     .create();

    public @Nullable BiPredicate<EnumFacing, IBlockState> parse(JsonElement json) {
        return GSON.fromJson(json, PredicateMap.class);
    }
}
