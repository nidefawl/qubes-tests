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

public class TestFullscreenTri extends GameBase {

	public TestFullscreenTri() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestFullscreenTri().startGame();
	}

	private Shader shaderTri;
	int a = 0;
	private AssetTexture t;
	private int image;
	int curTick;

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
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
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

        	if (this.shaderTri != null) {
        		this.shaderTri.release();
        	}
        	this.shaderTri = AssetManager.getInstance().loadShader(null, "debug/singletri");
        	this.shaderTri.enable();
        	this.shaderTri.setProgramUniform1i("texColor", 0);
        	Engine.checkGLError("UI!");
    	} catch (ShaderCompileError e) {
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
    		
    	}
	}

	@Override
	public void tick() {
		curTick++;
	}

	@Override
	public void initGame() {
        Engine.init();
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		FrameBuffer.unbindFramebuffer();
		glEnable(GL_DEPTH_TEST);
		Engine.setBlend(true);
	}

	@Override
	public void render(float f) {
        glClearColor(1,1,1,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, this.image);
//        Engine.pxStack.push(0, 32, 0);
        if ((curTick/40)%2==0) {
            Shaders.textured.enable();
            Engine.drawFullscreenQuad();
        } else {
        	this.shaderTri.enable();
            Engine.drawFSTri();
        }
//        Engine.pxStack.pop();
	}

	@Override
	public void lateInitGame() {
		this.t = AssetManager.getInstance().loadPNGAsset("textures/Unigine01.png");
		GLFW.glfwSetWindowSize(windowId, t.getWidth(), t.getHeight());
		
		this.image = TextureManager.getInstance().makeNewTexture(t, false, true, 0, GL_RGBA8);
	}
	
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
