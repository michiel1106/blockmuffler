package bikerboys.blockmuffler.client;

import bikerboys.blockmuffler.*;
import bikerboys.blockmuffler.client.areas.*;
import bikerboys.blockmuffler.client.config.*;
import bikerboys.blockmuffler.client.keybindhandler.*;
import bikerboys.blockmuffler.client.screen.*;
import eu.midnightdust.lib.config.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.input.*;

public class BlockMufflerClient implements ClientModInitializer {



	@Override
	public void onInitializeClient() {
		MidnightConfig.register(BlockMuffler.MOD_ID, BlockMufflerConfig.class);
		ModKeybindings.init();
		AreaManager.init();
		AreaManager.addTestBoxes();
		AreaEditRenderManager.init();




	}













}