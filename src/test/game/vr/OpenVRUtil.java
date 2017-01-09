/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.game.vr;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import jopenvr.HmdMatrix34_t;
import jopenvr.HmdMatrix44_t;
import nidefawl.qubes.vec.Matrix4f;

/**
 *
 * @author reden
 */
public class OpenVRUtil {

    private static final long SLEEP_PRECISION = TimeUnit.MILLISECONDS.toNanos(4);
    private static final long SPIN_YIELD_PRECISION = TimeUnit.MILLISECONDS.toNanos(2);
    
    public static void sleepNanos(long nanoDuration) {
        final long end = System.nanoTime() + nanoDuration; 
        long timeLeft = nanoDuration; 
        do { 
            try {
                if (timeLeft > SLEEP_PRECISION) {
                    Thread.sleep(1); 
                } else if (timeLeft > SPIN_YIELD_PRECISION) {
                    Thread.sleep(0); 
                }
            } catch(Exception e) { }
            timeLeft = end - System.nanoTime(); 
        } while (timeLeft > 0); 
    }

    // VIVE START
    public static void Matrix4fSet(Matrix4f mat, float m11, float m12, float m13, float m14, float m21, float m22, float m23, float m24, float m31, float m32, float m33, float m34, float m41, float m42, float m43, float m44)
    {
        mat.m00 = m11;
        mat.m10 = m12;
        mat.m20 = m13;
        mat.m30 = m14;
        mat.m01 = m21;
        mat.m11 = m22;
        mat.m21 = m23;
        mat.m31 = m24;
        mat.m02 = m31;
        mat.m12 = m32;
        mat.m22 = m33;
        mat.m32 = m34;
        mat.m03 = m41;
        mat.m13 = m42;
        mat.m23 = m43;
        mat.m33 = m44;
    }

    public static void Matrix4fCopy(Matrix4f source, Matrix4f dest)
    {
        dest.m00 = source.m00;
        dest.m10 = source.m10;
        dest.m20 = source.m20;
        dest.m30 = source.m30;
        dest.m01 = source.m01;
        dest.m11 = source.m11;
        dest.m21 = source.m21;
        dest.m31 = source.m31;
        dest.m02 = source.m02;
        dest.m12 = source.m12;
        dest.m22 = source.m22;
        dest.m32 = source.m32;
        dest.m03 = source.m03;
        dest.m13 = source.m13;
        dest.m23 = source.m23;
        dest.m33 = source.m33;
    }

    public static void Matrix4fSetIdentity(Matrix4f mat)
    {
        mat.m00 = mat.m11 = mat.m22 = mat.m33 = 1.0F;
        mat.m10 = mat.m01 = mat.m32 = mat.m13 = 0.0F;
        mat.m20 = mat.m21 = mat.m02 = mat.m23 = 0.0F;
        mat.m30 = mat.m31 = mat.m12 = mat.m03 = 0.0F;
    }
        
    public static Matrix4f convertSteamVRMatrix3ToMatrix4f(HmdMatrix34_t hmdMatrix, Matrix4f mat){
        Matrix4fSet(mat,
                hmdMatrix.m[0], hmdMatrix.m[1], hmdMatrix.m[2], hmdMatrix.m[3],
                hmdMatrix.m[4], hmdMatrix.m[5], hmdMatrix.m[6], hmdMatrix.m[7],
                hmdMatrix.m[8], hmdMatrix.m[9], hmdMatrix.m[10], hmdMatrix.m[11],
                0f, 0f, 0f, 1f
        );
        return mat;
    }
    
    public static Matrix4f convertSteamVRMatrix4ToMatrix4f(HmdMatrix44_t hmdMatrix, Matrix4f mat)
    {
        Matrix4fSet(mat, hmdMatrix.m[0], hmdMatrix.m[1], hmdMatrix.m[2], hmdMatrix.m[3],
                hmdMatrix.m[4], hmdMatrix.m[5], hmdMatrix.m[6], hmdMatrix.m[7],
                hmdMatrix.m[8], hmdMatrix.m[9], hmdMatrix.m[10], hmdMatrix.m[11],
                hmdMatrix.m[12], hmdMatrix.m[13], hmdMatrix.m[14], hmdMatrix.m[15]);
        return mat;
    }

//    public static Vector3f convertMatrix4ftoTranslationVector(Matrix4f mat) {
//        return new Vector3f(mat.m30, mat.m31, mat.m32);
//    }
//
//    public static Quatf convertMatrix4ftoRotationQuat(Matrix4f mat) {
//        return JMonkeyHelpers.convertMatrix4ftoRotationQuat(
//                mat.m00,mat.m10,mat.m20,
//                mat.m01,mat.m11,mat.m21,
//                mat.m02,mat.m12,mat.m22);
//    }
//
//    public static Matrix4f rotationXMatrix(float angle) {
//        float sina = (float) Math.sin((double)angle);
//        float cosa = (float) Math.cos((double)angle);
//        return new Matrix4f(1.0F, 0.0F, 0.0F,
//                            0.0F, cosa, -sina,
//                            0.0F, sina, cosa);
//    }
//
//    public static Matrix4f rotationZMatrix(float angle) {
//        float sina = (float) Math.sin((double)angle);
//        float cosa = (float) Math.cos((double)angle);
//        return new Matrix4f(cosa, -sina, 0.0F,
//                sina, cosa, 0.0f,
//                0.0F, 0.0f, 1.0f);
//    }
//
//    public static EulerOrient getEulerAnglesDegYXZ(Quatf q) {
//        EulerOrient eulerAngles = new EulerOrient();
//
//        eulerAngles.yaw = (float)Math.toDegrees(Math.atan2( 2*(q.x*q.z + q.w*q.y), q.w*q.w - q.x*q.x - q.y*q.y + q.z*q.z ));
//        eulerAngles.pitch = (float)Math.toDegrees(Math.asin ( -2*(q.y*q.z - q.w*q.x) ));
//        eulerAngles.roll = (float)Math.toDegrees(Math.atan2( 2*(q.x*q.y + q.w*q.z), q.w*q.w - q.x*q.x + q.y*q.y - q.z*q.z ));
//
//        return eulerAngles;
//    }
    // VIVE END

//    public static long getNativeWindow() {
//        long window = -1;
//        try {
//            Object displayImpl = null;
//            Method[] displayMethods = Display.class.getDeclaredMethods();
//            for (Method m : displayMethods) {
//                if (m.getName().equals("getImplementation")) {
//                    m.setAccessible(true);
//                    displayImpl = m.invoke(null, (Object[]) null);
//                    break;
//                }
//            }            
//            String fieldName = null;
//            switch (LWJGLUtil.getPlatform()) {
//                case LWJGLUtil.PLATFORM_LINUX:
//                    fieldName = "current_window";
//                    break;
//                case LWJGLUtil.PLATFORM_WINDOWS:
//                    fieldName = "hwnd";
//                    break;
//            }
//            if (null != fieldName) {
//                Field[] windowsDisplayFields = displayImpl.getClass().getDeclaredFields();
//                for (Field f : windowsDisplayFields) {
//                    if (f.getName().equals(fieldName)) {
//                        f.setAccessible(true);
//                        window = (Long) f.get(displayImpl);
//                        continue;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            throw new IllegalStateException(e);
//        }
//        return window;
//    }
}
