package test.game;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;

public class TestBlending extends GameBase {
	public TestBlending() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestBlending().startGame();
	}
	

	@Override
	public void onStatsUpdated() {
		System.out.println(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
	}

	@Override
	protected void onTextInput(long window, int codepoint) {


	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {


	}
	
	@Override
	public void render(float f) {


	}

	@Override
	public void input(float f) {


	}

	@Override
	public void preRenderUpdate(float f) {
        UniformBuffer.updateUBO(null, f);
	}

	@Override
	public void postRenderUpdate(float f) {


	}

	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        }
	}

	@Override
	public void tick() {


	}

	@Override
	public void initGame() {
        Engine.init();
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}

	@Override
	public void lateInitGame() {
		int[] blendStates = new int[4];
		int[] blendConst = { GL14.GL_BLEND_SRC_RGB, GL14.GL_BLEND_SRC_ALPHA, GL14.GL_BLEND_DST_RGB, GL14.GL_BLEND_DST_ALPHA};
		for (int i = 0; i < 4; i++) {
			blendStates[i] = GL11.glGetInteger(blendConst[i]);
		}
		System.out.printf("%d %d %d %d\n", blendStates[0], blendStates[1], blendStates[2], blendStates[3]);
      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		for (int i = 0; i < 4; i++) {
			blendStates[i] = GL11.glGetInteger(blendConst[i]);
		}
		System.out.printf("%d %d %d %d\n", blendStates[0], blendStates[1], blendStates[2], blendStates[3]);
      GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
      for (int i = 0; i < 3; i++) {
          GL40.glBlendFuncSeparatei(1+i, GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);
      }
      GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		for (int i = 0; i < 4; i++) {
			blendStates[i] = GL11.glGetInteger(blendConst[i]);
		}
		System.out.printf("%d %d %d %d\n", blendStates[0], blendStates[1], blendStates[2], blendStates[3]);
	}


	/* (non-Javadoc)
	 * @see nidefawl.qubes.GameBase#onWheelScroll(long, double, double)
	 */
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {

		
	}

}
