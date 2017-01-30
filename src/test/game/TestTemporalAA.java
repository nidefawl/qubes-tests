package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.shader.DebugShaders.Var;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vec3D;

public class TestTemporalAA extends GameBase {
	final CameraController cameraController = new CameraController();
	private FrameBuffer buf;
	private FrameBuffer buf2;
	private FrameBuffer sceneFB;
	private TesselatorState tessState;
	SMAA smaa;
	public TestTemporalAA() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
		TICKS_PER_SEC = 20;
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestTemporalAA().startGame();
	}
	

	@Override
	public void onStatsUpdated() {
		setTitle(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+") Reprojection: "+temporal);
		initShaders();
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	boolean temporal = true;
	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		if (action==GLFW.GLFW_PRESS)
		switch (key) {
		case GLFW.GLFW_KEY_F1:
			temporal=!temporal;
			
			initAA();
			break;

		default:
			break;
		}
	}

	BufferedMatrix prevView, prevProj;
	
	@Override
	public void render(float f) {
		Matrix4f prevProjJittered = Matrix4f.pool();
		Engine.addJitterToProjection(prevProj, prevProjJittered);
		Matrix4f.mul(prevProjJittered, prevView, prevProj);
		prevProj.update();
		Engine.getSceneFB().bind();
		Engine.getSceneFB().setDrawAll();
		Engine.getSceneFB().clearFrameBuffer();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, TMgr.getEmptyWhite());
        shaderDraw3dVelocity.enable();
        shaderDraw3dVelocity.setProgramUniformMatrix4("mvp_prev", false, prevProj.get(), false);
		prevProj.load(Engine.getMatSceneP_internal());
		prevView.load(Engine.getMatSceneMV());
		tessState.drawQuads();
		FrameBuffer.unbindFramebuffer();

		HBAOPlus.renderAO();
		buf2.bind();
		buf2.clearFrameBuffer();

//		Matrix4f t = Matrix4f.pool();
//		System.out.println(Matrix4f.sub(Engine.getMatSceneMVP(), prevProj, t));
		shaderCombineao.enable();
//		shaderCombineao.setProgramUniformMatrix4("p_cur_inv", false, Engine.getMatSceneP().getInv(), false);
//		shaderCombineao.setProgramUniformMatrix4("v_cur_inv", false, Engine.getMatSceneV().getInv(), false);
//		shaderCombineao.setProgramUniformMatrix4("vp_cur_inv", false, Engine.getMatSceneMVP().getInv(), false);
//		shaderCombineao.setProgramUniformMatrix4("vp_prev", false, prevProj.get(), false);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        GL.bindTexture(GL_TEXTURE0+1, GL_TEXTURE_2D, buf.getTexture(0));
//        GL.bindTexture(GL_TEXTURE0+2, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
		Engine.drawFSTri();
//		List<Var> debugVars = this.shaderCombineao.readDebugVars();
		
		Engine.getSceneFB().bind();
		Engine.getSceneFB().setDrawMask(1);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.tonemap.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, buf2.getTexture(0));
        Engine.drawFullscreenQuad();
		FrameBuffer.unbindFramebuffer();
		
		
        glClearColor(0.11F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

//
		smaa.render(Engine.getSceneFB().getTexture(0), 0, Engine.getSceneFB().getTexture(1), 0, null);
//		smaa.render(Engine.getSceneFB().getTexture(0), 0, TMgr.getEmpty(), 0, null);
//		buffer.put(Engine.getMatSceneVP().get()); 
//        glClearColor(0.11F, 0.82F, 1.00F, 1F);
//        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
//      Shaders.textured.enable();
//      GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
//      Engine.drawFullscreenQuad();
//		

        glClear(GL11.GL_DEPTH_BUFFER_BIT);
		Engine.setBlend(true);
		int hT = 0 * 18 + 50;
//		int hT = debugVars.size() * 18 + 50;
		int yT = displayHeight - hT;
		Shaders.colored.enable();
		Tess.instance.setColorF(0, 0.7f);
		Tess.instance.add(600, yT);
		Tess.instance.add(0, yT);
		Tess.instance.add(0, yT + hT);
		Tess.instance.add(600, yT + hT);
		Tess.instance.drawQuads();
		Shaders.textured.enable();
		int y = yT + 20;
//		this.font.drawString(this.stats, 10, y, -1, true, 1.0f);
		y += 26;
//		for (int i = 0; i < debugVars.size(); i++) {
//			this.font.drawString("" + debugVars.get(i), 10, y, -1, true, 1.0f);
//			y += 18;
//		}
		y += 30;
		if (this.error != null) {
			this.font.drawString(this.error, Game.displayWidth / 2, 30, 0xff8989, true, 1.0f, 2);
		}
		Engine.setBlend(false);
	}

	@Override
	public void preRenderUpdate(float f) {
//		float fsleep = (1000/10f)-(Stats.avgFrameTime);
//		if (fsleep > 1f&&fsleep<100) {
//			try {
//				Thread.sleep((int)fsleep);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f); 
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);

	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	boolean hadContext = false;
	private FontRenderer font;
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
        	initAA();
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
			if (buf != null) buf.release();
			if (sceneFB != null) sceneFB.release();
			buf = new FrameBuffer(displayWidth, displayHeight);
			buf.setColorAtt(GL_COLOR_ATTACHMENT0, GL11.GL_RGBA8);
			buf.setClearColor(GL_COLOR_ATTACHMENT0, 1, 1, 1, 1);
			buf.setFilter(GL_COLOR_ATTACHMENT0, GL11.GL_NEAREST, GL11.GL_NEAREST);
			buf.setup(null);
			buf2 = new FrameBuffer(displayWidth, displayHeight);
			buf2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
			buf2.setClearColor(GL_COLOR_ATTACHMENT0, 1, 1, 1, 1);
			buf2.setFilter(GL_COLOR_ATTACHMENT0, GL11.GL_NEAREST, GL11.GL_NEAREST);
			buf2.setup(null);
	        sceneFB = new FrameBuffer(displayWidth, displayHeight);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGBA16F);
	        sceneFB.setFilter(GL_COLOR_ATTACHMENT0, GL_NEAREST, GL_NEAREST);
	        sceneFB.setFilter(GL_COLOR_ATTACHMENT1, GL_NEAREST, GL_NEAREST);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 1.0F, 1.0F, 1.0F, 1.0F);
	        sceneFB.setHasDepthAttachment();
	        sceneFB.setup(null);
	        Engine.setSceneFB(sceneFB);
	        HBAOPlus.setDepthTex(Engine.getSceneFB().getDepthTex());
	        HBAOPlus.setNormalTex(Engine.getSceneFB().getTexture(1));
	        long ptr = MemoryUtil.memAddress(Engine.getMatSceneP().get());
	        HBAOPlus.setProjMatrix(ptr);
	        HBAOPlus.setOutputFBO(buf.getFB());
	        HBAOPlus.setRadius(1.8f);
	        HBAOPlus.setBias(0.15f);
	        HBAOPlus.setCoarseAO(1.3f);
	        HBAOPlus.setBlur(true, 8, 16.0f);
	        HBAOPlus.setBlurSharpen(false, 16, 0, 0);
	        HBAOPlus.setDetailAO(0.9f);
	        HBAOPlus.setPowerExponent(1.5f);
	        HBAOPlus.setDepthThreshold(false, 220, 0.5f);
	        HBAOPlus.setNormalDecodeScaleBias(2.0f,-1f);
	        HBAOPlus.setRenderMask(1|2);
	        HBAOPlus.setBlur(true, 8, 16);
	        buf.bind();
	        buf.clearFrameBuffer();
			FrameBuffer.unbindFramebuffer();
        }
        glActiveTexture(GL_TEXTURE0);
	}

	private void initAA() {

    	if (smaa != null) {
    		smaa.releaseAll(EResourceType.FRAMEBUFFER);
    	}
    	smaa = new SMAA(SMAA.SMAA_PRESET_MEDIUM, false, false, temporal);
    	smaa.init(displayWidth, displayHeight);
	}
	@Override
	public void tick() {
		this.cameraController.tickUpdate();
	}

	@Override
	public void initGame() {
        Engine.init();
		TextureManager.getInstance().init();
		setVSync(true);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
        Engine.setBlend(false);
	}

	@Override
	public void lateInitGame() {
		tessState = new TesselatorState(GL15.GL_STATIC_DRAW);
		int w = 1;
		int d = 2;
		Tess.instance.setColor(0x880088, 0xff);
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
		Tess.instance.setColor(0x880000, 0xff);
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
		Tess.instance.setColor(0x008888, 0xff);
		Tess.instance.add(-4*w, 0, -d*4);
		Tess.instance.add(-4*w, 0, d*4);
		Tess.instance.add(4*w, 0, d*4);
		Tess.instance.add(4*w, 0, -d*4);
		Tess.instance.draw(GL_QUADS, this.tessState);
		initShaders();
		glEnable(GL_DEPTH_TEST);
        prevProj = new BufferedMatrix();
        prevView = new BufferedMatrix();
		this.font = FontRenderer.get(0, 12, 0);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}


    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	private static boolean startup;
	private String error;
	private Shader shaderCombineao;
	private Shader shaderDraw3dVelocity;

    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_combineao = assetMgr.loadShader(newshaders, "debug/combineao");
            Shader new_shaderDraw3dVelocity = assetMgr.loadShader(newshaders, "debug/textured_3D_vel");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            shaderDraw3dVelocity = new_shaderDraw3dVelocity;
            shaderCombineao = new_combineao;
            shaderCombineao.enable();
            shaderCombineao.setProgramUniform1i("texColor", 0);
            shaderCombineao.setProgramUniform1i("texAO", 1);
            shaderCombineao.setProgramUniform1i("texDepth", 2);
            shaderDraw3dVelocity.enable();
            shaderDraw3dVelocity.setProgramUniform1i("tex0", 0);
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

}
