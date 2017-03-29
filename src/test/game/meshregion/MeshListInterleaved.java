package test.game.meshregion;

import org.lwjgl.opengl.*;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.meshing.BlockFaceAttr;

public class MeshListInterleaved extends MeshList {

	static int vao = 0;
	final static int VERT_LEN1 = (6 + 3) << 2;
	private VertexBuffer bufferDataVertex;

	public MeshListInterleaved() {
		super(VMeshBufferInterleaved.class);
		this.bufferDataVertex = new VertexBuffer(1024 * 1024);
	}

	@Override
	public void init() {
		if (vao == 0) {
			setupVAO();
		}
		for (VMeshBuffer m : array) {
			((VMeshBufferInterleaved) m).vertexBuffer = new GLTriBuffer(false);
		}
	}

	@Override
	public void draw() {
    	this.bindVAO();
		for (VMeshBuffer m : array) {
			VMeshBufferInterleaved il = (VMeshBufferInterleaved) m;
			GL43.glBindVertexBuffer(0, il.vertexBuffer.getGLArrayBuffer(), 0, VERT_LEN1);
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, il.vertexBuffer.getGLIndexBuffer());
			GL11.glDrawElements(GL11.GL_TRIANGLES, il.vertexBuffer.getIdxCount(), GL11.GL_UNSIGNED_INT, 0);
		}
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
	}

	@Override
	public void upload(int x, int z) {
		System.out.println(getClass().getSimpleName() + " upload " + (this.bufferDataVertex.getPos() * 4)
				+ " bytes for vertex data");
		System.out.println(getClass().getSimpleName() + " upload " + (this.bufferDataVertex.getTriIdxPos() * 4)
				+ " bytes for index data");
		VMeshBufferInterleaved m = (VMeshBufferInterleaved) getMesh(x, z);
		 m.vertexBuffer.upload(this.bufferDataVertex);
	}
	

	static void setupVAO() {
		vao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vao);
		// POS
		GL20.glEnableVertexAttribArray(0);
		GL43.glVertexAttribFormat(0, 3, GL11.GL_FLOAT, false, 0);
		GL43.glVertexAttribBinding(0, 0); // bind to first vertex buffer
		int offset = 3;
		// NORMAL
		GL20.glEnableVertexAttribArray(1);
		GL43.glVertexAttribFormat(1, 3, GL11.GL_BYTE, false, offset * 4);
		GL43.glVertexAttribBinding(1, 0); // bind to first vertex buffer
		offset += 1;

		// 1 BYTE UNUSED (normal has 3 bytes)

		// TEXCOORD
		GL20.glEnableVertexAttribArray(2);
		GL43.glVertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, offset * 4);
		GL43.glVertexAttribBinding(2, 0); // bind to first vertex buffer
		offset += 1;
		// COLOR
		GL20.glEnableVertexAttribArray(3);
		GL43.glVertexAttribFormat(3, 4, GL11.GL_UNSIGNED_BYTE, true, offset * 4);
		GL43.glVertexAttribBinding(3, 0); // bind to first vertex buffer
		offset += 1;
		// BLOCKINFO
		GL20.glEnableVertexAttribArray(4);
		GL43.glVertexAttribIFormat(4, 4, GL11.GL_UNSIGNED_SHORT, offset * 4);
		GL43.glVertexAttribBinding(4, 0); // bind to first vertex buffer
		offset += 2;
		// LIGHTINFO
		GL20.glEnableVertexAttribArray(5);
		GL43.glVertexAttribIFormat(5, 2, GL11.GL_UNSIGNED_SHORT, offset * 4);
		GL43.glVertexAttribBinding(5, 0); // bind to first vertex buffer
		offset += 1;
		GL30.glBindVertexArray(0);
		Engine.checkGLError("glBindVertexArray");
	}
}
