/**
 * 
 */
package test.game;

import nidefawl.qubes.input.KeybindManager;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Vec3D;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class CameraController {
	public Vec3D pos = new Vec3D();
	public Vec3D lastPos = new Vec3D();
	public Vec3D mot = new Vec3D();
	public Vec3D lastMot = new Vec3D();
	public float yaw, lastYaw;
	public float pitch, lastPitch;
	protected float   forward;
	protected float   strafe;
	protected float   maxSpeed = 0.82F;
	protected float   jump;
	protected boolean   sneak;

	/**
	 * @param movement 
	 * 
	 */
	public void update(KeybindManager movement) {
        float fa = 0.14F;
        float mx = movement.mX * fa;
        float my = -movement.mY * fa;
        float newP = (float) Math.max(-90F, Math.min(90F, this.pitch + my));
        float newY = (float) this.yaw + mx;
        float diffP = newP - this.pitch;
        float diffY = newY - this.yaw;
        this.pitch = newP;
        this.yaw = newY;
        this.lastPitch += diffP;
        this.lastYaw += diffY;
        this.strafe = movement.strafe;
        this.forward = movement.forward;
        this.jump = movement.jump?1:0;
        this.sneak = movement.sneak;
        movement.mX = 0;
        movement.mY = 0;
	}
	public void tickUpdate() {
        float vel = GameMath.sqrtf(this.forward * this.forward + this.strafe * this.strafe);
        float slowdown = 0.28F;

        maxSpeed = 0.9F;
        float var7 = 0.0F;
        this.mot.y -= 0.98D * (this.sneak?1:0);
        this.mot.y += 0.98D * this.jump;

        float f4 = 0.0F;
        float f5 = 0.0F;
        float f6 = 0.0F;
        float f7 = 0.0F;
        if (vel >= 0.01F) {
            if (vel < 1.0F) {
                vel = 1.0F;
            }

            float strafe = -this.strafe / vel;
            float forward = -this.forward / vel;
            float sinY = GameMath.sin(GameMath.degreesToRadians(this.yaw));
            float cosY = GameMath.cos(GameMath.degreesToRadians(this.yaw));
            f4 = strafe * cosY;
            f5 = -forward * sinY;
            f6 = strafe * sinY;
            f7 = forward * cosY;
        }

        float f8 = GameMath.degreesToRadians(-this.pitch);
        float fm = GameMath.cos(f8);
        float f1 = -GameMath.sin(f8) * Math.signum(-this.forward);
        float f2 = f5 * fm + f4;
        float f3 = GameMath.sqrtf(f5 * f5 + f7 * f7) * f1 + var7;
        float f9 = f7 * fm + f6;
        float f10 = GameMath.sqrtf(GameMath.sqrtf(f2 * f2 + f9 * f9) + f3 * f3);
        if (f10 > 0.01F) {
            float f11 = maxSpeed / f10;
            this.mot.x += (double) (f2 * f11);
            this.mot.y += (double) (f3 * f11);
            this.mot.z += (double) (f9 * f11);
        }
        this.jump *= 1;
    
	    if (this.yaw > 360)
	        this.yaw -= 360;
	    if (this.yaw < 0)
	        this.yaw += 360;
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.lastMot.set(this.mot);
        this.lastPos.set(this.pos);
        Vec3D.add(this.pos, this.mot, this.pos);
        this.mot.x *= slowdown;
        this.mot.z *= slowdown;
        this.mot.y *= slowdown;
	}
	
	public void set(float x, float y, float z, float pitch, float yaw) {
		this.pos.set(x, y, z);
		this.lastPos.set(x, y, z);
		this.pitch = this.lastPitch = pitch;
		this.yaw = this.lastYaw = yaw;
	}
}
