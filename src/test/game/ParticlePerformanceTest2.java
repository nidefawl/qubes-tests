package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.RenderAssets;
import nidefawl.qubes.async.AsyncTask;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.config.RenderSettings;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gui.LoadingScreen;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.models.*;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.vr.VR;
import test.game.LinkedList.Node;

public class ParticlePerformanceTest2 extends GameBase {
    public final static int MAX_PARTICLES       = 1024*1024;
	public final static int MAX_PARTICLES_PER_DRAW_CALL = 1024 * 64;

    public final static ShaderBuffer        ssbo_particle_cubes_persistent_pooled        = new ShaderBuffer("ParticleCube_buffer").setMakePersistantMapped(true)
            .setSize((4+4)*4*MAX_PARTICLES_PER_DRAW_CALL, 128);
    public final static ShaderBuffer        ssbo_particle_cubes2        = new ShaderBuffer("ParticleCube_buffer")
            .setSize((4+4)*4*MAX_PARTICLES_PER_DRAW_CALL, 0);
    
	static class Particle {
		boolean dead = false;
		int maxLive = 122;
		Vector3f mot, lastMot;
		Vector3f pos, lastPos;
		Vector3f renderPos;
		Vector3f renderRot;
		Vector3f rot, lastRot;
		Vector3f rotspeed;
		Vector2f texOffset;
		float size, initSize, lastSize, renderSize;
		int tick = 0;
		private int tex;
		private int normalMap;
		private int type = 1;
		private int pass;
		private int color;

		public Particle() {
			this.rotspeed = new Vector3f();
			this.renderRot = new Vector3f();
			this.rot = new Vector3f();
			this.lastRot = new Vector3f();
			this.renderPos = new Vector3f();
			this.pos = new Vector3f();
			this.lastPos = new Vector3f();
			this.mot = new Vector3f();
			this.lastMot = new Vector3f();
			this.texOffset = new Vector2f();
		}
		public void reset() {
			dead = false;
			tick = 0;
		}
		
		public void setTex(int tex) {
			this.tex = tex;
		}
		public void setType(int type) {
			this.type = type;
		}
		
		private void die() {
			this.dead = true;
		}

		public void setMotion(float x, float y, float z) {
			this.mot.set(x, y, z);
			this.lastMot.set(x, y, z);
		}

		public void setPos(float x, float y, float z) {
			this.pos.set(x, y, z);
			this.lastPos.set(x, y, z);
		}

		public void setRot(float x, float y, float z) {
			this.rot.set(x, y, z);
			this.lastRot.set(x, y, z);
		}

		public void setRotSpeed(float x, float y, float z) {
			this.rotspeed.set(x, y, z);
		}

		public void setSize(float size) {
			this.initSize = this.size = this.lastSize = this.renderSize = size;
		}

		public int store(int offset, FloatBuffer bufMatFloat, IntBuffer bufBlockInfo) {
			Color.setColorVec(this.color, tmp);
//			int structSize = 4;
//			bufMatFloat.position(offset*(structSize));
//			bufBlockInfo.position((offset*(structSize))+2);
//			bufMatFloat.put(Half.fromFloat(this.renderPos.y) << 16 | (Half.fromFloat(this.renderPos.x)));
//			bufMatFloat.put(Half.fromFloat(this.renderSize) << 16 | (Half.fromFloat(this.renderPos.z)));
//			bufBlockInfo.put(color);
//			bufBlockInfo.put(0);
//			int structSize = 8;
//			bufMatFloat.position(offset*(structSize));
//			bufBlockInfo.position(offset*(structSize)+0);

			bufMatFloat.put(this.renderPos.x);
			bufMatFloat.put(this.renderPos.y);
			bufMatFloat.put(this.renderPos.z);
			bufMatFloat.put(this.renderSize);
			bufMatFloat.put(tmp.x);
			bufMatFloat.put(tmp.y);
			bufMatFloat.put(tmp.z);
			bufMatFloat.put(0);

//			bufBlockInfo.position(offset*(structSize)+8);
//			bufBlockInfo.put(0);
//			bufBlockInfo.put(0);
//			bufBlockInfo.put(0);
//			bufBlockInfo.put(0);
			return 1;
		}
		public int store(int offset, VertexBuffer vertexBuffer) {
			vertexBuffer.put(Half.fromFloat(this.renderPos.y) << 16 | (Half.fromFloat(this.renderPos.x)));
			vertexBuffer.put(Half.fromFloat(this.renderSize) << 16 | (Half.fromFloat(this.renderPos.z)));
			vertexBuffer.put(0xFF000000|(Integer.reverseBytes(this.color)>>8)&0x00FFFFFF);
			vertexBuffer.put(0);
			return 1;
		}

		public void tick() {
			lastSize = size;
			lastRot.set(rot);
			lastMot.set(mot);
			lastPos.set(pos);
			if (size > 1.0E-4f) {
				pos.addVec(mot);
				rot.addVec(this.rotspeed);
				size *= 0.98f;
				rotspeed.scale(0.98f);
				mot.x *= 0.98f;
				mot.y *= 0.98f;
				mot.z *= 0.98f;
				mot.y -= 0.09f*SPEED*0.4f;
			}
			tick++;
			if (tick > maxLive) {
				die();
			}
		}

		public void update(float f) {
			Vector3f.interp(lastPos, pos, f, renderPos);
			Vector3f.interp(lastRot, rot, f, renderRot);
			renderSize = Math.max(1.0E-12F, lastSize + (size - lastSize) * f);
//			

		}

		public void setTextureOffset(float u, float v) {
			this.texOffset.set(u, v);
		}


	}
	
	final static int MAX_SPRITES = 1024*64;
	static SimpleResourceManager newshaders = new SimpleResourceManager();
	static SimpleResourceManager shaders = new SimpleResourceManager();
	private static boolean startup;
    final static Vector3f tmp = new Vector3f();
    static int VERT_LEN1 = 0;
    static int VERT_LEN2 = 0;
    
    public ParticlePerformanceTest2() {
		useWindowSizeAsRenderResolution = false;
		TICKS_PER_SEC = 20;
		Engine.znear = 0.1f;
		Engine.zfar = 512.0f;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new ParticlePerformanceTest2().startGame();
	}

    private FontRenderer font;
	
    final CameraController cameraController = new CameraController();
	public FrameBuffer  fbDeferred;
	private FrameBuffer sceneFB;
	
	Shader shaderDeferred;
	Shader skybox;
	Shader particleShaderSSBO;
	Shader particleShaderAttrBinding;
	private int vao;

	private GLTriBuffer cubeFormat1;
	private GLAttrBuffer faceAttrBuffer;
	

	boolean once = false;
	boolean hadContext = false;
	private int fireUpdate;
	private float lastUpdate;
	boolean pause=false;
    float pauseTime = 0;
    
	int totalSpritesRendered = 0;
	int storedSprites = 0;
	int tick = 0;

    private int renderMode;
	private int selDrawMode=1;
	private boolean updateBuffers = true;
	
	private String error;
	private String stats;

    

	private int maxSprites = MAX_PARTICLES/10;
	private int maxSpritesPerDraw = 1024*10;
	LinkedList<Particle> particlesAlive = new LinkedList<>();
	LinkedList<Particle> particlesDead = new LinkedList<>();


	Random r = new Random(4444);
	private Vec3D tmpPos = new Vec3D();

	private VertexBuffer bufferDataFace;
	private int drawCalls;
	private RenderSettings renderSettings = new RenderSettings();




	private static float SPEED= 0.2f;


	@Override
	public void initGame() {
		GameBase.loadingScreen = new LoadingScreen();
        Engine.init(windowWidth, windowHeight);
		TextureManager.getInstance().init();
        EntityModel.preInit();
        EntityModel.postInit();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
	}
	
	public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader particleShaderSSBO = assetMgr.loadShader(newshaders, "particle/cube_test", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("MAX_PARTICLES".equals(define)) {
                        return "#define MAX_PARTICLES "+MAX_PARTICLES_PER_DRAW_CALL;
                    }
                    if ("ATTR_MODE".equals(define)) {
                        return "#define ATTR_MODE ATTR_SSBO_STRUCT";
                    }
                    return null;
                }
            });
            Shader particleShaderAttrBinding = assetMgr.loadShader(newshaders, "particle/cube_test", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("MAX_PARTICLES".equals(define)) {
                        return "#define MAX_PARTICLES "+MAX_PARTICLES_PER_DRAW_CALL;
                    }
                    if ("ATTR_MODE".equals(define)) {
                        return "#define ATTR_MODE ATTR_VERTEX_ATTR";
                    }
                    return null;
                }
            });
            Shader new_deferred = assetMgr.loadShader(newshaders, "post/deferred", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("RENDER_PASS".equals(define)) {
                        return "#define RENDER_PASS 0";
                    }
                    return null;
                }
            });
            Shader skybox = assetMgr.loadShader(newshaders, "sky/skybox_generate");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            shaderDeferred = new_deferred;
            this.particleShaderSSBO = particleShaderSSBO;
            this.particleShaderAttrBinding = particleShaderAttrBinding;
            this.skybox = skybox;
            this.shaderDeferred.enable();
            shaderDeferred.setProgramUniform1i("texColor", 0);
            shaderDeferred.setProgramUniform1i("texNormals", 1);
            shaderDeferred.setProgramUniform1i("texMaterial", 2);
            shaderDeferred.setProgramUniform1i("texDepth", 3);
            shaderDeferred.setProgramUniform1i("texShadow", 4);
            shaderDeferred.setProgramUniform1i("texLight", 5);
            shaderDeferred.setProgramUniform1i("texBlockLight", 6);
            shaderDeferred.setProgramUniform1i("texAO", 7);
//            shaderDeferred.setProgramUniform1i("texWaterNoise", 8);
            shaderDeferred.setProgramUniform1i("texArrayNoise", 9);


            this.particleShaderSSBO.enable();
            this.particleShaderSSBO.setProgramUniform1i("blockTextures", 0);
            this.particleShaderSSBO.setProgramUniform1i("noisetex", 1);
            this.particleShaderSSBO.setProgramUniform1i("normalTextures", 2);
            this.particleShaderAttrBinding.enable();
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
	@Override
	public void lateInitGame() {

        RenderAssets.load(this.renderSettings, loadingScreen);
		
		this.font=FontRenderer.get(0, 22, 0);
		cubeFormat1 = new GLTriBuffer(GL15.GL_STREAM_DRAW);
		this.faceAttrBuffer = new GLAttrBuffer();
		this.bufferDataFace = new VertexBuffer(1024*1024);
		redraw();
		initShaders();

//        //POS
//        vertexAttribFormat(0, 3, GL30.GL_HALF_FLOAT, false, 2);
//        //NORMAL
//        vertexAttribFormat(1, 3, GL11.GL_BYTE, true, 1);
//        //TEXCOORD
//        vertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, 1);

		
		vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        
        int offset = 0;
        //POS
        GL20.glEnableVertexAttribArray(0);
        GL43.glVertexAttribFormat(0, 3, GL30.GL_HALF_FLOAT, false, offset * 4);
        GL43.glVertexAttribBinding(0, 0); // bind to first vertex buffer
        offset += 2; 
        //NORMAL
        GL20.glEnableVertexAttribArray(1);
        GL43.glVertexAttribFormat(1, 3, GL11.GL_BYTE, true, offset * 4);
        GL43.glVertexAttribBinding(1, 0); // bind to first vertex buffer
        offset += 1; 
        //TEXCOORD
        GL20.glEnableVertexAttribArray(2);
        GL43.glVertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, offset * 4);
        GL43.glVertexAttribBinding(2, 0); // bind to first vertex buffer
        offset += 1; 
        
        VERT_LEN1 = offset*4;

        offset = 0; 
        //POSITION + SCALE
        GL20.glEnableVertexAttribArray(3);
        GL43.glVertexAttribFormat(3, 4, GL30.GL_HALF_FLOAT, false, offset * 4);
        GL43.glVertexAttribBinding(3, 1); // bind to first vertex buffer
        offset += 2; 
        //COLOR
        GL20.glEnableVertexAttribArray(4);
        GL43.glVertexAttribFormat(4, 4, GL11.GL_UNSIGNED_BYTE, true, offset * 4);
        GL43.glVertexAttribBinding(4, 1); // bind to first vertex buffer
        GL43.glVertexBindingDivisor(1, 1);
        offset += 2; 
        VERT_LEN2 = offset*4;
        GL30.glBindVertexArray(0);
        Engine.checkGLError("glBindVertexArray");
	}
	
	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		if (action == GLFW.GLFW_PRESS) {
			switch (key) {
			case GLFW.GLFW_KEY_F1:
				toggleVR();
				break;
			case GLFW.GLFW_KEY_1:
				initShaders();
				break;
			case GLFW.GLFW_KEY_2:
				pause=!pause;
				pauseTime=lastUpdate;
				break;
			case GLFW.GLFW_KEY_3:
				renderMode=(renderMode+1)%4;
				break;
			case GLFW.GLFW_KEY_4:
				redraw();
				break;
			case GLFW.GLFW_KEY_5:
				updateBuffers=!updateBuffers;
				break;
			case GLFW.GLFW_KEY_6:
				selDrawMode = (selDrawMode+1)%3;
				break;
			case GLFW.GLFW_KEY_7:
				initShaders();
				break;
			}
		}
		if (action == GLFW.GLFW_REPEAT||action == GLFW.GLFW_PRESS) {
			float rotXIncr = 0;
			float rotYIncr = 0;
			float rotZIncr = 0;
			float scale = 1.0f;
			
			switch (key) {
			case GLFW.GLFW_KEY_KP_ADD:
				
				maxSprites+=Math.min(Math.max(maxSprites>>3, 1024), Math.max(32, (maxSprites>>3)));
				if (maxSprites > MAX_PARTICLES)
					maxSprites = MAX_PARTICLES;
				break;
			case GLFW.GLFW_KEY_KP_SUBTRACT:
				maxSprites-=Math.min(Math.max(maxSprites>>3, 1024), Math.max(32, (maxSprites>>3)));
				if (maxSprites < 1)
					maxSprites = 1;
				break;
			case GLFW.GLFW_KEY_PAGE_UP:
				if (maxSpritesPerDraw >= 1024)
					maxSpritesPerDraw+=1024;
				else 
					maxSpritesPerDraw<<=1;
				
				if (maxSpritesPerDraw > MAX_PARTICLES_PER_DRAW_CALL)
					maxSpritesPerDraw = MAX_PARTICLES_PER_DRAW_CALL;
				break;
			case GLFW.GLFW_KEY_PAGE_DOWN:
				if (maxSpritesPerDraw > 1024)
					maxSpritesPerDraw-=1024;
				else 
					maxSpritesPerDraw>>=1;
				if (maxSpritesPerDraw < 1)
					maxSpritesPerDraw = 1;
				break;
			case GLFW.GLFW_KEY_ENTER:
				spawnParticles(10);
				break;
			case GLFW.GLFW_KEY_T:
				SPEED+=0.1f;
				if (SPEED > 10f) SPEED = 10f;
				break;
			case GLFW.GLFW_KEY_G:
				SPEED-=0.1f;
				if (SPEED < 0.1f)
					SPEED = 0.1f;
				break;
			case GLFW.GLFW_KEY_U:
				rotXIncr+=GameMath.PI_OVER_180;
				break;
			case GLFW.GLFW_KEY_J:
				rotXIncr-=GameMath.PI_OVER_180;
				break;
			case GLFW.GLFW_KEY_I:
				rotYIncr+=GameMath.PI_OVER_180;
				break;
			case GLFW.GLFW_KEY_K:
				rotYIncr-=GameMath.PI_OVER_180;
				break;
			case GLFW.GLFW_KEY_O:
				rotZIncr+=GameMath.PI_OVER_180;
				break;
			case GLFW.GLFW_KEY_L:
				rotZIncr-=GameMath.PI_OVER_180;
				break;
			case GLFW.GLFW_KEY_R:
				scale = 0;
				break;
			}
			if (rotXIncr!=0||rotYIncr!=0||rotZIncr!=0||scale!=1.0f) {
				for (Particle p : particlesAlive) {
					p.rot.x+=rotXIncr;
					p.rot.y+=rotYIncr;
					p.rot.z+=rotZIncr;
					p.rot.scale(scale);
				}
				fireUpdate++;
			}
		}
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
			if (sceneFB != null) sceneFB.destroy();
			if (fbDeferred != null) fbDeferred.destroy();
	        sceneFB = new FrameBuffer(displayWidth, displayHeight);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGBA16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGBA16UI);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGBA16F);
	        sceneFB.setFilter(GL_COLOR_ATTACHMENT2, GL_NEAREST, GL_NEAREST);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
	        sceneFB.setHasDepthAttachment();
	        sceneFB.setup(null);
	        
	        
	        Engine.setSceneFB(sceneFB);
			FrameBuffer.unbindFramebuffer();
	        fbDeferred = FrameBuffer.make(null, displayWidth, displayHeight, GL_RGB16F);
        }
        glActiveTexture(GL_TEXTURE0);
	}
	@Override
	public void onStatsUpdated() {
		this.stats = String.format("%d FPS (%.2fms)", lastFPS, Stats.avgFrameTime);

		if (VR.getFB(0) != null) {

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
			tick = 2;
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
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}
	@Override
	public void postRenderUpdate(float f) {
		if (VR_SUPPORT) {
			VR.updatePose(f);

		}
	}

	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
		
        Engine.updateCamera();
        Engine.getSunLightModel().setTime(5850);
//        Engine.getSunLightModel().setTime(1700+(int)((ticksran+f)*32));
        Engine.getSunLightModel().updateFrame(f);
        Engine.setLightPosition(Engine.getSunLightModel().getLightPosition());
        UniformBuffer.updateUBO(null, f);
        if (!pause) {
            this.preRenderUpdateParticles(pause?pauseTime:(f<pauseTime?pauseTime:f));
        	pauseTime = 0;
        }
        lastUpdate = f;
	}
	void preRenderUpdateParticles(float ftime) {

		for (Particle p : particlesAlive) {
			p.update(ftime);
		}
		
	}
	private void redraw() {
		{
			VertexBuffer buf = new VertexBuffer(1024*1024);
	        RenderUtil.makeCube(buf, 1.0f, GLVAO.vaoStaticModel);
	        int data = cubeFormat1.upload(buf);
	        System.out.println("uploaded "+(data*4)+" bytes for format 1");
		}
	}
	@Override
	public void render(float fTime) {
		Engine.setBlend(false);
        
        glClearColor(0.71F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		setSceneViewport();
        for (int eye = 0; eye < (VR_SUPPORT ? 2 : 1); eye++) {
            if (VR_SUPPORT) {
                Engine.getMatSceneP().load(eye == 0 ? VR.cam.projLeft : VR.cam.projRight);
                Engine.getMatSceneP().update();

                Engine.setViewMatrix(VR.getViewMat(eye));
                UniformBuffer.updateUBO(null, fTime);
                
                VR.setViewPort(eye);
                Engine.checkGLError("setCameraAndViewport");
            }
			
			
			Engine.getSceneFB().bind();
			Engine.getSceneFB().clearFrameBuffer();
			Engine.enableDepthMask(false);
			skybox.enable();
			Engine.drawFullscreenQuad();
			Engine.enableDepthMask(true);
			glEnable(GL11.GL_DEPTH_TEST);

			renderParticles(fTime);
			glDisable(GL11.GL_DEPTH_TEST);
			
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

            FrameBuffer finalTarget = VR_SUPPORT ? VR.getFB(eye) : null;
            if (finalTarget == null) FrameBuffer.unbindFramebuffer();
            else {
                finalTarget.bind();
                finalTarget.clearFrameBuffer();
            }
	        
			Shaders.tonemap.enable();
			Shaders.tonemap.setProgramUniform1f("constexposure", 130);
			GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbDeferred.getTexture(0));
			
			if (renderMode >= 1) {

				Shaders.textured.enable();
				GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(renderMode));	
			}
			Engine.drawFullscreenQuad();
//            System.out.println("eye "+eye+" GL11.glGetBoolean(GL11.GL_CULL_FACE) "+GL11.glGetBoolean(GL11.GL_CULL_FACE));
//            System.out.println("eye "+eye+"GL11.glGetBoolean(GL11.GL_DEPTH_TEST) "+GL11.glGetBoolean(GL11.GL_DEPTH_TEST));

			if (VR_SUPPORT) {
				
				glEnable(GL11.GL_DEPTH_TEST);
				glDisable(GL11.GL_CULL_FACE);
	            Engine.setViewMatrixCameraPos(VR.getPoseMat(eye), Vector3f.ZERO);
	            UniformBuffer.updateUBO(null, fTime);
				VR.renderControllers();
				glEnable(GL11.GL_CULL_FACE);
				glDisable(GL11.GL_DEPTH_TEST);
			}
		
		}


        if (VR_SUPPORT) {

            FrameBuffer.unbindFramebuffer();
            VR.Submit();
            Engine.checkGLError("VR.Submit");
            setWindowViewport();
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("setGUIProjection");
            VR.drawFullscreenCompanion(windowWidth, windowHeight);
            Engine.checkGLError("drawFullscreenCompanion");
        }
		glClear(GL_DEPTH_BUFFER_BIT);
		Engine.setBlend(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        int hT = 260;
        int yT = Engine.displayHeight-hT;
		Shaders.colored.enable();
		Tess.instance.setColorF(0, 0.7f);
		Tess.instance.add(600, yT);
		Tess.instance.add(0, yT);
		Tess.instance.add(0, yT+hT);
		Tess.instance.add(600, yT+hT);
		Tess.instance.drawQuads();
		Shaders.textured.enable();
		int y = yT+5;
		this.font.drawString(this.stats, 10, y+=30, -1, true, 1.0f);
		y+=30;
		String nModeName = "";
		switch (selDrawMode) {
		case 0:
			nModeName="PersistentMappedPooled SSBO";
			break;
		case 1:
			nModeName="Reuse BufferData SSBO";
			break;
		case 2:
			nModeName="Vertex Attr Data";
			break;
		}
		nModeName+=" ("+selDrawMode+")";
		this.font.drawString(""+storedSprites+"/"+maxSprites+" cubes", 10, y+=30, -1, true, 1.0f, 0);
		this.font.drawString(maxSpritesPerDraw+" cubes per drawcall", 10, y+=30, -1, true, 1.0f, 0);
		this.font.drawString(drawCalls+" drawcalls per frame", 10, y+=30, -1, true, 1.0f, 0);
		this.font.drawString("Mode: "+nModeName, 10, y+=30, -1, true, 1.0f, 0);
		this.font.drawString("updateBuffers: "+updateBuffers, 10, y+=30, -1, true, 1.0f, 0);
		if (this.error != null) {
			this.font.drawString(this.error, Engine.displayWidth/2, 30, 0xff8989, true, 1.0f, 2);	
		}
		Engine.setBlend(false);
		// Engine.checkGLError("drawAll");
	}
	private void renderParticles(float f) {
		Engine.setBlend(false);


		Engine.setBlend(true);
//        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

//        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
//        for (int i = 0; i < 3; i++) {
//            GL40.glBlendFuncSeparatei(1+i, GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);
//        }
        if (selDrawMode <= 1) {
        	particleShaderSSBO.enable();
        } else {
        	particleShaderAttrBinding.enable();
        }
//      int nSprites = (int) GameMath.clamp(Math.round(this.totalSprites*(WEATHER*0.7f+0.3f)), 0, this.totalSprites);
        
		Engine.checkGLError("storeparticles");
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());

		Engine.checkGLError("bind texture");
		GLTriBuffer buffer = cubeFormat1;
		if (selDrawMode <= 1) {
			GLVAO vao = GLVAO.vaoStaticModel;
			Engine.bindVAO(vao);
	        Engine.bindBuffer(buffer.getVbo());
	        Engine.bindIndexBuffer(buffer.getVboIndices());
		} else {
			Engine.disableBindless();
	        GL30.glBindVertexArray(vao);
			Engine.bindBuffer(buffer.getVbo(), 0, VERT_LEN1);
			Engine.bindBuffer(faceAttrBuffer.getVbo(), 1, VERT_LEN2);
//			System.out.println("VERT_LEN2 "+VERT_LEN2);
//			System.out.println("bind faceAttrBuffer with size "+faceAttrBuffer.getVbo().getBufferSize(GL15.GL_ARRAY_BUFFER));
//			System.out.println("size/VERT_LEN2 "+(faceAttrBuffer.getVbo().getBufferSize(GL15.GL_ARRAY_BUFFER)/VERT_LEN2));
//			System.out.println("storedSprites "+(storedSprites));
	        Engine.bindIndexBuffer(buffer.getVboIndices());
	        GL43.glVertexBindingDivisor(3, 1);
	        GL43.glVertexBindingDivisor(4, 1);
//	        GL33.glVertexAttribDivisor(3, 1);
//	        GL33.glVertexAttribDivisor(4, 1);
//	        return ;
		}
//		Engine.getSceneFB().setDraw(0);
		Engine.checkGLError("predraw");
		if (updateBuffers) {
			int nDraw = 0;
			int nDrawCalls = (MAX_PARTICLES / maxSpritesPerDraw)+1;
			int nActualDrawCalls = 0;
			for (int i = 0; i < nDrawCalls; i++) {
				int n = storeParticles(f, i);
//				System.out.println(i+"/"+storedSprites+"/"+n);
				if (storedSprites > 0) {
					nActualDrawCalls++;
					GL32.glDrawElementsInstancedBaseVertex(GL11.GL_TRIANGLES, buffer.getTriCount()*3, GL11.GL_UNSIGNED_INT, 0, storedSprites, 0);
					nDraw+=storedSprites;
				}
				if (selDrawMode == 0) {
					ssbo_particle_cubes_persistent_pooled.sync();
				}
				if (n < 0) {
					break;
				}
			}
			drawCalls=nActualDrawCalls;
			storedSprites=nDraw;
		} else {
	        GL32.glDrawElementsInstancedBaseVertex(GL11.GL_TRIANGLES, buffer.getTriCount()*3, GL11.GL_UNSIGNED_INT, 0, storedSprites, 0);
		}

//		Engine.getSceneFB().setDrawAll();
//		GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0, this.storedSprites);
		Engine.checkGLError("draw");
        Engine.bindVAO(null);
        GL30.glBindVertexArray(0);
	}

	public void spawnParticles(int n) {
		if (ticksran<30)
			return;
		if (particlesAlive.size()+1>=maxSprites)
			return;
		float maxVelXZ = 1.3f*SPEED;
		float minVelY = 0.3f*SPEED;
		float maxVelY = 3.3f*SPEED;
//		 minVelY = 1.8f;
//		 maxVelY = 15.3f;
		float rotRange = 0.03f;
		for (int i = 0; i < n; i++) {
			Particle p = null;
			int firstNull = -1;
//			System.out.println("spawn "+particlesAlive.size());
			if (!this.particlesDead.isEmpty()) {
				p = this.particlesDead.removeFirst();
				p.reset();
			}
			if (p == null) {
				p = new Particle();
			}
			
			float mx = (float) (-maxVelXZ+2.0*maxVelXZ*r.nextFloat());
			float mz = (float) (-maxVelXZ+2.0*maxVelXZ*r.nextFloat());
			float my = (float) (minVelY+(maxVelY-minVelY)*r.nextFloat()*r.nextFloat());
			p.setMotion(mx, my, mz);
			p.setPos(55*0.1f, -32*0.1f, 0);
			p.pos.x+=p.mot.x*r.nextFloat()*0.3f;
			p.pos.y+=p.mot.y*r.nextFloat()*0.3f;
			p.pos.z+=p.mot.z*r.nextFloat()*0.3f;
			int size = r.nextInt(4);
			p.setSize(1F/4f+size/4F);
			int toffx = r.nextInt(8);
			int toffz = r.nextInt(8);
			p.color = Color.HSBtoRGB(r.nextFloat(), 0.8f, 0.8f);
			
			p.setTextureOffset(toffx/8F, toffz/8F);
			p.setRot(r.nextFloat(), r.nextFloat(), r.nextFloat());
			p.setRotSpeed(r.nextFloat()*rotRange, r.nextFloat()*rotRange, r.nextFloat()*rotRange);
			p.setTex(r.nextInt(TextureArrays.blockTextureArray.totalSlots));
			particlesAlive.add(p);
			if (particlesAlive.size()+1>=maxSprites)
				return;
		}
	}
	int storeParticles(float ftime, int nround) {
		if (selDrawMode == 0) {
			ssbo_particle_cubes_persistent_pooled.nextFrame();
		} else if (selDrawMode == 1) {
			ssbo_particle_cubes2.nextFrame();
		} else if (selDrawMode == 2) {
			bufferDataFace.reset();
		}
		storedSprites = 0;
		int offset=0;
		int start = nround*maxSpritesPerDraw;
		int end = start+maxSpritesPerDraw;
		int size = particlesAlive.size();
		Node<Particle> node = particlesAlive.node(start);
		for (int j = start; j < end && j < size; j++) {
			Particle cloud = node.item;
			if (selDrawMode == 0) {
				IntBuffer bufBlockInfo = ssbo_particle_cubes_persistent_pooled.getIntBuffer();
				FloatBuffer bufModelMat = ssbo_particle_cubes_persistent_pooled.getFloatBuffer();
				storedSprites+=cloud.store(offset, bufModelMat, bufBlockInfo);
			} else if (selDrawMode == 1) {
				IntBuffer bufBlockInfo = ssbo_particle_cubes2.getIntBuffer();
				FloatBuffer bufModelMat = ssbo_particle_cubes2.getFloatBuffer();
				storedSprites+=cloud.store(offset, bufModelMat, bufBlockInfo);
			} else {
				storedSprites+=cloud.store(offset, bufferDataFace);
			}
			offset++;
			node = node.next;
		}
		if (selDrawMode == 0) {
	        ssbo_particle_cubes_persistent_pooled.update();
		} else if (selDrawMode == 1) {
			ssbo_particle_cubes2.update();
		} else {
			faceAttrBuffer.upload(this.bufferDataFace);
		}
		int left = particlesAlive.size()-end;
		return left;
	}

	long s = 0l;
	long lAvg10=0L;
	private int spawnTicks;
	@Override
	public void tick() {
		if (s == 0L){
			s = System.currentTimeMillis();
			if (lAvg10==0l) {
				lAvg10 = TICKS_PER_SEC*10;
			}
		} else {
			long l = System.currentTimeMillis();
			long passed = l-s;
			lAvg10 = (lAvg10 + (passed*10))/2;
			s = l;
		}
//		System.out.println(lAvg10/10);
		if (!isStarting) {
	        if (VR_SUPPORT) VR.tick();
			if (VR_SUPPORT) {
				boolean b = true;
				if (b) {
					this.cameraController.tickUpdate();
				} else {
					this.cameraController.move();
				}
			} else {
				this.cameraController.tickUpdate();
			}
			if (!pause) {
				this.updateTickParticles();
//				if (this.particles.isEmpty()) {
//					spawnParticles(1);
//				}
				if (spawnTicks > 0) {
					spawnTicks--;
					if (spawnTicks>7||spawnTicks%2==0) {
						spawnParticles(maxSprites/15);

//						for (int a = 0; a < Math.max(1, Math.min(1230, maxSprites/100)); a++)
//							if (r.nextInt(120+maxSprites/1200)>1&&r.nextInt(maxSprites)>storedSprites) {
//								spawnParticles(1+r.nextInt(23));
//								if (this.particlesAlive.size()+1 >= this.maxSprites)
//									break;
//							}
					}
				}
				else if (this.particlesAlive.isEmpty()) {
					spawnTicks = 22;
				}
			} else if (fireUpdate>0) {
				fireUpdate=0;
	            this.preRenderUpdateParticles(pauseTime);
			}
		}
	}

	void updateTickParticles() {
		Iterator<Particle> it = this.particlesAlive.iterator();
		while (it.hasNext()) {
			Particle p = it.next();
			if (p.dead) {
				it.remove();
				particlesDead.add(p);
			} else {
				p.tick();
			}
		}
	}

}
