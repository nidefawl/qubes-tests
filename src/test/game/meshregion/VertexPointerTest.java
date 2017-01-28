package test.game.meshregion;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.lwjgl.opengl.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.*;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.*;
import nidefawl.qubes.texture.array.TextureArray;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import test.game.*;

public class VertexPointerTest extends GameBase {
    static final int REGION_DIST = 24;
	final CameraController cameraController = new CameraController();
	private FrameBuffer sceneFB;
    public FrameBuffer  fbDeferred;
	boolean hadContext = false;
    boolean once = false;
    Vector3f tmp = new Vector3f();
	public Vector3f skyColor = new Vector3f(0.43F, .69F, 1.F);
	// public Vector3f fogColor = new Vector3f(0.7F, 0.82F, 1F);
	public Vector3f fogColor = new Vector3f(0.7F, 0.82F, 1F);

	public VertexPointerTest() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
		TICKS_PER_SEC = 20;
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new VertexPointerTest().startGame();
	}
	

	int tick = 10;
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	private static boolean startup;



	Shader modelShader;
	Shader terrainShader;
	Shader shaderDeferred;
    public Shader       skyShader;
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_model_shader = assetMgr.loadShader(newshaders, "model/model_viewer");
            Shader new_deferred = assetMgr.loadShader(newshaders, "post/deferred", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("RENDER_PASS".equals(define)) {
                        return "#define RENDER_PASS 0";
                    }
                    return null;
                }
            });
            Shader new_terr_shader = assetMgr.loadShader(newshaders, "terrain/terrain", new IShaderDef() {
				
				@Override
				public String getDefinition(String define) {
                    if ("TEST_RENDER".equals(define)) {
                        return "#define TEST_RENDER 1";
                    }
					return null;
				}
			});
            
            Shader sky = assetMgr.loadShader(newshaders, "sky/sky");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            terrainShader = new_terr_shader;
            shaderDeferred = new_deferred;
            skyShader = sky;
            this.terrainShader.enable();
            this.terrainShader.setProgramUniform1i("blockTextures", 0);
//            this.terrainShader.setProgramUniform1i("noisetex", 1);
            this.terrainShader.setProgramUniform1i("normalTextures", 2);
            this.shaderDeferred.enable();
            shaderDeferred.setProgramUniform1i("texColor", 0);
            shaderDeferred.setProgramUniform1i("texNormals", 1);
            shaderDeferred.setProgramUniform1i("texMaterial", 2);
            shaderDeferred.setProgramUniform1i("texDepth", 3);
            shaderDeferred.setProgramUniform1i("texShadow", 4);
            shaderDeferred.setProgramUniform1i("texLight", 5);
            shaderDeferred.setProgramUniform1i("texBlockLight", 6);
            shaderDeferred.setProgramUniform1i("texAO", 7);
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
	private String stats;
	@Override
	public void onStatsUpdated() {
		ArrayList<String> n = this.glProfileResults;
		if (!n.isEmpty() && n.size()> 1) {
			String s = "";
			for (String s2 : n) {
				s+=s2+"\n";
			}
			this.stats=String.format("%d FPS (%.2fms)\n", lastFPS, Stats.avgFrameTime);
			this.stats+="\n"+s;
		}
		tick--;
		if (tick <= 0) {
//			String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")";
//			System.out.println(stats);
//			initShaders();
//			Shaders.initShaders();
			tick = 10;
//
//	        redraw();
//	        setTitle(stats);
//			try {
//				once = false;
//			} catch (Exception e) {
//				e.printStackTrace();
//				System.err.println("FAIL");
//			}
//			for (String s : glProfileResults) {
//				System.out.println(s);
//			}
		}
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
	}

	@Override
	public void render(float f) {
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("render");
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
	    Engine.setBlend(false);
	    
	    Engine.enableDepthMask(false);
	    skyShader.enable();
	    skybox1.bindAndDraw(GL_QUAD_STRIP);
	    skybox2.bindAndDraw(GL_QUADS);
	    if (GL_ERROR_CHECKS)
	        Engine.checkGLError("skyShader.drawSkybox");
	    Shader.disable();
	    Engine.enableDepthMask(true);
		Shaders.colored3D.enable();
//		tessState.drawQuads();
//		modelShader.enable();
//		modelShader.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
//		glPointSize(4.0f);
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, TMgr.getEmptyWhite());
//		Engine.pxStack.push();
		Engine.setBlend(false);
		int k = 20;
		int r = 2;
//        this.buf.bind();
//        MeshedRegion.enableVertexPtrs(5);
//		for (int i = -k; i <= k; i++) {
//			for (int j = -k; j <= k; j++) {
//				tmp.set(i*r, 0, j*r);
//				int nn = Engine.camFrustum.sphereInFrustum(tmp, 2);
//				if (nn > -1) {
//					Engine.pxStack.setTranslation(i*r, 0, j*r);	
////					tessState.drawQuads();
//			        this.buf.drawElements();
//				} else{
//					System.out.println("out "+nn);
//				}
//						
//
//			}
//		}
//        this.buf.unbind();
		GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D_ARRAY, arr.glid);
		

		Engine.pxStack.push();
		terrainShader.enable();
        Engine.checkGLError("pre draw");
        int iLen = this.lists.length;
        float fI = iLen;
        int mode = ((int)(((ticksran+f)/90f)%fI))%iLen;
        MeshList list = this.lists[mode];
        String renderMode = list.getName();
        GPUProfiler.start(renderMode);
//        list.bindVAO();
        list.draw();
        Engine.checkGLError(renderMode+" list.draw");
        GPUProfiler.end();
        Engine.bindVAO(null);
		Engine.pxStack.pop();

		FrameBuffer.unbindFramebuffer();
		String name = "Pass0";
        GLDebugTextures.readTexture(false, name, "texColor", Engine.getSceneFB().getTexture(0));
        GLDebugTextures.readTexture(false, name, "texNormals", Engine.getSceneFB().getTexture(1));
        GLDebugTextures.readTexture(false, name, "texMaterial", Engine.getSceneFB().getTexture(2));
        GLDebugTextures.readTexture(false, name, "blocklight", Engine.getSceneFB().getTexture(3));
        GLDebugTextures.readTexture(false, name, "texDepth", Engine.getSceneFB().getDepthTex(), 2);
        Engine.checkGLError("Pass0");
        fbDeferred.bind();
        fbDeferred.clearFrameBuffer();
        shaderDeferred.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getEmptyWhite()); //SHADOW
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, TMgr.getEmpty()); //LIGHTCOMPUTE
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3));
        GL.bindTexture(GL_TEXTURE7, GL_TEXTURE_2D, TMgr.getEmptyWhite()); //SSAO
        Engine.drawFullscreenQuad();
		FrameBuffer.unbindFramebuffer();
        GLDebugTextures.readTexture(true, "Deferred", "Output", fbDeferred.getTexture(0));
        glClearColor(0,0,0,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.tonemap.enable();
        Shaders.tonemap.setProgramUniform1f("constexposure", 70);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbDeferred.getTexture(0));
        Engine.drawFullscreenQuad();
        FrameBuffer.unbindFramebuffer();
        glClear(GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.wireframe.enable();
        Shaders.wireframe.setProgramUniform1i("num_vertex", 4);
        Shaders.wireframe.setProgramUniform1f("thickness", 0.2f);
        Shaders.wireframe.setProgramUniform1f("maxDistance", 110);
        Shaders.wireframe.setProgramUniform4f("linecolor", 1, 0.2f, 0.2f, 1);
//
//        String renderMode = "";
//        list.bindVAO();
        list.draw();
        Engine.bindVAO(null);
        GLDebugTextures.drawAll(displayWidth, displayHeight);
        Engine.checkGLError("drawAll");
        GPUProfiler.end();
	    Engine.setBlend(true);

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		Shaders.colored.enable();
		Tess.instance.setColorF(0, 0.7f);
		Tess.instance.add(400, 440);
		Tess.instance.add(0, 440);
		Tess.instance.add(0, 880);
		Tess.instance.add(400, 880);
		Tess.instance.drawQuads();
        Shaders.textured.enable();
		this.font.drawString(renderMode, 0, 470, -1, true, 1.0f);
		this.font.drawString(this.stats, 0, 520, -1, true, 1.0f);
//		try {
////			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	private Vec3D tmpPos = new Vec3D();
	private boolean reverse;
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
			if (fbDeferred != null) fbDeferred.release();
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
	MeshList listIntNV_Half = new MeshListInterleavedNV_Half();
	MeshList listIntNVBuf = new MeshListInterleavedNV(true);
	MeshList listIntNV = new MeshListInterleavedNV(false);
	MeshList listInt = new MeshListInterleaved();
	MeshList listSep = new MeshListSeperate();
	MeshList[] lists = {listIntNV, listIntNVBuf, listIntNV_Half, listInt, listSep};
	public void redraw() {
        Engine.checkGLError("redraw");
//		System.exit(0);
		int k =4;
		int r = 1;
		int n = 0;
        int rOffset = (k*2+1);
		for (int x = -REGION_DIST; x <= REGION_DIST; x++) {
			for (int z = -REGION_DIST; z <= REGION_DIST; z++) {
				int rX = x*rOffset*r*1;
				int rZ = z*rOffset*r*1;
		        for (int m = 0; m < lists.length; m++) {
		        	lists[m].reset();
		        }
				for (int i = -k; i <= k; i++) {
					for (int j = -k; j <= k; j++) {
						drawFace(rX+i*r, 0, rZ+j*r, n++%9);
				        for (int m = 0; m < lists.length; m++) {
				        	lists[m].addFace(attr);
				        }
					}
				}
		        for (int m = 0; m < lists.length; m++) {
		        	lists[m].upload(x, z);
//		        	System.err.println("upload");
//		            Engine.checkGLError("post upload");
		        }
			}
		}
	}

	private void drawFace(float i, float j, float k, int tex) {
		int w = 1;
		int d = 2;
		int faceDir = Dir.DIR_POS_Y;
		attr.setOffset(i, j, k);
		attr.setNormal(0, 1, 0);
		attr.setTex(tex);
		attr.setFaceDir(faceDir);
		attr.setNormalMap(0);
        attr.setRoughness(0.1f);
		attr.setReverse(false);
		attr.setAO(BlockFaceAttr.maskAO(2, 2, 2, 2));
		int br = 0<<4|15;
		int br2 = 0<<4|0;
        int brPP = BlockFaceAttr.mix_light(br, br, br, br);
        int brNP = BlockFaceAttr.mix_light(br, br, br, br);
        int brNN = BlockFaceAttr.mix_light(br, br, br, br);
        int brPN = BlockFaceAttr.mix_light(br2, br2, br2, br2);
        attr.maskLight(brNN, brPN, brPP, brNP, 0);
		attr.setType(1);
		int rgb = -1;
		float alpha = 1;
		attr.v0.setUV(0, 0);
		attr.v0.setColorRGBA(rgb, alpha);
		attr.v0.setPos(0, 0, 0);
		attr.v0.setFaceVertDir(-1);
		attr.v0.setDirection(faceDir, 0, false);
		attr.v1.setUV(0, 1);
		attr.v1.setColorRGBA(rgb, alpha);
		attr.v1.setPos(0, 0, size);
		attr.v1.setFaceVertDir(-1);
		attr.v0.setDirection(faceDir, 1, false);
		attr.v2.setUV(1, 1);
		attr.v2.setColorRGBA(rgb, alpha);
		attr.v2.setPos(size, 0, size);
		attr.v2.setFaceVertDir(-1);
		attr.v0.setDirection(faceDir, 2, false);
		attr.v3.setUV(1, 0);
		attr.v3.setColorRGBA(rgb, alpha);
		attr.v3.setPos(size, 0, 0);
		attr.v3.setFaceVertDir(-1);
		attr.v0.setDirection(faceDir, 3, false);
	}
	int size = 1;


	BlockFaceAttr attr = new BlockFaceAttr(); 
	TextureArray arr;
	
    private TesselatorState skybox1;
    private TesselatorState skybox2;
	private FontRenderer font;
	@Override
	public void lateInitGame() {
		this.font=FontRenderer.get(0, 22, 0);
		skybox1 = new TesselatorState(GL15.GL_STATIC_DRAW);
		skybox2 = new TesselatorState(GL15.GL_STATIC_DRAW);
        for (MeshList mlist : lists) {
        	mlist.init();
        }
		
		final int texSize = 128;
		arr = new TextureArray(10) {

			@Override
			protected void uploadTextures() {
				final byte[] datablocktex = new byte[texSize*texSize*4];
				int colors[] =  {
						0x0000ff,
						0x00ff00,
						0x00ffff,
						0xff0000,
						0xff00ff,
						0xffff00,
						0xffffff,
						0x7fff7f,
						0x7f7fff,
				};
				for (int slot = 0; slot < colors.length; slot++) {
					for (int x = 0; x < texSize; x++) {
						for (int y = 0; y < texSize; y++) {
							int idx = (y*texSize+x)*4;
							if (((x/4+y/4))%2==0) {
								datablocktex[idx+0] = (byte) ((colors[slot]>>16)&0xff);
								datablocktex[idx+1] = (byte) ((colors[slot]>>8)&0xff);
								datablocktex[idx+2] = (byte) ((colors[slot]>>0)&0xff);
							}
							datablocktex[idx+3] = (byte) 0xff;
						}
						
					}
					byte[] data = datablocktex;
			        ByteBuffer directBuf = null;
	                int avg = TextureUtil.getAverageColor(datablocktex, this.tileSize, this.tileSize);
	                int mipmapSize = this.tileSize;
	                for (int m = 0; m < numMipmaps; m++) {
	                    directBuf = put(directBuf, data);
	                    //                      System.out.println(m+"/"+mipmapSize+"/"+directBuf.position()+"/"+directBuf.capacity()+"/"+directBuf.remaining());
	                    GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, m, //Mipmap number
	                            0, 0, slot, //xoffset, yoffset, zoffset
	                            mipmapSize, mipmapSize, 1, //width, height, depth
	                            GL_RGBA, //format
	                            GL_UNSIGNED_BYTE, //type
	                            directBuf);//pointer to data
	                    Engine.checkGLError("GL12.glTexSubImage3D");
	                    mipmapSize /= 2;
	                    if (mipmapSize > 0)
	                        data = TextureUtil.makeMipMap(data, mipmapSize, mipmapSize, avg);
	                }
				}
			}

			@Override
			protected void findMaxTileWidth() {
	            this.tileSize = texSize;
		        this.numTextures = 9;
			}
			@Override
			protected void collectTextures(AssetManager mgr) {
			}

			@Override
			protected void postUpload() {
		        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.glid);
		        boolean useDefault = false;
		        if (useDefault) {

		            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
		            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
		            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
		        } else {// does not work with alpha testing

		            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
		            glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
		            glTexParameterf(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT, 4.0f);
		            //      GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
		        }
			}
			
		};


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

		arr.reload();
		redraw();
		initShaders();
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
