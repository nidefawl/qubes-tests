package test.game.vr;

import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import java.io.*;
import java.nio.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;

import jopenvr.*;
import jopenvr.JOpenVRLibrary.EVRCompositorError;
import jopenvr.TrackedDevicePose_t.ByReference;
import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.vec.*;

public class VR {

	public static boolean DEBUG_PRINT = false;
	public static boolean READ_MATS = false;
	public static VR_IVRSystem_FnTable vrsystem;
	public static class VRViewProjection {
		public Matrix4f projLeft = new Matrix4f();
		public Matrix4f projRight = new Matrix4f();
		public Matrix4f poseEyeLeft = new Matrix4f();
		public Matrix4f poseEyeRight = new Matrix4f();
		public Matrix4f viewLeft=new Matrix4f();
		public Matrix4f viewRight=new Matrix4f();
		public void setEyeToHeadTransform() {
			if (!READ_MATS) {
				HmdMatrix34_t matL = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left);
				OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(matL, this.poseEyeLeft);
				if (DEBUG_PRINT) {
					System.out.println("this.poseEyeLeft "+this.poseEyeLeft);
					System.out.println("this.poseEyeRight "+this.poseEyeRight);
				}
			}
			HmdMatrix34_t matR = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right);
			OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(matR, this.poseEyeRight);
		}
		public void setEyeProj(float nearClip, float farClip)
		{
			if (!READ_MATS) {
				HmdMatrix44_t matL = vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);

				OpenVRUtil.convertSteamVRMatrix4ToMatrix4f(matL, this.projLeft);
				if (DEBUG_PRINT) {
					System.out.println("this.projLeft "+this.projLeft);
					System.out.println("this.projRight "+this.projRight);
				}
				
			}
			HmdMatrix44_t matR = vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
			OpenVRUtil.convertSteamVRMatrix4ToMatrix4f(matR, this.projRight);
		}
		public void update(float f) {
//			setEyeProj(Engine.znear, Engine.zfar);
			setEyeToHeadTransform();
			Matrix4f.mul(this.poseEyeLeft, VR.hmdPose, viewLeft);
			Matrix4f.mul(this.poseEyeRight, VR.hmdPose2, viewRight);
		}
	}
	static class VRSettings {
		boolean seated;
		public boolean vrReverseHands;
	}
	public static VRSettings settings = new VRSettings();
	public static VRViewProjection cam = new VRViewProjection();
	public static FloatBuffer tlastVsync;
	public static LongBuffer _tframeCount;
	public static IntBuffer hmdDisplayFrequency;
	public static ByReference hmdTrackedDevicePoseReference;
	public static TrackedDevicePose_t[] hmdTrackedDevicePoses;
	public static double timePerFrame;
	public static boolean initSuccess;
	public static boolean initDone;
	public static String initStatus;
	public static VR_IVRCompositor_FnTable vrCompositor;
	public static IntBuffer hmdErrorStore;
	public static VR_IVROverlay_FnTable vrOverlay;
	public static VR_IVRRenderModels_FnTable vrRenderModels;
	public static VR_IVRSettings_FnTable vrSettings;
	final static VRTextureBounds_t texBounds = new VRTextureBounds_t();
	final static Texture_t texType0 = new Texture_t();
	final static Texture_t texType1 = new Texture_t();


	private static Matrix4f[] poseMatrices;
	private static Vec3D	[] deviceVelocity;
	public static final Matrix4f hmdPose = new Matrix4f();
	public static final Matrix4f hmdPose2 = new Matrix4f();
	private static boolean headIsTracking;
	// Controllers
	private static int RIGHT_CONTROLLER = 0;
	private static int LEFT_CONTROLLER = 1;
	
	static boolean[] controllerTracking = new boolean[2];
	private static Matrix4f[] controllerPose = new Matrix4f[2];
	private static Matrix4f[] controllerRotation = new Matrix4f[2];
	private static int[] controllerDeviceIndex = new int[2];
	private static VRControllerState_t.ByReference[] inputStateRefernceArray = new VRControllerState_t.ByReference[2];
	private static VRControllerState_t[] lastControllerState = new VRControllerState_t[2];
	private static VRControllerState_t[] controllerStateReference = new VRControllerState_t[2];
	private static final int maxControllerVelocitySamples = 5;
	private static Vec3D[][] controllerVelocitySamples = new Vec3D[2][maxControllerVelocitySamples];
	private static int[] controllerVelocitySampleCount = new int[2];
	private static Matrix4f[] controllerTipTransform = new Matrix4f[2];
	private static Matrix4f[] handRotation = new Matrix4f[2];
	public static int renderWidth;
	public static int renderHeight;

	static {

		for (int c=0;c<2;c++)
		{
//			aimSource[c] = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
			for (int sample = 0; sample < 5; sample++)
			{
//				touchpadSamples[c][sample] = new Vector2f(0, 0);
			}
//			touchpadSampleCount[c] = 0;
			controllerPose[c] = new Matrix4f();
			controllerRotation[c] = new Matrix4f();
			controllerDeviceIndex[c] = -1;
			controllerTipTransform[c] = new Matrix4f();
			handRotation[c] = new Matrix4f();
			
			lastControllerState[c] = new VRControllerState_t();
			controllerStateReference[c] = new VRControllerState_t();
			inputStateRefernceArray[c] = new VRControllerState_t.ByReference();

			inputStateRefernceArray[c].setAutoRead(false);
			inputStateRefernceArray[c].setAutoWrite(false);
			inputStateRefernceArray[c].setAutoSynch(false);
			for (int i = 0; i < 5; i++)
			{
				lastControllerState[c].rAxis[i] = new VRControllerAxis_t();
			}

			//controllerVelocitySamples[c] = new Vec3[2][maxControllerVelocitySamples];
			controllerVelocitySampleCount[c] = 0;
			for (int i=0;i<maxControllerVelocitySamples;i++)
			{
				controllerVelocitySamples[c][i] = new Vec3D(0, 0, 0);
			}
		}
	}
	private static Pointer ptrFomrString(String in){
		Pointer p = new Memory(in.length()+1);
		p.setString(0, in);
		return p;

	}
//	public static void main(String[] args) {
//		if (!InitVR()) {
//			System.out.println("fail");
//			System.out.println("init "+initStatus);
//		}
//		
//	}

	static boolean InitVR() {

		NativeLibrary.addSearchPath("openvr_api", ".");		

		if(jopenvr.JOpenVRLibrary.VR_IsHmdPresent() == 0){
			initStatus =  "VR Headset not detected.";
			return false;
		}

		try {
			initializeJOpenVR();
			initOpenVRCompositor(true) ;
			initOpenVROverlay() ;	
			initOpenVROSettings();
			initOpenVRRenderModels();
		} catch (Exception e) {
			e.printStackTrace();
			initSuccess = false;
			initStatus = e.getLocalizedMessage();
			return false;
		}

		deviceVelocity = new Vec3D[JOpenVRLibrary.k_unMaxTrackedDeviceCount];

		for(int i=0;i<poseMatrices.length;i++)
		{
			poseMatrices[i] = new Matrix4f();
			deviceVelocity[i] = new Vec3D(0, 0, 0);
		}
		cam.setEyeToHeadTransform();
		return true;
	
	}


	public static void initOpenVRRenderModels() throws Exception
	{
		vrRenderModels = new VR_IVRRenderModels_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRRenderModels_Version, hmdErrorStore));
		if (vrRenderModels != null && hmdErrorStore.get(0) == 0) {
			vrRenderModels.setAutoSynch(false);
			vrRenderModels.read();
			int count = vrRenderModels.GetRenderModelCount.apply();
			Pointer pointer = new Memory(JOpenVRLibrary.k_unMaxPropertyStringSize);
			for (int i = 0; i < count; i++) {
				vrRenderModels.GetRenderModelName.apply(i, pointer, JOpenVRLibrary.k_unMaxPropertyStringSize - 1);
				String name = pointer.getString(0);
				System.out.println("Render Model " + i + ": " + name);
			}
			System.out.println("OpenVR RenderModels initialized OK");
		} else {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));
		}
	}
	// needed for in-game keyboard
	public static void initOpenVROverlay() throws Exception
	{
		vrOverlay =   new VR_IVROverlay_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVROverlay_Version, hmdErrorStore));
		if (vrOverlay != null &&  hmdErrorStore.get(0) == 0) {     		
			vrOverlay.setAutoSynch(false);
			vrOverlay.read();					
			System.out.println("OpenVR Overlay initialized OK");
		} else {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		}
	}
	public static void initOpenVROSettings() throws Exception
	{
		vrSettings =   new VR_IVRSettings_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSettings_Version, hmdErrorStore));
		if (vrSettings != null &&  hmdErrorStore.get(0) == 0) {     		
			vrSettings.setAutoSynch(false);
			vrSettings.read();					
			System.out.println("OpenVR Settings initialized OK");
			
			IntByReference e = new IntByReference();
		
			float ret =	vrSettings.GetFloat.apply(ptrFomrString("steamvr"), ptrFomrString("renderTargetMultiplier"), -1f, e);

			int a = 9;
			
		} else {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		}
	}
	private static void initializeJOpenVR() throws Exception {
		hmdErrorStore = IntBuffer.allocate(1);
		JOpenVRLibrary.VR_InitInternal(hmdErrorStore, JOpenVRLibrary.EVRApplicationType.EVRApplicationType_VRApplication_Scene);
		
		if( hmdErrorStore.get(0) == 0 ) {
			// ok, try and get the vrsystem pointer..
			vrsystem = new VR_IVRSystem_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSystem_Version, hmdErrorStore));
		}
		if( vrsystem == null || hmdErrorStore.get(0) != 0 ) {
			throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));		
		} else {
			
			vrsystem.setAutoSynch(false);
			vrsystem.read();
			
			System.out.println("OpenVR initialized & VR connected.");
			
			tlastVsync = FloatBuffer.allocate(1);
			_tframeCount = LongBuffer.allocate(1);

			hmdDisplayFrequency = IntBuffer.allocate(1);
			hmdDisplayFrequency.put( (int) JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayFrequency_Float);
			hmdTrackedDevicePoseReference = new TrackedDevicePose_t.ByReference();
			hmdTrackedDevicePoses = (TrackedDevicePose_t[])hmdTrackedDevicePoseReference.toArray(JOpenVRLibrary.k_unMaxTrackedDeviceCount);
			poseMatrices = new Matrix4f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
			for(int i=0;i<poseMatrices.length;i++) poseMatrices[i] = new Matrix4f();

			timePerFrame = 1.0 / hmdDisplayFrequency.get(0);

			
			// disable all this stuff which kills performance
			hmdTrackedDevicePoseReference.setAutoRead(false);
			hmdTrackedDevicePoseReference.setAutoWrite(false);
			hmdTrackedDevicePoseReference.setAutoSynch(false);
			for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
				hmdTrackedDevicePoses[i].setAutoRead(false);
				hmdTrackedDevicePoses[i].setAutoWrite(false);
				hmdTrackedDevicePoses[i].setAutoSynch(false);
			}

			initSuccess = true;
		}
		
	}

	public static void initOpenVRCompositor(boolean set) throws Exception
	{
		if( set && vrsystem.GetFloatTrackedDeviceProperty != null ) {
			vrCompositor = new VR_IVRCompositor_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRCompositor_Version, hmdErrorStore));
			if(vrCompositor != null && hmdErrorStore.get(0) == 0){                
				System.out.println("OpenVR Compositor initialized OK.");
				vrCompositor.setAutoSynch(false);
				vrCompositor.read();
				vrCompositor.SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding);
				
				int buffsize=20;
				Pointer s=new Memory(buffsize);

				//vrCompositor.GetTrackingSpace.apply();
//				debugOut();

				vrsystem.GetStringTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd,JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ManufacturerName_String,s,buffsize,hmdErrorStore);
				String id=s.getString(0);
				System.out.println("Device manufacturer is: "+id);
				
//				if(!id.equals("HTC")) {
//					isVive=false;
//					mc.vrSettings.loadOptions();
//				}
//				
//				//TODO: detect tracking system
//				if(mc.vrSettings.seated && !isVive)
//					resetPosition();
//				else
//					clearOffset();
				
			} else {
				throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));			 
			}
		}
		if( vrCompositor == null ) {
			System.out.println("Skipping VR Compositor...");
//			if( vrsystem != null ) {
//				vsyncToPhotons = vrsystem.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_SecondsFromVsyncToPhotons_Float, hmdErrorStore);
//			} else {
//				vsyncToPhotons = 0f;
//			}
		}

		// left eye
		texBounds.uMax = 1f;
		texBounds.uMin = 0f;
		texBounds.vMax = 1f;
		texBounds.vMin = 0f;
		texBounds.setAutoSynch(false);
		texBounds.setAutoRead(false);
		texBounds.setAutoWrite(false);
		texBounds.write();


		// texture type
		texType0.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType0.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texType0.setAutoSynch(false);
		texType0.setAutoRead(false);
		texType0.setAutoWrite(false);
		texType0.handle = -1;
		texType0.write();

		
		// texture type
		texType1.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		texType1.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		texType1.setAutoSynch(false);
		texType1.setAutoRead(false);
		texType1.setAutoWrite(false);
		texType1.handle = -1;
		texType1.write();
		
		System.out.println("OpenVR Compositor initialized OK.");

	}


	
	public static String getCompostiorError(int code){
		switch (code){
		case EVRCompositorError.EVRCompositorError_VRCompositorError_DoNotHaveFocus:
			return "DoesNotHaveFocus";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_IncompatibleVersion:
			return "IncompatibleVersion";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_IndexOutOfRange:
			return "IndexOutOfRange";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_InvalidTexture:
			return "InvalidTexture";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_IsNotSceneApplication:
			return "IsNotSceneApplication";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_RequestFailed:
			return "RequestFailed";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_SharedTexturesNotSupported:
			return "SharedTexturesNotSupported";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_TextureIsOnWrongDevice:
			return "TextureIsOnWrongDevice";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_TextureUsesUnsupportedFormat:
			return "TextureUsesUnsupportedFormat:";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_None:
			return "None:";
		case EVRCompositorError.EVRCompositorError_VRCompositorError_AlreadySubmitted:
			return "AlreadySubmitted:";
		}
		return "Unknown";
	}
	
	

	static boolean hasRead=false;
	public static void readMat4s(String string, Matrix4f m) {
		File f = new File("nulldriver mats/"+string);
		try {
			FileInputStream fin = new FileInputStream(f);
			int off = 0;
			byte[] data = new byte[4];
			int i = 0;
			float[] f16 = new float[16];
			while (true) {
				int n = fin.read(data, off, data.length);
				if (n == -1) {
					break;
				}
				if (n != 4) {
					throw new RuntimeException("unexpected file len "+f);
				}
				int nFloat = (data[3]&0xFF) << 24 | (data[2]&0xFF) << 16 | (data[1]&0xFF) << 8| (data[0]&0xFF) << 0;
				float fl = Float.intBitsToFloat(nFloat);
				if (i == 0) {
					System.out.println(string+" "+fl);
				}
				int row = i/4;
				int col = i%4;
				f16[col*4+row] = fl;
				i++;
			}
			m.load(f16);
			fin.close();
		} catch (IOException e) {
			throw new RuntimeException("IOException "+f, e);
		}
	}
	public static void readMats() {
//		readMat4s("m_mat4ProjectionLeft", cam.projLeft);
//		readMat4s("m_mat4ProjectionRight", cam.projRight);
//		readMat4s("m_mat4eyePosLeft", cam.poseEyeLeft);
//		readMat4s("m_mat4eyePosRight", cam.poseEyeRight);
//		readMat4s("m_mat4HMDPose", hmdPose);
		
		
		readMat4s("m_mat4ProjectionRight", cam.projLeft);
		readMat4s("m_mat4eyePosRight", cam.poseEyeLeft);
		readMat4s("m_mat4HMDPose", hmdPose);
	}
	public static void updatePose(float f)
	{
		if ( vrsystem == null || vrCompositor == null || vrCompositor.WaitGetPoses == null)
			return;

		vrCompositor.WaitGetPoses.apply(hmdTrackedDevicePoseReference, JOpenVRLibrary.k_unMaxTrackedDeviceCount, null, 0);

		if (READ_MATS && !hasRead) {
			hasRead = true;
			readMats();
		}
		for (int nDevice = 0; nDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; ++nDevice )
		{
			hmdTrackedDevicePoses[nDevice].read();
			if ( hmdTrackedDevicePoses[nDevice].bPoseIsValid != 0 )
			{
				HmdMatrix34_t deviceTrackingMat = hmdTrackedDevicePoses[nDevice].mDeviceToAbsoluteTracking;
				OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(deviceTrackingMat, poseMatrices[nDevice]);
				deviceVelocity[nDevice].x = hmdTrackedDevicePoses[nDevice].vVelocity.v[0];
				deviceVelocity[nDevice].y = hmdTrackedDevicePoses[nDevice].vVelocity.v[1];
				deviceVelocity[nDevice].z = hmdTrackedDevicePoses[nDevice].vVelocity.v[2];
			}
		}

		if ( hmdTrackedDevicePoses[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd].bPoseIsValid != 0 )
		{
				
			OpenVRUtil.Matrix4fCopy(poseMatrices[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd], hmdPose2);
			//hellovr does hmdPose.invert() here
			hmdPose2.invert();
			headIsTracking = true;
//			System.out.println("headIsTracking "+hmdPose);
		}
		else
		{
			headIsTracking = false;
			OpenVRUtil.Matrix4fSetIdentity(hmdPose2);
			hmdPose2.m31 = 1.62f;
		}
		if (!READ_MATS) {
			hmdPose.load(hmdPose2);
		}

//		findControllerDevices();
//
//		for (int c=0;c<2;c++)
//		{
//			if (controllerDeviceIndex[c] != -1)
//			{
//				controllerTracking[c] = true;
//				OpenVRUtil.Matrix4fCopy(poseMatrices[controllerDeviceIndex[c]], controllerPose[c]);
//			}
//			else
//			{
//				controllerTracking[c] = false;
////				OpenVRUtil.Matrix4fSetIdentity(controllerPose[c]);
//			}
//		}
//		getTipTransforms();
		cam.update(f);
	}
	
	private static void getTipTransforms(){
		int count = vrRenderModels.GetRenderModelCount.apply();
		Pointer pointer = new Memory(JOpenVRLibrary.k_unMaxPropertyStringSize);
		for (int i = 0; i < 2; i++) {
			if (controllerDeviceIndex[i] != -1 && !settings.seated) {
				vrsystem.GetStringTrackedDeviceProperty.apply(controllerDeviceIndex[i], JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_RenderModelName_String, pointer, JOpenVRLibrary.k_unMaxPropertyStringSize - 1, hmdErrorStore);
				RenderModel_ControllerMode_State_t modeState = new RenderModel_ControllerMode_State_t();
				RenderModel_ComponentState_t componentState = new RenderModel_ComponentState_t();
				vrRenderModels.GetComponentState.apply(pointer, ptrFomrString("tip"), controllerStateReference[i], modeState, componentState);
				OpenVRUtil.convertSteamVRMatrix3ToMatrix4f(componentState.mTrackingToComponentLocal, controllerTipTransform[i]);
			} else {
				OpenVRUtil.Matrix4fSetIdentity(controllerTipTransform[i]);
			}
		}
	}

	private static void findControllerDevices()
	{
		controllerDeviceIndex[RIGHT_CONTROLLER] = -1;
		controllerDeviceIndex[LEFT_CONTROLLER] = -1;
		
			if(settings.vrReverseHands){
				controllerDeviceIndex[RIGHT_CONTROLLER]  = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_LeftHand);
				controllerDeviceIndex[LEFT_CONTROLLER] = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_RightHand);
			}else {
				controllerDeviceIndex[LEFT_CONTROLLER]  = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_LeftHand);
				controllerDeviceIndex[RIGHT_CONTROLLER] = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_RightHand);
			}
	}

	public static void Submit() {
		if(VR.vrCompositor.Submit == null) return;
		if (VR.texType0.handle != fbLeft.getTexture(0)) {
			VR.texType0.handle = fbLeft.getTexture(0);
			VR.texType0.write();
		}
		if (VR.texType1.handle != fbRight.getTexture(0)) {
			VR.texType1.handle = fbRight.getTexture(0);
			VR.texType1.write();
		}
		int lret = VR.vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Left,
				VR.texType0, null,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);

		int rret = VR.vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Right,
				VR.texType1, null,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
		if (lret == EVRCompositorError.EVRCompositorError_VRCompositorError_DoNotHaveFocus)
			lret = 0;
		
		if (rret == EVRCompositorError.EVRCompositorError_VRCompositorError_DoNotHaveFocus){
			rret = 0;
			System.out.println("no focus");
		}
		
		if(lret + rret > 0){
			throw new RuntimeException("Compositor Error: Texture submission error: Left/Right " + VR.getCompostiorError(lret) + "/" + VR.getCompostiorError(rret));		
		}
//		IntBuffer rtx = IntBuffer.allocate(1);
//		IntBuffer rty = IntBuffer.allocate(1);
//		VR.vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);
//		if (rtx.get(0) != Game.displayWidth 
//				&& rty.get(0) != Game.displayHeight 
//				&& rtx.get(0) != renderWidth 
//				&& rty.get(0) != renderHeight) {
//			throw new RuntimeException("res changed: vr: "+rtx.get(0)+"x"+rty.get(0)+ " prev: "+renderWidth+"x"+renderHeight + " Game "+Game.displayWidth+"x"+Game.displayHeight);		
//		}

//		VR.vrCompositor.PostPresentHandoff.apply();
	}

	public static void initApp(GameBase instance) {
		if (!VR.InitVR()) {
			throw new GameError("VR NOT PRESENT");
		}
		
		IntBuffer rtx = IntBuffer.allocate(1);
		IntBuffer rty = IntBuffer.allocate(1);
		VR.vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);
		renderWidth = rtx.get(0);
		renderHeight = rty.get(0);
		System.out.printf("GetRecommendedRenderTargetSize %d %d\n", renderWidth, renderHeight);
		instance.setRenderResolution(renderWidth, renderHeight);
		initVRFB(renderWidth, renderHeight);
		VR.cam.setEyeProj(Engine.znear, Engine.zfar);
		initDone = true;
	}

	static FrameBuffer fbLeft, fbRight;
	private static void initVRFB(int w, int h) {
		if (fbLeft != null) fbLeft.release();
		if (fbRight != null) fbRight.release();
        fbLeft = new FrameBuffer(w, h);
        fbLeft.setColorAtt(GL_COLOR_ATTACHMENT0, GL11.GL_RGBA8);
        fbLeft.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 1F, 1F);
        fbLeft.setColorTexExtFmt(GL11.GL_RGBA);
        fbLeft.setColorTexExtType(GL11.GL_UNSIGNED_BYTE);
        fbLeft.setup(null);
        fbRight = new FrameBuffer(w, h);
        fbRight.setColorAtt(GL_COLOR_ATTACHMENT0, GL11.GL_RGBA8);
        fbRight.setClearColor(GL_COLOR_ATTACHMENT0, 1F, 0F, 0F, 1F);
        fbRight.setColorTexExtFmt(GL11.GL_RGBA);
        fbRight.setColorTexExtType(GL11.GL_UNSIGNED_BYTE);
        fbRight.setHasDepthAttachment();
        fbRight.setup(null);
        FrameBuffer.unbindFramebuffer();
	}
	final static Matrix4f tmpMat = new Matrix4f();
	public static void setupCamera(int i, float f) {
		switch (i) {
		case 0:
			Engine.getMatSceneP().load(VR.cam.projLeft);
			Engine.getMatSceneP().update();
			Matrix4f.mul(VR.cam.viewLeft, Engine.getIdentityMatrix(), tmpMat);
			break;
		case 1:
			Engine.getMatSceneP().load(VR.cam.projRight);
			Engine.getMatSceneP().update();
			Matrix4f.mul(VR.cam.viewRight, Engine.getIdentityMatrix(), tmpMat);
			break;
		case 2:
			Engine.getMatSceneP().load(Engine.getMatSceneP_internal());
			Engine.getMatSceneP().update();
			Matrix4f.mul(VR.cam.viewRight, Engine.camera.getViewMatrix(), tmpMat);
			break;
		}
    	Engine.updateCamera(tmpMat, Engine.camera.getPosition());
        UniformBuffer.updateUBO(null, f);
	}

	public static void bindAndClearFramebuffer(int i) {
		switch (i) {
		case 0:
			fbLeft.bind();
			fbLeft.clearFrameBuffer();
			break;
		case 1:
			fbRight.bind();
			fbRight.clearFrameBuffer();
			break;
		case 2:
			FrameBuffer.unbindFramebuffer();
	        glClearColor(0,0,0,0);
	        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		}

	}

	
	public static boolean isInit() {
		return initDone;
	}

	public static FrameBuffer getFB(int i) {
		return 0 == i ? fbLeft : fbRight;
	}

	public static void setViewPort(int i) {
		switch (i) {
		case 0:
			Engine.setViewport(0, 0, fbLeft.getWidth(), fbLeft.getHeight());
			break;
		case 1:
			Engine.setViewport(0, 0, fbRight.getWidth(), fbRight.getHeight());
			break;
		case 2:
			Engine.setViewport(0, 0, fbRight.getWidth(), fbRight.getHeight());
			break;
		}
	}
}
