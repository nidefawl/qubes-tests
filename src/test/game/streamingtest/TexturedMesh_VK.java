package test.game.streamingtest;


import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.*;
import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.*;
import nidefawl.qubes.gui.controls.*;
import nidefawl.qubes.gui.controls.ComboBox.ComboBoxList;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.render.gui.BoxGUI;
import nidefawl.qubes.render.gui.LineGUI;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.vulkan.FrameBuffer;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.biomes.HexBiome;

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

    final CameraController cameraController = new CameraController();
	private boolean needsRedraw = true;

	@Override
	public void onStatsUpdated() {
		String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+") bytes uploaded: "+Stats.uploadBytes;
		setTitle(stats);
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

	int bufferSize = 12312;
	Random rand = new Random();
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
        Engine.getSunLightModel().setTime(5850);
//      Engine.getSunLightModel().setTime(1700+(int)((ticksran+f)*32));
      Engine.getSunLightModel().updateFrame(0);
      Engine.setLightPosition(Engine.getSunLightModel().getLightPosition());
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
	}

	private void drawScene() {
		VkTess tess = VkTess.instance;
		int l = 8;
		float d = 5;
		for (int x = -l; x <= l; x++) {
			for (int z = -l; z <= l; z++) {
				for (int y = -l; y <= l; y++) {
					tess.setOffset(x*d, y*d, z*d);
					tess.setNormals(0, 0, 1);
					tess.add( 1,  1, 1, 1, 1);
					tess.add(-1,  1, 1, 0, 1);
					tess.add(-1, -1, 1, 0, 0);
					tess.add( 1, -1, 1, 1, 0);
					tess.setNormals(0, 0, -1);
					tess.add(-1, -1, -1, 0, 0);
					tess.add(-1,  1, -1, 0, 1);
					tess.add( 1,  1, -1, 1, 1);
					tess.add( 1, -1, -1, 1, 0);
					tess.setNormals(0, 1, 0);
					tess.add(-1,  1, -1, 0, 0);
					tess.add(-1,  1,  1, 0, 1);
					tess.add( 1,  1,  1, 1, 1);
					tess.add( 1,  1, -1, 1, 0);
					tess.setNormals(0, -1, 0);
					tess.add(-1, -1,  1, 0, 1);
					tess.add(-1, -1, -1, 0, 0);
					tess.add( 1, -1, -1, 1, 0);
					tess.add( 1, -1,  1, 1, 1);
					tess.setNormals(1, 0, 0);
					tess.add( 1, -1, -1, 0, 1);
					tess.add( 1, -1,  1, 0, 0);
					tess.add( 1,  1,  1, 1, 0);
					tess.add( 1,  1, -1, 1, 1);
					tess.setNormals(-1, 0, 0);
					tess.add(-1, -1, -1, 0, 1);
					tess.add(-1, -1,  1, 0, 0);
					tess.add(-1,  1,  1, 1, 0);
					tess.add(-1,  1, -1, 1, 1);
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
        if (isRunning()) {
            Game.displayWidth=displayWidth;
            Game.displayHeight=displayHeight;
            Engine.resize(displayWidth, displayHeight);
//            if (Game.GL_ERROR_CHECKS)
//                Engine.checkGLError("onResize");
//            GLDebugTextures.onResize();
        }
	}
    @Override
    public void onWindowResize(int displayWidth, int displayHeight) {
        if (!VR_SUPPORT||isStarting) {
            Game.displayWidth=displayWidth;
            Game.displayHeight=displayHeight;
            setRenderResolution(displayWidth, displayHeight);
        }
    }

	@Override
	public void initGame() {
		EngineInitSettings settings = EngineInitSettings.INIT_NONE.setVulkan(true).setInverseZ();
		settings.initShadowProj = true;
		Engine.init(settings);
        GameBase.loadingScreen = new LoadingScreen();
		if (loadingScreen != null)
        loadingScreen.setProgress(0, 0, "Initializing");
		setVSync(false);
		camera = new Camera();
		
		AssetBinary bin = AssetManagerClient.getInstance().loadBin("vulkan/texture.bin");
		texture2dData = new TextureBinMips(bin);
		this.cameraController.set(-1.58f, 0.01f, 1.50f, 0.00f, 17.64f);
		try {
			Thread.sleep(600);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (loadingScreen != null)
        loadingScreen.setProgress(0, 1, "Something done");
	}
	
	TextureBinMips texture2dData;
    public Camera         camera;
	private VkCommandBuffer[] renderCommandBuffers;
	private long descTextureCube;
	private AssetTexture t;
	private long sampler;
	private long textureView;
	private VkTexture texture;

	VkTesselatorState cube;
	VkTesselatorState plane;
	private FontRenderer font;
    VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .pNext(NULL).flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
	private FrameBuffer frameBuffer;
	private FrameBuffer frameBufferScene;
	private VkTesselatorState cubesShadow;
	@Override
	public void lateInitGame() {
		if (loadingScreen != null)
        loadingScreen.setProgress(1, 0, "lateInitGame");
		try {
			Thread.sleep(600);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (loadingScreen != null)
        loadingScreen.setProgress(1, 0.33f, "loadTexture");
		loadTexture(this.texture2dData);
		if (loadingScreen != null)
        loadingScreen.setProgress(1, 0.66f, "setupDescriptorSets");
    	this.descTextureCube = vkContext.descLayouts.allocDescSetSampleSingle();
    	this.descTextureGbufferColor = vkContext.descLayouts.allocDescSetSampleSingle();
        this.font = FontRenderer.get(0, 22, 1);
		showGUI(new GuiTest());
		if (loadingScreen != null)
        loadingScreen.setProgress(1, 1f, "done");
		this.frameBuffer = new FrameBuffer(vkContext);
		this.frameBuffer.fromRenderpass(VkRenderPasses.passgbuffer, 0, VK_IMAGE_USAGE_SAMPLED_BIT);

		this.frameBufferScene = new FrameBuffer(vkContext);
		this.frameBufferScene.fromRenderpass(VkRenderPasses.passGBufferSubpass, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
		

		cube = new VkTesselatorState(vkContext).tag("cubes");
		plane = new VkTesselatorState(vkContext).tag("plane");
		cubesShadow = new VkTesselatorState(vkContext).tag("cubesShadow");
		drawScene();
	}
	private void updateDescriptorSets() {
        try ( MemoryStack stack = stackPush() ) {
//        	vkContext.descLayouts.getDescriptorSets);
	        VkDescriptorImageInfo.Buffer textureDescriptor = VkDescriptorImageInfo.callocStack(1, stack);
	        textureDescriptor.imageView(textureView);
	        textureDescriptor.sampler(sampler);
	        textureDescriptor.imageLayout(texture.imageLayout);
            VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.callocStack(1, stack);
	        VkInitializers.writeDescriptorSet(writeDescriptorSet, 0, this.descTextureCube, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 0, textureDescriptor);
	        vkUpdateDescriptorSets(vkContext.device, writeDescriptorSet, null);
        }
        try ( MemoryStack stack = stackPush() ) {
//        	vkContext.descLayouts.getDescriptorSets);
	        VkDescriptorImageInfo.Buffer textureDescriptor = VkDescriptorImageInfo.callocStack(1, stack);
	        FramebufferAttachment att = this.frameBuffer.getAtt(0);
	        textureDescriptor.imageView(att.getView());
	        textureDescriptor.sampler(sampler);
	        textureDescriptor.imageLayout(att.imageLayout);
            VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.callocStack(1, stack);
	        VkInitializers.writeDescriptorSet(writeDescriptorSet, 0, this.descTextureGbufferColor, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 0, textureDescriptor);
	        vkUpdateDescriptorSets(vkContext.device, writeDescriptorSet, null);
        }
	}


	private void loadTexture(TextureBinMips texture2dData) {
		int vkFormat = VK_FORMAT_BC2_UNORM_BLOCK;

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
	        		.viewType(VK_IMAGE_VIEW_TYPE_2D)
	        		.format(vkFormat)
	        		.components(VkComponentMapping.callocStack(stack));
	        VkImageSubresourceRange viewSubResRange = view.subresourceRange();
	        viewSubResRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
	        viewSubResRange.baseMipLevel(0);
	        viewSubResRange.baseArrayLayer(0);
	        viewSubResRange.layerCount(1);
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

	

	float lastExtend = 1.0f;
	float extend = 1.0f;
	private VkBuffer tmpBuffer1;
	private VkBuffer tmpBuffer2;
	int nTick = 0;
	private long descTextureGbufferColor;
	@Override
	public void tick() {
		if (!isStarting) {
            if (this.gui != null) {
                this.gui.update();
            }
			this.cameraController.tickUpdate();
//			if (nTick++%40==0) {
//				if (this.rand.nextInt(4)==0) {
//					if (tmpBuffer1 != null) {
//						tmpBuffer1.destroy();
//					}
//					bufferSize = rand.nextInt(100000)+555;
//					tmpBuffer1 = vkContext.createBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
//							bufferSize, false, "dummy1");
//				}
//				if (this.rand.nextInt(4)==0) {
//					if (tmpBuffer2 != null) {
//						tmpBuffer2.destroy();
//					}
//					bufferSize = rand.nextInt(100000)+555;
//					tmpBuffer2 = vkContext.createBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
//							bufferSize, false, "dummy2");
//				}
//			}
		}
		lastExtend = extend;
		extend = 1.0f+GameMath.sin(ticksran*1.03f)*0.5f;
	}
	
    private void createRenderCommandBuffers(VkCommandBuffer commandBuffer, int currentBuf, float fTime) {

    	long fbSwapchain = vkContext.swapChain.framebuffers[currentBuf];
        int err = vkBeginCommandBuffer(commandBuffer, this.cmdBufInfo);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to begin render command buffer: " + VulkanErr.toString(err));
        }
        if (Game.displayWidth != vkContext.swapChain.width || Game.displayHeight != vkContext.swapChain.height) {
        	System.err.println("swapchain size != display size");
        	System.err.printf("%dx%d vs %dx%d vs %dx%d\n", windowWidth, windowHeight, Game.displayWidth, Game.displayHeight, vkContext.swapChain.width, vkContext.swapChain.height);
        } else {

            Engine.setViewport(0, 0, Game.displayWidth, Game.displayHeight);
//            {
//                Engine.beginRenderPass(commandBuffer, VkRenderPasses.passGBufferSubpass, this.frameBufferScene.get(), VK_SUBPASS_CONTENTS_INLINE);
    //
//                Engine.clearDescriptorSet1();
//                Engine.bindPipeline(VkPipelines.shadowSolid);
//                PushConstantBuffer buf = PushConstantBuffer.INST;
//                buf.setMat4(0, Engine.getIdentityMatrix());
//                buf.setInt(16, 0);
//                vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
    //
//        		if(cubesShadow.idxCount > 0)
//        			cubesShadow.bindAndDraw(commandBuffer);
//        		
//        		vkCmdNextSubpass(commandBuffer, VK_SUBPASS_CONTENTS_INLINE);
//        		
//                vkCmdEndRenderPass(commandBuffer);
//            }
//            {
//                Engine.beginRenderPass(commandBuffer, VkRenderPasses.passgbuffer, this.frameBuffer.get(), VK_SUBPASS_CONTENTS_INLINE);
//                
//                Engine.setDescriptorSet1(this.descTextureCube);
//                Engine.bindPipeline(VkPipelines.mainOffscreen);
//                
//        		if(cube.idxCount > 0)
//        		cube.bindAndDraw(commandBuffer);
//        		if(plane.idxCount > 0)
//        		plane.bindAndDraw(commandBuffer);
//        		
//                vkCmdEndRenderPass(commandBuffer);
//            }
            {

                Engine.beginRenderPass(commandBuffer, VkRenderPasses.passSubpassSwapchain, fbSwapchain, VK_SUBPASS_CONTENTS_INLINE);
                
                Engine.setDescriptorSet1(this.descTextureCube);
                Engine.bindPipeline(VkPipelines.main);

        		if(cube.idxCount > 0)
        		cube.bindAndDraw(commandBuffer);
        		if(plane.idxCount > 0)
        		plane.bindAndDraw(commandBuffer);

        		vkCmdNextSubpass(commandBuffer, VK_SUBPASS_CONTENTS_INLINE);

        		VkTess tess = VkTess.instance;
                Engine.clearDescriptorSet1();
                Engine.bindPipeline(VkPipelines.guiDebug);
        		tess.setColorRGBAF(1.0f, 0.8f, 0.0f, 0.6f);
                tess.add(200, 100, 0);
                tess.add(100, 100, 0);
                tess.add(100, 200, 0);
                tess.add(200, 200, 0);
        		tess.drawQuads();
                
//                Engine.setDescriptorSet1(this.descTextureGbufferColor);
                Engine.setDescriptorSet1(this.descTextureCube);
        		Engine.setPipeStateTextured2D();

        		tess.setOffset(400, 50, 0);
        		tess.setColor(-1, 255);
        		tess.add(0, 0, 0, 0, 0);
        		tess.add(320, 0, 0, 1, 0);
        		tess.add(320, 320, 0, 1, 1);
        		tess.add(0, 320, 0, 0, 1);
        		tess.drawQuads();
        		tess.setOffset(0, 0, 0);

                Engine.clearDescriptorSet1();
        		Engine.setPipeStateColored2D();
        		tess.setColor(0x0, 180);
        		tess.add(0, displayHeight-600, 0);
        		tess.add(300, displayHeight-600, 0);
        		tess.add(300, displayHeight, 0);
        		tess.add(0, displayHeight, 0);
        		tess.drawQuads();


                LineGUI.INST.start(4F);
                LineGUI.INST.add(32, 32, 0, -1, 1f);
                LineGUI.INST.add(128, 32, 0, -1, 1f);
                LineGUI.INST.add((128-32)/2+32, 128, 0, 0xff00ff, 1f);
                LineGUI.INST.add(32, 32, 0, -1, 1f);
                LineGUI.INST.drawLines();

                Engine.pxStack.push(10, 10, 0);
        		this.font.drawString("test string hello", 0, displayHeight-40, -1, true, 0.5f);
                Engine.pxStack.pop();

                Engine.clearDescriptorSet1();
                
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
	public void rebuildRenderCommands() {
		{
			if (this.frameBuffer != null) {
				this.frameBuffer.destroy();
			}
			this.frameBuffer.build(VkRenderPasses.passgbuffer, windowWidth, windowHeight);
//			if (this.frameBufferScene != null) {
//				this.frameBufferScene.destroy();
//			}
//			this.frameBufferScene.build(VkRenderPasses.passGBufferSubpass, windowWidth, windowHeight);

		}
		
		
		vkContext.resetRenderCommandPool();
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
		updateDescriptorSets();
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
