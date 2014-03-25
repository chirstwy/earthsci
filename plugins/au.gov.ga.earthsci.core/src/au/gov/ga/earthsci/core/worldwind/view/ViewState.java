/*******************************************************************************
 * Copyright 2014 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.earthsci.core.worldwind.view;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;

/**
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 * 
 */
public class ViewState
{
	//constants
	protected final static double EPSILON = 1.0e-9;

	//state
	protected Position center = Position.ZERO;
	protected Angle heading = Angle.ZERO;
	protected Angle pitch = Angle.ZERO;
	protected Angle roll = Angle.ZERO;
	protected double zoom = 1.0;

	//cached derived values
	protected Globe lastGlobe;
	protected Vec4 lastUp;
	protected Vec4 lastForward;
	protected Vec4 lastSide;
	protected Matrix lastTransform;
	protected Matrix lastRotation;
	protected Vec4 lastCenterPoint;
	protected Vec4 lastEyePoint;
	protected Position lastEye;

	protected void clearCachedValues()
	{
		lastUp = null;
		lastForward = null;
		lastSide = null;
		lastTransform = null;
		lastRotation = null;
		lastCenterPoint = null;
		lastEyePoint = null;
		lastEye = null;
	}

	protected void clearCachedValuesIfGlobeChanged(Globe globe)
	{
		if (lastGlobe != globe)
		{
			clearCachedValues();
			lastGlobe = globe;
		}
	}

	public Position getCenter()
	{
		return center;
	}

	public void setCenter(Position center)
	{
		this.center = center;
		clearCachedValues();
	}

	public Angle getHeading()
	{
		return heading;
	}

	public void setHeading(Angle heading)
	{
		this.heading = heading;
		clearCachedValues();
	}

	public Angle getPitch()
	{
		return pitch;
	}

	public void setPitch(Angle pitch)
	{
		this.pitch = pitch;
		clearCachedValues();
	}

	public Angle getRoll()
	{
		return roll;
	}

	public void setRoll(Angle roll)
	{
		this.roll = roll;
		clearCachedValues();
	}

	public double getZoom()
	{
		return zoom;
	}

	public void setZoom(double zoom)
	{
		this.zoom = zoom;
		clearCachedValues();
	}

	public Matrix getRotation(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastRotation == null)
		{
			Matrix transform = Matrix.IDENTITY;
			transform = transform.multiply(Matrix.fromAxisAngle(roll, 0, 0, 1));
			transform = transform.multiply(Matrix.fromAxisAngle(pitch, -1, 0, 0));
			transform = transform.multiply(Matrix.fromAxisAngle(heading, 0, 0, -1));

			Vec4 eyePoint = globe.computePointFromPosition(center);
			Vec4 normal = globe.computeSurfaceNormalAtLocation(center.getLatitude(), center.getLongitude());
			Vec4 lookAtPoint = eyePoint.add3(normal);
			Vec4 north = globe.computeNorthPointingTangentAtLocation(center.getLatitude(), center.getLongitude());
			Matrix lookat = Matrix.fromViewLookAt(eyePoint, lookAtPoint, north);

			lastRotation = transform.multiply(lookat).getInverse();
		}
		return lastRotation;
	}

	public Vec4 getForward(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastForward == null)
		{
			Matrix rotation = getRotation(globe);
			lastForward = lastSide != null && lastUp != null ?
					lastSide.cross3(lastUp) :
					Vec4.UNIT_Z.transformBy4(rotation);
		}
		return lastForward;
	}

	public Vec4 getUp(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastUp == null)
		{
			Matrix rotation = getRotation(globe);
			lastUp = lastForward != null && lastSide != null ?
					lastForward.cross3(lastSide) :
					Vec4.UNIT_Y.transformBy4(rotation);
		}
		return lastUp;
	}

	public Vec4 getSide(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastSide == null)
		{
			Matrix rotation = getRotation(globe);
			lastSide = lastUp != null && lastForward != null ?
					lastUp.cross3(lastForward) :
					Vec4.UNIT_X.transformBy4(rotation);
		}
		return lastSide;
	}

	public Matrix getTransform(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastTransform == null)
		{
			//gluLookAt defines s as (f x u), but getSide returns (u x f), so negate it during matrix creation
			Vec4 s = getSide(globe);
			Vec4 u = getUp(globe);
			Vec4 f = getForward(globe);
			Vec4 eye = getEyePoint(globe);
			Matrix mAxes = new Matrix(
					-s.x, -s.y, -s.z, 0.0,
					u.x, u.y, u.z, 0.0,
					-f.x, -f.y, -f.z, 0.0,
					0.0, 0.0, 0.0, 1.0);
			Matrix mEye = Matrix.fromTranslation(-eye.x, -eye.y, -eye.z);
			lastTransform = mAxes.multiply(mEye);
		}
		return lastTransform;
	}

	public Vec4 getCenterPoint(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastCenterPoint == null)
		{
			lastCenterPoint = globe.computePointFromPosition(getCenter());
		}
		return lastCenterPoint;
	}

	public Vec4 getEyePoint(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastEyePoint == null)
		{
			Vec4 center = getCenterPoint(globe);
			Vec4 forward = getForward(globe);
			lastEyePoint = center.add3(forward.multiply3(-zoom));
		}
		return lastEyePoint;
	}

	public Position getEye(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastEye == null)
		{
			lastEye = globe.computePositionFromPoint(getEyePoint(globe));
		}
		return lastEye;
	}

	public void setEye(Position eye, Globe globe)
	{
		//TODO implement this so that it doesn't modify the center point, if possible, or required?

		Vec4 centerPoint = getCenterPoint(globe);
		Vec4 eyePoint = globe.computePointFromPosition(eye);
		setRoll(Angle.ZERO);
		setPitch(Angle.ZERO);
		setHeading(Angle.ZERO);
		setZoom(centerPoint.distanceTo3(eyePoint));
		setCenter(new Position(eye, 0));
	}

	//TODO remove temporary functions:

	@Deprecated
	public static String v4ts(Vec4 v)
	{
		java.text.DecimalFormat df = new java.text.DecimalFormat("0.0########");
		df.setRoundingMode(java.math.RoundingMode.HALF_EVEN);
		return "(" + df.format(v.x) + ", " + df.format(v.y) + ", " + df.format(v.z) + ", " + df.format(v.w) + ")";
	}

	@Deprecated
	public static String mts(Matrix m)
	{
		java.text.DecimalFormat df = new java.text.DecimalFormat("0.0########");
		df.setRoundingMode(java.math.RoundingMode.HALF_EVEN);
		return "Rounded: \n("
				+ df.format(m.m11) + ", " + df.format(m.m12) + ", " + df.format(m.m13) + ", " + df.format(m.m14) + "\n"
				+ df.format(m.m21) + ", " + df.format(m.m22) + ", " + df.format(m.m23) + ", " + df.format(m.m24) + "\n"
				+ df.format(m.m31) + ", " + df.format(m.m32) + ", " + df.format(m.m33) + ", " + df.format(m.m34) + "\n"
				+ df.format(m.m41) + ", " + df.format(m.m42) + ", " + df.format(m.m43) + ", " + df.format(m.m44) + ")";
	}
}