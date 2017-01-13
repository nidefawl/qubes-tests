package test.game.meshregion;

import org.lwjgl.opengl.*;

import nidefawl.qubes.gl.*;
import static org.lwjgl.opengl.NVVertexBufferUnifiedMemory.*;
import static org.lwjgl.opengl.NVShaderBufferLoad.*;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.util.GameError;

public class MeshListInterleavedNV extends MeshList {

	static int vao = 0;
	final static int VERT_LEN1 = (6 + 3) << 2;
	private VertexBuffer bufferDataVertex;
	final static int[] attrOffsets = new int[6];

    protected MultiDrawIndirectBuffer buffer = new MultiDrawIndirectBuffer();
	private boolean bindless;
	public MeshListInterleavedNV(boolean b) {
		super(VMeshBufferInterleavedNV.class);
		this.bufferDataVertex = new VertexBuffer(1024 * 1024);
		this.bindless=b;
	}

	@Override
	public void init() {
		if (vao == 0) {
			setupVAO();
		}
		for (VMeshBuffer m : array) {
			((VMeshBufferInterleavedNV) m).vertexBuffer = new GLTriBuffer(GL15.GL_STATIC_DRAW);
		}
	}

	@Override
	public void draw() {
		if (bindless) {
			this.draw2();
		} else {
			this.draw1();
		}
	}
	boolean first = true;
	public void draw2() {
		if (first) {
			first = false;
			buffer.preDraw(GLVAO.vaoBlocks);
			for (VMeshBuffer m : array) {
				VMeshBufferInterleavedNV il = (VMeshBufferInterleavedNV) m;
				GLVBO vboV = il.vertexBuffer.getVbo();
				GLVBO vboI = il.vertexBuffer.getVboIndices();
				buffer.add(vboV, vboI, il.vertexBuffer.getIdxCount());
			}
		}
		buffer.render();
	}
	public void draw1() {
    	this.bindVAO();
        Engine.enableBindless();
		GL11.glEnableClientState(GL_VERTEX_ATTRIB_ARRAY_UNIFIED_NV);
		GL11.glEnableClientState(GL_ELEMENT_ARRAY_UNIFIED_NV);
		for (VMeshBuffer m : array) {
			VMeshBufferInterleavedNV il = (VMeshBufferInterleavedNV) m;
			GLVBO vboV = il.vertexBuffer.getVbo();
			GLVBO vboI = il.vertexBuffer.getVboIndices();
			for (int i = 0; i < 6; i++) {
				glBufferAddressRangeNV(GL_VERTEX_ATTRIB_ARRAY_ADDRESS_NV, i, vboV.addr + attrOffsets[i], vboV.size - attrOffsets[i]);
			}
			glBufferAddressRangeNV(GL_ELEMENT_ARRAY_ADDRESS_NV, 0, vboI.addr, vboI.size);
			GL11.glDrawElements(GL11.GL_TRIANGLES, il.vertexBuffer.getIdxCount(), GL11.GL_UNSIGNED_INT, 0);
		}
        Engine.disableBindless();
	}

	@Override
	public void bindVAO() {
		GL30.glBindVertexArray(vao);
	}


	@Override
	public void addFace(BlockFaceAttr attr) {
		attr.put(bufferDataVertex);
	}

	@Override
	public void reset() {
		bufferDataVertex.reset();
		first = true;
	}

	@Override
	public void upload(int x, int z) {
		System.out.println(getClass().getSimpleName() + " upload " + (this.bufferDataVertex.getPos() * 4)
				+ " bytes for vertex data");
		System.out.println(getClass().getSimpleName() + " upload " + (this.bufferDataVertex.getTriIdxPos() * 4)
				+ " bytes for index data");
		VMeshBufferInterleavedNV m = (VMeshBufferInterleavedNV) getMesh(x, z);
		m.vertexBuffer.upload(this.bufferDataVertex);
	}
	

	static void setupVAO() {
		vao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vao);
		int offset = 0;
		// POS
		GL20.glEnableVertexAttribArray(0);
		glVertexAttribFormatNV(0, 3, GL11.GL_FLOAT, false, VERT_LEN1);
		attrOffsets[0] = offset * 4;
//		GL43.glVertexAttribBinding(0, 0); // bind to first vertex buffer
		offset += 3;
		// NORMAL
		GL20.glEnableVertexAttribArray(1);
		glVertexAttribFormatNV(1, 3, GL11.GL_BYTE, false, VERT_LEN1);
		attrOffsets[1] = offset * 4;
//		GL43.glVertexAttribBinding(1, 0); // bind to first vertex buffer
		offset += 1;

		// 1 BYTE UNUSED (normal has 3 bytes)

		// TEXCOORD
		GL20.glEnableVertexAttribArray(2);
		glVertexAttribFormatNV(2, 2, GL30.GL_HALF_FLOAT, false, VERT_LEN1);
		attrOffsets[2] = offset * 4;
//		GL43.glVertexAttribBinding(2, 0); // bind to first vertex buffer
		offset += 1;
		// COLOR
		GL20.glEnableVertexAttribArray(3);
		glVertexAttribFormatNV(3, 4, GL11.GL_UNSIGNED_BYTE, true, VERT_LEN1);
		attrOffsets[3] = offset * 4;
//		GL43.glVertexAttribBinding(3, 0); // bind to first vertex buffer
		offset += 1;
		// BLOCKINFO
		GL20.glEnableVertexAttribArray(4);
		glVertexAttribIFormatNV(4, 4, GL11.GL_UNSIGNED_SHORT, VERT_LEN1);
		attrOffsets[4] = offset * 4;
//		GL43.glVertexAttribBinding(4, 0); // bind to first vertex buffer
		offset += 2;
		// LIGHTINFO
		GL20.glEnableVertexAttribArray(5);
		glVertexAttribIFormatNV(5, 2, GL11.GL_UNSIGNED_SHORT, VERT_LEN1);
		attrOffsets[5] = offset * 4;
//		GL43.glVertexAttribBinding(5, 0); // bind to first vertex buffer
		offset += 1;
		if (offset<<2 != VERT_LEN1) {
			throw new GameError("Invalid stride");
		}
		GL30.glBindVertexArray(0);
		Engine.checkGLError("glBindVertexArray");
	}
	public String getName() {
		return getClass().getSimpleName() + (bindless?" (bindless)":"");
	}
}
