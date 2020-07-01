package test.game;

import static org.lwjgl.opengl.GL11.*;

import java.io.*;
import java.util.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;

public class Test2DArrayMesh extends GameBase {
	public static class HeightMesh2D {
		int x, z, w, h;
		private int t;
		public boolean consumed;
		public HeightMesh2D(int t, int x, int z, int w, int h) {
			this.t = t;
			this.x = x;
			this.z = z;
			this.w = w;
			this.h = h;
		}
	}

	final static float SC = 8.0f;
	final static float inset = SC/16.0f;
	final static int LOAD = 8;
	public static class Ch2D {

		private int[] data;
		private int x;
		private int z;
		private ArrayList<HeightMesh2D> meshes = new ArrayList<>();

		public Ch2D(int x, int z, int[] chunk1) {
			this.x = x;
			this.z = z;
			this.data = chunk1;
		}
		public void render(Test2DArrayMesh p) {
			float posX = SC*(this.x*16);
			float posZ = SC*(this.z*16);
			Tess.instance.setOffset(posX, posZ, 0);
			int min = 255;
			int max = 0;
			for (int _z = 0; _z < 16; _z++) {
				for (int _x = 0; _x < 16; _x++) {
					int v = p.rangeVal(this.data[_z*16+_x]);
					min = Math.min(min, v);
					max = Math.max(max, v);
					int rgba = v<<16|v;
					Tess.instance.setColorF(rgba, 0.3f);
					Tess.instance.add(_x*SC+SC-inset, _z*SC+inset);
					Tess.instance.add(_x*SC+inset, _z*SC+inset);
					Tess.instance.add(_x*SC+inset, _z*SC+SC-inset);
					Tess.instance.add(_x*SC+SC-inset, _z*SC+SC-inset);
				}
			}
			Tess.instance.drawQuads();
//			Tess.instance.setColorF(0xff00ff, 1.0f);
//			Tess.instance.setOffset(0, 0, 0);
//			for (int i = 0; i < this.meshes.size(); i++) {
//				Mesh n = this.meshes.get(i);
//				int br = n.t&0xFF;
//				int rgba = br<<16|br<<8|br;
//				Tess.instance.setColorF(rgba, 1.0f);
//				Tess.instance.add((n.x+n.w)*SC-inset, (n.z)*SC+inset, 0);
//				Tess.instance.add((n.x)*SC+inset, (n.z)*SC+inset, 0);
//				Tess.instance.add((n.x)*SC+inset, (n.z+n.h)*SC-inset, 0);
//				Tess.instance.add((n.x+n.w)*SC-inset, (n.z+n.h)*SC-inset, 0);
//			}
//			Tess.instance.drawQuads();
			Tess.instance.setOffset(0, 0, 0);
		}

		public void mesh() {
			this.meshes.clear();
	        boolean[] mask = new boolean[16*16];
            int br1 = data[0];
            boolean fail = false;
	        for (int z = 0; z < 16; z++) {
	            for (int x = 0; x < 16; x++) {
	                int br = data[z*16+x];
	            	if (br != br1) {
	            		fail=true;
	            	}
	            }
	        }
//	        if (!fail) {
//                meshes.add(new HeightMesh2D(br1&0xFF, 0+this.x*16, 0+this.z*16, 16, 16));
//                System.out.println("1");
//	        	return;
//	        }
	        for (int z = 0; z < 16; z++) {
	            for (int x = 0; x < 16;) {
	                int br = data[z*16+x];
	                if (mask[z*16+x]) {
	                    x++;
	                    continue;
	                }
	                int x2 = x+1;
	                for (; x2 < 16; x2++) {
	                    if (mask[z*16+x2] || data[z*16+x2] != br) {
	                        break;
	                    }
	                }
	                int z2 = z+1;
	                boolean b = false;
	                int zf = z+1;
	                for (; !b && z2 < 16; z2++) {
	                    for (int x3 = x; !b && x3 < x2; x3++) {
	                        if (mask[z2*16+x3] || data[z2*16+x3] != br) {
	                            b = true;
	                            break;
	                        }
	                    }
	                    if (b) break;
	                    zf = z2+1;
	                }
	                meshes.add(new HeightMesh2D(br&0xFF, x+this.x*16, z+this.z*16, x2-x, zf-z));
	                for (int x4 = x; x4 < x2; x4++) {
	                    for (int z4 = z; z4 < zf; z4++) {
	                        mask[z4*16+x4] = true;
	                    }
	                }
	                x=x2;
	            }
	        }
	    }
			
	}
	int getHeight(int x, int z) {
		int cx = x>>4;
		int cz = z>>4;
        Ch2D c = this.chunks[cx*LOAD+cz];
        int bx = x&0xF;
        int bz = z&0xF;
        return c.data[bz*16+bx];
	}
	private void mesh(int _x, int _z, int d) {
		int blocks = (d*2+1)*16;
        boolean[] mask = new boolean[blocks*blocks];
        int offsetX = (_x-d)*16;
        int offsetZ = (_z-d)*16;
        for (int z = 0; z < blocks; z++) {
            for (int x = 0; x < blocks;) {
                int br = getHeight(offsetX+x, offsetZ+z);
                if (mask[z*blocks+x]) {
                    x++;
                    continue;
                }
                /*

                int xExtend = x+1;
                int zExtend = z+1;
                int maxX = xExtend;
                int maxZ = zExtend;
                int maxA = 0;
                for (; xExtend < blocks; xExtend++) {
                    if (mask[z*blocks+xExtend] || getHeight(offsetX+xExtend, offsetZ+z) != br) {
                        break;
                    }
//                    if (maxA == 0 || xExtend*zExtend > maxA && (Math.abs(xExtend-zExtend)<10)) {
//                    	maxA = xExtend*zExtend;
//                    	maxX = xExtend;
//                    	maxZ = zExtend;
//                	}
                }
//                xExtend = maxX;
                zExtend = maxZ;
                    zExtend = getZExtend(mask, blocks, x, z, xExtend, offsetX, offsetZ, br);*/
                int xExtend = x+1;
                int zExtend = z+1;
                int maxX = xExtend;
                int maxZ = zExtend;
                int maxA = 0;
                for (; xExtend < blocks; xExtend++) {
                    if (mask[z*blocks+xExtend] || getHeight(offsetX+xExtend, offsetZ+z) != br) {
                        break;
                    }
                    zExtend = getZExtend(mask, blocks, x, z, xExtend, offsetX, offsetZ, br);
                    if (maxA == 0 || xExtend*zExtend > maxA) {
                    	maxA = xExtend*zExtend;
                    	maxX = xExtend;
                    	maxZ = zExtend;
                	}
                }
                xExtend = maxX;
                zExtend = maxZ;
                meshes.add(new HeightMesh2D(br&0xFF, x+offsetX, z+offsetZ, xExtend-x, zExtend-z));
                for (int x4 = x; x4 < xExtend; x4++) {
                    for (int z4 = z; z4 < zExtend; z4++) {
                        mask[z4*blocks+x4] = true;
                    }
                }
                x=xExtend;
            }
        }
	}

	private int getZExtend(boolean[] mask, int blocks, int x, int z, int xExtend, int offsetX, int offsetZ, int br) {
        int zBegin = z+1;
        boolean b = false;
        int zExtend = z+1;
        for (; !b && zBegin < blocks; zBegin++) {
            for (int x3 = x; !b && x3 < xExtend; x3++) {
                if (mask[zBegin*blocks+x3] || getHeight(offsetX+x3, offsetZ+zBegin) != br || ((zBegin)-z)>16) {
                    b = true;
                    break;
                }
            }
            if (b) break;
            zExtend = zBegin+1;
        }
        return zExtend;
	}

	final CameraController cameraController = new CameraController();
	Ch2D[] chunks = new Ch2D[LOAD*LOAD];

	public Test2DArrayMesh() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new Test2DArrayMesh().startGame();
	}
	

	int a = 0;
	private int[] map;
	private ArrayList<HeightMesh2D> meshes;
	@Override
	public void onStatsUpdated() {
		String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")";
//		System.out.println();
		a++;
		if (a > 12) {
			setTitle(stats);

        	a = 0;
		}
		rebuild();
		
	}

	private boolean merge(ArrayList<HeightMesh2D> meshes) {
        int merged = 0;
//        for (int i = 0; i < meshes.size(); i++) {
//            HeightMesh2D m = meshes.get(i);
//            if (m.consumed)
//                continue;
//            for (int j = 0; j < meshes.size(); j++) {
//                HeightMesh2D n = meshes.get(j);
//                if (n.consumed)
//                    continue;
//                if (Math.abs(n.t-m.t) > 0)
//                    continue;
//                if (n.x+n.w==m.x&&n.h==m.h&&n.w==m.w&&m.z==n.z) {
//                    n.w=m.w+n.w;
//                    n.h=Math.max(m.h, n.h);
//                    merged++;
//                    m.consumed=true;
//                    break;
//                }
//            }
//        }
        for (int i = 0; i < meshes.size(); i++) {
            HeightMesh2D m = meshes.get(i);
            if (m.consumed)
                continue;
            for (int j = 0; j < meshes.size(); j++) {
                HeightMesh2D n = meshes.get(j);
                if (n.consumed)
                    continue;
                if (Math.abs(n.t-m.t) > 0)
                    continue;
                if (n.w<54&&n.x+n.w==m.x&&(Math.abs(m.h-n.h)<2)&&(Math.abs(m.z-n.z)<1)) {
                    n.w=m.w+n.w;
                    n.h=Math.max(m.h, n.h);
                    merged++;
                    m.consumed=true;
                    break;
                }
                if (n.h<54&&n.z+n.h==m.z&&(Math.abs(m.w-n.w)<2)&&(Math.abs(m.x-n.x)<1)) {
                    n.h=m.h+n.h;
                    n.w=Math.max(m.w, n.w);
                    merged++;
                    m.consumed=true;
                    break;
                }  
                     
            }
        }
        for (int i = 0; i < meshes.size(); i++) {
            HeightMesh2D m = meshes.get(i);
            if (m.consumed)
                meshes.remove(i--);
        }
        return merged>0;
    }
	@Override
	protected void onTextInput(long window, int codepoint) {
	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
	}


	@Override
	public void render(float f) {
        glClearColor(0f,0f,0f,0);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		Shaders.colored.enable();
		
		Tess.instance.setOffset(0, 0, 0);
		Tess.instance.setColorF(0xff00ff, 1.0f);
		for (int i = 0; i < this.meshes.size(); i++) {
			HeightMesh2D n = this.meshes.get(i);
			int br = n.t&0xFF;
			int rgba = br<<16|br<<8|br;
			Tess.instance.setColorF(rgba, 1.0f);
			Tess.instance.add((n.x+n.w)*SC-inset, (n.z)*SC+inset, 0);
			Tess.instance.add((n.x)*SC+inset, (n.z)*SC+inset, 0);
			Tess.instance.add((n.x)*SC+inset, (n.z+n.h)*SC-inset, 0);
			Tess.instance.add((n.x+n.w)*SC-inset, (n.z+n.h)*SC-inset, 0);
		}
		Tess.instance.drawQuads();
        glClear(GL11.GL_DEPTH_BUFFER_BIT);
		Engine.setBlend(true);
		for (int x = 0; x < LOAD; x++) {
			for (int z = 0; z < LOAD; z++) {
				Ch2D n = this.chunks[x*LOAD+z];
				n.render(this);
			}
			
		}
        glClear(GL11.GL_DEPTH_BUFFER_BIT);
		Shaders.textured.enable();
		FontRenderer fr = FontRenderer.get(0, 8, 0);
		for (int i = 0; i < this.meshes.size(); i++) {
			HeightMesh2D n = this.meshes.get(i);
			if (n.w>2) {
				float y1 = n.z*SC+fr.getLineHeight()-3;
				float x2 = fr.drawString(String.format("%dx%d", n.w, n.h), n.x*SC, y1, -1, true, 1.0f);
				int x = (int) (n.x*SC);
				if (n.h>1) {
					y1+=fr.getLineHeight();
				} else {
					x += (int) x2+ 10;
				}
				fr.drawString(String.format("%d", n.t), x, y1, -1, true, 1.0f);
			}
		}
		Engine.setBlend(false);
		
	}

	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
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
        }
	}

	/**
	 * 
	 */

	@Override
	public void tick() {
		this.cameraController.tickUpdate();
	}
	
	@Override
	public void initGame() {
        Engine.init(windowWidth, windowHeight);
		TextureManager.getInstance().init();
		FontRenderer.init();
		setVSync(true);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		this.cameraController.set(-3.93f, 2.21f, 0.13f, 25.3f, 89.6f);
		FrameBuffer.unbindFramebuffer();
		glEnable(GL_DEPTH_TEST);
		Engine.setBlend(true);
	}

	public int[] readChunk(int x, int z) {
//		int bits = this.map.getBits();
//		int comps = this.map.getComponents();
//		if (bits != 16) {
//			throw new GameError("Unsupported bitdepth in height map");
//		}
//		if (comps != 1) {
//			throw new GameError("Unsupported number of color components in height map");
//		}
//		short[] data = this.map.getUShortData();
//
//		int w = this.map.getWidth();
//		int h = this.map.getHeight();
		int w = 512; 
		int h = w;
		int[] data = this.map;
		int[] out = new int[16*16];
		int x_px = x*16;
		int z_px = z*16;
		x_px = (((x_px % w) + w) % w);
		z_px = (((z_px % h) + h) % h);
		for (int _z = z_px; _z < z_px+16; _z++) {
			for (int _x = x_px; _x < x_px+16; _x++) {
				int val = data[_x+_z*w];
				min = Math.min(min, val);
				max = Math.max(max, val);
//				val = Math.min(255, Math.max(0, ((val-5000)*2)>>8));
				if (val == 34) {
					System.exit(1);;
				}
				out[(_x-x_px)+(_z-z_px)*16] = val;
			}
		}
		return out;
	}
	int min = 256;
	int max = 0;
	@Override
	public void lateInitGame() {
//		this.map = AssetManager.getInstance().loadPNGAsset("textures/brushes/hillMound/hillMound_3.png");
		int data[] = new int[512*512];
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(new File("U:\\mc\\bin\\heightmap.dat"))));	
			for (int x = 0; x < 512; x++)
				for (int y = 0; y < 512; y++) {
					int v = in.readInt();
					data[y*512+x] = v;
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.map = data;
		rebuild();
	}
	private void rebuild() {
		for (int x = 0; x < LOAD; x++) {
			for (int z = 0; z < LOAD; z++) {
				Ch2D ch = new Ch2D(x, z, readChunk(14+x, 14+z));
				chunks[x*LOAD+z] = ch;
			}
		}
		ArrayList<HeightMesh2D> meshes = new ArrayList<>();
		for (int x = 0; x < LOAD; x++) {
			for (int z = 0; z < LOAD; z++) {
				chunks[x*LOAD+z].mesh();
				meshes.addAll(chunks[x*LOAD+z].meshes);
			}
		}
		this.meshes = meshes;
//		this.meshes = new ArrayList<>();
//		mesh(3, 3, 3);
		while(merge(meshes));
	}

	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
	}
	int rangeVal(int v) {
		int min = Math.max(0, this.min - 1);
		float fscaled = (v-min) / (float) (max-min);

		int scaled = GameMath.clampI(GameMath.floor(fscaled * 255.0f), 0, 255);
		return scaled;
	}
}
