package test.game.streamingtest;


import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.*;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.GuiTest;
import nidefawl.qubes.gui.LoadingScreen;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.render.RenderersVulkan;
import nidefawl.qubes.render.gui.BoxGUI;
import nidefawl.qubes.render.gui.LineGUI;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.vulkan.FrameBuffer;

public class TexturedMesh_VK extends GameBase {
	static {
		System.setProperty("renderer.vulkan", "true");
	}
	public TexturedMesh_VK() {
		TICKS_PER_SEC = 20;
		DEBUG_LAYER = true;
	}
	public static void main(String[] args) {

        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TexturedMesh_VK().startGame();
	}

    public Camera         camera;
    final CameraController cameraController = new CameraController();
    
	private boolean needsRedraw = true;
	private boolean updateDescSets = true;

	private FrameBuffer frameBufferScene;
	private FrameBuffer frameBufferShadow;
	private FrameBuffer frameBufferShadow2;
	private FrameBuffer frameBuffer;
	private VkTesselatorState cubesShadow;
	private VkDescriptor descTextureTerrain;
	private VkDescriptor descTextureShadowColorDBG;
	private VkDescriptor descTextureShadowDepth;
	private VkDescriptor descTextureCubeShadowMap;
	private VkDescriptor descTextureGbufferColor;
	private VkDescriptor descTextureItem;
	
	
	VkViewport.Buffer viewport = VkViewport.calloc(1);

	TextureBinMips texture2dData1;
	TextureBinMips texture2dData2;

	private long sampler;
	private long samplerShadowMap;
	private long textureView;
	
	private VkTexture texture;
	
	private FontRenderer font;

	VkTesselatorState cube;
	VkTesselatorState plane;
	BlockFaceVBuffer vBuf = new BlockFaceVBuffer();
	private VkDescriptor descTextureTerrainOnly;
	
	@Override
	public void onStatsUpdated() {
		String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+") bytes uploaded: "+Stats.uploadBytes;
		System.out.println(stats);
		System.out.println(""+Stats.callsBindDescSets+","+Stats.callsBindPipeline);
//		setTitle(stats);
//		AssetManager assetManager = AssetManagerClient.getInstance();
//		VkShader n = vkContext.loadCompileGLSL(assetManager, "shaders/textured.fsh", VK_SHADER_STAGE_FRAGMENT_BIT);
//		VkShader n2 = vkContext.loadCompileGLSL(assetManager, "shaders/textured.vsh", VK_SHADER_STAGE_VERTEX_BIT);
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
        if (GuiContext.input != null) {
            GuiContext.input.onTextInput(codepoint);
        }
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
        if (window == windowId) {
            if (key == -1) { // ALT + Print Screen (maybe more)
                return;
            }
            if (GuiContext.input != null) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                    GuiContext.input.focused = false;
                    GuiContext.input = null;
                }
//                return;
            }
            if (GuiWindowManager.onKeyPress(key, scancode, action, mods)) {
                return;
            }
            if (this.gui != null) {
                if (this.gui.onKeyPress(key, scancode, action, mods)) {
                    return;
                }
            }

    		if (action == GLFW.GLFW_PRESS) {
    			switch (key) {
    			case GLFW.GLFW_KEY_Q:
    				Engine.INVERSE_MAP = !Engine.INVERSE_MAP;
                    vkContext.reinitSwapchain = true;
//    				System.out.println(cameraController.pos);
//    				System.out.println(cameraController.pitch+","+cameraController.yaw);
//    				renderModeReRecord = !renderModeReRecord;
//    				forceRedraw = true;
//    				Arrays.fill(recorded, false);
    				return;
				case GLFW.GLFW_KEY_ENTER:
					showGUI(new GuiTest());
	//				renderModeReRecord = !renderModeReRecord;
	//				forceRedraw = true;
	//				Arrays.fill(recorded, false);
					return;
				}
    		}
        }
	}

	
	
	@Override
	public void render(float f) {
		CommandBuffer buffer = vkContext.getCurrentCmdBuffer();
		Engine.beginCommandBuffer(buffer);
        createRenderCommandBuffers(buffer, f);
        Engine.endCommandBuffer();
		this.vkContext.submitCommandBuffer();
	}
	
	@Override
	public void preRenderUpdate(float f) {
		if (updateDescSets) {
			updateDescSets = false;
			updateDescriptorSets();
		}
		if (needsRedraw) {
			needsRedraw = false;
			drawScene();
		}
        Engine.getSunLightModel().setTime(5555);
//      Engine.getSunLightModel().setTime(1700+(int)((ticksran+f)*32));
      Engine.getSunLightModel().updateFrame(0);
      Engine.setLightPosition(Engine.getSunLightModel().getLightPosition());
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
		Engine.updateShadowProjections(f);
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
//        System.out.println(cameraController.pos+","+cameraController.pitch);
	}

	private void drawScene() {
		VkTess tess = VkTess.instance;
		int l = 4;
		float d = 4;
		float scale = 3.0f;
		for (int x = -l; x <= l; x++) {
			for (int z = -l; z <= l; z++) {
				for (int y = 1; y <= l; y++) {
					tess.setOffset(x*d*scale, y*d*scale, z*d*scale);
					tess.setNormals(0, 0, 1);
					tess.add( scale,  scale, scale, 1, 1);
					tess.add(-scale,  scale, scale, 0, 1);
					tess.add(-scale, -scale, scale, 0, 0);
					tess.add( scale, -scale, scale, 1, 0);
					tess.setNormals(0, 0, -1);
					tess.add(-scale, -scale, -scale, 0, 0);
					tess.add(-scale,  scale, -scale, 0, 1);
					tess.add( scale,  scale, -scale, 1, 1);
					tess.add( scale, -scale, -scale, 1, 0);
					tess.setNormals(0, 1, 0);
					tess.add(-scale,  scale, -scale, 0, 0);
					tess.add(-scale,  scale,  scale, 0, 1);
					tess.add( scale,  scale,  scale, 1, 1);
					tess.add( scale,  scale, -scale, 1, 0);
					tess.setNormals(0, -1, 0);
					tess.add(-scale, -scale,  scale, 0, 1);
					tess.add(-scale, -scale, -scale, 0, 0);
					tess.add( scale, -scale, -scale, 1, 0);
					tess.add( scale, -scale,  scale, 1, 1);
					tess.setNormals(1, 0, 0);
					tess.add( scale, -scale,  scale, 0, 0);
					tess.add( scale, -scale, -scale, 0, 1);
					tess.add( scale,  scale, -scale, 1, 1);
					tess.add( scale,  scale,  scale, 1, 0);
					tess.setNormals(-1, 0, 0);
					tess.add(-scale, -scale, -scale, 0, 1);
					tess.add(-scale, -scale,  scale, 0, 0);
					tess.add(-scale,  scale,  scale, 1, 0);
					tess.add(-scale,  scale, -scale, 1, 1);
				}
				
			}
		}
		tess.setOffset(0, 0, 0);
		tess.setNormals(0, 1, 0);
		float xf = 146;
		float xy = -10;
		tess.add(-xf, xy, xf, 0, 1);
		tess.add( xf, xy, xf, 1, 1);
		tess.add( xf, xy, -xf, 1, 0);
		tess.add(-xf, xy, -xf, 0, 0);
		 xf = 66;
		 xy = 485;
		 tess.setOffset(620, 0, 0);
		tess.add(-xf, xy, xf, 0, 1);
		tess.add( xf, xy, xf, 1, 1);
		tess.add( xf, xy, -xf, 1, 0);
		tess.add(-xf, xy, -xf, 0, 0);
		tess.setOffset(0, 0, 0);
		int stateVsize = tess.getVSize();
		int stateVcount = tess.vertexcount;
		tess.finish(VkTess.CREATE_QUAD_IDX_BUFFER, VkTess.DEVICE_LOCAL_UPLOAD, cube);
		int[] buffer = tess.rawBuffer;
		int idx = 0;
		for (int i = 0; i < stateVcount; i++) {
			int offset = i*stateVsize;
			tess.rawBuffer[idx++] = buffer[offset+0];
			tess.rawBuffer[idx++] = buffer[offset+1];
			tess.rawBuffer[idx++] = buffer[offset+2];
			tess.rawBuffer[idx++] = buffer[offset+3];
			tess.vertexcount++;
		}
		tess.finish(VkTess.CREATE_QUAD_IDX_BUFFER, VkTess.DEVICE_LOCAL_UPLOAD, cubesShadow);
		tess.setOffset(0, 0, 0);
		tess.setNormals(0, 0, 1);
		tess.add( 2,  2, 3.5f, 1, 1);
		tess.add(-2,  2, 3.5f, 0, 1);
		tess.add(-2, -2, 3.5f, 0, 0);
		tess.add( 2, -2, 3.5f, 1, 0);
		tess.finish(VkTess.CREATE_QUAD_IDX_BUFFER, VkTess.DEVICE_LOCAL_UPLOAD, plane);
    
	}
	@Override
	public void postRenderUpdate(float f) {
	}

	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        Engine.updateRenderResolution(displayWidth, displayHeight);
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);

    		{
    			if (this.frameBufferScene != null) {
    				this.frameBufferScene.destroy();
        			this.frameBufferScene.build(VkRenderPasses.passTerrain_Pass0, displayWidth, displayHeight);
    			}
    			if (this.frameBuffer != null) {
    				this.frameBuffer.destroy();
        			this.frameBuffer.build(VkRenderPasses.passFramebuffer, displayWidth, displayHeight);
    			}
    			if (this.frameBufferShadow != null) {
    				this.frameBufferShadow.destroy();
        			this.frameBufferShadow.build(VkRenderPasses.passShadow, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());

    			}
    			if (this.frameBufferShadow2 != null) {
    				this.frameBufferShadow2.destroy();
        			this.frameBufferShadow2.build(VkRenderPasses.passShadow, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());

    			}

    		}
            updateDescSets = true;
    		needsRedraw=true;
//            if (Game.GL_ERROR_CHECKS)
//                Engine.checkGLError("onResize");
//            GLDebugTextures.onResize();
        }
	}
    @Override
    public void onWindowResize(int displayWidth, int displayHeight) {
        if (!VR_SUPPORT||isStarting) {
            setRenderResolution(displayWidth, displayHeight);
        }
    }

	@Override
	public void initGame() {
		EngineInitSettings settings = EngineInitSettings.INIT_NONE.setFBSize(windowWidth, windowHeight).setVulkan(true).setInverseZ();
		settings.initShadowProj = true;
		Engine.init(settings);
        GameBase.loadingScreen = new LoadingScreen();
		if (loadingScreen != null)
        loadingScreen.setProgress(0, 0, "Initializing");
		setVSync(true);
		camera = new Camera();
		AssetTexture bin1 = AssetManagerClient.getInstance().loadPNGAsset("textures/blocks/ground/dirt.png");
		
		AssetTexture bin2 = AssetManagerClient.getInstance().loadPNGAsset("textures/blocks/ground/sand.png");
		texture2dData1 = new TextureBinMips(bin1);
		texture2dData2 = new TextureBinMips(bin2);
		this.cameraController.set(98.31f, 88.73f, -130.53f, 26.86f, 224.16f);
		if (loadingScreen != null)
        loadingScreen.setProgress(0, 1, "Something done");
		this.frameBufferScene = new FrameBuffer(vkContext);
		this.frameBufferScene.fromRenderpass(VkRenderPasses.passTerrain_Pass0, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);

		this.frameBufferShadow = new FrameBuffer(vkContext);
		this.frameBufferShadow.fromRenderpass(VkRenderPasses.passShadow, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);
		
		this.frameBufferShadow2 = new FrameBuffer(vkContext);
		this.frameBufferShadow2.fromRenderpass(VkRenderPasses.passShadow, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);

		this.frameBuffer = new FrameBuffer(vkContext);
		this.frameBuffer.fromRenderpass(VkRenderPasses.passFramebuffer, 0, VK_IMAGE_USAGE_TRANSFER_SRC_BIT);

	}

	@Override
	public void lateInitGame() {
		if (loadingScreen != null)
        loadingScreen.setProgress(1, 0, "lateInitGame");
		loadTexture(this.texture2dData1, this.texture2dData2);
        RenderAssets.load(null, loadingScreen);

    	this.descTextureTerrain = vkContext.descLayouts.allocDescSetSamplerDouble();
    	this.descTextureTerrainOnly = vkContext.descLayouts.allocDescSetSampleSingle();
    	this.descTextureCubeShadowMap = vkContext.descLayouts.allocDescSetSamplerDouble();
    	this.descTextureGbufferColor = vkContext.descLayouts.allocDescSetSampleSingle();
    	this.descTextureShadowColorDBG = vkContext.descLayouts.allocDescSetSampleSingle();
    	this.descTextureShadowDepth = vkContext.descLayouts.allocDescSetSampleSingle();
        this.descTextureItem = vkContext.descLayouts.allocDescSetSampleSingle();
        this.font = FontRenderer.get(0, 22, 1);
		if (loadingScreen != null)
			loadingScreen.setProgress(1, 1f, "done");

        try ( MemoryStack stack = stackPush() ) {

			VkSamplerCreateInfo sampler = VkSamplerCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
					.magFilter(VK_FILTER_LINEAR)
					.minFilter(VK_FILTER_LINEAR)
					.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
					.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
					.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
					.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
					.mipLodBias(0.0f)
					.compareOp(VK_COMPARE_OP_NEVER)
					.compareEnable(false)
					.minLod(0.0f)
					.maxLod(1.0f)
					.borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE);
	        LongBuffer pSampler = stack.longs(0);
	        int err = vkCreateSampler(vkContext.device, sampler, null, pSampler);
	        if (err != VK_SUCCESS) {
	            throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
	        }
	        this.samplerShadowMap = pSampler.get(0);
        }
		cube = new VkTesselatorState(vkContext).tag("cubes");
		plane = new VkTesselatorState(vkContext).tag("plane");
		cubesShadow = new VkTesselatorState(vkContext).tag("cubesShadow");
		drawScene();
		this.vBuf.init(vkContext);
		this.vBuf.redraw();
//		showGUI(new GuiTest());
	}
	private void updateDescriptorSets() {
		this.descTextureTerrainOnly.setBindingCombinedImageSampler(0, 
				TextureArrays.blockTextureArrayVK.getView(), 
				TextureArrays.blockTextureArrayVK.getSampler(), 
				TextureArrays.blockTextureArrayVK.getImageLayout());
		this.descTextureTerrain.setBindingCombinedImageSampler(0, 
				TextureArrays.blockTextureArrayVK.getView(), 
				TextureArrays.blockTextureArrayVK.getSampler(), 
				TextureArrays.blockTextureArrayVK.getImageLayout());
		this.descTextureTerrain.setBindingCombinedImageSampler(1, 
				TextureArrays.blockNormalMapArrayVK.getView(), 
				TextureArrays.blockNormalMapArrayVK.getSampler(), 
				TextureArrays.blockNormalMapArrayVK.getImageLayout());
		
        FramebufferAttachment coloratt = this.frameBufferScene.getAtt(0);
		this.descTextureGbufferColor.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.finalLayout);
		
        FramebufferAttachment depthatt = this.frameBufferShadow.getAtt(0);
		this.descTextureCubeShadowMap.setBindingCombinedImageSampler(0, 
				TextureArrays.blockTextureArrayVK.getView(), 
				TextureArrays.blockTextureArrayVK.getSampler(), 
				TextureArrays.blockTextureArrayVK.getImageLayout());
		this.descTextureCubeShadowMap.setBindingCombinedImageSampler(1, 
				depthatt.getView(), 
				samplerShadowMap, 
				depthatt.finalLayout);

        FramebufferAttachment shadowColorAtt = this.frameBufferShadow.getAtt(1);
		this.descTextureShadowColorDBG.setBindingCombinedImageSampler(0, 
				this.frameBufferShadow2.getAtt(1).getView(), 
				sampler, 
				this.frameBufferShadow2.getAtt(1).finalLayout);
		

		this.descTextureShadowDepth.setBindingCombinedImageSampler(0, 
				depthatt.getView(), 
				samplerShadowMap, 
				depthatt.finalLayout);
		this.descTextureItem.setBindingCombinedImageSampler(0, 
				TextureArrays.itemTextureArrayVK.getView(), 
				TextureArrays.itemTextureArrayVK.getSampler(), 
				TextureArrays.itemTextureArrayVK.getImageLayout());
        this.descTextureTerrain.update(vkContext);
        this.descTextureGbufferColor.update(vkContext);
        this.descTextureCubeShadowMap.update(vkContext);
        this.descTextureShadowColorDBG.update(vkContext);
        this.descTextureShadowDepth.update(vkContext);
        this.descTextureItem.update(vkContext);
        this.descTextureTerrainOnly.update(vkContext);
	}


	private void loadTexture(TextureBinMips... texture2dData) {
		int vkFormat = VK_FORMAT_R8G8B8A8_UNORM;

        try ( MemoryStack stack = stackPush() ) {

			this.texture = new VkTexture(vkContext);
			texture.build(vkFormat, texture2dData);

            VkSamplerCreateInfo sampler = VkInitializers.samplerCreateStack();
			// Set max level-of-detail to mip level count of the texture
			sampler.maxLod((float)this.texture.getNumMips());
			// Enable anisotropic filtering
			// This feature is optional, so we must check if it's supported on the device
			if (vkContext.features.samplerAnisotropy())
			{
				sampler.maxAnisotropy(vkContext.limits.maxSamplerAnisotropy());
				sampler.anisotropyEnable(true);
			} else {
				sampler.maxAnisotropy(1.0f);
				sampler.anisotropyEnable(false);
			}
			sampler.borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE);
	        LongBuffer pSampler = stack.longs(0);
	        int err = vkCreateSampler(vkContext.device, sampler, null, pSampler);
	        if (err != VK_SUCCESS) {
	            throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
	        }
	        this.sampler = pSampler.get(0);
	        
	        VkImageViewCreateInfo view = VkImageViewCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
	        		.viewType(VK_IMAGE_VIEW_TYPE_2D_ARRAY)
	        		.format(vkFormat)
	        		.components(VkComponentMapping.callocStack(stack));
	        VkImageSubresourceRange viewSubResRange = view.subresourceRange();
	        viewSubResRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
	        viewSubResRange.baseMipLevel(0);
	        viewSubResRange.baseArrayLayer(0);
	        viewSubResRange.layerCount(this.texture.getNumLayers());
			// Linear tiling usually won't support mip maps
			// Only set mip map count if optimal tiling is used
	        viewSubResRange.levelCount(this.texture.getNumMips());
			// The view will be based on the texture's image
	        view.image(this.texture.getImage());
	        LongBuffer pView = stack.longs(0);
	        err = vkCreateImageView(vkContext.device, view, null, pView);
	        if (err != VK_SUCCESS) {
	            throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
	        }
	        this.textureView = pView.get(0);
		}
	}

	

	@Override
	public void tick() {
		if (!isStarting) {
            if (this.gui != null) {
                this.gui.update();
            }
			this.cameraController.tickUpdate();
		}
	}
    private void createRenderCommandBuffers(CommandBuffer commandBuffer, float fTime) {
        Engine.updateRenderResolution(Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());
        Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());

//        System.out.println(TextureArrays.blockTextureArray.totalSlots);
        if (this.frameBufferShadow.getWidth() == Engine.getShadowMapTextureSize()&&this.frameBufferShadow.getHeight() == Engine.getShadowMapTextureSize())
        {
//        	viewport.minDepth(0.0f);
//        	viewport.maxDepth(1.0f);
//        	viewport.minDepth(Engine.INVERSE_Z_BUFFER ? 1.0f : 0.0f);
//            viewport.maxDepth(Engine.INVERSE_Z_BUFFER ? 0.0f : 1.0f);
            PushConstantBuffer buf = PushConstantBuffer.INST;
            int mapSize = Engine.getShadowMapTextureSize()/2;
            VkRenderPasses.passShadow.getClearValueDepth().set(Engine.INVERSE_MAP?0:1, 0);
            Engine.beginRenderPass(VkRenderPasses.passShadow, this.frameBufferShadow, VK_SUBPASS_CONTENTS_INLINE);

            Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, descTextureTerrainOnly);
            Engine.setDescriptorSet(VkDescLayouts.UBO_CONSTANTS_DESC_IDX, Engine.descriptorSetUboShadow);
            Engine.bindPipeline(VkPipelines.shadowSolid);
            if (!Engine.INVERSE_MAP)
            Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize(), 0, 1);
            else 
            	Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize(), 1, 0);
            buf.setMat4(0, Engine.getIdentityMatrix());
            buf.setInt(16, 2);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
//          Engine.clearDepth();
            float f = Engine.INVERSE_MAP?-1:1;
            vkCmdSetDepthBias(commandBuffer, f*1.15f, 0.0f, f*1.15f);
			cubesShadow.bindAndDraw(commandBuffer);   
//            Engine.setViewport(mapSize, 0, mapSize, mapSize, 1, 0);
//            buf.setMat4(0, Engine.getIdentityMatrix());
//            buf.setInt(16, 1);
//            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
//			cubesShadow.bindAndDraw(commandBuffer);   
//            Engine.setViewport(0, mapSize, mapSize, mapSize, 1f, 0);
//            buf.setMat4(0, Engine.getIdentityMatrix());
//            buf.setInt(16, 2);
//            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
//			cubesShadow.bindAndDraw(commandBuffer);   
//	        Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());
            Engine.endRenderPass();
            

            VkRenderPasses.passShadow.getClearValueDepth().set(Engine.INVERSE_MAP?0:1, 0);
            Engine.beginRenderPass(VkRenderPasses.passShadow, this.frameBufferShadow2, VK_SUBPASS_CONTENTS_INLINE);
            Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureShadowDepth);
            Engine.bindPipeline(VkPipelines.shadowDebug);
            if (!Engine.INVERSE_MAP)
            Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize(), 0, 1);
            else 
            	Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize(), 1, 0);
            buf.setMat4(0, Engine.getIdentityMatrix());
            buf.setInt(16, 2);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowDebug.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
			cubesShadow.bindAndDraw(commandBuffer);   
//          Engine.clearDepth();

            Engine.endRenderPass();
            Engine.clearDescriptorSet(VkDescLayouts.UBO_CONSTANTS_DESC_IDX);
            
        } else {
        	System.err.println("SKIPPED, framebuffer is not sized");
        	System.err.printf("%dx%d vs %dx%d vs %dx%d vs %dx%d\n", 
        			this.frameBufferShadow.getWidth(), this.frameBufferShadow.getHeight(),
        			Engine.displayWidth, Engine.displayHeight,  
        			windowWidth, windowHeight,
        			vkContext.swapChain.width, vkContext.swapChain.height);

        }
        Engine.updateRenderResolution(windowWidth, windowHeight);
        Engine.setViewport(0, 0, windowWidth, windowHeight);
        if (Engine.displayWidth != vkContext.swapChain.width || Engine.displayHeight != vkContext.swapChain.height) {
        	System.err.println("swapchain size != display size");
        	System.err.printf("%dx%d vs %dx%d vs %dx%d\n", windowWidth, windowHeight, Engine.displayWidth, Engine.displayHeight, vkContext.swapChain.width, vkContext.swapChain.height);
        } else {
            if (this.frameBufferScene.getWidth() == windowWidth&&this.frameBufferScene.getHeight() == windowHeight)
            {
            	
                Engine.beginRenderPass(VkRenderPasses.passTerrain_Pass0, this.frameBufferScene, VK_SUBPASS_CONTENTS_INLINE);
                Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureTerrain);
                Engine.setDescriptorSet(VkDescLayouts.UBO_CONSTANTS_DESC_IDX, Engine.descriptorSetUboConstants);
//                
                Engine.bindPipeline(VkPipelines.terrain);
                this.vBuf.draw(commandBuffer, 0);
                Engine.endRenderPass();
            } else {
            	System.err.println("SKIPPED, framebuffer is not sized");
            	System.err.printf("%dx%d vs %dx%d vs %dx%d vs %dx%d\n", 
            			this.frameBufferScene.getWidth(), this.frameBufferScene.getHeight(),
            			Engine.displayWidth, Engine.displayHeight,  
            			windowWidth, windowHeight,
            			vkContext.swapChain.width, vkContext.swapChain.height);

            }
            {
                VkRenderPasses.passFramebuffer.getClearValueDepth().set(0, 0);
                Engine.beginRenderPass(VkRenderPasses.passFramebuffer, this.frameBuffer, VK_SUBPASS_CONTENTS_INLINE);

                Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureCubeShadowMap);
                Engine.setDescriptorSet(VkDescLayouts.UBO_CONSTANTS_DESC_IDX, Engine.descriptorSetUboShadow);
                Engine.bindPipeline(VkPipelines.main);
                Engine.setViewport(0, 0, windowWidth, windowHeight, 1, 0);

        		if(cube.vertexcount > 0)
        		cube.bindAndDraw(commandBuffer);
//        		if(plane.vertexcount > 0)
//        		plane.bindAndDraw(commandBuffer);

                Engine.clearDescriptorSet(VkDescLayouts.UBO_CONSTANTS_DESC_IDX);

        		VkTess tess = VkTess.instance;
                Engine.clearDepth();
                Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureShadowDepth);
                Engine.bindPipeline(VkPipelines.debugShader);
//
        		tess.setColor(-1, 255);
        		tess.add(0, 0, 0, 0, 0);
        		tess.add(0, windowHeight, 0, 0, 1);
        		tess.add(windowWidth, windowHeight, 0, 1, 1);
        		tess.add(windowWidth, 0, 0, 1, 0);
        		tess.drawQuads();
        		tess.setOffset(0, 0, 0);
//                Engine.setDescriptorSet(1, this.descTextureShadowDepth);
//                Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureGbufferColor);
//                Engine.bindPipeline(VkPipelines.debugShader);
//
////        		tess.setOffset(400, 50, 0);
//        		tess.setColor(-1, 255);
//        		tess.add(windowWidth, 0, 0, 1, 0);
//        		tess.add(0, 0, 0, 0, 0);
//        		tess.add(0, windowHeight, 0, 0, 1);
//        		tess.add(windowWidth, windowHeight, 0, 1, 1);
//        		tess.drawQuads();
//        		tess.setOffset(0, 0, 0);

        		/*
        		 * 
                Engine.clearDescriptorSet(VkDescLayouts.TEX_DESC_IDX);
        		Engine.setPipeStateColored2D();
        		tess.setColor(0x0, 180);
        		tess.add(300, windowHeight-600, 0);
        		tess.add(0, windowHeight-600, 0);
        		tess.add(0, windowHeight, 0);
        		tess.add(300, windowHeight, 0);
        		tess.drawQuads();
        		

                LineGUI.INST.start(4F);
                LineGUI.INST.add(32, 32, 0, -1, 1f);
                LineGUI.INST.add(128, 32, 0, -1, 1f);
                LineGUI.INST.add((128-32)/2+32, 128, 0, 0xff00ff, 1f);
                LineGUI.INST.add(32, 32, 0, -1, 1f);
                LineGUI.INST.drawLines();

                Engine.pxStack.push(10, 10, 0);
        		this.font.drawString("test string hello", 0, Engine.displayHeight-40, -1, true, 0.5f);
                Engine.pxStack.pop();

                Engine.clearDescriptorSet(VkDescLayouts.TEX_DESC_IDX);
                
            	BoxGUI.reset();
            	BoxGUI.setBox(100, 350, 200, 190);
                BoxGUI.INST.drawQuad();

                {
                    Engine.setDescriptorSet(VkDescLayouts.TEX_DESC_IDX, this.descTextureItem);
                    Engine.setPipeStateItem();
                    float x = 400;
                    float y = 400;
                    float h = 32;
                    float w = 32;
                    tess.setColorF(-1, 1);
                    tess.setUIntLSB(1);
                    tess.add(x+w, y+0, 0, 1, 1);
                    tess.add(x+0, y+0, 0, 0, 1);
                    tess.add(x+0, y+h, 0, 0, 0);
                    tess.add(x+w, y+h, 0, 1, 0);
                    tess.drawQuads();
                    tess.resetState();
                }
        		 */

                double mx = Mouse.getX();
                double my = Mouse.getY();
        		
                if (this.gui != null)
                	this.gui.render(fTime, mx, my);
        		
                Engine.endRenderPass();
                
            }


            vkContext.swapChain.blitFramebufferAndPreset(commandBuffer, frameBuffer, 0);
        }
        
    }
    
	public void shutdown() {
        if(DEBUG_LAYER) System.err.println("TexturedMesh_VK.shutdown");
        if (isVulkan)
            vkContext.syncAllFences();
        RenderAssets.destroy();
    	vkDestroyImageView(vkContext.device, textureView, null);
    	vkDestroySampler(vkContext.device, this.sampler, null);
    	vkDestroySampler(vkContext.device, this.samplerShadowMap, null);
    	this.frameBufferScene.destroy();
    	this.frameBufferShadow.destroy();
    	this.frameBufferShadow2.destroy();
    	this.texture.destroy();
    	super.shutdown();
    }
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		if (this.gui != null) {

            this.gui.onWheelScroll(xoffset, yoffset);
		}
	}
}
