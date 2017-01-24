package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

import java.io.File;
import java.util.Iterator;
import java.util.Stack;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.hex.*;
import nidefawl.qubes.input.*;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.world.biomes.HexBiomesServer;

public class Hex extends GameBase {
	final CameraController cameraController = new CameraController();
	final PositionMouseOver mouseOverRight = new PositionMouseOver();
	private FrameBuffer sceneFB;
	boolean ortho = true;
	public Hex() {
		TICKS_PER_SEC = 20;
		Engine.initRenderers = false;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new Hex().startGame();
	}
	

	int tick = 0;
    static SimpleResourceManager shaders = new SimpleResourceManager();
    static SimpleResourceManager newshaders = new SimpleResourceManager();
	private static boolean startup;
	final static double halfLen = Math.cos(Math.PI*2*(30/360.0));


	Shader modelShader;
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_model_shader = assetMgr.loadShader(newshaders, "model/model_viewer");
            shaders.release();
            SimpleResourceManager tmp = shaders;
            shaders = newshaders;
            newshaders = tmp;
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
	@Override
	public void onStatsUpdated() {
//		System.out.println(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
		tick--;
		if (tick <= 0) {
			initShaders();
			renderQuad();
			tick = 22225;
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
        if (window == windowId) {
        	if (key == GLFW.GLFW_KEY_F1 && Keyboard.getState(action)) {
        		ortho = !ortho;
        	}
        	if (key == GLFW.GLFW_KEY_SPACE && Keyboard.getState(action)) {
        		mapOffsetX=mapOffsetZ=0;
        		updateOffset();
        	}
        	if (key == GLFW.GLFW_KEY_RIGHT && Keyboard.getState(action)) {
        		mapOffsetX+=scaleWidth;
        		updateOffset();
        	}
        	if (key == GLFW.GLFW_KEY_LEFT && Keyboard.getState(action)) {
        		mapOffsetX-=scaleWidth;
        		updateOffset();
        	}
        	if (key == GLFW.GLFW_KEY_UP && Keyboard.getState(action)) {
        		mapOffsetZ-=scaleHeight;
        		updateOffset();
        	}
        	if (key == GLFW.GLFW_KEY_DOWN && Keyboard.getState(action)) {
        		mapOffsetZ+=scaleHeight;
        		updateOffset();
        	}
        }
	}
	boolean drag = false;

	@Override
	public void onMouseClick(long window, int button, int action, int mods) {
        boolean b = Mouse.isGrabbed();
        boolean isDown = Mouse.getState(action);
        switch (button) {
            case 0:
        		drag = isDown;
            	if (!isDown) {
            		if (!diddrag) {

            			long l = biomes.toHex(mousePosX(), mousePosY());
            			int hX = GameMath.lhToX(l)+xPos;
            			int hZ = GameMath.lhToZ(l)+zPos;
            			HexBiome hexMouseOver = biomes.getPos(GameMath.toLong(hX, hZ));
            			Iterator<Long> it = hexMouseOver.getChunks().iterator();
            			while (it.hasNext()) {
            				Long l2 = it.next();
            				int x=GameMath.lhToX(l2);
            				int z=GameMath.lhToZ(l2);
            				System.out.println(x+"/"+z);
            			}
            		}

                	diddrag=false;
            	}
                break;
            case 1:
                if (isDown ) {
                    setGrabbed(!b);
                    b = !b;
                }
                break;
            case 2:
                break;
        }
        if (b != this.movement.grabbed()) {
            setGrabbed(b);
        }
	}


    Stack<QModelPoseBone> stack = new Stack<>();
    Vector3f tmp = new Vector3f();
    boolean once = false;
    HexBiomesServer biomes = new HexBiomesServer(new File("biomes"));
    
	private boolean update = true;
	float mapOffsetX = 0;
	float mapOffsetZ = 0;
	private float scale2;
	private float inOffsetX;
	private float inOffsetZ;
	private float scaleWidth;
	private float scaleHeight;
	private float floatzpos1;
	private int zPos1;
	private int zPos;
	private float rOffsetZ;
	private float floatxpos1;
	private int xPos1;
	private int xPos;
	private float rOffsetX;

	void updateOffset() {
		if (zoom < 1/4.0f)
			zoom = 1/4.0f;
		if (zoom  > 128)
			zoom = 128;
		scale2 = (float) ((1 / 16f) * zoom);
		inOffsetX = (float) (mapOffsetX);
		inOffsetZ = (float) (mapOffsetZ);
		scaleWidth = (float) (biomes.width * scale2);
		scaleHeight = (float) (biomes.height * scale2);
		floatzpos1 = (float) (inOffsetZ / scaleHeight);
		zPos1 = GameMath.floor(floatzpos1);
		zPos = 0 - zPos1;
		rOffsetZ = floatzpos1 - zPos1;
		floatxpos1 = (float) (inOffsetX / scaleWidth);
		floatxpos1 -= (zPos1 / 2.0f);
		xPos1 = GameMath.floor(floatxpos1);
		xPos = 0 - xPos1;
		rOffsetX = floatxpos1 - xPos1;
	}

	@Override
	public void render(float f) {
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
		GL11.glLineWidth(4);
		Shaders.colored.enable();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		Tess t = Tess.instance;

        FontRenderer fr = FontRenderer.get(0, 16, 0);

		long l = biomes.toHex(mousePosX(), mousePosY());
		int hX = GameMath.lhToX(l)+xPos;
		int hZ = GameMath.lhToZ(l)+zPos;
		HexBiome hexMouseOver = biomes.getPos(GameMath.toLong(hX, hZ));

		Engine.pxStack.push(displayWidth/2, displayHeight/2, 0);
		//		System.out.println(floatzpos1);
//		rOffsetX-=GameMath.floor(rOffsetZ/2.0);
		Engine.pxStack.translate(rOffsetX*scaleWidth, rOffsetZ*scaleHeight, 0);

		int xrange = 6;
		int zrange = 6;
		xrange=1+(int) (displayWidth/(scaleWidth*2));
		zrange=1+(int) (displayHeight/(scaleHeight*2));
		for (int z = -zrange-1; z <= +zrange; z++) {
			int xOffset = GameMath.floor(z/2.0);
			for (int x = -xrange-xOffset-1; x <= +xrange-xOffset; x++){
				t.setColorF(0xdddddd, 1);
				t.setOffset(0, 0, 0);
				int hx = x+xPos;
				int hz = z+zPos;
				HexBiome hexHit = biomes.getPos(GameMath.toLong(hx, hz));
				
				for (int i = 0; i < 7; i++) {
					float fx = (float) biomes.getPointX(x, z, i%6)*scale2;
					float fy = (float) biomes.getPointY(x, z, i%6)*scale2;
					t.add(fx, fy);
				}
				t.draw(GL11.GL_LINE_STRIP);
				float cx = (float)biomes.getCenterX(x, z)*scale2;
				float cz = (float)biomes.getCenterY(x, z)*scale2;
				t.setOffset(0, 0, 0);
				t.setColorF(hexHit.biome.color, 1);
				t.add(cx, cz);
				if (hexHit == hexMouseOver) {
					t.setColorF(0xffffff, 1);
				}
				for (int i = 6; i >= 0; i--) {
					float fx = (float) biomes.getPointX(x, z, i%6)*scale2;
					float fy = (float) biomes.getPointY(x, z, i%6)*scale2;
					t.add(fx, fy);
				}
				t.draw(GL11.GL_TRIANGLE_FAN);
//				if (hexHit == hexMouseOver) {
//					cx = (float) biomes.getCenterX(x, z) * scale2;
//					cz = (float) biomes.getCenterY(x, z) * scale2;
//					t.setOffset(0, 0, 0);
//					t.setColorF(0xaaaaaa, 0.7f);
//					float fx,fy,cdx,cdy;
//					for (int i = 6; i >= 0; i--) {
//						i++;
//						 fx = (float) biomes.getPointX(x, z, i % 6) * scale2;
//						 fy = (float) biomes.getPointY(x, z, i % 6) * scale2;
//						 cdx = fx-cx;
//						 cdy = fy-cz;
//						t.add(cx+cdx*0.8f, cz+cdy*0.8f);
//						i--;
//
//						 float bothx=cdx;
//						 float bothy=cdy;
//						 fx = (float) biomes.getPointX(x, z, i % 6) * scale2;
//						 fy = (float) biomes.getPointY(x, z, i % 6) * scale2;
//						 
//						 
//						 cdx = fx-cx;
//						 cdy = fy-cz;
//						 bothx=(cdx+bothx)/2.0f;
//						 bothy=(cdy+bothy)/2.0f;
//						t.add(cx+cdx*0.8f, cz+cdy*0.8f);
//						t.add(cx+bothx*0.3f, cz+bothy*0.3f);
//					}
//					t.draw(GL11.GL_TRIANGLES);
//				}
				Shaders.textured.enable();
				fr.drawString(""+hexHit.x+"/"+hexHit.z, cx, cz+7, -1, false, 1, 2);
				Shaders.colored.enable();
			}	
		}
		if (hexMouseOver != null) {
			Engine.pxStack.translate(-xPos*scaleWidth-zPos*0.5f*scaleWidth, -zPos*scaleHeight, 0);
			Shaders.colored.enable();
			Iterator<Long> it = hexMouseOver.getChunks().iterator();
			while (it.hasNext()) {
				Long l2 = it.next();
				int x=GameMath.lhToX(l2)*16;
				int z=GameMath.lhToZ(l2)*16;
				t.setColorF(0xdddddd, 1);
				t.setOffset(0, 0, 0);
				float minX = x*scale2;
				float maxX = (x+16)*scale2;
				float minZ = z*scale2;
				float maxZ = (z+16)*scale2;
				maxZ-=2;
				maxX-=2;
				t.add(maxX, minZ);
				t.add(minX, minZ);
				t.add(minX, maxZ);
				t.add(maxX, maxZ);
				t.draw(GL11.GL_QUADS);
			}
		}
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		Engine.pxStack.pop();
		
		FrameBuffer.unbindFramebuffer();
        glClearColor(0,0,0,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.textured.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        Engine.drawFullscreenQuad();
		Shaders.textured.enable();
        String str = String.format("%.2f %.2f", hexMouseOver.getCenterX(), hexMouseOver.getCenterY());
        fr.drawString(str, displayWidth/2, displayHeight-4, -1, true, 1, 2);
//		float scale = (float) this.zoom;
//		float newScale = (float) ((1/16f)*scale);
//    	this.mapOffsetX = this.mapOffsetZ = 0;
//    	double pixelPosNew=mX*(1/newScale)*biomes.width;
//		System.out.println(pixelPosNew);
	}
	public double mousePosX() {
        double mouseX=Mouse.getX()-rOffsetX*scaleWidth;
		double mX=(((mouseX/displayWidth)-0.5)*displayWidth) / scaleWidth;
		return mX*biomes.width;
	}
	public double mousePosY() {
        double mouseY=Mouse.getY()-rOffsetZ*scaleHeight;
		double mY=(((mouseY/displayHeight)-0.5)*displayHeight) / scaleHeight;
		return mY*biomes.height;
	}
	public void render2(float f) {
		Engine.getSceneFB().bind();
		Engine.getSceneFB().clearFrameBuffer();
		float scale2 = (float) ((1/16f)*zoom);
		float oneOverScale2 = 1/scale2;
		Tess t = Tess.instance;
		GL11.glLineWidth(2);
		Shaders.colored.enable();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		int hw = 160;
//		GL11.glScissor((int)GameMath.floor(left), (int)GameMath.floor(displayHeight-top-rHeight), (int)GameMath.floor(rWidth), (int)GameMath.floor(rHeight));
//		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		float centerPxX = rWidth/2.0f;
		float centerPxZ = rHeight/2.0f;
		float renderOffsetX = 0%1.0f;
		float renderOffsetZ = 0%1.0f;
		System.out.println(renderOffsetX);
		long leftTop = biomes.toHex(mapOffsetX-centerPxX*oneOverScale2, mapOffsetZ-centerPxZ*oneOverScale2);
		long bottomRight = biomes.toHex(mapOffsetX+(centerPxX)*oneOverScale2, mapOffsetZ+(centerPxZ)*oneOverScale2);
		HexCell hex1 = biomes.getPos(leftTop);
		HexCell hex2 = biomes.getPos(bottomRight);
		int len = 1;
		int minX = hex1.x;
		int maxX = hex2.x;
		minX-=1+Math.abs(hex1.z)/2;
		maxX+=1+Math.abs(hex2.z)/2;
//		minX -= ()
		int zOffset =0;// GameMath.floor(x/2.0);
		for (int z = hex1.z-zOffset-1; z <= hex2.z-zOffset+1; z++) {
			int xOffset = GameMath.floor(z/2.0);
			for (int x = minX-xOffset; x <= maxX-xOffset; x++){
				t.setColorF(0xff00ff, 1);
				t.setOffset(renderOffsetX, 0, renderOffsetZ);
				HexBiome hexHit = biomes.getPos(GameMath.toLong(x, z));

				int hx = x;
				int hz = z;
				for (int i = 0; i < 7; i++) {
					float fx = (float) biomes.getPointX(hx, hz, i%6)*scale2;
					float fy = (float) biomes.getPointY(hx, hz, i%6)*scale2;
					t.add(fx, fy);
				}
				t.draw(GL11.GL_LINE_STRIP);
				float cx = (float)biomes.getCenterX(hx, hz)*scale2;
				float cz = (float)biomes.getCenterY(hx, hz)*scale2;
				t.setOffset(renderOffsetX, 0, renderOffsetZ);
				t.setColorF(hexHit.biome.color, 1);
				t.add(cx, cz);
				for (int i = 6; i >= 0; i--) {
					float fx = (float) biomes.getPointX(hx, hz, i%6)*scale2;
					float fy = (float) biomes.getPointY(hx, hz, i%6)*scale2;
					t.add(fx, fy);
				}
				t.draw(GL11.GL_TRIANGLE_FAN);
			}
		}
		if (hit != null) {
			GL11.glLineWidth(2);
			long hexPos = biomes.toHex(hit.x*oneOverScale2, hit.z*oneOverScale2);
			HexCell hexHit = biomes.getPos(hexPos);
			float cx = (float)biomes.getCenterX(hexHit.x, hexHit.z)*scale2;
			float cz = (float)biomes.getCenterY(hexHit.x, hexHit.z)*scale2;
			t.setColorF(0xffff00, 1);
			t.setOffset(renderOffsetX, 0, renderOffsetZ);
			t.add(cx, cz);
			for (int i = 6; i >= 0; i--) {
				float fx = (float) biomes.getPointX(hexHit.x, hexHit.z, i%6)*scale2;
				float fy = (float) biomes.getPointY(hexHit.x, hexHit.z, i%6)*scale2;
				t.add(fx, fy);
			}
			t.draw(GL11.GL_TRIANGLE_FAN);
		}
		GL11.glLineWidth(2);

		t.setColorF(0x00ffff, 1);
		t.setOffset(renderOffsetX, 0, renderOffsetZ);
		t.add(0, 0,0);
		t.add(0, 0,22);
		t.draw(GL11.GL_LINE_STRIP);
		if (hit != null) {
			t.setColorF(0xffff00, 1);
			t.setOffset(renderOffsetX, 0, renderOffsetZ);
			t.add(hit.x*oneOverScale2, hit.z*oneOverScale2,0);
			t.add(hit.x*oneOverScale2, hit.z*oneOverScale2, 22);
			t.draw(GL11.GL_LINE_STRIP);
		}
		{
			t.setColorF(0xffffff, 1);
			t.add(left, top);
			t.add(left, top+rHeight);
			t.add(left+rWidth, top+rHeight);
			t.add(left+rWidth, top);
			t.add(left, top);
			t.draw(GL11.GL_LINE_STRIP);
		}
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		FrameBuffer.unbindFramebuffer();
//		HBAOPlus.renderAO();
        glClearColor(0,0,0,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.textured.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        Engine.drawFullscreenQuad();
	}


	/**
	 * @param x
	 * @param z
	 * @return
	 */
	private int getBlockColor(int x, int z) {
		HexBiome hex = (HexBiome) biomes.blockToHex(x, z);
		double dist = hex.getDistanceCenter(x, z);
		int rgb = hex.biome.colorFoliage;
		float innerScale = 0.75f;
		if (dist < biomes.hwidth*innerScale) {
			return rgb;
		}
		double centerX = hex.getCenterX();
		double centerY = hex.getCenterY();
		
		double dist2 = dist-biomes.hwidth*innerScale;
		double scale = dist2/(biomes.hwidth*(1-innerScale));
		float fscale = (float) (scale > 1 ? 1 : scale);
		int n = hex.getClosesCorner(x, z);
		float fx = (float) biomes.getPointX(hex.x, hex.z, n);
		float fy = (float) biomes.getPointY(hex.x, hex.z, n);
		double angle = GameMath.getAngle(x-centerX, z-centerY, fx-centerX, fy-centerY);
		double scaleTangent = angle/(Math.PI/6.0);
		
		int offset = 5-n;
		long k1 = HexagonGrid.offset(hex.x, hex.z, (offset+0)%6);
		HexBiome hex1 = (HexBiome) biomes.getPos(k1);
		long k2 = HexagonGrid.offset(hex.x, hex.z, (offset+1)%6);
		HexBiome hex2 = (HexBiome) biomes.getPos(k2);
		int rgbCorner = TextureUtil.mix3RGB(rgb, hex2.biome.colorFoliage, hex1.biome.colorFoliage);
		if (scaleTangent > 0) {
			int rgbT = TextureUtil.mixRGB(rgb, hex2.biome.colorFoliage, 0.5f);
			rgbCorner = TextureUtil.mixRGB(rgbCorner, rgbT, (float)scaleTangent);
//			return -1;
		} else if  (scaleTangent < 0) {
			int rgbT = TextureUtil.mixRGB(rgb, hex1.biome.colorFoliage, 0.5f);
			rgbCorner = TextureUtil.mixRGB(rgbCorner, rgbT, ((float)-scaleTangent));
		}
		rgbCorner = TextureUtil.mixRGB(rgb, rgbCorner, fscale);
		return rgbCorner;
	}


	private Vec3D tmpPos = new Vec3D();
	private Vector3f hit;
	private float left;
	private float top;
	private float rWidth;
	private float rHeight;
	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.update(movement);
		Vec3D.sub(this.cameraController.pos, this.cameraController.lastPos, this.tmpPos);
		this.tmpPos.scale(f);
		Vec3D.add(this.tmpPos, this.cameraController.lastPos, this.tmpPos);
		if (ortho) {
			left = 10;
			top = 10;
			rWidth = displayWidth - 20;
			rHeight = displayHeight - 20;
//			BufferedMatrix mat = Engine.getMatSceneP();
//			mat.setZero();
//			int scale = (int) (1024*(4)+zoom*128);
//			if (scale < 10)
//				scale = 10;
//			int wX = (int) (scale *(displayWidth/(float)displayHeight));
//			left = -wX/2;
//			top = -scale/2;
//			rHeight = scale;
//			rWidth = wX;
//			int out = (int) (scale*0.05);
//			left+=out;
//			top+=out;
//			rHeight-=out*2;
//			rWidth-=out*2;
//            Project.orthoMat(-wX/2, wX/2, -scale/2, scale/2, -100, 100, mat);
//            mat.update();
//	        Engine.camera.setPosition(this.tmpPos);
//	        Engine.camera.setOrientation(0, -90, false, 4.0f);
//	        Engine.updateCamera();
		} else {
	        Engine.camera.setPosition(this.tmpPos);
	        Engine.camera.setOrientation(this.cameraController.yaw, this.cameraController.pitch, false, 4.0f);   
		}
        Engine.updateCamera();
        UniformBuffer.updateUBO(null, f);
        float winX, winY;

        if (this.movement.grabbed()) {
            winX = (float) displayWidth/2.0F;
            winY = (float) displayHeight/2.0F;
        } else {
            winX = (float) Mouse.getX();
            winY = (float) (displayHeight-Mouse.getY());
            if (winX < 0) winX = 0; if (winX > displayWidth) winX = 1;
            if (winY < 0) winY = 0; if (winY > displayHeight) winY = 1;
        }
		hit = null;
		mouseOverRight.updateMouseFromScreenPos(winX, winY, displayWidth, displayHeight, null);
        if (mouseOverRight.vDir != null) {
        	Vector3f tmp = new Vector3f(0, 1, 0); //plane normal
        	
        	//ray-plane intersection (plane = y axis at 0 aka ground)
        	double t = - (Vector3f.dot(tmp, mouseOverRight.vOrigin)) / (Vector3f.dot(tmp, mouseOverRight.vDir));
        	if (t > 0) {
        		tmp.set(mouseOverRight.vDir);
        		tmp.scale((float) t);
        		tmp.addVec(mouseOverRight.vOrigin);
        		hit = tmp;
        	} else {
//        		System.err.println("nope");
        	}
        }

	}

	@Override
	public void postRenderUpdate(float f) {
	}
	
	boolean hadContext = false;
	private int tex;
	private double zoom = 4;
	private boolean diddrag;
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
			if (sceneFB != null) sceneFB.release();
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
		renderQuad();
		initShaders();
		int[] tex = new int[2*2];
		for (int i = 0; i < tex.length; i++) {
			if ((i&1)==((i>>1)&1)) {
				tex[i] = 0xFFBBBBBB;
			} else {
				tex[i] = 0xFF333333;
			}
			tex[i] = 0xFFBBBBBB;
		}
		byte[] rgba = TextureUtil.toBytesRGBA(tex);
		this.tex = TextureManager.getInstance().makeNewTexture(rgba, 2, 2, true, false, 0, GL11.GL_RGBA);
		updateOffset();
	}

	/**
	 * 
	 */
	private void renderQuad() {
		int w = 1;
		float texScale = 0.125f;
//		Tess.instance.setColor(-1, 0xff);
//		Tess.instance.setOffset(0, 0, 0);
//		Tess.instance.setNormals(0, 1, 0);
//		Tess.instance.add(-w, 0, -w, 0, 0);
//		Tess.instance.add(-w, 0, w, 0, 1);
//		Tess.instance.add(w, 0, w, 1, 1);
//		Tess.instance.add(w, 0, -w, 1, 0);
//		Tess.instance.draw(GL_QUADS, this.tessState);
	}

	@Override
    public void input(float fTime) {
        double mdX = Mouse.getDX();
        double mdY = Mouse.getDY();
        this.movement.update(mdX, -mdY);
        if (drag&&!this.movement.grabbed()&&(mdX*mdX+mdY*mdY)!=0) {
    		float scale2 = 1;
    		diddrag=true;
        	this.mapOffsetX+=scale2*mdX;
        	this.mapOffsetZ+=scale2*mdY;
    		updateOffset();
        }
    }
	public double mousePosX2() {
        double mouseX=Mouse.getX()-rOffsetX*scaleWidth;
		double mX=(((mouseX/displayWidth)-0.5)*displayWidth) / scaleWidth;
		return mX;
	}
	public double mousePosY2() {
        double mouseY=Mouse.getY()-rOffsetZ*scaleHeight;
		double mY=(((mouseY/displayHeight)-0.5)*displayHeight) / scaleHeight;
		return mY;
	}
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		float pScale = (float) this.zoom;
		float scale = (float) this.zoom;
		scale*=yoffset>0?1.05f:0.95f;
		if (scale < 1/4.0f)
			scale = 1/4.0f;
		if (scale  > 128)
			scale = 128;
		
		if (pScale != scale) {
			double rx = xPos1;
			double rz = zPos1;
			double mxw = mousePosX2();
			double myw = mousePosY2();
			this.zoom = scale;
			update = true;
			updateOffset();
			double mxw2 = mousePosX2();
			double myw2 = mousePosY2();
			double distX = xPos1-rx;
			double distZ = zPos1-rz;
			mapOffsetX+=(mxw2-mxw-(distX+distZ*0.5)) * scaleWidth;
			mapOffsetZ+=(myw2-myw-(distZ)) * scaleHeight;
			System.out.println(distX+","+distZ);
			 rx = xPos1;
			 rz = zPos1;
			updateOffset();
        }
	}

}
