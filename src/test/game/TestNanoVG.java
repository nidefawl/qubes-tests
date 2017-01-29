package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.BufferUtils.*;



import org.lwjgl.BufferUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;



import java.util.Arrays;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.*;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vec3D;

public class TestNanoVG extends GameBase {
	final CameraController cameraController = new CameraController();
	static final NVGColor
	colorA = NVGColor.create(),
	colorB = NVGColor.create(),
	colorC = NVGColor.create();
	static NVGColor rgba(int r, int g, int b, int a, NVGColor color) {
		color.r(r / 255.0f);
		color.g(g / 255.0f);
		color.b(b / 255.0f);
		color.a(a / 255.0f);

		return color;
	}
	long vg;
	static final int
		GRAPH_RENDER_FPS     = 0,
		GRAPH_RENDER_MS      = 1,
		GRAPH_RENDER_PERCENT = 2;
	private static final int GRAPH_HISTORY_COUNT = 100;

	static class PerfGraph {
		int style;
		ByteBuffer name   = BufferUtils.createByteBuffer(32);
		float[]    values = new float[GRAPH_HISTORY_COUNT];
		int head;
	}
	static class DemoData {

		final ByteBuffer entypo = loadResource("demo/nanovg/entypo.ttf", 40 * 1024);
		final ByteBuffer RobotoRegular = loadResource("demo/nanovg/Roboto-Regular.ttf", 150 * 1024);
		final ByteBuffer RobotoBold = loadResource("demo/nanovg/Roboto-Bold.ttf", 150 * 1024);

		int fontNormal,
			fontBold,
			fontIcons;

		int[] images = new int[12];
	}

	static int loadDemoData(long vg, DemoData data) {
		int i;

		if ( vg == NULL )
			return -1;

		for ( i = 0; i < 12; i++ ) {
			String file = "demo/nanovg/images/image" + (i + 1) + ".jpg";
			ByteBuffer img = loadResource(file, 32 * 1024);
			data.images[i] = nvgCreateImageMem(vg, 0, img);
			if ( data.images[i] == 0 ) {
				System.err.format("Could not load %s.\n", file);
				return -1;
			}
		}

		data.fontIcons = nvgCreateFontMem(vg, "icons", data.entypo, 0);
		if ( data.fontIcons == -1 ) {
			System.err.format("Could not add font icons.\n");
			return -1;
		}
		data.fontNormal = nvgCreateFontMem(vg, "sans", data.RobotoRegular, 0);
		if ( data.fontNormal == -1 ) {
			System.err.format("Could not add font italic.\n");
			return -1;
		}
		data.fontBold = nvgCreateFontMem(vg, "sans-bold", data.RobotoBold, 0);
		if ( data.fontBold == -1 ) {
			System.err.format("Could not add font bold.\n");
			return -1;
		}

		return 0;
	}
	private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
		ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
		buffer.flip();
		newBuffer.put(buffer);
		return newBuffer;
	}

	/**
	 * Reads the specified resource and returns the raw data as a ByteBuffer.
	 *
	 * @param resource   the resource to read
	 * @param bufferSize the initial buffer size
	 *
	 * @return the resource data
	 *
	 * @throws IOException if an IO error occurs
	 */
	public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
		ByteBuffer buffer;

		File file = new File(resource);
		if ( file.isFile() ) {
			FileInputStream fis = new FileInputStream(file);
			FileChannel fc = fis.getChannel();
			
			buffer = BufferUtils.createByteBuffer((int)fc.size() + 1);

			while ( fc.read(buffer) != -1 ) ;
			
			fis.close();
			fc.close();
		} else {
			buffer = createByteBuffer(bufferSize);

			InputStream source = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
			if ( source == null )
				throw new FileNotFoundException(resource);

			try {
				ReadableByteChannel rbc = Channels.newChannel(source);
				try {
					while ( true ) {
						int bytes = rbc.read(buffer);
						if ( bytes == -1 )
							break;
						if ( buffer.remaining() == 0 )
							buffer = resizeBuffer(buffer, buffer.capacity() * 2);
					}
				} finally {
					rbc.close();
				}
			} finally {
				source.close();
			}
		}

		buffer.flip();
		return buffer;
	}

	static ByteBuffer loadResource(String resource, int bufferSize) {
		try {
			return ioResourceToByteBuffer(resource, bufferSize);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load resource: " + resource, e);
		}
	}

	
	PerfGraph fps = new PerfGraph();
	DemoData data = new DemoData();


	static void initGraph(PerfGraph fps, int style, String name) {
		fps.style = style;
		fps.name = memUTF8(name);
		Arrays.fill(fps.values, 0);
		fps.head = 0;
	}

	static void updateGraph(PerfGraph fps, float frameTime) {
		fps.head = (fps.head + 1) % GRAPH_HISTORY_COUNT;
		fps.values[fps.head] = frameTime;
	}

	static float getGraphAverage(PerfGraph fps) {
		float avg = 0;
		for ( int i = 0; i < GRAPH_HISTORY_COUNT; i++ ) {
			avg += fps.values[i];
		}
		return avg / (float)GRAPH_HISTORY_COUNT;
	}

	static void renderGraph(long vg, float x, float y, PerfGraph fps) {
		float avg = getGraphAverage(fps);

		int w = 200;
		int h = 35;

		nvgBeginPath(vg);
		nvgRect(vg, x, y, w, h);
		nvgFillColor(vg, rgba(0, 0, 0, 128, colorA));
		nvgFill(vg);

		nvgBeginPath(vg);
		nvgMoveTo(vg, x, y + h);
		if ( fps.style == GRAPH_RENDER_FPS ) {
			for ( int i = 0; i < GRAPH_HISTORY_COUNT; i++ ) {
				float v = 1.0f / (0.00001f + fps.values[(fps.head + i) % GRAPH_HISTORY_COUNT]);
				float vx, vy;
				if ( v > 1000.0f ) v = 1000.0f;
				vx = x + ((float)i / (GRAPH_HISTORY_COUNT - 1)) * w;
				vy = y + h - ((v / 1000.0f) * h);
				nvgLineTo(vg, vx, vy);
			}
		} else if ( fps.style == GRAPH_RENDER_PERCENT ) {
			for ( int i = 0; i < GRAPH_HISTORY_COUNT; i++ ) {
				float v = fps.values[(fps.head + i) % GRAPH_HISTORY_COUNT] * 1.0f;
				float vx, vy;
				if ( v > 100.0f ) v = 100.0f;
				vx = x + ((float)i / (GRAPH_HISTORY_COUNT - 1)) * w;
				vy = y + h - ((v / 100.0f) * h);
				nvgLineTo(vg, vx, vy);
			}
		} else {
			for ( int i = 0; i < GRAPH_HISTORY_COUNT; i++ ) {
				float v = fps.values[(fps.head + i) % GRAPH_HISTORY_COUNT] * 1000.0f;
				float vx, vy;
				if ( v > 4.0f ) v = 4.0f;
				vx = x + ((float)i / (GRAPH_HISTORY_COUNT - 1)) * w;
				vy = y + h - ((v / 4.0f) * h);
				nvgLineTo(vg, vx, vy);
			}
		}
		nvgLineTo(vg, x + w, y + h);
		nvgFillColor(vg, rgba(255, 192, 0, 128, colorA));
		nvgFill(vg);

		nvgFontFace(vg, "sans");

		if ( fps.name.get(0) != '\0' ) {
			nvgFontSize(vg, 14.0f);
			nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
			nvgFillColor(vg, rgba(240, 240, 240, 192, colorA));
			nvgText(vg, x + 3, y + 1, fps.name);
		}

		if ( fps.style == GRAPH_RENDER_FPS ) {
			nvgFontSize(vg, 18.0f);
			nvgTextAlign(vg, NVG_ALIGN_RIGHT | NVG_ALIGN_TOP);
			nvgFillColor(vg, rgba(240, 240, 240, 255, colorA));
			nvgText(vg, x + w - 3, y + 1, String.format("%.2f FPS", 1.0f / avg));

			nvgFontSize(vg, 15.0f);
			nvgTextAlign(vg, NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM);
			nvgFillColor(vg, rgba(240, 240, 240, 160, colorA));
			nvgText(vg, x + w - 3, y + h - 1, String.format("%.2f ms", avg * 1000.0f));
		} else if ( fps.style == GRAPH_RENDER_PERCENT ) {
			nvgFontSize(vg, 18.0f);
			nvgTextAlign(vg, NVG_ALIGN_RIGHT | NVG_ALIGN_TOP);
			nvgFillColor(vg, rgba(240, 240, 240, 255, colorA));
			nvgText(vg, x + w - 3, y + 1, String.format("%.1f %%", avg * 1.0f));
		} else {
			nvgFontSize(vg, 18.0f);
			nvgTextAlign(vg, NVG_ALIGN_RIGHT | NVG_ALIGN_TOP);
			nvgFillColor(vg, rgba(240, 240, 240, 255, colorA));
			nvgText(vg, x + w - 3, y + 1, String.format("%.2f ms", avg * 1000.0f));
		}
	}
	public TestNanoVG() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestNanoVG().startGame();
	}
	private int image;
	SMAA smaa;
	FrameBuffer fb;
	FrameBuffer fb2;
	

	int a = 0;
	@Override
	public void onStatsUpdated() {
		String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")";
//		System.out.println();
		a++;
		if (a > 1) {
			setTitle(stats);

        	loadShader();
        	a = 0;
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
		glEnable(GL_DEPTH_TEST);
		this.fb.bind();
		this.fb.clearFrameBuffer();
        this.shaderTexture.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.image);
        Engine.drawFullscreenQuad();
        this.shaderTexture.enable();
		this.fb2.bind();
		this.fb2.clearFrameBuffer();
		this.fb.bindRead();
        GL30.glBlitFramebuffer(0, 0, this.fb.getWidth(), this.fb.getHeight(), 0, 0, this.fb2.getWidth(), this.fb2.getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        FrameBuffer.unbindReadFramebuffer();
        
		glDepthFunc(GL_EQUAL);
		
        this.shaderHeavy.enable();
        Engine.drawFullscreenQuad();
		FrameBuffer.unbindFramebuffer();
		
		glDepthFunc(GL_LEQUAL);
        glClearColor(0.2f,0.2f,0.2f,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
//
        Shaders.textured.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.fb2.getTexture(0));
        Engine.drawFullscreenQuad();
//		nvgStart();
//
//		renderGraph(vg, 5, 5, fps);
//
//		{
//			String text = "Can you read me?";
//			ByteBuffer textEncoded = memEncodeASCII(text, BufferAllocator.MALLOC);
//
//			nvgFontSize(vg, 22.0f);
//			nvgFontFace(vg, "sans-bold");
//			float tw = nvgTextBounds(vg, 0, 0, textEncoded, (ByteBuffer)null);
//
//			nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
//			nvgFillColor(vg, rgba(0, 0, 0, 160, colorA));
//			nvgText(vg, ((float)Game.displayWidth-tw)/2.0f+2, 32+2, textEncoded);
//			nvgFillColor(vg, rgba(255, 255, 255, 255, colorA));
//			nvgText(vg, ((float)Game.displayWidth-tw)/2.0f, 32, textEncoded);
//			memFree(textEncoded);
//		}
//		{
//			String text = "Can you still read me?";
//			ByteBuffer textEncoded = memEncodeASCII(text, BufferAllocator.MALLOC);
//
//			nvgFontSize(vg, 10.0f);
//			nvgFontFace(vg, "sans-bold");
//			float tw = nvgTextBounds(vg, 0, 0, textEncoded, NULL, (ByteBuffer)null);
//
//			nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_BOTTOM);
//			nvgFillColor(vg, rgba(0, 0, 0, 160, colorA));
//			nvgText(vg, ((float)Game.displayWidth-tw)/2.0f+2, 64+2, textEncoded);
//			nvgFillColor(vg, rgba(255, 255, 255, 255, colorA));
//			nvgText(vg, ((float)Game.displayWidth-tw)/2.0f, 64, textEncoded);
//			memFree(textEncoded);
//		}
//		nvgEnd();
		{
			String text = "Can you read me?";
			Shaders.textured.enable();
			FontRenderer fr = FontRenderer.get(0, 18, 1);
			float tw = fr.getStringWidth(text);
			fr.drawString(text, ((float)Game.displayWidth-tw)/2.0f, 110, -1, true, 1.0f);
		}
		{
			String text = "Can you read me?";
			Shaders.textured.enable();
			FontRenderer fr = FontRenderer.get(0, 10, 1);
			float tw = fr.getStringWidth(text);
			fr.drawString(text, ((float)Game.displayWidth-tw)/2.0f, 140, -1, true, 1.0f);
		}
		
	}
	 void nvgStart() {
		nvgBeginFrame(vg, Game.displayWidth, Game.displayHeight, 1);
		
	}
	 void nvgEnd() {
			nvgEndFrame(vg);
		Engine.restoreDepthMask();
		Engine.restoreScissorTest();
		Shader.disable();
		glEnable(GL_DEPTH_TEST);
		Engine.setDefaultViewport();
		Engine.bindVAO(null);
	    Engine.setBlend(true);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glActiveTexture(GL_TEXTURE0);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glColorMask(true, true, true, true);
        glEnable(GL_CULL_FACE);
	}

	private Shader shaderHeavy;
	private Shader shaderTexture;
	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);

		updateGraph(fps, Stats.lastFrameTimeD/1000.0f);
	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        	if (smaa != null) {
        		smaa.releaseAll(EResourceType.FRAMEBUFFER);
        	}
        	if (smaa == null) smaa = new SMAA(SMAA.SMAA_PRESET_MEDIUM);
        	smaa.init(displayWidth, displayHeight);
        	if (fb != null) {
        		fb.release();
        	}
            fb = new FrameBuffer(displayWidth, displayHeight);
            fb.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
            fb.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
            fb.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
            fb.setHasDepthAttachment();
            fb.setup(null);
        	if (fb2 != null) {
        		fb2.release();
        	}
            fb2 = new FrameBuffer(displayWidth, displayHeight);
            fb2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16F);
            fb2.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
            fb2.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
            fb2.setHasDepthAttachment();
            fb2.setup(null);
        	loadShader();
        }
	}

	/**
	 * 
	 */
	private void loadShader() {
    	try {
        	if (this.shaderTexture != null) {
        		this.shaderTexture.release();
        	}

        	if (this.shaderHeavy != null) {
        		this.shaderHeavy.release();
        	}

        	this.shaderHeavy = AssetManager.getInstance().loadShader(null, "debug/slowshader");
        	this.shaderHeavy.enable();
        	this.shaderHeavy.setProgramUniform1i("tex0", 0);

        	this.shaderTexture = AssetManager.getInstance().loadShader(null, "textured", new IShaderDef() {
				
				@Override
				public String getDefinition(String define) {
					if ("ALPHA_TEST".equals(define))
						return "#define ALPHA_TEST";
					return null;
				}
			});
        	this.shaderTexture.enable();
        	this.shaderTexture.setProgramUniform1i("tex0", 0);
    	} catch (ShaderCompileError e) {
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
    		
    	}
	}

	@Override
	public void tick() {
		this.cameraController.tickUpdate();
	}
	
	@Override
	public void initGame() {
        Engine.init();
		TextureManager.getInstance().init();
		FontRenderer.init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
		FrameBuffer.unbindFramebuffer();
		glEnable(GL_DEPTH_TEST);
		Engine.setBlend(true);
		initGraph(fps, GRAPH_RENDER_FPS, "Frame Time");

		vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES | NVG_DEBUG);

		if ( vg == NULL ) {
			throw new RuntimeException("Could not init nanovg.");
		}
		if ( loadDemoData(vg, data) == -1 )
			throw new RuntimeException();
	}

	@Override
	public void lateInitGame() {
		AssetTexture t = AssetManager.getInstance().loadPNGAsset("textures/mask.png");
		GLFW.glfwSetWindowSize(windowId, t.getWidth(), t.getHeight());
		this.image = TextureManager.getInstance().makeNewTexture(t, false, true, 0);
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		
	}

}
