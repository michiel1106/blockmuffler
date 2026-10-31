package bikerboys.blockmuffler.client;

import bikerboys.blockmuffler.*;
import static bikerboys.blockmuffler.BlockMuffler.MOD_ID;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.areas.render.*;
import bikerboys.blockmuffler.client.config.*;
import bikerboys.blockmuffler.client.keybindhandler.*;
import com.mojang.renderpearl.api.pipeline.*;
import eu.midnightdust.lib.config.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.*;

public class BlockMufflerClient implements ClientModInitializer {
	private static final RenderPipeline AREA_FILL = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/area_fill"))
					.withDepthStencilState(DepthStencilState.DEFAULT)
					.build());


	@Override
	public void onInitializeClient() {
		MidnightConfig.register(MOD_ID, BlockMufflerConfig.class);
		AreaRenderTypes.init();
		ModKeybindings.init();
		AreaManager.init();
		AreaManager.addTestBoxes();
		AreaEditRenderManager.init();
		AreaSaveManager.init();



	}













}