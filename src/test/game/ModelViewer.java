/**
 * 
 */
package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.Stack;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.models.EntityModelManager;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.render.*;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.post.HBAOPlus;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ModelViewer extends GameBase {
	final CameraController cameraController = new CameraController();
	private FrameBuffer buf;
	private FrameBuffer sceneFB;
	private TesselatorState tessState;
	Shader shaderModelSingle;
	Shader shaderDeferred;
    int action = 0;
    Gui gui = null;
    boolean wasGrabbed = true;
    public static ModelViewer instance;
	public static void main(String[] args) {
		TICKS_PER_SEC = 20;
		Engine.initRenderers = false;
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		Gui.FONT_SIZE_WINDOW_TITLE = 16;
		Gui.FONT_SIZE_BUTTON = 14;
		instance = new ModelViewer();
		instance.startGame();
	}
	

	int tick = 0;
//	private ModelQModel model2;
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	private static boolean startup;
	
//	public static String[] models = {
//			"male", "female", "archer", "cat", "chicken", "cow", "dog", "duck", "pig", "pony", "puppy", "sheep"
//	};
    public void initShaders() {
        try {
			renderBatched.initShaders();
			renderSingle.initShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader newShaderModelSingle = assetMgr.loadShader(newshaders, "model/model_viewer");
            Shader new_shaderDef = assetMgr.loadShader(newshaders, "post/deferred", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("RENDER_PASS".equals(define)) {
                        return "#define RENDER_PASS 0";
                    }
                    return null;
                }
            });
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
            shaderModelSingle = newShaderModelSingle;
            shaderDeferred = new_shaderDef;
            
            shaderModelSingle.enable();
            shaderModelSingle.setProgramUniform1i("tex0", 0);
            shaderDeferred.enable();
            shaderDeferred.setProgramUniform1i("texColor", 0);
            shaderDeferred.setProgramUniform1i("texNormals", 1);
            shaderDeferred.setProgramUniform1i("texMaterial", 2);
            shaderDeferred.setProgramUniform1i("texDepth", 3);
            shaderDeferred.setProgramUniform1i("texShadow", 4);
            shaderDeferred.setProgramUniform1i("texLight", 5);
            shaderDeferred.setProgramUniform1i("texBlockLight", 6);
            shaderDeferred.setProgramUniform1i("texAO", 7);
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
		setTitle(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
		tick--;
		if (tick <= 0) {
			initShaders();
			Shaders.initShaders();
			tick = 2222;
//			reloadModel();
		}
		
	}
	public boolean showNormals;
	public boolean showBones;
	public boolean showWireframe;
	public boolean renderBatchedMode;
	QModelRender renderBatched;
	QModelRender renderSingle;
	QModelRender curRender = null;
	EntityModel entityModel;
	int modelidx = 0;
    boolean once = false;

	private Vec3D tmpPos = new Vec3D();
	boolean hadContext = false;
	private FrameBuffer buf2;
	public QModelProperties config = new QModelProperties();
	private void reloadModel() {

		try {
			if (!once) {
				this.entityModel = EntityModel.models[this.modelidx];
//				String mdoelName = EntityModel.models[this.modelidx];
//				setting = new ModelSetting("models/"+mdoelName+".qmodel");
			}
//			once = true;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("FAIL");
		}
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		if (action == GLFW.GLFW_PRESS)
		switch (key) {
		case GLFW.GLFW_KEY_KP_ADD:
			setModel(this.modelidx+1);
			break;
		case GLFW.GLFW_KEY_KP_SUBTRACT:
			setModel(this.modelidx-1);
			break;
		}
	}

	public void setModel(int i) {
			modelidx=i;
		if (modelidx < 0) {
			modelidx=EntityModel.HIGHEST_MODEL_ID;
		}
		if (modelidx > EntityModel.HIGHEST_MODEL_ID) {
			modelidx=0;
		}
		reloadModel();
		GuiWindowManager.getWindow(GuiModelViewer.class).setModel(this.entityModel, this.curRender, this.config);
	}
	
	@Override
	public void render(float fTime) {
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
		this.shaderModelSingle.enable();
		this.shaderModelSingle.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
		this.shaderModelSingle.setProgramUniformMatrix4("normal_matrix", false, Engine.getMatSceneNormal().get(), false);

        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        for (int i = 0; i < 3; i++) {
            GL40.glBlendFuncSeparatei(1+i, GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);
        }

        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, TMgr.getEmptyWhite());
		tessState.drawQuads();
		glLineWidth(1.5f);
		glPointSize(12.0f);
        Shaders.normals.enable();
        Shaders.normals.setProgramUniformMatrix4("model_matrix", false, Engine.getIdentityMatrix().get(), false);
		tessState.drawQuads();
		

        float absTimeInSeconds = ((GameBase.ticksran+fTime)/GameBase.TICKS_PER_SEC);
        curRender.reset();

        this.curRender.setModel(entityModel.model);
        this.config.rot.x = 0;
        this.config.rot.y = -90;
		this.entityModel.setPose(this.curRender, this.config, absTimeInSeconds, fTime);

		this.curRender.render(fTime);
        curRender.reset();
		
//		this.shaderModelSingle.enable();
//		this.shaderModelSingle.setProgramUniformMatrix4("model_matrix", false, this.render.modelMat.get(), false);
//		this.shaderModelSingle.setProgramUniformMatrix4("normal_matrix", false, this.render.normalMat.get(), false);
//		render.render(this.entityModel, fTime);
		
        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        GLDebugTextures.readTexture("Pass0", "texColor", Engine.getSceneFB().getTexture(0));
        GLDebugTextures.readTexture("Pass0", "texNormals", Engine.getSceneFB().getTexture(1));
        GLDebugTextures.readTexture("Pass0", "texMaterial", Engine.getSceneFB().getTexture(2));
        GLDebugTextures.readTexture("Pass0", "texLight", Engine.getSceneFB().getTexture(3));
        Engine.checkGLError("Pass0");
        
        
		HBAOPlus.renderAO();
        GLDebugTextures.readTexture("HBAOOutput", "texAO", buf.getTexture(0));
        Engine.checkGLError("HBAOOutput "+buf.getTexture(0));
        
        
        buf2.bind();
        buf2.clearFrameBuffer();
        shaderDeferred.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
//      GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, Engine.shadowRenderer.getDepthTex());
//      GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.lightCompute.getTexture());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getEmpty());
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, TMgr.getEmpty());
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(3));
        GL.bindTexture(GL_TEXTURE7, GL_TEXTURE_2D, this.buf.getTexture(0));
        Engine.drawFullscreenQuad();
        glClear(GL11.GL_DEPTH_BUFFER_BIT);
        sceneFB.bindRead();
        GL30.glBlitFramebuffer(0, 0, sceneFB.getWidth(), sceneFB.getHeight(), 0, 0, sceneFB.getWidth(), sceneFB.getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        FrameBuffer.unbindReadFramebuffer();
//
        if (!this.renderBatchedMode) {
            Engine.bindVAO(GLVAO.vaoModel);
            for (int i = 0; i < curRender.rendered.size(); i++) {
    	        QModelObject obj = curRender.rendered.get(i);
    	        if (showNormals) {
//    	            UniformBuffer.setNormalMat(this.render.normalMat.get());
//    	            UniformBuffer.setNormalMat(Engine.getMatSceneNormal().get());
    	    		glLineWidth(1.5f);
    	    		glPointSize(12.0f);
    		        Shaders.normals.enable();
    		        Shaders.normals.setProgramUniformMatrix4("model_matrix", false, curRender.modelMat.get(), false);

    	            for (QModelGroup grp : obj.listGroups) {
    	            	curRender.renderGroup(this.entityModel.model, obj, grp, fTime);
    	            }
//    	            UniformBuffer.setNormalMat(Engine.getMatSceneNormal().get());
    	        }
    	        if (showWireframe) {
    	    		glLineWidth(2f);
    	    		glPointSize(12.0f);
    				Shaders.wireframe.enable();
    		        Shaders.wireframe.setProgramUniformMatrix4("model_matrix", false, curRender.modelMat.get(), false);
    		        Shaders.wireframe.setProgramUniform3f("in_offset", Engine.GLOBAL_OFFSET.x, Engine.GLOBAL_OFFSET.y, Engine.GLOBAL_OFFSET.z);
    		        Shaders.wireframe.setProgramUniform1i("num_vertex", 3);
    		        Shaders.wireframe.setProgramUniform1f("thickness", 0.2f);
    		        Shaders.wireframe.setProgramUniform1f("maxDistance", 110);
    		        Shaders.wireframe.setProgramUniform4f("linecolor", 1, 0.2f, 0.2f, 1);
    	            for (QModelGroup grp : obj.listGroups) {
    	            	curRender.renderGroup(this.entityModel.model, obj, grp, fTime);
    	            }
    	        }
            }
        }
        if (showBones) {
	        glClear(GL11.GL_DEPTH_BUFFER_BIT);
        	GL11.glDisable(GL11.GL_BLEND);
        	
        	renderBones((ModelRigged)this.entityModel.model, curRender.modelMat);
        	GL11.glEnable(GL11.GL_BLEND);
        }
    
        
		FrameBuffer.unbindFramebuffer();
        GLDebugTextures.readTexture("Pass1", "out", buf2.getTexture(0), 1);
        glClearColor(0,0,0,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.tonemap.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, buf2.getTexture(0));
        Engine.drawFullscreenQuad();
        GLDebugTextures.drawAll(displayWidth, displayHeight);
        Engine.checkGLError("drawAll");
        double mx = Mouse.getX();
        double my = Mouse.getY();
        if (this.gui != null) {
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.start("gui");
            GuiWindow window = GuiWindowManager.getMouseOver(mx, my);
            if (window != null && (Gui.selectedButton == null || Gui.selectedButton.parent != gui)) {
                mx-=10000;
                my-=10000;
            }
            this.gui.render(fTime, mx, my);
            if (window != null && (Gui.selectedButton == null || Gui.selectedButton.parent != gui)) {
                mx+=10000;
                my+=10000;
            }
            
            if (GPUProfiler.PROFILING_ENABLED)
                GPUProfiler.end();
        }
        GuiWindowManager.getInstance().render(fTime, mx, my);
//        glDisable(GL_DEPTH_TEST);
        glEnable(GL_DEPTH_TEST);
	}
    public void showGUI(Gui gui) {

        if (gui != null && this.gui == null) {
            if (Mouse.isGrabbed()) {
                setGrabbed(false);
                wasGrabbed = true;
            }
        }
        if (this.gui != null) {
            this.gui.onClose();
        }
        this.gui = gui;
        if (this.gui != null) {
            this.gui.setPos(0, 0);
            this.gui.setSize(displayWidth, displayHeight);
            this.gui.initGui(this.gui.firstOpen);
            this.gui.firstOpen = false;
            if (Mouse.isGrabbed()) {
                setGrabbed(false);
                wasGrabbed = true;
            }
        } else {
            if (wasGrabbed) {
                setGrabbed(true);
            }
            wasGrabbed = false;
        }
            
    }

	static Stack<QModelPoseBone> stack = new Stack<>();
	static Vector3f tmp = new Vector3f();
	
	private static void renderBones(ModelRigged model, BufferedMatrix modelMat) {
//		Engine.enableDepthMask(false);
//		glDisable(GL_DEPTH_TEST);
		glLineWidth(2f);
		glPointSize(12.0f);
		Shaders.colored3D.enable();
		for (int i = 0; i < 2; i++) {
			int color1 = 0x0000aa;
			int color2 = 0x9999ff;
			if (i == 1) {
				 color1 = 0xaa00aa;
				 color2 = 0xff99ff;
			} else if (i == 2) {
				 color1 = 0x00aa00;
				 color2 = 0x99ff99;
			}
			Tess.tessFont.setColorF(color1, 1.0f);
			Tess.instance.setColorF(color2, 1.0f);
			stack.clear();
			stack.push(model.rootJoint);
			while (!stack.isEmpty()) {
				QModelPoseBone bone = stack.pop();
//				System.out.println(bone.restbone.name+" - "+bone.getChildren());
                if (bone.isDeform()) {
					Matrix4f.transform(i == 0 ? bone.getMatRest() : bone.getMatDeform(), Vector3f.ZERO, tmp);
					Matrix4f.transform(modelMat, tmp, tmp);
					Tess.tessFont.add(tmp.x, tmp.y, tmp.z);
					Tess.instance.add(tmp.x, tmp.y, tmp.z);
//					System.out.println(bone.tailLocal);
					tmp.set(bone.getTailLocal());
//					tmp.scale(1f);
					Matrix4f.transform(i == 0 ? bone.getMatRest() : bone.getMatDeform(), tmp, tmp);
					Matrix4f.transform(modelMat, tmp, tmp);
					Tess.instance.add(tmp.x, tmp.y, tmp.z);
					Tess.tessFont.add(tmp.x, tmp.y, tmp.z);

                }
				for (QModelPoseBone child : bone.getChildren()) {
					stack.push(child);
				}
			}


		}
		Tess.instance.draw(GL11.GL_LINES);
		Tess.tessFont.draw(GL11.GL_POINTS);
		for (int i = 0; i < 2; i++) {
			Tess.tessFont.setColorF(0x00aaaa, 1.0f);
			Tess.instance.setColorF(0x99ffff, 1.0f);
			stack.clear();
			stack.push(model.rootJoint);
			while (!stack.isEmpty()) {
				QModelPoseBone parent = stack.pop();
				for (QModelPoseBone child : parent.getChildren()) {
					if (!child.isConnected()) {
//						System.out.println(child.name+" is disconnected "+child.flags);
						Matrix4f.transform(i == 0 ? child.getMatRest() : child.getMatDeform(), Vector3f.ZERO, tmp);
						Matrix4f.transform(modelMat, tmp, tmp);
						Tess.instance.add(tmp.x, tmp.y, tmp.z);
						Tess.tessFont.add(tmp.x, tmp.y, tmp.z);
						Matrix4f.transform(i == 0 ? parent.getMatRest() : parent.getMatDeform(), parent.getTailLocal(), tmp);
						Matrix4f.transform(modelMat, tmp, tmp);
						Tess.tessFont.add(tmp.x, tmp.y, tmp.z);
						Tess.instance.add(tmp.x, tmp.y, tmp.z);
					}
					stack.push(child);
				}
			}
			Tess.instance.draw(GL11.GL_LINES);
			Tess.tessFont.draw(GL11.GL_POINTS);
		}
//		glEnable(GL_DEPTH_TEST);
//		Engine.enableDepthMask(true);
    
	}

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

        if (renderBatchedMode) {
        	curRender = renderBatched;
        } else {
        	curRender = renderSingle;
        }
	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	
	@Override
	public void onResize(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        	if (hadContext) {
                Engine.checkGLError("pre GLNativeLib.deleteContext");
        		HBAOPlus.deleteContext();
                Engine.checkGLError("post GLNativeLib.deleteContext");
        	}
            Engine.checkGLError("pre GLNativeLib.createContext");
    		HBAOPlus.createContext(displayWidth, displayHeight);
            Engine.checkGLError("post GLNativeLib.createContext");
            Engine.resize(displayWidth, displayHeight);
			if (buf != null) buf.release();
			if (buf2 != null) buf2.release();
			if (sceneFB != null) sceneFB.release();
			buf2 = new FrameBuffer(displayWidth, displayHeight);
			buf2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
			buf2.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
			buf2.setHasDepthAttachment();
			buf2.setup(null);
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
	        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
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
            if (this.gui != null) {
                this.gui.setPos(0, 0);
                this.gui.setSize(displayWidth, displayHeight);
                this.gui.initGui(this.gui.firstOpen);
            }
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("onResize");
            GLDebugTextures.onResize();
        }
        glActiveTexture(GL_TEXTURE0);
	}

	@Override
	public void tick() {
		this.cameraController.tickUpdate();
	       if (this.gui != null) {
	           this.gui.update();
	       }
	       GuiWindowManager.update();
	       AsyncTasks.completeTasks();
	}

	@Override
	public void initGame() {
		QModelBatchedRender.isModelViewer = true;
        Engine.init();
		TextureManager.getInstance().init();
        EntityModel.preInit();
        EntityModel.postInit();
		setVSync(true);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
        renderBatched = Engine.renderBatched;
        renderSingle = new QModelDirectRender();
        curRender = renderBatched;
	}

	@Override
	public void lateInitGame() {
        EntityModelManager.getInstance().reload();
        renderBatched.initShaders();
        renderSingle.initShaders();
		tessState = new TesselatorState(GL15.GL_STATIC_DRAW);
		int w = 1;
		int d = 4;
		Tess.instance.setColor(0x550055, 0xff);
		Tess.instance.setOffset(0, 0, 0);
		Tess.instance.setNormals(0, 0, -1);
		Tess.instance.add(0, 0, d);
		Tess.instance.add(0, w, d);
		Tess.instance.add(w, w, d);
		Tess.instance.add(w, 0, d);
		Tess.instance.setNormals(-1, 0, 0);
		Tess.instance.add(0, w, d);
		Tess.instance.add(0, 0, d);
		Tess.instance.add(0, 0, d*2);
		Tess.instance.add(0, w, d*2);
		Tess.instance.setNormals(0, 1, 0);
		Tess.instance.add(w, w, d);
		Tess.instance.add(0, w, d);
		Tess.instance.add(0, w, d*2);
		Tess.instance.add(w, w, d*2);
		Tess.instance.setColor(0x330000, 0xff);
		Tess.instance.setNormals(0, 0, 1);
		Tess.instance.add(0, w, -d);
		Tess.instance.add(0, 0, -d);
		Tess.instance.add(w, 0, -d);
		Tess.instance.add(w, w, -d);
		Tess.instance.setNormals(-1, 0, 0);
		Tess.instance.add(0, 0, -d);
		Tess.instance.add(0, w, -d);
		Tess.instance.add(0, w, -d*2);
		Tess.instance.add(0, 0, -d*2);
		Tess.instance.setNormals(0, 1, 0);
		Tess.instance.add(0, w, -d);
		Tess.instance.add(w, w, -d);
		Tess.instance.add(w, w, -d*2);
		Tess.instance.add(0, w, -d*2);
		Tess.instance.setColor(0x005555, 0xff);
		Tess.instance.setNormals(0, 1, 0);
		Tess.instance.add(-4*w, 0, -d*4);
		Tess.instance.add(-4*w, 0, d*4);
		Tess.instance.add(4*w, 0, d*4);
		Tess.instance.add(4*w, 0, -d*4);
		Tess.instance.draw(GL_QUADS, this.tessState);
		initShaders();
        GuiWindow window = GuiWindowManager.openWindow(GuiModelViewer.class);
        window.allwaysVisible = true;
        setModel(0);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
