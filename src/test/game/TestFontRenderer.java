package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.BufferUtils.*;



import org.lwjgl.BufferUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;



import java.nio.ByteBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.input.InputController;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;

public class TestFontRenderer extends GameBase {
	final CameraController cameraController = new CameraController();

	public static void main(String[] args) {
		TICKS_PER_SEC = 20;
		Engine.initRenderers = false;
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestFontRenderer().startGame();
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
		if (a > 12) {
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
        
		glDepthFunc(GL_EQUAL);
		
        this.shaderHeavy.enable();
        Engine.drawFullscreenQuad();
		FrameBuffer.unbindFramebuffer();
		
		glDepthFunc(GL_LEQUAL);
        glClearColor(0.2f,0.2f,0.2f,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
//
        Shaders.textured.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fb2.getTexture(0));
        Engine.drawFullscreenQuad();
		{
			String text = "Can you read me?";
			Shaders.textured.enable();
			FontRenderer fr = FontRenderer.get(0, 18, 1);
			float tw = fr.getStringWidth(text);
			fr.drawString(text, ((float)Game.displayWidth-tw)/2.0f, 110, -1, true, 1.0f);
		}
		{
			String text = "----";
			Shaders.textured.enable();
			FontRenderer fr = FontRenderer.get(0, 10, 1);
			fr.drawString(text, ((float)Game.displayWidth)/2.0f, 140, -1, true, 1.0f, 2);
			float tw = fr.getStringWidth(text);
			fr.drawString(text, ((float)Game.displayWidth-tw)/2.0f, 170, -1, true, 1.0f);
		}
		{
			String text = "----";
			Shaders.textured.enable();
			FontRenderer fr = FontRenderer.get(0, 10, 1);
			fr.drawString(text, ((float)Game.displayWidth)/2.0f, 210, -1, true, 1.0f, 0);
		}
		{
			String text = "Right\nMulti\nLine";
			Shaders.textured.enable();
			FontRenderer fr = FontRenderer.get(0, 22, 1);
			float tw = fr.getStringWidth(text);
			fr.drawString(text, Game.displayWidth, 140, -1, true, 1.0f, 1);
		}
		{
			String text = "Left\nMulti\nLine";
			Shaders.textured.enable();
			FontRenderer fr = FontRenderer.get(0, 22, 1);
			float tw = fr.getStringWidth(text);
			fr.drawString(text, 0, 140, -1, true, 1.0f, 0);
		}
		{
			String text = "Center\nMulti\nLine";
			Shaders.textured.enable();
			FontRenderer fr = FontRenderer.get(0, 22, 1);
			float tw = fr.getStringWidth(text);
			fr.drawString(text, Game.displayWidth/2.0f, 340, -1, true, 1.0f, 2);
		}
		Shaders.colored.enable();
		GL11.glLineWidth(2.0f);
		Tess.instance.setColorF(0xff00ff, 1.0f);
		Tess.instance.add(Game.displayWidth/2.0f, 0);
		Tess.instance.add(Game.displayWidth/2.0f, Game.displayHeight-1);
		Tess.instance.draw(GL_LINES);
		
	}

	private Vec3D tmpPos = new Vec3D();
	private Shader shaderHeavy;
	private Shader shaderTexture;
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
	}

	@Override
	public void postRenderUpdate(float f) {
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
        	if (fb != null) {
        		fb.release();
        	}
            fb = new FrameBuffer(displayWidth, displayHeight);
            fb.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
            fb.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
            fb.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
            fb.setHasDepthAttachment();
            fb.setup(null);
        	if (fb2 != null) {
        		fb2.release();
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
        		this.shaderTexture.release();
        	}

        	if (this.shaderHeavy != null) {
        		this.shaderHeavy.release();
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
        Engine.init();
		TextureManager.getInstance().init();
		FontRenderer.init();
		setVSync(true);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
		FrameBuffer.unbindFramebuffer();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
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
