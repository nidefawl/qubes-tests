package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.*;
import java.util.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vec.Vector3f;

public class SkyboxSpriteTest extends GameBase {
	final static int MAX_SPRITES = 1024*64;
    final static int SKYBOX_RES = 512;
	final CameraController cameraController = new CameraController();
	private FrameBuffer sceneFB;
    public FrameBuffer  fbDeferred;
    public FrameBuffer  fbSkybox;
	public SkyboxSpriteTest() {
		TICKS_PER_SEC = 20;
		Engine.initRenderers = false;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new SkyboxSpriteTest().startGame();
	}
	

	int tick = 0;
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	private static boolean startup;

	GLVBO vboAttr;
	GLVBO vboStaticQuad;
	GLVBO vboIdx;


	Shader spriteShader;
	Shader shaderDeferred;

	public Shader skyShader;
	public Shader cloudsShader;
	private Shader skybox;

	private TesselatorState skybox1;
	private TesselatorState skybox2;

	public Vector3f skyColor = new Vector3f(0.34f, 0.54f, 0.96f);
	public Vector3f fogColor = new Vector3f(0.34f, 0.54f, 0.96f);
	
	private String error;
	final CubeMapCamera cubeMatrix = new CubeMapCamera();

    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader particle = assetMgr.loadShader(newshaders, "particle/pointsprite");
            Shader cloudsShader = assetMgr.loadShader(newshaders, "sky/clouds");
            Shader skybox = assetMgr.loadShader(newshaders, "sky/skybox_cubemap");
//            Shader cloudsShader = assetMgr.loadShader(newshaders, "sky/sky");
            Shader new_deferred = assetMgr.loadShader(newshaders, "post/deferred", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("RENDER_PASS".equals(define)) {
                        return "#define RENDER_PASS 0";
                    }
                    return null;
                }
            });
            Shader sky = assetMgr.loadShader(newshaders, "sky/sky");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            shaderDeferred = new_deferred;
            skyShader = sky;
            this.skybox = skybox;
            this.cloudsShader = cloudsShader;
            spriteShader = particle;
            this.shaderDeferred.enable();
            shaderDeferred.setProgramUniform1i("texColor", 0);
            shaderDeferred.setProgramUniform1i("texNormals", 1);
            shaderDeferred.setProgramUniform1i("texMaterial", 2);
            shaderDeferred.setProgramUniform1i("texDepth", 3);
            shaderDeferred.setProgramUniform1i("texShadow", 4);
            shaderDeferred.setProgramUniform1i("texLight", 5);
            shaderDeferred.setProgramUniform1i("texBlockLight", 6);
            shaderDeferred.setProgramUniform1i("texAO", 7);

            spriteShader.enable();
            spriteShader.setProgramUniform1i("tex0", 0);
            cloudsShader.enable();
            cloudsShader.setProgramUniform1i("tex0", 0);
            Shader.disable();
            this.error = null;
        } catch (ShaderCompileError e) {
            newshaders.release();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            this.error="shader " + e.getName() + " failed to compile\n"+e.getLog();
            if (startup) {
                throw e;
            } else {
            }
        }
        startup = false;
    }
    int action = 0;
	private String stats;
	@Override
	public void onStatsUpdated() {
//		ArrayList<String> n = this.glProfileResults;
//		if (!n.isEmpty() && n.size()> 1) {
//			String s = "";
//			for (String s2 : n) {
//				s+=s2+"\n";
//			}
//			this.stats=String.format("%d FPS (%.2fms)\n%d clouds\n%d sprites",
//					lastFPS, Stats.avgFrameTime, this.clouds.size(), this.totalSpritesRendered);
//			this.stats+="\n"+s;
			setTitle(""+lastFPS);
//		}

		
		
		
		tick--;
		if (tick <= 0) {
//			this.stats=String.format("%d FPS (%.2fms)\n%d clouds\n%d sprites",
//					lastFPS, Stats.avgFrameTime, this.clouds.size(), this.totalSpritesRendered);
//			setTitle(stats);
//			initShaders();
//			Shaders.initShaders();
			tick = 4;

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
			case GLFW.GLFW_KEY_1:
				initShaders();
				break;
			case GLFW.GLFW_KEY_2:
				redraw();
				break;
			}
		}
		if (action == GLFW.GLFW_REPEAT||action == GLFW.GLFW_PRESS) {
			switch (key) {
			case GLFW.GLFW_KEY_KP_ADD:
				WEATHER += 0.01f;
				if (WEATHER > 1)
					WEATHER = 1;
				break;
			case GLFW.GLFW_KEY_KP_SUBTRACT:
				WEATHER -= 0.01f;
				if (WEATHER < 0)
					WEATHER = 0;
				break;
			case GLFW.GLFW_KEY_PAGE_UP:
				TIME+=50;
				break;
			case GLFW.GLFW_KEY_PAGE_DOWN:
				TIME-=50;
				break;
			}
		}
	}

    boolean once = false;
    final static Vector3f tmp = new Vector3f();
	@Override
	public void render(float f) {
		
		Engine.enableDepthMask(false);
		if (renderIntoCubemap) {
			glDisable(GL11.GL_DEPTH_TEST);
			UniformBuffer.uboMatrix3D_Temp.bind();
			this.fbSkybox.bind();
	        Engine.setViewport(0, 0, SKYBOX_RES, SKYBOX_RES);
			for (int c = 0; c < 6; c++) {
				this.fbSkybox.bindCubeMapFace(c);
		        cubeMatrix.setupScene(c, Engine.camera.getPosition());
		        renderSky(f);
			}
			Engine.setDefaultViewport();
			
			UniformBuffer.uboMatrix3D.bind();
			
			Engine.enableDepthMask(true);
			glEnable(GL11.GL_DEPTH_TEST);
		}
		
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
		if (renderIntoCubemap) {
			skybox.enable();
	        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_CUBE_MAP, this.fbSkybox.getTexture(0));
			Engine.drawFullscreenQuad();
		}
		
		glDisable(GL11.GL_DEPTH_TEST);
		if (!renderIntoCubemap) {
			renderSky(f);
		}
		
		Engine.checkGLError("Pass0");
		fbDeferred.bind();
		fbDeferred.clearFrameBuffer();
		shaderDeferred.enable();
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
		GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
		GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
		GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
		GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getEmptyWhite()); // SHADOW
		GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, TMgr.getEmpty()); // LIGHTCOMPUTE
		GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3));
		GL.bindTexture(GL_TEXTURE7, GL_TEXTURE_2D, TMgr.getEmptyWhite()); // SSAO
		Engine.drawFullscreenQuad();
		FrameBuffer.unbindFramebuffer();
		glClearColor(0, 0, 0, 0);
		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		Shaders.tonemap.enable();
		Shaders.tonemap.setProgramUniform1f("constexposure", 30);
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbDeferred.getTexture(0));
		Engine.drawFullscreenQuad();

	}

	private void renderSky(float f) {
		glDisable(GL_BLEND);
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.texNoise);
		cloudsShader.enable();
		cloudsShader.setProgramUniform1f("rainStrength", WEATHER);
		cloudsShader.setProgramUniform1i("worldTime", TIME);
		if (GPUProfiler.PROFILING_ENABLED) {
			GPUProfiler.start("clouds");
		}
		Engine.drawFullscreenQuad();
		if (GPUProfiler.PROFILING_ENABLED) {
			GPUProfiler.end();
		}


		glEnable(GL_BLEND);
//        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        spriteShader.enable();
		float weatherStr = (WEATHER);
		weatherStr = GameMath.powf(weatherStr*0.9f, 1.6f);
		spriteShader.setProgramUniform1f("spritebrightness", weatherStr);
        spriteShader.setProgramUniform1f("transparency", 0.4f-weatherStr*0.1f);
//      int nSprites = (int) GameMath.clamp(Math.round(this.totalSprites*(WEATHER*0.7f+0.3f)), 0, this.totalSprites);
        GL30.glBindVertexArray(vaoPos);
        for (int i = 0; i < this.texClouds.length; i++) {
            storeSprites(f, i);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIdx.getVboId());
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.texClouds[i]);
            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0, this.storedSprites);
        }
        GL30.glBindVertexArray(0);
        Engine.bindVAO(null);
		glDisable(GL_BLEND);
	}


	private Vec3D tmpPos = new Vec3D();
	private float curWeather;
	private float lastWeather;
	boolean renderIntoCubemap = false;
	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.update(movement);
		Vec3D.sub(this.cameraController.pos, this.cameraController.lastPos, this.tmpPos);
		this.tmpPos.scale(f);
		Vec3D.add(this.tmpPos, this.cameraController.lastPos, this.tmpPos);
        Engine.camera.setPosition(this.tmpPos);
        Engine.camera.setOrientation(this.cameraController.yaw, this.cameraController.pitch, false, 4.0f);   
        Engine.updateCamera();
        Engine.getSunLightModel().setTime(TIME);
//        Engine.getSunLightModel().setTime(1700+(int)((ticksran+f)*32));
        Engine.getSunLightModel().updateFrame(f);
        Engine.setLightPosition(Engine.getSunLightModel().getLightPosition());
        UniformBuffer.updateUBO(null, f);
        this.updateSprites(f);
        this.curWeather = lastWeather + (WEATHER-lastWeather)*f;
	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	boolean hadContext = false;
	private VertexBuffer vertexBuf;
	
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
			if (sceneFB != null) sceneFB.release();
			if (fbDeferred != null) fbDeferred.release();
			if (fbSkybox != null) fbSkybox.release();
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
	        fbDeferred = FrameBuffer.make(null, displayWidth, displayHeight, GL_RGB16F);
	        fbSkybox = new FrameBuffer(SKYBOX_RES, SKYBOX_RES);
	        fbSkybox.setTextureType(GL13.GL_TEXTURE_CUBE_MAP);
	        fbSkybox.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
//	        fbSkybox.setHasDepthAttachment();
	        fbSkybox.setup(null);
        }
        glActiveTexture(GL_TEXTURE0);
	}

	@Override
	public void tick() {
		this.cameraController.tickUpdate();
		this.updateSpritesTick();
		if (((ticksran-1)/70%2==0) != (ticksran/70%2==0)) {
			renderIntoCubemap = !renderIntoCubemap;
			System.out.println((!renderIntoCubemap) ? "direct" : "cubemap");
		}
		this.lastWeather = WEATHER;
	}

	@Override
	public void initGame() {
        Engine.init();
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
		this.cubeMatrix.init();
	}


	BlockFaceAttr attr = new BlockFaceAttr();
	private int vaoPos;
	List<Cloud> clouds = Lists.newArrayList();
	private ByteBuffer bufMat;
	private FloatBuffer bufMatFloat;
	ReallocIntBuffer vertexUploadDirectBuf;
	private FontRenderer font;
	int storedSprites = 0;
	int totalSpritesRendered = 0;
	void updateSprites(float ftime) {
		for (int i = 0; i < clouds.size(); i++) {
			Cloud cloud = clouds.get(i);
			cloud.update(ftime);
		}
		
	}
	void storeSprites(float ftime, int n) {
		this.bufMatFloat.clear();
		storedSprites = 0;
		for (int i = 0; i < clouds.size(); i++) {
			Cloud cloud = clouds.get(i);
			if (cloud.texture == n) {
				storedSprites+=cloud.store(this.bufMatFloat);
			}
		}
		this.bufMatFloat.flip();
		this.vboAttr.upload(GL15.GL_ARRAY_BUFFER, this.bufMat, this.bufMatFloat.limit()*4);
//		System.out.println("totalSprites "+totalSprites);
		
	}
	private int[] texClouds;
	private int texNoise;
	void updateSpritesTick() {
		for (int i = 0; i < clouds.size(); i++) {
			Cloud sprite = clouds.get(i);
			sprite.tick();
		}
	}
	public void redraw() {
		clouds.clear();
		Random r = new Random(4444);
		float l = 0.2f;
		float hl = 1.2f;
		float hu = 3.6f;
		float motRange = 0.05f;
		float rotRange = 0.0005f;
		float minBr=0.25f;
		float maxBr=1.0f;
		float minSize = 12;
		float maxSize = 76;
		float l2 = minSize*0.6f;
		float h2 = minSize*0.3f;
		for (int i = 0; i < 4; i++) {
			Cloud cloud = new Cloud();
			cloud.texture = r.nextInt(this.texClouds.length);
			cloud.pos.x = r.nextFloat()*l*2.0f-l;
			cloud.pos.y = hl+(hu-hl)*r.nextFloat();
			cloud.pos.z = r.nextFloat()*l*2.0f-l;
//			cloud.mot.x = (r.nextFloat()*2.0f-1.0f)*motRange;
//			cloud.mot.y = 0;
//			cloud.mot.z = (r.nextFloat()*2.0f-1.0f)*motRange;
			for (int j = 0; j < 8; j++) {
				PointSprite sprite = new PointSprite();
				sprite.xoffset = r.nextFloat();
				sprite.yoffset = r.nextFloat();
				sprite.posOffset.x = r.nextFloat()*l2*2.0f-l2;
				sprite.posOffset.y = r.nextFloat()*h2*2.0f-h2;
				sprite.posOffset.z = r.nextFloat()*l2*2.0f-l2;
				sprite.setSize(minSize+r.nextFloat()*(maxSize-minSize));
				float f1 = (minBr+r.nextFloat()*(maxBr-minBr));
				float f2 = f1*0.7f+0.3f*(minBr+r.nextFloat()*(maxBr-minBr));
				sprite.setCol(f1, f2, f2);
				sprite.rot = sprite.lastRot = r.nextFloat()*0.43f;
				
				sprite.rotspeed = (r.nextFloat()*2.0f-1.0f)*rotRange;
				cloud.sprites.add(sprite);
			}
			clouds.add(cloud);
		}
		
	
		
		

        Tess tesselator = Tess.instance;
        int scale = (int) (Engine.zfar / 1.43F);
        int x = -scale;
        int y = -scale / 16;
        int z = -scale;
        int x2 = scale;
        int y2 = scale / 16;
        int z2 = scale;
        int rgbai = 0;
        rgbai = ((int) (fogColor.x * 255.0F)) << 16 | ((int) (fogColor.y * 255.0F)) << 8 | ((int) (fogColor.z * 255.0F));
        //      Shaders.colored.enable();
        tesselator.setColor(rgbai, 255);
        tesselator.add(x, y2, z);
        tesselator.add(x, y, z);
        tesselator.add(x2, y2, z);
        tesselator.add(x2, y, z);
        tesselator.add(x2, y2, z2);
        tesselator.add(x2, y, z2);
        tesselator.add(x, y2, z2);
        tesselator.add(x, y, z2);
        tesselator.add(x, y2, z);
        tesselator.add(x, y, z);
        tesselator.draw(GL_QUAD_STRIP, skybox1);
        //      tesselator.draw(GL_TRIANGLE_STRIP);

        rgbai = ((int) (skyColor.x * 255.0F)) << 16 | ((int) (skyColor.y * 255.0F)) << 8 | ((int) (skyColor.z * 255.0F));
        tesselator.setColor(-1, 255);
        tesselator.add(x, y, z2);
        tesselator.add(x2, y, z2);
        tesselator.add(x2, y, z);
        tesselator.add(x, y, z);
        tesselator.add(x, y2, z);
        tesselator.add(x2, y2, z);
        tesselator.add(x2, y2, z2);
        tesselator.add(x, y2, z2);
        //    tesselator.draw(GL_TRIANGLES);
        tesselator.draw(GL_QUADS, skybox2);
	}
	private void buildQuad(VertexBuffer vertexBuf) {
        vertexBuf.put(Half.fromFloat(0) << 16 | Half.fromFloat(1));
        vertexBuf.put(Half.fromFloat(1) << 16 | Half.fromFloat(1));
        vertexBuf.put(Half.fromFloat(1) << 16 | Half.fromFloat(0));
        vertexBuf.put(Half.fromFloat(0) << 16 | Half.fromFloat(0));
	}
	static class Cloud {
		public int texture;

		List<PointSprite> sprites = Lists.newArrayList();

		Vector3f mot;
		Vector3f pos, lastPos, renderPos;
		public Cloud() {
			this.pos = new Vector3f();
			this.lastPos = new Vector3f();
			this.renderPos = new Vector3f();
			this.mot = new Vector3f();
		}
		public int store(FloatBuffer bufMatFloat) {
			for (PointSprite s : this.sprites) {
				tmp.set(this.renderPos);
				tmp.addVec(s.renderPos);
				tmp.store(bufMatFloat);
				bufMatFloat.put(s.renderSize);
				s.renderCol.store(bufMatFloat);
				bufMatFloat.put(s.renderRot);
			}
			return this.sprites.size();
		}
		public void update(float f) {
		    Vector3f.interp(this.lastPos, this.pos, f, this.renderPos);

			for (PointSprite s : this.sprites) {
				s.update(f);
			}
		}
		public void tick() {
			
		    this.lastPos.set(this.pos);
//		    this.pos.addVec(this.mot);
			for (PointSprite s : this.sprites) {
				s.tick();
			}
		}
	}
	static class PointSprite {
		public float size, initSize, lastSize, renderSize;
		public float rotspeed;
		public float rot, lastRot, renderRot;
		public float xoffset;
		public float yoffset;
		Vector3f posOffset;
		public Vector3f col, lastCol, initCol, renderCol;
		private Vector3f renderPos;
		int tick = 0;
		public PointSprite() {
			this.renderPos = new Vector3f();
			this.posOffset = new Vector3f();
			this.col = new Vector3f();
			this.lastCol = new Vector3f();
			this.initCol = new Vector3f();
			this.renderCol = new Vector3f();
		}
		public void setSize(float size) {
			this.initSize = this.size = this.lastSize = this.renderSize = size;
		}
		public void setCol(float x, float y, float z) {
			this.col.set(x, y, z);
			this.lastCol.set(x, y, z);
			this.initCol.set(x, y, z);
			this.renderCol.set(x, y, z);
		}
		public void update(float f) {
			renderSize = lastSize+(size-lastSize)*f;
		    renderRot = lastRot+(rot-lastRot)*f;
		    Vector3f.interp(lastCol, col, f, renderCol);
		    this.renderPos.set(this.posOffset);
		    {
			    float f2 = (tick+f+xoffset)/15520.0f;
			    f2 = (f2*GameMath.PI)%GameMath.PI*2;
			    posOffset.x += 0.0001f*GameMath.sin(f2);
		    }
		    {
			    float f2 = (tick+f+yoffset)/21220.0f;
			    f2 = (f2*GameMath.PI)%GameMath.PI*2;
			    posOffset.y += 0.0001f*GameMath.sin(f2);
		    }
		    
		}

		public void tick() {
			lastSize = size;
			lastRot = rot;
			this.lastCol.set(this.col);
			float weatherStr = (1-WEATHER);
			weatherStr = GameMath.powf(weatherStr, 2.2f);
			this.col.x = this.initCol.x*(weatherStr);
			this.col.y = this.initCol.y*(weatherStr);
			this.col.z = this.initCol.z*(weatherStr);
			size = initSize*(WEATHER*0.5f+0.5f);
			rot += rotspeed;
			tick++;
		}
	}
	public static float WEATHER = 0.40f;
	public static int TIME = 5850;
	@Override
	public void lateInitGame() {
		this.font=FontRenderer.get(0, 22, 0);
		skybox1 = new TesselatorState(GL15.GL_STATIC_DRAW);
		skybox2 = new TesselatorState(GL15.GL_STATIC_DRAW);
		this.bufMat = Memory.createByteBufferAligned(64, 16*4*MAX_SPRITES);
		this.bufMatFloat = this.bufMat.asFloatBuffer();
		this.vertexUploadDirectBuf = new ReallocIntBuffer();
		this.vaoPos = GL30.glGenVertexArrays();
		this.vertexBuf = new VertexBuffer(1024*1024);
		this.vboStaticQuad = new GLVBO(GL15.GL_STREAM_DRAW);
		this.vboAttr = new GLVBO(GL15.GL_STREAM_DRAW);
		this.vboIdx = new GLVBO(GL15.GL_STREAM_DRAW);
		
        GL30.glBindVertexArray(vaoPos);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboStaticQuad.getVboId());
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL30.GL_HALF_FLOAT, false, 4, 0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboAttr.getVboId());
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 32, 0);
        GL33.glVertexAttribDivisor(1, 1);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, 32, 16);
        GL33.glVertexAttribDivisor(2, 1);
        GL30.glBindVertexArray(0);
        
        
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIdx.getVboId());
        ByteBuffer buf = Memory.createByteBufferAligned(64, 6*4);
        IntBuffer idxBuf = buf.asIntBuffer();
        idxBuf.put(0);
        idxBuf.put(1);
        idxBuf.put(2);
        idxBuf.put(2);
        idxBuf.put(3);
        idxBuf.put(0);
        idxBuf.flip();
        this.vboIdx.upload(GL15.GL_ELEMENT_ARRAY_BUFFER, buf, idxBuf.limit()*4);
        Memory.free(buf);
		this.vertexBuf.reset();
		buildQuad(this.vertexBuf);
		int intsize = this.vertexBuf.storeVertexData(this.vertexUploadDirectBuf);
		this.vboStaticQuad.upload(GL15.GL_ARRAY_BUFFER, this.vertexUploadDirectBuf.getByteBuf(), intsize*4);
		ArrayList<AssetTexture> clouds = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
	        AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/sky/cloud"+i+".png", i>0);
			if (tex == null)
				break;
			clouds.add(tex);
		}
		this.texClouds = new int[clouds.size()];
		for (int i = 0; i < clouds.size(); i++) {
			this.texClouds[i] = TextureManager.getInstance().makeNewTexture(clouds.get(i), false, true, -1);
		}
		AssetTexture t = AssetManager.getInstance().loadPNGAsset("textures/tex10.png");
		this.texNoise = TextureManager.getInstance().makeNewTexture(t, true, true, 0);
		redraw();

		initShaders();

	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
