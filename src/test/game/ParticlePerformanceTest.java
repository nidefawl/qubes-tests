package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.*;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.async.AsyncTask;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gui.LoadingScreen;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.models.*;
import nidefawl.qubes.models.render.ModelConstants;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.texture.array.*;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.vr.VR;

public class ParticlePerformanceTest extends GameBase {
    public final static int MAX_PARTICLES       = 1024*8;
	
    public final static ShaderBuffer        ssbo_particle_cubes        = new ShaderBuffer("ParticleCube_mat_model")
            .setSize(ModelConstants.SIZE_OF_MAT4*MAX_PARTICLES);
    public final static ShaderBuffer        ssbo_particle_cubes_blockinfo = new ShaderBuffer("ParticleCube_blockinfo")
            .setSize(ModelConstants.SIZE_OF_VEC4*MAX_PARTICLES);
    public final static ShaderBuffer        ssbo_particle_cubes_persist        = new ShaderBuffer("ParticleCube_mat_model_persist")
            .setSize(ModelConstants.SIZE_OF_MAT4*MAX_PARTICLES).setMakePersistantMapped(true);
    public final static ShaderBuffer        ssbo_particle_cubes_blockinfo_persist = new ShaderBuffer("ParticleCube_blockinfo_persist")
            .setSize(ModelConstants.SIZE_OF_VEC4*MAX_PARTICLES).setMakePersistantMapped(true);
    public final static ShaderBuffer        ssbo_particle_structs = new ShaderBuffer("ParticleCube_data")
            .setSize((20*4)*MAX_PARTICLES);
    public final static ShaderBuffer        ssbo_particle_arrays = new ShaderBuffer("ParticleCube_data_arrays")
            .setSize((4+16*4)*MAX_PARTICLES);
    
	static class Particle {
		boolean dead = false;
		int maxLive = 60;
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
			BufferedMatrix mat = Engine.getTempMatrix();
			mat.setIdentity();
			mat.translate(this.renderPos);
			mat.rotate(this.renderRot.y*GameMath.PI*2.0f, 0.0f, 1.0f, 0.0f);
			mat.rotate(this.renderRot.x*GameMath.PI*2.0f, 1.0f, 0.0f, 0.0f);
			mat.rotate(this.renderRot.z*GameMath.PI*2.0f, 0.0f, 0.0f, 1.0f);
			mat.scale(this.renderSize);
			mat.store(bufMatFloat);
			int attr = this.tex | this.normalMap << 12 | this.type << 16 | this.pass << (16+12);
			bufBlockInfo.put(attr);
			bufBlockInfo.put(Float.floatToRawIntBits(this.initSize));
			bufBlockInfo.put(Float.floatToRawIntBits(this.texOffset.x));
			bufBlockInfo.put(Float.floatToRawIntBits(this.texOffset.y));
			return 1;
		}

		public int storeInterlacedStruct(int offset, ShaderBuffer ssboParticleData) {
			FloatBuffer floatBuf = ssboParticleData.getFloatBuffer();
			IntBuffer intBuf = ssboParticleData.getIntBuffer();
			int structSize = 20;
			floatBuf.position(ssboParticleData.offsetInt()+offset*(structSize));
			intBuf.position(ssboParticleData.offsetInt()+(offset*(structSize))+16);
			
			BufferedMatrix mat = Engine.getTempMatrix();
			mat.setIdentity();
			mat.translate(this.renderPos);
			mat.rotate(this.renderRot.y*GameMath.PI*2.0f, 0.0f, 1.0f, 0.0f);
			mat.rotate(this.renderRot.x*GameMath.PI*2.0f, 1.0f, 0.0f, 0.0f);
			mat.rotate(this.renderRot.z*GameMath.PI*2.0f, 0.0f, 0.0f, 1.0f);
			mat.scale(this.renderSize);
			mat.store(floatBuf);

			int attr = this.tex | this.normalMap << 12 | this.type << 16 | this.pass << (16+12);
			
			intBuf.put(attr);
			
			return 1;
		}
		public int storeArrays(int offset, ShaderBuffer ssboParticleData) {
			FloatBuffer floatBuf = ssboParticleData.getFloatBuffer();
			IntBuffer intBuf = ssboParticleData.getIntBuffer();
			floatBuf.position(ssboParticleData.offsetInt()+offset*(16));
			intBuf.position(ssboParticleData.offsetInt()+MAX_PARTICLES*16+offset*1);
			
			BufferedMatrix mat = Engine.getTempMatrix();
			mat.setIdentity();
			mat.translate(this.renderPos);
			mat.rotate(this.renderRot.y*GameMath.PI*2.0f, 0.0f, 1.0f, 0.0f);
			mat.rotate(this.renderRot.x*GameMath.PI*2.0f, 1.0f, 0.0f, 0.0f);
			mat.rotate(this.renderRot.z*GameMath.PI*2.0f, 0.0f, 0.0f, 1.0f);
			mat.scale(this.renderSize);
			mat.store(floatBuf);

			int attr = this.tex | this.normalMap << 12 | this.type << 16 | this.pass << (16+12);
			
			mat.store(floatBuf);
			intBuf.put(attr);
			return 1;
		}

		public void tick() {
			lastSize = size;
			lastRot.set(rot);
			lastMot.set(mot);
			lastPos.set(pos);
			pos.addVec(mot);
			rot.addVec(this.rotspeed);
			size *= 0.98f;
			rotspeed.scale(0.98f);
			mot.x *= 0.98f;
			mot.y *= 0.98f;
			mot.z *= 0.98f;
			mot.y -= 0.09f*SPEED*0.4f;
			tick++;
			if (tick > maxLive) {
				die();
			}
		}

		public void update(float f) {
			Vector3f.interp(lastPos, pos, f, renderPos);
			Vector3f.interp(lastRot, rot, f, renderRot);
			renderSize = lastSize + (size - lastSize) * f;

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
    public ParticlePerformanceTest() {
		useWindowSizeAsRenderResolution = false;
		TICKS_PER_SEC = 20;
		Engine.znear = 0.1f;
		Engine.zfar = 512.0f;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new ParticlePerformanceTest().startGame();
	}

    private FontRenderer font;
	
    final CameraController cameraController = new CameraController();
	public FrameBuffer  fbDeferred;
	private FrameBuffer sceneFB;
	
	private GLTriBuffer cubeFormat1;
	private GLTriBuffer cubeFormat2;
	Shader shaderDeferred;
	Shader skybox;
	Shader particleShaderStruct;
	Shader particleShaderSeperateBuffer;
	Shader particleShaderSeperateBufferPersist;
	Shader particleShaderArrays;
	
	private TesselatorState tessState;

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
	private int selFormat;
	private int selShader=0;
	
	private String error;
	private String stats;

    

    private int maxSprites=3*1024;
	List<Particle> particles = Lists.newArrayList();


	Random r = new Random(4444);
	private Vec3D tmpPos = new Vec3D();

	private static float SPEED= 0.2f;


	@Override
	public void initGame() {
		GameBase.loadingScreen = new LoadingScreen();
        Engine.init();
		TextureManager.getInstance().init();
        EntityModel.preInit();
        EntityModel.postInit();
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
	}
	
	public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader particleStruct = assetMgr.loadShader(newshaders, "particle/cube_performance_test_ssbo", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("PARTICLE_TYPE".equals(define)) {
                        return "#define PARTICLE_TYPE_BLOCK";
                    }
                    if ("MAX_PARTICLES".equals(define)) {
                        return "#define MAX_PARTICLES "+MAX_PARTICLES;
                    }
                    if ("USE_STRUCT_BUFFER".equals(define)) {
                        return "#define USE_STRUCT_BUFFER";
                    }
                    return null;
                }
            });
            Shader particleArrays = assetMgr.loadShader(newshaders, "particle/cube_performance_test_ssbo", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("PARTICLE_TYPE".equals(define)) {
                        return "#define PARTICLE_TYPE_BLOCK";
                    }
                    if ("MAX_PARTICLES".equals(define)) {
                        return "#define MAX_PARTICLES "+MAX_PARTICLES;
                    }
                    if ("USE_STRUCT_BUFFER".equals(define)) {
                        return "#define USE_ARRAYS_BUFFER";
                    }
                    return null;
                }
            });
            Shader particleSeperateBuffer = assetMgr.loadShader(newshaders, "particle/cube_performance_test_ssbo", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("PARTICLE_TYPE".equals(define)) {
                        return "#define PARTICLE_TYPE_BLOCK";
                    }
                    if ("MAX_PARTICLES".equals(define)) {
                        return "#define MAX_PARTICLES "+MAX_PARTICLES;
                    }
                    if ("USE_STRUCT_BUFFER".equals(define)) {
                        return null;
                    }
                    return null;
                }
            });
            Shader particleShaderSeperateBufferPersist = assetMgr.loadShader(newshaders, "particle/cube_performance_test_ssbo", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("PARTICLE_TYPE".equals(define)) {
                        return "#define PARTICLE_TYPE_BLOCK";
                    }
                    if ("MAX_PARTICLES".equals(define)) {
                        return "#define MAX_PARTICLES "+MAX_PARTICLES;
                    }
                    if ("USE_STRUCT_BUFFER".equals(define)) {
                        return "#define USE_PERSIST_BUFFER";
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
            Shader skybox = assetMgr.loadShader(newshaders, "sky/clouds");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            shaderDeferred = new_deferred;
            particleShaderStruct = particleStruct;
            particleShaderArrays = particleArrays;
            particleShaderSeperateBuffer = particleSeperateBuffer;
            this.particleShaderSeperateBufferPersist = particleShaderSeperateBufferPersist;
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

            particleShaderStruct.enable();
            particleShaderStruct.setProgramUniform1i("blockTextures", 0);
            particleShaderStruct.setProgramUniform1i("noisetex", 1);
            particleShaderStruct.setProgramUniform1i("normalTextures", 2);
            particleShaderSeperateBuffer.enable();
            particleShaderSeperateBuffer.setProgramUniform1i("blockTextures", 0);
            particleShaderSeperateBuffer.setProgramUniform1i("noisetex", 1);
            particleShaderSeperateBuffer.setProgramUniform1i("normalTextures", 2);
            particleShaderSeperateBufferPersist.enable();
            particleShaderSeperateBufferPersist.setProgramUniform1i("blockTextures", 0);
            particleShaderSeperateBufferPersist.setProgramUniform1i("noisetex", 1);
            particleShaderSeperateBufferPersist.setProgramUniform1i("normalTextures", 2);
            particleShaderArrays.enable();
            particleShaderArrays.setProgramUniform1i("blockTextures", 0);
            particleShaderArrays.setProgramUniform1i("noisetex", 1);
            particleShaderArrays.setProgramUniform1i("normalTextures", 2);
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
        loadingScreen.setProgress(0, 0.8f, "Loading... Item Models");
        ItemModelManager.getInstance().reload();
        loadingScreen.setProgress(0, 0.9f, "Loading... Block Models");
        BlockModelManager.getInstance().reload();
        loadingScreen.setProgress(0, 1f, "Loading... Entity Models");
        EntityModelManager.getInstance().reload();
        loadingScreen.setProgress(0, 1f, "Loading... Item Textures");
        TextureArray[] arrays = {
                ItemTextureArray.getInstance(),
                BlockNormalMapArray.getInstance(),
                BlockTextureArray.getInstance(),
        };
        for (int i = 0; i < arrays.length; i++) {
            final TextureArray arr = arrays[i];
            AsyncTasks.submit(new AsyncTask() {
                @Override
                public void pre() {
                    try {
                        arr.preUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void post() {
                    arr.postUpdate();
                }
                @Override
                public Void call() throws Exception {
                    try {
                        arr.load();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                @Override
                public TaskType getType() {
                    return TaskType.LOAD_TEXTURES;
                }
            });
        }
        while(!AsyncTasks.completeTasks()) {
            float pr = 0;
            for (int i = 0; i < arrays.length; i++) {
                pr+=arrays[i].getProgress();
            }
            pr/=(float)arrays.length;
            loadingScreen.setProgress(1, pr, "Loading...");
        }
		
		this.font=FontRenderer.get(0, 22, 0);
		cubeFormat1 = new GLTriBuffer(GL15.GL_STREAM_DRAW);
		cubeFormat2 = new GLTriBuffer(GL15.GL_STREAM_DRAW);
		redraw();
		

		initShaders();

		
		

		tessState = new TesselatorState(GL15.GL_STATIC_DRAW);
		int w = 1;
		int d = 2;
		Tess.instance.setColor(0xff00ff, 0xff);
		Tess.instance.setOffset(0, 0, 0);
		Tess.instance.add(0, 0, d);
		Tess.instance.add(0, w, d);
		Tess.instance.add(w, w, d);
		Tess.instance.add(w, 0, d);
		Tess.instance.add(0, w, d);
		Tess.instance.add(0, 0, d);
		Tess.instance.add(0, 0, d*2);
		Tess.instance.add(0, w, d*2);
		Tess.instance.add(w, w, d);
		Tess.instance.add(0, w, d);
		Tess.instance.add(0, w, d*2);
		Tess.instance.add(w, w, d*2);
		Tess.instance.setColor(0xff0000, 0xff);
		Tess.instance.add(0, w, -d);
		Tess.instance.add(0, 0, -d);
		Tess.instance.add(w, 0, -d);
		Tess.instance.add(w, w, -d);
		Tess.instance.add(0, 0, -d);
		Tess.instance.add(0, w, -d);
		Tess.instance.add(0, w, -d*2);
		Tess.instance.add(0, 0, -d*2);
		Tess.instance.add(0, w, -d);
		Tess.instance.add(w, w, -d);
		Tess.instance.add(w, w, -d*2);
		Tess.instance.add(0, w, -d*2);
		Tess.instance.setColor(0x00ffff, 0xff);
		Tess.instance.add(-4*w, 0, -d*4);
		Tess.instance.add(-4*w, 0, d*4);
		Tess.instance.add(4*w, 0, d*4);
		Tess.instance.add(4*w, 0, -d*4);
		Tess.instance.draw(GL_QUADS, this.tessState);
		
		
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
				selFormat ^= 1;
				break;
			case GLFW.GLFW_KEY_6:
				selShader = (selShader+1)%2;
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
				
				maxSprites+=Math.min(1024, Math.max(32, (maxSprites>>3)));
				if (maxSprites > MAX_PARTICLES)
					maxSprites = MAX_PARTICLES;
				break;
			case GLFW.GLFW_KEY_KP_SUBTRACT:
				maxSprites-=Math.min(1024, Math.max(32, (maxSprites>>3)));
				if (maxSprites < 1)
					maxSprites = 1;
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
				for (int a = 0; a < this.particles.size(); a++) {
					this.particles.get(a).rot.x+=rotXIncr;
					this.particles.get(a).rot.y+=rotYIncr;
					this.particles.get(a).rot.z+=rotZIncr;
					this.particles.get(a).rot.scale(scale);
				}
				fireUpdate++;
			}
		}
	}
	@Override
	public void onWindowResize(int displayWidth, int displayHeight) {
        if (!VR_SUPPORT||isStarting) {
            Game.displayWidth=displayWidth;
            Game.displayHeight=displayHeight;
            setRenderResolution(displayWidth, displayHeight);
        }
	}
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
			if (sceneFB != null) sceneFB.release();
			if (fbDeferred != null) fbDeferred.release();
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
            		displayWidth, displayHeight, 
            		windowWidth, windowHeight, 
            		Engine.getSceneFB().getWidth(), Engine.getSceneFB().getHeight(), 
            		VR.getFB(0).getWidth(), VR.getFB(0).getHeight(), 
            		guiWidth, guiHeight);
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
        if (VR_SUPPORT) {
            this.cameraController.updateVR();
        } else {
            this.cameraController.update(movement);
        }
        Vector3f renderPos = this.cameraController.getRenderPos(f);
        Engine.camera.setPosition(renderPos);
        if (!VR_SUPPORT) {
            Engine.camera.setOrientation(this.cameraController.yaw, this.cameraController.pitch, false, 4.0f);   
        }
		
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
		
		for (int i = 0; i < particles.size(); i++) {
			Particle cloud = particles.get(i);
			cloud.update(ftime);
		}
		
	}
	private void redraw() {
		VertexBuffer buf = new VertexBuffer(1024*1024);
        RenderUtil.makeCube(buf, 1.0f, GLVAO.vaoStaticModel);
        int data = cubeFormat1.upload(buf);
        System.out.println("uploaded "+(data*4)+" bytes for format 1");
		VertexBuffer buf2 = new VertexBuffer(1024*1024);
        RenderUtil.makeCube(buf2, 1.0f, GLVAO.vaoModel);
        int data2 = cubeFormat2.upload(buf2);
        System.out.println("uploaded "+(data2*4)+" bytes for format 2");
	}
	@Override
	public void render(float fTime) {
		Engine.setBlend(false);
        
        glClearColor(0.71F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Engine.setDefaultViewport();
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
			if (selShader==1) {
//				GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
			}
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
				VR.renderControllers();
				glEnable(GL11.GL_CULL_FACE);
				glDisable(GL11.GL_DEPTH_TEST);
			}
		
		}


        if (VR_SUPPORT) {

            FrameBuffer.unbindFramebuffer();
            VR.Submit();
            Engine.checkGLError("VR.Submit");
            Game.displayWidth=windowWidth;
            Game.displayHeight=windowHeight;
            updateProjection();
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("setGUIProjection");
            VR.drawFullscreenCompanion(windowWidth, windowHeight);
            Engine.checkGLError("drawFullscreenCompanion");
        }
		glClear(GL_DEPTH_BUFFER_BIT);
		Engine.setBlend(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        int yT = 400;
        int hT = displayHeight-yT;
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
		this.font.drawString(""+storedSprites+"/"+maxSprites+" sprites", 10, y+=30, -1, true, 1.0f, 0);
		this.font.drawString("selected shader: "+selShader, 10, y+=30, -1, true, 1.0f, 0);	
		this.font.drawString("selected format: "+selFormat, 10, y+=30, -1, true, 1.0f, 0);	
		if (this.error != null) {
			this.font.drawString(this.error, Game.displayWidth/2, 30, 0xff8989, true, 1.0f, 2);	
		}
		Engine.setBlend(false);
		// Engine.checkGLError("drawAll");
        if (VR_SUPPORT) {
        	setVRViewport();
        }
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
        if (selShader == 0) {
        	particleShaderSeperateBuffer.enable();
        } else if (selShader == 1) {
        	particleShaderSeperateBufferPersist.enable();
        } else if (selShader == 2) {
        	particleShaderStruct.enable();
        } else {
        	particleShaderArrays.enable();
        }
//      int nSprites = (int) GameMath.clamp(Math.round(this.totalSprites*(WEATHER*0.7f+0.3f)), 0, this.totalSprites);
        
		storeParticles(f, 0);
		Engine.checkGLError("storeparticles");
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());

		Engine.checkGLError("bind texture");
		GLVAO vao = selFormat == 0 ? GLVAO.vaoStaticModel : GLVAO.vaoModel;
		GLTriBuffer buffer = selFormat == 0 ? cubeFormat1 : cubeFormat2;
		Engine.bindVAO(vao);
        Engine.bindBuffer(buffer.getVbo());
        Engine.bindIndexBuffer(buffer.getVboIndices());
        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, buffer.getTriCount()*3, GL11.GL_UNSIGNED_INT, 0, storedSprites);

//		GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0, this.storedSprites);
		Engine.checkGLError("draw");
        Engine.bindVAO(null);
	}

	public void spawnParticles(int n) {
		if (ticksran<30)
			return;
		if (particles.size()+1>=MAX_PARTICLES)
			return;
		float maxVelXZ = 1.3f*SPEED;
		float minVelY = 0.3f*SPEED;
		float maxVelY = 3.3f*SPEED;
//		 minVelY = 1.8f;
//		 maxVelY = 15.3f;
		float rotRange = 0.03f;
		for (int i = 0; i < n; i++) {
			
			Particle p = new Particle();
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
			
			p.setTextureOffset(toffx/8F, toffz/8F);
			p.setRot(r.nextFloat(), r.nextFloat(), r.nextFloat());
			p.setRotSpeed(r.nextFloat()*rotRange, r.nextFloat()*rotRange, r.nextFloat()*rotRange);
			p.setTex(r.nextInt(BlockTextureArray.getInstance().totalSlots));
			particles.add(p);
			if (particles.size()+1>=MAX_PARTICLES)
				return;
		}
	}
	
	void storeParticles(float ftime, int n) {
		if (selShader == 0) {
			ssbo_particle_cubes.nextFrame();
			ssbo_particle_cubes_blockinfo.nextFrame();
		} else if (selShader == 1) {
			ssbo_particle_cubes_persist.nextFrame();
			ssbo_particle_cubes_blockinfo_persist.nextFrame();
		}  if (selShader == 2) {
			ssbo_particle_structs.clearBuffers();
		} else {
			ssbo_particle_arrays.clearBuffers();
		}
		storedSprites = 0;
		int offset=0;
		for (int i = 0; i < particles.size(); i++) {
			if (i >= MAX_PARTICLES) {
				if (Stats.fpsCounter%10==0)
				System.err.println("too many particles "+i);
				break;
			}
			Particle cloud = particles.get(i);
			if (selShader == 0) {
				IntBuffer bufBlockInfo = ssbo_particle_cubes_blockinfo.getIntBuffer();
				FloatBuffer bufModelMat = ssbo_particle_cubes.getFloatBuffer();
				storedSprites+=cloud.store(offset, bufModelMat, bufBlockInfo);
			} else if (selShader == 1) {
				IntBuffer bufBlockInfo = ssbo_particle_cubes_blockinfo_persist.getIntBuffer();
				FloatBuffer bufModelMat = ssbo_particle_cubes_persist.getFloatBuffer();
				storedSprites+=cloud.store(offset, bufModelMat, bufBlockInfo);
			} else if (selShader == 2) {
				storedSprites+=cloud.storeInterlacedStruct(offset, ssbo_particle_structs);
			} else {
				storedSprites+=cloud.storeArrays(offset, ssbo_particle_arrays);
			}
			offset++;
		}
		if (selShader == 0) {
	        ssbo_particle_cubes.update();
	        ssbo_particle_cubes_blockinfo.update();
		} else if (selShader == 1) {
			ssbo_particle_cubes_persist.update();
	        ssbo_particle_cubes_blockinfo_persist.update();
		} else if (selShader == 2) {
			ssbo_particle_structs.getBuf().flip();
			ssbo_particle_structs.update();
		} else {
			ssbo_particle_arrays.getBuf().flip();
			ssbo_particle_arrays.update();
		}
		
	}

	@Override
	public void tick() {
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
				for (int a = 0; a < Math.max(1, Math.min(130, maxSprites/100)); a++)
				if (r.nextInt(120)>1&&r.nextInt(maxSprites)>storedSprites) {
					spawnParticles(1+r.nextInt(23));
				}
			} else if (fireUpdate>0) {
				fireUpdate=0;
	            this.preRenderUpdateParticles(pauseTime);
			}
		}
	}

	void updateTickParticles() {
		for (int i = 0; i < particles.size(); i++) {
			Particle sprite = particles.get(i);
			sprite.tick();
			if (sprite.dead) {
				particles.remove(i--);
			}
		}
	}

}
