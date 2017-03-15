package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.font.*;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.shader.DebugShaders.Var;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vr.VR;
import test.game.ParticlePerformanceTest2.Particle;

public class TestShaderToy extends GameBase implements ITextEdit {
	final CameraController cameraController = new CameraController();
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	private static boolean startup;
	private String error;
    final static Vector3f tmp = new Vector3f();

	public TestShaderToy() {
		TICKS_PER_SEC = 20;
	}
	
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestShaderToy().startGame();
	}
	private int image;
	FrameBuffer fb2;
	int a = 0;
	private boolean down;
	/**
	 * 
	 */
	boolean first = true;
	private Shader shaderHeavy;
	private FontRenderer font;
	float lastMx, lastMy;
	private String stats = "";
	int reloadTick;
	@Override
	public void onStatsUpdated() {
		this.stats = String.format("%d FPS (%.2fms)", lastFPS, Stats.avgFrameTime);


        String s = String.format("%s - Display %dx%d - Window %dx%d - Gui %dx%d", 
        		this.stats, 
        		Engine.displayWidth, Engine.displayHeight, 
        		windowWidth, windowHeight, 
        		Engine.getGuiWidth(), Engine.getGuiHeight());

		if (VR.getFB(0) != null) {

             s = String.format("%s - Display %dx%d - Window %dx%d - VRFB %dx%d - Gui %dx%d", 
            		this.stats, 
            		Engine.displayWidth, Engine.displayHeight, 
            		windowWidth, windowHeight, 
            		VR.getFB(0).getWidth(), VR.getFB(0).getHeight(), 
            		Engine.getGuiWidth(), Engine.getGuiHeight());
		}
        setTitle(s);

        reloadTick--;
		if (reloadTick <= 0) {
			loadShader();
//			Shaders.initShaders();
			reloadTick = 8;
		}
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}
	
	@Override
	public void onMouseClick(long window, int button, int action, int mods) {
		super.onMouseClick(window, button, action, mods);
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
			this.down = true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_RELEASE) {
			this.down = false;
		}
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		if (action == GLFW.GLFW_PRESS) {
			switch (key) {
			case GLFW.GLFW_KEY_F1:
				toggleVR();
				break;
			}
		}
	}

	@Override
	public void render(float f) {
		glClearColor(1, 1, 1, 0);
		glClear(GL_DEPTH_BUFFER_BIT);

		Engine.setDefaultViewport();
		for (int eye = 0; eye < (VR_SUPPORT ? 2 : 1); eye++) {
			if (VR_SUPPORT) {
				Engine.getMatSceneP().load(eye == 0 ? VR.cam.projLeft : VR.cam.projRight);
				Engine.getMatSceneP().update();

                Engine.setViewMatrix(VR.getViewMat(eye));

				VR.setViewPort(eye);
				Engine.checkGLError("setCameraAndViewport");
				FrameBuffer finalTarget = VR.getFB(eye);
				finalTarget.bind();
				finalTarget.clearFrameBuffer();
			} else {
				this.fb2.bind();
				this.fb2.clearFrameBuffer();
			}
			this.shaderHeavy.enable();
			GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.image);
			Engine.drawFullscreenQuad();
			if (VR_SUPPORT) {
				glEnable(GL11.GL_DEPTH_TEST);
				glDisable(GL11.GL_CULL_FACE);
	            Engine.setViewMatrixCameraPos(VR.getPoseMat(eye), Vector3f.ZERO);
				VR.renderControllers();
				glEnable(GL11.GL_CULL_FACE);
				glDisable(GL11.GL_DEPTH_TEST);
			} else {
				FrameBuffer.unbindFramebuffer();
				Shaders.textured.enable();
				GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fb2.getTexture(0));
				Engine.drawFullscreenQuad();
			}
		}
		FrameBuffer.unbindFramebuffer();

		if (VR_SUPPORT) {
			VR.Submit();
			if (VR_SUPPORT) {
				VR.updatePose(f);
			}
			Engine.checkGLError("VR.Submit");
            setWindowViewport();
			if (Game.GL_ERROR_CHECKS)
				Engine.checkGLError("setGUIProjection");
			VR.drawFullscreenCompanion(windowWidth, windowHeight);
			Engine.checkGLError("drawFullscreenCompanion");
		}

		List<Var> debugVars = this.shaderHeavy.readDebugVars();
		Engine.setBlend(true);
		int hT = debugVars.size() * 18 + 50;
		int yT = Engine.displayHeight - hT;
		Shaders.colored.enable();
		Tess.instance.setColorF(0, 0.7f);
		Tess.instance.add(600, yT);
		Tess.instance.add(0, yT);
		Tess.instance.add(0, yT + hT);
		Tess.instance.add(600, yT + hT);
		Tess.instance.drawQuads();
		Shaders.textured.enable();
		int y = yT + 20;
		this.font.drawString(this.stats, 10, y, -1, true, 1.0f);
		y += 26;
		for (int i = 0; i < debugVars.size(); i++) {
			this.font.drawString("" + debugVars.get(i), 10, y, -1, true, 1.0f);
			y += 18;
		}
		y += 30;
		if (this.error != null) {
			this.font.drawString(this.error, Engine.displayWidth / 2, 30, 0xff8989, true, 1.0f, 2);
		}
		Engine.setBlend(false);
	}


	private void updateMousePos() {
		boolean inside = !(Mouse.getX()<0||Mouse.getX()>windowWidth||Mouse.getY()<0||Mouse.getY()>windowHeight);
		if (!inside) {
			down = false;
		}
		if (!movement.grabbed()&&down) {
			lastMx = (float) Mouse.getX();
			lastMy = (float) Mouse.getY();
		}
		this.shaderHeavy.enable();
		this.shaderHeavy.setProgramUniform4f("iMouse", GameMath.clamp((float)lastMx, 0f, windowWidth), GameMath.clamp(windowHeight-1-(float)lastMy, 0f, windowHeight), down ? 1 : 0, 0);

	}
	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
		
        Engine.updateCamera();
        Engine.getSunLightModel().setTime(5850);
//        Engine.getSunLightModel().setTime(1700+(int)((ticksran+f)*32));
        Engine.getSunLightModel().updateFrame(f);
        Engine.setLightPosition(Engine.getSunLightModel().getLightPosition());
        UniformBuffer.updateUBO(null, f);
//		this.cameraController.update(movement);
//		Vec3D.sub(this.cameraController.pos, this.cameraController.lastPos, this.tmpPos);
//		this.tmpPos.scale(f);
//		Vec3D.add(this.tmpPos, this.cameraController.lastPos, this.tmpPos);
//        Engine.camera.setPosition(this.tmpPos);
//        Engine.camera.setOrientation(this.cameraController.yaw, this.cameraController.pitch, false, 4.0f);   
//        Engine.updateCamera(Engine.camera.getViewMatrix(), Vector3f.ZERO, false);
//        UniformBuffer.updateUBO(null, f);
        updateMousePos();

	}

	@Override
	public void postRenderUpdate(float f) {
	}
	@Override
	public void onWindowResize(int displayWidth, int displayHeight) {
	    if (!VR_SUPPORT||isStarting) {
	        setRenderResolution(displayWidth, displayHeight);
	    }
	}
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        Engine.updateRenderResolution(displayWidth, displayHeight);
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        	if (fb2 != null) {
        		fb2.release();
        	}
            fb2 = new FrameBuffer(displayWidth, displayHeight);
            fb2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
            fb2.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
            fb2.setClearColor(GL_COLOR_ATTACHMENT0, 0, 0, 0, 0);
            fb2.setHasDepthAttachment();
            fb2.setup(null);
    		FrameBuffer.unbindFramebuffer();
        	loadShader();
        }
	}
	
	private void loadShader() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader shader = assetMgr.loadShader(newshaders, "debug/shadertoy", "debug/shadertoy", null, null, null);
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            this.shaderHeavy = shader;
        	this.shaderHeavy.enable();
        	this.shaderHeavy.setProgramUniform1i("iChannel0", 0);
        	this.shaderHeavy.setProgramUniform1i("iChannel1", 1);
        	updateMousePos();
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


	@Override
	public void tick() {
		if (!isStarting) {
			this.cameraController.tickUpdate();
			if (VR_SUPPORT) {
				VR.tick();
			}
		}
	}

	@Override
	public void initGame() {
        Engine.init(windowWidth, windowHeight);
		TextureManager.getInstance().init();
		FontRenderer.init();
		this.font = FontRenderer.get(0, 12, 0);
		setVSync(true);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
	}

	@Override
	public void lateInitGame() {
		AssetTexture t = AssetManager.getInstance().loadPNGAsset("textures/tex16.png");
		this.image = TextureManager.getInstance().makeNewTexture(t, true, true, 0);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

	@Override
	public void submit(TextInput textInput) {
	}

	@Override
	public void onEscape(TextInput textInput) {
	}

}
