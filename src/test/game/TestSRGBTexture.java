package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.io.*;

import org.lwjgl.opengl.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.*;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.EngineInitSettings;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.*;
import nidefawl.qubes.util.*;

public class TestSRGBTexture extends GameBase {
	private AssetTexture t;
	private int imageSRGB;
	private int imageRGB;


	public TestSRGBTexture() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {


        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestSRGBTexture().startGame();
	}
	

	@Override
	public void onStatsUpdated() {
		System.out.println(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
		Shaders.initShaders();
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(float f) {
		glClearColor(0,0,0,0);
		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		Shaders.textured.enable();
		ITess tess = Engine.getTess();
		int infoY = 100;
		int infoX = 100;
		int infoW = 320;
		tess.resetState();
		Shaders.textured_to_lin.enable();
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.imageRGB);
		tess.setColorF(-1, 1);
		tess.add(infoX+infoW, infoY, 0, 1, 0);
		tess.add(infoX, infoY, 0, 0, 0);
		tess.add(infoX, infoY+infoW, 0, 0, 1);
		tess.add(infoX+infoW, infoY+infoW, 0, 1, 1);
		tess.drawQuads();
		infoX+=infoW+30;
		Shaders.textured.enable();
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.imageSRGB);
		tess.setColorF(-1, 1);
		tess.add(infoX+infoW, infoY, 0, 1, 0);
		tess.add(infoX, infoY, 0, 0, 0);
		tess.add(infoX, infoY+infoW, 0, 0, 1);
		tess.add(infoX+infoW, infoY+infoW, 0, 1, 1);
		tess.drawQuads();
		infoX+=infoW+30;
		Shaders.item.enable();
		GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
		tess.setColorF(-1, 1);
		tess.setUIntLSB(3);
		tess.add(infoX+infoW, infoY, 0, 1, 0);
		tess.add(infoX, infoY, 0, 0, 0);
		tess.add(infoX, infoY+infoW, 0, 0, 1);
		tess.add(infoX+infoW, infoY+infoW, 0, 1, 1);
		tess.drawQuads();

	}

	@Override
	public void preRenderUpdate(float f) {
        UniformBuffer.updateUBO(null, f);
	}

	@Override
	public void postRenderUpdate(float f) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        }
	}

	@Override
	public void tick() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initGame() {
		Engine.RENDER_SETTINGS.ssr = 0;
        Engine.init(EngineInitSettings.INIT_NONE.setFBSize(windowWidth, windowHeight));
		TextureManager.getInstance().init();
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		setVSync(true);
	}

	@Override
	public void lateInitGame() {
		Engine.SRGB_TEXTURES = true;
		Engine.setBlend(false);
		Engine.enableDepthMask(false);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		this.t = AssetManager.getInstance().loadPNGAsset("textures/blocks_512/ground/dirt.png");
		this.imageSRGB = TextureManager.getInstance().makeNewTexture(t, false, true, 0, GL21.GL_SRGB8_ALPHA8);
		this.imageRGB = TextureManager.getInstance().makeNewTexture(t, false, true, 0, GL11.GL_RGBA8);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
        RenderAssets.load(null, loadingScreen);
	}


	/* (non-Javadoc)
	 * @see nidefawl.qubes.GameBase#onWheelScroll(long, double, double)
	 */
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		// TODO Auto-generated method stub
		
	}

}
