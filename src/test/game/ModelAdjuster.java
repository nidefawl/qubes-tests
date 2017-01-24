/**
 * 
 */
package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.windows.*;
import nidefawl.qubes.gui.windows.GuiModelAdjustAbstract.GuiPlayerAdjust;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.models.EntityModelManager;
import nidefawl.qubes.models.render.*;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ModelAdjuster extends GameBase {
	final CameraController cameraController = new CameraController();
	private FrameBuffer sceneFB;
    boolean wasGrabbed = true;
    public static ModelAdjuster instance;
    public ModelAdjuster() {
		TICKS_PER_SEC = 20;
		Engine.initRenderers = false;
		Gui.FONT_SIZE_WINDOW_TITLE = 16;
		Gui.FONT_SIZE_BUTTON = 14;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		instance = new ModelAdjuster();
		instance.startGame();
	}
	

	int tick = 0;
	private static boolean startup;

	@Override
	public void onStatsUpdated() {
		setTitle(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
		tick--;
		if (tick <= 0) {
			Shaders.initShaders();
			tick = 4;
			Engine.renderBatched.initShaders();
//			reloadModel();
		}
		
	}

	boolean once = false;

	private Vec3D tmpPos = new Vec3D();

	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
        if (GuiWindowManager.onKeyPress(key, scancode, action, mods)) {
            return;
        }

	}

	@Override
	public void render(float fTime) {
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();

        
        
        
        
        
		FrameBuffer.unbindFramebuffer();
        glClearColor(0,0,0,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GLDebugTextures.drawAll(displayWidth, displayHeight);
        Engine.checkGLError("drawAll");
        double mx = Mouse.getX();
        double my = Mouse.getY();
        if (this.gui != null) {
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("gui");
            GuiWindow window = GuiWindowManager.getMouseOver(mx, my);
            if (window != null && (Gui.selectedButton == null || Gui.selectedButton.parent != gui)) {
                mx-=10000;
                my-=10000;
            }
            this.gui.render(fTime, mx, my);
            if (window != null && (Gui.selectedButton == null || Gui.selectedButton.parent != gui)) {
                mx+=10000;
                my+=10000;
            }
            
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
        }
        GuiWindowManager.getInstance().render(fTime, mx, my);
//        glDisable(GL_DEPTH_TEST);
        glEnable(GL_DEPTH_TEST);
	}
    public void showGUI(Gui gui) {

        if (gui != null && this.gui == null) {
            if (Mouse.isGrabbed()) {
                setGrabbed(false);
                wasGrabbed = true;
            }
        }
        if (this.gui != null) {
            this.gui.onClose();
        }
        this.gui = gui;
        if (this.gui != null) {
            this.gui.setPos(0, 0);
            this.gui.setSize(displayWidth, displayHeight);
            this.gui.initGui(this.gui.firstOpen);
            this.gui.firstOpen = false;
            if (Mouse.isGrabbed()) {
                setGrabbed(false);
                wasGrabbed = true;
            }
        } else {
            if (wasGrabbed) {
                setGrabbed(true);
            }
            wasGrabbed = false;
        }
            
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
	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
			if (sceneFB != null) sceneFB.release();
	        sceneFB = new FrameBuffer(displayWidth, displayHeight);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGBA16UI);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB16F);
	        sceneFB.setFilter(GL_COLOR_ATTACHMENT2, GL_NEAREST, GL_NEAREST);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
	        sceneFB.setHasDepthAttachment();
	        sceneFB.setup(null);
	        Engine.setSceneFB(sceneFB);
			FrameBuffer.unbindFramebuffer();
            if (this.gui != null) {
                this.gui.setPos(0, 0);
                this.gui.setSize(displayWidth, displayHeight);
                this.gui.initGui(this.gui.firstOpen);
            }
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("onResize");
            GLDebugTextures.onResize();
        }
        glActiveTexture(GL_TEXTURE0);
	}

	@Override
	public void tick() {
		this.cameraController.tickUpdate();
	       if (this.gui != null) {
	           this.gui.update();
	       }
	       GuiWindowManager.update();
	       AsyncTasks.completeTasks();
	}

	@Override
	public void initGame() {
		QModelBatchedRender.isModelViewer = true;
        Engine.init();
		TextureManager.getInstance().init();
        EntityModel.preInit();
        EntityModel.postInit();
		setVSync(true);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
	}

	@Override
	public void lateInitGame() {
        EntityModelManager.getInstance().reload();
//        GuiModelView window = (GuiModelView) GuiWindowManager.openWindow(GuiModelView.class);
//        window.allwaysVisible = true;
//        window.setModel(0);
        GuiPlayerAdjust window = (GuiPlayerAdjust) GuiWindowManager.openWindow(GuiPlayerAdjust.class);
        window.allwaysVisible = true;
        window.setModel(0);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
