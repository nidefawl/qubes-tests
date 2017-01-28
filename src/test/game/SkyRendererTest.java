package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.render.SkyRenderer;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vector3f;

public class SkyRendererTest extends GameBase {
	
	final CameraController cameraController = new CameraController();
	private FrameBuffer sceneFB;
	public FrameBuffer fbDeferred;
	
	public SkyRendererTest() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new SkyRendererTest().startGame();
	}
	

	int tick = 0;
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	private static boolean startup;

	Shader shaderDeferred;

	private Shader shaderSampleCubemap;
	private String error;

    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_sample_cubemap = assetMgr.loadShader(newshaders, "sky/skybox_sample_cubemap");
//            Shader cloudsShader = assetMgr.loadShader(newshaders, "sky/sky");
            Shader new_deferred = assetMgr.loadShader(newshaders, "post/deferred", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("RENDER_PASS".equals(define)) {
                        return "#define RENDER_PASS 0";
                    }
                    return null;
                }
            });
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            shaderDeferred = new_deferred;
            this.shaderSampleCubemap = new_sample_cubemap;
            this.shaderDeferred.enable();
            shaderDeferred.setProgramUniform1i("texColor", 0);
            shaderDeferred.setProgramUniform1i("texNormals", 1);
            shaderDeferred.setProgramUniform1i("texMaterial", 2);
            shaderDeferred.setProgramUniform1i("texDepth", 3);
            shaderDeferred.setProgramUniform1i("texShadow", 4);
            shaderDeferred.setProgramUniform1i("texLight", 5);
            shaderDeferred.setProgramUniform1i("texBlockLight", 6);
            shaderDeferred.setProgramUniform1i("texAO", 7);

            Shader.disable();
            this.error = null;
        } catch (ShaderCompileError e) {
            newshaders.release();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            this.error="shader " + e.getName() + " failed to compile\n"+e.getLog();
            if (startup) {
                throw e;
            } else {
            }
        }
        startup = false;
    }
    int action = 0;
	private String stats;
	@Override
	public void onStatsUpdated() {
			setTitle(""+lastFPS);
		
		
		
		tick--;
		if (tick <= 0) {
			tick = 4;
			try {
				once = false;
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("FAIL");
			}
		}
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		if (action == GLFW.GLFW_PRESS) {
			switch (key) {
			case GLFW.GLFW_KEY_1:
				initShaders();
				break;
			case GLFW.GLFW_KEY_2:
				skyrenderer.redraw();
				break;
			case GLFW.GLFW_KEY_3:
				Engine.isDither = !Engine.isDither;
				System.out.println(Engine.isDither);
				break;
			}
		}
		if (action == GLFW.GLFW_REPEAT||action == GLFW.GLFW_PRESS) {
			switch (key) {
			case GLFW.GLFW_KEY_KP_ADD:
				WEATHER += 0.01f;
				if (WEATHER > 1)
					WEATHER = 1;
				break;
			case GLFW.GLFW_KEY_KP_SUBTRACT:
				WEATHER -= 0.01f;
				if (WEATHER < 0)
					WEATHER = 0;
				break;
			case GLFW.GLFW_KEY_PAGE_UP:
				TIME+=50;
				break;
			case GLFW.GLFW_KEY_PAGE_DOWN:
				TIME-=50;
				break;
			}
		}
	}

    boolean once = false;
    final static Vector3f tmp = new Vector3f();
	@Override
	public void render(float f) {
		Engine.enableDepthMask(false);
		glDisable(GL11.GL_DEPTH_TEST);
		Engine.setBlend(false);
		this.skyrenderer.renderSky(Engine.getSunLightModel().getDayTime(), f);
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
        this.shaderSampleCubemap.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_CUBE_MAP, skyrenderer.fbSkybox.getTexture(0));
        Engine.drawFSTri();
        Engine.enableDepthMask(true);
		glDisable(GL11.GL_DEPTH_TEST);
		Engine.checkGLError("Pass0");
		fbDeferred.bind();
		fbDeferred.clearFrameBuffer();
		shaderDeferred.enable();
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
		GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
		GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
		GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
		GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getEmptyWhite()); // SHADOW
		GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, TMgr.getEmpty()); // LIGHTCOMPUTE
		GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3));
		GL.bindTexture(GL_TEXTURE7, GL_TEXTURE_2D, TMgr.getEmptyWhite()); // SSAO
		Engine.drawFSTri();
		FrameBuffer.unbindFramebuffer();
		glClearColor(0, 0, 0, 0);
		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		Shaders.tonemap.enable();
		Shaders.tonemap.setProgramUniform1f("constexposure", 30);
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbDeferred.getTexture(0));
		Engine.drawFullscreenQuad();
		Shaders.textured.enable();

	}



	private float curWeather;
	private float lastWeather;
	@Override
	public void preRenderUpdate(float f) {
        if (VR_SUPPORT) {
            this.cameraController.updateVR();
        } else {
            this.cameraController.update(movement);
        }
        Vector3f renderPos = this.cameraController.getRenderPos(f);
        Engine.camera.setPosition(renderPos);
        if (!VR_SUPPORT) {
            Engine.camera.setOrientation(this.cameraController.yaw, this.cameraController.pitch, false, 4.0f);   
        }
		
        Engine.updateCamera();
        Engine.getSunLightModel().setTime(TIME);
//        Engine.getSunLightModel().setTime(1700+(int)((ticksran+f)*32));
        Engine.getSunLightModel().updateFrame(f);
        Engine.setLightPosition(Engine.getSunLightModel().getLightPosition());
        UniformBuffer.updateUBO(null, f);
        this.curWeather = lastWeather + (WEATHER-lastWeather)*f;
	}

	@Override
	public void postRenderUpdate(float f) {
	}

	boolean hadContext = false;
	
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
            this.skyrenderer.resizeRenderer(displayWidth, displayHeight);
			if (sceneFB != null) sceneFB.release();
			if (fbDeferred != null) fbDeferred.release();
	        sceneFB = new FrameBuffer(displayWidth, displayHeight);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGBA16UI);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB16F);
	        sceneFB.setFilter(GL_COLOR_ATTACHMENT2, GL_NEAREST, GL_NEAREST);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
	        sceneFB.setHasDepthAttachment();
	        sceneFB.setup(null);
	        Engine.setSceneFB(sceneFB);
			FrameBuffer.unbindFramebuffer();
	        fbDeferred = FrameBuffer.make(null, displayWidth, displayHeight, GL_RGB16F);
        }
        glActiveTexture(GL_TEXTURE0);
	}

	@Override
	public void tick() {
		this.cameraController.tickUpdate();
		this.lastWeather = WEATHER;
		this.skyrenderer.tickUpdate();
	}

	@Override
	public void initGame() {
		this.skyrenderer = new SkyRenderer();
		this.skyrenderer.preinit();
        Engine.init();
		this.skyrenderer.init();
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
	}


	private FontRenderer font;
	private SkyRenderer skyrenderer;
	public static float WEATHER = 0.40f;
	public static int TIME = 5850;
	@Override
	public void lateInitGame() {
		this.font=FontRenderer.get(0, 22, 0);
		initShaders();

	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
