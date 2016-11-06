package test.game.meshregion;

import org.lwjgl.opengl.*;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.perf.GPUProfiler;

public class VMeshBufferSeperate extends VMeshBuffer {

	public GLTriBuffer vertexBuffer;
	public GLAttrBuffer faceAttrBuffer;
}
