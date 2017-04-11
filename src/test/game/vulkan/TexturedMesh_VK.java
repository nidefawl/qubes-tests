package test.game.vulkan;


import static org.lwjgl.vulkan.VK10.*;


import org.lwjgl.glfw.GLFW;
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
import nidefawl.qubes.shader.UniformBuffer;
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
	private VkTesselatorState cubesShadow;
	VkTesselatorState cube;
	VkTesselatorState plane;
	
	private VkDescriptor descTextureTerrainOnly;
	private VkDescriptor descTextureTerrain;
	
	
	VkViewport.Buffer viewport = VkViewport.calloc(1);


	private static long samplerShadowMap;
	
	
	private FontRenderer font;

	static class PerFrame {

		private FrameBuffer frameBufferShadow;
		private FrameBuffer frameBuffer;
		private VkDescriptor descTextureShadowDepth;
		private VkDescriptor descTextureCubeShadowMap;
		public void setRes(int displayWidth, int displayHeight) {
			if (this.frameBuffer != null) {
				this.frameBuffer.destroy();
    			this.frameBuffer.build(VkRenderPasses.passFramebuffer, displayWidth, displayHeight);
			}
			if (this.frameBufferShadow != null) {
				this.frameBufferShadow.destroy();
    			this.frameBufferShadow.build(VkRenderPasses.passShadow, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());

			}
		}
		public void makeFB(VKContext vkContext) {

			this.frameBufferShadow = new FrameBuffer(vkContext);
			this.frameBufferShadow.fromRenderpass(VkRenderPasses.passShadow, VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_USAGE_SAMPLED_BIT);

			this.frameBuffer = new FrameBuffer(vkContext);
			this.frameBuffer.fromRenderpass(VkRenderPasses.passFramebuffer, 0, VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
		}
		public void destroy() {
	    	this.frameBufferShadow.destroy();
	    	this.frameBuffer.destroy();
		}
		public void makeDesc(VKContext vkContext) {
	    	this.descTextureCubeShadowMap = vkContext.descLayouts.allocDescSetSamplerDouble();
	    	this.descTextureShadowDepth = vkContext.descLayouts.allocDescSetSampleSingle();
		}
		public void updateDesc(VKContext vkContext) {

	        FramebufferAttachment depthatt = this.frameBufferShadow.getAtt(0);
			this.descTextureCubeShadowMap.setBindingCombinedImageSampler(0, 
					TextureArrays.blockTextureArrayVK.getView(), 
					TextureArrays.blockTextureArrayVK.getSampler(), 
					TextureArrays.blockTextureArrayVK.getImageLayout());
			this.descTextureCubeShadowMap.setBindingCombinedImageSampler(1, 
					depthatt.getView(), 
					samplerShadowMap, 
					depthatt.finalLayout);

			

			this.descTextureShadowDepth.setBindingCombinedImageSampler(0, 
					depthatt.getView(), 
					samplerShadowMap, 
					depthatt.finalLayout);
	        this.descTextureCubeShadowMap.update(vkContext);
	        this.descTextureShadowDepth.update(vkContext);
		}
		
	}
	PerFrame[] perFrame = new PerFrame[12];
	
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
    				Engine.INVERSE_Z_BUFFER = !Engine.INVERSE_Z_BUFFER;
    				VkRenderPasses.initClearValues(Engine.INVERSE_Z_BUFFER);
                    vkContext.reinitSwapchain = true;
//            		setSceneViewport();
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
        createRenderCommandBuffers(perFrame[buffer.frameIdx], buffer, f);
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

            for (int i = 0; i < perFrame.length; i++)
    		{
            	this.perFrame[i].setRes(displayWidth, displayHeight);

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
		EngineInitSettings settings = EngineInitSettings.INIT_NONE.setFBSize(windowWidth, windowHeight).setVulkan(true);
		settings.initShadowProj = true;
		Engine.init(settings);
        GameBase.loadingScreen = new LoadingScreen();
		if (loadingScreen != null)
        loadingScreen.setProgress(0, 0, "Initializing");
		setVSync(false);
		camera = new Camera();
		this.cameraController.set(98.31f, 88.73f, -130.53f, 26.86f, 224.16f);
		if (loadingScreen != null)
        loadingScreen.setProgress(0, 1, "Something done");
        for (int i = 0; i < perFrame.length; i++)
		{
        	this.perFrame[i] = new PerFrame();
        	this.perFrame[i].makeFB(vkContext);

		}

	}

	@Override
	public void lateInitGame() {
		if (loadingScreen != null)
        loadingScreen.setProgress(1, 0, "lateInitGame");
        RenderAssets.load(null, loadingScreen);

    	this.descTextureTerrain = vkContext.descLayouts.allocDescSetSamplerDouble();
    	this.descTextureTerrainOnly = vkContext.descLayouts.allocDescSetSampleSingle();
        for (int i = 0; i < perFrame.length; i++)
		{
        	this.perFrame[i].makeDesc(vkContext);

		}
        this.font = FontRenderer.get(0, 22, 1);
		if (loadingScreen != null)
			loadingScreen.setProgress(1, 1f, "done");
		samplerShadowMap = vkContext.samplerLinearClamp;
		cube = new VkTesselatorState(vkContext).tag("cubes");
		plane = new VkTesselatorState(vkContext).tag("plane");
		cubesShadow = new VkTesselatorState(vkContext).tag("cubesShadow");
		drawScene();
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

        for (int i = 0; i < perFrame.length; i++)
		{
        	this.perFrame[i].updateDesc(vkContext);

		}
        this.descTextureTerrain.update(vkContext);
        this.descTextureTerrainOnly.update(vkContext);
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
    private void createRenderCommandBuffers(PerFrame frame, CommandBuffer commandBuffer, float fTime) {
        Engine.updateRenderResolution(Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());
        Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());
		setSceneViewport();
        Engine.disableAutoBindDesc();
        Engine.setDescriptorSet(VkDescLayouts.DESC0, Engine.descriptorSetUboScene);
//        System.out.println(TextureArrays.blockTextureArray.totalSlots);
        if (frame.frameBufferShadow.getWidth() == Engine.getShadowMapTextureSize()&&frame.frameBufferShadow.getHeight() == Engine.getShadowMapTextureSize())
        {
            PushConstantBuffer buf = PushConstantBuffer.INST;
            VkRenderPasses.passShadow.getClearValueDepth().set(Engine.INVERSE_Z_BUFFER?0:1, 0);
            boolean render=true;
            boolean pass = true;
            if (pass) {
              Engine.beginRenderPass(VkRenderPasses.passShadow, frame.frameBufferShadow);
              if (render) {
	              Engine.setDescriptorSet(VkDescLayouts.DESC1, descTextureTerrainOnly);
	              Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboShadow);
	              Engine.bindPipeline(VkPipelines.shadowSolid);
	              if (!Engine.INVERSE_Z_BUFFER)
	              Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize(), 0, 1);
	              else 
	              	Engine.setViewport(0, 0, Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize(), 1, 0);
	              buf.setMat4(0, Engine.getIdentityMatrix());
	              buf.setInt(16, 2);
	              vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.shadowSolid.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT, 0, buf.getBuf(64+4));
	              float f = Engine.INVERSE_Z_BUFFER?-1:1;
//	              vkCmdSetDepthBias(commandBuffer, f*1.15f, 0.0f, f*1.15f);
	              cubesShadow.bindAndDraw(commandBuffer);   
              }
              Engine.endRenderPass();
              vkCmdPipelineBarrier(Engine.getDrawCmdBuffer(),
                      VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 
                      VK_DEPENDENCY_BY_REGION_BIT, VKContext.BARRIER_MEM_ATT_WRITE_SHADER_READ, null, null);
            }
            

            
        } else {
        	System.err.println("SKIPPED, framebuffer is not sized");
        	System.err.printf("%dx%d vs %dx%d vs %dx%d vs %dx%d\n", 
        			frame.frameBufferShadow.getWidth(), frame.frameBufferShadow.getHeight(),
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
//            System.out.println(new Matrix4f().loadTranspose(Engine.shadowProj.getSMVP(2)));
            {
                VkRenderPasses.passFramebuffer.getClearValueDepth().set(0, 0);
                Engine.beginRenderPass(VkRenderPasses.passFramebuffer, frame.frameBuffer);

                Engine.setDescriptorSet(VkDescLayouts.DESC1, frame.descTextureCubeShadowMap);
                Engine.setDescriptorSet(VkDescLayouts.DESC2, Engine.descriptorSetUboShadow);
                Engine.bindPipeline(VkPipelines.main);
                if (!Engine.INVERSE_Z_BUFFER)
                    Engine.setViewport(0, 0, windowWidth, windowHeight, 0, 1);
                    else 
                    	Engine.setViewport(0, 0, windowWidth, windowHeight, 1, 0);

        		if(cube.vertexcount > 0)
        		cube.bindAndDraw(commandBuffer);
//        		if(plane.vertexcount > 0)
//        		plane.bindAndDraw(commandBuffer);

                Engine.enableAutoBindDesc();

        		VkTess tess = VkTess.instance;
                Engine.clearDepth();
                Engine.setDescriptorSet(VkDescLayouts.DESC2, frame.descTextureShadowDepth);
                Engine.bindPipeline(VkPipelines.debugShader);
                float asp = windowHeight/(float)windowWidth;
                int w = 420;
                int h = (int) (w*asp);
                int x = 0;
                int y = 0;
        		tess.setColor(-1, 255);
        		tess.add(x, y, 0, 0, 0);
        		tess.add(x, y+h, 0, 0, 1);
        		tess.add(x+w, y+h, 0, 1, 1);
        		tess.add(x+w, y, 0, 1, 0);
        		tess.drawQuads();
        		tess.setOffset(0, 0, 0);
        		y+=h+20;
        		
                Engine.endRenderPass();
                
            }


            vkContext.swapChain.blitFramebufferAndPreset(commandBuffer, frame.frameBuffer, 0);
        }
        
    }
    
	public void shutdown() {
        if(DEBUG_LAYER) System.err.println("TexturedMesh_VK.shutdown");
        if (isVulkan)
            vkContext.syncAllFences();
        RenderAssets.destroy();
    	for (int i = 0; i < this.perFrame.length; i++) {
    		this.perFrame[i].destroy();
    	}
    	super.shutdown();
    }
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		if (this.gui != null) {

            this.gui.onWheelScroll(xoffset, yoffset);
		}
	}
}
