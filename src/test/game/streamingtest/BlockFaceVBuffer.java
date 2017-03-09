package test.game.streamingtest;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.VkCommandBuffer;

import nidefawl.qubes.gl.ReallocIntBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vulkan.VKContext;
import nidefawl.qubes.vulkan.VkBuffer;

public class BlockFaceVBuffer {
    protected static ReallocIntBuffer[] buffers = new ReallocIntBuffer[NUM_PASSES*4];
    protected static ReallocIntBuffer[] idxShortBuffers = new ReallocIntBuffer[NUM_PASSES*4];
    public static long totalBytes = 0;
    public static long totalBytesPass[] = new long[NUM_PASSES];
    static int nextBuffer = 0;
    static final int REGION_DIST = 0;
	int size = 1;

	BlockFaceAttr attr = new BlockFaceAttr(); 
	private VertexBuffer bufferDataVertex;
    public int[]     vertexCount   = new int[NUM_PASSES];
    public int[]     elementCount   = new int[NUM_PASSES];
    public boolean[] hasPass       = new boolean[NUM_PASSES];
    public boolean hasAnyPass;
    private int shadowDrawMode;
    long alloc[] = new long[NUM_PASSES];
	private VkBuffer[] vkbuffersI;
	private VkBuffer[] vkbuffersV;
	public BlockFaceVBuffer() {
	}
	
	public void init(VKContext ctxt) {

		this.bufferDataVertex = new VertexBuffer(1024 * 1024);
		this.vkbuffersV = new VkBuffer[NUM_PASSES];
		this.vkbuffersI = new VkBuffer[NUM_PASSES];
		for (int i = 0; i < NUM_PASSES; i++) {
			this.vkbuffersV[i] = new VkBuffer(ctxt).tag("blockface_"+i+"_vertex");
			this.vkbuffersI[i] = new VkBuffer(ctxt).tag("blockface_"+i+"_index");
		}
	}

	public void redraw() {
		int k =4;
		int r = 1;
		int n = 0;
        int rOffset = (k*2+1);
        bufferDataVertex.reset();
		for (int x = -REGION_DIST; x <= REGION_DIST; x++) {
			for (int z = -REGION_DIST; z <= REGION_DIST; z++) {
				int rX = x*rOffset*r*1;
				int rZ = z*rOffset*r*1;
				for (int i = -k; i <= k; i++) {
					for (int j = -k; j <= k; j++) {
						drawFace(rX+i*r, 0, rZ+j*r, n++%9);
			        	attr.put(bufferDataVertex);
					}
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

    public void uploadBuffer(int pass, VertexBuffer buffer, int shadowDrawMode) {
        int numV = buffer.getVertexCount();
        boolean wasEmpty = this.vertexCount[pass] == 0;
        boolean isEmpty = buffer.getVertexCount() == 0;
        if (wasEmpty && isEmpty) {
            return;
        }
        this.vertexCount[pass] = numV;
        this.hasPass[pass] |= numV > 0;
        this.hasAnyPass |= numV > 0;
        this.shadowDrawMode = shadowDrawMode;
        int bufIdx = (nextBuffer++) % 4;
        ReallocIntBuffer buf = buffers[bufIdx * 4 + pass];
        ReallocIntBuffer shBuffer = idxShortBuffers[pass];
        int intlen = buffer.storeVertexData(buf);
        int intlenIdx = buffer.storeIndexData(shBuffer);
        this.elementCount[pass] = intlenIdx;

        if (this.vkbuffersV[pass].getSize() <= intlen * 4L) {
            System.out.println("Remake vbuffer with size "+(intlen * 4L));
            this.vkbuffersV[pass].destroy();
            this.vkbuffersV[pass].create(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, intlen * 4L, true);
        }
        if (this.vkbuffersI[pass].getSize() <= intlenIdx * 4L) {
            System.out.println("Remake ibuffer with size "+(intlenIdx * 4L));
            this.vkbuffersI[pass].destroy();
            this.vkbuffersI[pass].create(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, intlenIdx * 4L, true);
        }
        vkbuffersV[pass].upload(buf.getByteBuf(), 0);
        vkbuffersI[pass].upload(shBuffer.getByteBuf(), 0);

        int byteSize = (intlenIdx * 4) + (intlen * 4);
        
        if (alloc[pass] != byteSize) {
            totalBytes -= this.alloc[pass];
            totalBytesPass[pass] -= this.alloc[pass];
            this.alloc[pass] = byteSize;
            totalBytes += this.alloc[pass];
            totalBytesPass[pass] += this.alloc[pass];
        }
    }

    long[] pointer = new long[1];
    long[] offset = new long[1];
    public void draw(VkCommandBuffer commandBuffer, int pass) {
    	if (this.elementCount[pass] > 0) {
            pointer[0] = this.vkbuffersV[pass].getBuffer();
            offset[0] = 0;
            vkCmdBindVertexBuffers(commandBuffer, 0, pointer, offset);
            vkCmdBindIndexBuffer(commandBuffer, this.vkbuffersI[pass].getBuffer(), 0, VK_INDEX_TYPE_UINT16);
            vkCmdDrawIndexed(commandBuffer, this.elementCount[pass], 1, 0, 0, 0);
    	}
    }
}
