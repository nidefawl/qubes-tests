package test.game;

import java.io.*;

import org.lwjgl.opengl.GL13;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManagerClient;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;

public class EmptyGame extends GameBase {
	public EmptyGame() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {


        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new EmptyGame().startGame();
	}

	@Override
	public void initGame() {
        Engine.init(windowWidth, windowHeight);
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}
	

	@Override
	public void onStatsUpdated() {
		System.out.println(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
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
		// TODO Auto-generated method stub

	}

	@Override
	public void preRenderUpdate(float f) {
		// TODO Auto-generated method stub

	}

	@Override
	public void postRenderUpdate(float f) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tick() {
		// TODO Auto-generated method stub

	}

	@Override
	public void lateInitGame() {
		// TODO Auto-generated method stub
		AssetBinary bin = AssetManagerClient.getInstance().loadBin("vulkan/texture.bin");
		TextureBinMips texture2dData = new TextureBinMips(bin);
	}


	/* (non-Javadoc)
	 * @see nidefawl.qubes.GameBase#onWheelScroll(long, double, double)
	 */
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		// TODO Auto-generated method stub
		
	}

}
