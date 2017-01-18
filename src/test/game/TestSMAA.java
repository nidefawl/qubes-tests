package test.game;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.input.KeybindManager;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;

public class TestSMAA extends GameBase {
	final CameraController cameraController = new CameraController();
	public TestSMAA() {
		TICKS_PER_SEC = 20;
		Engine.initRenderers = false;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestSMAA().startGame();
	}
	private int image;
	SMAA smaa;
	

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
        glClearColor(0.11F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		Engine.setBlend(false);
//        Shaders.textured.enable();
//        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.image);
        //TODO: move outside
        Engine.setBlend(false);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        smaa.render(this.image, 0, null);
//        smaa.renderDebug(this.image, 1);
//        Engine.drawFullscreenQuad();
	}

	private Vec3D tmpPos = new Vec3D();
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
		AssetTexture t = AssetManager.getInstance().loadPNGAsset("textures/Unigine01.png");
		GLFW.glfwSetWindowSize(windowId, t.getWidth(), t.getHeight());
		this.image = TextureManager.getInstance().makeNewTexture(t, false, true, 0);
		FrameBuffer.unbindFramebuffer();
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}
}
