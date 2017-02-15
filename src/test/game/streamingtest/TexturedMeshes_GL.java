package test.game.streamingtest;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.io.*;
import java.nio.ByteBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.*;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.*;
import nidefawl.qubes.texture.TextureCreateInfo.*;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vulkan.VKContext;
import nidefawl.qubes.vulkan.VkTess;

public class TexturedMeshes_GL extends GameBase {
	public TexturedMeshes_GL() {
		TICKS_PER_SEC = 20;
		DEBUG_LAYER = false;
	}
	static SimpleResourceManager newshaders = new SimpleResourceManager();
	static SimpleResourceManager shaders = new SimpleResourceManager();
	final CameraController cameraController = new CameraController();
	private int texture;
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TexturedMeshes_GL().startGame();
	}
	
	private Shader shaderTexturedLight;
	private TesselatorState[] cubes;
	private TesselatorState[] planes;
	private int currentBuffer;
	private int nextBuffer;
	@Override
	public void initGame() {
        Engine.init(EngineInitSettings.INIT_NONE.setInverseZ().setInverseYOpengl());
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		glEnable(GL_DEPTH_TEST);
		Engine.setBlend(false);
		this.cameraController.set(-1.58f, 0.01f, 1.50f, 0.00f, 17.64f);
	}

	@Override
	public void lateInitGame() {
		// TODO Auto-generated method stub
		AssetBinary bin = AssetManagerClient.getInstance().loadBin("vulkan/texture.bin");
		TextureBinMips texture2dData = new TextureBinMips(bin);
		TextureCreateInfo.TextureSub[] mips = new TextureCreateInfo.TextureSub[texture2dData.mips];
		for (int i = 0; i < mips.length; i++) {
			mips[i] = new TextureCreateInfo.TextureSub(texture2dData.w[i], texture2dData.h[i], texture2dData.sizes[i]);
		}
		ByteBuffer data = ByteBuffer.wrap(texture2dData.data);

		TextureCreateInfo tinfo = new TextureCreateInfo(TextureFormat.BC2, mips, data);
		tinfo.setFilter(FilterType.LINEAR);
		tinfo.setUVMode(UVCoordMode.CLAMP);
		tinfo.setAnisotropicFilterLevel(16);
		this.texture = TextureManager.getInstance().makeCompressedTexture(tinfo);
		currentBuffer = 0;
		cubes = new TesselatorState[32];
		planes = new TesselatorState[32];
//		Tess tess = Tess.instance;
//		tess.setNormals(0, 0, 1);
//		tess.setColorF(-1, 1.0f);
//        for (int a = 0; a < 10; a++) {
//        	float z = -a*1.01f;
//    		tess.add(1.0f,  1.0f, z, 1.0f, 1.0f);
//    		tess.add(-1.0f,  1.0f, z, 0.0f, 1.0f);
//    		tess.add(-1.0f, -1.0f, z, 0.0f, 0.0f);
//    		tess.add(1.0f, -1.0f, z, 1.0f, 0.0f);
//        }
//		tess.draw(GL_QUADS, this.tessState);
		Shaders.textured3D.enable();
		Shaders.textured3D.setProgramUniform1f("color_brightness", 1.0f);
		Shader.disable();
		glClearColor(100f/255f, 100f/255f, 100f/255f, 1.0f);
		Engine.setZBufferSetting();
		GL11.glDisable(GL11.GL_CULL_FACE);
		initShaders();
	}

	private void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_texturedLight = assetMgr.loadShader(newshaders, "debug/textured_light");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            this.shaderTexturedLight = new_texturedLight;
            this.shaderTexturedLight.enable();
            this.shaderTexturedLight.setProgramUniform1i("tex0", 0);
            Shader.disable();
        } catch (ShaderCompileError e) {
            newshaders.release();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
        }
	}
	boolean update = true;
	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
//        Vector4f v = Vector4f.pool(0, 0, -33);
//        v.w = 1;
//        Matrix4f.transform(Engine.getMatSceneP(), v, v);
//        System.out.println(v);
        if (update) {
            Tess tess = Tess.instance;
    		int l = 12;
    		float d = 5;
    		for (int x = -l; x <= l; x++) {
    			for (int z = -l; z <= l; z++) {
    				for (int y = -l; y <= l; y++) {
    					tess.setOffset(x*d, y*d, z*d);
    					tess.setNormals(0, 0, 1);
    					tess.add( 1,  1, 1, 1, 1);
    					tess.add(-1,  1, 1, 0, 1);
    					tess.add(-1, -1, 1, 0, 0);
    					tess.add( 1, -1, 1, 1, 0);
    					tess.setNormals(0, 0, -1);
    					tess.add(-1, -1, -1, 0, 0);
    					tess.add(-1,  1, -1, 0, 1);
    					tess.add( 1,  1, -1, 1, 1);
    					tess.add( 1, -1, -1, 1, 0);
    					tess.setNormals(0, 1, 0);
    					tess.add(-1,  1, -1, 0, 0);
    					tess.add(-1,  1,  1, 0, 1);
    					tess.add( 1,  1,  1, 1, 1);
    					tess.add( 1,  1, -1, 1, 0);
    					tess.setNormals(0, -1, 0);
    					tess.add(-1, -1,  1, 0, 1);
    					tess.add(-1, -1, -1, 0, 0);
    					tess.add( 1, -1, -1, 1, 0);
    					tess.add( 1, -1,  1, 1, 1);
    					tess.setNormals(1, 0, 0);
    					tess.add( 1, -1, -1, 0, 1);
    					tess.add( 1, -1,  1, 0, 0);
    					tess.add( 1,  1,  1, 1, 0);
    					tess.add( 1,  1, -1, 1, 1);
    					tess.setNormals(-1, 0, 0);
    					tess.add(-1, -1, -1, 0, 1);
    					tess.add(-1, -1,  1, 0, 0);
    					tess.add(-1,  1,  1, 1, 0);
    					tess.add(-1,  1, -1, 1, 1);
    				}
    				
    			}
    		}
    		if (cubes[nextBuffer] == null) {
    			cubes[nextBuffer] = new TesselatorState(GL15.GL_STATIC_DRAW);
    		}
    		if (planes[nextBuffer] == null) {
    			planes[nextBuffer] = new TesselatorState(GL15.GL_STATIC_DRAW);
    		}
    		tess.draw(GL11.GL_QUADS, cubes[nextBuffer]);
    		tess.setOffset(0, 0, 0);
    		tess.setNormals(0, 0, 1);
    		tess.add( 2,  2, 3.5f, 1, 1);
    		tess.add(-2,  2, 3.5f, 0, 1);
    		tess.add(-2, -2, 3.5f, 0, 0);
    		tess.add( 2, -2, 3.5f, 1, 0);
    		tess.draw(GL11.GL_QUADS, planes[nextBuffer]);
        }
	}

	int nframes = 0;
	@Override
	public void render(float f) {
		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		shaderTexturedLight.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, texture);
        if (nframes++ > 2) {
    		this.cubes[currentBuffer].drawQuads();
    		this.planes[currentBuffer].drawQuads();
        }
	}

	@Override
	public void postRenderUpdate(float f) {
        currentBuffer = (currentBuffer+1)%cubes.length;
        nextBuffer = (currentBuffer+2)%cubes.length;

	}
	
	@Override
	public void onStatsUpdated() {
		String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+") SPIR: "+SMAA.LOAD_SPIR;
		setTitle(stats);
		initShaders();
	}

	@Override
	public void tick() {
		this.cameraController.tickUpdate();
//		System.out.printf("%.2f, %.2f, %.2f, %.2f, %.2f\n",
//				this.cameraController.pos.x,
//				this.cameraController.pos.y,
//				this.cameraController.pos.z,
//				this.cameraController.pitch,
//				this.cameraController.yaw);
	}
	
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        }
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}
	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {

		if (action == GLFW.GLFW_PRESS) {
			switch (key) {
			case GLFW.GLFW_KEY_SPACE:
				update = !update;
				break;
			}
		}

	
	}
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
	}

}
