package test.game.vulkan;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

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

public class TexturedMesh_GL extends GameBase {
	public TexturedMesh_GL() {
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
		new TexturedMesh_GL().startGame();
	}
	
	private Shader shaderTexturedLight;
	private TesselatorState cubes;
	private TesselatorState planes;
	private FrameBuffer frameBuffer;
	private Shader shadowShader;
	private Shader textured_3Dvk_shadedShader;
	@Override
	public void initGame() {
		EngineInitSettings init = EngineInitSettings.INIT_NONE;
		init.setFBSize(windowWidth, windowHeight).setInverseZ();
		init.initShadowProj=true;
        Engine.init(init);
		TextureManager.getInstance().init();
		setVSync(true);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		glEnable(GL_DEPTH_TEST);
		Engine.setBlend(false);
		this.cameraController.set(-1.58f, 0.01f, 1.50f, 0.00f, 17.64f);
		this.frameBuffer = new FrameBuffer(Engine.getShadowMapTextureSize(), Engine.getShadowMapTextureSize());
		this.frameBuffer.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
		this.frameBuffer.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
		this.frameBuffer.setHasDepthAttachment();
		this.frameBuffer.setup(null);
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
		cubes = new TesselatorState(GL15.GL_STATIC_DRAW);
		planes = new TesselatorState(GL15.GL_STATIC_DRAW);
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
            Shader shadow = assetMgr.loadShader(newshaders, "shadow/shadow_solid");
            Shader textured_3Dvk_shaded = assetMgr.loadShader(newshaders, "textured_3Dvk_shaded");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            shadowShader = shadow;
            textured_3Dvk_shadedShader = textured_3Dvk_shaded;
            this.shaderTexturedLight = new_texturedLight;
            this.shaderTexturedLight.enable();
            this.shaderTexturedLight.setProgramUniform1i("tex0", 0);
            this.textured_3Dvk_shadedShader.enable();
            textured_3Dvk_shadedShader.setProgramUniform1i("samplerColor", 0);
            textured_3Dvk_shadedShader.setProgramUniform1i("texShadow", 1);
            shadowShader.enable();
            shadowShader.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
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
        Engine.getSunLightModel().setTime(7850);
//      Engine.getSunLightModel().setTime(1700+(int)((ticksran+f)*32));
      Engine.getSunLightModel().updateFrame(0);
      Engine.setLightPosition(Engine.getSunLightModel().getLightPosition());
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
		Engine.updateShadowProjections(f);
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
        if (update) {
        	
        	update = false;
        	drawScene();
        	
        }
	}
	void drawScene() {

        Tess tess = Tess.instance;
		int l = 3;
		float d = 5;
		float scale = 4.0f;
		for (int x = -l; x <= l; x++) {
			for (int z = -l; z <= l; z++) {
				for (int y = -l; y <= l; y++) {
					tess.setOffset(x*d*scale, y*d*scale, z*d*scale);
					tess.setNormals(0, 0, 1);
					tess.add( scale,  scale, scale, 1, 1);
					tess.add(-scale,  scale, scale, 0, 1);
					tess.add(-scale, -scale, scale, 0, 0);
					tess.add( scale, -scale, scale, 1, 0);
					tess.setNormals(0, 0, -1);
					tess.add(-scale, -scale, -scale, 0, 0);
					tess.add(-scale,  scale, -scale, 0, 1);
					tess.add( scale,  scale, -scale, 1, 1);
					tess.add( scale, -scale, -scale, 1, 0);
					tess.setNormals(0, 1, 0);
					tess.add(-scale,  scale, -scale, 0, 0);
					tess.add(-scale,  scale,  scale, 0, 1);
					tess.add( scale,  scale,  scale, 1, 1);
					tess.add( scale,  scale, -scale, 1, 0);
					tess.setNormals(0, -1, 0);
					tess.add(-scale, -scale,  scale, 0, 1);
					tess.add(-scale, -scale, -scale, 0, 0);
					tess.add( scale, -scale, -scale, 1, 0);
					tess.add( scale, -scale,  scale, 1, 1);
					tess.setNormals(1, 0, 0);
					tess.add( scale, -scale,  scale, 0, 0);
					tess.add( scale, -scale, -scale, 0, 1);
					tess.add( scale,  scale, -scale, 1, 1);
					tess.add( scale,  scale,  scale, 1, 0);
					tess.setNormals(-1, 0, 0);
					tess.add(-scale, -scale, -scale, 0, 1);
					tess.add(-scale, -scale,  scale, 0, 0);
					tess.add(-scale,  scale,  scale, 1, 0);
					tess.add(-scale,  scale, -scale, 1, 1);
				}
				
			}
		}
		tess.draw(GL11.GL_QUADS, cubes);
		tess.setOffset(0, 0, 0);
		tess.setNormals(0, 0, 1);
		tess.add( 2,  2, 3.5f, 1, 1);
		tess.add(-2,  2, 3.5f, 0, 1);
		tess.add(-2, -2, 3.5f, 0, 0);
		tess.add( 2, -2, 3.5f, 1, 0);
		tess.draw(GL11.GL_QUADS, planes);
    
	}

	@Override
	public void render(float f) {
		Engine.setZBufferSetting();
		this.frameBuffer.bind();

		this.frameBuffer.clearFrameBuffer();
		int mapsize = Engine.getShadowMapTextureSize() / 2;
        Engine.setViewport(0, 0, mapsize, mapsize);
        shadowShader.enable();
        shadowShader.setProgramUniform1i("shadowSplit", 0);
        glEnable(GL_POLYGON_OFFSET_FILL);
        float mult = Engine.isInverseZ?-1:1;
        glPolygonOffset(1.1f*mult, 2.f*mult);
		this.cubes.drawQuads();
        Engine.setViewport(mapsize, 0, mapsize, mapsize);
        shadowShader.setProgramUniform1i("shadowSplit", 1);
        glPolygonOffset(1.2f*mult, 2.f*mult);
		this.cubes.drawQuads();
        Engine.setViewport(0, mapsize, mapsize, mapsize);
        shadowShader.setProgramUniform1i("shadowSplit", 2);
        glPolygonOffset(1.4f*mult, 2.f*mult);
		this.cubes.drawQuads();
        glDisable(GL_POLYGON_OFFSET_FILL);
        Engine.setViewport(0, 0, Engine.fbWidth(), Engine.fbHeight());
		FrameBuffer.unbindFramebuffer();
		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		textured_3Dvk_shadedShader.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, texture);
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.frameBuffer.getDepthTex());
        
		this.cubes.drawQuads();
		this.planes.drawQuads();
		Engine.restoreZBufferSetting();
	}

	@Override
	public void postRenderUpdate(float f) {

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
			case GLFW.GLFW_KEY_ENTER:
				update = true;
				break;
			}
		}

	
	}
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
	}

}
