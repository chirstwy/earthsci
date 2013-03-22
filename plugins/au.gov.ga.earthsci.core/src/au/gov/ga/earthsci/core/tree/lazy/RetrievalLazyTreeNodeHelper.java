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
package au.gov.ga.earthsci.core.tree.lazy;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import au.gov.ga.earthsci.core.retrieve.IRetrieval;
import au.gov.ga.earthsci.core.retrieve.IRetrievalData;
import au.gov.ga.earthsci.core.retrieve.IRetrievalResult;
import au.gov.ga.earthsci.core.retrieve.IRetrievalService;
import au.gov.ga.earthsci.core.retrieve.RetrievalAdapter;
import au.gov.ga.earthsci.core.retrieve.RetrievalServiceFactory;
import au.gov.ga.earthsci.core.tree.ILazyTreeNode;
import au.gov.ga.earthsci.core.tree.ILazyTreeNodeCallback;
import au.gov.ga.earthsci.core.tree.ITreeNode;

/**
 * Helper class for the {@link IRetrievalLazyTreeNode}; uses the retrieval
 * service to retrieve the lazily loaded nodes.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class RetrievalLazyTreeNodeHelper<E>
{
	private final IRetrievalLazyTreeNode<E> node;
	private final AtomicBoolean loaded = new AtomicBoolean(false);
	private final AtomicBoolean loading = new AtomicBoolean(false);
	private Exception error;
	private final List<ITreeNode<E>> childrenAdded = new ArrayList<ITreeNode<E>>();

	public RetrievalLazyTreeNodeHelper(IRetrievalLazyTreeNode<E> node)
	{
		this.node = node;
	}

	/**
	 * @see ILazyTreeNode#load(ILazyTreeNodeCallback)
	 */
	public void load(final ILazyTreeNodeCallback callback)
	{
		if (!isLoaded() && loading.compareAndSet(false, true))
		{
			final URL url = node.getRetrievalURL();
			IRetrievalService retrievalService = RetrievalServiceFactory.getServiceInstance();
			IRetrieval retrieval = retrievalService.retrieve(node, url);
			retrieval.addListener(new RetrievalAdapter()
			{
				@Override
				public void cached(IRetrieval retrieval)
				{
					handleRetrieval(retrieval.getCachedData(), url);
					callback.loaded();
				}

				@Override
				public void complete(IRetrieval retrieval)
				{
					IRetrievalResult result = retrieval.getResult();
					if (!result.isSuccessful())
					{
						error = result.getError();
					}
					else if (!result.isFromCache())
					{
						handleRetrieval(result.getData(), url);
					}
					setLoaded(true);
					loading.set(false);
					callback.loaded();
				}
			});
			retrieval.start();
		}
	}

	protected void handleRetrieval(IRetrievalData data, URL url)
	{
		for (ITreeNode<E> child : childrenAdded)
		{
			node.remove(child);
		}
		childrenAdded.clear();

		try
		{
			ITreeNode<E>[] children = node.handleRetrieval(data, url);
			for (ITreeNode<E> child : children)
			{
				node.add(child);
				childrenAdded.clear();
			}
		}
		catch (Exception e)
		{
			error = e;
		}
	}

	/**
	 * @see ILazyTreeNode#isLoaded()
	 */
	public boolean isLoaded()
	{
		return loaded.get();
	}

	protected void setLoaded(boolean loaded)
	{
		node.firePropertyChange("loaded", this.loaded.getAndSet(loaded), loaded); //$NON-NLS-1$
	}

	/**
	 * @see ILazyTreeNode#getDisplayChildren()
	 */
	public ITreeNode<E>[] getDisplayChildren()
	{
		ITreeNode<E>[] children = node.getChildren();
		ITreeNode<E> firstNode = null;
		if (error != null)
		{
			firstNode = node.getErrorNode(error);
		}
		else if (loading.get())
		{
			firstNode = node.getLoadingNode();
		}
		if (firstNode != null)
		{
			@SuppressWarnings("unchecked")
			ITreeNode<E>[] newChildren = new ITreeNode[children.length + 1];
			newChildren[0] = firstNode;
			System.arraycopy(children, 0, newChildren, 1, children.length);
			children = newChildren;
		}
		return children;
	}
}
