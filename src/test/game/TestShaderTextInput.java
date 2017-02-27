package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.font.*;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.render.gui.BoxGUI;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;

public class TestShaderTextInput extends GameBase implements ITextEdit {
	final CameraController cameraController = new CameraController();
	
	public TestShaderTextInput() {
		TICKS_PER_SEC = 20;
	}
	
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestShaderTextInput().startGame();
	}
	private int image;
	FrameBuffer fb2;
	int a = 0;
	private boolean down;
	/**
	 * 
	 */
	boolean first = true;
	private Vec3D tmpPos = new Vec3D();
	private Shader shaderHeavy;
	private FontRenderer font;
	private TextInput text;
	float lastMx, lastMy;
	
	@Override
	public void onStatsUpdated() {
		String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")";
//		System.out.println();
		a++;
		if (a > 2) {
			setTitle(stats);

        	loadShader();
        	a = 0;
		}
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
		this.text.onTextInput(codepoint);
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		if (this.text.focused) {
			this.text.onKeyPress(key, scancode, action, mods);
		}
	}

	@Override
	public void render(float f) {
		this.fb2.bind();
		this.fb2.clearFrameBuffer();
		this.shaderHeavy.enable();
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.image);
		Engine.drawFullscreenQuad();
		FrameBuffer.unbindFramebuffer();
		glClearColor(1, 1, 1, 0);
		glClear(GL11.GL_DEPTH_BUFFER_BIT);
		Shaders.textured.enable();
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fb2.getTexture(0));
		Engine.drawFullscreenQuad();
		glClear(GL11.GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
		Engine.setBlend(true);
//		
//
		this.text.width = Math.min(500, displayWidth - 100);
		this.text.height = displayHeight - 100;
		this.text.xPos = (int) ((displayWidth-this.text.width)/2.0f);
		this.text.yPos = 50;
		Shaders.gui.enable();
		boolean drawShadow=true;
		float br = 16;
		float z = -4;
		float x = this.text.xPos-br;
		float y = this.text.yPos-br;
		float w = this.text.width+2*br;
		float h = this.text.height+2*br;
		float alpha = 0.5f;
		float shadowSigma = 4;
		float boxSigma = 1f;
		float round = 4.0f;
		float r = 0;
		float g = 0;
		float b = 0;
		
        if (drawShadow) {
            BoxGUI.setZPos(z-1);
            BoxGUI.setBox(x, y+1, x+w, y+h);
//            BoxGUI.setColor(1-r, 1-g, 1-b, alpha);
            BoxGUI.setColor(0.05f,0.05f,0.05f, alpha);
            BoxGUI.setSigma(shadowSigma);
            BoxGUI.setRound(round);
          Engine.enableDepthMask(false);
            Engine.drawQuad();
          Engine.enableDepthMask(true);
        } else {
            BoxGUI.setRound(round);
        }
        BoxGUI.setBox(x, y, x+w, y+h);
        BoxGUI.setZPos(z);
        BoxGUI.setColor(r, g, b, alpha);
        BoxGUI.setSigma(boxSigma);
        Engine.drawQuad();
		Shaders.textured.enable();
//		Engine.pxStack.push(50, 50, 50);
		this.text.drawStringWithCursor(Mouse.getX(), Mouse.getY(), Mouse.isButtonDown(0));
//		Engine.pxStack.pop();
		Engine.setBlend(false);
		glDisable(GL_DEPTH_TEST);
	}


	private void updateMousePos() {
		boolean inside = !(Mouse.getX()<0||Mouse.getX()>displayWidth||Mouse.getY()<0||Mouse.getY()>displayHeight);
		if (!inside) {
			down = false;
		}
		if (!movement.grabbed()&&down) {
			lastMx = (float) Mouse.getX();
			lastMy = (float) Mouse.getY();
		}
		this.shaderHeavy.enable();
		this.shaderHeavy.setProgramUniform2f("iMouse", GameMath.clamp((float)lastMx, 0f, displayWidth), GameMath.clamp(displayHeight-1-(float)lastMy, 0f, displayHeight));

	}
	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
        updateMousePos();

	}

	@Override
	public void postRenderUpdate(float f) {
		this.text.focused = !GameBase.baseInstance.isGrabbed();
	}
	
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
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

        	if (this.shaderHeavy != null) {
        		this.shaderHeavy.release();
        	}

        	this.shaderHeavy = AssetManager.getInstance().loadShader(null, "debug/clouds4");
        	this.shaderHeavy.enable();
        	this.shaderHeavy.setProgramUniform1i("iChannel0", 0);
        	this.shaderHeavy.setProgramUniform1i("iChannel1", 1);
        	updateMousePos();
			

        	if (first) {
        		first=false;
        		ShaderSourceBundle src = this.shaderHeavy.getSource();
        		ShaderSource fragmentSrc = src.getFragment();
        		this.text.setEditText(fragmentSrc.getSource());
        	}
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
        Engine.init();
		TextureManager.getInstance().init();
		FontRenderer.init();
		this.font = FontRenderer.get(0, 12, 0);
		this.text = new TextInput(this.font, this);
		this.text.multiline=true;
		setVSync(true);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
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
