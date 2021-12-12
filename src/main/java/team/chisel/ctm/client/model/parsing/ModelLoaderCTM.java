package team.chisel.ctm.client.model.parsing;

import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.client.model.IModelLoader;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.model.IModelParser;

public enum ModelLoaderCTM implements IModelLoader<IModelCTM> {

	INSTANCE;

	private static final Map<Integer, IModelParser> parserVersions = Map.of(1, new ModelParserV1());

//    @Override
//    public boolean accepts(ResourceLocation modelLocation) {        
//        if (modelLocation instanceof ModelResourceLocation) {
//            modelLocation = new ResourceLocation(modelLocation.getNamespace(), modelLocation.getPath());
//        }
//
//        JsonElement json = getJSON(modelLocation);
//        return json.isJsonObject() && json.getAsJsonObject().has("ctm_version");
//    }
//
//    @Override
//    public IModel loadModel(ResourceLocation modelLocation) throws IOException {
//        loadedModels.computeIfAbsent(modelLocation, res -> loadFromFile(res, true));
//        IModelCTM model = loadedModels.get(modelLocation);
//        if (model != null) {
//            model.load();
//        }
//        return model;
//    }
//    

	@Override
	public IModelCTM read(JsonDeserializationContext ctx, JsonObject json) {
		IModelParser parser = parserVersions.get(json.get("ctm_version").getAsInt());
		if (parser == null) {
			throw new IllegalArgumentException("Invalid \"ctm_version\" in model " + json);
		}
		json.remove("loader"); // Prevent reentrant parsing
		return parser.fromJson(ctx, json);
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {

	}
}
