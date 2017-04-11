package test.game.vulkan;


import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;
import org.lwjgl.vulkan.VkViewport;

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
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.vulkan.FrameBuffer;

public class TestVkQueries extends GameBase {
	static {
		System.setProperty("renderer.vulkan", "true");
	}
	public TestVkQueries() {
		TICKS_PER_SEC = 20;
		DEBUG_LAYER = true;
	}
	public static void main(String[] args) {

        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestVkQueries().startGame();
	}

    public Camera         camera;
    final CameraController cameraController = new CameraController();
    

	private FrameBuffer frameBuffer;
	private FrameBuffer frameBuffer2;
	private VkDescriptor descTextureInput;
	
	
	VkViewport.Buffer viewport = VkViewport.calloc(1);


	
	
	private FontRenderer font;
	private VkTexture vkTex;
	private VkDescriptor descTextureFB;
	private AssetTexture t;
	private long querypool;
	
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
				case GLFW.GLFW_KEY_TAB:
					vkContext.reinitSwapchain=true;
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

	
	static class TimeQuery {

		int frame;
		int startQuery;
		int endQuery;
		public TimeQuery(int frame) {
			this.frame = frame;
			this.startQuery = frame*2+0;
			this.endQuery = frame*2+1;
		}
	}
	ArrayList<TimeQuery> list = new ArrayList<>();
	int frame = 0;
	private LongBuffer buffer;
	@Override
	public void render(float f) {
		CommandBuffer buffer = vkContext.getCurrentCmdBuffer();
		Engine.beginCommandBuffer(buffer);
		if (frame >= 16) {
			frame = 0;
		}
		TimeQuery q = new TimeQuery(frame++); 
		vkCmdResetQueryPool(buffer, this.querypool, q.startQuery, 2);
		vkCmdWriteTimestamp(buffer, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, this.querypool, q.startQuery);
        createRenderCommandBuffers(buffer, f);
		vkCmdWriteTimestamp(buffer, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, this.querypool, q.endQuery);
//		System.out.println("submitting "+q.startQuery+"-"+q.endQuery);
        list.add(q);
        Engine.endCommandBuffer();
		this.vkContext.submitCommandBuffer();
	}
	
	long lAvg = 0;
	int n = 0;
	@Override
	public void preRenderUpdate(float f) {
        for (int i = 0; i < this.list.size(); i++) {
        	TimeQuery q = this.list.get(i);
//        	System.out.println("req "+q.startQuery+"-"+q.endQuery);
        	vkGetQueryPoolResults(vkContext.device, this.querypool, q.startQuery, 2, this.buffer, 0L, VK_QUERY_RESULT_WITH_AVAILABILITY_BIT);
        	
    		if (this.buffer.get(0) != 0L && this.buffer.get(1) != 0L) {
    			long l = this.buffer.get(1) - this.buffer.get(0);
    			lAvg += l;
    			lAvg /= 2;
            	list.remove(i--);
    		}
        }
//        System.out.println(lAvg);
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

	@Override
	public void postRenderUpdate(float f) {
	}

	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        Engine.updateRenderResolution(displayWidth, displayHeight);
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);

    		{
    			if (this.frameBuffer != null) {
    				this.frameBuffer.destroy();
        			this.frameBuffer.build(VkRenderPasses.passFramebuffer, displayWidth, displayHeight);
        			FramebufferAttachment colorAtt = this.frameBuffer.getAtt(0);
        	    	this.descTextureFB.setBindingCombinedImageSampler(0, colorAtt.getView(), vkContext.samplerLinearClamp, colorAtt.finalLayout);
        	    	this.descTextureFB.update(vkContext);
    			}
    			if (this.frameBuffer2 != null) {
    				this.frameBuffer2.destroy();
        			this.frameBuffer2.build(VkRenderPasses.passFramebuffer, displayWidth, displayHeight);
    			}
    		}
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
		this.t = AssetManagerClient.getInstance().loadPNGAsset("textures/Unigine01.png");
		GLFW.glfwSetWindowSize(windowId, t.getWidth(), t.getHeight());
		EngineInitSettings settings = EngineInitSettings.INIT_NONE.setFBSize(windowWidth, windowHeight).setVulkan(true).setInverseZ();
		settings.initShadowProj = true;
		Engine.RENDER_SETTINGS.smaaMode=1;
		Engine.RENDER_SETTINGS.smaaPredication=false;
		Engine.init(settings);
        GameBase.loadingScreen = new LoadingScreen();
		if (loadingScreen != null)
        loadingScreen.setProgress(0, 0, "Initializing");
		setVSync(true);
		camera = new Camera();
		
		this.cameraController.set(98.31f, 88.73f, -130.53f, 26.86f, 224.16f);
		if (loadingScreen != null)
        loadingScreen.setProgress(0, 1, "Something done");

		this.frameBuffer = new FrameBuffer(vkContext);
		this.frameBuffer.fromRenderpass(VkRenderPasses.passFramebuffer, 0, VK_IMAGE_USAGE_SAMPLED_BIT|VK_IMAGE_USAGE_TRANSFER_SRC_BIT);

		this.frameBuffer2 = new FrameBuffer(vkContext);
		this.frameBuffer2.fromRenderpass(VkRenderPasses.passFramebuffer, 0, VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
		TextureBinMips texture2dData1 = new TextureBinMips(t.getData(), t.getWidth(), t.getHeight());
		this.vkTex = new VkTexture(vkContext);
		this.vkTex.build(VK_FORMAT_R8G8B8A8_UNORM, texture2dData1);
		this.vkTex.genView();

    	this.descTextureInput = vkContext.descLayouts.allocDescSetSampleSingle();
    	this.descTextureInput.setBindingCombinedImageSampler(0, this.vkTex.getView(), vkContext.samplerLinearClamp, this.vkTex.getImageLayout());
    	this.descTextureInput.update(vkContext);
    	this.descTextureFB = vkContext.descLayouts.allocDescSetSampleSingle();

        try ( MemoryStack stack = stackPush() ) {

        	VkQueryPoolCreateInfo pCreateInfo = VkQueryPoolCreateInfo.callocStack(stack);
        	pCreateInfo.sType(VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO);
        	pCreateInfo.pNext(0L);
        	pCreateInfo.flags(0);
        	pCreateInfo.queryType(VK_QUERY_TYPE_TIMESTAMP);
        	pCreateInfo.queryCount(32);
        	pCreateInfo.pipelineStatistics(0);
        	LongBuffer pQueryPool = stack.callocLong(1);
			int err = vkCreateQueryPool(vkContext.device, pCreateInfo, null, pQueryPool);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkCreateQueryPool failed: " + VulkanErr.toString(err));
            }
			this.querypool = pQueryPool.get(0);
        }
        this.buffer = MemoryUtil.memCallocLong(256);
	}

	@Override
	public void lateInitGame() {
		if (loadingScreen != null)
        loadingScreen.setProgress(1, 0, "lateInitGame");

    	
        this.font = FontRenderer.get(0, 22, 1);
		if (loadingScreen != null)
			loadingScreen.setProgress(1, 1f, "done");
//		showGUI(new GuiTest());
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
		setSceneViewport();
		boolean flip = false;
		boolean outputinput = false;
   
        if (this.frameBuffer.getWidth() == vkContext.swapChain.width&&this.frameBuffer.getHeight() == vkContext.swapChain.height)
        {
        	
            Engine.beginRenderPass(VkRenderPasses.passFramebuffer, this.frameBuffer);
            Engine.setDescriptorSet(VkDescLayouts.DESC2, this.descTextureInput);
            Engine.setPipeStateTextured2D(false);
    		VkTess tess = VkTess.instance;
            tess.setColor(-1, 255);
			if (flip) {
				tess.add(windowWidth, 0, 0, 1, 0);
				tess.add(0, 0, 0, 0, 0);
				tess.add(0, windowHeight, 0, 0, 1);
				tess.add(windowWidth, windowHeight, 0, 1, 1);
			} else {
				tess.add(windowWidth, 0, 0, 1, 1);
				tess.add(0, 0, 0, 0, 1);
				tess.add(0, windowHeight, 0, 0, 0);
				tess.add(windowWidth, windowHeight, 0, 1, 0);
			}
            tess.drawQuads();
            tess.setOffset(0, 0, 0);
            Engine.endRenderPass();
//            vkContext.swapChain.blitFramebufferAndPreset(commandBuffer, frameBuffer, 0);
        }
        Engine.disableAutoBindDesc();
        VkDescriptor smaaOutput = Engine.vkSMAAContext.render(this.descTextureFB, null, null);
        Engine.enableAutoBindDesc();

        if (this.frameBuffer2.getWidth() == vkContext.swapChain.width&&this.frameBuffer2.getHeight() == vkContext.swapChain.height)
        {
        	
            Engine.beginRenderPass(VkRenderPasses.passFramebuffer, this.frameBuffer2);
            Engine.setDescriptorSet(VkDescLayouts.DESC2, smaaOutput);
            Engine.setPipeStateTextured2D(false);
    		VkTess tess = VkTess.instance;
            tess.setColor(-1, 255);

			if (!flip) {
				tess.add(windowWidth, 0, 0, 1, 0);
				tess.add(0, 0, 0, 0, 0);
				tess.add(0, windowHeight, 0, 0, 1);
				tess.add(windowWidth, windowHeight, 0, 1, 1);
			} else {
				tess.add(windowWidth, 0, 0, 1, 1);
				tess.add(0, 0, 0, 0, 1);
				tess.add(0, windowHeight, 0, 0, 0);
				tess.add(windowWidth, windowHeight, 0, 1, 0);
			}
            tess.drawQuads();
            tess.setOffset(0, 0, 0);
            Engine.endRenderPass();
        }
            vkContext.swapChain.blitFramebufferAndPreset(commandBuffer, outputinput?frameBuffer:frameBuffer2, 0);

        
    }
    
	public void shutdown() {
        if(DEBUG_LAYER) System.err.println("TexturedMesh_VK.shutdown");
        if (isVulkan)
            vkContext.syncAllFences();
        RenderAssets.destroy();
    	super.shutdown();
    }
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		if (this.gui != null) {

            this.gui.onWheelScroll(xoffset, yoffset);
		}
	}
}
