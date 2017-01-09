package test.game.vr;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

import java.nio.IntBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import jopenvr.JOpenVRLibrary;
import jopenvr.JOpenVRLibrary.EVRCompositorError;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import test.game.CameraController;

public class VRApp extends GameBase {
	public VRApp() {
		TICKS_PER_SEC = 20;
		Engine.initRenderers = false;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new VRApp().startGame();
	}
	public enum InputSource {
		MOUSE, HEADTRACKING
	};
	InputSource selInputSource = InputSource.HEADTRACKING;
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	final CameraController cameraController = new CameraController();
	private FrameBuffer sceneFB;

	private GLTriBuffer cube;
	Shader modelShader;
	
	int tick = 0;
	private static boolean startup;
	
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_model_shader = assetMgr.loadShader(newshaders, "model/model_viewer");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            modelShader = new_model_shader;
            Shader.disable();
        } catch (ShaderCompileError e) {
            newshaders.release();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
            }
        }
        startup = false;
    }
    int action = 0;
	@Override
	public void onStatsUpdated() {
//		System.out.println(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
		tick--;
		if (tick <= 0) {
			setTitle(lastFPS+"");
//			initShaders();
//			Shaders.initShaders();
			tick = 5;

//	        redraw();
			try {
				once = false;
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("FAIL");
			}
		}
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
	}

    boolean once = false;
    Vector3f tmp = new Vector3f();
    void renderScene(float f) {
    	Vector3f v1 = new Vector3f(0, 0, 0);
    	Matrix4f.transform(Engine.getMatSceneMVP(), v1, v1);
//    	System.out.println( mvp);
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
		Shaders.colored3D.enable();
//		tessState.drawQuads();
		modelShader.enable();
		modelShader.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
//		glPointSize(4.0f);
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, TMgr.getEmptyWhite());
		Engine.pxStack.push();
		glDisable(GL_BLEND);
		int k = 4;
		int r = 10;
        Engine.bindVAO(GLVAO.vaoStaticModel);
        Engine.bindBuffer(cube.getVbo());
        Engine.bindIndexBuffer(cube.getVboIndices());
		for (int i = -k; i <= k; i++) {
			for (int j = -k; j <= k; j++) {
				tmp.set(i*r, 0, j*r);
				int nn = Engine.camFrustum.sphereInFrustum(tmp, 2);
				if (nn > -1) {
//					System.out.println("in "+nn);
					Engine.pxStack.setTranslation(i*r, 0, j*r);	
			        this.cube.drawElements();
				} else{
//					System.out.println("out "+nn);
				}
						

			}
		}
		Engine.pxStack.pop();
		FrameBuffer.unbindFramebuffer();
        Engine.checkGLError("Pass0");
    }
	@Override
	public void render(float f) {

		for (int i = 0; i < 3; i++) {
			VR.setupCamera(i, f);
			renderScene(f);
	        Shaders.tonemap.enable();
	        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
	        VR.bindAndClearFramebuffer(i);
	        Engine.drawFullscreenQuad();
		}
		FrameBuffer.unbindFramebuffer();
        Engine.checkGLError("drawAll");
		VR.Submit();
	}

	private Vec3D tmpPos = new Vec3D();
	@Override
	public void preRenderUpdate(float f) {
		VR.updatePose(f);
		this.cameraController.update(movement);
		Vec3D.interp(this.cameraController.lastPos, this.cameraController.pos, f, this.tmpPos);
		Engine.camera.setOrientation(this.cameraController.yaw, this.cameraController.pitch, false, 4.0f);
		Engine.camera.setPosition(this.tmpPos);
		Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	boolean hadContext = false;
	
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
        }
        glActiveTexture(GL_TEXTURE0);
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
	public void redraw() {

		VertexBuffer buf = new VertexBuffer(1024*1024);
        RenderUtil.makeCube(buf, 1.0f, GLVAO.vaoStaticModel);
        int data = cube.upload(buf);
        System.out.println("uploaded "+(data*4)+" bytes for format 1");
	}

	@Override
	public void lateInitGame() {
		cube = new GLTriBuffer(GL15.GL_STREAM_DRAW);
		

		redraw();
		initShaders();
		VR.initApp(this);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
