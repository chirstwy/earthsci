/*******************************************************************************
 * Copyright 2012 Geoscience Australia
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
package au.gov.ga.earthsci.core.model.layer.uri.handler;

import gov.nasa.worldwind.layers.Layer;

import java.net.URI;

/**
 * {@link ILayerURIHandler} implementation that handles the class:// URI scheme,
 * which uses the URI's authority part as a class name, then instantiates the
 * class as a new Layer.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class ClassURIHandler extends AbstractURIHandler
{
	private static final String SCHEME = "class"; //$NON-NLS-1$

	@Override
	public String getSupportedScheme()
	{
		return SCHEME;
	}

	@Override
	public Layer createLayerFromURI(URI uri) throws LayerURIHandlerException
	{
		try
		{
			@SuppressWarnings("unchecked")
			Class<? extends Layer> c = (Class<? extends Layer>) Class.forName(uri.getAuthority());
			return c.newInstance();
		}
		catch (Exception e)
		{
			throw new LayerURIHandlerException(e);
		}
	}
}