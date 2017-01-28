package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;


public class TestSMAA extends GameBase {

	static SimpleResourceManager newshaders = new SimpleResourceManager();
	static SimpleResourceManager shaders = new SimpleResourceManager();
    final static boolean SRGB = false;
	final CameraController cameraController = new CameraController();
	public TestSMAA() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestSMAA().startGame();
	}
	private int image;
	SMAA smaa;

	private Vec3D tmpPos = new Vec3D();
	private AssetTexture t;
	boolean once = false;
	private Shader shaderGammaToLin;
	private Shader shaderLinToGamma;
	boolean locked;
	int lx;
	int ly;
	private boolean useQuads;
	private Shader shaderZoomTex;

	@Override
	public void onStatsUpdated() {
		System.out.println(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
		
		if (tick>=80) {
			setTitle(""+lastFPS+" "+drawMode+" "+(useQuads?"quad":"tri"));
			tick = 0;
        	if (smaa != null) {
        		smaa.releaseAll(EResourceType.FRAMEBUFFER);
        	}
        	smaa = new SMAA(SMAA.SMAA_PRESET_MEDIUM, false, SRGB, useQuads);
        	smaa.init(displayWidth, displayHeight);
        	Shaders.initShaders();
        	initShaders();
		}
	}

	private void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader shaderGammaToLin = AssetManager.getInstance().loadShader(newshaders, "textured", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("SAMPLER_CONVERT_GAMMA".equals(define)) {
                        return "#define SAMPLER_SRGB_TO_LIN 1";
                    }
                    return null;
                }
            });
            Shader shaderLinToGamma = AssetManager.getInstance().loadShader(newshaders, "textured", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("SAMPLER_CONVERT_GAMMA".equals(define)) {
                        return "#define SAMPLER_LIN_TO_SRGB 1";
                    }
                    return null;
                }
            });
            Shader zoomtex = assetMgr.loadShader(newshaders, "post/SMAA/zoomtexture");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            this.shaderGammaToLin = shaderGammaToLin;
            this.shaderLinToGamma = shaderLinToGamma; 
            this.shaderZoomTex = zoomtex; 
            this.shaderZoomTex.enable();
            this.shaderZoomTex.setProgramUniform1i("texColor", 0);
            this.shaderGammaToLin.enable();
            this.shaderGammaToLin.setProgramUniform1i("tex0", 0);
            this.shaderLinToGamma.enable();
            this.shaderLinToGamma.setProgramUniform1i("tex0", 0);
            Shader.disable();
        } catch (ShaderCompileError e) {
            newshaders.release();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
        }
	}
	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	int drawMode = 0;
	private FrameBuffer outputBuffer;
	private boolean renderPixelInspector;
	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		if (action == GLFW.GLFW_PRESS||action==GLFW.GLFW_REPEAT) {
			switch (key) {
			case GLFW.GLFW_KEY_UP:
				ly--;
				break;
			case GLFW.GLFW_KEY_DOWN:
				ly++;
				break;
			case GLFW.GLFW_KEY_LEFT:
				lx--;
				break;
			case GLFW.GLFW_KEY_RIGHT:
				lx++;
				break;
			}
			
		}
		if (action == GLFW.GLFW_PRESS) {
			switch (key) {
			case GLFW.GLFW_KEY_SPACE:
				drawMode = (drawMode+1)%4;
				setTitle(""+lastFPS+" "+drawMode);
				break;
			case GLFW.GLFW_KEY_2:
				renderPixelInspector = !renderPixelInspector;
				break;
			case GLFW.GLFW_KEY_3:
				useQuads = !useQuads;
				tick+=80;
				break;
			case GLFW.GLFW_KEY_1:
				locked = !locked;
				lx=GameMath.floor(Mouse.getX());
				ly=GameMath.floor(Mouse.getY());
			}
		}
	}
	@Override
	public void render(float f) {

		if (SRGB)
        GL11.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
		glEnable(GL_DEPTH_TEST);
		smaa.render(this.image, 0, drawMode, outputBuffer);
		glDisable(GL_DEPTH_TEST);
		if (SRGB)
        GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
		FrameBuffer.unbindFramebuffer();
		glClearColor(0.11F, 0.82F, 1.00F, 1F);
		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		Shaders.textured.enable();
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.outputBuffer.getTexture(0));
		Engine.drawFullscreenQuad();
		shaderZoomTex.enable();
		int mx = locked?lx:GameMath.floor(Mouse.getX());
		int my = locked?ly:GameMath.floor(Mouse.getY());
		shaderZoomTex.setProgramUniform2f("mousePixelPos", mx, (this.t.getHeight()-1-my));
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.outputBuffer.getTexture(0));
		Engine.drawFSTri();
		Shader.disable();
		if (!renderPixelInspector) {
			return;
		}
		readImage(this.outputBuffer.getTexture(0));
		Shaders.colored.enable();
		Tess tess = Tess.instance;

		float scale = 32;
		float gX = 4;
		float gY = 4;
		float inset = 1f;
		Engine.setBlend(true);
		tess.setOffset(gX, gY, 0);
		tess.setColorF(0x232323, 0.7f);
		tess.add(scale*9+inset*2, -inset);
		tess.add(-inset, -inset);
		tess.add(-inset, scale*9+inset*2);
		tess.add(scale*9+inset*2, scale*9+inset*2);
		tess.setColorF(-1, 0.7f);
		float posX = gX + 4*scale;
		float posY = gY + 4*scale;
		tess.setOffset(posX, posY, 0);
		tess.add(scale, -inset);
		tess.add(-inset, -inset);
		tess.add(-inset, scale);
		tess.add(scale, scale);
		tess.drawQuads();
		Engine.setBlend(false);
		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < 9; y++) {
				 posX = gX + x*scale;
				 posY = gY + y*scale;
				tess.setOffset(posX, posY, 0);
				int rgb = 0;
				int tx= mx-4+x;
				int ty= my-4+y;
				if (tx >= 0 && tx < this.texW&&ty >= 0 && ty < this.texH) {
					int idxT = (texH-1-ty)*texW+tx;
					if (idxT > 0 && idxT < texData.length) {
//						System.out.println(posX+","+posY+" = "+idxT+" - "+Integer.toHexString(rgb));
						int n = texData[idxT];
//						rgb = Integer.reverseBytes(texData[idxT]);
						int r = n & 0xFF;
						n>>=8;
						int g = n & 0xFF;
						n>>=8;
						int b = n & 0xFF;
						n>>=8;
						int a = n & 0xFF;
						rgb = r<<16|g<<8|b;
					}
				}
				tess.setColorF(rgb, 1);
				tess.add(scale-inset*2, inset);
				tess.add(inset, inset);
				tess.add(inset, scale-inset*2);
				tess.add(scale-inset*2, scale-inset*2);
			}
			
		}
		tess.setOffset(0, 0, 0);
		tess.drawQuads();
		tess.resetState();
		
		
		int infoY = (int) (gY+scale*9+inset*2);
		int infoX = (int) (gX);
		int infoW = (int) ((scale)*9);
		tess.setColorF(0x444444, 0.7f);
		tess.add(infoX+infoW, infoY);
		tess.add(infoX, infoY);
		tess.add(infoX, infoY+70);
		tess.add(infoX+infoW, infoY+70);
		tess.drawQuads();
		Shaders.textured.enable();
		Engine.setBlend(true);
		int idx = (texH-1-my)*texW+mx;
		if (idx > 0 && idx < texData.length) {
			int n = texData[idx];
			int r = n & 0xFF;
			n>>=8;
			int g = n & 0xFF;
			n>>=8;
			int b = n & 0xFF;
			n>>=8;
			int a = n & 0xFF;
//			System.out.println(r+","+g+","+b+","+a);
			this.font.drawString("xy", infoX, infoY + 30, -1, true, 1.0f, 0);
			this.font.drawString(""+mx+",", infoX+80, infoY + 30, -1, true, 1.0f, 1);
			this.font.drawString(""+my, infoX+60+80, infoY + 30, -1, true, 1.0f, 1);
			this.font.drawString(""+r, infoX+60, infoY + 60, -1, true, 1.0f, 1);
			this.font.drawString(""+g, infoX+60+60, infoY + 60, -1, true, 1.0f, 1);
			this.font.drawString(""+b, infoX+60+60+60, infoY + 60, -1, true, 1.0f, 1);
			this.font.drawString(""+a, infoX+60+60+60+60, infoY + 60, -1, true, 1.0f, 1);
		}
		Engine.setBlend(false);
	}

	int[] texData=new int[0];
	private int texW;
	private int texH;
	private FontRenderer font;
	private void readImage(int texture) {
		glBindTexture(GL_TEXTURE_2D, texture);
		Engine.checkGLError("glBindTexture");
		Engine.checkGLError("glGetTexLevelParameteri1");
		this.texW = GL11.glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH); // get width of GL texture
		Engine.checkGLError("glGetTexLevelParameteri2");
		this.texH = GL11.glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT); // get height of GL texture
		Engine.checkGLError("glGetTexLevelParameteri3");
		int size = this.texW*this.texH*4;
		if (texData == null || texData.length != size) {
			texData = new int[size];
		}
		glGetTexImage(GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, texData);
		Engine.checkGLError("glGetTexImage");

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
        if (!once) {
        	once = true;
        }
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
        	if (smaa == null) smaa = new SMAA(SMAA.SMAA_PRESET_MEDIUM);
        	smaa.init(displayWidth, displayHeight);
        	
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
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
	}

	@Override
	public void lateInitGame() {
		this.t = AssetManager.getInstance().loadPNGAsset("textures/Unigine01.png");
		GLFW.glfwSetWindowSize(windowId, t.getWidth(), t.getHeight());
		
        int format = SRGB?GL21.GL_SRGB8_ALPHA8:GL_RGBA8;
		this.image = TextureManager.getInstance().makeNewTexture(t, false, true, 0, format);
        this.outputBuffer = FrameBuffer.make(null, t.getWidth(), t.getHeight(), format, false, true);
        initShaders();
		Engine.setBlend(false);
		this.font=FontRenderer.get(0, 22, 0);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}
}
