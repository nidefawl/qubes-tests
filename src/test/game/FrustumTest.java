package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.opengl.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.*;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vec.Vector3f;

public class FrustumTest extends GameBase {
	final CameraController cameraController = new CameraController();
	private FrameBuffer sceneFB;
	public FrustumTest() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new FrustumTest().startGame();
	}
	

	int tick = 0;
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	private static boolean startup;



	Shader modelShader;

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
			initShaders();
			Shaders.initShaders();
			tick = 5;

	        redraw();
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
	@Override
	public void render(float f) {
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
        Engine.bindVAO(GLVAO.vaoModel);
        Engine.bindBuffer(this.buf.getVbo());
        Engine.bindIndexBuffer(this.buf.getVboIndices());
        int nRendered = 0;
		for (int i = -k; i <= k; i++) {
			for (int j = -k; j <= k; j++) {
				int nSphere = (i+k)+(j+k)*(k*2+1);
				tmp.set(i*r, 0, j*r);
				int nn = Engine.camFrustum.sphereInFrustum(tmp, 2);
				if (nn > -1) {
					Engine.pxStack.setTranslation(i*r, 0, j*r);	
			        this.buf.drawElements();
					nRendered++;
				} else{
//					System.out.println(nSphere+" frustum result "+nn);
				}
						

			}
		}
		System.out.println(nRendered);
		Engine.pxStack.pop();
		
		FrameBuffer.unbindFramebuffer();
        GLDebugTextures.readTexture(false, "Pass0", "texColor", Engine.getSceneFB().getTexture(0));
        Engine.checkGLError("Pass0");
        glClearColor(0,0,0,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.tonemap.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        Engine.drawFullscreenQuad();
        GLDebugTextures.drawAll(displayWidth, displayHeight);
        Engine.checkGLError("drawAll");
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
        Engine.updateFrustumFromInternal();
        UniformBuffer.updateUBO(null, f);
	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	boolean hadContext = false;
	private VertexBuffer vertexBuf;
	private GLTriBuffer buf;
	
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

		this.vertexBuf.reset();
		RenderUtil.makeSphere(this.vertexBuf, 2, 16, 16);
		System.out.println(vertexBuf.getVertexCount()+"/"+vertexBuf.getTriIdxPos());
		buf.upload(this.vertexBuf);
	}

	@Override
	public void lateInitGame() {
		this.vertexBuf = new VertexBuffer(1024*1024);
		this.buf = new GLTriBuffer(GL15.GL_STATIC_DRAW);
		

		redraw();
		initShaders();
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
