package test.game.meshregion;

import nidefawl.qubes.meshing.BlockFaceAttr;

public abstract class MeshList {
	VMeshBuffer[] array = new VMeshBuffer[(VertexPointerTest.REGION_DIST*2+1)*(VertexPointerTest.REGION_DIST*2+1)];
	public MeshList(Class<? extends VMeshBuffer> clazz) {
		for (int x = -VertexPointerTest.REGION_DIST; x <= VertexPointerTest.REGION_DIST; x++) {
			for (int z = -VertexPointerTest.REGION_DIST; z <= VertexPointerTest.REGION_DIST; z++) {
    			VMeshBuffer m;
    			try {
					m = clazz.newInstance();
					m.x = x;
					m.z = z;
					int _x=x+VertexPointerTest.REGION_DIST;
					int _z=z+VertexPointerTest.REGION_DIST;
					int idx = _z*(VertexPointerTest.REGION_DIST*2+1)+_x;
					array[idx] = m;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
    		}
		}
	}

	public VMeshBuffer getMesh(int x, int z) {
		x+=VertexPointerTest.REGION_DIST;
		z+=VertexPointerTest.REGION_DIST;
		int idx = z*(VertexPointerTest.REGION_DIST*2+1)+x;
		return array[idx];
	}

	public abstract void bindVAO();


	public abstract void init();
	public abstract void draw();
	public abstract void reset();

	public abstract void upload(int x, int z);

	public abstract void addFace(BlockFaceAttr attr);

	public String getName() {
		return getClass().getSimpleName();
	}
}