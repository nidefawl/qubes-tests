package test.game.streamingtest;


import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.Random;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManagerClient;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.async.AsyncTask;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.LoadingScreen;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.render.gui.BoxGUI;
import nidefawl.qubes.render.gui.LineGUI;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.texture.array.TextureArray;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.GameContext;
import nidefawl.qubes.util.Side;
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

	private VkCommandBuffer[] renderCommandBuffers;

	private FrameBuffer frameBuffer;
	private FrameBuffer frameBufferScene;
	private VkTesselatorState cubesShadow;
	private VkDescriptor descTextureCube;
	private VkDescriptor descTextureShadowColorDBG;
	private VkDescriptor descTextureShadowDepth;
	private VkDescriptor descTextureCubeShadowMap;
	private VkDescriptor descTextureGbufferColor;
	
	
    VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .pNext(NULL).flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
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
	
	@Override
	public void onStatsUpdated() {
//		String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+") bytes uploaded: "+Stats.uploadBytes;
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
    			case GLFW.GLFW_KEY_SPACE:
//    				renderModeReRecord = !renderModeReRecord;
//    				forceRedraw = true;
//    				Arrays.fill(recorded, false);
    				return;
    			}
    		}
        }
	}

	
	
	@Override
	public void render(float f) {
		int currentBuffer = VKContext.currentBuffer;
		VkCommandBuffer buffer = renderCommandBuffers[currentBuffer];
		vkResetCommandBuffer(buffer, 0);
        createRenderCommandBuffers(buffer, currentBuffer, f);
		this.vkContext.submitCommandBuffer(buffer);
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
        Engine.getSunLightModel().setTime(5850);
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
		int l = 8;
		float d = 5;
		float scale = 2.0f;
		for (int x = -l; x <= l; x++) {
			for (int z = -l; z <= l; z++) {
				for (int y = -l; y <= l; y++) {
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
//		setVSync(false);
		camera = new Camera();
		AssetTexture bin1 = AssetManagerClient.getInstance().loadPNGAsset("textures/blocks/ground/dirt.png");
		
		AssetTexture bin2 = AssetManagerClient.getInstance().loadPNGAsset("textures/blocks/ground/sand.png");
		texture2dData1 = new TextureBinMips(bin1);
		texture2dData2 = new TextureBinMips(bin2);
		this.cameraController.set(-1.58f, 91.01f, 1.50f, 38.64f, 33.00f);
		try {
			Thread.sleep(600);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (loadingScreen != null)
        loadingScreen.setProgress(0, 1, "Something done");
	}

	@Override
	public void lateInitGame() {
		if (loadingScreen != null)
        loadingScreen.setProgress(1, 0, "lateInitGame");
		loadTexture(this.texture2dData1, this.texture2dData2);

        TextureArray[] arrays = TextureArrays.init();
        for (int i = 0; i < arrays.length; i++) {
            final TextureArray arr = arrays[i];
            AsyncTasks.submit(new AsyncTask() {
                @Override
                public void pre() {
                    try {
                        arr.preUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void post() {
                    arr.postUpdate();
                }
                @Override
                public Void call() throws Exception {
                    try {
                        arr.load();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                @Override
                public TaskType getType() {
                    return TaskType.LOAD_TEXTURES;
                }
            });
        }
        while(!AsyncTasks.completeTasks()) {
            float pr = 0;
            for (int i = 0; i < arrays.length; i++) {
                pr+=arrays[i].getProgress();
            }
            pr/=(float)arrays.length;
            loadingScreen.setProgress(1, pr, "Loading...");
        }
		
    	this.descTextureCube = vkContext.descLayouts.allocDescSetSampleSingle();
    	this.descTextureCubeShadowMap = vkContext.descLayouts.allocDescSetSamplerDouble();
    	this.descTextureGbufferColor = vkContext.descLayouts.allocDescSetSampleSingle();
    	this.descTextureShadowColorDBG = vkContext.descLayouts.allocDescSetSampleSingle();
    	this.descTextureShadowDepth = vkContext.descLayouts.allocDescSetSampleSingle();
        this.font = FontRenderer.get(0, 22, 1);
//		showGUI(new GuiTest());
		if (loadingScreen != null)
			loadingScreen.setProgress(1, 1f, "done");
		this.frameBuffer = new FrameBuffer(vkContext);
		this.frameBuffer.fromRenderpass(VkRenderPasses.passTerrain, 0, VK_IMAGE_USAGE_SAMPLED_BIT);

		this.frameBufferScene = new FrameBuffer(vkContext);
		this.frameBufferScene.fromRenderpass(VkRenderPasses.passShadow, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);

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
	}
	private void updateDescriptorSets() {
		this.descTextureCube.setBindingCombinedImageSampler(0, textureView, sampler, texture.imageLayout);
		
        FramebufferAttachment coloratt = this.frameBuffer.getAtt(0);
		this.descTextureGbufferColor.setBindingCombinedImageSampler(0, coloratt.getView(), sampler, coloratt.imageLayout);
		
        FramebufferAttachment depthatt = this.frameBufferScene.getAtt(0);
		this.descTextureCubeShadowMap.setBindingCombinedImageSampler(0, 
				TextureArrays.blockTextureArrayVK.getView(), 
				TextureArrays.blockTextureArrayVK.getSampler(), 
				TextureArrays.blockTextureArrayVK.getImageLayout());
		this.descTextureCubeShadowMap.setBindingCombinedImageSampler(1, 
				depthatt.getView(), 
				samplerShadowMap, 
				depthatt.imageLayout);

        FramebufferAttachment shadowColorAtt = this.frameBufferScene.getAtt(1);
		this.descTextureShadowColorDBG.setBindingCombinedImageSampler(0, 
				shadowColorAtt.getView(), 
				sampler, 
				shadowColorAtt.imageLayout);
		
		
		this.descTextureShadowDepth.setBindingCombinedImageSampler(0, 
				depthatt.getView(), 
				samplerShadowMap, 
				depthatt.imageLayout);
        this.descTextureCube.update(vkContext);
        this.descTextureGbufferColor.update(vkContext);
        this.descTextureCubeShadowMap.update(vkContext);
        this.descTextureShadowColorDBG.update(vkContext);
        this.descTextureShadowDepth.update(vkContext);
	}


	private void loadTexture(TextureBinMips... texture2dData) {
		int vkFormat = VK_FORMAT_R8G8B8A8_UNORM;

        try ( MemoryStack stack = stackPush() ) {

			this.texture = new VkTexture(vkContext);
			texture.build(vkFormat, texture2dData);
			
			VkSamplerCreateInfo sampler = VkSamplerCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
					.magFilter(VK_FILTER_LINEAR)
					.minFilter(VK_FILTER_LINEAR)
					.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
					.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
					.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
					.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
					.mipLodBias(0.0f)
					.compareOp(VK_COMPARE_OP_NEVER)
					.minLod(0.0f);
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
	        view.image(this.texture.image);
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
	
    private void createRenderCommandBuffers(VkCommandBuffer commandBuffer, int currentBuf, float fTime) {

    	long fbSwapchain = vkContext.swapChain.framebuffers[currentBuf];
        int err = vkBeginCommandBuffer(commandBuffer, this.cmdBufInfo);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to begin render command buffer: " + VulkanErr.toString(err));
        }
        Engine.updateRenderResolution(Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());
        Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());

        if (this.frameBufferScene.getWidth() == Engine.getShadowMapTextureSize()&&this.frameBufferScene.getHeight() == Engine.getShadowMapTextureSize())
        {
        	viewport.minDepth(0.0f);
        	viewport.maxDepth(1.0f);
            PushConstantBuffer buf = PushConstantBuffer.INST;
            int mapSize = Engine.getShadowMapTextureSize()/2;
            
            Engine.beginRenderPass(commandBuffer, VkRenderPasses.passShadow, this.frameBufferScene.get(), VK_SUBPASS_CONTENTS_INLINE);

            Engine.clearDescriptorSet(1);
            Engine.bindPipeline(VkPipelines.shadowSolid);
            float f = -1.0f;
            vkCmdSetDepthBias(commandBuffer, f*1.0f, f*0.2f, f*0.2f);
//            vkCmdSetDepthBias(commandBuffer, 0,0,0);
            vkCmdSetViewport(commandBuffer, 0, viewport.x(0).y(0).width(mapSize).height(mapSize));
            buf.setMat4(0, Engine.getIdentityMatrix());
            buf.setInt(16, 0);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
			cubesShadow.bindAndDraw(commandBuffer);   
            vkCmdSetViewport(commandBuffer, 0, viewport.x(mapSize).y(0).width(mapSize).height(mapSize));
            buf.setMat4(0, Engine.getIdentityMatrix());
            buf.setInt(16, 1);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
			cubesShadow.bindAndDraw(commandBuffer);   
            vkCmdSetViewport(commandBuffer, 0, viewport.x(0).y(mapSize).width(mapSize).height(mapSize));
            buf.setMat4(0, Engine.getIdentityMatrix());
            buf.setInt(16, 2);
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
			cubesShadow.bindAndDraw(commandBuffer);        		
            vkCmdEndRenderPass(commandBuffer);
        } else {
        	System.err.println("SKIPPED, framebuffer is not sized");
        	System.err.printf("%dx%d vs %dx%d vs %dx%d vs %dx%d\n", 
        			this.frameBufferScene.getWidth(), this.frameBufferScene.getHeight(),
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
            if (this.frameBuffer.getWidth() == windowWidth&&this.frameBuffer.getHeight() == windowHeight)
            {
            	
//                Engine.beginRenderPass(commandBuffer, VkRenderPasses.passTerrain, this.frameBuffer.get(), VK_SUBPASS_CONTENTS_INLINE);
//                
//                Engine.setDescriptorSet(1, this.descTextureCube);
//                Engine.bindPipeline(VkPipelines.terrain);
//                
//        		if(cube.idxCount > 0)
//        		cube.bindAndDraw(commandBuffer);
//        		if(plane.idxCount > 0)
//        		plane.bindAndDraw(commandBuffer);
//        		
//                vkCmdEndRenderPass(commandBuffer);
            } else {
            	System.err.println("SKIPPED, framebuffer is not sized");
            	System.err.printf("%dx%d vs %dx%d vs %dx%d vs %dx%d\n", 
            			this.frameBuffer.getWidth(), this.frameBuffer.getHeight(),
            			Engine.displayWidth, Engine.displayHeight,  
            			windowWidth, windowHeight,
            			vkContext.swapChain.width, vkContext.swapChain.height);

            }
            {
                Engine.beginRenderPass(commandBuffer, VkRenderPasses.passSubpassSwapchain, fbSwapchain, VK_SUBPASS_CONTENTS_INLINE);
                
                Engine.setDescriptorSet(1, this.descTextureCubeShadowMap);
                Engine.bindPipeline(VkPipelines.main);

        		if(cube.idxCount > 0)
        		cube.bindAndDraw(commandBuffer);
        		if(plane.idxCount > 0)
        		plane.bindAndDraw(commandBuffer);

        		vkCmdNextSubpass(commandBuffer, VK_SUBPASS_CONTENTS_INLINE);

        		VkTess tess = VkTess.instance;
                
                Engine.setDescriptorSet(1, this.descTextureShadowDepth);
//                Engine.setDescriptorSet1(this.descTextureGbufferColor);
                Engine.bindPipeline(VkPipelines.debugShader);

        		tess.setOffset(400, 50, 0);
        		tess.setColor(-1, 255);
        		tess.add(320, 0, 0, 1, 0);
        		tess.add(0, 0, 0, 0, 0);
        		tess.add(0, 320, 0, 0, 1);
        		tess.add(320, 320, 0, 1, 1);
        		tess.drawQuads();
        		tess.setOffset(0, 0, 0);

                Engine.clearDescriptorSet(1);
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

                Engine.clearDescriptorSet(1);
                
            	BoxGUI.reset();
            	BoxGUI.setBox(100, 350, 200, 190);
                BoxGUI.INST.drawQuad();
                

                double mx = Mouse.getX();
                double my = Mouse.getY();
        		
                if (this.gui != null)
                	this.gui.render(fTime, mx, my);
        		
                vkCmdEndRenderPass(commandBuffer);
            }
        }
        
        err = vkEndCommandBuffer(commandBuffer);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to end render command buffer: " + VulkanErr.toString(err));
        }
    }
	@Override
	public void rebuildRenderCommands(int width, int height) {
		vkContext.resetRenderCommandPool();
		{
			if (this.frameBuffer != null) {
				this.frameBuffer.destroy();
			}
			this.frameBuffer.build(VkRenderPasses.passTerrain, width, height);
			if (this.frameBufferScene != null) {
				this.frameBufferScene.destroy();
			}
			this.frameBufferScene.build(VkRenderPasses.passShadow, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());

		}
		
		
    	int nFrameBuffers = vkContext.swapChain.numImages;
        try ( MemoryStack stack = stackPush() ) {

    		VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkInitializers.commandBufferAllocInfo(
            		vkContext.renderCommandPool, nFrameBuffers);
        
            PointerBuffer pCommandBuffer = stack.callocPointer(nFrameBuffers);
            int err = vkAllocateCommandBuffers(vkContext.device, cmdBufAllocateInfo, pCommandBuffer);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to allocate render command buffer: " + VulkanErr.toString(err));
            }
            renderCommandBuffers = new VkCommandBuffer[nFrameBuffers];
            for (int i = 0; i < nFrameBuffers; i++) {
                renderCommandBuffers[i] = new VkCommandBuffer(pCommandBuffer.get(i), vkContext.device);
            }
        }
        updateDescSets = true;
		needsRedraw=true;
	}

	private void destroyCommandBuffers() {
    	if (renderCommandBuffers != null) {
    		for (int i = 0; i < renderCommandBuffers.length; i++) {
    			VkCommandBuffer cmdBuf = renderCommandBuffers[i];
    			vkFreeCommandBuffers(vkContext.device, vkContext.renderCommandPool, cmdBuf);
    		}
    		renderCommandBuffers = null;
    	}
	}
	public void shutdown() {
    	destroyCommandBuffers();
    	vkDestroyImageView(vkContext.device, textureView, null);
    	vkDestroySampler(vkContext.device, sampler, null);
    	this.frameBuffer.destroy();
    	this.frameBufferScene.destroy();
    	this.texture.destroy();
    	super.shutdown();
    	cmdBufInfo.free();
    }
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		if (this.gui != null) {

            this.gui.onWheelScroll(xoffset, yoffset);
		}
	}
}
