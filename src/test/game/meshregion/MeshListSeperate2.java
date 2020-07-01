package test.game.meshregion;

import org.lwjgl.opengl.*;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.meshing.BlockFaceAttr;

public class MeshListSeperate2 extends MeshList {

	static int vao=0;
    final static int VERT_LEN1 = (6)<<2;
    final static int VERT_LEN2 = (3)<<2;
	private VertexBuffer bufferDataVertex;
	private VertexBuffer[] bufferDataAttr;
	final static int NUM_ATTR = 5;

	public MeshListSeperate2() {
		super(VMeshBufferSeperate2.class);
		this.bufferDataVertex = new VertexBuffer(1024*1024);
		this.bufferDataAttr = new VertexBuffer[NUM_ATTR];
		for (int i = 0; i < NUM_ATTR; i++) {
			this.bufferDataAttr[i] = new VertexBuffer(1024*1024);
		}
	}
	@Override
	public void init() {
		if (vao == 0) {
			setupVAO();
		}
		for (VMeshBuffer m : array) {
			((VMeshBufferSeperate2) m).vertexBuffer = new GLTriBuffer(false);
			((VMeshBufferSeperate2) m).faceAttrBuffers = new GLAttrBuffer[5];
			for (int i = 0; i < NUM_ATTR; i++) {
				((VMeshBufferSeperate2) m).faceAttrBuffers[i] = new GLAttrBuffer();
			}
		}
	}

	@Override
	public void draw() {
    	this.bindVAO();
		for (VMeshBuffer m : array) {
			VMeshBufferSeperate2 il = (VMeshBufferSeperate2) m;
	        GL43.glBindVertexBuffer(0, il.vertexBuffer.getGLArrayBuffer(), 0, 16);
			for (int i = 0; i < NUM_ATTR; i++) {
		        GL43.glBindVertexBuffer(1+i, il.faceAttrBuffers[i].getGLArrayBuffer(), 0, getVStride(i)*4);
			}
	        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, il.vertexBuffer.getGLIndexBuffer());
	        GL11.glDrawElements(GL11.GL_TRIANGLES, il.vertexBuffer.getIdxCount(), GL11.GL_UNSIGNED_INT, 0);
//	        System.out.println(il.vertexBuffer.getIdxCount());
		}
	}

	@Override
	public void bindVAO() {
        GL30.glBindVertexArray(vao);
	}
	@Override
	public void addFace(BlockFaceAttr attr) {
		attr.putPos(bufferDataVertex);
		attr.putFaceVAttr(bufferDataAttr[0], 0);
		attr.putFaceVAttr(bufferDataAttr[1], 1);
		attr.putFaceVAttr(bufferDataAttr[2], 2);
		attr.putFaceVAttr(bufferDataAttr[3], 3);
		attr.putFaceVAttr(bufferDataAttr[4], 4);
	}
	@Override
	public void reset() {
		bufferDataVertex.reset();
		for (int i = 0; i < NUM_ATTR; i++) {
			bufferDataAttr[i].reset();
		}
	}
	@Override
	public void upload(int x, int z) {
		System.out.println(getClass().getSimpleName()+" upload "+(this.bufferDataVertex.getPos()*4)+" bytes for vertex data");
		System.out.println(getClass().getSimpleName()+" upload "+(this.bufferDataVertex.getTriIdxPos()*4)+" bytes for index data");
//		System.out.println(getClass().getSimpleName()+" upload "+(this.bufferDataFace.getPos()*4)+" bytes for face attr data");
//		vertexBuffer.upload(this.bufferDataVertex);
//		faceAttrBuffer.upload(bufferDataFace);
		VMeshBufferSeperate2 m = (VMeshBufferSeperate2) getMesh(x, z);
		m.vertexBuffer.upload(this.bufferDataVertex);
		for (int i = 0; i < NUM_ATTR; i++) {
			m.faceAttrBuffers[i].upload(this.bufferDataAttr[i]);
		}
	}
	

	private int getVStride(int i) {
		switch (i) {
		case 0:
			return 1;
		case 1:
			return 1;
		case 2:
			return 1;
		case 3:
			return 2;
		case 4:
			return 1;
		}
		return 0;
	}
	static void setupVAO() {

		vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        //POS
        GL20.glEnableVertexAttribArray(0);
        GL43.glVertexAttribFormat(0, 4, GL11.GL_FLOAT, false, 0);
        GL43.glVertexAttribBinding(0, 0); // bind to first vertex buffer
        
        
        //NORMAL
        GL20.glEnableVertexAttribArray(1);
        GL43.glVertexAttribFormat(1, 3, GL11.GL_BYTE, false, 0);
        GL43.glVertexAttribBinding(1, 1);
        
        //TEXCOORD
        GL20.glEnableVertexAttribArray(2);
        GL43.glVertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, 0);
        GL43.glVertexAttribBinding(2, 2);
        //COLOR
        GL20.glEnableVertexAttribArray(3);
        GL43.glVertexAttribFormat(3, 4, GL11.GL_UNSIGNED_BYTE, true, 0);
        GL43.glVertexAttribBinding(3, 3);
        //BLOCKINFO
        GL20.glEnableVertexAttribArray(4);
        GL43.glVertexAttribIFormat(4, 4, GL11.GL_UNSIGNED_SHORT, 0);
        GL43.glVertexAttribBinding(4, 4);
        //LIGHTINFO
        GL20.glEnableVertexAttribArray(5);
        GL43.glVertexAttribIFormat(5, 2, GL11.GL_UNSIGNED_SHORT, 0);
        GL43.glVertexAttribBinding(5, 5);
        GL30.glBindVertexArray(0);
        Engine.checkGLError("glBindVertexArray");
	}
}
