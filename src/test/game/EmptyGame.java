package test.game;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.Stats;

public class EmptyGame extends GameBase {
	public EmptyGame() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {

		new EmptyGame().startGame();
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
	public void initGame() {
		// TODO Auto-generated method stub
		setVSync(false);
	}

	@Override
	public void lateInitGame() {
		// TODO Auto-generated method stub

	}


	/* (non-Javadoc)
	 * @see nidefawl.qubes.GameBase#onWheelScroll(long, double, double)
	 */
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		// TODO Auto-generated method stub
		
	}

}
