package team.chisel.ctm.client.newctm.json;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.client.newctm.CTMLogicBakery;
import team.chisel.ctm.client.newctm.CustomCTMLogic;
import team.chisel.ctm.client.newctm.ICTMLogic;
import team.chisel.ctm.client.newctm.TextureTypeCustom;
import team.chisel.ctm.client.texture.type.TextureTypeRegistry;
import team.chisel.ctm.client.util.Dir;
import team.chisel.ctm.client.util.Submap;

@Log4j2
public class CTMDefinitionManager {
    static final FileToIdConverter CTM_LOGIC_DEFINITIONS = FileToIdConverter.json("ctm_logic");
    
    private final Map<ResourceLocation, ICTMLogic> logicDefinitions = new HashMap<>();
    
    @Getter
    private final PreparableReloadListener reloadListener = new SimplePreparableReloadListener<Map<ResourceLocation, CTMLogicDefinition>>() {
        @Override
        protected Map<ResourceLocation, CTMLogicDefinition> prepare(ResourceManager resources, ProfilerFiller profiler) {
            TextureTypeRegistry.lock.writeLock().lock(); // Manually aqcuire to prevent registry being used in invalid state
            try {
                logicDefinitions.keySet().forEach(id -> TextureTypeRegistry.remove(id.toString()));
                logicDefinitions.clear();
                profiler.startTick();
                var ops = JsonOps.INSTANCE;
                var gson = new Gson();
                var ret = new HashMap<ResourceLocation, CTMLogicDefinition>();
                for (var entry : CTM_LOGIC_DEFINITIONS.listMatchingResources(resources).entrySet()) {
                    ResourceLocation fullLoc = entry.getKey();
                    ResourceLocation id = CTM_LOGIC_DEFINITIONS.fileToId(fullLoc);
                    profiler.push(id::toString);
    
                    try (var r = entry.getValue().openAsReader()) {
                        profiler.push("reading");
                        var json = GsonHelper.fromJson(gson, r, JsonObject.class);
                        profiler.popPush("parsing");
                        var dataresult = CTMLogicDefinition.CODEC.parse(ops, json);
                        ret.put(id, dataresult.get().orThrow());
                    } catch (Exception e) {
                        log.error("Failed to read CTM definition: " + id, e);
                    } finally {
                        profiler.pop();
                        profiler.pop();
                    }
                }
                profiler.endTick();
                apply(ret, profiler);
                return ret;
            } finally {
                TextureTypeRegistry.lock.writeLock().unlock();
            }
        }
        
        @Override
        protected void apply(Map<ResourceLocation, CTMLogicDefinition> values, ResourceManager resources, ProfilerFiller profiler) {
        }
        
        private void apply(Map<ResourceLocation, CTMLogicDefinition> values, ProfilerFiller profiler) {
            profiler.startTick();
            profiler.push("reloading");
            values.forEach((id, def) -> {
               var bakery = createBakery(def);
               var logic = bakery.bake();
               logicDefinitions.put(id, logic);
               TextureTypeRegistry.register(id.toString(), new TextureTypeCustom(logic));
            });
            profiler.pop();
            profiler.endTick();
         }

         public String getName() {
            return "CTMDefinitionManager";
         }
    };
    
    private static CTMLogicBakery createBakery(CTMLogicDefinition def) {
        var bakery = new CTMLogicBakery();
        var bitNames = new Object2IntOpenHashMap<String>();
        var submapNames = new HashMap<String, Pair<ISubmap, Integer>>();
        var faceNames = new HashMap<String, ISubmap>();
        var bit = def.positions().size() - 1;
        for (var position : def.positions()) {
            bitNames.put(position.id(), bit);
            bakery.input(bit--, Dir.fromDirections(position.directions()));
        }
        var outputId = 0;
        for (var e : def.submaps().entrySet()) {
            for (var p : e.getValue().forName(e.getKey())) {
                submapNames.put(p.getLeft(), Pair.of(p.getRight(), outputId++));
            }
        }
        for (var e : def.faces().entrySet()) {
            for (var p : e.getValue().forName(e.getKey())) {
                faceNames.put(p.getLeft(), p.getRight());
            }
        }
        for (var rule : def.rules()) {
            var submapData = submapNames.get(rule.output());
            var submap = submapData.getLeft();
            var ruleId = submapData.getRight();
            bakery.output(ruleId, rule.from(), submap, rule.at().map(faceNames::get).orElse(Submap.X1));
            for (var connected : rule.connected()) {
                bakery.when(bitNames.getInt(connected), true);
            }
            for (var unconnected : rule.unconnected()) {
                bakery.when(bitNames.getInt(unconnected), false);
            }
        }
        return bakery;
    }
    
    @VisibleForTesting
    public static CustomCTMLogic fromReader(Reader r) {
        var gson = new Gson();
        var json = GsonHelper.fromJson(gson, r, JsonObject.class);
        var ops = JsonOps.INSTANCE;
        var dataresult = CTMLogicDefinition.CODEC.parse(ops, json);
        return createBakery(dataresult.get().orThrow()).bake();
    }
}
