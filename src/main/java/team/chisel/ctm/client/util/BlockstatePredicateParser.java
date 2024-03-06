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

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

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
        
        private final BiFunction<Predicate<BlockState>, Predicate<BlockState>, Predicate<BlockState>> composer;
    }
    
    @Value
    class PropertyPredicate<T extends Comparable<T>> implements Predicate<BlockState> {
        private Block block;
        private Property<T> prop;
        private T value;
        private ComparisonType type;
        
        @Override
        public boolean test(BlockState t) {
            return t.getBlock() == block && type.compareFunc.test(t.getValue(prop).compareTo(value));
        }
    }
    
    @Value
    static class MultiPropertyPredicate<T extends Comparable<T>> implements Predicate<BlockState> {
        private Block block;
        private Property<T> prop;
        private Set<T> validValues;
        
        @Override
        public boolean test(BlockState t) {
            return t.getBlock() == block && validValues.contains(t.getValue(prop));
        }
    }
    
    @Value
    class BlockPredicate implements Predicate<BlockState> {
        private Block block;
        
        @Override
        public boolean test(BlockState t) {
            return t.getBlock() == block;
        }
    }
    
    @RequiredArgsConstructor
    @ToString
    class PredicateComposition implements Predicate<BlockState> {
        private final Composition type;
        private final List<Predicate<BlockState>> composed;
        
        @Override
        public boolean test(BlockState t) {
            if (type == Composition.AND) {
                for (Predicate<BlockState> p : composed) {
                    if (!p.test(t)) {
                        return false;
                    }
                }
                return true;
            } else {
                for (Predicate<BlockState> p : composed) {
                    if (p.test(t)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
    
    class PredicateDeserializer implements JsonDeserializer<Predicate<BlockState>> {
        
        private static final Predicate<BlockState> EMPTY = p -> false;
        
        // Unlikely that this will be threaded, but I think foamfix tries, so let's be safe
        // A global cache for the default predicate for use in creating deferring predicates
        ThreadLocal<Predicate<BlockState>> defaultPredicate = new ThreadLocal<>();

        @Override
        public Predicate<BlockState> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(GsonHelper.getAsString(obj, "block")));
                if (block == Blocks.AIR) {
                    return EMPTY;
                }
                Composition composition = null;
                if (obj.has("defer")) {
                    if (defaultPredicate.get() == null) {
                        throw new JsonParseException("Cannot defer when no default is set!");
                    }
                    try {
                        composition = Composition.valueOf(GsonHelper.getAsString(obj, "defer").toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        throw new JsonSyntaxException(GsonHelper.getAsString(obj, "defer") + " is not a valid defer type.");
                    }
                }
                if (!obj.has("predicate")) {
                    return compose(composition, new BlockPredicate(block));
                }
                JsonElement propsEle = obj.get("predicate");
                if (propsEle.isJsonObject()) {
                    return compose(composition, parsePredicate(block, propsEle.getAsJsonObject(), context));
                } else if (propsEle.isJsonArray()) {
                    List<Predicate<BlockState>> predicates = new ArrayList<>();
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
                List<Predicate<BlockState>> predicates = new ArrayList<>();
                for (JsonElement ele : json.getAsJsonArray()) {
                    Predicate<BlockState> p = context.deserialize(ele, PREDICATE_TYPE);
                    if (p != EMPTY) {
                        predicates.add(p);
                    }
                }
                return predicates.isEmpty() ? EMPTY : predicates.size() == 1 ? predicates.get(0) : new PredicateComposition(Composition.OR, predicates);
            }
            throw new JsonSyntaxException("Predicate deserialization expects an object or an array. Found: " + json);
        }
        
        private Predicate<BlockState> compose(@Nullable Composition composition, @Nonnull Predicate<BlockState> child) {
            if (composition == null) {
                return child;
            }
            return composition.composer.apply(defaultPredicate.get(), child);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Predicate<BlockState> parsePredicate(@Nonnull Block block, JsonObject obj, JsonDeserializationContext context) {
            ComparisonType compareFunc = GsonHelper.getAsObject(obj, "compare_func", ComparisonType.EQUAL, context, ComparisonType.class);
            obj.remove("compare_func");
            
            var entryset = obj.entrySet();
            if (obj.size() != 1) {
                throw new JsonSyntaxException("Predicate entry must define exactly one property->value pair. Found: " + entryset.size());
            }
            
            String key = entryset.iterator().next().getKey();
            
            Optional<Property<?>> prop = Optional.ofNullable(block.getStateDefinition().getProperty(key));

            if (prop.isEmpty()) {
                throw new JsonParseException(key + " is not a valid property for blockstate " + block.defaultBlockState());
            }
            JsonElement valueEle = obj.get(key);
            if (valueEle.isJsonArray()) {
                return new MultiPropertyPredicate(block, prop.get(), StreamSupport.stream(valueEle.getAsJsonArray().spliterator(), false).map(e -> this.parseValue(prop.get(), e)).collect(Collectors.toSet()));
            } else {
                return new PropertyPredicate(block, prop.get(), parseValue(prop.get(), valueEle), compareFunc);
            }
        }
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Comparable parseValue(Property prop, JsonElement ele) {
            String valstr = GsonHelper.convertToString(ele, prop.getName());
            Optional<Comparable> value = prop.getPossibleValues().stream().filter(v -> prop.getName((Comparable) v).equalsIgnoreCase(valstr)).findFirst();
            if (value.isEmpty()) {
                throw new JsonParseException(valstr + " is not a valid value for property " + prop);
            }
            return value.get();
        }
    }

    @RequiredArgsConstructor
    class PredicateMap implements BiPredicate<Direction, BlockState> {
                
        private final EnumMap<Direction, Predicate<BlockState>> predicates = new EnumMap<>(Direction.class);
        
        @Override
        public boolean test(Direction dir, BlockState state) {
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
                for (Direction dir : Direction.values()) {
                    ret.predicates.putIfAbsent(dir, Optional.ofNullable(predicateDeserializer.defaultPredicate.get()).orElse(predicateDeserializer.EMPTY));
                }
                predicateDeserializer.defaultPredicate.remove();
                return ret;
            } else if (json.isJsonArray()) {
                Predicate<BlockState> predicate = context.deserialize(json, PREDICATE_TYPE);
                PredicateMap ret = new PredicateMap();
                for (Direction dir : Direction.values()) {
                    ret.predicates.put(dir, predicate);
                }
                return ret;
            }
            throw new JsonSyntaxException("connectTo must be an object or an array. Found: " + json);
        }
    }
    
    static final Type MAP_TYPE = new TypeToken<EnumMap<Direction, Predicate<BlockState>>>(){}.getType();
    static final Type PREDICATE_TYPE = new TypeToken<Predicate<BlockState>>() {}.getType();
    
    final PredicateDeserializer predicateDeserializer = new PredicateDeserializer();
    
    private final Gson GSON = new GsonBuilder()
                                     .registerTypeAdapter(PREDICATE_TYPE, predicateDeserializer)
                                     .registerTypeAdapter(ComparisonType.class, new ComparisonType.Deserializer())
                                     .registerTypeAdapter(MAP_TYPE, (InstanceCreator<?>) type -> new EnumMap<>(Direction.class))
                                     .registerTypeAdapter(PredicateMap.class, new MapDeserializer())
                                     .create();

    public @Nullable BiPredicate<Direction, BlockState> parse(JsonElement json) {
        return GSON.fromJson(json, PredicateMap.class);
    }
}
