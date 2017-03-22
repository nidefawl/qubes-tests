package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.glBindImageTexture;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

import java.nio.IntBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.*;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;

public class TestAtomicInc extends GameBase {

	public TestAtomicInc() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestAtomicInc().startGame();
	}

	private Shader shaderAtomicInc;
	int a = 0;
	private int glR32UITexture;
	private int glAtomicCounterBuffer;
	private IntBuffer uploadUintBuf;

	@Override
	public void onStatsUpdated() {
		String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")";
//		System.out.println();
		a++;
		if (a > 1) {
			setTitle(stats);
        	loadShader();
        	a = 0;
		}
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
	}
	
	@Override
	public void preRenderUpdate(float f) {
	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        	loadShader();
        }
	}

	/**
	 * 
	 */
	private void loadShader() {
    	try {

        	if (this.shaderAtomicInc != null) {
        		this.shaderAtomicInc.destroy();
        	}
        	this.shaderAtomicInc = AssetManager.getInstance().loadShader(null, "debug/atomicIncPixels");
        	this.shaderAtomicInc.enable();
        	this.shaderAtomicInc.setProgramUniform1i("ex_image", 0);
        	Engine.checkGLError("UI!");
    	} catch (ShaderCompileError e) {
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
    		
    	}
	}

	@Override
	public void tick() {
	}

	@Override
	public void initGame() {
        Engine.init(windowWidth, windowHeight);
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		FrameBuffer.unbindFramebuffer();
		Engine.setBlend(true);
	}

	@Override
	public void render(float f) {
        glClearColor(1,1,1,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        clearTex();
//        GL_TEXTURE_UPDATE_BARRIER_BIT, GL_BUFFER_UPDATE_BARRIER_BIT
        GL42.glMemoryBarrier(GL42.GL_TEXTURE_UPDATE_BARRIER_BIT|GL42.GL_BUFFER_UPDATE_BARRIER_BIT);
        glBindBufferRange(GL42.GL_ATOMIC_COUNTER_BUFFER, 0, this.glAtomicCounterBuffer, 0, 4);
        glBindImageTexture(0, this.glR32UITexture, 0, false, 0, GL15.GL_READ_WRITE, GL30.GL_R32UI);
        shaderAtomicInc.enable();

        int tw = Engine.displayWidth;
        int th = Engine.displayHeight;
        float x = 0;
        float y = 0;
        Tess tess = Tess.instance;
        tess.setColor(0xFFFFFF, 0xff);
//        tess.add(x + tw,   y,      0, 1, 1);
//        tess.add(x,        y,      0, 0, 1);
//        tess.add(x,        y + th, 0, 0, 0);
//        tess.add(x,        y + th, 0, 0, 0);
//        tess.add(x + tw,   y + th, 0, 1, 0);
//        tess.add(x + tw,   y,      0, 1, 1);
        tess.add(x + tw+tw,   y,      0, 1, 1);
        tess.add(x,        y,      0, 0, 1);
        tess.add(x,        y + th + th, 0, 0, 0);
        tess.draw(GL_TRIANGLES); // == Engine.drawFullscreenQuad
	}

	@Override
	public void lateInitGame() {
		this.uploadUintBuf = Memory.createIntBuffer(Engine.displayHeight*Engine.displayWidth);
		this.glR32UITexture = GL.genStorage(Engine.displayWidth, Engine.displayHeight, GL30.GL_R32UI, GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
    	Engine.checkGLError("genStorage");
		this.glAtomicCounterBuffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL42.GL_ATOMIC_COUNTER_BUFFER, this.glAtomicCounterBuffer);
    	Engine.checkGLError("glBindBuffer");
		GL15.glBufferData(GL42.GL_ATOMIC_COUNTER_BUFFER, 4, GL15.GL_DYNAMIC_DRAW);
    	Engine.checkGLError("glBufferData");
		GL15.glBindBuffer(GL42.GL_ATOMIC_COUNTER_BUFFER, 0);
    	clearTex();
		glDisable(GL_DEPTH_TEST);
		Engine.setBlend(false);
		Engine.enableDepthMask(false);
		
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, 0);
	}
	private void clearTex() {
//		this.uploadUintBuf.clear();
//		for (int i = 0; i < displayHeight * displayWidth; i++) {
//			this.uploadUintBuf.put(16);
//		}
//		this.uploadUintBuf.flip();
//
//		GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, this.glR32UITexture);
//		Engine.checkGLError("bindTexture");
//		GL11.glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, displayWidth, displayHeight, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_INT, this.uploadUintBuf);
//		Engine.checkGLError("glTexImage2D");
//		GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, 0);
		
		int clearValue = 22222;
		this.uploadUintBuf.put(0, clearValue);
		this.uploadUintBuf.position(0).limit(1);
    	GL44.glClearTexImage(this.glR32UITexture, 0, GL30.GL_RGBA_INTEGER, GL11.GL_UNSIGNED_INT, this.uploadUintBuf);
    	Engine.checkGLError("glClearTexImage");
		this.uploadUintBuf.put(0, clearValue);
		this.uploadUintBuf.position(0).limit(1);
		GL15.glBindBuffer(GL42.GL_ATOMIC_COUNTER_BUFFER, this.glAtomicCounterBuffer);
		GL15.glBufferData(GL42.GL_ATOMIC_COUNTER_BUFFER, this.uploadUintBuf, GL15.GL_DYNAMIC_DRAW);
		GL15.glBindBuffer(GL42.GL_ATOMIC_COUNTER_BUFFER, 0);
    	Engine.checkGLError("glBindBuffer");
        shaderAtomicInc.enable();
        shaderAtomicInc.setProgramUniform1ui("clear_value", clearValue);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
