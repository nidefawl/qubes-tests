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
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.FinalRenderer;
import nidefawl.qubes.render.SkyRenderer;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vector3f;

public class FinalRendererTest extends GameBase {
	final CameraController cameraController = new CameraController();
	public FinalRendererTest() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new FinalRendererTest().startGame();
	}
	

	int tick = 0;
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	private static boolean startup;


	private String error;

    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
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
			setTitle(String.format("%d fps, %.2fms", lastFPS, Stats.avgFrameTime));
		
		
		
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
				Engine.skyRenderer.redraw();
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
	@Override
	public void render(float f) {
		Engine.enableDepthMask(false);
		
		glDisable(GL11.GL_DEPTH_TEST);
		Engine.setBlend(false);
		Engine.skyRenderer.renderSky(Engine.getSunLightModel().getDayTime(), f);
		setSceneViewport();
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
		Engine.skyRenderer.renderSkybox();
		//enable depth test + mask then draw something solid
        
        
		Engine.checkGLError("Pass0");
		Engine.outRenderer.renderDeferred(f, 0);
		
		
        if (Engine.outRenderer.getSsr() > 0) {
            Engine.outRenderer.raytraceSSR();
        }

        if (Engine.outRenderer.getSsr() > 0) {
            Engine.outRenderer.combineSSR();
        }
        Engine.setBlend(false);
        glDisable(GL_DEPTH_TEST);
        Engine.enableDepthMask(false);
        Engine.outRenderer.renderBlur();


        
        Engine.outRenderer.renderBloom();
        glEnable(GL_DEPTH_TEST);
        Engine.enableDepthMask(true);
        
        
        
		


        FrameBuffer fbOut = Engine.outRenderer.renderTonemap();
		FrameBuffer.unbindFramebuffer();
		glClearColor(0, 0, 0, 0);
		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Engine.outRenderer.renderAA(fbOut.getTexture(0), null);
        

//		Shaders.tonemap.enable();
//		Shaders.tonemap.setProgramUniform1f("constexposure", 30);
//		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.outRenderer.fbDeferred.getTexture(0));
//		Engine.drawFullscreenQuad();
//		Shaders.textured.enable();

	}



	private float curWeather;
	private float lastWeather;
	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
		
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

	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        }
        glActiveTexture(GL_TEXTURE0);
	}

	@Override
	public void tick() {
		this.cameraController.tickUpdate();
		this.lastWeather = WEATHER;
		Engine.skyRenderer.tickUpdate();
	}

	@Override
	public void initGame() {
        Engine.init(EngineInitSettings.INIT_SKY_FINAL);
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
	}


	private FontRenderer font;
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
