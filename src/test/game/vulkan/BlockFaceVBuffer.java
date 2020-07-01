package test.game.vulkan;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.VkCommandBuffer;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.meshing.BlockFaceAttrUINT;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vulkan.*;

public class BlockFaceVBuffer {
    protected ReallocIntBuffer[] buffers = new ReallocIntBuffer[NUM_PASSES*4];
    protected ReallocIntBuffer[] idxShortBuffers = new ReallocIntBuffer[NUM_PASSES*4];
    public static long totalBytes = 0;
    public static long totalBytesPass[] = new long[NUM_PASSES];
    static int nextBuffer = 0;
    static final int REGION_DIST = 6;
	float size = 0.5f;

	BlockFaceAttr attr = new BlockFaceAttrUINT(); 
	private VertexBuffer bufferDataVertex;
    public int[]     vertexCount   = new int[NUM_PASSES];
    public boolean[] hasPass       = new boolean[NUM_PASSES];
    public boolean hasAnyPass;
    private int shadowDrawMode;
    long alloc[] = new long[NUM_PASSES];
	private BufferPair[] vkbuffers;
	public BlockFaceVBuffer() {
	}
	
	public void init(VKContext ctxt) {

        for (int i = 0; i < idxShortBuffers.length; i++) {
            idxShortBuffers[i] = new ReallocIntBuffer();
        }
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new ReallocIntBuffer();
        }
		this.bufferDataVertex = new VertexBuffer(1024 * 1024);
		this.vkbuffers = new BufferPair[NUM_PASSES];
//		for (int i = 0; i < NUM_PASSES; i++) {
//			this.vkbuffers[i] = new VkBuffer(ctxt).tag("blockface_"+i+"_vertex");
//			this.vkbuffersI[i] = new VkBuffer(ctxt).tag("blockface_"+i+"_index");
//		}
	}

	public void redraw() {
		int k =4;
		float r = 1f;
		int n = 0;
        int rOffset = (k*2+1);
        bufferDataVertex.reset();
		for (int x = -REGION_DIST; x <= REGION_DIST; x++) {
			for (int z = -REGION_DIST; z <= REGION_DIST; z++) {
				float rX = x*rOffset*r*1;
				float rZ = z*rOffset*r*1;
				for (int i = -k; i <= k; i++) {
					for (int j = -k; j <= k; j++) {
						drawFace(rX+i*r, 0, rZ+j*r, n++%12);
			        	attr.put(bufferDataVertex);
					}
				}
			}
		}
		uploadBuffer(0, bufferDataVertex, 0);
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
        Engine.vkContext.orphanResource(this.vkbuffers[pass]);
        BufferPair newBuffer = Engine.vkContext.getFreeBuffer();
        newBuffer.uploadDeviceLocal(buf.getByteBuf(), intlen, shBuffer.getByteBuf(), intlenIdx);
        newBuffer.setElementCount(intlenIdx);
        this.vkbuffers[pass] = newBuffer;

        int byteSize = (intlenIdx * 4) + (intlen * 4);
        
        if (alloc[pass] != byteSize) {
            totalBytes -= this.alloc[pass];
            totalBytesPass[pass] -= this.alloc[pass];
            this.alloc[pass] = byteSize;
            totalBytes += this.alloc[pass];
            totalBytesPass[pass] += this.alloc[pass];
        }
    }

    public void draw(CommandBuffer commandBuffer, int pass) {
    	BufferPair n = this.vkbuffers[pass];
    	if (n != null) {
    		n.draw(commandBuffer);
    	}
    }
}
