package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;

public class FrameBufferChainTest extends GameBase {
	SimpleResourceManager resMgr = new SimpleResourceManager();
	public FrameBufferChainTest() {
		TICKS_PER_SEC = 20;
		Engine.initRenderers = false;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new FrameBufferChainTest().startGame();
	}

	public final static int NUM_FRAMEBUFFERS = 64; 
	private int ticks;
	private Shader shader;
	private Shader shader2;
	FrameBuffer buffers[] = new FrameBuffer[NUM_FRAMEBUFFERS];
	

	@Override
	public void onStatsUpdated() {
		if (ticks++>1) {
			String stats = "";
			if (glProfileResults.size() > 2) {
				stats = lastFPS+" ("+String.format("%.5fms %s %s %s", Stats.avgFrameTime, glProfileResults.get(0), glProfileResults.get(1), glProfileResults.get(2))+")";	
			} else {
				stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")";
			}
			
			System.out.println(stats);
//			setTitle(stats);
			ticks = 0;
//			reloadShaders();
		}
	}

	/**
	 * 
	 */
	private void reloadShaders() {
		// TODO Auto-generated method stub

		try {
			resMgr.release();
			Shader shader = AssetManager.getInstance().loadShader(resMgr, "debug/test");
			Shader shader2 = AssetManager.getInstance().loadShader(resMgr, "sky/skybox");
			this.shader = shader;
			this.shader2 = shader2;
			this.shader.enable();
			this.shader.setProgramUniform1i("tex0", 0);
			this.shader2.enable();
			this.shader2.setProgramUniform1i("tex0", 0);
			this.shader2.setProgramUniform1f("lightIntens", 1);
			Shader.disable();
		} catch (ShaderCompileError e) {
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
        }
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(float f) {
	      glClearColor(0,0,0,1);
	      glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
	      


//	      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.start("draw");
	      shader.enable();
//	      shader.setProgramUniform1f("iGlobalTime", this.tick+f);
	      int input = TMgr.getNoise();
	      for (int i = 0; i < buffers.length; i++) {
//	    	  if (i == 32)
//	    		  shader2.enable();
		      GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
	    	  buffers[i].bind();
	    	  buffers[i].clearFrameBuffer();
		      Engine.drawFullscreenQuad();
		      input = buffers[i].getTexture(0);
//	    	  if (i == 32)
//	    		  shader.enable();
	      }
	      FrameBuffer.unbindFramebuffer();
	      GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
	      Engine.drawFullscreenQuad();
	      Shader.disable();
//	      Shaders.textured.enable();
//	      FontRenderer.get(0, 12, 0).drawString("hey", 0, 20, -1, true, 1);
//	      Shader.disable();
//	      if (GPUProfiler.PROFILING_ENABLED) GPUProfiler.end();
	}

	@Override
	public void preRenderUpdate(float f) {
        UniformBuffer.updateUBO(null, f);
	}

	@Override
	public void postRenderUpdate(float f) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
            for (int i = 0; i < buffers.length; i++) {
            	if (buffers[i] != null)
            	buffers[i].release();
            	buffers[i] = new FrameBuffer(displayWidth, displayHeight);
            	buffers[i].setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
                buffers[i].setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
                buffers[i].setup(null);
            }
        }
	}

	@Override
	public void tick() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initGame() {
        Engine.init();
		TextureManager.getInstance().init();
		setVSync(false);
		reloadShaders();
	 	glActiveTexture(GL_TEXTURE0);
	}

	@Override
	public void lateInitGame() {
	}


	/* (non-Javadoc)
	 * @see nidefawl.qubes.GameBase#onWheelScroll(long, double, double)
	 */
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		// TODO Auto-generated method stub
		
	}

}
