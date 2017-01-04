package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.font.*;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.input.KeybindManager;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;

public class TestShader extends GameBase implements ITextEdit {
	final CameraController cameraController = new CameraController();
	
	public static void main(String[] args) {
		TICKS_PER_SEC = 20;
		Engine.initRenderers = false;
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestShader().startGame();
	}
	private int image;
	SMAA smaa;
	FrameBuffer fb2;
	

	int a = 0;
	private boolean down;
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
		FrameBuffer.unbindFramebuffer();
        glClearColor(1,1,1,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
//		this.fb2.bind();
//		this.fb2.clearFrameBuffer();
        this.shaderHeavy.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.image);
        Engine.drawFullscreenQuad();
//		FrameBuffer.unbindFramebuffer();
		
//		glDepthFunc(GL_LEQUAL);
//        Shaders.textured.enable();
//        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fb2.getTexture(0));
//        Engine.drawFullscreenQuad();
//        glClear(GL11.GL_DEPTH_BUFFER_BIT);
//		Shaders.textured.enable();
//        glEnable(GL_BLEND);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//        Engine.pxStack.push(50, 50, 50);
//        this.text.width = Game.displayWidth-100;
//        this.text.height = Game.displayHeight-100;
//        this.text.xPos = 50;
//        this.text.yPos = 50;
//        this.text.drawStringWithCursor(Mouse.getX(), Mouse.getY(), Mouse.isButtonDown(0));
//        Engine.pxStack.pop();
	}

	private Vec3D tmpPos = new Vec3D();
	private Shader shaderHeavy;
	private Shader shaderTexture;
	private FontRenderer font;
	private TextInput text;
	float lastMx, lastMy;
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
		this.cameraController.update(movement);
		Vec3D.sub(this.cameraController.pos, this.cameraController.lastPos, this.tmpPos);
		this.tmpPos.scale(f);
		Vec3D.add(this.tmpPos, this.cameraController.lastPos, this.tmpPos);
        Engine.camera.setPosition(this.tmpPos);
        Engine.camera.setOrientation(this.cameraController.yaw, this.cameraController.pitch, false, 4.0f);   
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
        updateMousePos();

	}

	@Override
	public void postRenderUpdate(float f) {
		this.text.focused = !GameBase.baseInstance.isGrabbed();
	}
	
	@Override
	public void onResize(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        	if (smaa != null) {
        		smaa.releaseAll(EResourceType.FRAMEBUFFER);
        	}
        	if (smaa == null) smaa = new SMAA(SMAA.SMAA_PRESET_MEDIUM);
        	smaa.init(displayWidth, displayHeight);
        	if (fb2 != null) {
        		fb2.release();
        	}
            fb2 = new FrameBuffer(displayWidth, displayHeight);
            fb2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
            fb2.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
            fb2.setClearColor(GL_COLOR_ATTACHMENT0, 0, 0, 0, 0);
            fb2.setHasDepthAttachment();
            fb2.setup(null);
        	loadShader();
        }
	}

	@Override
	public void updateInput() {
		super.updateInput();

        if (hasTextHook()!=this.text.focused) {
            setTextHook(this.text.focused);
        }
	}
	/**
	 * 
	 */
	boolean first = true;
	private int image2;
	private void loadShader() {
    	try {
        	if (this.shaderTexture != null) {
        		this.shaderTexture.release();
        	}

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
        	}
    		ShaderSourceBundle src = this.shaderHeavy.getSource();
    		ShaderSource fragmentSrc = src.getFragment();
//    		this.text.setEditText(fragmentSrc.getSource());
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
        Engine.init();
		TextureManager.getInstance().init();
		FontRenderer.init();
		this.font = FontRenderer.get(0, 12, 0);
		this.text = new TextInput(this.font, this);
		this.text.multiline=true;
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
		FrameBuffer.unbindFramebuffer();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
	}

	@Override
	public void lateInitGame() {
		AssetTexture t = AssetManager.getInstance().loadPNGAsset("textures/tex16.png");
		this.image = TextureManager.getInstance().makeNewTexture(t, true, true, 0);
		AssetTexture t2 = AssetManager.getInstance().loadPNGAsset("textures/tex12.png");
		this.image2 = TextureManager.getInstance().makeNewTexture(t2, true, true, 0);
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
