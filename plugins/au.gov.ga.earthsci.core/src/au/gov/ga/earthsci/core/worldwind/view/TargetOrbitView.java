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

import gov.nasa.worldwind.awt.ViewInputHandler;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.view.orbit.OrbitView;

import java.awt.Point;
import java.awt.Rectangle;
import java.nio.FloatBuffer;

import javax.media.opengl.GL2;

/**
 * {@link OrbitView} extension that allows the user to optionally modify the
 * center of rotation point, instead of keeping the center point fixed to the
 * earth's surface, which is the default.
 * <p/>
 * Also draws an optional axis marker whenever the view changes to indicate the
 * current center of rotation.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class TargetOrbitView extends BaseOrbitView
{
	protected boolean targetMode = false;

	protected boolean nonTargetModeDetectCollisions = true;
	protected boolean targetModeDetectCollisions = false;
	protected Angle nonTargetMaxPitch = DEFAULT_MAX_PITCH;
	protected Angle targetMaxPitch = Angle.fromDegrees(170);

	protected boolean drawAxisMarker = true;
	protected final AxisRenderable axisMarker = new AxisRenderable();
	protected final EmptyScreenCredit viewScreenCredit = new EmptyScreenCredit()
	{
		@Override
		public void render(DrawContext dc)
		{
			TargetOrbitView.this.render(dc);
		}
	};

	protected final FloatBuffer depthPixel = FloatBuffer.allocate(1);
	protected Position mousePosition;

	@Override
	protected ViewInputHandler createViewInputHandler()
	{
		return new TargetOrbitViewInputHandler();
	}

	/**
	 * @return Is target mode enabled?
	 */
	public boolean isTargetMode()
	{
		return targetMode;
	}

	/**
	 * Enable/disable target mode. When enabled, the user can modify the center
	 * point, instead of fixing it to the earth's surface.
	 * <p/>
	 * If target mode is enabled, the minimum pitch limit will be set to -180
	 * degrees, and collision detection will be disabled.
	 * 
	 * @param targetMode
	 */
	public void setTargetMode(boolean targetMode)
	{
		if (this.targetMode == targetMode)
		{
			return;
		}

		this.targetMode = targetMode;

		Angle[] pitchLimits = this.viewLimits.getPitchLimits();
		if (targetMode)
		{
			this.nonTargetMaxPitch = pitchLimits[1];
			pitchLimits[1] = this.targetMaxPitch;
			this.nonTargetModeDetectCollisions = isDetectCollisions();
			setDetectCollisions(this.targetModeDetectCollisions);
		}
		else
		{
			this.targetMaxPitch = pitchLimits[1];
			pitchLimits[1] = this.nonTargetMaxPitch;
			this.targetModeDetectCollisions = isDetectCollisions();
			setDetectCollisions(this.nonTargetModeDetectCollisions);
		}
		this.viewLimits.setPitchLimits(pitchLimits[0], pitchLimits[1]);
	}

	/**
	 * @return Should the axis marker be drawn when the view changes?
	 */
	public boolean isDrawAxisMarker()
	{
		return drawAxisMarker;
	}

	/**
	 * Enable/disable the axis marker that is drawn when the view changes.
	 * 
	 * @param drawAxisMarker
	 */
	public void setDrawAxisMarker(boolean drawAxisMarker)
	{
		this.drawAxisMarker = drawAxisMarker;
	}

	/**
	 * @return Axis marker that is drawn when the view changes
	 */
	public AxisRenderable getAxisMarker()
	{
		return axisMarker;
	}

	/**
	 * @return Approximate mouse position in geographic coordinates
	 */
	public Position getMousePosition()
	{
		return mousePosition;
	}

	@Override
	public void focusOnViewportCenter()
	{
		if (isTargetMode())
		{
			//if we are in target mode, the center point can be changed by the user, so don't change it automatically
			return;
		}

		super.focusOnViewportCenter();
	}

	@Override
	protected void doApply(DrawContext dc)
	{
		Vec4 beforeApply = Vec4.UNIT_W.transformBy4(this.modelview);

		super.doApply(dc);

		//the screen credits are stored in a map, so adding this each frame is not a problem
		dc.addScreenCredit(viewScreenCredit);

		if (isDrawAxisMarker())
		{
			Vec4 afterApply = Vec4.UNIT_W.transformBy4(this.modelview);
			if (beforeApply.distanceToSquared3(afterApply) > 10)
			{
				//view has changed, so show the axis marker
				axisMarker.trigger();
			}
		}
	}

	/**
	 * Render method is called during the rendering of the scene controller's
	 * screen credits, but the {@link #viewScreenCredit}.
	 * 
	 * @param dc
	 */
	protected void render(DrawContext dc)
	{
		if (isDrawAxisMarker())
		{
			axisMarker.render(dc);
		}

		if (viewInputHandler instanceof TargetOrbitViewInputHandler)
		{
			//calculate mouse position in geographic coordinates

			Point mousePoint = ((TargetOrbitViewInputHandler) viewInputHandler).getMousePoint();
			if (mousePoint == null)
			{
				mousePosition = null;
				return;
			}

			GL2 gl = dc.getGL().getGL2();
			Rectangle viewport = getViewport();
			int winX = mousePoint.x;
			int winY = viewport.height - mousePoint.y - 1;
			gl.glReadPixels(winX, winY, 1, 1, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, depthPixel.rewind());
			float winZ = depthPixel.get(0);

			//see gluUnproject:
			Matrix mvpi = projection.multiply(modelview).getInverse();
			Vec4 screen = new Vec4(
					2.0 * (winX - viewport.x) / viewport.width - 1.0,
					2.0 * (winY - viewport.y) / viewport.height - 1.0,
					2.0 * winZ - 1.0, 1.0);
			Vec4 model = screen.transformBy4(mvpi);
			model = model.divide3(model.w);
			mousePosition = globe.computePositionFromPoint(model);
		}
	}
}
