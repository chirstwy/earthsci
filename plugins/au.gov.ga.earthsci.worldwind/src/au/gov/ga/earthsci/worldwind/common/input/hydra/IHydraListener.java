/*******************************************************************************
 * Copyright 2013 Geoscience Australia
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
package au.gov.ga.earthsci.worldwind.common.input.hydra;

/**
 * Listens to Razer Hydra controller events.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public interface IHydraListener
{
	void updated(HydraEvent event);

	void buttonChanged(HydraButtonEvent event);

	void stickChanged(HydraStickEvent event);

	void triggerChanged(HydraTriggerEvent event);
}
