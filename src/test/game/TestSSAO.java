package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;

public class TestSSAO extends GameBase {
	final CameraController cameraController = new CameraController();
	private FrameBuffer buf;
	private FrameBuffer sceneFB;
	private TesselatorState tessState;
	public TestSSAO() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
		TICKS_PER_SEC = 20;
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestSSAO().startGame();
	}
	

	@Override
	public void onStatsUpdated() {
		System.out.println(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
	}

	@Override
	public void render(float f) {
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
		Shaders.colored3D.enable();
		tessState.drawQuads();
		FrameBuffer.unbindFramebuffer();
		HBAOPlus.renderAO();
        glClearColor(0.11F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.textured.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, buf.getTexture(0));
        Engine.drawFullscreenQuad();
	}

	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f); 
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);

	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	boolean hadContext = false;
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
        	if (hadContext) {
                Engine.checkGLError("pre GLNativeLib.deleteContext");
        		HBAOPlus.deleteContext();
                Engine.checkGLError("post GLNativeLib.deleteContext");
        	}
            Engine.checkGLError("pre GLNativeLib.createContext");
            System.out.println(GameBase.baseInstance.caps);
            HBAOPlus.createContext(128, 128, GameBase.baseInstance.caps);
            HBAOPlus.hasContext = true;
            Engine.checkGLError("post GLNativeLib.createContext");
            Engine.resize(displayWidth, displayHeight);
			if (buf != null) buf.destroy();
			if (sceneFB != null) sceneFB.destroy();
			buf = new FrameBuffer(displayWidth, displayHeight);
			buf.setColorAtt(GL_COLOR_ATTACHMENT0, GL11.GL_RGBA8);
//			buf.setColorTexExtFmt(GL11.GL_RGBA);
//			buf.setColorTexExtType(GL11.GL_UNSIGNED_BYTE);
			buf.setClearColor(GL_COLOR_ATTACHMENT0, 1, 1, 1, 1);
			buf.setFilter(GL_COLOR_ATTACHMENT0, GL11.GL_NEAREST, GL11.GL_NEAREST);
			buf.setup(null);
	        sceneFB = new FrameBuffer(displayWidth, displayHeight);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGBA16UI);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB16F);
	        sceneFB.setFilter(GL_COLOR_ATTACHMENT2, GL_NEAREST, GL_NEAREST);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
	        sceneFB.setHasDepthAttachment();
	        sceneFB.setup(null);
	        Engine.setSceneFB(sceneFB);
	        HBAOPlus.setDepthTex(Engine.getSceneFB().getDepthTex());
	        HBAOPlus.setNormalTex(Engine.getSceneFB().getTexture(1));
	        long ptr = MemoryUtil.memAddress(Engine.getMatSceneP().get());
	        HBAOPlus.setProjMatrix(ptr);
	        HBAOPlus.setOutputFBO(buf.getFB());
	        HBAOPlus.setRadius(1);
	        HBAOPlus.setBias(0.2f);
	        HBAOPlus.setCoarseAO(1.2f);
	        HBAOPlus.setBlur(true, 8, 16.0f);
	        HBAOPlus.setBlurSharpen(false, 16, 0, 0);
	        HBAOPlus.setDetailAO(1f);
	        HBAOPlus.setPowerExponent(1);
	        HBAOPlus.setDepthThreshold(false, 220, 0.5f);
	        buf.bind();
	        buf.clearFrameBuffer();
			FrameBuffer.unbindFramebuffer();
        }
        glActiveTexture(GL_TEXTURE0);
	}

	@Override
	public void tick() {
		this.cameraController.tickUpdate();
	}

	@Override
	public void initGame() {
        Engine.init(windowWidth, windowHeight);
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
        Engine.setBlend(false);
	}

	@Override
	public void lateInitGame() {
		tessState = new TesselatorState(GL15.GL_STATIC_DRAW);
		int w = 1;
		int d = 2;
		Tess.instance.setColor(0xff00ff, 0xff);
		Tess.instance.setOffset(0, 0, 0);
		Tess.instance.add(0, 0, d);
		Tess.instance.add(0, w, d);
		Tess.instance.add(w, w, d);
		Tess.instance.add(w, 0, d);
		Tess.instance.add(0, w, d);
		Tess.instance.add(0, 0, d);
		Tess.instance.add(0, 0, d*2);
		Tess.instance.add(0, w, d*2);
		Tess.instance.add(w, w, d);
		Tess.instance.add(0, w, d);
		Tess.instance.add(0, w, d*2);
		Tess.instance.add(w, w, d*2);
		Tess.instance.setColor(0xff0000, 0xff);
		Tess.instance.add(0, w, -d);
		Tess.instance.add(0, 0, -d);
		Tess.instance.add(w, 0, -d);
		Tess.instance.add(w, w, -d);
		Tess.instance.add(0, 0, -d);
		Tess.instance.add(0, w, -d);
		Tess.instance.add(0, w, -d*2);
		Tess.instance.add(0, 0, -d*2);
		Tess.instance.add(0, w, -d);
		Tess.instance.add(w, w, -d);
		Tess.instance.add(w, w, -d*2);
		Tess.instance.add(0, w, -d*2);
		Tess.instance.setColor(0x00ffff, 0xff);
		Tess.instance.add(-4*w, 0, -d*4);
		Tess.instance.add(-4*w, 0, d*4);
		Tess.instance.add(4*w, 0, d*4);
		Tess.instance.add(4*w, 0, -d*4);
		Tess.instance.draw(GL_QUADS, this.tessState);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}



}
