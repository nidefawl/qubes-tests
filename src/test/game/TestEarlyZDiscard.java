package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
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

public class TestEarlyZDiscard extends GameBase {
	final CameraController cameraController = new CameraController();

	public TestEarlyZDiscard() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestEarlyZDiscard().startGame();
	}

	private int image;
	SMAA smaa;
	FrameBuffer fb;
	FrameBuffer fb2;
	int a = 0;

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
	public void render(float f) {
		glEnable(GL_DEPTH_TEST);
		this.fb.bind();
		this.fb.clearFrameBuffer();
        this.shaderTexture.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.image);
        Engine.drawFullscreenQuad();
        this.shaderTexture.enable();
		this.fb2.bind();
		this.fb2.clearFrameBuffer();
		this.fb.bindRead();
        GL30.glBlitFramebuffer(0, 0, this.fb.getWidth(), this.fb.getHeight(), 0, 0, this.fb2.getWidth(), this.fb2.getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        FrameBuffer.unbindReadFramebuffer();
        
        Engine.setDepthFunc(GL_EQUAL);
		
        this.shaderHeavy.enable();
        Engine.drawFullscreenQuad();
		FrameBuffer.unbindFramebuffer();

        Engine.setDepthFunc(GL_LEQUAL);
        glClearColor(1,1,1,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.textured.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fb2.getTexture(0));
        Engine.drawFullscreenQuad();
	}

	private Shader shaderHeavy;
	private Shader shaderTexture;
	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
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
        	if (smaa != null) {
        		smaa.releaseAll(EResourceType.FRAMEBUFFER);
        	}
        	smaa = new SMAA(SMAA.SMAA_PRESET_MEDIUM, displayWidth, displayHeight);
        	if (fb != null) {
        		fb.destroy();
        	}
            fb = new FrameBuffer(displayWidth, displayHeight);
            fb.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
            fb.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
            fb.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
            fb.setHasDepthAttachment();
            fb.setup(null);
        	if (fb2 != null) {
        		fb2.destroy();
        	}
            fb2 = new FrameBuffer(displayWidth, displayHeight);
            fb2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
            fb2.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
            fb2.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
            fb2.setHasDepthAttachment();
            fb2.setup(null);
        	loadShader();
        }
	}

	/**
	 * 
	 */
	private void loadShader() {
    	try {
        	if (this.shaderTexture != null) {
        		this.shaderTexture.destroy();
        	}

        	if (this.shaderHeavy != null) {
        		this.shaderHeavy.destroy();
        	}

        	this.shaderHeavy = AssetManager.getInstance().loadShader(null, "debug/slowshader");
        	this.shaderHeavy.enable();
        	this.shaderHeavy.setProgramUniform1i("tex0", 0);

        	this.shaderTexture = AssetManager.getInstance().loadShader(null, "textured", new IShaderDef() {
				
				@Override
				public String getDefinition(String define) {
					if ("ALPHA_TEST".equals(define))
						return "#define ALPHA_TEST";
					return null;
				}
			});
        	this.shaderTexture.enable();
        	this.shaderTexture.setProgramUniform1i("tex0", 0);
    	} catch (ShaderCompileError e) {
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
    		
    	}
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
		FrameBuffer.unbindFramebuffer();
		glEnable(GL_DEPTH_TEST);
		Engine.setBlend(true);
	}

	@Override
	public void lateInitGame() {
		AssetTexture t = AssetManager.getInstance().loadPNGAsset("textures/mask.png");
		GLFW.glfwSetWindowSize(windowId, t.getWidth(), t.getHeight());
		this.image = TextureManager.getInstance().makeNewTexture(t, false, true, 0);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
