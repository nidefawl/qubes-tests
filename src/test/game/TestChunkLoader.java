package test.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

import java.io.File;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.RenderAssets;
import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.*;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.chunk.server.ChunkReader;
import nidefawl.qubes.chunk.server.RegionFileCache;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.models.BlockModelManager;
import nidefawl.qubes.models.ItemModelManager;
import nidefawl.qubes.render.RenderersGL;
import nidefawl.qubes.render.gui.SingleBlockRenderAtlas;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.IChunkWorld;
/** Loads chunks from region files of an already generated world and renders them using forward rendering */
public class TestChunkLoader extends GameBase {
	final CameraController cameraController = new CameraController();

	public static class TestChunkManager extends ChunkManager {

	    public Chunk testChunk;
	    ChunkReader reader;
	    private RegionFileCache regionFileCache;
		public TestChunkManager(IChunkWorld world) {
			super(world);
	        this.regionFileCache = new RegionFileCache(new File("../Game/worlds/world_single_biome/data/"));
	        this.reader = new ChunkReader(this.regionFileCache);
	        this.testChunk = new Chunk(world, 0, 0);
	        this.testChunk.checkIsEmtpy();
		}

	    @Override
	    public Chunk get(int x, int z) {
	        Chunk c = this.table.get(x, z);
	        if (c == null) {
		    	if (Math.abs(x*x+z*z)<32) 
		    	{
		        	c = this.reader.loadChunk(this.world, x, z);
		    	}
	        	if (c == null)
	        		c = this.testChunk;
	            this.table.put(x, z, c);
	        }
	        return c;
	    }

	    @Override
	    protected ChunkTable makeChunkTable() {
	        return new ChunkTable(MAX_CHUNK*2);
	    }

	    /**
	     * @param x
	     * @param z
	     */
	    public void remove(int x, int z) {
	        Chunk c = this.table.remove(x, z);
	        if (c != null) {
	            c.isValid = false;
	        }
	    }
	}


	public static class TestBlockWorld implements IChunkWorld {

	    public final int worldHeight;
	    public final int worldHeightMinusOne;
	    public final int worldHeightBits;
	    public final int worldHeightBitsPlusFour;
	    public final int worldSeaLevel;
	    private final ChunkManager chunkMgr;
		private String name;

	    public ChunkManager makeChunkManager() {
	    	return new TestChunkManager(this);
	    }

	    public TestBlockWorld() {
	        this.name = "test";
	        this.worldHeightBits = 8;
	        this.worldHeightBitsPlusFour = worldHeightBits + 4;
	        this.worldHeight = 1 << worldHeightBits;
	        this.worldHeightMinusOne = (1 << worldHeightBits) - 1;
	        this.worldSeaLevel = 59;//1 << (worldHeightBits - 1);
	        this.chunkMgr = makeChunkManager();
//	        this.generator = new TerrainGenerator2(this, this.seed);

	    }
	    public Chunk getChunk(int x, int z) {
	        return chunkMgr.get(x, z);
	    }
		@Override
		public int getType(int x, int y, int z) {
	        if (y >= this.worldHeight)
	            return 0;
	        if (y < 0)
	            return 0;
	        Chunk c = getChunk(x >> 4, z >> 4);
	        if (c == null) {
	            return 0;
	        }
	        return c.getTypeId(x & 0xF, y, z & 0xF);
        }

		@Override
		public boolean setType(int x, int y, int z, int type, int flags) {
	        if (y >= this.worldHeight)
	            return false;
	        if (y < 0)
	            return false;
	        Chunk c = getChunk(x >> 4, z >> 4);
	        if (c == null) {
	            return false;
	        }
	        if (c.setType(x & 0xF, y, z & 0xF, type)) {
	            if ((flags & Flags.LIGHT) != 0) {
	                updateLight(x, y, z);
	            }
	            if ((flags & Flags.MARK) != 0) {
	                flagBlock(x, y, z);
	            }
	            updateBlocks(x, y, z, type, flags);
	        }
	        return true;
	    }
	    public void updateLight(int x, int y, int z) {
	        
	    }
	    public void updateBlocks(int x, int y, int z, int type, int flags) {
//	        for (int i = 0; i < 6; i++) {
//	            int x1 = x+Dir.getDirX(i);
//	            int y1 = y+Dir.getDirY(i);
//	            int z1 = z+Dir.getDirZ(i);
//	            getBlock(x1, y1, z1).onUpdate(this, x1, y1, z1, Dir.opposite(i));
//	        }
	    }
	    public void flagBlock(int x, int y, int z) {
	        Engine.regionRenderer.flagBlock(x, y, z);
	    }

	    public Block getBlock(int x, int y, int z) {
	        int b = getType(x, y, z);
	        if (!Block.isValid(b)) {
	            return Block.air;
	        }
	        return Block.get(b);
	    }

		@Override
		public int getHeight(int x, int z) {
	        Chunk c = this.getChunk(x>>Chunk.SIZE_BITS, z>>Chunk.SIZE_BITS);
	        if (c != null) {
	            return c.getTopBlock(x&Chunk.MASK, z&Chunk.MASK);
	        }
	        return 0;
		}

		@Override
		public boolean setData(int x, int y, int z, int type, int render) {
	        if (y >= this.worldHeight)
	            return false;
	        if (y < 0)
	            return false;
	        Chunk c = getChunk(x >> 4, z >> 4);
	        if (c == null) {
	            return false;
	        }
	        if (c.setData(x & 0xF, y, z & 0xF, type)) {
	            if ((render & Flags.LIGHT) != 0) {
	                updateLight(x, y, z);
	            }
	            if ((render & Flags.MARK) != 0) {
	                flagBlock(x, y, z);
	            }   
	        }
	        return true;
	    }

		@Override
		public int getData(int x, int y, int z) {
	        if (y >= this.worldHeight)
	            return 0;
	        if (y < 0)
	            return 0;
	        Chunk c = getChunk(x >> 4, z >> 4);
	        if (c == null) {
	            return 0;
	        }
	        return c.getData(x & 0xF, y, z & 0xF);
	    }

		@Override
		public boolean isNormalBlock(int ix, int iy, int iz, int offsetId) {
	        if (offsetId < 0) {
	            offsetId = this.getType(ix, iy, iz);
	        }
	        return Block.get(offsetId).isNormalBlock(this, ix, iy, iz);
	    }

		@Override
		public boolean setTypeData(int x, int y, int z, int type, int data, int render) {
	        if (y >= this.worldHeight)
	            return false;
	        if (y < 0)
	            return false;
	        Chunk c = getChunk(x >> 4, z >> 4);
	        if (c == null) {
	            return false;
	        }
	        if (c.setTypeData(x & 0xF, y, z & 0xF, type, data)) {
	            updateLight(x, y, z);
	            if ((render & Flags.MARK) != 0) {
	                flagBlock(x, y, z);
	            }   
	            updateBlocks(x, y, z, type, render);
	        }
	        return true;
	    }

		@Override
		public int getLight(int i, int j, int k) {
	        Chunk c = getChunk(i >> Chunk.SIZE_BITS, k >> Chunk.SIZE_BITS);
	        if (c == null)
	            return 0;
	        return c.getLight(i & Chunk.MASK, j, k & Chunk.MASK);
	    }

		@Override
		public BlockData getBlockData(int x, int y, int z) {
	        if (y >= this.worldHeight)
	            return null;
	        if (y < 0)
	            return null;
	        Chunk c = getChunk(x >> 4, z >> 4);
	        if (c == null) {
	            return null;
	        }
	        return c.getBlockData(x & 0xF, y, z & 0xF);
	    }

		@Override
		public boolean setBlockData(int x, int y, int z, BlockData bd, int flags) {
	        if (y >= this.worldHeight)
	            return false;
	        if (y < 0)
	            return false;
	        Chunk c = getChunk(x >> 4, z >> 4);
	        if (c == null) {
	            return false;
	        }
	        if (c.setBlockData(x & 0xF, y, z & 0xF, bd)) {
	            if ((flags & Flags.LIGHT) != 0) {
	                updateLight(x, y, z);
	            }
	            if ((flags & Flags.MARK) != 0) {
	                flagBlock(x, y, z);
	            }
	            updateBlocks(x, y, z, c.getTypeId(x, y, z), flags);
	        }
	        return true;
	    }

		@Override
		public Biome getBiome(int i, int k) {
	        Chunk c = getChunk(i >> Chunk.SIZE_BITS, k >> Chunk.SIZE_BITS);
	        if (c == null)
	            return Biome.MEADOW_GREEN;
	        return c.getBiome(i & Chunk.MASK, k & Chunk.MASK);
	    }

		@Override
		public int getBiomeFaceColor(int x, int y, int z, int faceDir, int pass, BiomeColor colorType) {
	        return getBiome(x, z).getFaceColor(colorType);
        }

		@Override
		public int getWater(int x, int y, int z) {
	        if (y >= this.worldHeight)
	            return 0;
	        if (y < 0)
	            return 0;
	        Chunk c = getChunk(x >> 4, z >> 4);
	        if (c == null) {
	            return 0;
	        }
	        return c.getWater(x & 0xF, y, z & 0xF);
	    }

		@Override
		public void updateLightHeightMap(Chunk chunk, int i, int k, int min, int max, boolean add) {
		}

		@Override
		public void flagChunkLightUpdate(int x, int z) {
		}

		@Override
		public String getName() {
	        return this.name;
		}

		@Override
		public int getHeightBits() {
	        return this.worldHeightBits;
		}

	}


	private TestBlockWorld world;
	private FrameBuffer sceneFB;


	public TestChunkLoader() {
		TICKS_PER_SEC = 20;
	}
	public static void main(String[] args) {
        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TestChunkLoader().startGame();
	}
	

	@Override
	public void onStatsUpdated() {
		System.out.println(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
	}

	@Override
	protected void onTextInput(long window, int codepoint) {


	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {


	}
	
	@Override
	public void render(float f) {
		Engine.setZBufferSetting();
		setSceneViewport();
        sceneFB.bind();
        sceneFB.clearFrameBuffer();
		RenderersGL.worldRenderer.renderTerrain(f);
		Shader.disable();
		Engine.restoreZBufferSetting();
		FrameBuffer.unbindFramebuffer();
		
		setWindowViewport();
		GL11.glClearColor(1, 0, 1, 1);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        Shaders.textured.enable();
        GL.bindTexture(GL_TEXTURE0, GL11.GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        Engine.drawFullscreenQuad();
        
        
        Engine.checkGLError("end frame");
	}

	@Override
	public void preRenderUpdate(float f) {

        Engine.regionRenderer.rendered = 0;
        Vector3f renderPos = this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
        Engine.camera.setPosition(renderPos);
        Engine.updateGlobalRenderOffset(renderPos);
        Engine.updateFrustumFromInternal();
        Engine.updateCamera();
//        Engine.updateShadowProjections(f);
        Engine.getSunLightModel().setTime(5850);
        Engine.getSunLightModel().updateFrame(f);
        Engine.setLightPosition(Engine.getSunLightModel().getLightPosition());
        UniformBuffer.updateUBO(null, f);

        
        
        if (this.world != null) {
            float renderRegionX = renderPos.x;
//          float renderRegionY = follow ? py : lastCamY;
          float renderRegionZ = renderPos.x;
          int xPosP = GameMath.floor(renderRegionX)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
          int zPosP = GameMath.floor(renderRegionZ)>>(Chunk.SIZE_BITS+RegionRenderer.REGION_SIZE_BITS);
          if (Engine.updateRenderOffset) {
              Engine.regionRenderer.reRender();
          }
          Engine.regionRenderer.update(this.world, this.world.chunkMgr, renderPos.x, renderPos.y, renderPos.z, xPosP, zPosP, f);
//          Engine.lightCompute.updateLights(this.world, f);
//          Engine.worldRenderer.prepareEntitiesBatched(this.world, f);
        
        }
        Engine.regionRenderer.tickUpdate();
	}

	@Override
	public void postRenderUpdate(float f) {
        Engine.worldRenderer.rendered = Engine.regionRenderer.rendered;


	}
	
	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
			if (sceneFB != null) sceneFB.destroy();
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
        }
//        glActiveTexture(GL_TEXTURE0);
	}
	@Override
	public void tick() {
		if (!isStarting) {
//			System.out.println(cameraController.pos+","+cameraController.yaw+","+cameraController.pitch);
//Vec3[35.77761695937934, 241.20666952796537, -67.74502838277994],211.31967,41.999985
			this.cameraController.tickUpdate();
		}

	}

	@Override
	public void initGame() {
	    loadSettings();
		EngineInitSettings settings = EngineInitSettings.INIT_NONE;
		settings.initWorldRenderer=true;
		settings.initShadowRenderer=true;
		settings.initShadowProj=true;
		settings.setFBSize(windowWidth, windowHeight);
        Engine.init(settings);
        if (!Engine.isVulkan) {
            TextureManager.getInstance().init();
        }
        BlockModelManager.getInstance().init();
        ItemModelManager.getInstance().init();
        SingleBlockRenderAtlas.getInstance().init();
        this.world = new TestBlockWorld();
		TextureManager.getInstance().init();
		setVSync(false);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
//		35.77761695937934, 241.20666952796537, -67.74502838277994],211.31967,41.999985
		this.cameraController.set(35.7f, 241.2f, -67.74f, 42.0f, 211.31f);
	}

	@Override
	public void lateInitGame() {
        RenderAssets.load(this.settings.renderSettings, loadingScreen);
		Engine.enableDepthMask(true);
		Engine.setDepthFunc(GL11.GL_LEQUAL);

		GL11.glEnable(GL11.GL_DEPTH_TEST);

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
        
        Engine.setBlend(false);
        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        for (int i = 0; i < 3; i++) {
            GL40.glBlendFuncSeparatei(1+i, GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);
        }
		
	}


	/* (non-Javadoc)
	 * @see nidefawl.qubes.GameBase#onWheelScroll(long, double, double)
	 */
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {

		
	}

}
