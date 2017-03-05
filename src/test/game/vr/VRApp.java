package test.game.vr;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.vr.VR;

public class VRApp extends GameBase {
	public VRApp() {
		TICKS_PER_SEC = 20;
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
    private FontRenderer font;
	
	int tick = 0;
    int action = 0;
	private String stats;
	
	
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
    
	@Override
	public void onStatsUpdated() {
		this.stats = String.format("%d FPS (%.2fms)", lastFPS, Stats.avgFrameTime);

		if (VR.getFB(0) != null&&Engine.getSceneFB() != null) {
            String s = String.format("%s - Display %dx%d - Window %dx%d - SceneFB %dx%d - VRFB %dx%d - Gui %dx%d", 
            		this.stats, 
            		Engine.displayWidth, Engine.displayHeight, 
            		windowWidth, windowHeight, 
            		Engine.getSceneFB().getWidth(), Engine.getSceneFB().getHeight(), 
            		VR.getFB(0).getWidth(), VR.getFB(0).getHeight(), 
            		Engine.getGuiWidth(), Engine.getGuiHeight());
            setTitle(s);
		}
		tick--;
		if (tick <= 0) {
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
		if (action == GLFW.GLFW_PRESS) {
			switch (key) {
			case GLFW.GLFW_KEY_F1:
				toggleVR();
				break;
			}
		}
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
		Engine.setBlend(false);
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
		
		Engine.setDefaultViewport();
        for (int eye = 0; eye < (VR_SUPPORT ? 2 : 1); eye++) {
            if (VR_SUPPORT) {
                Engine.getMatSceneP().load(eye == 0 ? VR.cam.projLeft : VR.cam.projRight);
                Engine.getMatSceneP().update();
                Engine.setViewMatrix(VR.getViewMat(eye));
                VR.setViewPort(eye);
                Engine.checkGLError("setCameraAndViewport");
            }
			renderScene(f);
	        Shaders.tonemap.enable();
	        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
            FrameBuffer finalTarget = VR_SUPPORT ? VR.getFB(eye) : null;
            if (finalTarget == null) FrameBuffer.unbindFramebuffer();
            else {
                finalTarget.bind();
                finalTarget.clearFrameBuffer();
            }
	        
	        Engine.drawFullscreenQuad();
		}
        if (VR_SUPPORT) {

            FrameBuffer.unbindFramebuffer();
            VR.Submit();
            Engine.checkGLError("VR.Submit");
            setGUIViewport();
            Engine.checkGLError("setGUIProjection");
            VR.drawFullscreenCompanion(Engine.getGuiWidth(), Engine.getGuiHeight());
            Engine.checkGLError("drawFullscreenCompanion");
        }
		glClear(GL_DEPTH_BUFFER_BIT);
		Engine.setBlend(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        int hT = 50;
        int yT = Engine.displayHeight-hT;
		Shaders.colored.enable();
		Tess.instance.setColorF(0x333300, 0.7f);
		Tess.instance.add(220, yT);
		Tess.instance.add(0, yT);
		Tess.instance.add(0, yT+hT);
		Tess.instance.add(220, yT+hT);
		Tess.instance.drawQuads();
		Shaders.textured.enable();
		int y = yT+5;
		this.font.drawString(this.stats, 10, y+=30, -1, true, 1.0f);
		Engine.setBlend(false);
		Engine.checkGLError("drawGUI");
		setSceneViewport();
	}

	private Vec3D tmpPos = new Vec3D();
	@Override
	public void preRenderUpdate(float f) {
		if (VR_SUPPORT) VR.updatePose(f);
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
		Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
	}

	@Override
	public void postRenderUpdate(float f) {
	}
	

	@Override
	public void onWindowResize(int displayWidth, int displayHeight) {
        if (!VR_SUPPORT||isStarting) {
            setRenderResolution(displayWidth, displayHeight);
        }
	}
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        Engine.updateRenderResolution(displayWidth, displayHeight);
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
	}
	
	@Override
	public void tick() {
		this.cameraController.tickUpdate();
	}

	@Override
	public void initGame() {
        Engine.init(windowWidth, windowHeight);
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
		this.font=FontRenderer.get(0, 22, 0);
		

		redraw();
		initShaders();
        VR.initApp();
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
