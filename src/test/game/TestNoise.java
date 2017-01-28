package test.game;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import com.google.common.collect.Lists;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.*;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.texture.array.TextureArray;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.vr.VR;

public class TestNoise extends GameBase {
	static SimpleResourceManager newshaders = new SimpleResourceManager();
	static SimpleResourceManager shaders = new SimpleResourceManager();
	private static boolean startup;
	public TestNoise() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
		TICKS_PER_SEC = 20;
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestNoise().startGame();
	}

	final static int TEX_SIZE = 64;
	private String error;
	private String stats;
	final CameraController cameraController = new CameraController();
	private FrameBuffer buf;
	private FrameBuffer sceneFB;
	private TesselatorState tessState;
	int reloadtick=40;

	@Override
	public void onStatsUpdated() {
		this.stats = String.format("%d FPS (%.2fms)", lastFPS, Stats.avgFrameTime);


        String s = String.format("%s - Display %dx%d - Window %dx%d - Gui %dx%d", 
        		this.stats, 
        		displayWidth, displayHeight, 
        		windowWidth, windowHeight, 
        		guiWidth, guiHeight);
        setTitle(s);
		reloadtick--;
		if (reloadtick <= 0) {
			initShaders();
//			Shaders.initShaders();
			reloadtick = 4;
		}
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
	}

	Random rand = new Random(0x3737);
	int frame = 0;
	@Override
	public void render(float f) {
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
		Shaders.colored3D.enable();
		tessState.drawQuads();
		FrameBuffer.unbindFramebuffer();
		HBAOPlus.renderAO();

        glClearColor(0.11F, 0.82F, 1.00F, 1F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.textured.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, buf.getTexture(0));
        Engine.drawFullscreenQuad();
        this.shader.enable();
        this.shader.setProgramUniform1i("texSlot", (frame++%arr.getNumTextures()));
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D_ARRAY, arr.glid);
		Engine.drawFullscreenQuad();
//        String[] stack = HBAOPlus.getCallStack();
//        for (int i = 0; i < stack.length; i++) {
//        	System.out.println(stack[i]);
//        }
//        System.exit(1);;
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
	
	boolean hadContext = false;
	private TextureArray arr;
	private Shader shader;
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
        	if (hadContext) {
                Engine.checkGLError("pre GLNativeLib.deleteContext");
        		HBAOPlus.deleteContext();
                Engine.checkGLError("post GLNativeLib.deleteContext");
        	}
            Engine.checkGLError("pre GLNativeLib.createContext");
    		HBAOPlus.createContext(displayWidth, displayHeight, GameBase.baseInstance.caps);
            Engine.checkGLError("post GLNativeLib.createContext");
            Engine.resize(displayWidth, displayHeight);
			if (buf != null) buf.release();
			if (sceneFB != null) sceneFB.release();
			buf = new FrameBuffer(displayWidth, displayHeight);
			buf.setColorAtt(GL_COLOR_ATTACHMENT0, GL11.GL_RGBA);
			buf.setColorTexExtFmt(GL11.GL_RGBA);
			buf.setColorTexExtType(GL11.GL_UNSIGNED_BYTE);
			buf.setClearColor(GL_COLOR_ATTACHMENT0, 1, 1, 1, 1);
			buf.setFilter(GL_COLOR_ATTACHMENT0, GL11.GL_NEAREST, GL11.GL_NEAREST);
			buf.setup(null);
	        sceneFB = new FrameBuffer(displayWidth, displayHeight);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB16F);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGBA16UI);
	        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB16F);
	        sceneFB.setFilter(GL_COLOR_ATTACHMENT2, GL_NEAREST, GL_NEAREST);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
	        sceneFB.setHasDepthAttachment();
	        sceneFB.setup(null);
	        Engine.setSceneFB(sceneFB);
	        HBAOPlus.setDepthTex(Engine.getSceneFB().getDepthTex());
	        HBAOPlus.setNormalTex(Engine.getSceneFB().getTexture(1));
	        long ptr = MemoryUtil.memAddress(Engine.getMatSceneP().get());
	        HBAOPlus.setProjMatrix(ptr);
	        HBAOPlus.setOutputFBO(buf.getFB());
	        HBAOPlus.setRadius(1);
	        HBAOPlus.setBias(0.2f);
	        HBAOPlus.setCoarseAO(1.2f);
	        HBAOPlus.setBlur(true, 8, 16.0f);
	        HBAOPlus.setBlurSharpen(false, 16, 0, 0);
	        HBAOPlus.setDetailAO(1f);
	        HBAOPlus.setPowerExponent(1);
	        HBAOPlus.setDepthThreshold(false, 220, 0.5f);
	        buf.bind();
	        buf.clearFrameBuffer();
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

	@Override
	public void lateInitGame() {
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
		this.arr = new TextureArray(64) {
			
			@Override
			protected void uploadTextures() {
		        int nBlock = 0;
		        int totalTex = this.blockIDToAssetList.size();
		        ByteBuffer directBuf = null;
		        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = blockIDToAssetList.entrySet().iterator();
		        while (it.hasNext()) {
		            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
	                AssetTexture tex = entry.getValue().get(0);
//                    System.out.println("put data with dim "+tex.getWidth()+"x"+tex.getHeight()+" in tex slot "+slot+" with size "+this.tileSize+"x"+this.tileSize);
                    directBuf = put(directBuf, tex.getData());
                    System.out.println("put in slot "+entry.getKey()+": "+tex.getWidth()+","+tex.getHeight()+", "+tex.getComponents()+" channels, expected: "+this.tileSize+"px");
                    GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,                     //Mipmap number
                          0, 0, entry.getKey(),                 //xoffset, yoffset, zoffset
                          this.tileSize, this.tileSize, 1,                 //width, height, depth
                          GL_RGBA,                //format
                          GL_UNSIGNED_BYTE,      //type
                          directBuf);                //pointer to data
                    Engine.checkGLError("GL12.glTexSubImage3D");
		            uploadprogress = ++nBlock/(float)totalTex;
		        }
		    }
			
			@Override
			protected void postUpload() {
	            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
	            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
	            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL12.GL_TEXTURE_MAX_LEVEL, 0);
		        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
			}
		    @Override
		    public void load() {
		        super.load();
		        this.numMipmaps = 1;
		    }
			
			@Override
			protected void collectTextures(AssetManager mgr) {
		        int len = 64;
		        for (int i = 0; i < len; i++) {
		        	String path = "textures/noise/LDR_RGBA_"+i+".png";
		        	AssetTexture tex = mgr.loadPNGAsset(path, false);
	                blockIDToAssetList.put(i, Lists.newArrayList(tex));
	                texNameToAssetMap.put(path, tex);
		            loadprogress = (i / (float) len);
		        }
			}
		};

		arr.reload();
		initShaders();
	}

	
	public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader draw_tex_array = assetMgr.loadShader(newshaders, "debug/draw_tex_array");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            this.shader = draw_tex_array;
            this.shader.enable();
            this.shader.setProgramUniform1i("texArray", 0);
            this.shader.setProgramUniform1i("texSlot", 0);
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
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}



}
